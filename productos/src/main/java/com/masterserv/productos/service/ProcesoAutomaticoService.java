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

import org.thymeleaf.TemplateEngine; 
import org.thymeleaf.context.Context; 

import java.util.*;

@Service
@EnableScheduling 
public class ProcesoAutomaticoService {

    private static final Logger logger = LoggerFactory.getLogger(ProcesoAutomaticoService.class);

    @Autowired
    private ProductoRepository productoRepository;
    
    @Autowired
    private CotizacionRepository cotizacionRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private TemplateEngine templateEngine; 

    /**
     * TAREA PROGRAMADA (CRON):
     * Se ejecuta 10 segundos después de iniciar la app (para pruebas)
     * y luego cada 1 hora.
     */
    @Scheduled(initialDelay = 10000, fixedRate = 3600000) 
    @Transactional
    public void generarPrePedidosAgrupados() {
        logger.info("--- 🕑 INICIANDO TAREA PROGRAMADA: Generar Pre-Pedidos Agrupados ---");

        // 1. Buscar Faltantes (RF-21)
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
                mapaProveedorProductos
                        .computeIfAbsent(proveedor, k -> new ArrayList<>())
                        .add(producto);
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
     * UTILIZA LA NUEVA LÓGICA DE LOTE DE REPOSICIÓN.
     */
    private void crearYNotificarCotizacion(Proveedor proveedor, List<Producto> productos) {
        if (proveedor.getEmail() == null || proveedor.getEmail().isBlank()) {
            logger.warn("-> 🟡 Proveedor '{}' no tiene email. Omitiendo cotización.", proveedor.getRazonSocial());
            return;
        }

        try {
            // 1. Crear la Cotizacion (Padre)
            Cotizacion cotizacion = new Cotizacion();
            cotizacion.setProveedor(proveedor);
            cotizacion.setEstado(EstadoCotizacion.PENDIENTE_PROVEEDOR);
            cotizacion.setToken(UUID.randomUUID().toString()); 
            
            // 2. Crear los Items (Hijos)
            Set<ItemCotizacion> items = new HashSet<>();
            for (Producto producto : productos) {
                ItemCotizacion item = new ItemCotizacion();
                item.setCotizacion(cotizacion);
                item.setProducto(producto);
                
                // --- Mentor: INICIO DE LA MODIFICACIÓN ---
                // Reemplazamos (producto.getStockMinimo() * 2) 
                // por el valor flexible que definimos en el Admin.
                int cantidadAPedir = producto.getLoteReposicion(); 
                // --- Mentor: FIN DE LA MODIFICACIÓN ---
                
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
            
            String linkOferta = "http://localhost:4200/oferta/" + cotizacionGuardada.getToken();

            Context context = new Context();
            context.setVariable("proveedorNombre", proveedor.getRazonSocial());
            context.setVariable("linkOferta", linkOferta);
            context.setVariable("items", cotizacionGuardada.getItems());

            String cuerpoHtml = templateEngine.process("email-oferta", context);

            emailService.enviarEmailHtml(
                proveedor.getEmail(),
                "Masterserv: Solicitud de Cotización #" + cotizacionGuardada.getId(), 
                cuerpoHtml
            );

        } catch (Exception e) {
            logger.error("-> 🔴 Error al crear cotización para Proveedor '{}': {}", 
                proveedor.getRazonSocial(), e.getMessage());
        }
    }

    /**
     * Listener Asíncrono (para futura lógica de WhatsApp).
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