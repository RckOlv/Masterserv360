package com.masterserv.productos.service;

import com.masterserv.productos.entity.*;
import com.masterserv.productos.enums.EstadoListaEspera;
import com.masterserv.productos.repository.*;
import com.twilio.twiml.MessagingResponse;
import com.twilio.twiml.messaging.Message;
import com.twilio.twiml.messaging.Body;
import com.twilio.twiml.messaging.Media;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private static final String LINK_REGISTRO = "https://masterserv360.vercel.app/auth/register"; 

    // Listas temporales para visualización
    private final Map<String, List<Recompensa>> usuarioOpcionesCanje = new ConcurrentHashMap<>();
    private final Map<String, List<Producto>> usuarioOpcionesBusqueda = new ConcurrentHashMap<>();

    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final InteraccionChatbotRepository interaccionRepository;
    private final PuntosService puntosService;
    private final SolicitudProductoRepository solicitudProductoRepository;
    private final RecompensaRepository recompensaRepository;
    private final CuentaPuntosRepository cuentaPuntosRepository;
    private final ListaEsperaRepository listaEsperaRepository;
    private final CuponService cuponService;
    private final AlertaService alertaService;
    private final SesionChatRepository sesionRepository;

    public ChatbotService(UsuarioRepository usuarioRepository,
                          ProductoRepository productoRepository,
                          InteraccionChatbotRepository interaccionRepository,
                          PuntosService puntosService,
                          SolicitudProductoRepository solicitudProductoRepository,
                          RecompensaRepository recompensaRepository,
                          CuentaPuntosRepository cuentaPuntosRepository,
                          ListaEsperaRepository listaEsperaRepository,
                          CuponService cuponService,
                          AlertaService alertaService,
                          SesionChatRepository sesionRepository) {
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
        this.sesionRepository = sesionRepository;
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
        
        System.out.println("📩 Msg de " + telefono + ": " + body);

        // Registro de entrada (Protegido para no romper flujo)
        registrarInteraccionSegura(body, null, usuarioOpt.orElse(null));

        BotResponse respuesta;
        try {
            respuesta = procesarFlujo(body.trim(), telefono, usuarioOpt);
        } catch (Exception e) {
            e.printStackTrace();
            respuesta = new BotResponse("😓 Ups, ocurrió un error. Escribe 'Hola' para reiniciar.");
            resetearSesion(telefono); 
        }
        
        // Registro de salida (Protegido)
        registrarInteraccionSegura(null, respuesta.texto, usuarioOpt.orElse(null));
        
        return construirRespuestaTwiML(respuesta);
    }

    // --- 🧠 CEREBRO CON MEMORIA EN BASE DE DATOS ---
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
        SesionChat sesion = obtenerSesion(telefono);

        // 2. COMANDOS GLOBALES
        if (detectarIntencion(texto, List.of("hola", "menu", "inicio", "salir", "cancelar", "chau", "atras"))) {
            resetearSesion(telefono);
            return mostrarMenuPrincipal(usuario.getNombre());
        }

        // 3. MÁQUINA DE ESTADOS
        String estadoActual = (sesion.getEstadoActual() != null) ? sesion.getEstadoActual() : "MENU";
        System.out.println("🤖 Estado BD para " + telefono + ": " + estadoActual);

        switch (estadoActual) {
            case "MENU":
                return procesarOpcionMenu(texto, telefono, usuario, sesion);
            case "BUSCANDO":
                return procesarBusquedaProducto(texto, telefono, usuario, sesion);
            case "SELECCIONANDO_PRODUCTO":
                return procesarSeleccionProductoBuscado(texto, telefono, usuario, sesion);
            case "CONFIRMANDO_ESPERA":
                return procesarConfirmacionEspera(texto, telefono, usuario, sesion);
            case "CANJEANDO":
                return procesarSeleccionPremio(texto, telefono, usuario, sesion);
            default:
                resetearSesion(telefono);
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

    private BotResponse procesarOpcionMenu(String input, String telefono, Usuario usuario, SesionChat sesion) {
        if (input.contains("1")) {
            actualizarEstado(sesion, "BUSCANDO");
            return new BotResponse("🔎 *Buscador de Repuestos*\n\nEscribe el nombre o código del producto que buscas.\n_(Ej: bujia, aceite, espejo)_");
        } 
        else if (input.contains("2")) {
            actualizarEstado(sesion, "CANJEANDO");
            return mostrarPremiosDisponibles(usuario, telefono);
        }
        else if (input.contains("3")) {
            alertaService.crearAlerta(
                "🆘 Solicitud de Humano",
                "El cliente " + usuario.getNombre() + " (" + telefono + ") solicita hablar con un humano.",
                usuario,
                "/clientes/detalle/" + usuario.getId() 
            );
            return new BotResponse("💬 Entendido. He enviado una alerta a nuestros asesores. 🔔\n\nEn breve te contactarán.");
        }
        else {
            return new BotResponse("⚠️ Opción no válida. Escribe *1*, *2* o *3*.");
        }
    }

    // --- 🛍️ LÓGICA DE BÚSQUEDA Y ESPERA ---

    private BotResponse procesarBusquedaProducto(String termino, String telefono, Usuario usuario, SesionChat sesion) {
        if (termino.length() < 3) return new BotResponse("⚠️ Escribe un nombre más largo para buscar mejor.");

        Pageable top5 = PageRequest.of(0, 5);
        List<Producto> encontrados;
        try {
             encontrados = productoRepository.buscarFlexible(termino, top5).getContent();
        } catch (Exception e) {
             encontrados = productoRepository.findByNombreILike(termino, top5);
        }

        if (encontrados.isEmpty()) {
            return new BotResponse("🚫 Disculpa, no tenemos ese producto.\n\nIntenta con otro nombre o escribe *Hola* para volver.");
        }

        if (encontrados.size() == 1) {
            return evaluarStockProducto(encontrados.get(0), telefono, sesion);
        }

        usuarioOpcionesBusqueda.put(telefono, encontrados);
        actualizarEstado(sesion, "SELECCIONANDO_PRODUCTO");

        StringBuilder msg = new StringBuilder("🔎 *Encontré varias opciones:*\n");
        for (int i = 0; i < encontrados.size(); i++) {
            Producto p = encontrados.get(i);
            msg.append(String.format("\n%d️⃣ %s", i + 1, p.getNombre()));
        }
        msg.append("\n\n👇 *Escribe el número* del que buscas.");
        return new BotResponse(msg.toString());
    }

    private BotResponse procesarSeleccionProductoBuscado(String input, String telefono, Usuario usuario, SesionChat sesion) {
        List<Producto> opciones = usuarioOpcionesBusqueda.get(telefono);
        
        if (opciones == null || opciones.isEmpty()) {
            resetearSesion(telefono);
            return new BotResponse("⏳ Sesión expirada. Vuelve a buscar.");
        }

        try {
            int indice = Integer.parseInt(input) - 1;
            if (indice >= 0 && indice < opciones.size()) {
                Producto elegido = opciones.get(indice);
                return evaluarStockProducto(elegido, telefono, sesion);
            } else {
                return new BotResponse("⚠️ Número inválido. Elige una opción de la lista.");
            }
        } catch (NumberFormatException e) {
            return new BotResponse("⚠️ Por favor escribe solo el número.");
        }
    }

    private BotResponse evaluarStockProducto(Producto producto, String telefono, SesionChat sesion) {
        if (producto.getStockActual() > 0) {
            resetearSesion(telefono);
            return formatearRespuestaProducto(producto);
        } else {
            sesion.setEstadoActual("CONFIRMANDO_ESPERA");
            sesion.setUltimoProductoId(producto.getId());
            sesionRepository.save(sesion);
            
            return new BotResponse(
                "📦 *" + producto.getNombre() + "*\n" +
                "🔴 Estado: AGOTADO TEMPORALMENTE\n\n" +
                "¿Quieres que te anote en la lista de espera?\n\n" +
                "1️⃣ *Sí, avísame*\n" +
                "2️⃣ *No, gracias*"
            );
        }
    }

    private BotResponse procesarConfirmacionEspera(String input, String telefono, Usuario usuario, SesionChat sesion) {
        Long productoId = sesion.getUltimoProductoId();
        
        if (productoId == null) {
            resetearSesion(telefono);
            return new BotResponse("⏳ Error de sesión. Vuelve a buscar el producto.");
        }

        Producto producto = productoRepository.findById(productoId).orElse(null);

        if (input.contains("1") || input.toLowerCase().contains("si")) {
            boolean yaAnotado = listaEsperaRepository.existsByUsuarioAndProductoAndEstado(usuario, producto, EstadoListaEspera.PENDIENTE);
            
            if (!yaAnotado) {
                ListaEspera espera = new ListaEspera();
                espera.setUsuario(usuario);
                espera.setProducto(producto);
                espera.setFechaInscripcion(java.time.LocalDate.now());
                espera.setFechaSolicitud(LocalDateTime.now());
                espera.setEstado(EstadoListaEspera.PENDIENTE);
                listaEsperaRepository.save(espera);
            }
            resetearSesion(telefono);
            return new BotResponse("✅ ¡Listo! Te anoté. Te avisaremos cuando llegue *" + producto.getNombre() + "*.");
        } 
        else if (input.contains("2") || input.toLowerCase().contains("no")) {
            resetearSesion(telefono);
            return new BotResponse("👍 Entendido.");
        } 
        else {
            return new BotResponse("⚠️ Responde *1* (Sí) o *2* (No).");
        }
    }

    // --- 🎁 LÓGICA DE CANJE ---

    private BotResponse mostrarPremiosDisponibles(Usuario usuario, String telefono) {
        var saldoDTO = puntosService.getSaldoByEmail(usuario.getEmail());
        int puntosActuales = saldoDTO.getSaldoPuntos();
        
        List<Recompensa> premiosVisibles = recompensaRepository.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getActivo()) && r.getStock() > 0)
                .collect(Collectors.toList());

        usuarioOpcionesCanje.put(telefono, premiosVisibles);

        StringBuilder msg = new StringBuilder();
        msg.append("🏆 *Tienes ").append(puntosActuales).append(" Puntos*\n\n");
        
        if (premiosVisibles.isEmpty()) {
            msg.append("😓 No hay premios en stock.");
            resetearSesion(telefono);
        } else {
            msg.append("🎁 *Premios:*\n");
            for (int i = 0; i < premiosVisibles.size(); i++) {
                Recompensa r = premiosVisibles.get(i);
                msg.append(String.format("\n%d️⃣ %s *%s* (%d pts)", i + 1, "🎁", r.getDescripcion(), r.getPuntosRequeridos()));
            }
            msg.append("\n\n👇 *Escribe el número* para canjear.");
        }
        return new BotResponse(msg.toString());
    }

    private BotResponse procesarSeleccionPremio(String input, String telefono, Usuario usuario, SesionChat sesion) {
        List<Recompensa> opciones = usuarioOpcionesCanje.get(telefono);
        
        if (opciones == null || opciones.isEmpty()) {
            resetearSesion(telefono);
            return new BotResponse("⏳ Vuelve al menú.");
        }

        try {
            int indice = Integer.parseInt(input) - 1; 
            if (indice >= 0 && indice < opciones.size()) {
                Recompensa premioElegido = opciones.get(indice);
                return ejecutarCanje(usuario, premioElegido, telefono);
            } else {
                return new BotResponse("⚠️ Número inválido.");
            }
        } catch (NumberFormatException e) {
            return new BotResponse("⚠️ Escribe solo el número.");
        }
    }

    private BotResponse ejecutarCanje(Usuario usuario, Recompensa premio, String telefono) {
        var cuentaOpt = cuentaPuntosRepository.findByCliente(usuario);
        if (cuentaOpt.isEmpty() || cuentaOpt.get().getSaldoPuntos() < premio.getPuntosRequeridos()) {
             return new BotResponse("🚫 *Saldo insuficiente*.");
        }

        try {
            Cupon cupon = cuponService.crearCuponPorCanje(usuario, premio);
            CuentaPuntos cuenta = cuentaOpt.get();
            cuenta.setSaldoPuntos(cuenta.getSaldoPuntos() - premio.getPuntosRequeridos());
            cuentaPuntosRepository.save(cuenta);
            premio.setStock(premio.getStock() - 1);
            recompensaRepository.save(premio);

            resetearSesion(telefono);
            return new BotResponse("🎉 *¡CANJE EXITOSO!*\nCódigo: 👉 *" + cupon.getCodigo() + "*");
        } catch (Exception e) {
            return new BotResponse("🔥 Error técnico.");
        }
    }

    // --- MÉTODOS DE APOYO Y SESIÓN ---

    private SesionChat obtenerSesion(String telefono) {
        return sesionRepository.findByTelefono(telefono)
            .orElseGet(() -> {
                SesionChat nueva = new SesionChat();
                nueva.setTelefono(telefono);
                nueva.setEstadoActual("MENU");
                nueva.setFechaUltimaInteraccion(LocalDateTime.now());
                return sesionRepository.save(nueva);
            });
    }

    private void actualizarEstado(SesionChat sesion, String nuevoEstado) {
        sesion.setEstadoActual(nuevoEstado);
        sesion.setFechaUltimaInteraccion(LocalDateTime.now());
        sesionRepository.save(sesion);
    }

    private void resetearSesion(String telefono) {
        sesionRepository.findByTelefono(telefono).ifPresent(s -> {
            s.setEstadoActual("MENU");
            s.setUltimoProductoId(null);
            sesionRepository.save(s);
        });
        usuarioOpcionesCanje.remove(telefono);
        usuarioOpcionesBusqueda.remove(telefono);
    }

    private String normalizarTexto(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                         .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                         .replaceAll("[^a-zA-Z0-9\\s]", "").toLowerCase().trim();
    }

    private boolean detectarIntencion(String textoUsuario, List<String> palabrasClave) {
        return palabrasClave.stream().anyMatch(textoUsuario::contains);
    }

    private BotResponse formatearRespuestaProducto(Producto p) {
        String precioStr = (p.getPrecioVenta() != null) ? String.format("$%,.2f", p.getPrecioVenta().doubleValue()) : "Consultar";
        String imagen = (p.getImagenUrl() != null && p.getImagenUrl().startsWith("http")) ? p.getImagenUrl() : null;
        return new BotResponse("✅ *¡Sí hay stock!*\n📦 " + p.getNombre() + "\n💲 " + precioStr + "\n🟢 Disponibles: " + p.getStockActual() + "\n📍 Ven al local.", imagen);
    }
    
    // Método BLINDADO contra errores de DB para que el bot siga respondiendo
    private void registrarInteraccionSegura(String in, String out, Usuario u) {
        try {
            InteraccionChatbot i = new InteraccionChatbot();
            i.setFecha(LocalDateTime.now());
            i.setMensajeUsuario(in);
            i.setRespuestaBot(out);
            i.setUsuario(u); 
            interaccionRepository.save(i);
        } catch (Exception e) {
            // Logueamos el error pero NO lanzamos excepción para que el bot responda
            System.err.println("⚠️ No se pudo guardar el log del chat (ID null o tabla corrupta): " + e.getMessage());
        }
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