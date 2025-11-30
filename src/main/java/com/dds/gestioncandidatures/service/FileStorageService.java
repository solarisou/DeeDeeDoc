package com.dds.gestioncandidatures.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {
    
    @Value("${app.upload.dir}")
    private String uploadDir;
    
    public String storeFile(MultipartFile file, String candidatId, String type) throws IOException {
        // Créer le dossier s'il n'existe pas
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Nouveau format de nom de fichier: [id_entreprise][id_candidat][type].pdf
        // candidatId est déjà au format: 4 chiffres entreprise + 4 chiffres candidat (ex: 00011234)
        // Exemple: 00011234CV.pdf ou 00011234LM.pdf
        String fileName = candidatId + type + ".pdf";
        Path filePath = uploadPath.resolve(fileName);
        
        // Copier le fichier
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        return fileName;
    }
    
    public void deleteFile(String fileName) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log l'erreur mais ne pas faire planter l'application
            System.err.println("Erreur lors de la suppression du fichier: " + fileName);
        }
    }
    
    public Path getFilePath(String fileName) {
        return Paths.get(uploadDir).resolve(fileName);
    }
}