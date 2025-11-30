package com.dds.gestioncandidatures.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.dds.gestioncandidatures.dto.CandidatureEmailDTO;
import com.dds.gestioncandidatures.entity.Candidat;
import com.dds.gestioncandidatures.entity.Entreprise;
import com.dds.gestioncandidatures.entity.Poste;
import com.dds.gestioncandidatures.repository.PosteRepository;
import com.dds.gestioncandidatures.repository.EntrepriseRepository;
import com.dds.gestioncandidatures.service.CandidatIdGeneratorService;
import com.dds.gestioncandidatures.service.CandidatSaveService;
import com.dds.gestioncandidatures.service.CvParserService;
import com.dds.gestioncandidatures.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Controller
@RequestMapping("/candidat")
public class CandidatController {
    
    public static final String ENTREPRISE_PRINCIPALE_ID = "0002";

    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private CvParserService cvParserService;
    
    @Autowired
    private CandidatIdGeneratorService candidatIdGeneratorService;

    @Autowired
    private PosteRepository posteRepository;

    @Autowired
    private EntrepriseRepository entrepriseRepository;
    
    @Autowired
    private CandidatSaveService candidatSaveService;
    
    @Value("${app.upload.dir}")
    private String uploadDir;
    
    private final ObjectMapper objectMapper;
    
