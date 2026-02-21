package com.masterserv.productos.service;

import com.masterserv.productos.dto.AbrirCajaDTO;
import com.masterserv.productos.dto.CerrarCajaDTO;
import com.masterserv.productos.dto.RetiroCajaDTO; // ✅ Importamos el nuevo DTO
import com.masterserv.productos.entity.Caja;
import com.masterserv.productos.entity.Usuario;
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

    // 1. VERIFICAR SI TIENE CAJA ABIERTA (Angular llamará esto al entrar al POS)
    public Caja obtenerCajaAbierta(Long usuarioId) {
        return cajaRepository.findCajaAbiertaByUsuario(usuarioId).orElse(null);
    }

    // 2. ABRIR LA CAJA AL INICIO DEL TURNO
    @Transactional
    public Caja abrirCaja(AbrirCajaDTO dto) {
        // Verificamos que no tenga otra caja abierta
        if (obtenerCajaAbierta(dto.getUsuarioId()) != null) {
            throw new RuntimeException("El usuario ya tiene una caja abierta.");
        }

        Usuario cajero = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Caja nuevaCaja = new Caja();
        nuevaCaja.setUsuario(cajero);
        nuevaCaja.setMontoInicial(dto.getMontoInicial());
        
        // Inicializamos las extracciones en CERO para evitar errores matemáticos
        nuevaCaja.setExtracciones(BigDecimal.ZERO);
        
        return cajaRepository.save(nuevaCaja);
    }

    // 3. CERRAR LA CAJA Y HACER EL ARQUEO (Matemática pura)
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

        // EL GRAN CÁLCULO DEL ARQUEO (AHORA CON RETIROS):
        // Cuánto debería haber: Monto Inicial + Ventas en Efectivo - Extracciones
        BigDecimal extracciones = caja.getExtracciones() != null ? caja.getExtracciones() : BigDecimal.ZERO;
        
        BigDecimal totalEsperadoCajon = caja.getMontoInicial()
                .add(caja.getVentasEfectivo())
                .subtract(extracciones); // ✅ RESTAMOS LO QUE SACARON PARA GALLETITAS
        
        // Diferencia = Lo que el cajero declara que hay - Lo que el sistema dice que debería haber
        BigDecimal diferencia = dto.getMontoDeclarado().subtract(totalEsperadoCajon);
        caja.setDiferencia(diferencia);

        return cajaRepository.save(caja);
    }

    // 4. RETIRAR DINERO PARA GASTOS VARIOS (Sangría/Extracción)
    @Transactional
    public Caja registrarRetiro(RetiroCajaDTO dto) {
        Caja caja = cajaRepository.findById(dto.getCajaId())
                .orElseThrow(() -> new RuntimeException("Caja no encontrada"));

        if ("CERRADA".equals(caja.getEstado())) {
            throw new RuntimeException("No puedes sacar dinero de una caja cerrada.");
        }

        // Validación de seguridad para que la suma no explote si es null
        BigDecimal extraccionActual = caja.getExtracciones() != null ? caja.getExtracciones() : BigDecimal.ZERO;
        
        // Le sumamos el nuevo retiro al acumulador total de la caja
        caja.setExtracciones(extraccionActual.add(dto.getMonto()));
        
        return cajaRepository.save(caja);
    }
}