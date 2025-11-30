package com.dds.gestioncandidatures.config;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FileUploadConfig implements WebMvcConfigurer {
    
    @Value("${app.upload.dir}")
    private String uploadDir;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Créer le dossier s'il n'existe pas
        File uploadDirectory = new File(uploadDir);
        if (!uploadDirectory.exists()) {
            uploadDirectory.mkdirs();
        }
        
        // Permettre l'accès aux fichiers uploadés
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDirectory.getAbsolutePath() + "/");
    }
}
