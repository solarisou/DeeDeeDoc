package com.dds.gestioncandidatures.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.dds.gestioncandidatures.service.CandidatIdGeneratorService;
import com.dds.gestioncandidatures.service.DebugService;

@Controller
@RequestMapping("/admin")
public class AdminController {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private CandidatIdGeneratorService candidatIdGeneratorService;
    
    @Autowired
    private DebugService debugService;
    
    @Value("${app.upload.dir}")
    private String uploadDir;
    
    @Value("${spring.datasource.url}")
    private String datasourceUrl;
    
    @Value("${spring.datasource.username}")
    private String datasourceUsername;
    
    @Value("${spring.datasource.password}")
    private String datasourcePassword;
    
    @GetMapping("/database")
    public String afficherPageAdmin(Model model) {
        try {
            // Vérifier si les tables existent en essayant de compter les entreprises
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM entreprises", Integer.class);
            
            // Si on arrive ici, les tables existent, on peut compter
            Integer nbEntreprises = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM entreprises", Integer.class);
            Integer nbCandidats = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM candidats", Integer.class);
            Integer nbFichiers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM fichiers_candidat", Integer.class);
            Integer nbDiplomes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM diplomes", Integer.class);
            Integer nbExperiences = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM experiences", Integer.class);
            Integer nbCompetences = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM competences", Integer.class);
            Integer nbSoftSkills = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM soft_skills", Integer.class);
            Integer nbPermis = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM permis_conduire", Integer.class);
            Integer nbLangues = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM langues", Integer.class);
            Integer nbPostes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM postes_disponibles", Integer.class);
            Integer nbMatching = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM matching_candidat_poste", Integer.class);
            Integer nbTransferts = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transferts_candidatures", Integer.class);
            Integer nbReponses = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reponses_candidatures", Integer.class);
            Integer nbRenouvellements = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM renouvellements_autorisation", Integer.class);
            Integer nbLogs = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM logs_activite", Integer.class);
            
            // Compter les fichiers dans le dossier uploads
            long nbFichiersUploads = compterFichiersUploads();
            
            model.addAttribute("nbEntreprises", nbEntreprises);
            model.addAttribute("nbCandidats", nbCandidats);
            model.addAttribute("nbFichiers", nbFichiers);
            model.addAttribute("nbDiplomes", nbDiplomes);
            model.addAttribute("nbExperiences", nbExperiences);
            model.addAttribute("nbCompetences", nbCompetences);
            model.addAttribute("nbSoftSkills", nbSoftSkills);
            model.addAttribute("nbPermis", nbPermis);
            model.addAttribute("nbLangues", nbLangues);
            model.addAttribute("nbPostes", nbPostes);
            model.addAttribute("nbMatching", nbMatching);
            model.addAttribute("nbTransferts", nbTransferts);
            model.addAttribute("nbReponses", nbReponses);
            model.addAttribute("nbRenouvellements", nbRenouvellements);
            model.addAttribute("nbLogs", nbLogs);
            model.addAttribute("nbFichiersUploads", nbFichiersUploads);
            model.addAttribute("tablesExistent", true);
            
            // État du mode debug
            model.addAttribute("debugMode", debugService.isDebugMode());
            
        } catch (Exception e) {
            // Les tables n'existent pas ou autre erreur
            model.addAttribute("tablesExistent", false);
            model.addAttribute("nbFichiersUploads", compterFichiersUploads());
            
            // Message d'avertissement au lieu d'erreur
            model.addAttribute("warning", 
                "ATTENTION : Les tables de la base de données n'existent pas ! " +
                "Cliquez sur 'RÉINITIALISER LA BASE (TOUT EN 1)' pour créer les tables et charger les données.");
            
            // Mettre tous les compteurs à 0
            model.addAttribute("nbEntreprises", 0);
            model.addAttribute("nbCandidats", 0);
            model.addAttribute("nbFichiers", 0);
            model.addAttribute("nbDiplomes", 0);
            model.addAttribute("nbExperiences", 0);
            model.addAttribute("nbCompetences", 0);
            model.addAttribute("nbSoftSkills", 0);
            model.addAttribute("nbPermis", 0);
            model.addAttribute("nbLangues", 0);
            model.addAttribute("nbPostes", 0);
            model.addAttribute("nbMatching", 0);
            model.addAttribute("nbTransferts", 0);
            model.addAttribute("nbReponses", 0);
            model.addAttribute("nbRenouvellements", 0);
            model.addAttribute("nbLogs", 0);
        }
        
        return "admin/database";
    }
    
