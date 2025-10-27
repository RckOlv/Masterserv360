package com.masterserv.productos.service;

import com.masterserv.productos.dto.ProveedorDTO;
import com.masterserv.productos.entity.Categoria; 
import com.masterserv.productos.entity.Proveedor;
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
     * CORREGIDO: Lista proveedores filtrando por estado
     */
    @Transactional(readOnly = true)
    public List<ProveedorDTO> findAll(String estado) {
        List<Proveedor> proveedores;

        if ("TODOS".equalsIgnoreCase(estado)) {
            // Llama al findAll() que sobreescribimos
            proveedores = proveedorRepository.findAll(); 
        } else if ("INACTIVO".equalsIgnoreCase(estado)) {
            // Llama al findByEstado() con @EntityGraph
            proveedores = proveedorRepository.findByEstado("INACTIVO"); 
        } else {
            // Por defecto, trae solo activos
            proveedores = proveedorRepository.findByEstado("ACTIVO"); 
        }
        return proveedorMapper.toProveedorDTOList(proveedores);
    }

    /**
     * CORREGIDO: Usa el findById() que sobreescribimos
     */
    @Transactional(readOnly = true)
    public ProveedorDTO findById(Long id) {
        Proveedor proveedor = proveedorRepository.findById(id) // Llama al findById() con @EntityGraph
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con id: " + id));
        return proveedorMapper.toProveedorDTO(proveedor);
    }

    @Transactional
    public ProveedorDTO create(ProveedorDTO proveedorDTO) {
        proveedorRepository.findByCuit(proveedorDTO.getCuit()).ifPresent(p -> {
            throw new RuntimeException("Ya existe un proveedor con el CUIT: " + proveedorDTO.getCuit());
        });

        Proveedor proveedor = proveedorMapper.toProveedor(proveedorDTO);
        proveedor.setEstado("ACTIVO");
        
        // Lógica M:N para Categorías
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
        // Usamos findById normal aquí, ya que el 'update' necesita el objeto base
        Proveedor proveedorExistente = proveedorRepository.findById(id) 
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con id: " + id));

        proveedorRepository.findByCuit(proveedorDTO.getCuit()).ifPresent(p -> {
            if (!p.getId().equals(id)) {
                 throw new RuntimeException("Ya existe OTRO proveedor con el CUIT: " + proveedorDTO.getCuit());
            }
        });

        proveedorMapper.updateProveedorFromDto(proveedorDTO, proveedorExistente);
        
        // Lógica M:N para Categorías
        if (proveedorDTO.getCategoriaIds() != null) {
             Set<Categoria> categorias = proveedorDTO.getCategoriaIds().stream()
                .map(catId -> categoriaRepository.findById(catId)
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + catId)))
                .collect(Collectors.toSet());
            proveedorExistente.setCategorias(categorias);
        } else {
             proveedorExistente.getCategorias().clear(); // Si manda lista vacía, se quitan
        }

        if (proveedorDTO.getEstado() != null) {
            proveedorExistente.setEstado(proveedorDTO.getEstado());
        }
        
        Proveedor actualizado = proveedorRepository.save(proveedorExistente);
        // Devolvemos el DTO (cargará las categorías gracias al @EntityGraph de findById)
        return this.findById(actualizado.getId()); 
    }

    @Transactional
    public void softDelete(Long id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con id: " + id));
        proveedor.setEstado("INACTIVO");
        proveedorRepository.save(proveedor);
    }
    
     @Transactional
     public void reactivar(Long id) {
         Proveedor proveedor = proveedorRepository.findById(id)
                 .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con id: " + id));
         proveedor.setEstado("ACTIVO");
         proveedorRepository.save(proveedor);
     }
}