    public CandidatController() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    @GetMapping("/deposer")
    public String afficherFormulaireDepot(@RequestParam(value = "posteId", required = false) Long posteId,
                                          @RequestParam(value = "type", required = false) String typeParam,
                                          Model model) {
        List<Poste> postesOuverts = posteRepository.findByStatutOrderByDateCreationDesc(Poste.Statut.ouvert)
            .stream()
            .filter(poste -> poste.getEntreprise() != null &&
                             ENTREPRISE_PRINCIPALE_ID.equals(poste.getEntreprise().getIdEntreprise()))
            .toList();
        model.addAttribute("postes", postesOuverts);

        Candidat.TypeCandidature typeDefaut = Candidat.TypeCandidature.SPONTANEE;
        if (typeParam != null) {
            try {
                typeDefaut = Candidat.TypeCandidature.valueOf(typeParam.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                typeDefaut = Candidat.TypeCandidature.SPONTANEE;
            }
        }
        model.addAttribute("typeCandidatureDefaut", typeDefaut.name());

        String entrepriseParDefaut = entrepriseRepository.findById(ENTREPRISE_PRINCIPALE_ID)
            .map(Entreprise::getIdEntreprise)
            .orElse(ENTREPRISE_PRINCIPALE_ID);
        model.addAttribute("posteSelectionne", null);
        model.addAttribute("entrepriseParDefaut", entrepriseParDefaut);

        if (posteId != null) {
            posteRepository.findById(posteId)
                .filter(poste -> poste.getEntreprise() != null &&
                                 ENTREPRISE_PRINCIPALE_ID.equals(poste.getEntreprise().getIdEntreprise()))
                .ifPresent(poste -> {
                    model.addAttribute("posteSelectionne", posteId);
                    model.addAttribute("entrepriseParDefaut", poste.getEntreprise().getIdEntreprise());
                    model.addAttribute("typeCandidatureDefaut", Candidat.TypeCandidature.POSTE.name());
                });
        }

        return "candidat/deposer-simple";
    }

    @GetMapping("/postes")
    public String listerPostes(Model model) {
        List<Poste> postes = posteRepository.findByStatutOrderByDateCreationDesc(Poste.Statut.ouvert)
            .stream()
            .filter(poste -> poste.getEntreprise() != null &&
                             ENTREPRISE_PRINCIPALE_ID.equals(poste.getEntreprise().getIdEntreprise()))
            .toList();
        model.addAttribute("postes", postes);
        return "candidat/postes";
    }
    
    @PostMapping("/deposer")
    public String deposerCandidature(@RequestParam("entrepriseId") String entrepriseId,
                                   @RequestParam("cvFile") MultipartFile cvFile,
                                   @RequestParam(value = "lmFile", required = false) MultipartFile lmFile,
                                   @RequestParam("autorisationStocker") String autorisationStocker,
                                   @RequestParam("autorisationDiffuser") String autorisationDiffuser,
                                   @RequestParam(value = "typeCandidature", defaultValue = "SPONTANEE") String typeCandidatureParam,
                                   @RequestParam(value = "posteId", required = false) Long posteId,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {

        List<Poste> postesOuverts = posteRepository.findByStatutOrderByDateCreationDesc(Poste.Statut.ouvert)
            .stream()
            .filter(poste -> poste.getEntreprise() != null &&
                             ENTREPRISE_PRINCIPALE_ID.equals(poste.getEntreprise().getIdEntreprise()))
            .toList();
        model.addAttribute("postes", postesOuverts);
        Candidat.TypeCandidature typeSelectionne = Candidat.TypeCandidature.SPONTANEE;

        try {
            typeSelectionne = Candidat.TypeCandidature.valueOf(typeCandidatureParam.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            typeSelectionne = Candidat.TypeCandidature.SPONTANEE;
        }
        model.addAttribute("typeCandidatureDefaut", typeSelectionne != null ? typeSelectionne.name() : null);
        model.addAttribute("posteSelectionne", posteId);
        entrepriseId = ENTREPRISE_PRINCIPALE_ID;
        model.addAttribute("entrepriseParDefaut", entrepriseId);

        if (cvFile.isEmpty()) {
            model.addAttribute("error", "Le CV est obligatoire");
            return "candidat/deposer-simple";
        }
        
        try {
            Poste posteSelectionne = null;
            if (typeSelectionne == Candidat.TypeCandidature.POSTE) {
                if (posteId == null) {
                    model.addAttribute("error", "Merci de sélectionner un poste avant de déposer votre CV.");
                    return "candidat/deposer-simple";
                }
                posteSelectionne = posteRepository.findById(posteId)
                    .filter(poste -> poste.getEntreprise() != null &&
                                     ENTREPRISE_PRINCIPALE_ID.equals(poste.getEntreprise().getIdEntreprise()))
                    .orElse(null);
                if (posteSelectionne == null) {
                    model.addAttribute("error", "Le poste sélectionné n'est plus disponible.");
                    return "candidat/deposer-simple";
                }
                entrepriseId = posteSelectionne.getEntreprise().getIdEntreprise();
                model.addAttribute("entrepriseParDefaut", entrepriseId);
            } else {
                entrepriseId = ENTREPRISE_PRINCIPALE_ID;
                model.addAttribute("entrepriseParDefaut", entrepriseId);
            }

            // Générer un ID avec incrémentation automatique
            String candidatId = candidatIdGeneratorService.genererNouvelId(entrepriseId);
            
            String cvFileName = fileStorageService.storeFile(cvFile, candidatId, "CV");
            String lmFileName = null;
            
            if (lmFile != null && !lmFile.isEmpty()) {
                lmFileName = fileStorageService.storeFile(lmFile, candidatId, "LM");
            }
            
            Map<String, Object> resultTraitement = cvParserService.traiterFichiers(cvFileName, lmFileName, candidatId);
            
            // Lire le JSON généré par main.py
            CandidatureEmailDTO candidatureDTO = null;
            Map<String, String> anonymizationKeys = null;
            try {
                candidatureDTO = lireJsonGenere(candidatId);
                anonymizationKeys = lireAnonymizationKeys(candidatId);
            } catch (Exception e) {
                System.err.println("Erreur lors de la lecture du JSON: " + e.getMessage());
                e.printStackTrace();
            }
            
            // Créer le candidat avec les informations extraites
            Candidat candidat = new Candidat();
            candidat.setIdCandidature(candidatId);
            candidat.setDateCandidature(LocalDate.now());
            candidat.setTypeCandidature(typeSelectionne);
            
            if (typeSelectionne == Candidat.TypeCandidature.POSTE && posteSelectionne != null) {
                candidat.setPoste(posteSelectionne);
                candidat.setPosteVise(posteSelectionne.getIntitulePoste());
            }
            
            candidat.setAutorisationStocker(Candidat.Autorisation.valueOf(autorisationStocker));
            candidat.setAutorisationDiffuser(Candidat.Autorisation.valueOf(autorisationDiffuser));
            
            // Remplir les informations depuis le JSON si disponible
            if (candidatureDTO != null) {
                if (candidatureDTO.getNom() != null && !candidatureDTO.getNom().isEmpty()) {
                    candidat.setNom(candidatureDTO.getNom());
                }
                if (candidatureDTO.getPrenom() != null && !candidatureDTO.getPrenom().isEmpty()) {
                    candidat.setPrenom(candidatureDTO.getPrenom());
                }
                if (candidatureDTO.getMail() != null && !candidatureDTO.getMail().isEmpty()) {
                    String email = candidatureDTO.getMail().trim();
                    // Remplacer les tokens d'anonymisation par les vraies valeurs
                    if (email.startsWith("<") && email.endsWith(">") && anonymizationKeys != null) {
                        email = anonymizationKeys.getOrDefault(email, "non_extraite_" + candidatId + "@example.com");
                    }
                    candidat.setEmail(email);
                }
                if (candidatureDTO.getTelephone() != null && !candidatureDTO.getTelephone().isEmpty()) {
                    String telephone = candidatureDTO.getTelephone().trim();
                    // Remplacer les tokens d'anonymisation par les vraies valeurs
                    if (telephone.startsWith("<") && telephone.endsWith(">") && anonymizationKeys != null) {
                        telephone = anonymizationKeys.getOrDefault(telephone, "NON_EXTRAIT");
                    }
                    candidat.setTelephone(telephone);
                }
                if (candidatureDTO.getPrenom() != null && !candidatureDTO.getPrenom().isEmpty()) {
                    String prenom = candidatureDTO.getPrenom().trim();
                    // Remplacer les tokens d'anonymisation par les vraies valeurs
                    if (prenom.startsWith("<") && prenom.endsWith(">") && anonymizationKeys != null) {
                        prenom = anonymizationKeys.getOrDefault(prenom, "NON_EXTRAIT");
                    }
                    candidat.setPrenom(prenom);
                }
                if (candidatureDTO.getPosteVise() != null && !candidatureDTO.getPosteVise().isEmpty() && candidat.getPosteVise() == null) {
                    candidat.setPosteVise(candidatureDTO.getPosteVise());
                }
                if (candidatureDTO.getDateDisponibilite() != null && !candidatureDTO.getDateDisponibilite().isEmpty()) {
                    candidat.setDateDisponibilite(parseDate(candidatureDTO.getDateDisponibilite()));
                }
            }
            
            // Si les informations essentielles manquent, utiliser des valeurs par défaut
            if (candidat.getNom() == null || candidat.getNom().isEmpty()) {
                candidat.setNom("NON_EXTRAIT");
            }
            if (candidat.getPrenom() == null || candidat.getPrenom().isEmpty()) {
                candidat.setPrenom("NON_EXTRAIT");
            }
            if (candidat.getEmail() == null || candidat.getEmail().isEmpty() || 
                !candidat.getEmail().contains("@") || candidat.getEmail().startsWith("<")) {
                // Générer un email valide avec l'ID du candidat
                candidat.setEmail("non_extraite_" + candidatId + "@example.com");
            }
            if (candidat.getTelephone() == null || candidat.getTelephone().isEmpty()) {
                candidat.setTelephone("NON_EXTRAIT");
            }
            
            // Validation finale de l'email : s'assurer qu'il est valide
            String email = candidat.getEmail();
            if (email == null || email.isEmpty() || email.startsWith("<") || 
                !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                // Si l'email n'est toujours pas valide, utiliser un format valide
                String safeId = candidatId.replaceAll("[^A-Za-z0-9]", "_");
                candidat.setEmail("non_extraite_" + safeId + "@example.com");
            }
            
            candidat.setStatutCandidature(Candidat.StatutCandidature.en_attente);
            
            // Calculer la date d'expiration des autorisations
            if (candidat.getAutorisationStocker() == Candidat.Autorisation.O) {
                candidat.setDateExpirationAutorisation(LocalDate.now().plusYears(1));
            }
            
            // Vérifier que le poste existe bien si c'est une candidature sur un poste
            if (typeSelectionne == Candidat.TypeCandidature.POSTE && posteSelectionne != null) {
                // Recharger le poste depuis la base pour éviter les problèmes de lazy loading
                Poste posteVerifie = posteRepository.findById(posteSelectionne.getId())
                    .orElseThrow(() -> new RuntimeException("Le poste sélectionné n'existe plus"));
                candidat.setPoste(posteVerifie);
            } else {
                // S'assurer que le poste est null pour les candidatures spontanées
                candidat.setPoste(null);
            }
            
            // Validation finale : vérifier que tous les champs obligatoires sont remplis
            if (candidat.getNom() == null || candidat.getNom().isEmpty() ||
                candidat.getPrenom() == null || candidat.getPrenom().isEmpty() ||
                candidat.getEmail() == null || candidat.getEmail().isEmpty() ||
                candidat.getTelephone() == null || candidat.getTelephone().isEmpty() ||
                candidat.getDateCandidature() == null ||
                candidat.getTypeCandidature() == null ||
                candidat.getAutorisationStocker() == null ||
                candidat.getAutorisationDiffuser() == null) {
                throw new RuntimeException("Des champs obligatoires sont manquants pour le candidat");
            }
            
            System.out.println("Validation du candidat OK:");
            System.out.println("  - ID: " + candidat.getIdCandidature());
            System.out.println("  - Nom: " + candidat.getNom());
            System.out.println("  - Prénom: " + candidat.getPrenom());
            System.out.println("  - Email: " + candidat.getEmail());
            System.out.println("  - Téléphone: " + candidat.getTelephone());
            System.out.println("  - Type: " + candidat.getTypeCandidature());
            System.out.println("  - Poste: " + (candidat.getPoste() != null ? candidat.getPoste().getId() : "null"));
            
            // Sauvegarder le candidat et toutes les données associées dans une transaction
            Long lmSize = (lmFile != null && !lmFile.isEmpty()) ? Long.valueOf(lmFile.getSize()) : Long.valueOf(0L);
            candidatSaveService.sauvegarderCandidatComplet(candidat, candidatureDTO, cvFileName, lmFileName, 
                                                           Long.valueOf(cvFile.getSize()), lmSize);
            
            Map<String, Object> infosAffichage = new HashMap<>();
            infosAffichage.put("cvFileName", cvFile.getOriginalFilename());
            infosAffichage.put("cvSize", cvFile.getSize());
            
            if (lmFile != null && !lmFile.isEmpty()) {
                infosAffichage.put("lmFileName", lmFile.getOriginalFilename());
                infosAffichage.put("lmSize", lmFile.getSize());
            } else {
                infosAffichage.put("lmFileName", null);
                infosAffichage.put("lmSize", null);
            }
            
            infosAffichage.put("traitementStatus", resultTraitement.get("status"));
            infosAffichage.put("message", "Fichiers stockés avec succès. Traitement Python exécuté.");
            infosAffichage.put("output", resultTraitement.get("output"));
            
            model.addAttribute("candidat", candidat);
            model.addAttribute("infosExtraites", infosAffichage);
            model.addAttribute("success", "Candidature enregistrée avec succès !");
            
            return "candidat/validation-infos";
            
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors du traitement des fichiers: " + e.getMessage());
            return "candidat/deposer-simple";
        }
    }
    
    @GetMapping("/confirmation")
    public String afficherConfirmation() {
        return "candidat/confirmation";
    }
    
    // ============================================
    // MÉTHODES HELPER POUR LA SAUVEGARDE
    // ============================================
    
    private CandidatureEmailDTO lireJsonGenere(String candidatId) throws IOException {
        // Chercher le JSON dans le dossier debug d'abord
        Path debugDir = Paths.get(uploadDir, "debug", candidatId);
        Path jsonFile = debugDir.resolve("04_ia_response.json");
        
        if (!Files.exists(jsonFile)) {
            // Chercher à la racine du projet (format: candidature_{nom}_{prenom}.json)
            Path projectRoot = Paths.get(".").toAbsolutePath();
            File[] jsonFiles = projectRoot.toFile().listFiles((dir, name) -> 
                name.startsWith("candidature_") && name.endsWith(".json"));
            
            if (jsonFiles != null && jsonFiles.length > 0) {
                // Prendre le fichier le plus récent
                File latestFile = jsonFiles[0];
                for (File f : jsonFiles) {
                    if (f.lastModified() > latestFile.lastModified()) {
                        latestFile = f;
                    }
                }
                jsonFile = latestFile.toPath();
            } else {
                throw new IOException("Fichier JSON non trouvé pour le candidat: " + candidatId);
            }
        }
        
        return objectMapper.readValue(jsonFile.toFile(), CandidatureEmailDTO.class);
    }
    
    private Map<String, String> lireAnonymizationKeys(String candidatId) {
        try {
            // Chercher le fichier de clés dans le dossier debug d'abord
            Path debugDir = Paths.get(uploadDir, "debug", candidatId);
            Path keysFile = debugDir.resolve("02_anonymization_keys.json");
            
            if (!Files.exists(keysFile)) {
                // Chercher à la racine du dossier uploads
                Path uploadPath = Paths.get(uploadDir);
                keysFile = uploadPath.resolve(candidatId + "_extracted_keys.json");
            }
            
            if (Files.exists(keysFile)) {
                @SuppressWarnings("unchecked")
                Map<String, String> keys = objectMapper.readValue(keysFile.toFile(), Map.class);
                return keys;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture des clés d'anonymisation: " + e.getMessage());
        }
        return null;
    }
    
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Gérer le format "YYYY-MM" (année-mois)
            if (dateStr.matches("\\d{4}-\\d{2}")) {
                String[] parts = dateStr.split("-");
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                return LocalDate.of(year, month, 1); // Premier jour du mois
            }
            
            DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ISO_DATE
            };
            
            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDate.parse(dateStr, formatter);
                } catch (Exception e) {
                    // Continuer avec le prochain format
                }
            }
        } catch (Exception e) {
            System.err.println("Impossible de parser la date : " + dateStr);
        }
        
        return null;
    }
}