    private long compterFichiersUploads() {
        try {
            Path uploadsPath = Paths.get(uploadDir);
            if (!Files.exists(uploadsPath)) {
                return 0;
            }
            try (Stream<Path> files = Files.list(uploadsPath)) {
                return files.filter(Files::isRegularFile).count();
            }
        } catch (IOException e) {
            return 0;
        }
    }
    
    private int supprimerTousFichiersUploads() {
        int count = 0;
        try {
            Path uploadsPath = Paths.get(uploadDir);
            if (Files.exists(uploadsPath)) {
                try (Stream<Path> files = Files.list(uploadsPath)) {
                    files.filter(Files::isRegularFile).forEach(file -> {
                        try {
                            Files.delete(file);
                        } catch (IOException e) {
                            System.err.println("Erreur suppression: " + file.getFileName());
                        }
                    });
                }
                count = (int) compterFichiersUploads();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la suppression des fichiers uploads: " + e.getMessage());
        }
        return count;
    }
    
    @PostMapping("/truncate-all")
    public String viderToutesLesTables(@RequestParam(value = "deleteFiles", defaultValue = "true") boolean deleteFiles,
                                      RedirectAttributes redirectAttributes) {
        try {
            int nbFichiersSupprimés = 0;
            
            // Supprimer les fichiers uploads si demandé
            if (deleteFiles) {
                long nbAvant = compterFichiersUploads();
                supprimerTousFichiersUploads();
                long nbApres = compterFichiersUploads();
                nbFichiersSupprimés = (int)(nbAvant - nbApres);
            }
            
            // Désactiver les contraintes de clés étrangères temporairement
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
            
            // Vider toutes les tables dans l'ordre
            jdbcTemplate.execute("TRUNCATE TABLE logs_activite");
            jdbcTemplate.execute("TRUNCATE TABLE renouvellements_autorisation");
            jdbcTemplate.execute("TRUNCATE TABLE reponses_candidatures");
            jdbcTemplate.execute("TRUNCATE TABLE transferts_candidatures");
            jdbcTemplate.execute("TRUNCATE TABLE matching_candidat_poste");
            jdbcTemplate.execute("TRUNCATE TABLE postes_disponibles");
            jdbcTemplate.execute("TRUNCATE TABLE langues");
            jdbcTemplate.execute("TRUNCATE TABLE permis_conduire");
            jdbcTemplate.execute("TRUNCATE TABLE soft_skills");
            jdbcTemplate.execute("TRUNCATE TABLE competences");
            jdbcTemplate.execute("TRUNCATE TABLE experiences");
            jdbcTemplate.execute("TRUNCATE TABLE diplomes");
            jdbcTemplate.execute("TRUNCATE TABLE fichiers_candidat");
            jdbcTemplate.execute("TRUNCATE TABLE candidats");
            jdbcTemplate.execute("TRUNCATE TABLE entreprises");
            
            // Réactiver les contraintes
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
            
            // Réinitialiser le compteur d'ID candidats
            candidatIdGeneratorService.reinitialiserCompteur();
            
            String message = "Toutes les tables ont été vidées avec succès ! Le compteur d'ID a été réinitialisé.";
            if (deleteFiles && nbFichiersSupprimés > 0) {
                message += " (" + nbFichiersSupprimés + " fichiers supprimés dans uploads)";
            }
            redirectAttributes.addFlashAttribute("success", message);
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors du vidage des tables : " + e.getMessage());
        }
        
        return "redirect:/admin/database";
    }
    
    @PostMapping("/truncate-table")
    public String viderUneTable(@RequestParam("tableName") String tableName, 
                               RedirectAttributes redirectAttributes) {
        try {
            // Liste blanche des tables autorisées
            String[] tablesAutorisees = {
                "logs_activite", "renouvellements_autorisation", "reponses_candidatures",
                "transferts_candidatures", "matching_candidat_poste", "postes_disponibles",
                "langues", "permis_conduire", "soft_skills", "competences", "experiences",
                "diplomes", "fichiers_candidat", "candidats", "entreprises"
            };
            
            boolean tableValide = false;
            for (String table : tablesAutorisees) {
                if (table.equals(tableName)) {
                    tableValide = true;
                    break;
                }
            }
            
            if (!tableValide) {
                redirectAttributes.addFlashAttribute("error", "Nom de table non autorisé : " + tableName);
                return "redirect:/admin/database";
            }
            
            // Désactiver les contraintes temporairement
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
            jdbcTemplate.execute("TRUNCATE TABLE " + tableName);
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
            
            redirectAttributes.addFlashAttribute("success", "Table '" + tableName + "' vidée avec succès !");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors du vidage de la table : " + e.getMessage());
        }
        
        return "redirect:/admin/database";
    }
    
    @PostMapping("/drop-all")
    public String supprimerToutesLesTables(RedirectAttributes redirectAttributes) {
        try {
            // Désactiver les contraintes de clés étrangères
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
            
            // Supprimer toutes les tables dans l'ordre inverse de dépendance
            jdbcTemplate.execute("DROP TABLE IF EXISTS logs_activite");
            jdbcTemplate.execute("DROP TABLE IF EXISTS renouvellements_autorisation");
            jdbcTemplate.execute("DROP TABLE IF EXISTS reponses_candidatures");
            jdbcTemplate.execute("DROP TABLE IF EXISTS transferts_candidatures");
            jdbcTemplate.execute("DROP TABLE IF EXISTS matching_candidat_poste");
            jdbcTemplate.execute("DROP TABLE IF EXISTS postes_disponibles");
            jdbcTemplate.execute("DROP TABLE IF EXISTS langues");
            jdbcTemplate.execute("DROP TABLE IF EXISTS permis_conduire");
            jdbcTemplate.execute("DROP TABLE IF EXISTS soft_skills");
            jdbcTemplate.execute("DROP TABLE IF EXISTS competences");
            jdbcTemplate.execute("DROP TABLE IF EXISTS experiences");
            jdbcTemplate.execute("DROP TABLE IF EXISTS diplomes");
            jdbcTemplate.execute("DROP TABLE IF EXISTS fichiers_candidat");
            jdbcTemplate.execute("DROP TABLE IF EXISTS candidats");
            jdbcTemplate.execute("DROP TABLE IF EXISTS entreprises");
            
            // Réactiver les contraintes
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
            
            redirectAttributes.addFlashAttribute("success", "Toutes les tables ont été supprimées ! Vous devez recréer le schéma.");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la suppression des tables : " + e.getMessage());
        }
        
        return "redirect:/admin/database";
    }
    
    @PostMapping("/drop-table")
    public String supprimerUneTable(@RequestParam("tableName") String tableName,
                                   RedirectAttributes redirectAttributes) {
        try {
            // Liste blanche des tables autorisées
            String[] tablesAutorisees = {
                "logs_activite", "renouvellements_autorisation", "reponses_candidatures",
                "transferts_candidatures", "matching_candidat_poste", "postes_disponibles",
                "langues", "permis_conduire", "soft_skills", "competences", "experiences",
                "diplomes", "fichiers_candidat", "candidats", "entreprises"
            };
            
            boolean tableValide = false;
            for (String table : tablesAutorisees) {
                if (table.equals(tableName)) {
                    tableValide = true;
                    break;
                }
            }
            
            if (!tableValide) {
                redirectAttributes.addFlashAttribute("error", "Nom de table non autorisé : " + tableName);
                return "redirect:/admin/database";
            }
            
            // Désactiver les contraintes temporairement
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + tableName);
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
            
            redirectAttributes.addFlashAttribute("success", "Table '" + tableName + "' supprimée avec succès !");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la suppression de la table : " + e.getMessage());
        }
        
        return "redirect:/admin/database";
    }
    
