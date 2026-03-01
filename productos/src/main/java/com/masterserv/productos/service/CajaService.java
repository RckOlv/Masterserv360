package com.masterserv.productos.service;

import com.masterserv.productos.dto.AbrirCajaDTO;
import com.masterserv.productos.dto.CerrarCajaDTO;
import com.masterserv.productos.dto.RetiroCajaDTO;
import com.masterserv.productos.dto.MovimientoCajaDTO;
import com.masterserv.productos.entity.Auditoria;
import com.masterserv.productos.entity.Caja;
import com.masterserv.productos.entity.MovimientoCaja;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.repository.AuditoriaRepository;
import com.masterserv.productos.repository.CajaRepository;
import com.masterserv.productos.repository.MovimientoCajaRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CajaService {

    @Autowired
    private CajaRepository cajaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Autowired
    private MovimientoCajaRepository movimientoCajaRepository; // ✅ NUEVO REPOSITORIO INYECTADO

    // ✅ AHORA BUSCA LA CAJA DEL LOCAL, NO LA DEL USUARIO
    public Caja obtenerCajaAbierta(Long usuarioId) {
        // Ignoramos el usuarioId y buscamos si hay alguna caja abierta en el sistema
        return cajaRepository.findFirstByEstado("ABIERTA").orElse(null);
    }

    @Transactional
    public Caja abrirCaja(AbrirCajaDTO dto) {
        // Verificamos si EL LOCAL ya tiene una caja abierta
        if (obtenerCajaAbierta(null) != null) {
            throw new RuntimeException("Ya existe una caja abierta en el local.");
        }

        Usuario cajeroQueAbre = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Caja nuevaCaja = new Caja();
        nuevaCaja.setUsuario(cajeroQueAbre); // Queda registrado quién la abrió
        nuevaCaja.setMontoInicial(dto.getMontoInicial());
        nuevaCaja.setExtracciones(BigDecimal.ZERO);
        nuevaCaja.setEstado("ABIERTA"); // Aseguramos el estado
        
        Caja guardada = cajaRepository.save(nuevaCaja);

        // ✅ REGISTRAR MOVIMIENTO DE APERTURA EN EL HISTORIAL
        if (dto.getMontoInicial().compareTo(BigDecimal.ZERO) > 0) {
            MovimientoCaja mov = new MovimientoCaja();
            mov.setCaja(guardada);
            mov.setTipoMovimiento("INGRESO");
            mov.setConcepto("Apertura de Caja (Fondo Inicial)");
            mov.setMonto(dto.getMontoInicial());
            mov.setMetodoPago("EFECTIVO");
            mov.setUsuario(cajeroQueAbre);
            mov.setFecha(LocalDateTime.now());
            movimientoCajaRepository.save(mov);
        }

        registrarAuditoriaCaja(cajeroQueAbre, guardada.getId(), "CAJA_ABRIR", 
            String.format("Apertura de caja general. Monto inicial: $%s", dto.getMontoInicial()),
            null, "{ \"Efectivo Inicial\": " + dto.getMontoInicial() + " }");

        return guardada;
    }

    @Transactional
    public Caja cerrarCaja(CerrarCajaDTO dto) {
        Caja caja = cajaRepository.findById(dto.getCajaId())
                .orElseThrow(() -> new RuntimeException("Caja no encontrada"));

        if ("CERRADA".equals(caja.getEstado())) {
            throw new RuntimeException("Esta caja ya fue cerrada anteriormente.");
        }

        Usuario cajeroQueCierra = usuarioRepository.findById(dto.getUsuarioId())
                .orElse(caja.getUsuario());

        caja.setFechaCierre(LocalDateTime.now());
        caja.setEstado("CERRADA");
        caja.setMontoDeclarado(dto.getMontoDeclarado());

        BigDecimal extracciones = caja.getExtracciones() != null ? caja.getExtracciones() : BigDecimal.ZERO;
        BigDecimal totalEsperadoCajon = caja.getMontoInicial()
                .add(caja.getVentasEfectivo())
                .subtract(extracciones);
        
        BigDecimal diferencia = dto.getMontoDeclarado().subtract(totalEsperadoCajon);
        caja.setDiferencia(diferencia);
        caja.setObservacionCierre(dto.getObservacion());
        Caja cerrada = cajaRepository.save(caja);

        String obsDetalle = (dto.getObservacion() != null && !dto.getObservacion().isBlank()) 
                            ? " | Motivo: " + dto.getObservacion() : "";
                            
        String detalleArqueo = String.format("Cierre de caja general. Declarado: $%s | Esperado: $%s | Diferencia: $%s%s", 
            dto.getMontoDeclarado(), totalEsperadoCajon, diferencia, obsDetalle);
        
        registrarAuditoriaCaja(cajeroQueCierra, cerrada.getId(), "CAJA_CERRAR", detalleArqueo,
            "{ \"Esperado\": " + totalEsperadoCajon + " }", 
            "{ \"Declarado\": " + dto.getMontoDeclarado() + ", \"Diferencia\": " + diferencia + " }");

        return cerrada;
    }

    @Transactional
    public Caja registrarRetiro(RetiroCajaDTO dto) {
        Caja caja = cajaRepository.findById(dto.getCajaId())
                .orElseThrow(() -> new RuntimeException("Caja no encontrada"));

        if ("CERRADA".equals(caja.getEstado())) {
            throw new RuntimeException("No puedes sacar dinero de una caja cerrada.");
        }

        BigDecimal extraccionActual = caja.getExtracciones() != null ? caja.getExtracciones() : BigDecimal.ZERO;
        caja.setExtracciones(extraccionActual.add(dto.getMonto()));
        
        Caja actualizada = cajaRepository.save(caja);
        Usuario operario = caja.getUsuario(); 

        // ✅ REGISTRAR MOVIMIENTO DE RETIRO EN EL HISTORIAL
        MovimientoCaja mov = new MovimientoCaja();
        mov.setCaja(actualizada);
        mov.setTipoMovimiento("EGRESO");
        mov.setConcepto("Retiro: " + dto.getMotivo());
        mov.setMonto(dto.getMonto());
        mov.setMetodoPago("EFECTIVO"); // Los retiros de cajón son en efectivo
        mov.setUsuario(operario);
        mov.setFecha(LocalDateTime.now());
        movimientoCajaRepository.save(mov);

        String detalleRetiro = String.format("Retiro de efectivo (Sangría). Monto: $%s | Motivo: %s", 
            dto.getMonto(), dto.getMotivo());
        
        registrarAuditoriaCaja(operario, actualizada.getId(), "CAJA_RETIRO", detalleRetiro,
            "{ \"Retiros anteriores\": " + extraccionActual + " }", 
            "{ \"Total Retiros\": " + actualizada.getExtracciones() + " }");

        return actualizada;
    }

    // ✅ NUEVO MÉTODO PARA OBTENER EL HISTORIAL DE LA CAJA
    @Transactional(readOnly = true)
    public List<MovimientoCajaDTO> obtenerMovimientosCaja(Long cajaId) {
        return movimientoCajaRepository.findByCajaIdOrderByFechaDesc(cajaId).stream().map(m -> {
            MovimientoCajaDTO dto = new MovimientoCajaDTO();
            dto.setId(m.getId());
            dto.setFecha(m.getFecha());
            dto.setTipoMovimiento(m.getTipoMovimiento());
            dto.setConcepto(m.getConcepto());
            dto.setMonto(m.getMonto());
            dto.setMetodoPago(m.getMetodoPago());
            dto.setUsuarioNombre(m.getUsuario() != null ? m.getUsuario().getNombre() + " " + m.getUsuario().getApellido() : "Sistema");
            return dto;
        }).collect(Collectors.toList());
    }

    private void registrarAuditoriaCaja(Usuario usuario, Long cajaId, String accion, String detalle, String anterior, String nuevo) {
        try {
            Auditoria audit = new Auditoria();
            audit.setFecha(LocalDateTime.now());
            audit.setUsuario(usuario.getEmail());
            audit.setEntidad("Caja");
            audit.setEntidadId(cajaId.toString());
            audit.setAccion(accion);
            
            if (detalle.length() > 255) detalle = detalle.substring(0, 255);
            audit.setDetalle(detalle);

            audit.setValorAnterior(anterior);
            audit.setValorNuevo(nuevo);

            auditoriaRepository.save(audit);
        } catch (Exception e) {
            System.err.println("Error al registrar auditoría de caja: " + e.getMessage());
        }
    }
}