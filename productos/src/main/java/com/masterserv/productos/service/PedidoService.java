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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Autowired
    private PedidoMapper pedidoMapper;
    @Autowired
    private MovimientoStockService movimientoStockService; // <-- ¡El servicio de Stock!

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
     * Marca un pedido como COMPLETADO y actualiza el stock.
     */
    @Transactional
    public void marcarPedidoCompletado(Long pedidoId, Long usuarioId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        
        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new RuntimeException("Solo se pueden completar pedidos PENDIENTES");
        }

        // 1. Iterar sobre los detalles y aumentar el stock
        for (DetallePedido detalle : pedido.getDetalles()) {
            MovimientoStockDTO movDto = new MovimientoStockDTO(
                detalle.getProducto().getId(),
                usuarioId, // El usuario que confirma la recepción
                TipoMovimiento.ENTRADA_PEDIDO,
                detalle.getCantidad(),
                "Recepción de Pedido #" + pedido.getId(),
                null,
                pedido.getId() // Vinculamos el ID del pedido
            );
            // ¡Llamamos al servicio de stock!
            movimientoStockService.registrarMovimiento(movDto);
        }

        // 2. Actualizar estado del pedido
        pedido.setEstado(EstadoPedido.COMPLETADO);
        pedidoRepository.save(pedido);
    }

    /**
     * Marca un pedido como CANCELADO. (No revierte stock, asume que nunca llegó).
     */
    @Transactional
    public void marcarPedidoCancelado(Long pedidoId) {
         Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
         
         if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new RuntimeException("Solo se pueden cancelar pedidos PENDIENTES");
        }
         
        pedido.setEstado(EstadoPedido.CANCELADO);
        pedidoRepository.save(pedido);
    }

    // Aquí irían los métodos de Listar (Paginados) y GetById
    @Transactional(readOnly = true)
    public PedidoDTO findById(Long id) {
        // Necesitamos un @EntityGraph en el repositorio para traer los detalles
        Pedido pedido = pedidoRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new RuntimeException("Pedido no encontrado: " + id));
        return pedidoMapper.toPedidoDTO(pedido);
    }

    // ... (Crear un findAll paginado similar al de Productos/Usuarios) ...
}