    @PostMapping("/create-schema")
    public String creerSchema(RedirectAttributes redirectAttributes) {
        try {
            // Vérifier si le fichier existe
            File schemaFile = new File("database/database_schema.sql");
            if (!schemaFile.exists()) {
                redirectAttributes.addFlashAttribute("error", 
                    "Fichier database/database_schema.sql non trouvé dans le dossier du projet !");
                return "redirect:/admin/database";
            }
            
            // Lire et exécuter le fichier SQL
            String sqlContent = new String(Files.readAllBytes(schemaFile.toPath()));
            int commandesExecutees = executerFichierSQL(sqlContent);
            
            redirectAttributes.addFlashAttribute("success", 
                "Schéma créé avec succès ! (" + commandesExecutees + " commandes exécutées)");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Erreur lors de la création du schéma : " + e.getMessage());
        }
        
        return "redirect:/admin/database";
    }
    
    @PostMapping("/load-sample-data")
    public String chargerDonneesExemple(RedirectAttributes redirectAttributes) {
        try {
            // Vérifier si le fichier existe
            File sampleFile = new File("database/sample_data.sql");
            if (!sampleFile.exists()) {
                redirectAttributes.addFlashAttribute("error", 
                    "Fichier database/sample_data.sql non trouvé dans le dossier du projet !");
                return "redirect:/admin/database";
            }
            
            // Lire et exécuter le fichier SQL
            String sqlContent = new String(Files.readAllBytes(sampleFile.toPath()));
            int commandesExecutees = executerFichierSQL(sqlContent);
            
            redirectAttributes.addFlashAttribute("success", 
                "Données d'exemple chargées avec succès ! (" + commandesExecutees + " commandes exécutées)");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Erreur lors du chargement des données : " + e.getMessage());
        }
        
        return "redirect:/admin/database";
    }
    
