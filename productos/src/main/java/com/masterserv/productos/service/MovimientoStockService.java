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

        // 1. Guardamos el movimiento de stock (Tabla específica)
        movimientoStockRepository.save(movimiento);

        // 2. Registramos en Auditoría General (Tabla genérica)
        registrarEnAuditoriaGeneral(producto, usuario, dto.getTipoMovimiento(), cantidadGuardar, motivoFinal);
    }

    private void registrarEnAuditoriaGeneral(Producto producto, Usuario usuario, TipoMovimiento tipo, int cantidad, String motivo) {
        
        Auditoria audit = new Auditoria();
        audit.setFecha(LocalDateTime.now());
        audit.setUsuario(usuario.getEmail()); 
        
        audit.setEntidad("Producto"); // Más limpio que "Producto (Stock)"
        audit.setEntidadId(producto.getId().toString());
        audit.setAccion("AJUSTE_MANUAL");
        
        // --- LÓGICA DE DETALLE ENRIQUECIDO ---
        
        // Obtenemos el nombre de la categoría de forma segura (avoid NullPointerException)
        String nombreCategoria = "Sin Categoría";
        if (producto.getCategoria() != null) {
            nombreCategoria = producto.getCategoria().getNombre();
        }

        // Formato final: "Prod: Aceite (Lubricantes) | AJUSTE: -5 | Motivo: Rotura"
        String detalleCompleto = String.format("Prod: %s (%s) | %s: %d | Motivo: %s", 
                producto.getNombre(), 
                nombreCategoria,
                tipo,
                cantidad, 
                motivo);

        // Recorte de seguridad (Max 255 chars)
        if (detalleCompleto.length() > 255) {
            detalleCompleto = detalleCompleto.substring(0, 255);
        }
        
        audit.setDetalle(detalleCompleto); 

        // --- Valores Anterior/Nuevo ---
        int stockAnterior = producto.getStockActual();
        // Nota: Asumimos que el stock se actualizará después o en paralelo
        int stockNuevo = stockAnterior + cantidad;

        audit.setValorAnterior("Stock: " + stockAnterior); 
        audit.setValorNuevo("Stock: " + stockNuevo); 

        // Guardamos. Si falla, Spring hará Rollback de todo.
        auditoriaRepository.save(audit); 
    }
}