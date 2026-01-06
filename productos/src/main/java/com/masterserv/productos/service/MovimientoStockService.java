package com.masterserv.productos.service;

import com.masterserv.productos.dto.MovimientoStockDTO;
import com.masterserv.productos.entity.Auditoria;
import com.masterserv.productos.entity.MovimientoStock;
import com.masterserv.productos.entity.Producto;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.enums.TipoMovimiento;
import com.masterserv.productos.mapper.MovimientoStockMapper;
import com.masterserv.productos.repository.AuditoriaRepository;
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
    
    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Transactional(propagation = Propagation.REQUIRED) 
    public void registrarMovimiento(MovimientoStockDTO dto) {

        Producto producto = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + dto.getProductoId()));

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + dto.getUsuarioId()));

        MovimientoStock movimiento = movimientoStockMapper.toMovimientoStock(dto);

        movimiento.setProducto(producto);
        movimiento.setUsuario(usuario);
        movimiento.setFecha(LocalDateTime.now());

        // Aseguramos que siempre haya un motivo, aunque sea genérico
        String motivoFinal = "Movimiento registrado por el sistema";
        if (dto.getMotivo() != null && !dto.getMotivo().trim().isEmpty()) {
            motivoFinal = dto.getMotivo().trim();
        }
        movimiento.setMotivo(motivoFinal);

        // Lógica de signos (Negativo para salidas/devoluciones)
        int cantidadGuardar = dto.getCantidad();
        if (dto.getTipoMovimiento() == TipoMovimiento.SALIDA_VENTA || 
            dto.getTipoMovimiento() == TipoMovimiento.DEVOLUCION) {
            cantidadGuardar = -Math.abs(cantidadGuardar); 
        }
        movimiento.setCantidad(cantidadGuardar);

        // 1. Guardamos el movimiento de stock
        movimientoStockRepository.save(movimiento);

        // 2. Registramos en Auditoría General (SIN try-catch silencioso)
        registrarEnAuditoriaGeneral(producto, usuario, dto.getTipoMovimiento(), cantidadGuardar, motivoFinal);
    }

    private void registrarEnAuditoriaGeneral(Producto producto, Usuario usuario, TipoMovimiento tipo, int cantidad, String motivo) {
        // NOTA: Eliminamos el try-catch. Si esto falla, queremos que falle todo (@Transactional)
        
        Auditoria audit = new Auditoria();
        audit.setFecha(LocalDateTime.now());
        audit.setUsuario(usuario.getEmail()); 
        
        audit.setEntidad("Producto (Stock)");
        audit.setEntidadId(producto.getId().toString());
        audit.setAccion("AJUSTE_STOCK");
        
        // Validación de longitud para evitar crash por DataTruncation
        if (motivo != null && motivo.length() > 255) {
            motivo = motivo.substring(0, 255);
        }
        audit.setMotivo(motivo); 

        // --- Detalle Técnico Limpio ---
        String detalle = String.format("Tipo: %s | Cantidad: %d", tipo, cantidad);
        if (detalle.length() > 255) {
            detalle = detalle.substring(0, 255);
        }
        audit.setDetalle(detalle);
        
        // --- Valores Anterior/Nuevo (Estimado) ---
        // Asumimos que el producto.getStockActual() aún tiene el valor base
        int stockAnterior = producto.getStockActual();
        int stockNuevo = stockAnterior + cantidad;

        audit.setValorAnterior(String.valueOf(stockAnterior)); 
        audit.setValorNuevo(String.valueOf(stockNuevo)); 

        // Guardamos. Si falla, Spring hará Rollback del stock también.
        auditoriaRepository.save(audit); 
    }
}