package com.masterserv.productos.service;

import com.masterserv.productos.event.StockActualizadoEvent;
import com.masterserv.productos.entity.*;
import com.masterserv.productos.enums.*;
import com.masterserv.productos.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.util.*;

@Service
@EnableScheduling
@RequiredArgsConstructor
public class ProcesoAutomaticoService {

    private static final Logger logger = LoggerFactory.getLogger(ProcesoAutomaticoService.class);

    private final ProductoRepository productoRepository;
    private final CotizacionRepository cotizacionRepository;
    private final ProveedorRepository proveedorRepository;
    private final ListaEsperaRepository listaEsperaRepository;
    private final EmailService emailService;
    private final TemplateEngine templateEngine;
    private final ItemCotizacionRepository itemCotizacionRepository;
    private final WhatsappService whatsappService;
    private final PedidoRepository pedidoRepository;

    /**
     * 🟢 TAREA 1: Generar pedidos automáticos (AGRUPADO POR PROVEEDOR).
     * Ejecución: Cada 10 minutos.
     */
    @Scheduled(fixedDelay = 600000) 
    public void generarPrePedidosAgrupados() {
        logger.info("⏰ [AUTO] Iniciando ciclo de reabastecimiento...");

        // PASO 1: Operación Transaccional (Rápida - Solo DB)
        List<Cotizacion> cotizacionesParaNotificar = crearCotizacionesEnTransaccion();

        // PASO 2: Operación de Red (Lenta - Envío de Emails)
        // Se hace fuera de la transacción para no bloquear la conexión a la BD mientras se envían correos.
        if (!cotizacionesParaNotificar.isEmpty()) {
            logger.info("📨 Iniciando envío de {} solicitudes agrupadas...", cotizacionesParaNotificar.size());
            for (Cotizacion cotizacion : cotizacionesParaNotificar) {
                notificarProveedor(cotizacion);
            }
        } else {
            logger.info("✅ Todo el stock está en orden o ya fue solicitado.");
        }
    }

    /**
     * Método transaccional HELPER: Solo interactúa con la Base de Datos.
     */
    @Transactional
    protected List<Cotizacion> crearCotizacionesEnTransaccion() {
        // Buscamos productos con stock bajo (Asegúrate de tener este @Query en el Repo)
        List<Producto> productosFaltantes = productoRepository.findProductosConStockBajo(); 

        if (productosFaltantes.isEmpty()) return Collections.emptyList();

        List<Proveedor> proveedoresActivos = proveedorRepository.findByEstado(EstadoUsuario.ACTIVO);
        
        // CORRECCIÓN AQUI: Eliminado el código duplicado y la llave abierta
        if (proveedoresActivos.isEmpty()) {
            logger.warn("⚠️ No hay proveedores activos para reponer stock.");
            return Collections.emptyList();
        }

        List<Cotizacion> nuevasCotizaciones = new ArrayList<>();

        for (Proveedor proveedor : proveedoresActivos) {
            List<Producto> productosParaEsteProveedor = new ArrayList<>();

            for (Producto p : productosFaltantes) {
                // A. ¿Vende esta categoría?
                if (proveedorVendeCategoria(proveedor, p.getCategoria())) {
                    
                    // B. ¿Ya se pidió? (Evitamos duplicados)
                    boolean yaPedido = itemCotizacionRepository.existePedidoActivo(
                        p, proveedor, 
                        Arrays.asList(EstadoCotizacion.PENDIENTE_PROVEEDOR, EstadoCotizacion.RECIBIDA, EstadoCotizacion.CONFIRMADA_ADMIN)
                    );

                    if (!yaPedido) {
                        productosParaEsteProveedor.add(p);
                    }
                }
            }

            // C. Si hay productos, creamos UNA cotización agrupada
            if (!productosParaEsteProveedor.isEmpty()) {
                Cotizacion nueva = guardarCotizacion(proveedor, productosParaEsteProveedor);
                nuevasCotizaciones.add(nueva);
                logger.info("📦 Cotización #{} creada para '{}' con {} items.", 
                    nueva.getId(), proveedor.getRazonSocial(), productosParaEsteProveedor.size());
            }
        }
        return nuevasCotizaciones;
    }

