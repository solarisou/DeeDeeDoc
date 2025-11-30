#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script d'anonymisation RGPD.
Détecte les PII (Emails, Téléphones, Liens, Noms) et les remplace par des tokens.
"""

import sys
import re
import json
import logging
import argparse
from pathlib import Path
from typing import Tuple, Dict

# Configuration du logging
logging.basicConfig(level=logging.INFO, format='%(message)s')
logger = logging.getLogger(__name__)

# Gestion de l'encodage pour Windows
if sys.platform == 'win32':
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

class DataAnonymizer:

    def __init__(self):
        self.pii_mapping: Dict[str, str] = {}
        self.counts = {"EMAIL": 0, "PHONE": 0, "LINK": 0, "NAME": 0}

        # Compilation des regex pour la performance
        self.re_email_std = re.compile(r'\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}\b', re.IGNORECASE)
        self.re_email_malformed = re.compile(r'\b([a-zA-Z0-9._-]+)(gmail|yahoo|hotmail|outlook|icloud|protonmail|mail)\.(com|fr|net|org|eu)\b', re.IGNORECASE)
        self.re_phone = re.compile(r'(?:(?:\+|00)33|0)\s*[1-9](?:[\s.-]*\d{2}){4}')
        
        # Regex URLs
        self.re_url_full = re.compile(r'https?://(?:[-\w.]|(?:%[\da-fA-F]{2}))+[/\w\.-]*', re.IGNORECASE)
        self.re_url_simple = re.compile(r'(?:github\.com|gitlab\.com|bitbucket\.org|linkedin\.com/in|linkedin\.com/pub|twitter\.com|x\.com|facebook\.com|instagram\.com)/[a-zA-Z0-9._/-]+', re.IGNORECASE)
        self.re_url_www = re.compile(r'www\.\w+(?:\.\w+)+[/\w\.-]*', re.IGNORECASE)
        self.re_username_extractor = re.compile(r'(?:github\.com|linkedin\.com/in|gitlab\.com)/([a-zA-Z0-9-]+)', re.IGNORECASE)


    def _get_token(self, pii_type: str) -> str:
        """Génère un token unique, ex: <EMAIL_1>"""
        self.counts[pii_type] += 1
        return f"<{pii_type}_{self.counts[pii_type]}>"

    def _register_and_mask(self, text: str, value: str, pii_type: str) -> str:
        """Helper pour enregistrer une PII et masquer ses occurrences"""
        if not value or value.startswith('<') and value.endswith('>'):
            return text
            
        # Vérifier si déjà mappé
        for token, original in self.pii_mapping.items():
            if original == value:
                return re.sub(re.escape(value), token, text, flags=re.IGNORECASE)

        token = self._get_token(pii_type)
        self.pii_mapping[token] = value
        return re.sub(re.escape(value), token, text, flags=re.IGNORECASE)

    def mask_emails(self, text: str) -> str:
        """Masque les emails standards et tente de corriger les mal formés."""
        # 1. Correction des emails mal formés
        for match in self.re_email_malformed.finditer(text):
            reconstructed = f"{match.group(1)}@{match.group(2)}.{match.group(3)}"
            text = text.replace(match.group(0), reconstructed)
            text = self._register_and_mask(text, reconstructed, "EMAIL")

        # 2. Masquage standards
        matches = set(self.re_email_std.findall(text))
        for email in matches:
            text = self._register_and_mask(text, email, "EMAIL")
            
        return text

    def mask_phones(self, text: str) -> str:
        """Masque les téléphones français."""
        matches = set(self.re_phone.findall(text))
        for phone in matches:
            clean_phone = phone.strip()
            if len(clean_phone) >= 10:
                text = self._register_and_mask(text, clean_phone, "PHONE")
        return text

    def mask_links(self, text: str) -> str:
        """Masque les liens web."""
        all_matches = []
        for pattern in [self.re_url_full, self.re_url_simple, self.re_url_www]:
            all_matches.extend(pattern.findall(text))
        
        # Traiter les plus longs en premier
        all_matches = sorted(set(all_matches), key=len, reverse=True)
        for url in all_matches:
            text = self._register_and_mask(text, url, "LINK")
        return text

    def mask_names(self, text: str) -> str:
        """Détecte et masque les noms : les deux premiers mots en majuscules."""
        # Trouver les deux premiers mots entièrement en majuscules (au moins 2 caractères)
        words = text.split()
        name_parts = []
        
        for word in words:
            # Nettoyer le mot (enlever la ponctuation)
            clean_word = re.sub(r'[^\wÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞ]', '', word)
            
            # Vérifier si le mot est entièrement en majuscules et fait au moins 2 caractères
            if len(clean_word) >= 2 and clean_word.isupper() and clean_word.isalpha():
                name_parts.append(clean_word)
                if len(name_parts) >= 2:
                    break
        
        # Si on a trouvé au moins 2 mots en majuscules, créer le nom
        if len(name_parts) >= 2:
            name = f"{name_parts[0]} {name_parts[1]}"
            
            # Ne pas masquer si le nom est déjà un token
            if not any(token in name for token in self.pii_mapping):
                token = self._get_token("NAME")
                self.pii_mapping[token] = name
                
                # Remplacement exact (insensible à la casse)
                text = re.sub(re.escape(name), token, text, flags=re.IGNORECASE)
                
                # Remplacement des parties individuelles
                text = re.sub(re.escape(name_parts[0]), token, text, flags=re.IGNORECASE)
                text = re.sub(re.escape(name_parts[1]), token, text, flags=re.IGNORECASE)

        return text

    def process(self, text: str) -> Tuple[str, Dict[str, str]]:
        """Pipeline complet."""
        # L'ordre est crucial : Noms -> Liens -> Emails -> Téléphones
        text = self.mask_names(text)
        text = self.mask_links(text)
        text = self.mask_emails(text)
        text = self.mask_phones(text)
        return text, self.pii_mapping

def main():
    parser = argparse.ArgumentParser(description="Script d'anonymisation RGPD pour fichiers texte.")
    parser.add_argument("input_file", type=Path, help="Chemin du fichier texte à traiter")
    args = parser.parse_args()

    if not args.input_file.exists():
        logger.error(f"ERREUR: Le fichier {args.input_file} n'existe pas.")
        sys.exit(1)

    logger.info(f"🔒 Lecture du fichier : {args.input_file}")
    
    try:
        raw_text = args.input_file.read_text(encoding='utf-8')
    except Exception as e:
        logger.error(f"ERREUR lecture : {e}")
        sys.exit(1)

    anonymizer = DataAnonymizer()
    safe_text, mapping_keys = anonymizer.process(raw_text)

    # Chemins de sortie
    output_text_path = args.input_file.parent / f"{args.input_file.stem}_anonymized.txt"
    output_keys_path = args.input_file.parent / f"{args.input_file.stem}_keys.json"

    try:
        output_text_path.write_text(safe_text, encoding='utf-8')
        with open(output_keys_path, 'w', encoding='utf-8') as f:
            json.dump(mapping_keys, f, indent=4, ensure_ascii=False)
    except Exception as e:
        logger.error(f"ERREUR écriture : {e}")
        sys.exit(1)

    print("-" * 50)
    logger.info("✅ Anonymisation terminée.")
    logger.info(f"📄 Texte IA :   {output_text_path}")
    logger.info(f"🔑 Clés :       {output_keys_path}")
    print("-" * 50)

    if mapping_keys:
        print(f"Aperçu ({len(mapping_keys)} trouvés) :")
        for k, v in list(mapping_keys.items())[:3]:
            print(f"  {k} -> {v}")
    else:
        print("Aucune donnée sensible trouvée.")

if __name__ == "__main__":
    main()