package com.masterserv.productos.service;

import com.masterserv.productos.dto.AbrirCajaDTO;
import com.masterserv.productos.dto.CerrarCajaDTO;
import com.masterserv.productos.dto.RetiroCajaDTO;
import com.masterserv.productos.entity.Auditoria;
import com.masterserv.productos.entity.Caja;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.repository.AuditoriaRepository;
import com.masterserv.productos.repository.CajaRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class CajaService {

    @Autowired
    private CajaRepository cajaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    public Caja obtenerCajaAbierta(Long usuarioId) {
        return cajaRepository.findCajaAbiertaByUsuario(usuarioId).orElse(null);
    }

    @Transactional
    public Caja abrirCaja(AbrirCajaDTO dto) {
        if (obtenerCajaAbierta(dto.getUsuarioId()) != null) {
            throw new RuntimeException("El usuario ya tiene una caja abierta.");
        }

        Usuario cajero = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Caja nuevaCaja = new Caja();
        nuevaCaja.setUsuario(cajero);
        nuevaCaja.setMontoInicial(dto.getMontoInicial());
        nuevaCaja.setExtracciones(BigDecimal.ZERO);
        
        Caja guardada = cajaRepository.save(nuevaCaja);

        registrarAuditoriaCaja(cajero, guardada.getId(), "CAJA_ABRIR", 
            String.format("Apertura de turno. Monto inicial: $%s", dto.getMontoInicial()));

        return guardada;
    }

    @Transactional
    public Caja cerrarCaja(CerrarCajaDTO dto) {
        Caja caja = cajaRepository.findById(dto.getCajaId())
                .orElseThrow(() -> new RuntimeException("Caja no encontrada"));

        if ("CERRADA".equals(caja.getEstado())) {
            throw new RuntimeException("Esta caja ya fue cerrada anteriormente.");
        }

        caja.setFechaCierre(LocalDateTime.now());
        caja.setEstado("CERRADA");
        caja.setMontoDeclarado(dto.getMontoDeclarado());

        BigDecimal extracciones = caja.getExtracciones() != null ? caja.getExtracciones() : BigDecimal.ZERO;
        BigDecimal totalEsperadoCajon = caja.getMontoInicial()
                .add(caja.getVentasEfectivo())
                .subtract(extracciones);
        
        BigDecimal diferencia = dto.getMontoDeclarado().subtract(totalEsperadoCajon);
        caja.setDiferencia(diferencia);

        Caja cerrada = cajaRepository.save(caja);

        String detalleArqueo = String.format("Cierre de caja. Declarado: $%s | Esperado: $%s | Diferencia: $%s", 
            dto.getMontoDeclarado(), totalEsperadoCajon, diferencia);
        
        registrarAuditoriaCaja(caja.getUsuario(), cerrada.getId(), "CAJA_CERRAR", detalleArqueo);

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

        String detalleRetiro = String.format("Retiro de efectivo (Sangría). Monto: $%s | Motivo: %s", 
            dto.getMonto(), dto.getMotivo());
        
        registrarAuditoriaCaja(caja.getUsuario(), actualizada.getId(), "CAJA_RETIRO", detalleRetiro);

        return actualizada;
    }

    /**
     * Registra el evento en la tabla de auditoría general para seguimiento administrativo.
     */
    private void registrarAuditoriaCaja(Usuario usuario, Long cajaId, String accion, String detalle) {
        try {
            Auditoria audit = new Auditoria();
            audit.setFecha(LocalDateTime.now());
            audit.setUsuario(usuario.getEmail());
            audit.setEntidad("Caja");
            audit.setEntidadId(cajaId.toString());
            audit.setAccion(accion);
            
            if (detalle.length() > 255) detalle = detalle.substring(0, 255);
            audit.setDetalle(detalle);

            auditoriaRepository.save(audit);
        } catch (Exception e) {
            System.err.println("Error al registrar auditoría de caja: " + e.getMessage());
        }
    }
}