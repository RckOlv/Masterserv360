package com.masterserv.productos.service;

import com.masterserv.productos.event.StockActualizadoEvent;
import com.masterserv.productos.entity.*;
import com.masterserv.productos.enums.EstadoCotizacion;
import com.masterserv.productos.enums.EstadoItemCotizacion;
import com.masterserv.productos.enums.EstadoListaEspera;
import com.masterserv.productos.enums.EstadoUsuario; // Importante
import com.masterserv.productos.repository.CotizacionRepository;
import com.masterserv.productos.repository.ListaEsperaRepository;
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
import org.springframework.transaction.annotation.Transactional;

import org.thymeleaf.TemplateEngine; 
import org.thymeleaf.context.Context; 

import java.util.*;
import java.util.stream.Collectors;

@Service
@EnableScheduling 
public class ProcesoAutomaticoService {

    private static final Logger logger = LoggerFactory.getLogger(ProcesoAutomaticoService.class);

    @Autowired
    private ProductoRepository productoRepository;
    
    @Autowired
    private CotizacionRepository cotizacionRepository;

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private ListaEsperaRepository listaEsperaRepository;
    
    @Autowired
    private EmailService emailService;

    @Autowired(required = false) 
    private WhatsappService whatsappService;
    
    @Autowired
    private TemplateEngine templateEngine; 

    /**
     * TAREA PROGRAMADA (CRON):
     * Se ejecuta cada 10 segundos para DEMOSTRACIÓN.
     * Incluye lógica anti-spam y filtrado inteligente de proveedores.
     */
    @Scheduled(fixedDelay = 10000) 
    @Transactional
    public void generarPrePedidosAgrupados() {
        
        // 1. Buscar Faltantes
        List<Producto> productosFaltantes = productoRepository.findAll().stream()
                .filter(p -> p.getStockActual() <= p.getStockMinimo())
                .collect(Collectors.toList());

        if (productosFaltantes.isEmpty()) {
            return;
        }

        // 2. AGRUPAR por Categoría
        Map<Categoria, List<Producto>> productosPorCategoria = productosFaltantes.stream()
                .collect(Collectors.groupingBy(Producto::getCategoria));

        // 3. Procesar cada grupo
        for (Map.Entry<Categoria, List<Producto>> entry : productosPorCategoria.entrySet()) {
            Categoria categoriaRequerida = entry.getKey();
            List<Producto> productosDeLaCategoria = entry.getValue();

            // --- CORRECCIÓN SENIOR #1: Solo Proveedores ACTIVOS ---
            // No gastamos recursos en proveedores inactivos
            List<Proveedor> proveedoresActivos = proveedorRepository.findByEstado(EstadoUsuario.ACTIVO);

            if (proveedoresActivos.isEmpty()) {
                logger.warn("⚠️ No hay proveedores ACTIVOS registrados en el sistema.");
                continue;
            }

            for (Proveedor proveedor : proveedoresActivos) {
                
                // --- CORRECCIÓN SENIOR #2: Validar Especialidad ---
                // Solo cotizamos si el proveedor vende esta categoría específica.
                // (Asumiendo que Proveedor tiene una relación ManyToMany con Categoria llamada 'categorias')
                if (!proveedorVendeCategoria(proveedor, categoriaRequerida)) {
                    continue; // Este proveedor no vende lo que necesitamos, saltar.
                }

                // --- FRENO DE MANO (ANTI-SPAM) ---
                boolean yaTienePendiente = cotizacionRepository.existsByProveedorAndEstado(
                        proveedor, EstadoCotizacion.PENDIENTE_PROVEEDOR
                );

                if (!yaTienePendiente) {
                    crearYNotificarCotizacion(proveedor, productosDeLaCategoria);
                }
            }
        }
    }

    /**
     * Helper para verificar si un proveedor maneja una categoría.
     * Esto evita pedirle "Aceite" a un proveedor de "Neumáticos".
     */
    private boolean proveedorVendeCategoria(Proveedor proveedor, Categoria categoria) {
        if (proveedor.getCategorias() == null || proveedor.getCategorias().isEmpty()) {
            // Si no tiene categorías asignadas, asumimos que es "Generalista" y le mandamos todo.
            // O podrías ser estricto y devolver false. Para tu tesis, true es más seguro.
            return true; 
        }
        // Verifica si la lista de categorías del proveedor contiene la que buscamos
        return proveedor.getCategorias().stream()
                .anyMatch(c -> c.getId().equals(categoria.getId()));
    }

