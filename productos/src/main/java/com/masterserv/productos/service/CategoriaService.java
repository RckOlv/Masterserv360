package com.masterserv.productos.service;

import com.masterserv.productos.dto.CategoriaDTO;
import com.masterserv.productos.entity.Categoria;
import com.masterserv.productos.mapper.CategoriaMapper;
import com.masterserv.productos.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors; 

@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private CategoriaMapper categoriaMapper;

    // --- ¡MÉTODO CORREGIDO! ---
    @Transactional(readOnly = true)
    public List<CategoriaDTO> findAll(String estado) { 
        List<Categoria> categorias;

        if ("TODOS".equalsIgnoreCase(estado)) {
            // Si el filtro es "TODOS", trae todo
            categorias = categoriaRepository.findAll();
        } else if ("INACTIVO".equalsIgnoreCase(estado)) {
            // Si es "INACTIVO", trae solo inactivos
            categorias = categoriaRepository.findByEstado("INACTIVO");
        } else {
            // Por defecto (o si estado es "ACTIVO" o null), trae solo activos
            categorias = categoriaRepository.findByEstado("ACTIVO");
        }

        return categoriaMapper.toCategoriaDTOList(categorias);
    }
    // -------------------------

    @Transactional(readOnly = true)
    public CategoriaDTO findById(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + id));
        return categoriaMapper.toCategoriaDTO(categoria);
    }

    @Transactional
    public CategoriaDTO create(CategoriaDTO categoriaDTO) {
        categoriaRepository.findByNombre(categoriaDTO.getNombre()).ifPresent(c -> {
            throw new RuntimeException("Ya existe una categoría con el nombre: " + categoriaDTO.getNombre());
        });

        Categoria categoria = categoriaMapper.toCategoria(categoriaDTO);
        categoria.setEstado("ACTIVO"); 
        Categoria categoriaGuardada = categoriaRepository.save(categoria);
        return categoriaMapper.toCategoriaDTO(categoriaGuardada);
    }

    @Transactional
    public CategoriaDTO update(Long id, CategoriaDTO categoriaDTO) {
        Categoria categoriaExistente = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + id));

        categoriaRepository.findByNombre(categoriaDTO.getNombre()).ifPresent(c -> {
            if (!c.getId().equals(id)) {
                 throw new RuntimeException("Ya existe otra categoría con el nombre: " + categoriaDTO.getNombre());
            }
        });

        categoriaMapper.updateCategoriaFromDto(categoriaDTO, categoriaExistente);
        // El estado se actualiza si viene en el DTO
        if (categoriaDTO.getEstado() != null) {
            categoriaExistente.setEstado(categoriaDTO.getEstado());
        }
        
        Categoria categoriaActualizada = categoriaRepository.save(categoriaExistente);
        return categoriaMapper.toCategoriaDTO(categoriaActualizada);
    }

    @Transactional
    public void softDelete(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + id));
        
        categoria.setEstado("INACTIVO");
        categoriaRepository.save(categoria);
    }
    
     @Transactional
     public void reactivar(Long id) {
         Categoria categoria = categoriaRepository.findById(id)
                 .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + id));
         categoria.setEstado("ACTIVO");
         categoriaRepository.save(categoria);
     }
}