package com.masterserv.productos.service;

import com.masterserv.productos.dto.ConfirmacionPedidoDTO;
import com.masterserv.productos.dto.DetallePedidoDTO;
import com.masterserv.productos.dto.MovimientoStockDTO;
import com.masterserv.productos.dto.PedidoDTO;
import com.masterserv.productos.dto.PedidoDetalladoDTO;
import com.masterserv.productos.dto.PedidoFiltroDTO;
import com.masterserv.productos.entity.Cotizacion;
import com.masterserv.productos.entity.DetallePedido;
import com.masterserv.productos.entity.ItemCotizacion;
import com.masterserv.productos.entity.Pedido;
import com.masterserv.productos.entity.Producto;
import com.masterserv.productos.entity.Proveedor;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.enums.EstadoCotizacion;
import com.masterserv.productos.enums.EstadoItemCotizacion;
import com.masterserv.productos.enums.EstadoPedido;
import com.masterserv.productos.enums.TipoMovimiento;
import com.masterserv.productos.mapper.PedidoMapper;
import com.masterserv.productos.repository.CotizacionRepository;
import com.masterserv.productos.repository.ItemCotizacionRepository;
import com.masterserv.productos.repository.PedidoRepository;
import com.masterserv.productos.repository.ProductoRepository;
import com.masterserv.productos.repository.ProveedorRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import com.masterserv.productos.specification.PedidoSpecification;

