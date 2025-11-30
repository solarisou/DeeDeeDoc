#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script d'extraction de texte (OCR + PDF Natif) pour CV et Lettre de Motivation.
Ce script remplace l'ancien process_cv.py.

Usage:
    python Pdf2Text.py <chemin_cv> [chemin_lm]
"""

import sys
import os
import fitz  # PyMuPDF
import re
import io
from PIL import Image

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

def safe_print(text, file=None):
    """Fonction helper pour imprimer du texte Unicode de manière sûre."""
    try:
        if file is None:
            print(text)
        else:
            print(text, file=file)
    except UnicodeEncodeError:
        # Fallback : encoder avec gestion des erreurs
        encoded = text.encode('utf-8', errors='replace').decode('utf-8')
        if file is None:
            print(encoded)
        else:
            print(encoded, file=file)

# Vérification des dépendances OCR
try:
    import pytesseract
    OCR_AVAILABLE = True
    # Si sous Windows et Tesseract n'est pas dans le PATH, décommente la ligne suivante :
    # pytesseract.pytesseract.tesseract_cmd = r'C:\Program Files\Tesseract-OCR\tesseract.exe'
except ImportError:
    OCR_AVAILABLE = False

class DocumentExtractor:
    """
    Classe chargée d'extraire le texte d'un fichier (PDF ou Image)
    en préservant la structure (colonnes) pour faciliter la lecture par l'IA.
    """
    def __init__(self, filepath):
        self.filepath = filepath
        self.doc = None
        try:
            self.doc = fitz.open(filepath)
        except Exception as e:
            safe_print(f"ERREUR: Impossible d'ouvrir le fichier {filepath}. Erreur: {e}", file=sys.stderr)

    def _clean_text(self, text):
        """Nettoyage basique : suppression des caractères de contrôle et normalisation des espaces."""
        if not text:
            return ""
        # Supprime les caractères non imprimables
        text = re.sub(r'[\x00-\x08\x0B-\x0C\x0E-\x1F]', '', text)
        # Remplace les caractères Unicode problématiques par leurs équivalents ASCII
        # Tirets cadratins et autres tirets Unicode → tiret simple
        text = text.replace('\u2013', '-')  # En dash
        text = text.replace('\u2014', '-')  # Em dash
        text = text.replace('\u2015', '-')  # Horizontal bar
        text = text.replace('\u2018', "'")  # Left single quotation mark
        text = text.replace('\u2019', "'")  # Right single quotation mark
        text = text.replace('\u201C', '"')  # Left double quotation mark
        text = text.replace('\u201D', '"')  # Right double quotation mark
        # Normalise les espaces
        text = re.sub(r'[ \t]+', ' ', text)
        return text.strip()

    def _extract_ocr(self, page):
        """Extraction via Tesseract (OCR) si le PDF est une image."""
        if not OCR_AVAILABLE:
            return "[NOTE: Texte non extractible et OCR non installé sur le serveur]"
        
        # Zoom x2 pour améliorer la précision
        pix = page.get_pixmap(matrix=fitz.Matrix(2, 2))
        img_data = pix.tobytes("png")
        image = Image.open(io.BytesIO(img_data))
        
        try:
            # Langues : Français + Anglais, Mode bloc (psm 6)
            text = pytesseract.image_to_string(image, lang='fra+eng', config='--psm 6')
            return self._clean_text(text)
        except Exception as e:
            return f"[ERREUR OCR: {str(e)}]"

    def _sort_blocks_by_column(self, blocks):
        """
        Reconstruit le texte en respectant les colonnes visuelles.
        Essentiel pour les CV modernes (Skills à gauche, Expérience à droite).
        Amélioré pour capturer aussi les blocs proches des bords.
        """
        if not blocks:
            return ""

        # Filtrer les blocs vides ou trop petits (moins de 3 caractères)
        valid_blocks = []
        for block in blocks:
            if len(block) >= 5:
                text = block[4] if isinstance(block[4], str) else str(block[4])
                if text and len(text.strip()) >= 3:
                    valid_blocks.append(block)
        
        if not valid_blocks:
            return ""
        
        # Algorithme de regroupement par colonnes
        # 1. Trier verticalement (Y) - inclure TOUS les blocs même proches des bords
        valid_blocks.sort(key=lambda b: b[1]) 
        
        columns = []
        for block in valid_blocks:
            bbox = block[:4] # x0, y0, x1, y1
            text = block[4] if isinstance(block[4], str) else str(block[4])
            x_center = (bbox[0] + bbox[2]) / 2
            
            # Augmenter la tolérance pour les colonnes (100px au lieu de 50px)
            # pour mieux gérer les CV avec des marges variables
            matched = False
            for col in columns:
                if abs(x_center - col['center']) < 100:
                    col['blocks'].append(block)
                    col['center'] = (col['center'] * col['count'] + x_center) / (col['count'] + 1)
                    col['count'] += 1
                    matched = True
                    break
            
            if not matched:
                columns.append({'center': x_center, 'blocks': [block], 'count': 1})

        # 2. Trier les colonnes de gauche à droite
        columns.sort(key=lambda c: c['center'])
        
        final_text = []
        for col in columns:
            # 3. Dans chaque colonne, trier de haut en bas
            col['blocks'].sort(key=lambda b: b[1])
            for b in col['blocks']:
                text = b[4] if isinstance(b[4], str) else str(b[4])
                cleaned = self._clean_text(text)
                if cleaned:
                    final_text.append(cleaned)
        
        return "\n".join(final_text)

    def _extract_all_text_simple(self, page):
        """Extraction simple de tout le texte sans tri par colonnes - pour ne rien perdre"""
        try:
            # Méthode la plus simple : extraire tout le texte linéairement
            text = page.get_text()
            if text and len(text.strip()) > 0:
                return self._clean_text(text)
        except Exception as e:
            safe_print(f"[WARNING] Erreur lors de l'extraction simple: {e}", file=sys.stderr)
        return ""
    
    def extract_content(self):
        """Fonction principale d'extraction"""
        if not self.doc:
            return ""

        full_text = []
        
        for page_num, page in enumerate(self.doc):
            # 1. Essai extraction native (texte sélectionnable)
            # Utiliser plusieurs méthodes pour ne rien perdre
            
            # Méthode 1: get_text("blocks") - meilleur pour la structure
            blocks = page.get_text("blocks")
            
            # Méthode 2: get_text() simple - extraction brute de tout le texte (fallback)
            simple_text = self._extract_all_text_simple(page)
            
            # Combiner les méthodes pour ne rien perdre
            page_text = ""
            page_text_structured = ""
            
            if blocks:
                # Utiliser la méthode par blocs pour la structure
                page_text_structured = self._sort_blocks_by_column(blocks)
                page_text = page_text_structured
            
            # Si l'extraction structurée est vide ou beaucoup plus courte que l'extraction simple,
            # utiliser l'extraction simple pour ne rien perdre (notamment les noms en haut de page)
            if simple_text:
                simple_len = len(simple_text.strip())
                structured_len = len(page_text.strip()) if page_text else 0
                
                # Si l'extraction simple contient significativement plus de texte,
                # ou si l'extraction structurée est vide, utiliser la simple
                if structured_len == 0 or (simple_len > structured_len * 1.2 and simple_len > 100):
                    # L'extraction simple est meilleure, mais on peut essayer de combiner
                    if page_text_structured:
                        # Combiner les deux : prendre les lignes uniques de chaque
                        structured_lines = set(page_text_structured.split('\n'))
                        simple_lines = simple_text.split('\n')
                        combined = []
                        for line in simple_lines:
                            line_clean = self._clean_text(line)
                            if line_clean and line_clean not in structured_lines:
                                combined.append(line_clean)
                        if combined:
                            page_text = page_text_structured + "\n" + "\n".join(combined)
                        else:
                            page_text = simple_text
                    else:
                        page_text = simple_text
                elif not page_text:
                    page_text = simple_text
            
            # 2. Fallback OCR si le texte est vide ou trop court (<50 chars)
            if len(page_text.strip()) < 50:
                # sys.stderr permet d'afficher des logs sans polluer la sortie standard (stdout) qui contiendra le texte
                safe_print(f"[INFO] Page {page_num+1} detectee comme image/scan. Tentative OCR...", file=sys.stderr)
                ocr_text = self._extract_ocr(page)
                if ocr_text and len(ocr_text.strip()) > len(page_text.strip()):
                    page_text = ocr_text
            
            full_text.append(page_text)
            
        return "\n\n".join(full_text)

def main():
    if len(sys.argv) < 2:
        print("Usage: python Pdf2Text.py <chemin_cv> [chemin_lm]")
        sys.exit(1)
    
    cv_path = sys.argv[1]
    lm_path = sys.argv[2] if len(sys.argv) > 2 else None
    
    # --- TRAITEMENT DU CV ---
    if os.path.exists(cv_path):
        extractor_cv = DocumentExtractor(cv_path)
        texte_cv = extractor_cv.extract_content()
        
        print("=== DEBUT CONTENU CV ===")
        safe_print(texte_cv)
        print("=== FIN CONTENU CV ===")
    else:
        safe_print(f"ERREUR: Le fichier CV n'existe pas : {cv_path}", file=sys.stderr)

    # --- TRAITEMENT DE LA LM (Optionnel) ---
    if lm_path:
        if os.path.exists(lm_path):
            extractor_lm = DocumentExtractor(lm_path)
            texte_lm = extractor_lm.extract_content()
            
            print("\n=== DEBUT CONTENU LM ===")
            safe_print(texte_lm)
            print("=== FIN CONTENU LM ===")
        else:
             safe_print(f"ERREUR: Le fichier LM n'existe pas : {lm_path}", file=sys.stderr)

if __name__ == "__main__":
    main()