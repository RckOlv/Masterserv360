package com.masterserv.productos.service;

import com.masterserv.productos.dto.DetalleVentaDTO;
import com.masterserv.productos.dto.MovimientoStockDTO; // Opcional, si usas MovimientoStockService
import com.masterserv.productos.dto.VentaDTO;
import com.masterserv.productos.entity.*; // Importa tus entidades Venta, DetalleVenta, Producto, Usuario
import com.masterserv.productos.enums.EstadoVenta;
import com.masterserv.productos.enums.TipoMovimiento; // Importar Enum
import com.masterserv.productos.mapper.VentaMapper; // ¡NECESITAS CREAR ESTE MAPPER!
import com.masterserv.productos.repository.ProductoRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import com.masterserv.productos.repository.VentaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // ¡Muy importante!

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// Define esta excepción simple en algún lugar (puede ser en un paquete 'exceptions')
class StockInsuficienteException extends RuntimeException {
    public StockInsuficienteException(String message) {
        super(message);
    }
}


@Service
public class VentaService {

    @Autowired
    private VentaRepository ventaRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository; // Para buscar cliente y vendedor
    @Autowired
    private VentaMapper ventaMapper; // ¡Asegúrate de crear esta interfaz MapStruct!

    // Opcional: Inyectar si registras movimientos de stock detallados
    @Autowired(required = false)
    private MovimientoStockService movimientoStockService;

    @Transactional // Garantiza atomicidad
    public VentaDTO create(VentaDTO ventaDTO, String vendedorEmail) {

        Usuario vendedor = usuarioRepository.findByEmail(vendedorEmail)
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado: " + vendedorEmail));

        Usuario cliente = usuarioRepository.findById(ventaDTO.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado: ID " + ventaDTO.getClienteId()));

        Venta venta = new Venta();
        venta.setFechaVenta(LocalDateTime.now());
        venta.setEstado(EstadoVenta.COMPLETADA);
        venta.setVendedor(vendedor);
        venta.setCliente(cliente);
        venta.setDetalles(new HashSet<>());

        BigDecimal totalVenta = BigDecimal.ZERO;

        // --- Procesar Detalles ---
        for (DetalleVentaDTO detalleDTO : ventaDTO.getDetalles()) {
            // --- CORRECCIÓN #1 ---
            Producto producto = productoRepository.findById(detalleDTO.getProductoId()) // Usar getProductoId()
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: ID " + detalleDTO.getProductoId())); // Usar getProductoId()

            if (producto.getStockActual() < detalleDTO.getCantidad()) {
                throw new StockInsuficienteException(
                    String.format("Stock insuficiente para '%s' (ID:%d). Disponible: %d, Solicitado: %d",
                                  producto.getNombre(), producto.getId(),
                                  producto.getStockActual(), detalleDTO.getCantidad())
                );
            }

            DetalleVenta detalle = new DetalleVenta();
            detalle.setProducto(producto);
            detalle.setCantidad(detalleDTO.getCantidad());
            detalle.setPrecioUnitario(producto.getPrecioVenta());
            detalle.setVenta(venta);
            venta.getDetalles().add(detalle);

            producto.setStockActual(producto.getStockActual() - detalleDTO.getCantidad());
            // No save() aquí, @Transactional lo maneja

            totalVenta = totalVenta.add(
                detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad()))
            );

            // Registrar movimiento (con correcciones)
            registrarMovimientoStockSalida(venta, detalle, vendedor);
        }

        venta.setTotalVenta(totalVenta);
        Venta ventaGuardada = ventaRepository.save(venta);

        // Actualizar movimientos con ID de venta si es necesario (lógica más compleja)
        // ...

