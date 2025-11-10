package com.masterserv.productos.service;

import com.masterserv.productos.event.StockActualizadoEvent;
import com.masterserv.productos.entity.*;
import com.masterserv.productos.enums.EstadoCotizacion;
import com.masterserv.productos.enums.EstadoItemCotizacion;
import com.masterserv.productos.repository.CotizacionRepository;
import com.masterserv.productos.repository.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// --- ¡NUEVOS IMPORTS! ---
import org.thymeleaf.TemplateEngine; // Para procesar el HTML
import org.thymeleaf.context.Context;    // Para las variables del HTML
// -----------------------

import java.util.*;

@Service
@EnableScheduling // Habilita las tareas programadas
public class ProcesoAutomaticoService {

    private static final Logger logger = LoggerFactory.getLogger(ProcesoAutomaticoService.class);

    @Autowired
    private ProductoRepository productoRepository;
    
    @Autowired
    private CotizacionRepository cotizacionRepository;
    
    // --- ¡NUEVAS INYECCIONES! ---
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private TemplateEngine templateEngine; // Inyectado gracias al starter-thymeleaf
    // ----------------------------

    /**
     * TAREA PROGRAMADA (CRON):
     * Se ejecuta 10 segundos después de iniciar la app (para pruebas)
     * y luego cada 1 hora.
     */
    @Scheduled(initialDelay = 10000, fixedRate = 3600000) 
    @Transactional
    public void generarPrePedidosAgrupados() {
        logger.info("--- 🕑 INICIANDO TAREA PROGRAMADA: Generar Pre-Pedidos Agrupados ---");

        // 1. Buscar Faltantes
        List<Producto> productosFaltantes = productoRepository.findProductosConStockBajo();

        if (productosFaltantes.isEmpty()) {
            logger.info("--- ✅ Tarea finalizada: No hay productos con stock bajo. ---");
            return;
        }

        logger.warn("--- ⚠️ Se encontraron {} productos con stock bajo. Agrupando... ---", productosFaltantes.size());

        // 2. Agrupación por Proveedor
        Map<Proveedor, List<Producto>> mapaProveedorProductos = new HashMap<>();
        for (Producto producto : productosFaltantes) {
            Set<Proveedor> proveedores = producto.getCategoria().getProveedores();
            for (Proveedor proveedor : proveedores) {
                // (Opcional: Chequear que el proveedor esté ACTIVO)
                // if ("ACTIVO".equals(proveedor.getEstado())) {
                    mapaProveedorProductos
                        .computeIfAbsent(proveedor, k -> new ArrayList<>())
                        .add(producto);
                // }
            }
        }

        logger.info("--- 📊 Agrupación completada. Se generarán {} cotizaciones. ---", mapaProveedorProductos.size());
        
        // 3. Crear y Notificar las Cotizaciones
        for (Map.Entry<Proveedor, List<Producto>> entry : mapaProveedorProductos.entrySet()) {
            Proveedor proveedor = entry.getKey();
            List<Producto> productosParaEsteProveedor = entry.getValue();
            
            crearYNotificarCotizacion(proveedor, productosParaEsteProveedor);
        }

        logger.info("--- ✅ Tarea finalizada: Todas las cotizaciones fueron generadas. ---");
    }

    /**
     * Método helper que crea la Cotizacion Y AHORA envía el email.
     */
    private void crearYNotificarCotizacion(Proveedor proveedor, List<Producto> productos) {
        // Validación: No enviar si el proveedor no tiene email
        if (proveedor.getEmail() == null || proveedor.getEmail().isBlank()) {
            logger.warn("-> 🟡 Proveedor '{}' no tiene email. Omitiendo cotización.", proveedor.getRazonSocial());
            return;
        }

        try {
            // 1. Crear la Cotizacion (Padre)
            Cotizacion cotizacion = new Cotizacion();
            cotizacion.setProveedor(proveedor);
            cotizacion.setEstado(EstadoCotizacion.PENDIENTE_PROVEEDOR);
            cotizacion.setToken(UUID.randomUUID().toString()); // ¡El link secreto!
            
            // 2. Crear los Items (Hijos)
            Set<ItemCotizacion> items = new HashSet<>();
            for (Producto producto : productos) {
                ItemCotizacion item = new ItemCotizacion();
                item.setCotizacion(cotizacion);
                item.setProducto(producto);
                int cantidadAPedir = producto.getStockMinimo() * 2; // (Tu lógica de cuánto pedir)
                item.setCantidadSolicitada(cantidadAPedir);
                item.setEstado(EstadoItemCotizacion.PENDIENTE);
                items.add(item);
            }
            cotizacion.setItems(items);

            // 3. Guardar (CascadeType.ALL guarda el padre y los hijos)
            Cotizacion cotizacionGuardada = cotizacionRepository.save(cotizacion);
            
            logger.info("-> ✉️ Cotización #{} creada para Proveedor '{}' ({} items).",
                cotizacionGuardada.getId(), proveedor.getRazonSocial(), items.size());

            // --- 4. ¡ENVIAR NOTIFICACIÓN! ---
            
            // URL para la prueba local (apunta a tu 'ng serve')
            String linkOferta = "http://localhost:4200/oferta/" + cotizacionGuardada.getToken();

            // Creamos el contexto (las variables para la plantilla HTML)
            Context context = new Context();
            context.setVariable("proveedorNombre", proveedor.getRazonSocial());
            context.setVariable("linkOferta", linkOferta);
            context.setVariable("items", cotizacionGuardada.getItems());

            // Procesamos la plantilla HTML (email-oferta.html)
            String cuerpoHtml = templateEngine.process("email-oferta", context);

            // ¡Enviamos el email! (Asíncrono)
            emailService.enviarEmailHtml(
                proveedor.getEmail(), // El email real del proveedor
                "Masterserv: Solicitud de Cotización #" + cotizacionGuardada.getId(), 
                cuerpoHtml
            );

        } catch (Exception e) {
            logger.error("-> 🔴 Error al crear cotización para Proveedor '{}': {}", 
                proveedor.getRazonSocial(), e.getMessage());
        }
    }

    /**
     * Este es el EventListener que hicimos para el "stock en tiempo real".
     * Lo dejamos aquí para la futura lógica de "Lista de Espera" de WhatsApp.
     */
    @Async
    @EventListener
    public void handleStockActualizado(StockActualizadoEvent event) {
        logger.info("-> 🟢 [EVENTO RECIBIDO ASYNC] Stock actualizado para Producto ID {} (Lógica de pre-pedido movida a @Scheduled)", event.productoId());
        
        boolean estabaAgotado = event.stockAnterior() <= 0;
        boolean ahoraHayStock = event.stockNuevo() > 0;

        if (estabaAgotado && ahoraHayStock) {
            logger.info("-> 🔵 ¡PRODUCTO REPUESTO! Producto ID {} ahora tiene stock. ¡NOTIFICANDO LISTA DE ESPERA!", event.productoId());
            // (Futuro) whatsAppListaEsperaService.notificarClientesEnEspera(event.productoId());
        }
    }
}