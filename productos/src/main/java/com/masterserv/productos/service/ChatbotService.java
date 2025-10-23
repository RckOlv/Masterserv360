package com.masterserv.productos.service;

import com.masterserv.productos.dto.TwilioRequestDTO;
import com.masterserv.productos.entity.*;
import com.masterserv.productos.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ChatbotService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ListaEsperaRepository listaEsperaRepository;

    @Autowired
    private InteraccionChatbotRepository interaccionRepository;

    // ----- AQUÍ VA TU LÓGICA DE TWILIO -----
    // @Autowired
    // private TuTwilioClient twilioClient;
    // ----------------------------------------

    /**
     * Lógica principal del chatbot. Procesa un mensaje entrante.
     * Es transaccional porque puede escribir en múltiples tablas.
     */
    @Transactional
    public String procesarMensaje(TwilioRequestDTO request) {
        
        // 1. Identificar al usuario (simplificado)
        // En un caso real, buscaríamos al usuario por su 'request.getFrom()' (nro de tel)
        Usuario usuario = usuarioRepository.findByTelefono(request.getFrom()).orElse(null);

        // 2. Procesar la intención del mensaje (¡AQUÍ VA TU LÓGICA!)
        // Esta es una simulación. Aquí es donde tu código de Twilio
        // interpretaría el 'request.getBody()'.
        
        String respuestaBot;
        Producto productoConsultado = null;

        if (request.getBody().toLowerCase().contains("stock de")) {
            // Lógica simulada de búsqueda de producto
            String nombreProducto = request.getBody().substring(10).trim();
            Optional<Producto> optProducto = productoRepository.findByNombre(nombreProducto); // Necesitarás este método en ProductoRepo

            if (optProducto.isPresent()) {
                productoConsultado = optProducto.get();
                if (productoConsultado.getStockActual() > 0) {
                    respuestaBot = "¡Buenas! Sí, tenemos stock del " + productoConsultado.getNombre() + ".";
                } else {
                    respuestaBot = "Hola. No tenemos stock del " + productoConsultado.getNombre() + ". ¿Deseas que te anote en la lista de espera? (Responde 'SI')";
                }
            } else {
                respuestaBot = "Disculpa, no encontré un producto con ese nombre.";
            }

        } else if (request.getBody().equalsIgnoreCase("SI")) {
            // Lógica simulada de "Anotar en Lista de Espera"
            // (Necesitaríamos guardar el 'productoConsultado' en una sesión de chat)
            respuestaBot = "Función 'Anotar en Lista' aún no implementada.";
            // ... aquí llamaríamos a anotarEnListaDeEspera(...) ...
            
        } else {
            respuestaBot = "Hola, soy el bot de Masterserv. Puedes consultarme por el stock de un producto (ej: 'stock de Filtro K&N').";
        }

        // 3. Guardar la interacción en el historial (Auditoría)
        InteraccionChatbot interaccion = new InteraccionChatbot();
        interaccion.setUsuario(usuario); // Puede ser null si el nro no está registrado
        interaccion.setProducto(productoConsultado);
        interaccion.setMensajeUsuario(request.getBody());
        interaccion.setRespuestaBot(respuestaBot);
        interaccion.setFecha(LocalDateTime.now());
        interaccionRepository.save(interaccion);

        // 4. Devolver la respuesta para que el Controller la envíe a Twilio
        return respuestaBot;
    }

    /**
     * Lógica para anotar a un usuario en la lista de espera (Fuente 195)
     */
    @Transactional
    public void anotarEnListaDeEspera(Usuario usuario, Producto producto) {
        // Verificamos si ya está anotado
        if (listaEsperaRepository.findByUsuarioAndProducto(usuario, producto).isEmpty()) {
            
            ListaEspera nuevaEspera = new ListaEspera();
            nuevaEspera.setUsuario(usuario);
            nuevaEspera.setProducto(producto);
            nuevaEspera.setFechaInscripcion(LocalDate.now());
            nuevaEspera.setEstado("ACTIVA");
            
            listaEsperaRepository.save(nuevaEspera);
        }
    }

    // Aquí iría la lógica del Proceso Automatizado 3:
    // Un @Scheduled que revisa el stock y envía notificaciones.
    // Lo haremos después, ¡primero el webhook!
}