        return ventaMapper.toVentaDTO(ventaGuardada);
    }

    private void registrarMovimientoStockSalida(Venta venta, DetalleVenta detalle, Usuario vendedor) {
        if (movimientoStockService != null) {
            MovimientoStockDTO movDto = new MovimientoStockDTO();
            movDto.setProductoId(detalle.getProducto().getId());
            movDto.setUsuarioId(vendedor.getId());
            // --- CORRECCIÓN #2 ---
            movDto.setTipoMovimiento(TipoMovimiento.SALIDA_VENTA); // Usar setTipoMovimiento
            movDto.setCantidad(detalle.getCantidad()); // Cantidad positiva en DTO (el tipo indica salida)
            // --- CORRECCIÓN #3 ---
            // Asignar motivo basado en ID de venta (puede ser null al principio)
            String motivoVenta = "Salida por Venta" + (venta.getId() != null ? " #" + venta.getId() : " (pendiente)");
            movDto.setMotivo(motivoVenta); // Usar setMotivo
            // movDto.setVentaId(venta.getId()); // Asignar si el DTO tiene el campo

            try {
                 movimientoStockService.registrarMovimiento(movDto);
            } catch (Exception e) {
                 System.err.println("WARN: No se pudo registrar el movimiento de stock para el producto ID "
                                   + detalle.getProducto().getId() + ". Error: " + e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public VentaDTO findById(Long id) {
        Venta venta = ventaRepository.findById(id)
             .orElseThrow(() -> new RuntimeException("Venta no encontrada: " + id));
        return ventaMapper.toVentaDTO(venta);
    }

    @Transactional(readOnly = true)
    public Page<VentaDTO> findAll(Pageable pageable) {
        Page<Venta> ventaPage = ventaRepository.findAll(pageable);
        return ventaPage.map(ventaMapper::toVentaDTO);
    }

    @Transactional
    public void cancelarVenta(Long ventaId, String usuarioEmailCancela) {
        // --- Carga LAZY (N+1) - Para eficiencia, idealmente usar findByIdWithDetails ---
        Venta venta = ventaRepository.findById(ventaId)
             .orElseThrow(() -> new RuntimeException("Venta no encontrada: " + ventaId));

        if (venta.getEstado() != EstadoVenta.COMPLETADA) {
             throw new RuntimeException("Solo se pueden cancelar ventas que estén COMPLETADAS.");
        }

        Usuario usuarioCancela = usuarioRepository.findByEmail(usuarioEmailCancela)
                 .orElseThrow(() -> new RuntimeException("Usuario que cancela no encontrado: " + usuarioEmailCancela));

        // Reponer stock
        for (DetalleVenta detalle : venta.getDetalles()) { // Causa N+1 si no se usa EntityGraph
            Producto producto = detalle.getProducto();
            producto.setStockActual(producto.getStockActual() + detalle.getCantidad());
            // No save() aquí, @Transactional lo maneja

            registrarMovimientoStockReposicion(venta, detalle, usuarioCancela); // Con correcciones
        }

        venta.setEstado(EstadoVenta.CANCELADA);
        // No save() aquí, @Transactional lo maneja
    }

     private void registrarMovimientoStockReposicion(Venta venta, DetalleVenta detalle, Usuario usuarioCancela) {
        if (movimientoStockService != null) {
            MovimientoStockDTO movDto = new MovimientoStockDTO();
            movDto.setProductoId(detalle.getProducto().getId());
            movDto.setUsuarioId(usuarioCancela.getId());
            // --- CORRECCIÓN #2 y #4 ---
            movDto.setTipoMovimiento(TipoMovimiento.AJUSTE_MANUAL); // O DEVOLUCION si prefieres
            movDto.setCantidad(detalle.getCantidad()); // Cantidad positiva en DTO
            // --- CORRECCIÓN #3 ---
            movDto.setMotivo("Reposición por cancelación Venta #" + venta.getId()); // Usar setMotivo
            // movDto.setVentaId(venta.getId());

             try {
                 movimientoStockService.registrarMovimiento(movDto);
            } catch (Exception e) {
                 System.err.println("WARN: No se pudo registrar el movimiento de stock para reposición producto ID "
                                   + detalle.getProducto().getId() + ". Error: " + e.getMessage());
            }
        }
    }
}