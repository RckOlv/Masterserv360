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

        // Aseguramos que siempre haya un motivo
        String motivoFinal = "Movimiento registrado por el sistema";
        if (dto.getMotivo() != null && !dto.getMotivo().trim().isEmpty()) {
            motivoFinal = dto.getMotivo().trim();
        }
        movimiento.setMotivo(motivoFinal);

        // Lógica de signos (Negativo para salidas/devoluciones)
        // NOTA: Si el tipo es SALIDA_VENTA, guardamos negativo.
        int cantidadGuardar = dto.getCantidad();
        if (dto.getTipoMovimiento() == TipoMovimiento.SALIDA_VENTA) { // Devolución suele ser entrada (+)
            cantidadGuardar = -Math.abs(cantidadGuardar); 
        }
        // Si es DEVOLUCION (Cliente devuelve producto), es ENTRADA (+), así que lo dejamos positivo.
        
        movimiento.setCantidad(cantidadGuardar);

        // 1. Guardamos el movimiento de stock (Tabla específica)
        movimientoStockRepository.save(movimiento);

        // 2. Registramos en Auditoría General (Tabla genérica)
        registrarEnAuditoriaGeneral(producto, usuario, dto.getTipoMovimiento(), cantidadGuardar, motivoFinal);
    }


    private void registrarEnAuditoriaGeneral(Producto producto, Usuario usuario, TipoMovimiento tipo, int cantidad, String motivo) {
    try {
        Auditoria audit = new Auditoria();
        audit.setFecha(LocalDateTime.now());
        audit.setUsuario(usuario != null ? usuario.getEmail() : "sistema@masterserv.com");
        audit.setEntidad("Producto");
        audit.setEntidadId(producto.getId().toString());
        
        // Acción dinámica según el Enum (ENTRADA_PEDIDO, SALIDA_VENTA, etc.)
        audit.setAccion(tipo.name()); 

        // --- DETALLE INTELIGENTE ---
        String detallePrefix = "";
        if (tipo == TipoMovimiento.SALIDA_VENTA) {
            detallePrefix = "🛒 Venta registrada. ";
        } else if (tipo == TipoMovimiento.ENTRADA_PEDIDO) {
            detallePrefix = "📦 Ingreso de mercadería. ";
        } else if (tipo == TipoMovimiento.DEVOLUCION) {
            detallePrefix = "↩️ Devolución/Cancelación. ";
        }

        String detalleCompleto = String.format("%sProd: %s | Cant: %d | Motivo: %s", 
                detallePrefix, producto.getNombre(), cantidad, motivo);

        if (detalleCompleto.length() > 255) detalleCompleto = detalleCompleto.substring(0, 255);
        audit.setDetalle(detalleCompleto); 

        // --- JSON DE VALORES ---
        int stockNuevo = producto.getStockActual();
        int stockAnterior = stockNuevo - cantidad;

        audit.setValorAnterior("{ \"Stock\": " + stockAnterior + " }");
        audit.setValorNuevo("{ \"Stock\": " + stockNuevo + ", \"Variacion\": " + cantidad + " }");

        auditoriaRepository.save(audit);
        
    } catch (Exception e) {
        System.err.println(">>> [ERROR] Auditoría: " + e.getMessage());
    }
}
}