package com.masterserv.productos.service;

import com.masterserv.productos.dto.MovimientoStockDTO;
import com.masterserv.productos.entity.MovimientoStock;
import com.masterserv.productos.entity.Producto;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.enums.TipoMovimiento;
import com.masterserv.productos.mapper.MovimientoStockMapper;
import com.masterserv.productos.repository.MovimientoStockRepository;
import com.masterserv.productos.repository.ProductoRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation; // Importar Propagation

import java.time.LocalDateTime;

@Service
public class MovimientoStockService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MovimientoStockRepository movimientoStockRepository;

    @Autowired
    private MovimientoStockMapper movimientoStockMapper;

    /**
     * Registra un movimiento de stock en la tabla de auditoría.
     * Se ejecuta DENTRO de la transacción principal que lo llama (Venta, Pedido, etc.).
     * Si la Venta falla y hace rollback, este log también hará rollback.
     */
    @Transactional(propagation = Propagation.REQUIRED) // <-- ¡CAMBIO IMPORTANTE!
    public void registrarMovimiento(MovimientoStockDTO dto) {

        // 1. Buscar las entidades referenciadas (Producto y Usuario)
        Producto producto = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado al registrar movimiento: " + dto.getProductoId()));

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado al registrar movimiento: " + dto.getUsuarioId()));

        // 2. Mapear DTO a Entidad MovimientoStock
        MovimientoStock movimiento = movimientoStockMapper.toMovimientoStock(dto);

        // 3. Completar datos de la entidad MovimientoStock
        movimiento.setProducto(producto);
        movimiento.setUsuario(usuario);
        movimiento.setFecha(LocalDateTime.now());

        // 4. Ajustar la cantidad guardada si es una salida
        int cantidadGuardar = dto.getCantidad(); // DTO viene positivo
        if (dto.getTipoMovimiento() == TipoMovimiento.SALIDA_VENTA || 
            dto.getTipoMovimiento() == TipoMovimiento.DEVOLUCION) { // Asumiendo que tengas más tipos
            
            cantidadGuardar = -Math.abs(cantidadGuardar); // Asegura que sea negativo
        } else {
             cantidadGuardar = Math.abs(cantidadGuardar); // Asegura que sea positivo
        }
        movimiento.setCantidad(cantidadGuardar);

        // 5. Guardar el registro del movimiento
        movimientoStockRepository.save(movimiento);
    }
}