    @PostMapping("/reset-database")
    public String reinitialiserBaseDonnees(RedirectAttributes redirectAttributes) {
        try {
            // 1. Supprimer toutes les tables
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
            jdbcTemplate.execute("DROP TABLE IF EXISTS logs_activite");
            jdbcTemplate.execute("DROP TABLE IF EXISTS renouvellements_autorisation");
            jdbcTemplate.execute("DROP TABLE IF EXISTS reponses_candidatures");
            jdbcTemplate.execute("DROP TABLE IF EXISTS transferts_candidatures");
            jdbcTemplate.execute("DROP TABLE IF EXISTS matching_candidat_poste");
            jdbcTemplate.execute("DROP TABLE IF EXISTS postes_disponibles");
            jdbcTemplate.execute("DROP TABLE IF EXISTS langues");
            jdbcTemplate.execute("DROP TABLE IF EXISTS permis_conduire");
            jdbcTemplate.execute("DROP TABLE IF EXISTS soft_skills");
            jdbcTemplate.execute("DROP TABLE IF EXISTS competences");
            jdbcTemplate.execute("DROP TABLE IF EXISTS experiences");
            jdbcTemplate.execute("DROP TABLE IF EXISTS diplomes");
            jdbcTemplate.execute("DROP TABLE IF EXISTS fichiers_candidat");
            jdbcTemplate.execute("DROP TABLE IF EXISTS candidats");
            jdbcTemplate.execute("DROP TABLE IF EXISTS entreprises");
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
            
            // 2. Supprimer les fichiers uploads
            long nbFichiers = compterFichiersUploads();
            supprimerTousFichiersUploads();
            
            // 3. Recréer le schéma depuis le fichier SQL
            File schemaFile = new File("database/database_schema.sql");
            if (!schemaFile.exists()) {
                redirectAttributes.addFlashAttribute("error", 
                    "Fichier database/database_schema.sql non trouvé !");
                return "redirect:/admin/database";
            }
            
            String schemaContent = new String(Files.readAllBytes(schemaFile.toPath()));
            int nbSchema = executerFichierSQL(schemaContent);
            
            // 4. Charger les données d'exemple
            File sampleFile = new File("database/sample_data.sql");
            if (!sampleFile.exists()) {
                redirectAttributes.addFlashAttribute("warning", 
                    "Schéma créé (" + nbSchema + " commandes) mais database/sample_data.sql non trouvé !");
                return "redirect:/admin/database";
            }
            
            String dataContent = new String(Files.readAllBytes(sampleFile.toPath()));
            int nbData = executerFichierSQL(dataContent);
            
            // Réinitialiser le compteur d'ID candidats
            candidatIdGeneratorService.reinitialiserCompteur();
            
            // Succès complet
            String message = "Base de données réinitialisée avec succès ! " +
                           "(Schéma: " + nbSchema + " commandes, Données: " + nbData + " commandes";
            if (nbFichiers > 0) {
                message += ", " + nbFichiers + " fichiers supprimés";
            }
            message += "). Compteur d'ID réinitialisé.";
            redirectAttributes.addFlashAttribute("success", message);
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Erreur lors de la réinitialisation : " + e.getMessage());
        }
        
        return "redirect:/admin/database";
    }
    
