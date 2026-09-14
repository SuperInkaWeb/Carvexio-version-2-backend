package com.saas.automotriz.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.saas.automotriz.model.Reclamacion;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

@Service
public class ReclamacionPdfService {

    private static final String TEMPLATE_PATH = "/templates/Templantes.pdf/libro-reclamaciones-base.pdf";
    private static final String TEMPLATE_PATH_FALLBACK = "/Templates/Templantes.pdf/libro-reclamaciones-base.pdf";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generate(Reclamacion reclamacion) {
        try (InputStream template = openTemplate();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PdfDocument pdf = new PdfDocument(new PdfReader(template), new PdfWriter(baos));
            PdfPage page = pdf.getFirstPage();
            Rectangle pageSize = page.getPageSize();

            write(page, pageSize, 92, 88, 130, 14, DATE_FORMAT.format(reclamacion.getFechaRegistro()), 8, true);
            write(page, pageSize, 402, 85, 125, 14, reclamacion.getCodigoReclamo(), 9, true);

            write(page, pageSize, 132, 166, 455, 14, reclamacion.getFullName(), 8, false);
            write(page, pageSize, 132, 192, 455, 14, reclamacion.getDocType() + " - " + reclamacion.getDocNumber(), 8, false);
            write(page, pageSize, 132, 218, 455, 14, reclamacion.getFullAddress(), 7, false);
            write(page, pageSize, 132, 236, 150, 14, reclamacion.getPhone(), 8, false);
            write(page, pageSize, 405, 236, 180, 14, reclamacion.getEmail(), 7, false);

            if (reclamacion.isMinor()) {
                String guardian = reclamacion.getGuardianFullName() + " / " + safe(reclamacion.getGuardianDocType()) + " " + safe(reclamacion.getGuardianDocNumber());
                write(page, pageSize, 330, 263, 255, 14, guardian, 7, false);
            }

            write(page, pageSize, 168, 312, 28, 14, isSelected(reclamacion.getProductOrService(), "PRODUCTO") ? "X" : "", 10, true);
            write(page, pageSize, 360, 312, 190, 14, money(reclamacion.getClaimedAmount()), 8, false);
            write(page, pageSize, 168, 327, 28, 14, isSelected(reclamacion.getProductOrService(), "SERVICIO") ? "X" : "", 10, true);
            write(page, pageSize, 360, 335, 220, 14, productDescription(reclamacion), 6, false);

            write(page, pageSize, 416, 352, 35, 14, isSelected(reclamacion.getClaimType(), "RECLAMO") ? "X" : "", 10, true);
            write(page, pageSize, 536, 352, 35, 14, isSelected(reclamacion.getClaimType(), "QUEJA") ? "X" : "", 10, true);

            write(page, pageSize, 65, 405, 520, 120, reclamacion.getDetails(), 8, false);
            write(page, pageSize, 65, 505, 350, 75, reclamacion.getPedido(), 8, false);

            write(page, pageSize, 445, 530, 145, 16, "Firma digital aceptada", 7, true);

            pdf.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de reclamacion", e);
        }
    }

    private InputStream openTemplate() {
        InputStream template = getClass().getResourceAsStream(TEMPLATE_PATH);
        if (template == null) {
            template = getClass().getResourceAsStream(TEMPLATE_PATH_FALLBACK);
        }
        if (template == null) {
            throw new IllegalStateException("No se encontro la plantilla PDF del libro de reclamaciones");
        }
        return template;
    }

    private void write(PdfPage page, Rectangle pageSize, float x, float top, float width, float height, String value, float fontSize, boolean centered) {
        if (value == null || value.isBlank()) {
            return;
        }

        Rectangle rect = new Rectangle(x, pageSize.getHeight() - top - height, width, height);
        PdfCanvas pdfCanvas = new PdfCanvas(page);
        Canvas canvas = new Canvas(pdfCanvas, rect);
        Paragraph paragraph = new Paragraph(value)
                .setFontSize(fontSize)
                .setFontColor(ColorConstants.BLACK)
                .setMargin(0)
                .setMultipliedLeading(1.0f);

        if (centered) {
            paragraph.setTextAlignment(TextAlignment.CENTER);
        }

        canvas.add(paragraph);
        canvas.close();
    }

    private String productDescription(Reclamacion reclamacion) {
        String description = safe(reclamacion.getProductName());
        if (!safe(reclamacion.getOrderNumber()).isBlank()) {
            description += " / Orden: " + reclamacion.getOrderNumber();
        }
        if (!safe(reclamacion.getProductDescription()).isBlank()) {
            description += " / " + reclamacion.getProductDescription();
        }
        return description.trim();
    }

    private String money(String value) {
        return safe(value).isBlank() ? "" : "S/ " + value;
    }

    private boolean isSelected(String value, String expected) {
        return expected.equalsIgnoreCase(safe(value));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
