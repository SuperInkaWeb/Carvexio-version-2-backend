package com.saas.automotriz.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saas.automotriz.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.HtmlUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${admin.notification.email}")
    private String adminNotificationEmail;

    private final RestTemplate restTemplate = new RestTemplate();

    private void sendEmail(String to, String subject, String htmlContent) {
        sendEmail(to, subject, htmlContent, null, null);
    }

    private void sendEmail(String to, String subject, String htmlContent, String filename, byte[] attachment) {
        try {
            String url = "https://api.resend.com/emails";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + resendApiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("from", "Carvexio <hola@carvexio.com>");
            body.put("to", new String[] { to });
            body.put("subject", subject);
            body.put("html", htmlContent);

            if (attachment != null && attachment.length > 0 && filename != null && !filename.isBlank()) {
                Map<String, Object> attachmentBody = new HashMap<>();
                attachmentBody.put("filename", filename);
                attachmentBody.put("content", Base64.getEncoder().encodeToString(attachment));
                body.put("attachments", List.of(attachmentBody));
            }

            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(body);

            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            log.info("Email enviado correctamente con Resend");

        } catch (Exception e) {
            log.error("Error enviando email con Resend", e);
            throw new RuntimeException("Error enviando email con Resend", e);
        }
    }

    @Async
    @Override
    public void sendWelcomeEmail(String to, String name) {
        name = HtmlUtils.htmlEscape(name == null ? "" : name);
        String html = """
                    <div style='font-family: Arial, sans-serif;'>
                        <h1 style='color:#1E3A8A;'>Â¡Hola, %s!</h1>
                        <p>Tu cuenta en <strong>Carvexio</strong> fue creada exitosamente.</p>
                        <a href='https://www.carvexio.com/login'
                           style='background:#1E3A8A;color:white;padding:10px 20px;
                                  text-decoration:none;border-radius:5px;'>
                           Ir a la plataforma
                        </a>
                        <br/><br/>
                        <p>El equipo de Carvexio</p>
                    </div>
                """.formatted(name);

        sendEmail(to, "Â¡Bienvenido a Carvexio!", html);
    }

    @Async
    @Override
    public void sendPasswordResetEmail(String to, String token) {
        String resetUrl = "https://www.carvexio.com/reset-password?token=" + token;

        String html = """
                    <div style='font-family: Arial;'>
                        <h2 style='color:#1E3A8A;'>Recupera tu contraseÃ±a</h2>
                        <p>Haz clic en el botÃ³n para restablecer tu contraseÃ±a:</p>
                        <a href='%s'
                           style='background:#1E3A8A;color:white;padding:10px 20px;
                                  text-decoration:none;border-radius:5px;'>
                           Restablecer contraseÃ±a
                        </a>
                        <p>Si no solicitaste esto, ignora este correo.</p>
                    </div>
                """.formatted(resetUrl);

        sendEmail(to, "RecuperaciÃ³n de contraseÃ±a - Carvexio", html);
    }

    @Override
    public void sendSupportEmail(String fromName, String fromEmail, String subject, String messageText) {
        fromName = HtmlUtils.htmlEscape(fromName == null ? "" : fromName);
        fromEmail = HtmlUtils.htmlEscape(fromEmail == null ? "" : fromEmail);
        messageText = HtmlUtils.htmlEscape(messageText == null ? "" : messageText);
        subject = subject == null ? "" : subject.replaceAll("[\\r\\n]", " ").trim();
        String html = """
                    <div style='font-family: Arial;'>
                        <h2>Nuevo mensaje de soporte</h2>
                        <p><b>Nombre:</b> %s</p>
                        <p><b>Email:</b> %s</p>
                        <p><b>Mensaje:</b></p>
                        <p>%s</p>
                    </div>
                """.formatted(fromName, fromEmail, messageText);

        sendEmail("hola@carvexio.com", "Soporte Carvexio - " + subject, html);
    }

    @Async
    @Override
    public void sendUserConfirmationEmail(String to, String name) {
        name = HtmlUtils.htmlEscape(name == null ? "" : name);
        String html = """
                    <div style='font-family: Arial;'>
                        <h2>Hola, %s</h2>
                        <p>Hemos recibido tu mensaje.</p>
                        <p>Nuestro equipo te responderÃ¡ pronto.</p>
                        <br/>
                        <p>Gracias por confiar en Carvexio.</p>
                    </div>
                """.formatted(name);

        sendEmail(to, "Recibimos tu mensaje - Carvexio", html);
    }

    @Async
    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        sendEmail(to, subject, htmlContent);
    }

    @Override
    public void sendHtmlEmailWithAttachment(String to, String subject, String htmlContent, String filename,
            byte[] attachment) {
        sendEmail(to, subject, htmlContent, filename, attachment);
    }

    @Async
    @Override
    public void sendBusinessPendingApprovalEmail(String to, String businessName, String ownerName) {
        businessName = HtmlUtils.htmlEscape(businessName == null ? "" : businessName);
        ownerName = HtmlUtils.htmlEscape(ownerName == null ? "" : ownerName);
        String html = """
                    <div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 16px;'>
                        <div style='text-align: center; margin-bottom: 20px;'>
                            <span style='font-size: 24px; font-weight: bold; color: #f97316;'>Carvexio</span>
                        </div>
                        <h2 style='color:#1E3A8A;'>Hola</h2>
                        <p>Tu solicitud de registro para el negocio <strong>%s</strong> ha sido recibida con Ã©xito.</p>
                        <p>Actualmente se encuentra en estado: <strong style='color: #d97706;'>Pendiente de AprobaciÃ³n</strong>.</p>
                        <p>Nuestro equipo administrativo revisarÃ¡ los detalles y te notificarÃ¡ por este medio una vez que tu cuenta sea aprobada. Este proceso suele tardar menos de 24 horas.</p>
                        <br/>
                        <p>Gracias por confiar en Carvexio.</p>
                        <hr style='border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;'/>
                        <p style='font-size: 11px; color: #64748b; text-align: center;'>Este es un correo automÃ¡tico, por favor no respondas directamente.</p>
                    </div>
                """
                .formatted(businessName);

        sendEmail(to, "Registro de negocio recibido - Pendiente de AprobaciÃ³n", html);
    }

    @Async
    @Override
    public void sendNewBusinessRegistrationNoticeToAdmin(String businessName, String ownerEmail, String ownerName) {
        businessName = HtmlUtils.htmlEscape(businessName == null ? "" : businessName);
        ownerEmail = HtmlUtils.htmlEscape(ownerEmail == null ? "" : ownerEmail);
        ownerName = HtmlUtils.htmlEscape(ownerName == null ? "" : ownerName);
        String html = """
                    <div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 16px;'>
                        <h2 style='color:#1E3A8A;'>Nuevo Negocio Registrado</h2>
                        <p>Se ha registrado un nuevo negocio en la plataforma que requiere revisiÃ³n y aprobaciÃ³n:</p>
                        <table style='width: 100%%; border-collapse: collapse; margin: 20px 0;'>
                            <tr>
                                <td style='padding: 8px; border-bottom: 1px solid #e2e8f0; font-weight: bold; width: 150px;'>Negocio:</td>
                                <td style='padding: 8px; border-bottom: 1px solid #e2e8f0;'>%s</td>
                            </tr>
                            <tr>
                                <td style='padding: 8px; border-bottom: 1px solid #e2e8f0; font-weight: bold;'>Representante:</td>
                                <td style='padding: 8px; border-bottom: 1px solid #e2e8f0;'>%s</td>
                            </tr>
                            <tr>
                                <td style='padding: 8px; border-bottom: 1px solid #e2e8f0; font-weight: bold;'>Email de contacto:</td>
                                <td style='padding: 8px; border-bottom: 1px solid #e2e8f0;'>%s</td>
                            </tr>
                        </table>
                        <p>Por favor, ingresa al panel de administraciÃ³n para evaluarlo.</p>
                        <a href='https://www.carvexio.com/admin/negocios'
                           style='display: inline-block; background:#1E3A8A; color:white; padding:10px 20px;
                                  text-decoration:none; border-radius:5px; font-weight: bold; margin-top: 10px;'>
                           Ir a ModeraciÃ³n de Negocios
                        </a>
                    </div>
                """
                .formatted(businessName, ownerName, ownerEmail);

        sendEmail(adminNotificationEmail, "Alerta: Nuevo negocio pendiente de aprobaciÃ³n - Carvexio",
                html);
    }

    @Async
    @Override
    public void sendBusinessApprovedEmail(String to, String businessName, String ownerName) {
        businessName = HtmlUtils.htmlEscape(businessName == null ? "" : businessName);
        ownerName = HtmlUtils.htmlEscape(ownerName == null ? "" : ownerName);
        String html = """
                    <div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 16px;'>
                        <div style='text-align: center; margin-bottom: 20px;'>
                            <span style='font-size: 24px; font-weight: bold; color: #f97316;'>Carvexio</span>
                        </div>
                        <h2 style='color:#10b981;'>Â¡Felicidades, %s!</h2>
                        <p>Nos complace informarte que tu negocio <strong>%s</strong> ha sido <strong style='color:#10b981;'>Aprobado</strong> por el administrador.</p>
                        <p>A partir de este momento:</p>
                        <ul>
                            <li>Tu negocio es visible pÃºblicamente en nuestro Marketplace.</li>
                            <li>Tienes acceso a todas las funcionalidades del panel de administraciÃ³n (gestiÃ³n de citas, inventarios, locales y reportes).</li>
                        </ul>
                        <p>Haz clic en el siguiente enlace para acceder a tu panel:</p>
                        <a href='https://www.carvexio.com/empresa'
                           style='display: inline-block; background:#f97316; color:white; padding:12px 24px;
                                  text-decoration:none; border-radius:8px; font-weight: bold; margin-top: 15px;'>
                           Ingresar a mi Taller
                        </a>
                        <br/><br/>
                        <p>Muchos Ã©xitos en tus ventas,</p>
                        <p>El equipo de Carvexio</p>
                    </div>
                """
                .formatted(ownerName, businessName);

        sendEmail(to, "Â¡Tu negocio ha sido Aprobado! - Carvexio", html);
    }

    @Async
    @Override
    public void sendBusinessRejectedEmail(String to, String businessName, String ownerName) {
        businessName = HtmlUtils.htmlEscape(businessName == null ? "" : businessName);
        ownerName = HtmlUtils.htmlEscape(ownerName == null ? "" : ownerName);
        String html = """
                    <div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 16px;'>
                        <div style='text-align: center; margin-bottom: 20px;'>
                            <span style='font-size: 24px; font-weight: bold; color: #f97316;'>Carvexio</span>
                        </div>
                        <h2 style='color:#ef4444;'>Hola, %s</h2>
                        <p>Lamentamos informarte que tu registro de negocio <strong>%s</strong> ha sido <strong style='color:#ef4444;'>Rechazado</strong> por el administrador de la plataforma.</p>
                        <p>Si consideras que ha sido un error o deseas recibir mÃ¡s informaciÃ³n sobre los motivos, por favor responde a este correo electrÃ³nico.</p>
                        <br/>
                        <p>Atentamente,</p>
                        <p>Soporte de Carvexio</p>
                    </div>
                """
                .formatted(ownerName, businessName);

        sendEmail(to, "ActualizaciÃ³n sobre tu registro de negocio - Carvexio", html);
    }
}













