package com.masterserv.productos.dto;

import lombok.Data;

/**
 * DTO simplificado para recibir un webhook de Twilio (o un simulador).
 * Representa el mensaje entrante de WhatsApp.
 */
@Data
public class TwilioRequestDTO {

    // El número de teléfono del usuario (ej: "whatsapp:+5493758...")
    private String from; 
    
    // El mensaje que el usuario escribió (ej: "stock filtro k&n")
    private String body; 
    
    // Podemos agregar más campos si Twilio los envía (MessageSid, AccountSid, etc.)
}