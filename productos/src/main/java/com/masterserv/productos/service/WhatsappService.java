package com.masterserv.productos.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WhatsappService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsappService.class);

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.whatsapp-number}") // Tu número de sandbox (+1415...)
    private String fromNumber; 

    @PostConstruct
    public void init() {
        try {
            Twilio.init(accountSid, authToken);
        } catch (Exception e) {
            logger.error("Error Twilio: {}", e.getMessage());
        }
    }

    public void enviarMensaje(String numeroDestino, String mensajeTexto) {
        try {
            if (numeroDestino == null || numeroDestino.isBlank()) return;

            // Formatear número (Twilio necesita "whatsapp:+54...")
            String destinoFinal = numeroDestino.startsWith("whatsapp:") ? numeroDestino : "whatsapp:" + numeroDestino;
            String remitenteFinal = fromNumber.startsWith("whatsapp:") ? fromNumber : "whatsapp:" + fromNumber;

            Message.creator(
                    new PhoneNumber(destinoFinal),
                    new PhoneNumber(remitenteFinal),
                    mensajeTexto
            ).create();
            
            logger.info("Mensaje enviado a {}", numeroDestino);

        } catch (Exception e) {
            logger.error("Error enviando WhatsApp: {}", e.getMessage());
        }
    }
}