package com.saas.automotriz.controller;

import com.saas.automotriz.model.Reclamacion;
import com.saas.automotriz.repository.ReclamacionRepository;
import com.saas.automotriz.service.EmailService;
import com.saas.automotriz.service.CloudinaryService;
import com.saas.automotriz.service.ReclamacionPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/reclamaciones")
@RequiredArgsConstructor
public class ReclamacionController {

    private static final DateTimeFormatter MAIL_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final EmailService emailService;
    private final CloudinaryService cloudinaryService;
    private final ReclamacionRepository reclamacionRepository;
    private final ReclamacionPdfService reclamacionPdfService;

    @PostMapping
    public ResponseEntity<String> registerReclamacion(
            @RequestParam("docType") String docType,
            @RequestParam("docNumber") String docNumber,
            @RequestParam("firstName") String firstName,
            @RequestParam(value = "middleName", required = false) String middleName,
            @RequestParam("lastName1") String lastName1,
            @RequestParam(value = "lastName2", required = false) String lastName2,
            @RequestParam("address") String address,
            @RequestParam("department") String department,
            @RequestParam("province") String province,
            @RequestParam("district") String district,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,

            @RequestParam(value = "isMinor", defaultValue = "false") boolean isMinor,
            @RequestParam(value = "guardianDocType", required = false) String guardianDocType,
            @RequestParam(value = "guardianDocNumber", required = false) String guardianDocNumber,
            @RequestParam(value = "guardianFirstName", required = false) String guardianFirstName,
            @RequestParam(value = "guardianMiddleName", required = false) String guardianMiddleName,
            @RequestParam(value = "guardianLastName1", required = false) String guardianLastName1,
            @RequestParam(value = "guardianLastName2", required = false) String guardianLastName2,
            @RequestParam(value = "guardianEmail", required = false) String guardianEmail,
            @RequestParam(value = "guardianPhone", required = false) String guardianPhone,

            @RequestParam("claimType") String claimType,
            @RequestParam("productOrService") String productOrService,
            @RequestParam(value = "orderNumber", required = false) String orderNumber,
            @RequestParam(value = "claimedAmount", required = false) String claimedAmount,
            @RequestParam(value = "productName", required = false) String productName,
            @RequestParam(value = "productDescription", required = false) String productDescription,
            @RequestParam("summary") String summary,
            @RequestParam("details") String details,
            @RequestParam("pedido") String pedido,
            @RequestParam(value = "attachment", required = false) MultipartFile attachment
    ) {
        String fileUrl = uploadAttachment(attachment);

        Reclamacion reclamacion = new Reclamacion();
        reclamacion.setFechaRegistro(LocalDateTime.now());
        reclamacion.setDocType(docType);
        reclamacion.setDocNumber(docNumber);
        reclamacion.setFirstName(firstName);
        reclamacion.setMiddleName(middleName);
        reclamacion.setLastName1(lastName1);
        reclamacion.setLastName2(lastName2);
        reclamacion.setAddress(address);
        reclamacion.setDepartment(department);
        reclamacion.setProvince(province);
        reclamacion.setDistrict(district);
        reclamacion.setEmail(email);
        reclamacion.setPhone(phone);
        reclamacion.setMinor(isMinor);
        reclamacion.setGuardianDocType(guardianDocType);
        reclamacion.setGuardianDocNumber(guardianDocNumber);
        reclamacion.setGuardianFirstName(guardianFirstName);
        reclamacion.setGuardianMiddleName(guardianMiddleName);
        reclamacion.setGuardianLastName1(guardianLastName1);
        reclamacion.setGuardianLastName2(guardianLastName2);
        reclamacion.setGuardianEmail(guardianEmail);
        reclamacion.setGuardianPhone(guardianPhone);
        reclamacion.setClaimType(claimType);
        reclamacion.setProductOrService(productOrService);
        reclamacion.setOrderNumber(orderNumber);
        reclamacion.setClaimedAmount(claimedAmount);
        reclamacion.setProductName(productName);
        reclamacion.setProductDescription(productDescription);
        reclamacion.setSummary(summary);
        reclamacion.setDetails(details);
        reclamacion.setPedido(pedido);
        reclamacion.setArchivoSustentoUrl(fileUrl);

        reclamacion = reclamacionRepository.save(reclamacion);
        reclamacion.setCodigoReclamo(buildClaimCode(reclamacion));
        reclamacion.setPdfFilename("hoja-reclamacion-" + reclamacion.getCodigoReclamo() + ".pdf");
        reclamacion = reclamacionRepository.save(reclamacion);

        byte[] pdfBytes = reclamacionPdfService.generate(reclamacion);
        String htmlBody = buildAdminEmail(reclamacion);
        String userHtmlBody = buildUserEmail(reclamacion, htmlBody);

        emailService.sendHtmlEmailWithAttachment(
                "hola@formex.digital",
                "Nuevo Reclamo Registrado - " + reclamacion.getCodigoReclamo(),
                htmlBody,
                reclamacion.getPdfFilename(),
                pdfBytes
        );

        emailService.sendHtmlEmailWithAttachment(
                email,
                "Recibimos tu reclamo - Codigo: " + reclamacion.getCodigoReclamo(),
                userHtmlBody,
                reclamacion.getPdfFilename(),
                pdfBytes
        );

        return ResponseEntity.ok("Reclamo registrado exitosamente con codigo " + reclamacion.getCodigoReclamo());
    }

