package com.masterserv.productos.service;

import com.masterserv.productos.dto.MovimientoStockDTO;
import com.masterserv.productos.entity.Auditoria; // <--- Importar
import com.masterserv.productos.entity.MovimientoStock;
import com.masterserv.productos.entity.Producto;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.enums.TipoMovimiento;
import com.masterserv.productos.mapper.MovimientoStockMapper;
import com.masterserv.productos.repository.AuditoriaRepository; // <--- Importar
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
    private AuditoriaRepository auditoriaRepository; // ✅ Inyectamos el repo de auditoría

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

        String motivoFinal = "Movimiento registrado por el sistema";
        if (dto.getMotivo() != null && !dto.getMotivo().trim().isEmpty()) {
            motivoFinal = dto.getMotivo();
        }
        movimiento.setMotivo(motivoFinal);

        // Lógica de signos
        int cantidadGuardar = dto.getCantidad();
        if (dto.getTipoMovimiento() == TipoMovimiento.SALIDA_VENTA || 
            dto.getTipoMovimiento() == TipoMovimiento.DEVOLUCION) {
            cantidadGuardar = -Math.abs(cantidadGuardar); 
        }
        movimiento.setCantidad(cantidadGuardar);

        movimientoStockRepository.save(movimiento);

        // ✅ REGISTRO EN AUDITORÍA GENERAL
        // Esto hace que aparezca en la pantalla de "Auditoría" con el motivo
        registrarEnAuditoriaGeneral(producto, usuario, dto.getTipoMovimiento(), cantidadGuardar, motivoFinal);
    }

    private void registrarEnAuditoriaGeneral(Producto producto, Usuario usuario, TipoMovimiento tipo, int cantidad, String motivo) {
        try {
            Auditoria audit = new Auditoria();
            audit.setFecha(LocalDateTime.now());
            audit.setUsuario(usuario.getEmail()); // O getUsername()
            audit.setEntidad("Producto (Stock)");
            audit.setEntidadId(producto.getId().toString());
            audit.setAccion("AJUSTE_STOCK");
            
            // Aquí metemos el motivo para que se vea en la tabla
            String detalle = String.format("Ajuste: %d u. | Motivo: %s", cantidad, motivo);
            if (detalle.length() > 255) detalle = detalle.substring(0, 255);
            
            audit.setDetalle(detalle);
            
            // Opcional: llenar valorAnterior/Nuevo si quieres ver el cambio numérico
            audit.setValorAnterior("Stock previo"); // Podrías buscarlo si quisieras ser preciso
            audit.setValorNuevo(String.valueOf(producto.getStockActual() + cantidad)); 

            auditoriaRepository.save(audit);
        } catch (Exception e) {
            System.err.println("Error al guardar auditoría de stock: " + e.getMessage());
        }
    }
}