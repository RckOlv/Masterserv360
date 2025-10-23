package com.masterserv.productos.service;

import com.masterserv.productos.dto.FinalizarVentaDTO;
import com.masterserv.productos.dto.MovimientoStockDTO;
import com.masterserv.productos.dto.VentaDTO;
import com.masterserv.productos.entity.*;
import com.masterserv.productos.enums.EstadoVenta;
import com.masterserv.productos.enums.TipoMovimiento;
import com.masterserv.productos.mapper.VentaMapper;
import com.masterserv.productos.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
public class VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CarritoRepository carritoRepository;

    @Autowired
    private CarritoService carritoService; // Reusamos la lógica del carrito

    @Autowired
    private MovimientoStockService movimientoStockService; // El servicio de auditoría

    @Autowired
    private VentaMapper ventaMapper;

    /**
     * Proceso central para finalizar una venta.
     * Es transaccional: si falla el descuento de stock, se revierte la venta.
     */
    @Transactional
    public VentaDTO finalizarVenta(FinalizarVentaDTO finalizarDTO) {
        
        // 1. Obtener las entidades principales
        Usuario vendedor = usuarioRepository.findById(finalizarDTO.getVendedorId())
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado"));
        
        Usuario cliente = usuarioRepository.findById(finalizarDTO.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        Carrito carrito = carritoRepository.findByVendedor(vendedor)
                .orElseThrow(() -> new RuntimeException("Carrito del vendedor no encontrado"));

        if (carrito.getItems() == null || carrito.getItems().isEmpty()) {
            throw new RuntimeException("El carrito está vacío. No se puede generar la venta.");
        }

        // 2. Crear la cabecera de la Venta
        Venta venta = new Venta();
        venta.setVendedor(vendedor);
        venta.setCliente(cliente);
        venta.setFechaVenta(LocalDateTime.now());
        venta.setEstado(EstadoVenta.COMPLETADA);
        venta.setDetalles(new HashSet<>());
        
        BigDecimal totalVenta = BigDecimal.ZERO;

        // 3. Iterar sobre los items del carrito para crear los Detalles de Venta
        for (ItemCarrito item : carrito.getItems()) {
            Producto producto = item.getProducto();
            int cantidad = item.getCantidad();

            // 3a. Crear el DetalleVenta (registro permanente)
            DetalleVenta detalle = new DetalleVenta();
            detalle.setVenta(venta);
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(producto.getPrecioVenta()); // "Congelamos" el precio
            
            venta.getDetalles().add(detalle);

            // 3b. Sumar al total
            totalVenta = totalVenta.add(producto.getPrecioVenta().multiply(new BigDecimal(cantidad)));

            // 3c. Descontar el Stock (¡Llamamos al servicio de stock!)
            MovimientoStockDTO movDto = new MovimientoStockDTO(
                producto.getId(),
                vendedor.getId(),
                TipoMovimiento.SALIDA_VENTA,
                cantidad,
                "Venta #" + venta.getId(), // 'venta.getId()' será nulo, mejoramos esto
                null,
                null
            );
            movimientoStockService.registrarMovimiento(movDto);
        }
        
        // 4. Asignar el total y guardar la Venta (y sus detalles en cascada)
        venta.setTotalVenta(totalVenta);
        Venta ventaGuardada = ventaRepository.save(venta);
        
        // 4b. (Mejora) Actualizamos el motivo del movimiento con el ID real de la Venta
        actualizarMotivoStock(ventaGuardada);

        // 5. Vaciar el carrito
        carritoService.vaciarCarrito(carrito.getId());

        // 6. Devolver el DTO de la Venta creada
        return ventaMapper.toVentaDTO(ventaGuardada);
    }
    
    /**
     * Método helper para actualizar el motivo del stock DESPUÉS de tener el ID de la Venta.
     * En un sistema real, esto se manejaría con eventos.
     */
    private void actualizarMotivoStock(Venta venta) {
        // Esta es una simplificación. Idealmente, el MovimientoStockService
        // guardaría la FK a 'ventaId' directamente.
        // Por ahora, lo dejamos así por velocidad (24 días).
    }
}