package com.masterserv.productos.service;

import com.masterserv.productos.entity.InteraccionChatbot;
import com.masterserv.productos.entity.Producto;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.repository.InteraccionChatbotRepository;
import com.masterserv.productos.repository.ProductoRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import com.twilio.Twilio;
import com.twilio.twiml.MessagingResponse;
import com.twilio.twiml.messaging.Message;

import jakarta.annotation.PostConstruct; 
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ChatbotService {

    @Value("${twilio.account-sid}")
    private String ACCOUNT_SID;
    @Value("${twilio.auth-token}")
    private String AUTH_TOKEN;
    @Value("${twilio.whatsapp-number}")
    private String TWILIO_NUMBER;

    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final InteraccionChatbotRepository interaccionRepository;
    private final PuntosService puntosService; 

    public ChatbotService(UsuarioRepository usuarioRepository,
                          ProductoRepository productoRepository,
                          InteraccionChatbotRepository interaccionRepository,
                          PuntosService puntosService) {
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.interaccionRepository = interaccionRepository;
        this.puntosService = puntosService;
    }

    @PostConstruct
    public void init() {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
    }

    @Transactional
    public String procesarMensajeWebhook(String from, String body) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByTelefono(from.replace("whatsapp:", ""));
        
        registrarInteraccion(body, null, usuarioOpt.orElse(null));

        String mensajeRespuesta = procesarComando(body.trim(), usuarioOpt); 

        registrarInteraccion(null, mensajeRespuesta, usuarioOpt.orElse(null));
        
        return construirRespuestaTwiML(mensajeRespuesta);
    }

    private String procesarComando(String comando, Optional<Usuario> usuarioOpt) {
        
        String comandoLower = comando.toLowerCase();

        if (usuarioOpt.isEmpty()) {
            return "¡Hola! No te encontramos en nuestra base de datos. Por favor, regístrate en nuestra web (masterserv.com) para usar el chatbot.";
        }

        Usuario usuario = usuarioOpt.get();

        if (comandoLower.equals("hola") || comandoLower.equals("menu") || comandoLower.equals("ayuda")) {
            return String.format(
                "¡Hola, %s! Bienvenido a Masterserv360.\n\n" +
                "Escribe un comando:\n" +
                "1. *stock [nombre o código]* (Ej: stock Filtro Aceite)\n" + 
                "2. *puntos* (Para ver tu saldo)\n" +
                "3. *menu* (Para ver esto de nuevo)",
                usuario.getNombre()
            );
        }

        if (comandoLower.startsWith("stock ")) {
            String termino = comando.substring(6).trim(); 
            
            Optional<Producto> productoPorCodigo = productoRepository.findByCodigo(termino.toUpperCase());
            if (productoPorCodigo.isPresent()) {
                return formatearRespuestaProducto(productoPorCodigo.get());
            }

            Pageable top5 = PageRequest.of(0, 5); 
            List<Producto> productos = productoRepository.findByNombreILike(termino, top5);

            if (productos.isEmpty()) {
                return String.format("Lo sentimos, no encontramos productos que coincidan con *%s*.", termino);
            
            } else if (productos.size() == 1) {
                return formatearRespuestaProducto(productos.get(0));
            
            } else {
                StringBuilder respuesta = new StringBuilder("Encontramos varios productos. ¿Cuál buscas?\n");
                int i = 1;
                for (Producto p : productos) {
                    respuesta.append(String.format("\n%d. *%s*", i++, p.getNombre()));
                }
                respuesta.append("\n\nEscribe *stock [código]* o sé más específico.");
                return respuesta.toString();
            }
        }

        if (comandoLower.equals("puntos")) {
            var saldo = puntosService.getSaldoByEmail(usuario.getEmail());
            return String.format(
                "Consulta de Saldo:\n\n" +
                "Tienes *%d puntos* acumulados.\n" +
                "Esto equivale a *ARS $%,.2f* en descuentos para tu próxima compra.",
                saldo.getSaldoPuntos(), saldo.getValorMonetario()
            );
        }
        
        if (comandoLower.equals("error_interno")) { 
             return "Lo siento, tuvimos un problema procesando tu solicitud. Por favor, intenta de nuevo más tarde.";
        }

        return "Lo siento, no entendí ese comando. Escribe *menu* para ver las opciones.";
    }

    // --- Mentor: INICIO DE LA CORRECCIÓN (Error Java int == null) ---
    /**
     * Formatea la respuesta estándar para un solo producto.
     * Ahora oculta la cantidad exacta de stock.
     */
    private String formatearRespuestaProducto(Producto p) {
        
        String disponibilidad;
        
        // CORRECCIÓN: Un 'int' no puede ser null, solo se compara con 0.
        if (p.getStockActual() <= 0) {
            disponibilidad = "*No nos queda stock en este momento.*";
        } else if (p.getStockActual() <= p.getStockMinimo()) {
            disponibilidad = "*¡Quedan pocas unidades!* (Te recomendamos pasar pronto)";
        } else {
            disponibilidad = "*Sí, tenemos stock disponible.*";
        }

        return String.format(
            "Consulta de Stock:\n*%s*\n(Código: %s)\nDisponibilidad: %s\nPrecio: *$%,.2f*",
            p.getNombre(), p.getCodigo(), disponibilidad, p.getPrecioVenta()
        );
    }
    // --- Mentor: FIN DE LA CORRECCIÓN ---

    private void registrarInteraccion(String mensajeEntrante, String mensajeSaliente, Usuario usuario) {
        InteraccionChatbot interaccion = new InteraccionChatbot();
        interaccion.setFecha(LocalDateTime.now());
        interaccion.setMensajeUsuario(mensajeEntrante);
        interaccion.setRespuestaBot(mensajeSaliente);
        interaccion.setUsuario(usuario);
        
        interaccionRepository.save(interaccion);
    }

    private String construirRespuestaTwiML(String mensaje) {
        return new MessagingResponse.Builder()
                .message(new Message.Builder(mensaje).build())
                .build()
                .toXml();
    }
}