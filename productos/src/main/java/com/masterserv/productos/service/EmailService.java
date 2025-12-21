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

    // --- NUEVO MÉTODO DE BIENVENIDA ---
    @Async
    public void sendWelcomeEmail(String para, String nombre) {
        String asunto = "¡Bienvenido a Masterserv360! 🏍️";
        
        // Creamos un HTML bonito para el correo
        String cuerpoHtml = """
            <div style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: auto; border: 1px solid #ddd; border-radius: 8px; overflow: hidden;">
                <div style="background-color: #E41E26; padding: 20px; text-align: center;">
                    <h1 style="color: white; margin: 0;">Masterserv360</h1>
                </div>
                <div style="padding: 20px;">
                    <h2>¡Hola %s! 👋</h2>
                    <p>Gracias por registrarte en nuestra plataforma digital.</p>
                    <p>Ahora podrás:</p>
                    <ul>
                        <li>🔍 Ver nuestro catálogo completo con precios actualizados.</li>
                        <li>📦 Consultar stock en tiempo real.</li>
                        <li>📅 Realizar pedidos y reservarlos.</li>
                    </ul>
                    <p>Si tienes alguna duda, puedes responder a este correo o contactarnos por WhatsApp.</p>
                    <br>
                    <p style="font-size: 12px; color: #777;">Saludos,<br>El equipo de Masterserv</p>
                </div>
            </div>
            """.formatted(nombre);

        // Reutilizamos el método genérico
        enviarEmailHtml(para, asunto, cuerpoHtml);
    }
    // ----------------------------------

    @Async
    public void sendPasswordResetEmail(String para, String nombre, String token) {
        // OJO: Cambia esto por tu URL real de Vercel (Frontend)
        String urlFrontend = "https://masterserv360.vercel.app/reset-password?token=" + token;
        // Para pruebas locales usa: "http://localhost:4200/reset-password?token=" + token;

        String asunto = "Recuperar Contraseña - Masterserv360 🔐";
        
        String cuerpoHtml = """
            <div style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: auto; border: 1px solid #ddd; border-radius: 8px;">
                <div style="background-color: #E41E26; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
                    <h2 style="color: white; margin: 0;">Restablecer Contraseña</h2>
                </div>
                <div style="padding: 20px;">
                    <p>Hola <strong>%s</strong>,</p>
                    <p>Recibimos una solicitud para cambiar tu contraseña.</p>
                    <p>Haz clic en el botón de abajo para crear una nueva:</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background-color: #E41E26; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;">Cambiar Contraseña</a>
                    </div>
                    <p>Este enlace expira en 1 hora.</p>
                    <p style="font-size: 12px; color: #999;">Si no fuiste tú, ignora este correo.</p>
                </div>
            </div>
            """.formatted(nombre, urlFrontend);

        enviarEmailHtml(para, asunto, cuerpoHtml);
    }

    @Async
    public void enviarEmail(String para, String asunto, String cuerpo) {
        enviarEmailHtml(para, asunto, cuerpo);
    }

    @Async
    public void enviarEmailHtml(String para, String asunto, String cuerpoHtml) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            
            helper.setTo(para);
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true); 
            helper.setFrom("no-reply@masterserv360.com"); 

            mailSender.send(mimeMessage);
            logger.info("-> 📧 Email enviado a: {}", para);

        } catch (Exception e) {
            logger.error("-> 🔴 Error al enviar email a {}: {}", para, e.getMessage());
        }
    }

    @Async
    public void enviarEmailConAdjunto(String para, String asunto, String cuerpoHtml, byte[] adjuntoBytes, String adjuntoNombre) {
        if (adjuntoBytes == null || adjuntoBytes.length == 0) {
            logger.warn("EmailService: Se intentó enviar un email a {} con un adjunto nulo o vacío.", para);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");
            
            helper.setTo(para);
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true); 
            helper.setFrom("comprobantes@masterserv360.com");

            ByteArrayDataSource dataSource = new ByteArrayDataSource(adjuntoBytes, "application/pdf");
            helper.addAttachment(adjuntoNombre, dataSource);
            
            mailSender.send(mimeMessage);
            logger.info("-> 📧 Email con PDF adjunto ({}) enviado a: {}", adjuntoNombre, para);

        } catch (Exception e) {
            logger.error("-> 🔴 Error al enviar email con adjunto a {}: {}", para, e.getMessage());
        }
    }
}