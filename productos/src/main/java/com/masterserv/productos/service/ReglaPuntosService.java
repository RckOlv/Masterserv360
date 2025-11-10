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

@Service
public class ReglaPuntosService {

    private static final String ESTADO_ACTIVO = "ACTIVA";

    @Autowired
    private ReglaPuntosRepository reglaPuntosRepository;

    @Autowired
    private ReglaPuntosMapper reglaPuntosMapper;

    /**
     * Obtiene la regla activa actual. Es el método más importante para la lógica de asignación de puntos.
     */
    @Transactional(readOnly = true)
    public Optional<ReglaPuntos> getReglaActiva() {
        // Asumimos que solo existe o debe existir una regla en estado ACTIVA
        return reglaPuntosRepository.findByEstadoRegla(ESTADO_ACTIVO);
    }

    /**
     * Obtiene todas las reglas (para el historial de configuración).
     */
    @Transactional(readOnly = true)
    public List<ReglaPuntosDTO> findAll() {
        return reglaPuntosMapper.toReglaPuntosDTOList(reglaPuntosRepository.findAll());
    }
    
    /**
     * Crea o actualiza la regla de puntos.
     * Si ya existe una regla activa, la desactiva y crea la nueva como ACTIVA.
     */
    @Transactional
    public ReglaPuntosDTO createOrUpdateRegla(ReglaPuntosDTO nuevaReglaDTO) {
        
        // 1. Desactivar la regla anterior si existe
        reglaPuntosRepository.findByEstadoRegla(ESTADO_ACTIVO).ifPresent(reglaAnterior -> {
            reglaAnterior.setEstadoRegla("CADUCADA");
            reglaPuntosRepository.save(reglaAnterior); // Guardar el estado anterior
        });
        
        // 2. Crear la nueva regla
        ReglaPuntos nuevaRegla = reglaPuntosMapper.toReglaPuntos(nuevaReglaDTO);
        nuevaRegla.setEstadoRegla(ESTADO_ACTIVO); // Marcar la nueva como ACTIVA
        
        // Asignar 0 al ID si es una creación nueva
        if (nuevaRegla.getId() != null && nuevaRegla.getId() != 0) {
            nuevaRegla.setId(null); // Forzar la creación de un nuevo registro
        }

        ReglaPuntos reglaGuardada = reglaPuntosRepository.save(nuevaRegla);

        return reglaPuntosMapper.toReglaPuntosDTO(reglaGuardada);
    }


}