import jakarta.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory; 
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.thymeleaf.TemplateEngine; 
import org.thymeleaf.context.Context; 

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID; 
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private static final Logger logger = LoggerFactory.getLogger(PedidoService.class);

    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ProductoService productoService; 
    @Autowired private PedidoMapper pedidoMapper;
    @Autowired private MovimientoStockService movimientoStockService;
    @Autowired private PedidoSpecification pedidoSpecification;

    @Autowired private ItemCotizacionRepository itemCotizacionRepository;
    @Autowired private CotizacionRepository cotizacionRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // --- INYECCIONES PARA NOTIFICACIÓN ---
    @Autowired private PdfService pdfService;
    @Autowired private EmailService emailService;
    @Autowired private TemplateEngine templateEngine;
    // -------------------------------------

    /**
     * Crea un Pedido Manual en estado PENDIENTE y NOTIFICA al proveedor.
     */
    @Transactional
    public PedidoDTO create(PedidoDTO pedidoDTO) {
        
        // 1. Cabecera del Pedido
        Pedido pedido = pedidoMapper.toPedido(pedidoDTO);
        pedido.setFechaPedido(LocalDateTime.now());
        pedido.setEstado(EstadoPedido.PENDIENTE); 
        
        // --- GENERAR TOKEN ÚNICO ---
        pedido.setToken(UUID.randomUUID().toString());
        // ---------------------------

        // 2. Buscar Entidades Relacionadas
        Proveedor proveedor = proveedorRepository.findById(pedidoDTO.getProveedorId())
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado"));
        Usuario usuario = usuarioRepository.findById(pedidoDTO.getUsuarioId())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        pedido.setProveedor(proveedor);
        pedido.setUsuario(usuario);

        // 3. Procesar Detalles
        Set<DetallePedido> detalles = new HashSet<>();
        BigDecimal totalPedido = BigDecimal.ZERO;

        for (DetallePedidoDTO detalleDTO : pedidoDTO.getDetalles()) {
            Producto producto = productoRepository.findById(detalleDTO.getProductoId())
                    .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + detalleDTO.getProductoId()));
            
            DetallePedido detalle = pedidoMapper.toDetallePedido(detalleDTO);
            detalle.setPedido(pedido);
            detalle.setProducto(producto);
            
            // Precio Manual o Costo Actual
            if (detalleDTO.getPrecioUnitario() != null) {
                detalle.setPrecioUnitario(detalleDTO.getPrecioUnitario());
            } else {
                detalle.setPrecioUnitario(producto.getPrecioCosto());
            }
            
            detalles.add(detalle);
            totalPedido = totalPedido.add(detalle.getPrecioUnitario().multiply(new BigDecimal(detalle.getCantidad())));
        }

        pedido.setDetalles(detalles);
        pedido.setTotalPedido(totalPedido);

        // 4. Guardar Pedido (Con token y estado pendiente)
        Pedido pedidoGuardado = pedidoRepository.save(pedido);
        
        // 5. --- NOTIFICAR AL PROVEEDOR ---
        try {
            if (proveedor.getEmail() != null && !proveedor.getEmail().isBlank()) {
                logger.info("📧 Generando Orden de Compra #{} para proveedor '{}'...", pedidoGuardado.getId(), proveedor.getRazonSocial());
                
                // A. Generar PDF
                byte[] pdfBytes = pdfService.generarOrdenCompraProveedor(pedidoGuardado);
                
                // B. Generar Link y HTML
                String linkConfirmacion = frontendUrl + "/proveedor/pedido/" + pedidoGuardado.getToken();
                
                Context context = new Context();
                context.setVariable("proveedorNombre", proveedor.getRazonSocial());
                context.setVariable("nroPedido", pedidoGuardado.getId());
                context.setVariable("linkConfirmacion", linkConfirmacion);
                
                String cuerpoHtml = templateEngine.process("email-orden-compra", context);
                String asunto = "Nueva Orden de Compra #" + pedidoGuardado.getId() + " - Masterserv";

                // C. Enviar Email
                emailService.enviarEmailConAdjunto(
                    proveedor.getEmail(),
                    asunto,
                    cuerpoHtml,
                    pdfBytes,
                    "Orden_Compra_" + pedidoGuardado.getId() + ".pdf"
                );
                
                logger.info("✅ Orden de Compra enviada exitosamente a: {}", proveedor.getEmail());
            } else {
                logger.warn("⚠️ Proveedor '{}' no tiene email registrado. No se envió la notificación.", proveedor.getRazonSocial());
            }
        } catch (Exception e) {
            logger.error("🔴 Error al notificar proveedor: {}", e.getMessage());
        }
        
        return pedidoMapper.toPedidoDTO(pedidoGuardado);
    }
    
    @Transactional
    public void marcarPedidoCompletado(Long pedidoId, String userEmail) {
        Usuario usuarioQueConfirma = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuario '" + userEmail + "' no encontrado."));

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado"));
        
        if (pedido.getEstado() != EstadoPedido.PENDIENTE && pedido.getEstado() != EstadoPedido.EN_CAMINO) {
            throw new IllegalStateException("Solo se pueden completar pedidos PENDIENTES o EN CAMINO. Estado actual: " + pedido.getEstado());
        }

        for (DetallePedido detalle : pedido.getDetalles()) { 
            Long productoId = detalle.getProducto().getId();
            int cantidadRecibida = detalle.getCantidad();
            
            productoService.reponerStock(productoId, cantidadRecibida); 

            MovimientoStockDTO movDto = new MovimientoStockDTO(
                    productoId,
                    usuarioQueConfirma.getId(), 
                    TipoMovimiento.ENTRADA_PEDIDO, 
                    cantidadRecibida,
                    "Recepción Pedido #" + pedido.getId() + " (" + pedido.getProveedor().getRazonSocial() + ")",
                    null,
                    pedidoId 
            );
            movimientoStockService.registrarMovimiento(movDto);
        }

        pedido.setEstado(EstadoPedido.COMPLETADO);
        pedidoRepository.save(pedido);
    }

    @Transactional
    public void marcarPedidoCancelado(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId) 
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado"));
        
        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden cancelar pedidos PENDIENTES.");
        }
        
        pedido.setEstado(EstadoPedido.CANCELADO);
        pedidoRepository.save(pedido);
    }

    @Transactional(readOnly = true)
    public PedidoDetalladoDTO obtenerDetallesPedido(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado: " + id));

        PedidoDetalladoDTO dto = new PedidoDetalladoDTO();
        dto.setId(pedido.getId());
        dto.setFechaPedido(pedido.getFechaPedido());
        dto.setEstado(pedido.getEstado());
        dto.setTotalPedido(pedido.getTotalPedido());
        
        if (pedido.getProveedor() != null) {
            dto.setProveedorId(pedido.getProveedor().getId());
            dto.setProveedorRazonSocial(pedido.getProveedor().getRazonSocial());
            dto.setProveedorCuit(pedido.getProveedor().getCuit());
            dto.setProveedorEmail(pedido.getProveedor().getEmail());
        }

        if (pedido.getUsuario() != null) {
            dto.setUsuarioSolicitante(pedido.getUsuario().getNombre() + " " + pedido.getUsuario().getApellido());
        }

        List<DetallePedidoDTO> detallesDTO = pedido.getDetalles().stream().map(d -> {
            DetallePedidoDTO det = new DetallePedidoDTO();
            det.setProductoId(d.getProducto().getId());
            det.setProductoNombre(d.getProducto().getNombre());
            det.setProductoCodigo(d.getProducto().getCodigo());
            det.setCantidad(d.getCantidad());
            det.setPrecioUnitario(d.getPrecioUnitario());
            
            if (d.getPrecioUnitario() != null) {
                det.setSubtotal(d.getPrecioUnitario().multiply(new BigDecimal(d.getCantidad())));
            } else {
                det.setSubtotal(BigDecimal.ZERO);
            }
            return det;
        }).collect(Collectors.toList());

        dto.setDetalles(detallesDTO);
        return dto;
    }

    @Transactional(readOnly = true)
    public Page<PedidoDTO> findAll(Pageable pageable) {
        Page<Pedido> pedidoPage = pedidoRepository.findAll(pageable);
        return pedidoPage.map(pedidoMapper::toPedidoDTO);
    }

    @Transactional(readOnly = true)
    public PedidoDTO findById(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado: " + id));
        return pedidoMapper.toPedidoDTO(pedido);
    }

    @Transactional
    public void confirmarPedidoPorProveedor(String token, ConfirmacionPedidoDTO dto) {
        Pedido pedido = pedidoRepository.findByToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado o token inválido."));

        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new IllegalStateException("Este pedido ya no está pendiente de confirmación.");
        }

        pedido.setFechaEntregaEstimada(dto.getFechaEntrega());

        BigDecimal nuevoTotal = BigDecimal.ZERO;
        
        if (dto.getItems() != null) {
            for (DetallePedido detalle : pedido.getDetalles()) {
                dto.getItems().stream()
                    .filter(item -> item.getProductoId().equals(detalle.getProducto().getId()))
                    .findFirst()
                    .ifPresent(item -> {
                        if (item.getNuevoPrecio() != null && item.getNuevoPrecio().compareTo(BigDecimal.ZERO) >= 0) {
                            detalle.setPrecioUnitario(item.getNuevoPrecio());
                        }
                    });
    
                nuevoTotal = nuevoTotal.add(detalle.getPrecioUnitario().multiply(new BigDecimal(detalle.getCantidad())));
            }
            pedido.setTotalPedido(nuevoTotal);
        }

        pedido.setEstado(EstadoPedido.EN_CAMINO); 

        pedidoRepository.save(pedido);
        logger.info("✅ Pedido #{} confirmado por proveedor. Llega el: {}", pedido.getId(), dto.getFechaEntrega());
    }

    @Transactional(readOnly = true)
    public Page<PedidoDTO> filter(PedidoFiltroDTO filtro, Pageable pageable) {
        var spec = pedidoSpecification.getByFilters(filtro);
        Page<Pedido> pedidosPage = pedidoRepository.findAll(spec, pageable);
        return pedidosPage.map(pedidoMapper::toPedidoDTO);
    }

    /**
     * 🚀 NUEVO MÉTODO CORREGIDO: Genera pedidos usando Precio de Costo si no hay oferta.
     */
    @Transactional
    public Map<String, Object> generarPedidosMasivos(List<Long> itemIds, Long usuarioId) {
        Usuario usuarioAdmin = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario Admin no encontrado"));

        List<ItemCotizacion> itemsSeleccionados = itemCotizacionRepository.findAllById(itemIds);
        
        if (itemsSeleccionados.isEmpty()) {
            throw new IllegalArgumentException("No se encontraron ítems seleccionados.");
        }

        Map<Cotizacion, List<ItemCotizacion>> itemsPorCotizacion = itemsSeleccionados.stream()
                .collect(Collectors.groupingBy(ItemCotizacion::getCotizacion));

        int pedidosCreados = 0;
        List<Long> pedidosIds = new ArrayList<>();

        for (Map.Entry<Cotizacion, List<ItemCotizacion>> entry : itemsPorCotizacion.entrySet()) {
            Cotizacion cotizacion = entry.getKey();
            List<ItemCotizacion> items = entry.getValue();

            Pedido pedido = new Pedido();
            pedido.setProveedor(cotizacion.getProveedor());
            pedido.setUsuario(usuarioAdmin);
            pedido.setFechaPedido(LocalDateTime.now());
            pedido.setEstado(EstadoPedido.PENDIENTE);
            pedido.setToken(UUID.randomUUID().toString());

            Set<DetallePedido> detallesPedido = new HashSet<>();
            BigDecimal total = BigDecimal.ZERO;

            for (ItemCotizacion item : items) {
                DetallePedido detalle = new DetallePedido();
                detalle.setPedido(pedido);
                detalle.setProducto(item.getProducto());
                detalle.setCantidad(item.getCantidadSolicitada());
                
                BigDecimal precio = item.getPrecioUnitarioOfertado() != null ? item.getPrecioUnitarioOfertado() 
                                   : (item.getProducto().getPrecioCosto() != null ? item.getProducto().getPrecioCosto() : BigDecimal.ZERO);

                detalle.setPrecioUnitario(precio);
                detallesPedido.add(detalle);
                total = total.add(precio.multiply(new BigDecimal(item.getCantidadSolicitada())));

                // --- 🧹 LÓGICA DE LIMPIEZA Y CIERRE ---
                // 1. Marcar el item seleccionado como COMPLETADO
                item.setEstado(EstadoItemCotizacion.COMPLETADO);

                // 2. Cancelar ofertas de otros proveedores para este mismo producto
                List<ItemCotizacion> rivales = itemCotizacionRepository.findItemsRivales(
                    item.getProducto().getId(), 
                    item.getId(), 
                    Arrays.asList(EstadoItemCotizacion.PENDIENTE)
                );
                for (ItemCotizacion rival : rivales) {
                    rival.setEstado(EstadoItemCotizacion.CANCELADO_SISTEMA);
                }
                itemCotizacionRepository.saveAll(rivales);
            }

            pedido.setDetalles(detallesPedido);
            pedido.setTotalPedido(total);
            Pedido pedidoGuardado = pedidoRepository.save(pedido);
            pedidosIds.add(pedidoGuardado.getId());
            pedidosCreados++;

            // Actualizar estado de la cotización padre
            cotizacion.setEstado(EstadoCotizacion.CONFIRMADA_ADMIN);
            cotizacionRepository.save(cotizacion);
        }

        // Guardar los cambios de estado de los items seleccionados
        itemCotizacionRepository.saveAll(itemsSeleccionados);

        return Map.of(
            "mensaje", "Se generaron " + pedidosCreados + " pedidos. Los productos seleccionados han sido procesados.",
            "cantidad", pedidosCreados,
            "pedidosIds", pedidosIds
        );
    }
}