package com.dds.gestioncandidatures.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CvParserService {
    
    @Value("${app.upload.dir}")
    private String uploadDir;
    
    @Autowired
    private DebugService debugService;
    
    public Map<String, Object> traiterFichiers(String cvFileName, String lmFileName, String candidatId) throws IOException {
        System.out.println("Lancement du traitement Python des fichiers (Pdf2Text + Anonymise) pour candidat: " + candidatId);
        
        Path uploadPath = Paths.get(uploadDir);
        String cvPath = uploadPath.resolve(cvFileName).toString();
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // ============================================
            // ÉTAPE 1 : Extraction du texte avec Pdf2Text.py
            // ============================================
            System.out.println("Étape 1: Extraction du texte avec Pdf2Text.py");
            
            ProcessBuilder processBuilderExtract;
            if (lmFileName != null) {
                String lmPath = uploadPath.resolve(lmFileName).toString();
                processBuilderExtract = new ProcessBuilder("python", "scripts/Pdf2Text.py", cvPath, lmPath);
            } else {
                processBuilderExtract = new ProcessBuilder("python", "scripts/Pdf2Text.py", cvPath);
            }
            
            processBuilderExtract.directory(Paths.get(".").toFile());
            
            Process processExtract = processBuilderExtract.start();
            
            // Lire la sortie standard (texte extrait)
            BufferedReader readerExtract = new BufferedReader(new InputStreamReader(processExtract.getInputStream()));
            StringBuilder extractedText = new StringBuilder();
            String line;
            while ((line = readerExtract.readLine()) != null) {
                extractedText.append(line).append("\n");
            }
            
            // Lire les erreurs (logs)
            BufferedReader errorReaderExtract = new BufferedReader(new InputStreamReader(processExtract.getErrorStream()));
            StringBuilder errorOutput = new StringBuilder();
            String errorLine;
            while ((errorLine = errorReaderExtract.readLine()) != null) {
                errorOutput.append(errorLine).append("\n");
                System.out.println("[Pdf2Text] " + errorLine);
            }
            
            int exitCodeExtract = processExtract.waitFor();
            
            if (exitCodeExtract != 0) {
                response.put("status", "error");
                response.put("message", "Erreur lors de l'extraction du texte (code: " + exitCodeExtract + ")");
                response.put("output", errorOutput.toString());
                return response;
            }
            
            // Extraire le texte entre les marqueurs
            String fullText = extractedText.toString();
            String texteCV = "";
            String texteLM = "";
            
            int debutCV = fullText.indexOf("=== DEBUT CONTENU CV ===");
            int finCV = fullText.indexOf("=== FIN CONTENU CV ===");
            if (debutCV != -1 && finCV != -1) {
                texteCV = fullText.substring(debutCV + "=== DEBUT CONTENU CV ===".length(), finCV).trim();
            }
            
            if (lmFileName != null) {
                int debutLM = fullText.indexOf("=== DEBUT CONTENU LM ===");
                int finLM = fullText.indexOf("=== FIN CONTENU LM ===");
                if (debutLM != -1 && finLM != -1) {
                    texteLM = fullText.substring(debutLM + "=== DEBUT CONTENU LM ===".length(), finLM).trim();
                }
            }
            
            // Combiner les textes
            String texteComplet = texteCV;
            if (!texteLM.isEmpty()) {
                texteComplet += "\n\n--- LETTRE DE MOTIVATION ---\n" + texteLM;
            }
            
            // Sauvegarder le texte dans un fichier temporaire pour Anonymise.py
            // Utilisation de l'ID de candidature pour le nommage
            Path tempTextFile = uploadPath.resolve(candidatId + "_extracted.txt");
            Files.write(tempTextFile, texteComplet.getBytes("UTF-8"));
            
            System.out.println("Texte extrait sauvegardé dans: " + tempTextFile);
            
            // Mode debug : copier le fichier extrait dans le dossier debug
            if (debugService.isDebugMode()) {
                try {
                    Path debugDir = debugService.createDebugDirectory(candidatId);
                    if (debugDir != null) {
                        Path debugExtractedFile = debugDir.resolve("01_extracted_text.txt");
                        Files.copy(tempTextFile, debugExtractedFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("[DEBUG] Fichier extrait copié dans: " + debugExtractedFile);
                    }
                } catch (Exception e) {
                    System.err.println("[DEBUG] Erreur lors de la copie du fichier extrait: " + e.getMessage());
                }
            }
            
            // ============================================
            // ÉTAPE 2 : Anonymisation avec Anonymise.py
            // ============================================
            System.out.println("Étape 2: Anonymisation avec Anonymise.py");
            
            // Utiliser le chemin absolu pour éviter les problèmes de référence
            String tempTextFileAbsolute = tempTextFile.toAbsolutePath().toString();
            System.out.println("[DEBUG] Chemin du fichier à anonymiser (absolu): " + tempTextFileAbsolute);
            
            ProcessBuilder processBuilderAnonymise = new ProcessBuilder(
                "python", 
                "scripts/Anonymise.py", 
                tempTextFileAbsolute
            );
            
            processBuilderAnonymise.directory(Paths.get(".").toFile());
            
            Process processAnonymise = processBuilderAnonymise.start();
            
            BufferedReader readerAnonymise = new BufferedReader(new InputStreamReader(processAnonymise.getInputStream()));
            StringBuilder anonymiseOutput = new StringBuilder();
            while ((line = readerAnonymise.readLine()) != null) {
                anonymiseOutput.append(line).append("\n");
                System.out.println("[Anonymise] " + line);
            }
            
            BufferedReader errorReaderAnonymise = new BufferedReader(new InputStreamReader(processAnonymise.getErrorStream()));
            while ((errorLine = errorReaderAnonymise.readLine()) != null) {
                System.out.println("[Anonymise ERROR] " + errorLine);
            }
            
            int exitCodeAnonymise = processAnonymise.waitFor();
            
            if (exitCodeAnonymise != 0) {
                response.put("status", "error");
                response.put("message", "Erreur lors de l'anonymisation (code: " + exitCodeAnonymise + ")");
                response.put("output", anonymiseOutput.toString());
                return response;
            }
            
            // Vérifier que les fichiers de sortie ont été créés
            // Utilisation de l'ID de candidature pour le nommage
            Path anonymizedFile = uploadPath.resolve(candidatId + "_extracted_anonymized.txt");
            Path keysFile = uploadPath.resolve(candidatId + "_extracted_keys.json");
            
            boolean anonymizedExists = Files.exists(anonymizedFile);
            boolean keysExists = Files.exists(keysFile);
            
            System.out.println("[DEBUG] Vérification des fichiers anonymisés:");
            System.out.println("  - Fichier anonymisé attendu: " + anonymizedFile);
            System.out.println("  - Existe: " + anonymizedExists);
            System.out.println("  - Fichier de clés attendu: " + keysFile);
            System.out.println("  - Existe: " + keysExists);
            
            // Mode debug : copier les fichiers anonymisés dans le dossier debug
            if (debugService.isDebugMode()) {
                try {
                    Path debugDir = debugService.createDebugDirectory(candidatId);
                    if (debugDir != null) {
                        System.out.println("[DEBUG] Dossier debug créé: " + debugDir);
                        if (anonymizedExists) {
                            Path debugAnonymizedFile = debugDir.resolve("02_anonymized_text.txt");
                            Files.copy(anonymizedFile, debugAnonymizedFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("[DEBUG] ✓ Fichier anonymisé copié dans: " + debugAnonymizedFile);
                        } else {
                            System.err.println("[DEBUG] ✗ Fichier anonymisé non trouvé: " + anonymizedFile);
                        }
                        if (keysExists) {
                            Path debugKeysFile = debugDir.resolve("02_anonymization_keys.json");
                            Files.copy(keysFile, debugKeysFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("[DEBUG] ✓ Fichier de clés copié dans: " + debugKeysFile);
                        } else {
                            System.err.println("[DEBUG] ✗ Fichier de clés non trouvé: " + keysFile);
                        }
                    } else {
                        System.err.println("[DEBUG] ✗ Impossible de créer le dossier debug");
                    }
                } catch (Exception e) {
                    System.err.println("[DEBUG] ✗ Erreur lors de la copie des fichiers anonymisés: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            response.put("status", "success");
            response.put("message", "Traitement Python terminé avec succès (Extraction + Anonymisation)");
            response.put("output", anonymiseOutput.toString());
            response.put("extractedTextLength", texteComplet.length());
            response.put("anonymizedFile", anonymizedExists ? anonymizedFile.getFileName().toString() : null);
            response.put("keysFile", keysExists ? keysFile.getFileName().toString() : null);
            
            System.out.println("Traitement terminé avec succès");
            System.out.println("- Fichier anonymisé: " + (anonymizedExists ? anonymizedFile : "non créé"));
            System.out.println("- Fichier de clés: " + (keysExists ? keysFile : "non créé"));
            
            // ============================================
            // ÉTAPE 3 : Traitement complet avec main.py (IA)
            // ============================================
            if (anonymizedExists) {
                System.out.println("Étape 3: Traitement IA avec main.py");
                
                Path debugDir = null;
                String debugDirPath = null;
                if (debugService.isDebugMode()) {
                    try {
                        debugDir = debugService.createDebugDirectory(candidatId);
                        if (debugDir != null) {
                            debugDirPath = debugDir.toString();
                            System.out.println("[DEBUG] Dossier debug pour main.py: " + debugDirPath);
                        }
                    } catch (Exception e) {
                        System.err.println("[DEBUG] Erreur lors de la création du dossier debug: " + e.getMessage());
                    }
                }
                
                // Construire la commande pour main.py
                // main.py attend: <chemin_cv> [chemin_lm] [candidat_id] [debug_dir]
                java.util.List<String> command = new java.util.ArrayList<>();
                command.add("python");
                command.add("scripts/main.py");
                command.add(cvPath);
                
                if (lmFileName != null) {
                    String lmPath = uploadPath.resolve(lmFileName).toString();
                    command.add(lmPath);
                } else {
                    command.add(""); // Paramètre vide pour lm_path
                }
                
                // Ajouter les paramètres de debug si le mode est activé
                if (debugDirPath != null) {
                    command.add(candidatId);
                    command.add(debugDirPath);
                }
                
                System.out.println("[DEBUG] Commande main.py: " + String.join(" ", command));
                
                ProcessBuilder processBuilderMain = new ProcessBuilder(command);
                processBuilderMain.directory(Paths.get(".").toFile());
                
                Process processMain = processBuilderMain.start();
                
                BufferedReader readerMain = new BufferedReader(new InputStreamReader(processMain.getInputStream()));
                StringBuilder mainOutput = new StringBuilder();
                while ((line = readerMain.readLine()) != null) {
                    mainOutput.append(line).append("\n");
                    System.out.println("[main.py] " + line);
                }
                
                BufferedReader errorReaderMain = new BufferedReader(new InputStreamReader(processMain.getErrorStream()));
                while ((errorLine = errorReaderMain.readLine()) != null) {
                    System.out.println("[main.py ERROR] " + errorLine);
                }
                
                int exitCodeMain = processMain.waitFor();
                
                if (exitCodeMain == 0) {
                    System.out.println("[main.py] ✓ Traitement IA terminé avec succès");
                    // Vérifier que les fichiers debug ont été créés
                    if (debugDirPath != null) {
                        Path requestFile = Paths.get(debugDirPath).resolve("03_ia_request.json");
                        Path responseFile = Paths.get(debugDirPath).resolve("04_ia_response.json");
                        System.out.println("[DEBUG] Vérification des fichiers IA:");
                        System.out.println("  - Requête IA: " + requestFile + " (existe: " + Files.exists(requestFile) + ")");
                        System.out.println("  - Réponse IA: " + responseFile + " (existe: " + Files.exists(responseFile) + ")");
                    }
                } else {
                    System.out.println("[main.py] ✗ Erreur lors du traitement IA (code: " + exitCodeMain + ")");
                }
            } else {
                System.out.println("[main.py] ✗ Fichier anonymisé non trouvé, impossible de lancer main.py");
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            response.put("status", "error");
            response.put("message", "Traitement interrompu: " + e.getMessage());
        } catch (IOException e) {
            response.put("status", "error");
            response.put("message", "Erreur d'exécution du script Python: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }
    
    @Deprecated
    public Map<String, Object> extraireInfosCV(Object cvFile) throws IOException {
        System.out.println("Méthode dépréciée - utiliser traiterFichiers() à la place");
        
        Map<String, Object> response = new HashMap<>();
        response.put("nom", "");
        response.put("prenom", "");
        response.put("email", "");
        response.put("telephone", "");
        response.put("competences", new String[]{});
        response.put("experiences", new Object[]{});
        response.put("diplomes", new Object[]{});
        response.put("langues", new Object[]{});
        response.put("status", "pipeline_a_implementer");
        return response;
    }
}