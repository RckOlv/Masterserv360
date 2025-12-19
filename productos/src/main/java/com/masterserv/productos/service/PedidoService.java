package com.masterserv.productos.service;

import com.masterserv.productos.dto.ConfirmacionPedidoDTO;
import com.masterserv.productos.dto.DetallePedidoDTO;
import com.masterserv.productos.dto.MovimientoStockDTO;
import com.masterserv.productos.dto.PedidoDTO;
import com.masterserv.productos.dto.PedidoDetalladoDTO;
import com.masterserv.productos.entity.DetallePedido;
import com.masterserv.productos.entity.Pedido;
import com.masterserv.productos.entity.Producto;
import com.masterserv.productos.entity.Proveedor;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.enums.EstadoPedido;
import com.masterserv.productos.enums.TipoMovimiento;
import com.masterserv.productos.mapper.PedidoMapper;
import com.masterserv.productos.repository.PedidoRepository;
import com.masterserv.productos.repository.ProductoRepository;
import com.masterserv.productos.repository.ProveedorRepository;
import com.masterserv.productos.repository.UsuarioRepository;

import jakarta.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory; // <--- Importar Logger
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.thymeleaf.TemplateEngine; 
import org.thymeleaf.context.Context; 

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID; // <--- Importante para el Token
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
    
    // --- INYECCIONES PARA NOTIFICACIÓN ---
    @Autowired private PdfService pdfService;
    @Autowired private EmailService emailService;
    @Autowired private TemplateEngine templateEngine; // Para el HTML bonito
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
        
        // 5. --- LÓGICA DE VIDA REAL: NOTIFICAR AL PROVEEDOR ---
        try {
            if (proveedor.getEmail() != null && !proveedor.getEmail().isBlank()) {
                logger.info("📧 Generando Orden de Compra #{} para proveedor '{}'...", pedidoGuardado.getId(), proveedor.getRazonSocial());
                
                // A. Generar PDF (VERSIÓN PROVEEDOR - SIN PRECIOS)
                byte[] pdfBytes = pdfService.generarOrdenCompraProveedor(pedidoGuardado);
                
                // B. Generar Link y HTML
                // (Recuerda crear el endpoint y pantalla en Angular para /proveedor/pedido/{token})
                String linkConfirmacion = "http://localhost:4200/proveedor/pedido/" + pedidoGuardado.getToken();
                
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
            // No bloqueamos la creación del pedido si falla el email, solo logueamos.
        }
        // -------------------------------------------------------
        
        return pedidoMapper.toPedidoDTO(pedidoGuardado);
    }
    
    // ... (Métodos completar, cancelar, detalles, findAll, findById SE MANTIENEN IGUAL) ...
    // ... CÓPIALOS TAL CUAL ESTABAN EN TU CÓDIGO ORIGINAL ...
    
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

    /**
     * Procesa la respuesta del proveedor: actualiza fecha, precios y estado.
     */
    @Transactional
    public void confirmarPedidoPorProveedor(String token, ConfirmacionPedidoDTO dto) {
        // 1. Buscar pedido por token
        Pedido pedido = pedidoRepository.findByToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado o token inválido."));

        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new IllegalStateException("Este pedido ya no está pendiente de confirmación.");
        }

        // 2. Guardar la fecha prometida (¡Esto servirá para tus alertas!)
        pedido.setFechaEntregaEstimada(dto.getFechaEntrega());

        // 3. Actualizar Precios si cambiaron
        BigDecimal nuevoTotal = BigDecimal.ZERO;
        
        if (dto.getItems() != null) {
            for (DetallePedido detalle : pedido.getDetalles()) {
                // Buscamos si vino un precio nuevo para este ID de producto
                dto.getItems().stream()
                    .filter(item -> item.getProductoId().equals(detalle.getProducto().getId()))
                    .findFirst()
                    .ifPresent(item -> {
                        // Si mandó precio y es mayor a 0, actualizamos
                        if (item.getNuevoPrecio() != null && item.getNuevoPrecio().compareTo(BigDecimal.ZERO) >= 0) {
                            detalle.setPrecioUnitario(item.getNuevoPrecio());
                        }
                    });
    
                // Recalculamos el subtotal con el precio (viejo o nuevo)
                nuevoTotal = nuevoTotal.add(detalle.getPrecioUnitario().multiply(new BigDecimal(detalle.getCantidad())));
            }
            pedido.setTotalPedido(nuevoTotal);
        }

        // 4. Cambiar estado (Semáforo Verde)
        // Asegúrate de agregar EN_CAMINO a tu enum EstadoPedido
        pedido.setEstado(EstadoPedido.EN_CAMINO); 

        pedidoRepository.save(pedido);
        logger.info("✅ Pedido #{} confirmado por proveedor. Llega el: {}", pedido.getId(), dto.getFechaEntrega());
    }
}