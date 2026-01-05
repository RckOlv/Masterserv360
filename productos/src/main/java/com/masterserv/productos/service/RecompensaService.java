package com.masterserv.productos.service;

import com.masterserv.productos.dto.RecompensaDTO;
import com.masterserv.productos.entity.Categoria;
import com.masterserv.productos.entity.Recompensa;
import com.masterserv.productos.mapper.RecompensaMapper;
import com.masterserv.productos.repository.CategoriaRepository;
import com.masterserv.productos.repository.RecompensaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecompensaService {

    @Autowired
    private RecompensaRepository recompensaRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private RecompensaMapper recompensaMapper;

    // --- LECTURA ---

    @Transactional(readOnly = true)
    public List<RecompensaDTO> findAll() {
        return recompensaRepository.findAll().stream()
                .map(recompensaMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecompensaDTO> findDisponibles() {
        // Filtramos en memoria (o podrías hacer una query en el repo)
        return recompensaRepository.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getActivo()) && r.getStock() > 0)
                .map(recompensaMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RecompensaDTO findById(Long id) {
        Recompensa recompensa = recompensaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recompensa no encontrada: " + id));
        return recompensaMapper.toDto(recompensa);
    }

    // --- ESCRITURA ---

    @Transactional
    public RecompensaDTO crear(RecompensaDTO dto) {
        Recompensa recompensa = recompensaMapper.toEntity(dto);

        // Validar y asignar Categoría (Opcional)
        asignarCategoria(recompensa, dto.getCategoriaId());

        // Valores por defecto
        if (recompensa.getActivo() == null) recompensa.setActivo(true);
        if (recompensa.getStock() == null) recompensa.setStock(0);

        Recompensa guardada = recompensaRepository.save(recompensa);
        return recompensaMapper.toDto(guardada);
    }

    @Transactional
    public RecompensaDTO actualizar(Long id, RecompensaDTO dto) {
        Recompensa recompensa = recompensaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recompensa no encontrada: " + id));

        recompensa.setDescripcion(dto.getDescripcion());
        recompensa.setPuntosRequeridos(dto.getPuntosRequeridos());
        recompensa.setTipoDescuento(dto.getTipoDescuento());
        recompensa.setValor(dto.getValor());
        recompensa.setStock(dto.getStock());

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