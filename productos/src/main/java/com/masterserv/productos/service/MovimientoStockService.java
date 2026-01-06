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
        
        System.out.println(">>> [DEBUG] 1. Iniciando registrarEnAuditoriaGeneral para: " + producto.getNombre());

        try {
            Auditoria audit = new Auditoria();
            audit.setFecha(LocalDateTime.now());
            
            // Verificación de nulidad para usuario
            String emailUsuario = (usuario != null) ? usuario.getEmail() : "sistema@masterserv.com";
            audit.setUsuario(emailUsuario); 
            
            audit.setEntidad("Producto");
            audit.setEntidadId(producto.getId().toString());
            audit.setAccion("AJUSTE_MANUAL"); // ✅ Esto es lo que buscamos
            
            // --- LÓGICA DE DETALLE ---
            String nombreCategoria = "Sin Categoría";
            if (producto.getCategoria() != null) {
                nombreCategoria = producto.getCategoria().getNombre();
            }

            String detalleCompleto = String.format("Prod: %s (%s) | %s: %d | Motivo: %s", 
                    producto.getNombre(), 
                    nombreCategoria,
                    tipo,
                    cantidad, 
                    motivo);

            // Recorte de seguridad extremo (por si acaso)
            if (detalleCompleto.length() > 250) {
                detalleCompleto = detalleCompleto.substring(0, 250);
            }
            audit.setDetalle(detalleCompleto); 

            // --- JSONs ---
            int stockAnterior = producto.getStockActual();
            int stockNuevo = stockAnterior + cantidad;

            // JSONs manuales simples
            String jsonAnterior = String.format("{\"Stock\": \"%d\"}", stockAnterior);
            
            // Sanitizamos el motivo para que no rompa el JSON (ej. si tiene comillas)
            String motivoSafe = (motivo != null) ? motivo.replace("\"", "'") : "Sin motivo";
            String jsonNuevo = String.format("{\"Stock\": \"%d\", \"Motivo\": \"%s\"}", stockNuevo, motivoSafe);

            audit.setValorAnterior(jsonAnterior); 
            audit.setValorNuevo(jsonNuevo); 

            System.out.println(">>> [DEBUG] 2. Objeto Auditoria preparado. Intentando guardar...");

            // Guardamos Y FORZAMOS la escritura inmediata
            auditoriaRepository.save(audit); 
            auditoriaRepository.flush(); // <--- EL TRUCO MAESTRO

            System.out.println(">>> [DEBUG] 3. ¡GUARDADO Y FLUSHEADO EXITOSAMENTE!");

        } catch (Exception e) {
            System.err.println(">>> [ERROR CRÍTICO] No se pudo guardar la auditoría: ");
            e.printStackTrace(); // <--- ESTO NOS DIRÁ EL PROBLEMA EXACTO
        }
    }
}