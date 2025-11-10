package com.masterserv.productos.service;

import jakarta.mail.internet.MimeMessage;
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
     * Envía un correo electrónico HTML.
     * Es @Async para no bloquear el hilo principal (el de ProcesoAutomaticoService).
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
            // Logueamos el error pero no lo relanzamos.
            // No queremos que un fallo de email detenga nuestro proceso de cotización.
            logger.error("-> 🔴 Error al enviar email a {}: {}", para, e.getMessage());
        }
    }
}