package com.masterserv.productos.service;

import com.masterserv.productos.dto.DetallePedidoDTO;
import com.masterserv.productos.dto.MovimientoStockDTO;
import com.masterserv.productos.dto.PedidoDTO;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.Hibernate; // Necesario para Lazy Loading (aunque aquí no se use)

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private ProveedorRepository proveedorRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private ProductoRepository productoRepository;
    
    // --- Mentor: ¡NUEVA INYECCIÓN! ---
    @Autowired
    private ProductoService productoService; 
    // ---------------------------------
    
    @Autowired
    private PedidoMapper pedidoMapper;
    @Autowired
    private MovimientoStockService movimientoStockService; 

    @Transactional
    public PedidoDTO create(PedidoDTO pedidoDTO) {
        
        // 1. Convertir DTO a Entidad Pedido (cabecera)
        Pedido pedido = pedidoMapper.toPedido(pedidoDTO);
        pedido.setFechaPedido(LocalDateTime.now());
        pedido.setEstado(EstadoPedido.PENDIENTE); // Todos los pedidos nacen PENDIENTES

        // 2. Buscar las entidades reales (Proveedor y Usuario)
        Proveedor proveedor = proveedorRepository.findById(pedidoDTO.getProveedorId())
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
        Usuario usuario = usuarioRepository.findById(pedidoDTO.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        pedido.setProveedor(proveedor);
        pedido.setUsuario(usuario);

        // 3. Procesar los detalles
        Set<DetallePedido> detalles = new HashSet<>();
        BigDecimal totalPedido = BigDecimal.ZERO;

        for (DetallePedidoDTO detalleDTO : pedidoDTO.getDetalles()) {
            // Buscar el producto real
            Producto producto = productoRepository.findById(detalleDTO.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + detalleDTO.getProductoId()));
            
            // Crear la entidad DetallePedido
            DetallePedido detalle = pedidoMapper.toDetallePedido(detalleDTO);
            detalle.setPedido(pedido); // Vincular al padre
            detalle.setProducto(producto);
            
            // Congelar el precio de costo del producto en el detalle
            detalle.setPrecioUnitario(producto.getPrecioCosto());
            
            detalles.add(detalle);

            // Sumar al total
            totalPedido = totalPedido.add(detalle.getPrecioUnitario().multiply(new BigDecimal(detalle.getCantidad())));
        }

        pedido.setDetalles(detalles);
        pedido.setTotalPedido(totalPedido);

        // 4. Guardar el Pedido (y los Detalles en cascada)
        Pedido pedidoGuardado = pedidoRepository.save(pedido);
        
        // 5. Devolver el DTO completo
        return pedidoMapper.toPedidoDTO(pedidoGuardado);
    }
    
    /**
     * Mentor: MÉTODO CORREGIDO
     * Marca un pedido como COMPLETADO y actualiza el stock.
     */
    @Transactional
    public void marcarPedidoCompletado(Long pedidoId, String userEmail) {
        
        // 1. Buscamos al usuario que está confirmando la operación
        Usuario usuarioQueConfirma = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario '" + userEmail + "' no encontrado. No se puede confirmar el pedido."));

        // 2. Buscamos el pedido
        Pedido pedido = pedidoRepository.findByIdWithDetails(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        
        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new RuntimeException("Solo se pueden completar pedidos PENDIENTES");
        }

        // 3. Iterar sobre los detalles y aumentar el stock
        for (DetallePedido detalle : pedido.getDetalles()) { 
            
            Long productoId = detalle.getProducto().getId();
            int cantidad = detalle.getCantidad();
            
            // 3a. ¡CORRECCIÓN CRÍTICA! Llama al servicio que ACTIVA la reposición
            productoService.reponerStock(productoId, cantidad); 

            // 3b. Registramos el movimiento (LOG)
            MovimientoStockDTO movDto = new MovimientoStockDTO(
                    productoId,
                    usuarioQueConfirma.getId(), 
                    TipoMovimiento.ENTRADA_PEDIDO,
                    cantidad,
                    "Recepción de Pedido #" + pedido.getId(),
                    null,
                    pedidoId // Vinculamos el ID del pedido
            );
            
            movimientoStockService.registrarMovimiento(movDto);
        }

        // 4. Actualizar estado del pedido
        pedido.setEstado(EstadoPedido.COMPLETADO);
        pedidoRepository.save(pedido);
    }

    /**
     * Marca un pedido como CANCELADO. (No revierte stock, asume que nunca llegó).
     */
    @Transactional
    public void marcarPedidoCancelado(Long pedidoId) {
        // ... (Tu método queda igual) ...
        Pedido pedido = pedidoRepository.findById(pedidoId) 
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        
        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new RuntimeException("Solo se pueden cancelar pedidos PENDIENTES");
        }
        
        pedido.setEstado(EstadoPedido.CANCELADO);
        pedidoRepository.save(pedido);
    }

    /**
     * Obtiene todos los pedidos de forma paginada.
     */
    @Transactional(readOnly = true)
    public Page<PedidoDTO> findAll(Pageable pageable) {
        Page<Pedido> pedidoPage = pedidoRepository.findAll(pageable);
        return pedidoPage.map(pedidoMapper::toPedidoDTO);
    }

    @Transactional(readOnly = true)
    public PedidoDTO findById(Long id) {
        Pedido pedido = pedidoRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado: " + id));
        return pedidoMapper.toPedidoDTO(pedido);
    }
}