#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script Principal (Orchestrateur)
1. Extrait le texte (Pdf2Text)
2. Anonymise les données sensibles (Anonymise)
3. Envoie à l'IA (Groq) pour structurer selon le schéma ProjetDDS.json
4. Réinjecte les vraies données (Deanonymization)
5. Génère le JSON final
"""

import sys
import os
import json
import uuid
from datetime import datetime
from groq import Groq

# Configuration de l'encodage UTF-8 pour Windows
if sys.platform == 'win32':
    try:
        # Python 3.7+
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')
        sys.stderr.reconfigure(encoding='utf-8', errors='replace')
    except AttributeError:
        # Python < 3.7
        import codecs
        sys.stdout = codecs.getwriter('utf-8')(sys.stdout.buffer, 'replace')
        sys.stderr = codecs.getwriter('utf-8')(sys.stderr.buffer, 'replace')

# Import de tes modules précédents
# Assure-toi que Pdf2Text.py et Anonymise.py sont dans le même dossier
try:
    from Pdf2Text import DocumentExtractor
    from Anonymise import DataAnonymizer
except ImportError:
    print("ERREUR CRITIQUE : Les fichiers 'Pdf2Text.py' et 'Anonymise.py' sont introuvables.")
    sys.exit(1)

def safe_print(text):
    """Fonction helper pour imprimer du texte Unicode de manière sûre."""
    try:
        print(text)
    except UnicodeEncodeError:
        # Fallback : encoder avec gestion des erreurs
        encoded = text.encode('utf-8', errors='replace').decode('utf-8')
        print(encoded)

# --- CONFIGURATION ---
# Mets ta clé API ici ou dans les variables d'environnement
API_KEY = os.environ.get("GROQ_API_KEY", "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")

# Le Schéma JSON cible (Copié et nettoyé depuis ProjetDDS.json)
TARGET_SCHEMA = {
    "type": "object",
    "properties": {
        "nom": {
            "type": "string",
            "description": "Nom de famille du candidat"
        },
        "prenom": {
            "type": "string",
            "description": "Prénom du candidat"
        },
        "mail": {
            "type": "string",
            "description": "Adresse email du candidat (peut être un token comme <EMAIL_1>)"
        },
        "telephone": {
            "type": "string",
            "description": "Numéro de téléphone du candidat (peut être un token comme <PHONE_1>)"
        },
        "diplomes": {
            "type": "array",
            "description": "Liste des diplômes obtenus",
            "items": {
                "type": "object",
                "properties": {
                    "nomDiplome": {
                        "type": "string",
                        "description": "Nom du diplôme (ex: Master, Licence, BTS)"
                    },
                    "anneeObtention": {
                        "type": "string",
                        "description": "Année d'obtention (format YYYY)"
                    },
                    "domaine": {
                        "type": "string",
                        "description": "Domaine d'études (ex: Informatique, Gestion, etc.)"
                    }
                },
                "required": ["nomDiplome"]
            }
        },
        "experiences": {
            "type": "array",
            "description": "Liste des expériences professionnelles",
            "items": {
                "type": "object",
                "properties": {
                    "nomEntreprise": {
                        "type": "string",
                        "description": "Nom de l'entreprise"
                    },
                    "dureeExperience": {
                        "type": "string",
                        "description": "Durée de l'expérience (ex: '2 ans', '6 mois')"
                    },
                    "dateDebut": {
                        "type": "string",
                        "description": "Date de début (format YYYY-MM ou YYYY-MM-DD)"
                    }
                },
                "required": ["nomEntreprise"]
            }
        },
        "posteVise": {
            "type": "string",
            "description": "Poste visé par le candidat"
        },
        "dateDisponibilite": {
            "type": "string",
            "description": "Date de disponibilité (format YYYY-MM-DD ou YYYY-MM)"
        },
        "competences": {
            "type": "array",
            "description": "Liste des compétences techniques (langages, outils, technologies)",
            "items": {"type": "string"}
        },
        "softSkills": {
            "type": "array",
            "description": "Liste des compétences comportementales (ex: Autonomie, Travail d'équipe, etc.)",
            "items": {"type": "string"}
        },
        "permisDeConduite": {
            "type": "array",
            "description": "Liste des permis de conduite possédés",
            "items": {
                "type": "string",
                "enum": ["A", "B", "AM", "C", "D", "F"]
            }
        },
        "langues": {
            "type": "array",
            "description": "Liste des langues parlées avec leur niveau",
            "items": {
                "type": "string",
                "description": "Format: 'Langue - Niveau' (ex: 'Anglais - C1', 'Français - Natif')"
            }
        }
    },
    "required": ["nom", "prenom", "mail", "telephone", "competences"]
}

def deanonymize_json(data, pii_mapping):
    """
    Fonction récursive qui parcourt le JSON généré par l'IA
    et remplace les tokens (<EMAIL_1>) par les vraies valeurs.
    """
    if isinstance(data, dict):
        for key, value in data.items():
            data[key] = deanonymize_json(value, pii_mapping)
    elif isinstance(data, list):
        for i in range(len(data)):
            data[i] = deanonymize_json(data[i], pii_mapping)
    elif isinstance(data, str):
        # Vérifie si la string contient un token connu
        for token, real_value in pii_mapping.items():
            if token in data:
                return data.replace(token, real_value)
    return data

def main():
    if len(sys.argv) < 2:
        print("Usage: python main.py <chemin_cv> [chemin_lm] [candidat_id] [debug_dir]")
        sys.exit(1)

    cv_path = sys.argv[1]
    lm_path = sys.argv[2] if len(sys.argv) > 2 else None
    candidat_id = sys.argv[3] if len(sys.argv) > 3 else None
    debug_dir = sys.argv[4] if len(sys.argv) > 4 else None
    
    # ---------------------------------------------------------
    # ETAPE 1 : EXTRACTION DU TEXTE
    # ---------------------------------------------------------
    safe_print(f"📄 1. Extraction du texte de : {cv_path}")
    extractor = DocumentExtractor(cv_path)
    raw_text = extractor.extract_content()
    
    if lm_path and lm_path.strip():
        safe_print(f"📄 1b. Extraction de la LM : {lm_path}")
        extractor_lm = DocumentExtractor(lm_path)
        raw_text += "\n\n--- LETTRE DE MOTIVATION ---\n" + extractor_lm.extract_content()
    
    # Mode debug : sauvegarder le texte extrait (si pas déjà fait par CvParserService)
    if debug_dir:
        try:
            os.makedirs(debug_dir, exist_ok=True)
            extracted_file = os.path.join(debug_dir, "01_extracted_text_main.txt")
            with open(extracted_file, "w", encoding="utf-8") as f:
                f.write(raw_text)
            print(f"[DEBUG] Texte extrait (main.py) sauvegardé dans: {extracted_file}")
        except Exception as e:
            print(f"[DEBUG] Erreur lors de la sauvegarde du texte extrait: {e}")

    # ---------------------------------------------------------
    # ETAPE 2 : ANONYMISATION (Privacy Guard)
    # ---------------------------------------------------------
    safe_print("🔒 2. Anonymisation des données sensibles...")
    anonymizer = DataAnonymizer()
    safe_text, pii_mapping = anonymizer.process(raw_text)
    
    # Mode debug : sauvegarder le texte anonymisé et les clés
    if debug_dir:
        try:
            os.makedirs(debug_dir, exist_ok=True)
            anonymized_file = os.path.join(debug_dir, "02_anonymized_text_main.txt")
            keys_file = os.path.join(debug_dir, "02_anonymization_keys_main.json")
            with open(anonymized_file, "w", encoding="utf-8") as f:
                f.write(safe_text)
            with open(keys_file, "w", encoding="utf-8") as f:
                json.dump(pii_mapping, f, indent=4, ensure_ascii=False)
            print(f"[DEBUG] Texte anonymisé (main.py) sauvegardé dans: {anonymized_file}")
            print(f"[DEBUG] Clés d'anonymisation (main.py) sauvegardées dans: {keys_file}")
        except Exception as e:
            print(f"[DEBUG] Erreur lors de la sauvegarde des fichiers anonymisés: {e}")

    # ---------------------------------------------------------
    # ETAPE 3 : APPEL IA (Groq)
    # ---------------------------------------------------------
    safe_print("🤖 3. Analyse IA (Extraction structurée)...")
    
    client = Groq(api_key=API_KEY)
    
    system_prompt = f"""
    Tu es un expert en recrutement et extraction de données depuis des CV.
    Analyse le texte de CV suivant (qui a été anonymisé pour protéger les données personnelles).
    Extrais toutes les informations pertinentes pour remplir strictement le schéma JSON fourni.
    
    RÈGLES IMPORTANTES :
    0. NOM ET PRÉNOM (CRITIQUE) :
       - Le "nom" est le NOM DE FAMILLE (ex: "Dupont", "Martin", "Gherras")
       - Le "prenom" est le PRÉNOM (ex: "Jean", "Marie", "Salman")
       - Si tu vois "Salman Gherras" ou "<NAME_1>" qui contient "Salman Gherras", 
         alors nom = "Gherras" et prenom = "Salman"
       - Si le nom complet est dans un token <NAME_1>, essaie de le décomposer en nom/prénom
       - En français, le format est généralement "Prénom Nom" (ex: "Jean Dupont" → prenom="Jean", nom="Dupont")
       - Si tu ne peux pas distinguer, utilise le premier mot comme prénom et le reste comme nom
    
    1. TOKENS D'ANONYMIATION : Si une information est un token comme <EMAIL_1>, <PHONE_1>, <NAME_1>, ou <LINK_1>, 
       garde-le TEL QUEL dans le JSON SAUF pour le nom/prénom où tu dois extraire les valeurs si possible.
       Pour les autres champs (email, téléphone, liens), garde les tokens tels quels.
    
    2. FORMAT DES DATES : 
       - Années : Format YYYY (ex: 2024)
       - Dates complètes : Format YYYY-MM-DD (ex: 2024-10-15)
       - Mois : Format YYYY-MM (ex: 2024-10)
       - Si seule l'année est disponible, utilise YYYY
    
    3. DIPLÔMES :
       - Extrais tous les diplômes mentionnés (Master, Licence, BTS, etc.)
       - Pour "anneeObtention", utilise l'année de fin d'études ou d'obtention
       - Le "domaine" correspond au domaine d'études (ex: Informatique, Gestion, etc.)
    
    4. EXPÉRIENCES PROFESSIONNELLES :
       - Extrais toutes les expériences professionnelles, stages, alternances
       - "dureeExperience" : Format lisible (ex: "2 ans", "6 mois", "1 an et 3 mois")
       - "dateDebut" : Date de début au format YYYY-MM ou YYYY-MM-DD
    
    5. COMPÉTENCES :
       - Liste toutes les compétences techniques : langages (Java, Python, JavaScript), 
         frameworks (Spring Boot, React), outils (Git, Docker), bases de données (MySQL, PostgreSQL), etc.
       - Sois exhaustif et inclusif
    
    6. SOFT SKILLS :
       - Extrais les compétences comportementales mentionnées (ex: Autonomie, Travail d'équipe, 
         Rigueur, Communication, Esprit d'initiative, etc.)
    
    7. PERMIS DE CONDUIRE :
       - Extrais uniquement les permis mentionnés (A, B, AM, C, D, F)
       - Si "Permis B" est mentionné, ajoute "B" dans le tableau
    
    8. LANGUES :
       - Format : "Langue - Niveau" (ex: "Français - Natif", "Anglais - C1", "Espagnol - B2")
       - Si le niveau n'est pas précisé, utilise "Non spécifié" ou estime selon le contexte
    
    9. POSTE VISÉ :
       - Si un poste spécifique est mentionné dans le CV, utilise-le
       - Sinon, déduis-le des compétences et expériences
    
    10. DATE DE DISPONIBILITÉ :
        - Si mentionnée explicitement, utilise-la
        - Sinon, laisse vide ou utilise une date raisonnable basée sur le contexte
    
    IMPORTANT : 
    - Ne réponds QUE par le JSON valide, rien d'autre (pas de texte avant ou après)
    - Respecte strictement le schéma fourni
    - Tous les champs requis doivent être présents
    - Les tableaux peuvent être vides [] si aucune information n'est trouvée
    - Sois précis et exhaustif dans l'extraction
    
    Schéma JSON à respecter :
    {json.dumps(TARGET_SCHEMA, indent=2, ensure_ascii=False)}
    """

    # Mode debug : sauvegarder la requête envoyée à l'IA
    if debug_dir:
        try:
            os.makedirs(debug_dir, exist_ok=True)
            request_file = os.path.join(debug_dir, "03_ia_request.json")
            request_data = {
                "model": "llama-3.3-70b-versatile",
                "system_prompt": system_prompt,
                "user_content": safe_text,
                "response_format": {"type": "json_object"},
                "temperature": 0
            }
            with open(request_file, "w", encoding="utf-8") as f:
                json.dump(request_data, f, indent=4, ensure_ascii=False)
            print(f"[DEBUG] Requête IA sauvegardée dans: {request_file}")
        except Exception as e:
            print(f"[DEBUG] Erreur lors de la sauvegarde de la requête: {e}")

    try:
        completion = client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": safe_text}
            ],
            response_format={"type": "json_object"},
            temperature=0
        )
        
        json_str = completion.choices[0].message.content
        data_ia = json.loads(json_str)
        
        # Mode debug : sauvegarder la réponse de l'IA
        if debug_dir:
            try:
                response_file = os.path.join(debug_dir, "04_ia_response.json")
                with open(response_file, "w", encoding="utf-8") as f:
                    json.dump(data_ia, f, indent=4, ensure_ascii=False)
                print(f"[DEBUG] Réponse IA sauvegardée dans: {response_file}")
            except Exception as e:
                print(f"[DEBUG] Erreur lors de la sauvegarde de la réponse: {e}")
        
    except Exception as e:
        print(f"ERREUR lors de l'appel API : {e}")
        sys.exit(1)

    # ---------------------------------------------------------
    # ETAPE 4 : RECONSTRUCTION & FORMATAGE FINAL
    # ---------------------------------------------------------
    safe_print("4. Reconstruction des donnees reelles...")
    
    # 1. Réinjecter les emails/tels à la place des tokens
    final_data = deanonymize_json(data_ia, pii_mapping)
    
    # 2. Correction spéciale pour nom/prénom si l'IA a mis le nom complet dans les deux champs
    # Si nom et prenom sont identiques et contiennent un espace, c'est probablement un nom complet
    if "nom" in final_data and "prenom" in final_data:
        nom = final_data.get("nom", "").strip()
        prenom = final_data.get("prenom", "").strip()
        
        # Si nom et prenom sont identiques et contiennent un espace, décomposer
        if nom == prenom and " " in nom:
            parts = nom.split()
            if len(parts) >= 2:
                # En français, format généralement "Prénom Nom"
                final_data["prenom"] = parts[0]
                final_data["nom"] = " ".join(parts[1:])
                safe_print(f"[CORRECTION] Nom complet detecte: '{nom}' -> prenom='{final_data['prenom']}', nom='{final_data['nom']}'")
        
        # Si le nom ou prénom contient encore un token <NAME_X>, essayer de le remplacer
        if "<NAME_" in nom or "<NAME_" in prenom:
            # Chercher dans le mapping
            for token, real_value in pii_mapping.items():
                if "NAME" in token:
                    if token in nom:
                        final_data["nom"] = nom.replace(token, real_value)
                    if token in prenom:
                        final_data["prenom"] = prenom.replace(token, real_value)
            
            # Si après remplacement, nom et prenom sont identiques, décomposer
            nom = final_data.get("nom", "").strip()
            prenom = final_data.get("prenom", "").strip()
            if nom == prenom and " " in nom:
                parts = nom.split()
                if len(parts) >= 2:
                    final_data["prenom"] = parts[0]
                    final_data["nom"] = " ".join(parts[1:])
    
    # 3. Ajouter les champs techniques manquants du schéma (ID, Fichiers...)
    # L'IA ne peut pas deviner l'ID ou les noms de fichiers, on le fait en Python
    final_data["idCandidature"] = str(uuid.uuid4()) # Génère un ID unique
    final_data["dateCandidature"] = datetime.now().strftime("%Y-%m-%d")
    
    # Récupération des emails et tels depuis le mapping s'ils n'ont pas été mis par l'IA
    # Parfois l'IA oublie de mettre le champ "mail" si elle voit <EMAIL_1>
    if "mail" not in final_data or not final_data["mail"]:
        # On cherche s'il y a un email dans le mapping
        emails = [v for k,v in pii_mapping.items() if "EMAIL" in k]
        if emails:
            final_data["mail"] = emails[0]
            
    if "telephone" not in final_data or not final_data["telephone"]:
        phones = [v for k,v in pii_mapping.items() if "PHONE" in k]
        if phones:
            final_data["telephone"] = phones[0]

    # Ajout de l'objet "fichiers" comme demandé dans ton JSON Schema
    final_data["fichiers"] = {
        "cv_filename": os.path.basename(cv_path),
        "lm_filename": os.path.basename(lm_path) if lm_path else ""
    }

    # ---------------------------------------------------------
    # ETAPE 5 : SAUVEGARDE
    # ---------------------------------------------------------
    output_filename = f"candidature_{final_data['nom']}_{final_data['prenom']}.json"
    # Nettoyage du nom de fichier
    output_filename = output_filename.replace(" ", "_").replace("<", "").replace(">", "")
    
    with open(output_filename, "w", encoding="utf-8") as f:
        json.dump(final_data, f, indent=4, ensure_ascii=False)
        
    print("-" * 50)
    safe_print("SUCCES ! Donnees structurees sauvegardees dans : " + output_filename)
    print("-" * 50)
    # Affichage d'un aperçu
    print(json.dumps(final_data, indent=2, ensure_ascii=False))

if __name__ == "__main__":
    main()