package com.masterserv.productos.service;

import com.masterserv.productos.event.StockActualizadoEvent;
import com.masterserv.productos.entity.*;
import com.masterserv.productos.enums.EstadoCotizacion;
import com.masterserv.productos.enums.EstadoItemCotizacion;
import com.masterserv.productos.enums.EstadoListaEspera;
import com.masterserv.productos.enums.EstadoPedido; // Importante
import com.masterserv.productos.enums.EstadoUsuario;
import com.masterserv.productos.repository.CotizacionRepository;
import com.masterserv.productos.repository.ItemCotizacionRepository;
import com.masterserv.productos.repository.ListaEsperaRepository;
import com.masterserv.productos.repository.PedidoRepository; // Importante
import com.masterserv.productos.repository.ProductoRepository;
import com.masterserv.productos.repository.ProveedorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation; 
import org.springframework.transaction.annotation.Transactional;

import org.thymeleaf.TemplateEngine; 
import org.thymeleaf.context.Context; 

import java.time.LocalDate; // Importante
import java.util.*;
import java.util.stream.Collectors;

@Service
@EnableScheduling 
public class ProcesoAutomaticoService {

    private static final Logger logger = LoggerFactory.getLogger(ProcesoAutomaticoService.class);

    @Autowired private ProductoRepository productoRepository;
    @Autowired private CotizacionRepository cotizacionRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private ListaEsperaRepository listaEsperaRepository;
    @Autowired private EmailService emailService;
    @Autowired private TemplateEngine templateEngine; 
    @Autowired private ItemCotizacionRepository itemCotizacionRepository;
    @Autowired private WhatsappService whatsappService;
    
    // --- NUEVA INYECCIÓN PARA ALERTAS ---
    @Autowired private PedidoRepository pedidoRepository; 

    /**
     * TAREA 1: Genera pedidos automáticos (Stock Bajo).
     * EJECUCIÓN: Cada 10 MINUTOS.
     */
    @Scheduled(fixedDelay = 600000) 
    @Transactional
    public void generarPrePedidosAgrupados() {
        // ... (Tu lógica original intacta) ...
        logger.info("⏰ Iniciando ciclo de revisión automática de stock...");

        List<Producto> productosFaltantes = productoRepository.findAll().stream()
                .filter(p -> p.getStockActual() <= p.getStockMinimo())
                .collect(Collectors.toList());

        if (productosFaltantes.isEmpty()) {
            logger.info("✅ Todo el stock está en orden. Nada que pedir.");
            return;
        }

        Map<Categoria, List<Producto>> productosPorCategoria = productosFaltantes.stream()
                .collect(Collectors.groupingBy(Producto::getCategoria));

        for (Map.Entry<Categoria, List<Producto>> entry : productosPorCategoria.entrySet()) {
            Categoria categoria = entry.getKey();
            List<Producto> todosLosProductosDeLaCategoria = entry.getValue();

            List<Proveedor> proveedoresActivos = proveedorRepository.findByEstado(EstadoUsuario.ACTIVO);
            if (proveedoresActivos.isEmpty()) continue;

            for (Proveedor proveedor : proveedoresActivos) {
                if (!proveedorVendeCategoria(proveedor, categoria)) continue; 

                List<Producto> productosParaPedirHoy = new ArrayList<>();
                List<EstadoCotizacion> estadosActivos = Arrays.asList(
                    EstadoCotizacion.PENDIENTE_PROVEEDOR, 
                    EstadoCotizacion.RECIBIDA,            
                    EstadoCotizacion.CONFIRMADA_ADMIN     
                );

                for (Producto p : todosLosProductosDeLaCategoria) {
                    boolean yaPedido = itemCotizacionRepository.existePedidoActivo(p, proveedor, estadosActivos);
                    if (!yaPedido) {
                        productosParaPedirHoy.add(p);
                    }
                }

                if (!productosParaPedirHoy.isEmpty()) {
                    crearYNotificarCotizacion(proveedor, productosParaPedirHoy);
                }
            }
        }
        logger.info("🏁 Ciclo de revisión finalizado.");
    }

    /**
     * TAREA 2: ALERTA DE ARRIBOS (El Vigilante)
     * EJECUCIÓN: Todos los días a las 08:00 AM.
     */
    @Scheduled(cron = "0 0 8 * * *") 
    @Transactional
    public void verificarPedidosEnCamino() {
        logger.info("📅 [ALERTA DIARIA] Verificando arribos de mercadería...");

        LocalDate hoy = LocalDate.now();
        LocalDate manana = hoy.plusDays(1);

        // 1. BUSCAR PEDIDOS QUE LLEGAN HOY
        List<Pedido> lleganHoy = pedidoRepository.findByEstadoAndFechaEntregaEstimada(EstadoPedido.EN_CAMINO, hoy);
        
        if (!lleganHoy.isEmpty()) {
            logger.info("-> 🚚 ¡ATENCIÓN! Hoy llegan {} pedidos.", lleganHoy.size());
            notificarAdminArribo(lleganHoy, "¡Llegan HOY!", "Prepara el depósito, hoy recibimos mercadería de:");
        }

        // 2. BUSCAR PEDIDOS QUE LLEGAN MAÑANA
        List<Pedido> lleganManana = pedidoRepository.findByEstadoAndFechaEntregaEstimada(EstadoPedido.EN_CAMINO, manana);
        
        if (!lleganManana.isEmpty()) {
            logger.info("-> 📅 Aviso: Mañana llegan {} pedidos.", lleganManana.size());
            notificarAdminArribo(lleganManana, "Llegan Mañana", "Te aviso que para mañana esperamos pedidos de:");
        }
        
        if (lleganHoy.isEmpty() && lleganManana.isEmpty()) {
            logger.info("-> 😴 Nada programado para hoy ni mañana.");
        }
    }

