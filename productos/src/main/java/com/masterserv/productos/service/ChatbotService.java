package com.masterserv.productos.service;

import com.masterserv.productos.entity.*;
import com.masterserv.productos.enums.EstadoCupon;
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
import java.util.List;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChatbotService {

    // --- CONFIGURACIÓN ---
    private static final String LINK_REGISTRO = "https://masterserv360.vercel.app/auth/registro"; 
    // ---------------------

    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final InteraccionChatbotRepository interaccionRepository;
    private final PuntosService puntosService;
    private final SolicitudProductoRepository solicitudProductoRepository;
    private final RecompensaRepository recompensaRepository;
    private final CuponRepository cuponRepository;
    private final CuentaPuntosRepository cuentaPuntosRepository;
    private final ListaEsperaRepository listaEsperaRepository;

    public ChatbotService(UsuarioRepository usuarioRepository,
                          ProductoRepository productoRepository,
                          InteraccionChatbotRepository interaccionRepository,
                          PuntosService puntosService,
                          SolicitudProductoRepository solicitudProductoRepository,
                          RecompensaRepository recompensaRepository,
                          CuponRepository cuponRepository,
                          CuentaPuntosRepository cuentaPuntosRepository,
                          ListaEsperaRepository listaEsperaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.interaccionRepository = interaccionRepository;
        this.puntosService = puntosService;
        this.solicitudProductoRepository = solicitudProductoRepository;
        this.recompensaRepository = recompensaRepository;
        this.cuponRepository = cuponRepository;
        this.cuentaPuntosRepository = cuentaPuntosRepository;
        this.listaEsperaRepository = listaEsperaRepository;
    }

    // Clase auxiliar interna para manejar Texto + Imagen
    private static class BotResponse {
        String texto;
        String mediaUrl; // URL de la imagen (puede ser null)

        public BotResponse(String texto) { this.texto = texto; }
        public BotResponse(String texto, String mediaUrl) { this.texto = texto; this.mediaUrl = mediaUrl; }
    }

    @Transactional
    public String procesarMensajeWebhook(String from, String body) {
        System.out.println("--- 📩 WHATSAPP ENTRANTE ---");
        
        String telefono = from.replace("whatsapp:", "").trim();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByTelefono(telefono);
        
        // Registrar Entrada (Protegido)
        try { registrarInteraccion(body, null, usuarioOpt.orElse(null)); } catch (Exception e) {}

        // Procesar Lógica
        BotResponse respuesta;
        try {
            respuesta = procesarComando(body.trim(), usuarioOpt);
        } catch (Exception e) {
            e.printStackTrace();
            respuesta = new BotResponse("😓 Tuve un pequeño problema técnico procesando eso. Por favor intenta de nuevo.");
        }
        
        // Registrar Salida (Protegido)
        try { registrarInteraccion(null, respuesta.texto, usuarioOpt.orElse(null)); } catch (Exception e) {}
        
        // Devolver XML a Twilio
        return construirRespuestaTwiML(respuesta);
    }

    private BotResponse procesarComando(String comando, Optional<Usuario> usuarioOpt) {
        String texto = comando.toLowerCase().trim();

        // 1. USUARIO NO REGISTRADO
        if (usuarioOpt.isEmpty()) {
            return new BotResponse(
                "👋 *¡Hola! Bienvenido a Masterserv360*\n\n" +
                "No veo tu número registrado en mi sistema. Para ver precios y stock, regístrate gratis aquí:\n\n" +
                "👉 " + LINK_REGISTRO + "\n\n" +
                "Una vez registrado, escríbeme \"Hola\" nuevamente. 🚀"
            );
        }

        Usuario usuario = usuarioOpt.get();

        // 2. SALUDO / MENÚ PRINCIPAL
        if (esSaludo(texto) || texto.contains("menu") || texto.equals("ayuda")) {
            return new BotResponse(
                String.format(
                    "👋 ¡Hola *%s*! Soy el asistente virtual de Masterserv. 🏍️\n\n" +
                    "Escribe la palabra clave:\n\n" +
                    "1️⃣ *Buscar [Producto]*\n" +
                    "   _(Ej: \"buscar aceite\", \"bateria\")_\n\n" +
                    "2️⃣ *Mis Puntos*\n" +
                    "   _(Ver saldo y premios)_\n\n" +
                    "3️⃣ *Solicitar [Nombre]*\n" +
                    "   _(Pedir algo que no encuentras)_\n\n" +
                    "❓ *Ayuda* - Ver este menú",
                    usuario.getNombre()
                )
            );
        }

        // 3. PUNTOS Y RECOMPENSAS
        if (texto.contains("punto") || texto.contains("saldo") || texto.contains("premio")) {
            var saldoDTO = puntosService.getSaldoByEmail(usuario.getEmail());
            int puntosActuales = saldoDTO.getSaldoPuntos();
            List<Recompensa> recompensas = recompensaRepository.findAll(); 
            
            StringBuilder msg = new StringBuilder();
            msg.append(String.format("🏆 *Tus Puntos: %d*\n\n🎁 *Premios Disponibles:*\n", puntosActuales));

            for (Recompensa r : recompensas) {
                String estado = (puntosActuales >= r.getPuntosRequeridos()) ? "✅" : "🔒";
                msg.append(String.format("\n%s *%s* (%d pts)", estado, r.getDescripcion(), r.getPuntosRequeridos()));
            }
            msg.append("\n\nEscribe *\"canjear [nombre]\"* para obtener tu cupón.");
            return new BotResponse(msg.toString());
        }

        // 4. CANJEAR
        if (texto.startsWith("canjear")) {
            String nombrePremio = limpiarPrefijo(texto);
            if (nombrePremio.isEmpty()) return new BotResponse("⚠️ Escribe el nombre del premio. Ej: *canjar 10% OFF Aceites*");
            return new BotResponse(procesarCanje(usuario, nombrePremio));
        }

        // 5. SOLICITAR / PEDIR (Lista de Espera)
        if (texto.startsWith("solicitar") || texto.startsWith("pedir") || texto.startsWith("quiero")) {
            return procesarSolicitud(usuario, limpiarPrefijo(texto));
        }

        // 6. BUSCADOR INTELIGENTE (Detecta intención de búsqueda implícita)
        // Si escribe algo largo y no es un comando, asumimos que busca un producto
        if (texto.length() > 2) {
            String termino = limpiarPrefijo(texto);
            if (!termino.isEmpty()) {
                return buscarProducto(termino);
            }
        }

        return new BotResponse("🤔 No entendí. Escribe *ayuda* para ver las opciones.");
    }

    private BotResponse buscarProducto(String termino) {
        // A. Buscar por Código Exacto
        Optional<Producto> productoPorCodigo = productoRepository.findByCodigo(termino.toUpperCase());
        if (productoPorCodigo.isPresent()) {
            return formatearRespuestaProducto(productoPorCodigo.get());
        }

        // B. Buscar por Nombre "Flexible" (Ignora acentos y mayúsculas)
        Pageable top5 = PageRequest.of(0, 5); 
        List<Producto> productos;
        try {
            // Intenta usar la búsqueda con unaccent (si la BD lo soporta)
            productos = productoRepository.findByNombreFlexible(termino, top5);
        } catch (Exception e) {
            // Fallback: Si la BD no tiene la extensión, usamos ILIKE normal
            System.err.println("⚠️ Fallback búsqueda (posiblemente falta extensión unaccent): " + e.getMessage());
            productos = productoRepository.findByNombreILike(termino, top5);
        }

        if (productos.isEmpty()) {
            return new BotResponse(
                "❌ No encontré nada parecido a *\"" + termino + "\"*.\n\n" +
                "📝 ¿Quieres solicitarlo?\nEscribe: *\"solicitar " + termino + "\"*"
            );
        } else if (productos.size() == 1) {
            // ¡Bingo! Un solo resultado -> Mostramos foto y detalle
            return formatearRespuestaProducto(productos.get(0));
        } else {
            // Múltiples resultados -> Lista de texto
            StringBuilder respuesta = new StringBuilder("🔎 *Encontré varias opciones:*\n");
            for (Producto p : productos) {
                // Formateo seguro de precio para lista
                String precio = (p.getPrecioVenta() != null) ? String.format("$%,.0f", p.getPrecioVenta().doubleValue()) : "Consultar";
                respuesta.append(String.format("\n▪ %s (%s)", p.getNombre(), precio));
            }
            respuesta.append("\n\nPara ver la foto y stock, escribe el nombre completo.");
            return new BotResponse(respuesta.toString());
        }
    }

    private BotResponse formatearRespuestaProducto(Producto p) {
        String disponibilidad;
        if (p.getStockActual() <= 0) {
            disponibilidad = "🔴 Sin Stock";
        } else if (p.getStockActual() <= p.getStockMinimo()) {
            disponibilidad = "🟡 Pocas Unidades (" + p.getStockActual() + ")";
        } else {
            disponibilidad = "🟢 Disponible (" + p.getStockActual() + ")";
        }

        // CORRECCIÓN: Convertir a doubleValue() para evitar IllegalFormatConversionException
        String precioStr = "Consultar";
        if (p.getPrecioVenta() != null) {
            precioStr = String.format("$%,.2f", p.getPrecioVenta().doubleValue());
        }

        String texto = String.format(
            "📦 *%s*\n\n" +
            "💲 Precio: *%s*\n" +
            "📊 Estado: %s\n" +
            "🏷️ Código: %s\n\n" +
            p.getNombre(), precioStr, disponibilidad, p.getCodigo()
        );

        // Si el producto tiene foto (y es una URL válida http...), la preparamos
        String imagen = (p.getImagenUrl() != null && p.getImagenUrl().startsWith("http")) 
                        ? p.getImagenUrl() : null;

        return new BotResponse(texto, imagen);
    }

    private BotResponse procesarSolicitud(Usuario usuario, String termino) {
        if (termino.length() < 3) return new BotResponse("⚠️ Escribe qué producto necesitas.");

        // Verificar si existe realmente (usando búsqueda flexible)
        Pageable top1 = PageRequest.of(0, 1);
        List<Producto> matches;
        try {
             matches = productoRepository.findByNombreFlexible(termino, top1);
        } catch (Exception e) {
             matches = productoRepository.findByNombreILike(termino, top1);
        }

        if (!matches.isEmpty()) {
            Producto p = matches.get(0);
            boolean yaEnEspera = listaEsperaRepository.existsByUsuarioIdAndProductoIdAndEstado(
                    usuario.getId(), p.getId(), EstadoListaEspera.PENDIENTE);

            if (yaEnEspera) return new BotResponse("📋 Ya estás en la lista de espera para *" + p.getNombre() + "*.");

            ListaEspera espera = new ListaEspera();
            espera.setUsuario(usuario);
            espera.setProducto(p);
            espera.setFechaInscripcion(LocalDate.now());
            espera.setEstado(EstadoListaEspera.PENDIENTE);
            listaEsperaRepository.save(espera);

            return new BotResponse("🔔 Te avisare cuando entre stock de: *" + p.getNombre() + "*");
        } else {
            SolicitudProducto s = new SolicitudProducto(termino, usuario);
            solicitudProductoRepository.save(s);
            return new BotResponse("📝 Anotado. Le pasaré tu pedido de *\"" + termino + "\"* al encargado de compras.");
        }
    }

    private String procesarCanje(Usuario usuario, String nombrePremio) {
        Optional<Recompensa> recompensaOpt = recompensaRepository.findByDescripcionContainingIgnoreCase(nombrePremio)
                .stream().findFirst();
        
        if (recompensaOpt.isEmpty()) return "❌ Premio no encontrado.";
        Recompensa recompensa = recompensaOpt.get();
        
        var cuentaOpt = cuentaPuntosRepository.findByCliente(usuario);
        if (cuentaOpt.isEmpty() || cuentaOpt.get().getSaldoPuntos() < recompensa.getPuntosRequeridos()) {
             return "🚫 Puntos insuficientes.";
        }
        
        try {
            CuentaPuntos cuenta = cuentaOpt.get();
            cuenta.setSaldoPuntos(cuenta.getSaldoPuntos() - recompensa.getPuntosRequeridos());
            cuentaPuntosRepository.save(cuenta);
            recompensa.setStock(recompensa.getStock() - 1);
            recompensaRepository.save(recompensa);
            
            Cupon cupon = new Cupon();
            cupon.setCodigo(generarCodigoCupon(recompensa.getDescripcion()));
            cupon.setValor(recompensa.getValor());
            cupon.setTipoDescuento(recompensa.getTipoDescuento());
            cupon.setCategoria(recompensa.getCategoria());
            cupon.setFechaVencimiento(LocalDate.now().plusDays(30));
            cupon.setEstado(EstadoCupon.VIGENTE);
            cupon.setCliente(usuario);
            cuponRepository.save(cupon);
            
            return "🎉 *CUPÓN GENERADO*: " + cupon.getCodigo();
        } catch (Exception e) {
            return "Error al canjear.";
        }
    }

    // --- UTILIDADES ---
    private String generarCodigoCupon(String nombre) {
        String prefix = nombre.length() > 3 ? nombre.substring(0, 3).toUpperCase() : "PRM";
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
    }

    private boolean esSaludo(String t) {
        return t.equals("hola") || t.equals("hi") || t.equals("buen dia") || t.equals("buenas") || t.equals("menu");
    }

    private String limpiarPrefijo(String texto) {
        String[] prefijos = {"buscar", "precio de", "precio", "solicitar", "pedir", "canjear", "quiero", "ver"};
        for (String prefijo : prefijos) {
            if (texto.startsWith(prefijo)) return texto.substring(prefijo.length()).trim();
        }
        return texto; 
    }
    
    private void registrarInteraccion(String in, String out, Usuario u) {
        InteraccionChatbot i = new InteraccionChatbot();
        i.setFecha(LocalDateTime.now());
        i.setMensajeUsuario(in);
        i.setRespuestaBot(out);
        i.setUsuario(u); 
        interaccionRepository.save(i);
    }

    // --- CONSTRUCCIÓN XML CON IMAGEN ---
    private String construirRespuestaTwiML(BotResponse respuesta) {
        Message.Builder messageBuilder = new Message.Builder();
        
        // 1. Agregar Cuerpo del Mensaje
        messageBuilder.body(new Body.Builder(respuesta.texto).build());

        // 2. Agregar Imagen (si existe)
        if (respuesta.mediaUrl != null && !respuesta.mediaUrl.isEmpty()) {
            messageBuilder.media(new Media.Builder(respuesta.mediaUrl).build());
        }

        return new MessagingResponse.Builder()
                .message(messageBuilder.build())
                .build()
                .toXml();
    }
}