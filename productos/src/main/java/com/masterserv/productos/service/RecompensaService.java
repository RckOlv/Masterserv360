package com.masterserv.productos.service;

import com.masterserv.productos.dto.RecompensaDTO;
import com.masterserv.productos.entity.Categoria;
import com.masterserv.productos.entity.Recompensa;
import com.masterserv.productos.entity.ReglaPuntos;
import com.masterserv.productos.mapper.RecompensaMapper;
import com.masterserv.productos.repository.CategoriaRepository;
import com.masterserv.productos.repository.RecompensaRepository;
import com.masterserv.productos.repository.ReglaPuntosRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecompensaService {

    @Autowired
    private RecompensaRepository recompensaRepository;
    @Autowired
    private ReglaPuntosRepository reglaPuntosRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private RecompensaMapper recompensaMapper;

    @Transactional
    public RecompensaDTO crear(RecompensaDTO dto) {
        // El mapper ya se encarga de pasar el stock del DTO a la Entidad
        Recompensa recompensa = recompensaMapper.toEntity(dto);

        // 1. Validar y asignar la Regla de Puntos (Obligatoria)
        ReglaPuntos regla = reglaPuntosRepository.findById(dto.getReglaPuntosId())
                .orElseThrow(() -> new EntityNotFoundException("Regla de Puntos no encontrada: " + dto.getReglaPuntosId()));
        recompensa.setReglaPuntos(regla);

        // 2. Validar y asignar la Categoría (Opcional)
        asignarCategoria(recompensa, dto.getCategoriaId());

        Recompensa guardada = recompensaRepository.save(recompensa);
        return recompensaMapper.toDto(guardada);
    }

    @Transactional
    public RecompensaDTO actualizar(Long id, RecompensaDTO dto) {
        Recompensa recompensa = recompensaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recompensa no encontrada: " + id));

        // Actualizar campos manualmente
        recompensa.setDescripcion(dto.getDescripcion());
        recompensa.setPuntosRequeridos(dto.getPuntosRequeridos());
        recompensa.setTipoDescuento(dto.getTipoDescuento());
        recompensa.setValor(dto.getValor());
        
        // --- MENTOR: CORRECCIÓN CRÍTICA ---
        // Faltaba actualizar el stock al editar
        recompensa.setStock(dto.getStock()); 
        // ----------------------------------

        // 2. Validar y asignar la Categoría (Opcional)
        asignarCategoria(recompensa, dto.getCategoriaId());
        
        Recompensa actualizada = recompensaRepository.save(recompensa);
        return recompensaMapper.toDto(actualizada);
    }

    @Transactional
    public void eliminar(Long id) {
        Recompensa recompensa = recompensaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recompensa no encontrada: " + id));
        recompensaRepository.delete(recompensa);
    }

    // --- Helper para no repetir código ---
    private void asignarCategoria(Recompensa recompensa, Long categoriaId) {
        if (categoriaId != null) {
            Categoria categoria = categoriaRepository.findById(categoriaId)
                    .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada: " + categoriaId));
            recompensa.setCategoria(categoria);
        } else {
            recompensa.setCategoria(null);
        }
    }
}