    // Helper privado para guardar en BD
    // Helper privado para guardar en BD
    private Cotizacion guardarCotizacion(Proveedor proveedor, List<Producto> productos) {
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setProveedor(proveedor);
        cotizacion.setEstado(EstadoCotizacion.PENDIENTE_PROVEEDOR);
        cotizacion.setToken(UUID.randomUUID().toString());
        cotizacion.setFechaCreacion(java.time.LocalDateTime.now()); 

        Set<ItemCotizacion> items = new HashSet<>();
        for (Producto producto : productos) {
            ItemCotizacion item = new ItemCotizacion();
            item.setCotizacion(cotizacion);
            item.setProducto(producto);
            int cant = (producto.getLoteReposicion() > 0) 
                      ? producto.getLoteReposicion() 
                      : Math.max(1, producto.getStockMinimo() * 2);
                      
            item.setCantidadSolicitada(cant);
            item.setEstado(EstadoItemCotizacion.PENDIENTE);
            items.add(item);
        }
        cotizacion.setItems(items);
        return cotizacionRepository.save(cotizacion);
    }
    // Helper para enviar email (Sin Transactional)
    private void notificarProveedor(Cotizacion cotizacion) {
        Proveedor proveedor = cotizacion.getProveedor();
        if (proveedor.getEmail() == null || proveedor.getEmail().isBlank()) return;

        try {
            // Ajusta la URL a tu dominio real o variable de entorno
            String linkOferta = "https://masterserv360.vercel.app/oferta/" + cotizacion.getToken();

            Context context = new Context();
            context.setVariable("proveedorNombre", proveedor.getRazonSocial());
            context.setVariable("linkOferta", linkOferta);
            context.setVariable("items", cotizacion.getItems());

            String html = templateEngine.process("email-oferta", context);
            
            emailService.enviarEmailHtml(proveedor.getEmail(), 
                "Masterserv: Solicitud Cotización #" + cotizacion.getId(), html);
            
            logger.info("-> 📨 Email enviado exitosamente a {}", proveedor.getRazonSocial());

        } catch (Exception e) {
            logger.error("-> 🔴 Error enviando email a {}: {}", proveedor.getRazonSocial(), e.getMessage());
        }
    }

    /**
     * 🟢 TAREA 2: ALERTA DE ARRIBOS (El Vigilante)
     * Ejecución: 08:00 AM diario.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional(readOnly = true)
    public void verificarPedidosEnCamino() {
        logger.info("📅 [ALERTA DIARIA] Verificando arribos de mercadería...");

        LocalDate hoy = LocalDate.now();
        LocalDate manana = hoy.plusDays(1);

        List<Pedido> lleganHoy = pedidoRepository.findByEstadoAndFechaEntregaEstimada(EstadoPedido.EN_CAMINO, hoy);
        if (!lleganHoy.isEmpty()) {
            notificarAdminArribo(lleganHoy, "¡Llegan HOY!", "Prepara el depósito, hoy recibimos mercadería de:");
        }

        List<Pedido> lleganManana = pedidoRepository.findByEstadoAndFechaEntregaEstimada(EstadoPedido.EN_CAMINO, manana);
        if (!lleganManana.isEmpty()) {
            notificarAdminArribo(lleganManana, "Llegan Mañana", "Te aviso que para mañana esperamos pedidos de:");
        }
        
        if (lleganHoy.isEmpty() && lleganManana.isEmpty()) {
            logger.info("-> 😴 Nada programado para hoy ni mañana.");
        }
    }

    private void notificarAdminArribo(List<Pedido> pedidos, String titulo, String mensajeIntro) {
        String emailAdmin = "admin@masterserv360.com"; 
        
        try {
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
        } catch (Exception e) {
            logger.error("Error notificando admin: {}", e.getMessage());
        }
    }

    /**
     * 🟢 TAREA 3: LISTA DE ESPERA (Reactiva)
     * Se ejecuta cuando entra stock (evento).
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStockActualizado(StockActualizadoEvent event) {
        if (event.stockNuevo() <= 0) return;
        
        procesarListaEspera(event.productoId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void procesarListaEspera(Long productoId) {
        Producto producto = productoRepository.findById(productoId).orElse(null);
        if (producto == null) return;

        List<ListaEspera> esperas = listaEsperaRepository.findByProductoAndEstado(producto, EstadoListaEspera.PENDIENTE);
        if (esperas.isEmpty()) return;

        logger.info("-> 📣 Notificando a {} clientes en espera por '{}'...", esperas.size(), producto.getNombre());

        for (ListaEspera espera : esperas) {
            try {
                Usuario usuario = espera.getUsuario();
                
                // Email
                emailService.enviarEmailHtml(usuario.getEmail(), 
                    "¡Ya llegó! " + producto.getNombre(), 
                    "Hola " + usuario.getNombre() + ", tu producto ya está disponible.");

                // WhatsApp
                if (whatsappService != null && usuario.getTelefono() != null) {
                    whatsappService.enviarMensaje(usuario.getTelefono(), 
                        "👋 Hola " + usuario.getNombre() + ", buenas noticias: Llegó " + producto.getNombre());
                }

                espera.setEstado(EstadoListaEspera.NOTIFICADA);
            } catch (Exception e) {
                logger.error("Error notificando usuario {}: {}", espera.getUsuario().getEmail(), e.getMessage());
            }
        }
        listaEsperaRepository.saveAll(esperas);
    }

    private boolean proveedorVendeCategoria(Proveedor proveedor, Categoria categoria) {
        if (proveedor.getCategorias() == null || proveedor.getCategorias().isEmpty()) return true;
        return proveedor.getCategorias().stream()
                .anyMatch(c -> c.getId().equals(categoria.getId()));
    }
}