    private String uploadAttachment(MultipartFile attachment) {
        if (attachment == null || attachment.isEmpty()) {
            return null;
        }
        return cloudinaryService.uploadDocument(attachment);
    }

    private String buildClaimCode(Reclamacion reclamacion) {
        return "AUT-" + String.format("%06d", reclamacion.getId());
    }

    private String buildAdminEmail(Reclamacion reclamacion) {
        String attachmentHtml = reclamacion.getArchivoSustentoUrl() != null
                ? "<p><b>Archivo de Sustento (Cloudinary):</b> <a href='" + escapeHtml(reclamacion.getArchivoSustentoUrl()) + "' style='color:#1E3A8A;font-weight:bold;'>Ver archivo adjunto</a></p>"
                : "<p><b>Archivo de Sustento:</b> No adjuntado</p>";

        String guardianHtml = reclamacion.isMinor()
                ? "<div style='background:#f9f9f9;padding:12px;border-radius:8px;margin-top:10px;border-left:4px solid #1E3A8A;'>" +
                "<h4>Informacion del Apoderado</h4>" +
                "<p><b>Nombre Apoderado:</b> " + escapeHtml(reclamacion.getGuardianFullName()) + "</p>" +
                "<p><b>Documento:</b> " + escapeHtml(reclamacion.getGuardianDocType()) + " - " + escapeHtml(reclamacion.getGuardianDocNumber()) + "</p>" +
                "<p><b>Email Apoderado:</b> " + escapeHtml(reclamacion.getGuardianEmail()) + "</p>" +
                "<p><b>Telefono Apoderado:</b> " + escapeHtml(reclamacion.getGuardianPhone()) + "</p>" +
                "</div>"
                : "";

        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; color: #333; line-height: 1.6;'>" +
                "<h2 style='color:#1E3A8A;border-bottom:2px solid #1E3A8A;padding-bottom:10px;'>HOJA DE RECLAMACION VIRTUAL N " + escapeHtml(reclamacion.getCodigoReclamo()) + "</h2>" +
                "<p><b>Fecha de Envio:</b> " + reclamacion.getFechaRegistro().format(MAIL_DATE) + "</p>" +

                "<h3>1. Identificacion del Consumidor Reclamante</h3>" +
                "<p><b>Nombre Completo:</b> " + escapeHtml(reclamacion.getFullName()) + "</p>" +
                "<p><b>Documento:</b> " + escapeHtml(reclamacion.getDocType()) + " - " + escapeHtml(reclamacion.getDocNumber()) + "</p>" +
                "<p><b>Email:</b> " + escapeHtml(reclamacion.getEmail()) + "</p>" +
                "<p><b>Telefono:</b> " + escapeHtml(reclamacion.getPhone()) + "</p>" +
                "<p><b>Direccion:</b> " + escapeHtml(reclamacion.getFullAddress()) + "</p>" +
                guardianHtml +

                "<h3>2. Detalle del Bien Contratado</h3>" +
                "<p><b>Tipo de Bien:</b> " + escapeHtml(reclamacion.getProductOrService()) + "</p>" +
                "<p><b>Monto Reclamado:</b> " + escapeHtml(formatAmount(reclamacion.getClaimedAmount())) + "</p>" +
                "<p><b>Codigo de Pedido / Curso:</b> " + escapeHtml(safe(reclamacion.getOrderNumber()) + " " + safe(reclamacion.getProductName())) + "</p>" +
                "<p><b>Descripcion del Bien:</b> " + escapeHtml(reclamacion.getProductDescription()) + "</p>" +

                "<h3>3. Detalle de la Reclamacion</h3>" +
                "<p><b>Tipo:</b> " + escapeHtml(reclamacion.getClaimType()) + "</p>" +
                "<p><b>Resumen / Motivo:</b> " + escapeHtml(reclamacion.getSummary()) + "</p>" +
                "<p><b>Detalles del reclamo:</b> " + escapeHtml(reclamacion.getDetails()) + "</p>" +
                "<p><b>Pedido (Accion solicitada):</b> " + escapeHtml(reclamacion.getPedido()) + "</p>" +

                "<h3>4. Sustentos y Firmas</h3>" +
                attachmentHtml +
                "<p><b>PDF:</b> Se adjunta la hoja de reclamacion generada.</p>" +
                "<p><b>Firma Digital:</b> Aceptada (Declaracion jurada de veracidad)</p>" +
                "</div>";
    }

    private String buildUserEmail(Reclamacion reclamacion, String htmlBody) {
        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; color: #333; line-height: 1.6;'>" +
                "<h2>Hola, " + escapeHtml(reclamacion.getFullName()) + "</h2>" +
                "<p>Hemos recibido tu Hoja de Reclamacion Virtual de manera exitosa.</p>" +
                "<p>Se te ha asignado el codigo correlativo de atencion: <b>" + escapeHtml(reclamacion.getCodigoReclamo()) + "</b></p>" +
                "<p>Conforme a la ley de proteccion al consumidor del Peru, nos comunicaremos contigo brindandote una respuesta formal en un plazo maximo de quince (15) dias habiles.</p>" +
                "<p>Adjuntamos una copia PDF de la hoja registrada.</p>" +
                "<div style='border: 1px solid #eee; padding: 15px; border-radius: 8px;'>" +
                htmlBody +
                "</div>" +
                "<br/><p>Atentamente,<br/>El Equipo de Carvexio</p>" +
                "</div>";
    }

    private String formatAmount(String amount) {
        return safe(amount).isBlank() ? "No especificado" : "S/. " + amount;
    }

    private String escapeHtml(String value) {
        return safe(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
