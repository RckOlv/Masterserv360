package com.masterserv.productos.service;

import com.masterserv.productos.dto.ReglaPuntosDTO;
import com.masterserv.productos.entity.ReglaPuntos;
import com.masterserv.productos.repository.ReglaPuntosRepository;
import com.masterserv.productos.mapper.ReglaPuntosMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate; // <--- IMPORTANTE: Agregar este import
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

@Service
public class ReglaPuntosService {

    private static final String ESTADO_ACTIVO = "ACTIVA";

    @Autowired
    private ReglaPuntosRepository reglaPuntosRepository;

    @Autowired
    private ReglaPuntosMapper reglaPuntosMapper;

    // --- MENTOR: VUELTA ATRÁS (Para PuntosService) ---
    @Transactional(readOnly = true)
    public Optional<ReglaPuntos> getReglaActiva() {
        return reglaPuntosRepository.findByEstadoRegla(ESTADO_ACTIVO);
    }

    // --- MENTOR: MÉTODO NUEVO (Para el Controller) ---
    @Transactional(readOnly = true)
    public Optional<ReglaPuntosDTO> getReglaActivaDTO() {
        return getReglaActiva()
                .map(reglaPuntosMapper::toReglaPuntosDTO);
    }
    // ------------------------------------------------

    @Transactional(readOnly = true)
    public List<ReglaPuntosDTO> findAll() {
        return reglaPuntosMapper.toReglaPuntosDTOList(reglaPuntosRepository.findAll());
    }
    
    @Transactional
    public ReglaPuntosDTO createOrUpdateRegla(ReglaPuntosDTO nuevaReglaDTO) {
        
        // 1. Desactivar la regla anterior
        reglaPuntosRepository.findByEstadoRegla(ESTADO_ACTIVO).ifPresent(reglaAnterior -> {
            reglaAnterior.setEstadoRegla("CADUCADA");
            reglaPuntosRepository.save(reglaAnterior);
        });
        
        // 2. Convertir DTO a Entidad
        ReglaPuntos nuevaRegla = reglaPuntosMapper.toReglaPuntos(nuevaReglaDTO);
        
        // --- MENTOR: CÁLCULO DE VIGENCIA (Corrección) ---
        // Aquí llenamos las columnas vigencia_desde y vigencia_hasta para la BD
        LocalDate hoy = LocalDate.now();
        nuevaRegla.setVigenciaDesde(hoy);

        if (nuevaRegla.getCaducidadPuntosMeses() != null && nuevaRegla.getCaducidadPuntosMeses() > 0) {
            LocalDate fechaVencimiento = hoy.plusMonths(nuevaRegla.getCaducidadPuntosMeses());
            nuevaRegla.setVigenciaHasta(fechaVencimiento);
        }
        // ------------------------------------------------

        nuevaRegla.setEstadoRegla(ESTADO_ACTIVO); 
        
        if (nuevaRegla.getEquivalenciaPuntos() == null) {
            nuevaRegla.setEquivalenciaPuntos(new BigDecimal("1.00"));
        }
        
        // Asegurar que se cree como nueva
        if (nuevaRegla.getId() != null && nuevaRegla.getId() != 0) {
            nuevaRegla.setId(null); 
        }

        ReglaPuntos reglaGuardada = reglaPuntosRepository.save(nuevaRegla);

        return reglaPuntosMapper.toReglaPuntosDTO(reglaGuardada);
    }
}