    private void crearYNotificarCotizacion(Proveedor proveedor, List<Producto> productos) {
        if (proveedor.getEmail() == null || proveedor.getEmail().isBlank()) {
            logger.warn("-> 🟡 Proveedor '{}' no tiene email. Omitiendo cotización.", proveedor.getRazonSocial());
            return;
        }

        try {
            // 1. Crear la Cotizacion
            Cotizacion cotizacion = new Cotizacion();
            cotizacion.setProveedor(proveedor);
            cotizacion.setEstado(EstadoCotizacion.PENDIENTE_PROVEEDOR);
            cotizacion.setToken(UUID.randomUUID().toString()); 
            
            // 2. Crear los Items
            Set<ItemCotizacion> items = new HashSet<>();
            for (Producto producto : productos) {
                ItemCotizacion item = new ItemCotizacion();
                item.setCotizacion(cotizacion);
                item.setProducto(producto);
                
                int cantidadAPedir = (producto.getLoteReposicion() > 0) 
                        ? producto.getLoteReposicion() 
                        : Math.max(1, producto.getStockMinimo() * 2);

                item.setCantidadSolicitada(cantidadAPedir);
                item.setEstado(EstadoItemCotizacion.PENDIENTE);
                items.add(item);
            }
            cotizacion.setItems(items);

            // 3. Guardar
            Cotizacion cotizacionGuardada = cotizacionRepository.save(cotizacion);
            
            logger.info("-> ✅ [AUTO] Cotización #{} generada para Proveedor '{}' ({} items).",
                cotizacionGuardada.getId(), proveedor.getRazonSocial(), items.size());

            // 4. Enviar Notificación
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
            logger.error("-> 🔴 Error al crear cotización automática: {}", e.getMessage());
        }
    }

    // ... (Mantén el método handleStockActualizado igual que antes) ...
    @Async
    @EventListener
    @Transactional 
    public void handleStockActualizado(StockActualizadoEvent event) {
        // ... (Tu código existente del listener está perfecto) ...
        if (event.stockNuevo() <= 0) return;

        Producto producto = productoRepository.findById(event.productoId()).orElse(null);
        if (producto == null) return;

        List<ListaEspera> esperas = listaEsperaRepository.findByProductoAndEstado(
                producto, 
                EstadoListaEspera.PENDIENTE
        );

        if (esperas.isEmpty()) return;

        logger.info("-> 📣 Encontrados {} clientes en lista de espera para '{}'. Notificando...", esperas.size(), producto.getNombre());

        for (ListaEspera espera : esperas) {
            try {
                Usuario usuario = espera.getUsuario();
                
                String asunto = "¡Ya llegó! " + producto.getNombre() + " está disponible";
                String mensajeCuerpo = String.format(
                    "Hola %s,\n\nTe avisamos que el producto '%s' ya tiene stock nuevamente en Masterserv.\n\n¡No te quedes sin el tuyo!",
                    usuario.getNombre(), producto.getNombre()
                );

                emailService.enviarEmailHtml(usuario.getEmail(), asunto, mensajeCuerpo);
                
                if (whatsappService != null && usuario.getTelefono() != null) {
                    String mensajeWhatsapp = String.format(
                        "👋 *¡Buenas noticias %s!*\n\n" +
                        "El producto *%s* que esperabas ya llegó. 🛵💨\n" +
                        "¡Vení a buscarlo!",
                        usuario.getNombre(), producto.getNombre()
                    );
                    whatsappService.enviarMensaje(usuario.getTelefono(), mensajeWhatsapp);
                }

                espera.setEstado(EstadoListaEspera.NOTIFICADA);

            } catch (Exception e) {
                logger.error("Error al notificar usuario ID {}: {}", espera.getUsuario().getId(), e.getMessage());
            }
        }

        listaEsperaRepository.saveAll(esperas);
        logger.info("-> ✅ Lista de espera procesada para Producto ID {}.", event.productoId());
    }
}