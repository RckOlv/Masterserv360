package com.masterserv.productos.service;

import com.masterserv.productos.dto.DetallePedidoDTO;
import com.masterserv.productos.dto.MovimientoStockDTO;
import com.masterserv.productos.dto.PedidoDTO;
import com.masterserv.productos.dto.PedidoDetalladoDTO; // Importante para el comprobante
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

import jakarta.persistence.EntityNotFoundException; // Importar excepción estándar

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    
    // Inyectamos ProductoService para manejar la lógica de stock de forma centralizada
    @Autowired
    private ProductoService productoService; 
    
    @Autowired
    private PedidoMapper pedidoMapper;
    @Autowired
    private MovimientoStockService movimientoStockService; 

    /**
     * Crea un Pedido Manual en estado PENDIENTE.
     * NO afecta el stock todavía.
     */
    @Transactional
    public PedidoDTO create(PedidoDTO pedidoDTO) {
        
        // 1. Cabecera del Pedido
        Pedido pedido = pedidoMapper.toPedido(pedidoDTO);
        pedido.setFechaPedido(LocalDateTime.now());
        pedido.setEstado(EstadoPedido.PENDIENTE); 

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
            
            // Lógica de Precio: Si el usuario mandó un precio manual, lo usamos.
            // Si no, usamos el costo actual del producto.
            if (detalleDTO.getPrecioUnitario() != null) {
                detalle.setPrecioUnitario(detalleDTO.getPrecioUnitario());
            } else {
                detalle.setPrecioUnitario(producto.getPrecioCosto());
            }
            
            detalles.add(detalle);

            // Sumar al total
            totalPedido = totalPedido.add(detalle.getPrecioUnitario().multiply(new BigDecimal(detalle.getCantidad())));
        }

        pedido.setDetalles(detalles);
        pedido.setTotalPedido(totalPedido);

        // 4. Guardar (Solo genera el registro del pedido)
        Pedido pedidoGuardado = pedidoRepository.save(pedido);
        
        return pedidoMapper.toPedidoDTO(pedidoGuardado);
    }
    
    /**
     * Confirma la recepción de la mercadería.
     * Cambia estado a COMPLETADO y AUMENTA el stock.
     */
    @Transactional
    public void marcarPedidoCompletado(Long pedidoId, String userEmail) {
        
        // 1. Identificar al responsable (Auditoría)
        Usuario usuarioQueConfirma = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuario '" + userEmail + "' no encontrado."));

        // 2. Buscar el pedido (Asegurarse de traer los detalles con JOIN FETCH si es posible, o Lazy load)
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado"));
        
        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden completar pedidos PENDIENTES. El estado actual es: " + pedido.getEstado());
        }

        // 3. IMPACTAR EL STOCK
        for (DetallePedido detalle : pedido.getDetalles()) { 
            
            Long productoId = detalle.getProducto().getId();
            int cantidadRecibida = detalle.getCantidad();
            
            // A. Aumentar Stock Físico
            productoService.reponerStock(productoId, cantidadRecibida); 

            // B. Registrar Movimiento en el Historial
            MovimientoStockDTO movDto = new MovimientoStockDTO(
                    productoId,
                    usuarioQueConfirma.getId(), 
                    TipoMovimiento.ENTRADA_PEDIDO, // Asegúrate de tener este Enum
                    cantidadRecibida,
                    "Recepción Pedido #" + pedido.getId() + " (" + pedido.getProveedor().getRazonSocial() + ")",
                    null,
                    pedidoId 
            );
            
            movimientoStockService.registrarMovimiento(movDto);
        }

        // 4. Cerrar el pedido
        pedido.setEstado(EstadoPedido.COMPLETADO);
        pedidoRepository.save(pedido);
    }

    /**
     * Cancela un pedido que nunca llegó.
     */
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

    /**
     * Método para el COMPROBANTE / DETALLE VISUAL
     */
    @Transactional(readOnly = true)
    public PedidoDetalladoDTO obtenerDetallesPedido(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado: " + id));

        PedidoDetalladoDTO dto = new PedidoDetalladoDTO();
        
        // Cabecera
        dto.setId(pedido.getId());
        dto.setFechaPedido(pedido.getFechaPedido());
        dto.setEstado(pedido.getEstado());
        dto.setTotalPedido(pedido.getTotalPedido());
        
        // Proveedor
        if (pedido.getProveedor() != null) {
            dto.setProveedorId(pedido.getProveedor().getId());
            dto.setProveedorRazonSocial(pedido.getProveedor().getRazonSocial());
            dto.setProveedorCuit(pedido.getProveedor().getCuit());
            dto.setProveedorEmail(pedido.getProveedor().getEmail());
        }

        // Usuario Solicitante
        if (pedido.getUsuario() != null) {
            dto.setUsuarioSolicitante(pedido.getUsuario().getNombre() + " " + pedido.getUsuario().getApellido());
        }

        // Detalles
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
        // Nota: Asegúrate de tener findByIdWithDetails en tu repo, o usa findById normal si tienes Lazy bien configurado
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado: " + id));
        return pedidoMapper.toPedidoDTO(pedido);
    }
}