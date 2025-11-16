package com.masterserv.productos.service;

import jakarta.mail.internet.MimeMessage;
// Mentor: Importamos la clase que vamos a usar
import jakarta.mail.util.ByteArrayDataSource; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
// Mentor: Ya no necesitamos ByteArrayResource
// import org.springframework.core.io.ByteArrayResource; 
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
     * Envía un correo electrónico HTML.
     * (Este método queda igual)
     */
    @Async
    public void enviarEmailHtml(String para, String asunto, String cuerpoHtml) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            
            // true = Habilita el modo multipart (necesario para HTML)
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            
            helper.setTo(para);
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true); // true = El texto es HTML
            helper.setFrom("no-reply@masterserv360.com"); // (Puede ser cualquier cosa)

            mailSender.send(mimeMessage);
            logger.info("-> 📧 Email de cotización enviado (simulado) a: {}", para);

        } catch (Exception e) {
            logger.error("-> 🔴 Error al enviar email a {}: {}", para, e.getMessage());
        }
    }

    /**
     * Mentor: MÉTODO MODIFICADO
     * Cambiamos ByteArrayResource por ByteArrayDataSource
     */
    @Async
    public void enviarEmailConAdjunto(String para, String asunto, String cuerpoHtml, byte[] adjuntoBytes, String adjuntoNombre) {
        // Hacemos la validación que pusimos en el Listener
        if (adjuntoBytes == null || adjuntoBytes.length == 0) {
            logger.warn("EmailService: Se intentó enviar un email a {} con un adjunto nulo o vacío.", para);
            // (Opcional: enviar el email sin adjunto)
            // enviarEmailHtml(para, asunto, cuerpoHtml); 
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            
            // true = Habilita el modo "multipart" (necesario para adjuntos)
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");
            
            helper.setTo(para);
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true); // true = El texto es HTML
            helper.setFrom("comprobantes@masterserv360.com");

            // --- Mentor: INICIO DE LA MODIFICACIÓN ---
            
            // 1. Creamos un DataSource explícito con los bytes y el tipo MIME
            ByteArrayDataSource dataSource = new ByteArrayDataSource(adjuntoBytes, "application/pdf");
            
            // 2. Lo añadimos al helper
            helper.addAttachment(adjuntoNombre, dataSource);
            
            // --- FIN DE LA MODIFICACIÓN ---

            mailSender.send(mimeMessage);
            logger.info("-> 📧 Email con PDF adjunto ({}) enviado (simulado) a: {}", adjuntoNombre, para);

        } catch (Exception e) {
            logger.error("-> 🔴 Error al enviar email con adjunto a {}: {}", para, e.getMessage());
        }
    }
}