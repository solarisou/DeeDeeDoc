package com.dds.gestioncandidatures.service;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DebugService {
    
    @Value("${app.upload.dir}")
    private String uploadDir;
    
    private static final String DEBUG_CONFIG_FILE = "debug.properties";
    
    /**
     * Vérifie si le mode debug est activé
     */
    public boolean isDebugMode() {
        Properties props = loadProperties();
        return "true".equalsIgnoreCase(props.getProperty("debug.mode", "false"));
    }
    
    /**
     * Active ou désactive le mode debug
     */
    public void setDebugMode(boolean enabled) throws IOException {
        Properties props = loadProperties();
        props.setProperty("debug.mode", String.valueOf(enabled));
        saveProperties(props);
    }
    
    /**
     * Charge les propriétés depuis le fichier de configuration
     */
    private Properties loadProperties() {
        Properties props = new Properties();
        File configFile = new File(DEBUG_CONFIG_FILE);
        
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                props.load(reader);
            } catch (IOException e) {
                System.err.println("Erreur lors de la lecture du fichier de configuration debug: " + e.getMessage());
            }
        }
        
        return props;
    }
    
    /**
     * Sauvegarde les propriétés dans le fichier de configuration
     */
    private void saveProperties(Properties props) throws IOException {
        File configFile = new File(DEBUG_CONFIG_FILE);
        try (FileWriter writer = new FileWriter(configFile)) {
            props.store(writer, "Configuration du mode debug");
        }
    }
    
    /**
     * Retourne le chemin du dossier debug pour un candidat donné
     */
    public Path getDebugPath(String candidatId) {
        Path uploadPath = Paths.get(uploadDir);
        return uploadPath.resolve("debug").resolve(candidatId);
    }
    
    /**
     * Crée le dossier debug pour un candidat si le mode debug est activé
     */
    public Path createDebugDirectory(String candidatId) throws IOException {
        if (!isDebugMode()) {
            return null;
        }
        
        Path debugPath = getDebugPath(candidatId);
        java.nio.file.Files.createDirectories(debugPath);
        return debugPath;
    }
}





