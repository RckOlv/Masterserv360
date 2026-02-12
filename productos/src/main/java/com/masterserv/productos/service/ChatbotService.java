package com.masterserv.productos.service;

import com.masterserv.productos.entity.*;
import com.masterserv.productos.enums.EstadoListaEspera;
import com.masterserv.productos.repository.*;
import com.twilio.twiml.MessagingResponse;
import com.twilio.twiml.messaging.Message;
import com.twilio.twiml.messaging.Body;
import com.twilio.twiml.messaging.Media;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private static final String LINK_REGISTRO = "https://masterserv360.vercel.app/auth/register"; 

    // --- MEMORIA TEMPORAL DEL BOT (RAM) ---
    // Guarda el estado actual de la conversación (MENU, BUSCANDO, CANJEANDO, etc.)
    private final Map<String, String> usuarioEstado = new ConcurrentHashMap<>();
    
    // Guarda el ID del producto que se buscó (para lista de espera)
    private final Map<String, Long> usuarioUltimoProducto = new ConcurrentHashMap<>();
    
    // Guarda la lista de recompensas mostradas al usuario para selección numérica
    private final Map<String, List<Recompensa>> usuarioOpcionesCanje = new ConcurrentHashMap<>();

    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final InteraccionChatbotRepository interaccionRepository;
    private final PuntosService puntosService;
    private final SolicitudProductoRepository solicitudProductoRepository;
    private final RecompensaRepository recompensaRepository;
    private final CuentaPuntosRepository cuentaPuntosRepository;
    private final ListaEsperaRepository listaEsperaRepository;
    private final CuponService cuponService;
    private final AlertaService alertaService; // <--- (1) NUEVO: Servicio de Alertas

    public ChatbotService(UsuarioRepository usuarioRepository,
                          ProductoRepository productoRepository,
                          InteraccionChatbotRepository interaccionRepository,
                          PuntosService puntosService,
                          SolicitudProductoRepository solicitudProductoRepository,
                          RecompensaRepository recompensaRepository,
                          CuentaPuntosRepository cuentaPuntosRepository,
                          ListaEsperaRepository listaEsperaRepository,
                          CuponService cuponService,
                          AlertaService alertaService) { // <--- (2) Inyectamos AlertaService
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.interaccionRepository = interaccionRepository;
        this.puntosService = puntosService;
        this.solicitudProductoRepository = solicitudProductoRepository;
        this.recompensaRepository = recompensaRepository;
        this.cuentaPuntosRepository = cuentaPuntosRepository;
        this.listaEsperaRepository = listaEsperaRepository;
        this.cuponService = cuponService;
        this.alertaService = alertaService;
    }

    private static class BotResponse {
        String texto;
        String mediaUrl;
        public BotResponse(String texto) { this.texto = texto; }
        public BotResponse(String texto, String mediaUrl) { this.texto = texto; this.mediaUrl = mediaUrl; }
    }

    @Transactional
    public String procesarMensajeWebhook(String from, String body) {
        String telefono = from.replace("whatsapp:", "").trim();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByTelefono(telefono);
        
        // Registrar Entrada
        try { registrarInteraccion(body, null, usuarioOpt.orElse(null)); } catch (Exception e) {}

        BotResponse respuesta;
        try {
            respuesta = procesarFlujo(body.trim(), telefono, usuarioOpt);
        } catch (Exception e) {
            e.printStackTrace();
            respuesta = new BotResponse("😓 Ups, ocurrió un error. Escribe 'Hola' para reiniciar.");
            usuarioEstado.remove(telefono); 
        }
        
        // Registrar Salida
        try { registrarInteraccion(null, respuesta.texto, usuarioOpt.orElse(null)); } catch (Exception e) {}
        
        return construirRespuestaTwiML(respuesta);
    }

    // --- 🧠 CEREBRO CON MÁQUINA DE ESTADOS ---
    private BotResponse procesarFlujo(String input, String telefono, Optional<Usuario> usuarioOpt) {
        String texto = normalizarTexto(input);

        // 1. USUARIO NO REGISTRADO
        if (usuarioOpt.isEmpty()) {
            return new BotResponse(
                "👋 *¡Bienvenido a Masterserv360!*\n\n" +
                "Para ver precios y canjear premios, necesito que te registres gratis aquí:\n👉 " + LINK_REGISTRO + "\n\n" +
                "Luego vuelve y escribe *Hola*."
            );
        }

        Usuario usuario = usuarioOpt.get();

        // 2. COMANDOS GLOBALES DE RESET
        if (detectarIntencion(texto, List.of("hola", "menu", "inicio", "salir", "cancelar", "chau", "atras"))) {
            resetearEstado(telefono);
            return mostrarMenuPrincipal(usuario.getNombre());
        }

        // 3. MÁQUINA DE ESTADOS
        String estadoActual = usuarioEstado.getOrDefault(telefono, "MENU");

        switch (estadoActual) {
            case "MENU":
                return procesarOpcionMenu(texto, telefono, usuario);
            
            case "BUSCANDO":
                return procesarBusquedaProducto(texto, telefono, usuario);

            case "CONFIRMANDO_ESPERA":
                return procesarConfirmacionEspera(texto, telefono, usuario);

            case "CANJEANDO":
                return procesarSeleccionPremio(texto, telefono, usuario);

            default:
                resetearEstado(telefono);
                return mostrarMenuPrincipal(usuario.getNombre());
        }
    }

    // --- PANTALLAS DEL BOT ---

    private BotResponse mostrarMenuPrincipal(String nombre) {
        return new BotResponse(
            "👋 Hola *" + nombre + "*. ¿En qué te ayudo hoy?\n\n" +
            "1️⃣ *Buscar Repuesto / Stock*\n" +
            "2️⃣ *Mis Puntos y Premios*\n" +
            "3️⃣ *Hablar con un Humano*\n\n" +
            "👇 _Escribe el número de la opción._"
        );
    }

    private BotResponse procesarOpcionMenu(String input, String telefono, Usuario usuario) {
        if (input.equals("1")) {
            usuarioEstado.put(telefono, "BUSCANDO");
            return new BotResponse("🔎 *Buscador de Repuestos*\n\nEscribe el nombre o código del producto que buscas.\n_(Ej: bujia, aceite, espejo)_");
        } 
        else if (input.equals("2")) {
            // Mostramos puntos y lista de premios numerada
            usuarioEstado.put(telefono, "CANJEANDO");
            return mostrarPremiosDisponibles(usuario, telefono);
        }
        else if (input.equals("3")) {
            // <--- (3) INTEGRACIÓN ALERTAS
            alertaService.crearAlerta(
                "🆘 Solicitud de Humano",
                "El cliente " + usuario.getNombre() + " (" + telefono + ") solicita hablar con un humano.",
                usuario,
                "/clientes/detalle/" + usuario.getId() 
            );
            return new BotResponse("💬 Entendido. He enviado una alerta a nuestros asesores. 🔔\n");
        }
        else {
            return new BotResponse("⚠️ Opción no válida. Escribe *1*, *2* o *3*.");
        }
    }

    // --- 🛍️ LÓGICA DE BÚSQUEDA Y ESPERA ---

    private BotResponse procesarBusquedaProducto(String termino, String telefono, Usuario usuario) {
        if (termino.length() < 3) return new BotResponse("⚠️ Escribe un nombre más largo para buscar mejor.");

        // Buscamos en BD
        Pageable top1 = PageRequest.of(0, 1);
        List<Producto> encontrados;
        try {
             encontrados = productoRepository.buscarFlexible(termino, top1).getContent();
        } catch (Exception e) {
             encontrados = productoRepository.findByNombreILike(termino, top1);
        }

        // CASO A: NO EXISTE (Anti-Choripán)
        if (encontrados.isEmpty()) {
            return new BotResponse("🚫 Disculpa, no tenemos ese producto en nuestro catálogo.\n\nIntenta con otro nombre o escribe *Hola* para volver al menú.");
        }

        Producto producto = encontrados.get(0);

        // CASO B: HAY STOCK
        if (producto.getStockActual() > 0) {
            resetearEstado(telefono);
            return formatearRespuestaProducto(producto);
        }

        // CASO C: NO HAY STOCK (Preguntar)
        usuarioEstado.put(telefono, "CONFIRMANDO_ESPERA");
        usuarioUltimoProducto.put(telefono, producto.getId());

        return new BotResponse(
            "📦 *" + producto.getNombre() + "*\n" +
            "🔴 Estado: AGOTADO TEMPORALMENTE\n\n" +
            "¿Quieres que te anote en la lista de espera para avisarte cuando llegue?\n\n" +
            "1️⃣ *Sí, avísame*\n" +
            "2️⃣ *No, gracias*"
        );
    }

    private BotResponse procesarConfirmacionEspera(String input, String telefono, Usuario usuario) {
        if (!usuarioUltimoProducto.containsKey(telefono)) {
            resetearEstado(telefono);
            return new BotResponse("⏳ Pasó mucho tiempo. Vuelve a buscar el producto.");
        }

        Long productoId = usuarioUltimoProducto.get(telefono);
        Producto producto = productoRepository.findById(productoId).orElse(null);

        if (input.equals("1") || input.equalsIgnoreCase("si")) {
            boolean yaAnotado = listaEsperaRepository.existsByUsuarioAndProductoAndEstado(usuario, producto, EstadoListaEspera.PENDIENTE);
            
            if (!yaAnotado) {
                ListaEspera espera = new ListaEspera();
                espera.setUsuario(usuario);
                espera.setProducto(producto);
                espera.setFechaSolicitud(LocalDateTime.now());
                espera.setEstado(EstadoListaEspera.PENDIENTE);
                listaEsperaRepository.save(espera);
            }

            resetearEstado(telefono);
            return new BotResponse("✅ ¡Listo! Te anoté. Te avisaremos por aquí cuando entre stock de *" + producto.getNombre() + "*.");
        } 
        else if (input.equals("2") || input.equalsIgnoreCase("no")) {
            resetearEstado(telefono);
            return new BotResponse("👍 Entendido. ¿Necesitas buscar otra cosa? Escribe *1* desde el menú.");
        } 
        else {
            return new BotResponse("⚠️ Responde *1* (Sí) o *2* (No).");
        }
    }

    // --- 🎁 LÓGICA DE CANJE NUMÉRICO ---

    private BotResponse mostrarPremiosDisponibles(Usuario usuario, String telefono) {
        var saldoDTO = puntosService.getSaldoByEmail(usuario.getEmail());
        int puntosActuales = saldoDTO.getSaldoPuntos();
        
        // Filtramos solo premios activos y con stock
        List<Recompensa> premiosVisibles = recompensaRepository.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getActivo()) && r.getStock() > 0)
                .collect(Collectors.toList());

        // Guardamos la lista en memoria para este usuario
        usuarioOpcionesCanje.put(telefono, premiosVisibles);

        StringBuilder msg = new StringBuilder();
        msg.append("🏆 *Tienes ").append(puntosActuales).append(" Puntos*\n\n");
        
        if (premiosVisibles.isEmpty()) {
            msg.append("😓 Por ahora no tenemos premios en stock. ¡Vuelve pronto!");
            resetearEstado(telefono);
        } else {
            msg.append("🎁 *Premios Disponibles:*\n");
            for (int i = 0; i < premiosVisibles.size(); i++) {
                Recompensa r = premiosVisibles.get(i);
                String icon = (puntosActuales >= r.getPuntosRequeridos()) ? "✅" : "🔒";
                msg.append(String.format("\n%d️⃣ %s *%s* (%d pts)", i + 1, icon, r.getDescripcion(), r.getPuntosRequeridos()));
            }
            msg.append("\n\n👇 *Escribe el número del premio* para canjear (o 'Salir').");
        }

        return new BotResponse(msg.toString());
    }

    private BotResponse procesarSeleccionPremio(String input, String telefono, Usuario usuario) {
        List<Recompensa> opciones = usuarioOpcionesCanje.get(telefono);
        
        if (opciones == null || opciones.isEmpty()) {
            resetearEstado(telefono);
            return new BotResponse("⏳ Sesión expirada. Vuelve al menú escribiendo *Hola*.");
        }

        try {
            int indice = Integer.parseInt(input) - 1; // Convertir "1" a índice 0
            
            if (indice >= 0 && indice < opciones.size()) {
                Recompensa premioElegido = opciones.get(indice);
                return ejecutarCanje(usuario, premioElegido, telefono);
            } else {
                return new BotResponse("⚠️ Número inválido. Elige una opción de la lista (ej: 1, 2...).");
            }
        } catch (NumberFormatException e) {
            return new BotResponse("⚠️ Por favor escribe solo el número de la opción.");
        }
    }

    private BotResponse ejecutarCanje(Usuario usuario, Recompensa premio, String telefono) {
        var cuentaOpt = cuentaPuntosRepository.findByCliente(usuario);
        if (cuentaOpt.isEmpty() || cuentaOpt.get().getSaldoPuntos() < premio.getPuntosRequeridos()) {
             return new BotResponse("🚫 *Saldo insuficiente.*\nTe faltan puntos para canjear *" + premio.getDescripcion() + "*.\n\nSigue sumando puntos con tus compras! 💪");
        }

        try {
            // Lógica Transaccional
            Cupon cupon = cuponService.crearCuponPorCanje(usuario, premio);
            
            // Actualizamos puntos localmente para reflejarlo
            CuentaPuntos cuenta = cuentaOpt.get();
            cuenta.setSaldoPuntos(cuenta.getSaldoPuntos() - premio.getPuntosRequeridos());
            cuentaPuntosRepository.save(cuenta);
            premio.setStock(premio.getStock() - 1);
            recompensaRepository.save(premio);

            resetearEstado(telefono); // Canje exitoso, salimos del flujo
            
            return new BotResponse(
                "🎉 *¡CANJE EXITOSO!* 🎉\n\n" +
                "Has canjeado: *" + premio.getDescripcion() + "*\n" +
                "Tu código es: 👉 *" + cupon.getCodigo() + "*\n\n"
            );

        } catch (Exception e) {
            e.printStackTrace();
            return new BotResponse("🔥 Hubo un error técnico al procesar el canje. Intenta de nuevo.");
        }
    }

    // --- MÉTODOS DE APOYO Y LIMPIEZA ---

    private void resetearEstado(String telefono) {
        usuarioEstado.remove(telefono);
        usuarioUltimoProducto.remove(telefono);
        usuarioOpcionesCanje.remove(telefono);
    }

    private String normalizarTexto(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                         .replaceAll("[^a-zA-Z0-9\\s]", "")
                         .toLowerCase()
                         .trim();
    }

    private boolean detectarIntencion(String textoUsuario, List<String> palabrasClave) {
        boolean contiene = palabrasClave.stream().anyMatch(k -> textoUsuario.contains(k));
        if (contiene) return true;
        return false;
    }

    private BotResponse formatearRespuestaProducto(Producto p) {
        String precioStr = (p.getPrecioVenta() != null) ? String.format("$%,.2f", p.getPrecioVenta().doubleValue()) : "Consultar";
        String imagen = (p.getImagenUrl() != null && p.getImagenUrl().startsWith("http")) ? p.getImagenUrl() : null;

        return new BotResponse(
            "✅ *¡Sí hay stock!*\n\n" +
            "📦 " + p.getNombre() + "\n" +
            "💲 " + precioStr + "\n" +
            "🟢 Disponibles: " + p.getStockActual() + "\n\n" +
            "📍 Pasa por el local a retirarlo.", imagen
        );
    }
    
    private void registrarInteraccion(String in, String out, Usuario u) {
        InteraccionChatbot i = new InteraccionChatbot();
        i.setFecha(LocalDateTime.now());
        i.setMensajeUsuario(in);
        i.setRespuestaBot(out);
        i.setUsuario(u); 
        interaccionRepository.save(i);
    }

    private String construirRespuestaTwiML(BotResponse respuesta) {
        Message.Builder messageBuilder = new Message.Builder();
        messageBuilder.body(new Body.Builder(respuesta.texto).build());
        if (respuesta.mediaUrl != null && !respuesta.mediaUrl.isEmpty()) {
            messageBuilder.media(new Media.Builder(respuesta.mediaUrl).build());
        }
        return new MessagingResponse.Builder().message(messageBuilder.build()).build().toXml();
    }
}