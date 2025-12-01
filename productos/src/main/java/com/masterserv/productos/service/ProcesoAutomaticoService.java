package com.masterserv.productos.service;

import com.masterserv.productos.event.StockActualizadoEvent;
import com.masterserv.productos.entity.*;
import com.masterserv.productos.enums.EstadoCotizacion;
import com.masterserv.productos.enums.EstadoItemCotizacion;
import com.masterserv.productos.enums.EstadoListaEspera;
import com.masterserv.productos.repository.CotizacionRepository;
import com.masterserv.productos.repository.ListaEsperaRepository;
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
    private ListaEsperaRepository listaEsperaRepository;
    
    @Autowired
    private EmailService emailService;

    // --- MENTOR: INYECCIÓN NUEVA ---
    @Autowired(required = false) // Opcional por si no lo has creado aún, pero debería estar
    private WhatsappService whatsappService;
    // -------------------------------
    
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
     * Método helper que crea la Cotizacion y envía el email.
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
                
                // Usamos loteReposicion si mayor a 0
                int cantidadAPedir = (producto.getLoteReposicion() > 0) 
                ? producto.getLoteReposicion() 
                : (producto.getStockMinimo() * 2);

                item.setCantidadSolicitada(cantidadAPedir);
                item.setEstado(EstadoItemCotizacion.PENDIENTE);
                items.add(item);
            }
            cotizacion.setItems(items);

            // 3. Guardar
            Cotizacion cotizacionGuardada = cotizacionRepository.save(cotizacion);
            
            logger.info("-> ✉️ Cotización #{} creada para Proveedor '{}' ({} items).",
                cotizacionGuardada.getId(), proveedor.getRazonSocial(), items.size());

            // 4. Enviar Notificación
            String linkOferta = "http://localhost:4200/oferta/" + cotizacionGuardada.getToken();

            Context context = new Context();
            context.setVariable("proveedorNombre", proveedor.getRazonSocial());
            context.setVariable("linkOferta", linkOferta);
            context.setVariable("items", cotizacionGuardada.getItems());

            // Asegúrate de tener el template 'email-oferta.html'
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
     * Listener Asíncrono: Reacciona cuando entra stock de un producto.
     * Busca en la Lista de Espera y notifica a los clientes pendientes.
     */
    @Async
    @EventListener
    @Transactional // Necesario para actualizar el estado a NOTIFICADA
    public void handleStockActualizado(StockActualizadoEvent event) {
        
        // 1. Validaciones básicas: Solo nos interesa si AHORA hay stock positivo
        if (event.stockNuevo() <= 0) {
            return;
        }

        // 2. Recuperar el producto completo (necesario para el repositorio)
        Producto producto = productoRepository.findById(event.productoId()).orElse(null);
        if (producto == null) return;

        // 3. Buscar clientes que estén ESPERANDO este producto (Estado PENDIENTE)
        List<ListaEspera> esperas = listaEsperaRepository.findByProductoAndEstado(
                producto, 
                EstadoListaEspera.PENDIENTE
        );

        if (esperas.isEmpty()) {
            return;
        }

        logger.info("-> 📣 Encontrados {} clientes en lista de espera para '{}'. Notificando...", esperas.size(), producto.getNombre());

        // 4. Recorrer, Notificar y Actualizar Estado
        for (ListaEspera espera : esperas) {
            try {
                Usuario usuario = espera.getUsuario();
                
                // A. Construir mensaje
                String asunto = "¡Ya llegó! " + producto.getNombre() + " está disponible";
                String mensajeCuerpo = String.format(
                    "Hola %s,\n\nTe avisamos que el producto '%s' ya tiene stock nuevamente en Masterserv.\n\n¡No te quedes sin el tuyo!",
                    usuario.getNombre(), producto.getNombre()
                );

                // B. Enviar Email
                emailService.enviarEmailHtml(usuario.getEmail(), asunto, mensajeCuerpo);
                
                // C. --- MENTOR: ENVIAR WHATSAPP ---
                if (whatsappService != null && usuario.getTelefono() != null) {
                    String mensajeWhatsapp = String.format(
                        "👋 *¡Buenas noticias %s!*\n\n" +
                        "El producto *%s* que esperabas ya llegó. 🛵💨\n" +
                        "¡Vení a buscarlo!",
                        usuario.getNombre(), producto.getNombre()
                    );
                    whatsappService.enviarMensaje(usuario.getTelefono(), mensajeWhatsapp);
                    logger.info("-> 📱 WhatsApp enviado a {}", usuario.getTelefono());
                }
                // ----------------------------------

                logger.info("-> 📧 Notificación enviada a {}", usuario.getEmail());

                // D. Marcar como notificado para no volver a avisar en la próxima recarga
                espera.setEstado(EstadoListaEspera.NOTIFICADA);

            } catch (Exception e) {
                logger.error("Error al notificar usuario ID {}: {}", espera.getUsuario().getId(), e.getMessage());
            }
        }

        // 5. Guardar todos los cambios de estado en la base de datos
        listaEsperaRepository.saveAll(esperas);
        logger.info("-> ✅ Lista de espera actualizada para Producto ID {}.", event.productoId());
    }
}