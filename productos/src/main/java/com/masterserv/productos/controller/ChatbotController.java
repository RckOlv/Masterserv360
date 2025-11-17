package com.masterserv.productos.controller;

import com.masterserv.productos.service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// --- Mentor: INICIO DE LA CORRECCIÓN DE RUTA ---
@RestController
@RequestMapping("/api/bot") // Cambiado de "/api/chatbot"
public class ChatbotController {
// --- Mentor: FIN DE LA CORRECCIÓN DE RUTA ---

    @Autowired
    private ChatbotService chatbotService;

    /**
     * Webhook público para recibir mensajes de Twilio.
     */
    // --- Mentor: INICIO DE LA CORRECCIÓN DE RUTA ---
    @PostMapping(value = "/whatsapp", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_XML_VALUE) // Cambiado de "/webhook"
    public ResponseEntity<String> recibirMensajeTwilio(
            @RequestParam(name = "Body") String body,
            @RequestParam(name = "From") String from) {     
    // --- Mentor: FIN DE LA CORRECCIÓN DE RUTA ---
        
        try {
            String twiMLResponse = chatbotService.procesarMensajeWebhook(from, body);
            return ResponseEntity.ok(twiMLResponse);
        } catch (Exception e) {
            String twiMLError = chatbotService.procesarMensajeWebhook(from, "error_interno");
            return ResponseEntity.ok(twiMLError);
        }
    }
}