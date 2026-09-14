package com.saas.automotriz.service;

public interface EmailService {
    void sendWelcomeEmail(String to, String name);
    void sendPasswordResetEmail(String to, String token);
    void sendSupportEmail(String fromName, String fromEmail, String subject, String messageText);
    void sendUserConfirmationEmail(String to, String name);
    void sendHtmlEmail(String to, String subject, String htmlContent);
    void sendHtmlEmailWithAttachment(String to, String subject, String htmlContent, String filename, byte[] attachment);

    void sendBusinessPendingApprovalEmail(String to, String businessName, String ownerName);
    void sendNewBusinessRegistrationNoticeToAdmin(String businessName, String ownerEmail, String ownerName);
    void sendBusinessApprovedEmail(String to, String businessName, String ownerName);
    void sendBusinessRejectedEmail(String to, String businessName, String ownerName);
}
