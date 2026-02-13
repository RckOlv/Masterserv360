package com.masterserv.productos.service;

import com.masterserv.productos.entity.InteraccionChatbot;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.repository.InteraccionChatbotRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ChatbotLogService {

    private final InteraccionChatbotRepository interaccionRepository;

    public ChatbotLogService(InteraccionChatbotRepository interaccionRepository) {
        this.interaccionRepository = interaccionRepository;
    }

    // ✅ Al ser una clase pública externa, @Async AHORA SÍ FUNCIONA
    @Async 
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Crea una transacción nueva e independiente
    public void registrarInteraccion(String in, String out, Usuario u) {
        try {
            InteraccionChatbot i = new InteraccionChatbot();
            i.setFecha(LocalDateTime.now());
            i.setMensajeUsuario(in);
            i.setRespuestaBot(out);
            i.setUsuario(u);
            
            interaccionRepository.save(i);
            
        } catch (Exception e) {
            // Si falla el log, solo lo imprimimos en consola, pero NO ROMPEMOS EL BOT
            System.err.println("⚠️ Error guardando log de chat (No crítico): " + e.getMessage());
        }
    }
}