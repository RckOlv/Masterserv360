package com.masterserv.productos.service;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    /**
     * --- MENTOR: MÉTODO AGREGADO ---
     * Este es el método que le faltaba a tu ClienteService.
     * Simplemente redirige al método de HTML para que se vea bonito.
     */
    @Async
    public void enviarEmail(String para, String asunto, String cuerpo) {
        // Reutilizamos la lógica de HTML
        enviarEmailHtml(para, asunto, cuerpo);
    }

    /**
     * Envía un correo electrónico HTML.
     */
    @Async
    public void enviarEmailHtml(String para, String asunto, String cuerpoHtml) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            
            helper.setTo(para);
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true); // true = El texto es HTML
            helper.setFrom("no-reply@masterserv360.com"); 

            mailSender.send(mimeMessage);
            logger.info("-> 📧 Email enviado a: {}", para);

        } catch (Exception e) {
            logger.error("-> 🔴 Error al enviar email a {}: {}", para, e.getMessage());
        }
    }

    /**
     * Envía un correo con adjunto (PDF).
     */
    @Async
    public void enviarEmailConAdjunto(String para, String asunto, String cuerpoHtml, byte[] adjuntoBytes, String adjuntoNombre) {
        if (adjuntoBytes == null || adjuntoBytes.length == 0) {
            logger.warn("EmailService: Se intentó enviar un email a {} con un adjunto nulo o vacío.", para);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            
            // true = Habilita el modo "multipart"
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");
            
            helper.setTo(para);
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true); 
            helper.setFrom("comprobantes@masterserv360.com");

            // Adjuntar PDF
            ByteArrayDataSource dataSource = new ByteArrayDataSource(adjuntoBytes, "application/pdf");
            helper.addAttachment(adjuntoNombre, dataSource);
            
            mailSender.send(mimeMessage);
            logger.info("-> 📧 Email con PDF adjunto ({}) enviado a: {}", adjuntoNombre, para);

        } catch (Exception e) {
            logger.error("-> 🔴 Error al enviar email con adjunto a {}: {}", para, e.getMessage());
        }
    }
}