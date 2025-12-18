package com.masterserv.productos.service;

import com.masterserv.productos.dto.ProveedorDTO;
import com.masterserv.productos.entity.Categoria; 
import com.masterserv.productos.entity.Proveedor;
import com.masterserv.productos.enums.EstadoUsuario; // <--- IMPORTANTE: Importar el Enum
import com.masterserv.productos.mapper.ProveedorMapper;
import com.masterserv.productos.repository.CategoriaRepository; 
import com.masterserv.productos.repository.ProveedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set; 
import java.util.stream.Collectors; 

@Service
public class ProveedorService {

    @Autowired
    private ProveedorRepository proveedorRepository;
    @Autowired
    private CategoriaRepository categoriaRepository; 
    @Autowired
    private ProveedorMapper proveedorMapper;

    /**
     * CORREGIDO: Lista proveedores filtrando por estado usando ENUMS
     */
    @Transactional(readOnly = true)
    public List<ProveedorDTO> findAll(String estadoStr) {
        List<Proveedor> proveedores;

        // Convertimos el String que llega del Controller a lógica de Enum
        if ("TODOS".equalsIgnoreCase(estadoStr)) {
            proveedores = proveedorRepository.findAll(); 
        } else if ("INACTIVO".equalsIgnoreCase(estadoStr)) {
            // Pasamos el ENUM, no el String
            proveedores = proveedorRepository.findByEstado(EstadoUsuario.INACTIVO); 
        } else {
            // Por defecto, trae solo activos (pasando el ENUM)
            proveedores = proveedorRepository.findByEstado(EstadoUsuario.ACTIVO); 
        }
        return proveedorMapper.toProveedorDTOList(proveedores);
    }

    @Transactional(readOnly = true)
    public ProveedorDTO findById(Long id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con id: " + id));
        return proveedorMapper.toProveedorDTO(proveedor);
    }

    @Transactional
    public ProveedorDTO create(ProveedorDTO proveedorDTO) {
        proveedorRepository.findByCuit(proveedorDTO.getCuit()).ifPresent(p -> {
            throw new RuntimeException("Ya existe un proveedor con el CUIT: " + proveedorDTO.getCuit());
        });

        Proveedor proveedor = proveedorMapper.toProveedor(proveedorDTO);
        
        // CORREGIDO: Setear Enum, no String
        // (Asegúrate que tu Entidad Proveedor tenga el campo: private EstadoUsuario estado;)
        proveedor.setEstado(EstadoUsuario.ACTIVO); 
        
        if (proveedorDTO.getCategoriaIds() != null && !proveedorDTO.getCategoriaIds().isEmpty()) {
            Set<Categoria> categorias = proveedorDTO.getCategoriaIds().stream()
                .map(id -> categoriaRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + id)))
                .collect(Collectors.toSet());
            proveedor.setCategorias(categorias);
        }

        Proveedor proveedorGuardado = proveedorRepository.save(proveedor);
        return proveedorMapper.toProveedorDTO(proveedorGuardado);
    }

    @Transactional
    public ProveedorDTO update(Long id, ProveedorDTO proveedorDTO) {
        Proveedor proveedorExistente = proveedorRepository.findById(id) 
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con id: " + id));

        proveedorRepository.findByCuit(proveedorDTO.getCuit()).ifPresent(p -> {
            if (!p.getId().equals(id)) {
                 throw new RuntimeException("Ya existe OTRO proveedor con el CUIT: " + proveedorDTO.getCuit());
            }
        });

        proveedorMapper.updateProveedorFromDto(proveedorDTO, proveedorExistente);
        
        if (proveedorDTO.getCategoriaIds() != null) {
             Set<Categoria> categorias = proveedorDTO.getCategoriaIds().stream()
                .map(catId -> categoriaRepository.findById(catId)
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + catId)))
                .collect(Collectors.toSet());
            proveedorExistente.setCategorias(categorias);
        } else {
             proveedorExistente.getCategorias().clear(); 
        }

        // CORREGIDO: Manejo de Enum en update
        if (proveedorDTO.getEstado() != null) {
            try {
                // Asumiendo que el DTO trae el estado como String, lo convertimos a Enum
                EstadoUsuario nuevoEstado = EstadoUsuario.valueOf(proveedorDTO.getEstado().toUpperCase());
                proveedorExistente.setEstado(nuevoEstado);
            } catch (IllegalArgumentException e) {
                // Si mandan un estado inválido, lo ignoramos o lanzamos error
                // System.out.println("Estado inválido: " + proveedorDTO.getEstado());
            }
        }
        
        Proveedor actualizado = proveedorRepository.save(proveedorExistente);
        return this.findById(actualizado.getId()); 
    }

    @Transactional
    public void softDelete(Long id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con id: " + id));
        // CORREGIDO: Usar Enum
        proveedor.setEstado(EstadoUsuario.INACTIVO); 
        proveedorRepository.save(proveedor);
    }
    
     @Transactional
     public void reactivar(Long id) {
         Proveedor proveedor = proveedorRepository.findById(id)
                 .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con id: " + id));
         // CORREGIDO: Usar Enum
         proveedor.setEstado(EstadoUsuario.ACTIVO);
         proveedorRepository.save(proveedor);
     }
}