    // --- Método auxiliar para enviar el mail al admin ---
    private void notificarAdminArribo(List<Pedido> pedidos, String titulo, String mensajeIntro) {
        // En un sistema real, este mail vendría de una config o de la tabla de usuarios con rol ADMIN
        String emailAdmin = "admin@masterserv360.com"; // Pon tu mail real aquí para probar

        StringBuilder cuerpo = new StringBuilder();
        cuerpo.append("<h2 style='color: #E41E26;'>").append(titulo).append("</h2>");
        cuerpo.append("<p>").append(mensajeIntro).append("</p><ul>");

        for (Pedido p : pedidos) {
            cuerpo.append("<li><strong>")
                  .append(p.getProveedor().getRazonSocial())
                  .append("</strong> (Orden #").append(p.getId()).append(")")
                  .append("</li>");
        }
        cuerpo.append("</ul>");
        cuerpo.append("<p>Ingresa al sistema para recepcionar la mercadería.</p>");

        emailService.enviarEmailHtml(emailAdmin, "📦 Alerta Stock: " + titulo, cuerpo.toString());
    }

    // ... (Resto de métodos auxiliares privados y Listener de stock SIN CAMBIOS) ...
    
    private boolean proveedorVendeCategoria(Proveedor proveedor, Categoria categoria) {
        if (proveedor.getCategorias() == null || proveedor.getCategorias().isEmpty()) return true; 
        return proveedor.getCategorias().stream().anyMatch(c -> c.getId().equals(categoria.getId()));
    }

    private void crearYNotificarCotizacion(Proveedor proveedor, List<Producto> productos) {
        if (proveedor.getEmail() == null || proveedor.getEmail().isBlank()) return;
        try {
            Cotizacion cotizacion = new Cotizacion();
            cotizacion.setProveedor(proveedor);
            cotizacion.setEstado(EstadoCotizacion.PENDIENTE_PROVEEDOR);
            cotizacion.setToken(UUID.randomUUID().toString()); 
            
            Set<ItemCotizacion> items = new HashSet<>();
            for (Producto producto : productos) {
                ItemCotizacion item = new ItemCotizacion();
                item.setCotizacion(cotizacion);
                item.setProducto(producto);
                int cant = (producto.getLoteReposicion() > 0) ? producto.getLoteReposicion() : Math.max(1, producto.getStockMinimo() * 2);
                item.setCantidadSolicitada(cant);
                item.setEstado(EstadoItemCotizacion.PENDIENTE);
                items.add(item);
            }
            cotizacion.setItems(items);
            Cotizacion guardada = cotizacionRepository.save(cotizacion);
            
            logger.info("-> 📨 [AUTO] Cotización #{} generada para '{}' con {} items.", 
                        guardada.getId(), proveedor.getRazonSocial(), items.size());

            String linkOferta = "http://localhost:4200/oferta/" + guardada.getToken();
            Context context = new Context();
            context.setVariable("proveedorNombre", proveedor.getRazonSocial());
            context.setVariable("linkOferta", linkOferta);
            context.setVariable("items", guardada.getItems()); 
            String html = templateEngine.process("email-oferta", context);
            emailService.enviarEmailHtml(proveedor.getEmail(), "Masterserv: Solicitud Cotización #" + guardada.getId(), html);
        } catch (Exception e) {
            logger.error("-> 🔴 Error cotización: {}", e.getMessage());
        }
    }

    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW) 
    public void handleStockActualizado(StockActualizadoEvent event) {
        // ... (Tu código de notificaciones que ya funcionaba bien) ...
        // (Resumido para brevedad, pero MANTENLO igual que antes)
        if (event.stockNuevo() <= 0) return;
        Producto producto = productoRepository.findById(event.productoId()).orElse(null);
        if (producto == null) return;
        
        List<ListaEspera> esperas = listaEsperaRepository.findByProductoAndEstado(producto, EstadoListaEspera.PENDIENTE);
        if (esperas.isEmpty()) return;

        for (ListaEspera espera : esperas) {
            try {
                Usuario usuario = espera.getUsuario();
                emailService.enviarEmailHtml(usuario.getEmail(), "¡Ya llegó! " + producto.getNombre(), "Hola " + usuario.getNombre() + ", ya hay stock.");
                if (whatsappService != null && usuario.getTelefono() != null) {
                    whatsappService.enviarMensaje(usuario.getTelefono(), "Hola " + usuario.getNombre() + ", llegó " + producto.getNombre());
                }
                espera.setEstado(EstadoListaEspera.NOTIFICADA);
            } catch (Exception e) {
                logger.error("Error notif: {}", e.getMessage());
            }
        }
        listaEsperaRepository.saveAll(esperas);
    }
}