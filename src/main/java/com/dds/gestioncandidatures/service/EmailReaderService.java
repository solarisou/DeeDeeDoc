package com.dds.gestioncandidatures.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.FlagTerm;

@Service
public class EmailReaderService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailReaderService.class);
    
    @Value("${email.host}")
    private String emailHost;
    
    @Value("${email.port}")
    private String emailPort;
    
    @Value("${email.username}")
    private String emailUsername;
    
    @Value("${email.password}")
    private String emailPassword;
    
    @Value("${email.protocol}")
    private String emailProtocol;
    
    public List<EmailContent> readAllEmails() {
        List<EmailContent> emailContents = new ArrayList<>();
        
        try {
            Properties properties = new Properties();
            properties.put("mail.store.protocol", emailProtocol);
            properties.put("mail.imap.host", emailHost);
            properties.put("mail.imap.port", emailPort);
            properties.put("mail.imap.ssl.enable", "true");
            properties.put("mail.imap.auth", "true");
            properties.put("mail.imap.starttls.enable", "true");
            properties.put("mail.imap.ssl.trust", emailHost);
            
            Session emailSession = Session.getInstance(properties);
            Store store = emailSession.getStore(emailProtocol);
            
            logger.info("Connexion à la boîte mail : {}", emailUsername);
            store.connect(emailHost, emailUsername, emailPassword);
            
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            
            // Récupérer TOUS les messages
            Message[] messages = inbox.getMessages();
            
            logger.info("Nombre total d'emails trouvés : {}", messages.length);
            
            for (Message message : messages) {
                EmailContent emailContent = new EmailContent();
                emailContent.setSubject(message.getSubject());
                emailContent.setFrom(message.getFrom()[0].toString());
                emailContent.setSentDate(message.getSentDate());
                
                // Extraire le contenu de l'email
                String content = extractTextFromMessage(message);
                emailContent.setContent(content);
                
                // Extraire les pièces jointes
                List<AttachmentInfo> attachments = extractAttachments(message);
                emailContent.setAttachments(attachments);
                
                emailContents.add(emailContent);
                
                logger.info("Email lu : {} - De : {}", emailContent.getSubject(), emailContent.getFrom());
            }
            
            inbox.close(false);
            store.close();
            
        } catch (Exception e) {
            logger.error("Erreur lors de la lecture des emails : {}", e.getMessage(), e);
        }
        
        return emailContents;
    }
    
    public List<EmailContent> readUnreadEmails() {
        List<EmailContent> emailContents = new ArrayList<>();
        
        try {
            Properties properties = new Properties();
            properties.put("mail.store.protocol", emailProtocol);
            properties.put("mail.imap.host", emailHost);
            properties.put("mail.imap.port", emailPort);
            properties.put("mail.imap.ssl.enable", "true");
            properties.put("mail.imap.auth", "true");
            properties.put("mail.imap.starttls.enable", "true");
            properties.put("mail.imap.ssl.trust", emailHost);
            
            Session emailSession = Session.getInstance(properties);
            Store store = emailSession.getStore(emailProtocol);
            
            logger.info("Connexion à la boîte mail : {}", emailUsername);
            store.connect(emailHost, emailUsername, emailPassword);
            
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            
            // Récupérer uniquement les messages non lus
            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            
            logger.info("Nombre d'emails non lus trouvés : {}", messages.length);
            
            for (Message message : messages) {
                EmailContent emailContent = new EmailContent();
                emailContent.setSubject(message.getSubject());
                emailContent.setFrom(message.getFrom()[0].toString());
                emailContent.setSentDate(message.getSentDate());
                
                // Extraire le contenu de l'email
                String content = extractTextFromMessage(message);
                emailContent.setContent(content);
                
                // Extraire les pièces jointes
                List<AttachmentInfo> attachments = extractAttachments(message);
                emailContent.setAttachments(attachments);
                
                emailContents.add(emailContent);
                
                logger.info("Email lu : {} - De : {}", emailContent.getSubject(), emailContent.getFrom());
            }
            
            inbox.close(false);
            store.close();
            
        } catch (Exception e) {
            logger.error("Erreur lors de la lecture des emails : {}", e.getMessage(), e);
        }
        
        return emailContents;
    }
    
    public void markEmailsAsRead(int count) {
        try {
            Properties properties = new Properties();
            properties.put("mail.store.protocol", emailProtocol);
            properties.put("mail.imap.host", emailHost);
            properties.put("mail.imap.port", emailPort);
            properties.put("mail.imap.ssl.enable", "true");
            properties.put("mail.imap.auth", "true");
            properties.put("mail.imap.starttls.enable", "true");
            properties.put("mail.imap.ssl.trust", emailHost);
            
            Session emailSession = Session.getInstance(properties);
            Store store = emailSession.getStore(emailProtocol);
            store.connect(emailHost, emailUsername, emailPassword);
            
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);
            
            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            
            for (int i = 0; i < Math.min(count, messages.length); i++) {
                messages[i].setFlag(Flags.Flag.SEEN, true);
            }
            
            inbox.close(true);
            store.close();
            
            logger.info("{} emails marqués comme lus", count);
            
        } catch (Exception e) {
            logger.error("Erreur lors du marquage des emails : {}", e.getMessage(), e);
        }
    }
    
    private String extractTextFromMessage(Message message) throws MessagingException, IOException {
        Object content = message.getContent();
        
        if (content instanceof String) {
            return (String) content;
        } else if (content instanceof MimeMultipart) {
            return extractTextFromMimeMultipart((MimeMultipart) content);
        }
        
        return "";
    }
    
    private String extractTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        StringBuilder result = new StringBuilder();
        int count = mimeMultipart.getCount();
        
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            
            if (bodyPart.isMimeType("text/plain")) {
                result.append(bodyPart.getContent());
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                result.append(html);
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result.append(extractTextFromMimeMultipart((MimeMultipart) bodyPart.getContent()));
            }
        }
        
        return result.toString();
    }
    
    private List<AttachmentInfo> extractAttachments(Message message) throws MessagingException, IOException {
        List<AttachmentInfo> attachments = new ArrayList<>();
        
        Object content = message.getContent();
        if (content instanceof MimeMultipart) {
            MimeMultipart multipart = (MimeMultipart) content;
            
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                
                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                    AttachmentInfo attachmentInfo = new AttachmentInfo();
                    attachmentInfo.setFileName(bodyPart.getFileName());
                    attachmentInfo.setContentType(bodyPart.getContentType());
                    attachmentInfo.setInputStream(bodyPart.getInputStream());
                    
                    attachments.add(attachmentInfo);
                }
            }
        }
        
        return attachments;
    }
    
    public static class EmailContent {
        private String subject;
        private String from;
        private Date sentDate;
        private String content;
        private List<AttachmentInfo> attachments = new ArrayList<>();
        
        // Getters et Setters
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        
        public Date getSentDate() { return sentDate; }
        public void setSentDate(Date sentDate) { this.sentDate = sentDate; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public List<AttachmentInfo> getAttachments() { return attachments; }
        public void setAttachments(List<AttachmentInfo> attachments) { this.attachments = attachments; }
    }
    
    public static class AttachmentInfo {
        private String fileName;
        private String contentType;
        private InputStream inputStream;
        
        // Getters et Setters
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        
        public InputStream getInputStream() { return inputStream; }
        public void setInputStream(InputStream inputStream) { this.inputStream = inputStream; }
    }
}

