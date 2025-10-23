package com.masterserv.productos.controller;

import com.masterserv.productos.dto.TwilioRequestDTO;
import com.masterserv.productos.service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador que actúa como Webhook para Twilio.
 * Es un endpoint PÚBLICO (no requiere JWT) porque es llamado por un servicio externo (Twilio).
 */
@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    /**
     * Endpoint que Twilio llamará cada vez que llegue un mensaje de WhatsApp.
     * Recibe el mensaje, lo procesa en el ChatbotService y devuelve la respuesta.
     */
    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleWebhook(@RequestBody TwilioRequestDTO request) {
        
        try {
            // 1. Procesamos el mensaje (buscamos stock, anotamos en lista, etc.)
            String respuestaBot = chatbotService.procesarMensaje(request);

            // 2. Devolvemos la respuesta
            // En un caso real con Twilio, devolveríamos un TwiML (XML)
            // <Response><Message>respuestaBot</Message></Response>
            // Por ahora (24 días), devolvemos texto plano o un JSON simple.
            return ResponseEntity.ok(respuestaBot);

        } catch (Exception e) {
            // Si algo falla, le respondemos al usuario que hubo un error
            return ResponseEntity.ok("Disculpa, tuve un problema interno. Intenta más tarde.");
        }
    }
}