    @PostMapping("/delete-uploads")
    public String supprimerFichiersUploads(RedirectAttributes redirectAttributes) {
        try {
            long nbAvant = compterFichiersUploads();
            supprimerTousFichiersUploads();
            long nbApres = compterFichiersUploads();
            int nbSupprimés = (int)(nbAvant - nbApres);
            
            if (nbSupprimés > 0) {
                redirectAttributes.addFlashAttribute("success", 
                    nbSupprimés + " fichier(s) supprimé(s) du dossier uploads !");
            } else {
                redirectAttributes.addFlashAttribute("success", 
                    "Aucun fichier à supprimer dans le dossier uploads.");
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Erreur lors de la suppression des fichiers : " + e.getMessage());
        }
        
        return "redirect:/admin/database";
    }
    
    @PostMapping("/toggle-debug")
    public String toggleDebugMode(@RequestParam("enabled") boolean enabled, 
                                  RedirectAttributes redirectAttributes) {
        try {
            debugService.setDebugMode(enabled);
            if (enabled) {
                redirectAttributes.addFlashAttribute("success", 
                    "Mode debug activé ! Les fichiers de traitement seront sauvegardés dans uploads/debug/");
            } else {
                redirectAttributes.addFlashAttribute("success", "Mode debug désactivé.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Erreur lors de la modification du mode debug : " + e.getMessage());
        }
        
        return "redirect:/admin/database";
    }
    
    private int executerFichierSQL(String sqlContent) {
        int commandesExecutees = 0;
        
        // Supprimer les commentaires multi-lignes /* ... */
        sqlContent = sqlContent.replaceAll("/\\*.*?\\*/", "");
        
        // Diviser par lignes et reconstruire les commandes complètes
        String[] lines = sqlContent.split("\n");
        StringBuilder currentCommand = new StringBuilder();
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // Ignorer les lignes vides et les commentaires
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("--")) {
                continue;
            }
            
            // Ajouter la ligne à la commande courante
            if (currentCommand.length() > 0) {
                currentCommand.append(" ");
            }
            currentCommand.append(trimmedLine);
            
            // Si la ligne se termine par ;, on a une commande complète
            if (trimmedLine.endsWith(";")) {
                String command = currentCommand.toString();
                // Enlever le point-virgule final
                command = command.substring(0, command.length() - 1).trim();
                
                if (!command.isEmpty()) {
                    try {
                        jdbcTemplate.execute(command);
                        commandesExecutees++;
                    } catch (Exception e) {
                        // Logger mais continuer
                        System.err.println("Erreur SQL (ignorée) : " + e.getMessage());
                        System.err.println("Commande : " + command.substring(0, Math.min(100, command.length())) + "...");
                    }
                }
                
                // Réinitialiser pour la prochaine commande
                currentCommand = new StringBuilder();
            }
        }
        
        return commandesExecutees;
    }
}

