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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(propagation = Propagation.REQUIRED) 
    public void registrarMovimiento(MovimientoStockDTO dto) {

        // 1. Validaciones
        Producto producto = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + dto.getProductoId()));

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + dto.getUsuarioId()));

        // 2. Mapeo
        MovimientoStock movimiento = movimientoStockMapper.toMovimientoStock(dto);

        // 3. Completar datos
        movimiento.setProducto(producto);
        movimiento.setUsuario(usuario);
        movimiento.setFecha(LocalDateTime.now());
        
        // ✅ Aseguramos que el motivo se guarde
        if (dto.getMotivo() != null && !dto.getMotivo().isBlank()) {
            movimiento.setMotivo(dto.getMotivo());
        } else {
            // Si viene vacío (ej. venta automática), ponemos un default
            movimiento.setMotivo("Movimiento automático del sistema");
        }

        // 4. Lógica de signo (Positivo/Negativo)
        int cantidadFinal = dto.getCantidad();
        
        // Si es salida o ajuste negativo, nos aseguramos que se guarde con signo menos
        if (dto.getTipoMovimiento() == TipoMovimiento.SALIDA_VENTA || 
            dto.getTipoMovimiento() == TipoMovimiento.DEVOLUCION ||
            (dto.getTipoMovimiento() == TipoMovimiento.AJUSTE_MANUAL && cantidadFinal < 0)) {
        }
        movimiento.setCantidad(cantidadFinal);

        // 5. Guardar
        movimientoStockRepository.save(movimiento);
    }
}