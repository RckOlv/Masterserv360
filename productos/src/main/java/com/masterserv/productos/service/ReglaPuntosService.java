package com.masterserv.productos.service;

import com.masterserv.productos.dto.ReglaPuntosDTO;
import com.masterserv.productos.entity.ReglaPuntos;
import com.masterserv.productos.repository.ReglaPuntosRepository;
import com.masterserv.productos.mapper.ReglaPuntosMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
// --- Mentor: Importar BigDecimal ---
import java.math.BigDecimal;

@Service
public class ReglaPuntosService {

    private static final String ESTADO_ACTIVO = "ACTIVA";

    @Autowired
    private ReglaPuntosRepository reglaPuntosRepository;

    @Autowired
    private ReglaPuntosMapper reglaPuntosMapper;

    @Transactional(readOnly = true)
    public Optional<ReglaPuntos> getReglaActiva() {
        return reglaPuntosRepository.findByEstadoRegla(ESTADO_ACTIVO);
    }

    @Transactional(readOnly = true)
    public List<ReglaPuntosDTO> findAll() {
        return reglaPuntosMapper.toReglaPuntosDTOList(reglaPuntosRepository.findAll());
    }
    
    @Transactional
    public ReglaPuntosDTO createOrUpdateRegla(ReglaPuntosDTO nuevaReglaDTO) {
        
        // 1. Desactivar la regla anterior si existe
        reglaPuntosRepository.findByEstadoRegla(ESTADO_ACTIVO).ifPresent(reglaAnterior -> {
            reglaAnterior.setEstadoRegla("CADUCADA");
            reglaPuntosRepository.save(reglaAnterior);
        });
        
        // 2. Crear la nueva regla
        ReglaPuntos nuevaRegla = reglaPuntosMapper.toReglaPuntos(nuevaReglaDTO);
        nuevaRegla.setEstadoRegla(ESTADO_ACTIVO); 
        
        // --- Mentor: CORRECCIÓN (Evitar error de Base de Datos) ---
        // Como la BD tiene 'equivalencia_puntos NOT NULL', si viene null del frontend,
        // le ponemos 1.0 por defecto para que no explote.
        if (nuevaRegla.getEquivalenciaPuntos() == null) {
            nuevaRegla.setEquivalenciaPuntos(new BigDecimal("1.00"));
        }
        // --- Mentor: FIN CORRECCIÓN ---
        
        if (nuevaRegla.getId() != null && nuevaRegla.getId() != 0) {
            nuevaRegla.setId(null); 
        }

        ReglaPuntos reglaGuardada = reglaPuntosRepository.save(nuevaRegla);

        return reglaPuntosMapper.toReglaPuntosDTO(reglaGuardada);
    }
}