#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script de traitement des CV et lettres de motivation
Pour le moment, ce script fait juste un print des fichiers reçus
"""

import sys
import os
from pathlib import Path

def main():
    if len(sys.argv) < 2:
        print("Usage: python process_cv.py <chemin_cv> [chemin_lm]")
        sys.exit(1)
    
    cv_path = sys.argv[1]
    lm_path = sys.argv[2] if len(sys.argv) > 2 else None
    
    print("=== FICHIERS RECUS ===")
    
    # Afficher le nom du CV
    cv_name = os.path.basename(cv_path)
    print(f"CV: {cv_name}")
    
    # Afficher le nom de la LM si presente
    if lm_path:
        lm_name = os.path.basename(lm_path)
        print(f"Lettre de motivation: {lm_name}")
    else:
        print("Lettre de motivation: Aucune")
    
    print("=== TRAITEMENT TERMINE ===")

if __name__ == "__main__":
    main()
