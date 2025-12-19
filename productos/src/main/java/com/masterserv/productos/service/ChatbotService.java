package com.masterserv.productos.service;

import com.masterserv.productos.entity.*;
import com.masterserv.productos.enums.EstadoCupon;
import com.masterserv.productos.enums.EstadoListaEspera; // <--- IMPORTANTE
import com.masterserv.productos.repository.*;
import com.twilio.twiml.MessagingResponse;
import com.twilio.twiml.messaging.Message;

// import jakarta.annotation.PostConstruct; 
// import org.springframework.beans.factory.annotation.Value;
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

    // --- CREDENCIALES COMENTADAS (Lo maneja WhatsappService) ---
    // @Value("${twilio.account-sid}")
    // private String ACCOUNT_SID;
    // @Value("${twilio.auth-token}")
    // private String AUTH_TOKEN;
    // -----------------------------------------------------------

    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final InteraccionChatbotRepository interaccionRepository;
    private final PuntosService puntosService;
    private final SolicitudProductoRepository solicitudProductoRepository;
    private final RecompensaRepository recompensaRepository;
    private final CuponRepository cuponRepository;
    private final CuentaPuntosRepository cuentaPuntosRepository;
    
    // --- NUEVO REPOSITORIO ---
    private final ListaEsperaRepository listaEsperaRepository;

    public ChatbotService(UsuarioRepository usuarioRepository,
                          ProductoRepository productoRepository,
                          InteraccionChatbotRepository interaccionRepository,
                          PuntosService puntosService,
                          SolicitudProductoRepository solicitudProductoRepository,
                          RecompensaRepository recompensaRepository,
                          CuponRepository cuponRepository,
                          CuentaPuntosRepository cuentaPuntosRepository,
                          ListaEsperaRepository listaEsperaRepository) { // <--- INYECCIÓN
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.interaccionRepository = interaccionRepository;
        this.puntosService = puntosService;
        this.solicitudProductoRepository = solicitudProductoRepository;
        this.recompensaRepository = recompensaRepository;
        this.cuponRepository = cuponRepository;
        this.cuentaPuntosRepository = cuentaPuntosRepository;
        this.listaEsperaRepository = listaEsperaRepository; // <--- ASIGNACIÓN
    }

    // --- INIT COMENTADO (Lo maneja WhatsappService) ---
    // @PostConstruct
    // public void init() {
    //   try {
    //       Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
    //   } catch (Exception e) {
    //       System.err.println("Error inicializando Twilio: " + e.getMessage());
    //   }
    // }
    // --------------------------------------------------

    @Transactional
    public String procesarMensajeWebhook(String from, String body) {
        String telefono = from.replace("whatsapp:", "");
        Optional<Usuario> usuarioOpt = usuarioRepository.findByTelefono(telefono);
        
        registrarInteraccion(body, null, usuarioOpt.orElse(null));
        String mensajeRespuesta = procesarComando(body.trim(), usuarioOpt); 
        registrarInteraccion(null, mensajeRespuesta, usuarioOpt.orElse(null));
        
        return construirRespuestaTwiML(mensajeRespuesta);
    }

    private String procesarComando(String comando, Optional<Usuario> usuarioOpt) {
        String texto = comando.toLowerCase().trim();

        if (usuarioOpt.isEmpty()) {
            return "👋 ¡Hola! No te encontramos en nuestra base de datos.\nRegístrate en *masterserv.com* o contacta a un vendedor.";
        }

        Usuario usuario = usuarioOpt.get();

        // 1. SALUDO
        if (esSaludo(texto) || texto.contains("menu") || texto.contains("ayuda")) {
            return String.format(
                "👋 ¡Hola %s! Bienvenido a *Masterserv360* 🏍️\n\n" +
                "Soy tu asistente virtual. ¿Qué necesitas hoy?\n\n" +
                "🔍 *Buscar:* Escribe el nombre (ej: _\"precio bateria\"_)\n" + 
                "🎁 *Puntos:* Escribe _\"mis puntos\"_ para ver saldo\n" +
                "📝 *Pedir:* Escribe _\"solicitar [producto]\"_ para pedirlo\n",
                usuario.getNombre()
            );
        }

        // 2. PUNTOS Y RECOMPENSAS
        if (texto.contains("punto") || texto.contains("saldo") || texto.contains("premio")) {
            var saldoDTO = puntosService.getSaldoByEmail(usuario.getEmail());
            int puntosActuales = saldoDTO.getSaldoPuntos();

            List<Recompensa> recompensas = recompensaRepository.findAll(); 
            
            StringBuilder msg = new StringBuilder();
            msg.append(String.format("🏆 *Tienes %d Puntos*\nCanjealos por:\n", puntosActuales));

            for (Recompensa r : recompensas) {
                String estado = (puntosActuales >= r.getPuntosRequeridos()) ? "✅" : "🔒";
                msg.append(String.format("\n🎁 %s *%s* (%d pts)", estado, r.getDescripcion(), r.getPuntosRequeridos()));
            }

            msg.append("\n\n👉 Para canjear escribe: _\"canjear [nombre premio]\"_");
            return msg.toString();
        }

        // 3. CANJEAR
        if (texto.startsWith("canjear")) {
            String nombrePremio = limpiarPrefijo(texto);
            if (nombrePremio.isEmpty()) return "⚠️ Por favor escribe el nombre del premio.";
            return procesarCanje(usuario, nombrePremio);
        }

        // =====================================================================
        // 4. SOLICITAR (LÓGICA MEJORADA)
        // =====================================================================
        if (texto.startsWith("solicitar") || texto.startsWith("pedir") || texto.startsWith("quiero")) {
            String termino = limpiarPrefijo(texto); 
            if (termino.length() < 3) return "⚠️ Dime qué producto necesitas. Ej: _\"solicitar espejo\"_";
            
            // A. ¿EL PRODUCTO YA EXISTE? Buscamos algo que se parezca en el catálogo
            Optional<Producto> productoExistente = productoRepository.findByNombreContainingIgnoreCase(termino)
                    .stream().findFirst();

            if (productoExistente.isPresent()) {
                Producto p = productoExistente.get();
                
                // B. SI EXISTE, TE METEMOS EN LA LISTA DE ESPERA (AUTOMÁTICA)
                boolean yaEnEspera = listaEsperaRepository.existsByUsuarioIdAndProductoIdAndEstado(
                        usuario.getId(), p.getId(), EstadoListaEspera.PENDIENTE);

                if (yaEnEspera) {
                    return "📋 Ya estás en la lista de espera para *" + p.getNombre() + "*. Te avisaremos por aquí cuando llegue.";
                }

                ListaEspera espera = new ListaEspera();
                espera.setUsuario(usuario);
                espera.setProducto(p);
                espera.setFechaInscripcion(LocalDate.now());
                espera.setEstado(EstadoListaEspera.PENDIENTE);
                listaEsperaRepository.save(espera);

                return String.format(
                    "✅ El producto *%s* existe en catálogo (Stock: %d).\n" +
                    "🔔 ¡Listo! Te he agregado a la **Lista de Espera** automática.\n" +
                    "Recibirás un WhatsApp en cuanto entre mercadería.", 
                    p.getNombre(), p.getStockActual()
                );
            } else {
                // C. SI NO EXISTE, GUARDAMOS LA NOTA PARA EL ADMIN (MANUAL)
                SolicitudProducto s = new SolicitudProducto(termino, usuario);
                solicitudProductoRepository.save(s);
                return "📝 No encontré ese producto en catálogo, pero generé una solicitud de: '" + termino;
            }
        }
        // =====================================================================

        // 5. STOCK Y PRECIOS
        if (texto.length() > 3 || texto.startsWith("stock") || texto.startsWith("precio")) {
            String termino = limpiarPrefijo(texto);
            return termino.isEmpty() ? "Dime qué producto buscas." : buscarProducto(termino);
        }

        return "🤔 No entendí tu consulta. Escribe *ayuda* para ver el menú.";
    }

    private String buscarProducto(String termino) {
        // A. Buscar por Código Exacto
        Optional<Producto> productoPorCodigo = productoRepository.findByCodigo(termino.toUpperCase());
        if (productoPorCodigo.isPresent()) {
            return formatearRespuestaProducto(productoPorCodigo.get());
        }

        // B. Buscar por Nombre (Top 5)
        Pageable top5 = PageRequest.of(0, 5); 
        List<Producto> productos = productoRepository.findByNombreILike(termino, top5);

        if (productos.isEmpty()) {
            return String.format(
                "❌ No encontré *%s* en el catálogo.\n\n" +
                "💡 ¿Quieres que lo pidamos para ti?\n" +
                "Escribe: _\"solicitar %s\"_", 
                termino, termino
            );
        
        } else if (productos.size() == 1) {
            return formatearRespuestaProducto(productos.get(0));
        
        } else {
            StringBuilder respuesta = new StringBuilder("🔎 *Encontré estas opciones:*\n");
            for (Producto p : productos) {
                respuesta.append(String.format("\n▪ %s ($%,.0f)", p.getNombre(), p.getPrecioVenta()));
            }
            respuesta.append("\n\nPara ver detalles, escribe el nombre exacto o el código.");
            return respuesta.toString();
        }
    }

    private String formatearRespuestaProducto(Producto p) {
        String disponibilidad;
        if (p.getStockActual() <= 0) {
            disponibilidad = "🔴 *Sin Stock*";
        } else if (p.getStockActual() <= p.getStockMinimo()) {
            disponibilidad = "🟡 *Últimas Unidades*";
        } else {
            disponibilidad = "🟢 *Disponible*";
        }

        return String.format(
            "📦 *%s*\nCódigo: %s\nEstado: %s\nPrecio: *$%,.2f*",
            p.getNombre(), p.getCodigo(), disponibilidad, p.getPrecioVenta()
        );
    }

    private String procesarCanje(Usuario usuario, String nombrePremio) {
        Optional<Recompensa> recompensaOpt = recompensaRepository.findByDescripcionContainingIgnoreCase(nombrePremio)
                .stream().findFirst();

        if (recompensaOpt.isEmpty()) return "❌ No encontré el premio \"" + nombrePremio + "\". Revisa el nombre en el menú de *puntos*.";

        Recompensa recompensa = recompensaOpt.get();

        if (recompensa.getStock() <= 0) return "😔 El premio *" + recompensa.getDescripcion() + "* está agotado por el momento.";

        var cuentaOpt = cuentaPuntosRepository.findByCliente(usuario);
        
        if (cuentaOpt.isEmpty() || cuentaOpt.get().getSaldoPuntos() < recompensa.getPuntosRequeridos()) {
            return "🚫 *Puntos insuficientes* para canjear este premio.";
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

            return String.format(
                "🎉 *¡CANJE EXITOSO!*\nPremio: *%s*\nCódigo: *%s*\n\nPresenta este código en la caja para usarlo. 🛵",
                recompensa.getDescripcion(), cupon.getCodigo()
            );

        } catch (Exception e) {
            return "🔴 Ocurrió un error procesando el canje. Intenta más tarde.";
        }
    }
    
    private String generarCodigoCupon(String nombre) {
        String prefix = nombre.length() > 3 ? nombre.substring(0, 3).toUpperCase() : "PRM";
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
    }

    private boolean esSaludo(String t) {
        return t.contains("hola") || t.contains("buen") || t.contains("hi");
    }

    private String limpiarPrefijo(String texto) {
        String[] prefijos = {"stock", "precio de", "precio", "solicitar", "pedir", "canjear", "quiero"};
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

    private String construirRespuestaTwiML(String mensaje) {
        return new MessagingResponse.Builder().message(new Message.Builder(mensaje).build()).build().toXml();
    }
}