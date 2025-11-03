package com.dds.gestioncandidatures.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CvParserService {
    
    @Value("${app.upload.dir}")
    private String uploadDir;
    
    public Map<String, Object> traiterFichiers(String cvFileName, String lmFileName) throws IOException {
        System.out.println("Lancement du traitement Python des fichiers");
        
        Path uploadPath = Paths.get(uploadDir);
        String cvPath = uploadPath.resolve(cvFileName).toString();
        
        ProcessBuilder processBuilder;
        if (lmFileName != null) {
            String lmPath = uploadPath.resolve(lmFileName).toString();
            processBuilder = new ProcessBuilder("python", "scripts/process_cv.py", cvPath, lmPath);
        } else {
            processBuilder = new ProcessBuilder("python", "scripts/process_cv.py", cvPath);
        }
        
        processBuilder.directory(Paths.get(".").toFile());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Process process = processBuilder.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                System.out.println(line);
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                response.put("status", "success");
                response.put("message", "Script Python exécuté avec succès");
                response.put("output", output.toString());
            } else {
                response.put("status", "error");
                response.put("message", "Erreur lors de l'exécution du script Python (code: " + exitCode + ")");
                response.put("output", output.toString());
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            response.put("status", "error");
            response.put("message", "Traitement interrompu: " + e.getMessage());
        } catch (IOException e) {
            response.put("status", "error");
            response.put("message", "Erreur d'exécution du script Python: " + e.getMessage());
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