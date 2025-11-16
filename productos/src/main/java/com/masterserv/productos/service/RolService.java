package com.masterserv.productos.service;

import com.masterserv.productos.dto.PermisoDTO;
import com.masterserv.productos.dto.RolDTO;
import com.masterserv.productos.entity.Permiso;
import com.masterserv.productos.entity.Rol;
import com.masterserv.productos.mapper.RolMapper;
import com.masterserv.productos.mapper.PermisoMapper;
import com.masterserv.productos.repository.PermisoRepository;
import com.masterserv.productos.repository.RolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RolService {

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private RolMapper rolMapper;

    @Autowired
    private PermisoRepository permisoRepository;
    @Autowired
    private PermisoMapper permisoMapper;

    /**
     * Helper para buscar las entidades Permiso reales a partir de sus DTOs.
     * Esto es CRÍTICO para que JPA pueda manejar la colección ManyToMany.
     */
    private Set<Permiso> getPermissoEntities(List<PermisoDTO> permisoDTOs) {
        if (permisoDTOs == null || permisoDTOs.isEmpty()) {
            return Collections.emptySet();
        }
        
        // Extraer todos los IDs de los permisos
        Set<Long> ids = permisoDTOs.stream()
                .map(PermisoDTO::getId)
                .collect(Collectors.toSet());
        
        // Buscar las entidades Permiso reales en la BD
        List<Permiso> permisos = permisoRepository.findAllById(ids);
        
        // Verificación de integridad: todos los IDs deben existir
        if (permisos.size() != ids.size()) {
             throw new RuntimeException("Error de integridad: Uno o más permisos seleccionados no existen.");
        }
        
        return new HashSet<>(permisos);
    }

    /**
     * Obtiene todos los roles disponibles (para formularios y listados).
     */
    @Transactional(readOnly = true)
    public List<RolDTO> findAll() {
        List<Rol> roles = rolRepository.findAll();
        return rolMapper.toRolDTOList(roles);
    }

    /**
     * Crea un nuevo rol, asignando sus permisos.
     */
    @Transactional
    public RolDTO create(RolDTO rolDTO) {
        // Validación de negocio: Evitar duplicados por nombre
        rolRepository.findByNombreRol(rolDTO.getNombreRol()).ifPresent(r -> {
            throw new RuntimeException("Ya existe un rol con el nombre: " + rolDTO.getNombreRol());
        });

        Rol rol = rolMapper.toRol(rolDTO);
        
        // 1. Sincronizar permisos (añadiendo la colección)
        Set<Permiso> permisos = getPermissoEntities(rolDTO.getPermisos());
        rol.setPermisos(permisos);
        
        Rol rolGuardado = rolRepository.save(rol);
        return rolMapper.toRolDTO(rolGuardado);
    }

    /**
     * Actualiza un rol existente, incluyendo la sincronización de permisos.
     */
    @Transactional
    public RolDTO update(Long id, RolDTO rolDTO) {
        Rol rolExistente = rolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado con ID: " + id));

        // Validación de negocio: Evitar duplicados por nombre
        rolRepository.findByNombreRol(rolDTO.getNombreRol()).ifPresent(r -> {
            if (!r.getId().equals(id)) {
                throw new RuntimeException("Ya existe otro rol con ese nombre: " + rolDTO.getNombreRol());
            }
        });

        // 1. Actualiza la entidad existente (nombre, descripción)
        rolMapper.updateRolFromDto(rolDTO, rolExistente);
        
        // 2. Sincronizar permisos (CRÍTICO)
        Set<Permiso> nuevosPermisos = getPermissoEntities(rolDTO.getPermisos());
        
        // Limpiar la colección antigua (Borra las entradas en roles_permisos)
        rolExistente.getPermisos().clear(); 
        
        // Añadir la colección nueva
        rolExistente.getPermisos().addAll(nuevosPermisos);

        Rol rolActualizado = rolRepository.save(rolExistente);
        return rolMapper.toRolDTO(rolActualizado);
    }

    /**
     * Elimina un rol por su ID (Borrado Físico - CUIDADO).
     */
    @Transactional
    public void delete(Long id) {
        if (!rolRepository.existsById(id)) {
            throw new RuntimeException("Rol no encontrado con ID: " + id);
        }
        rolRepository.deleteById(id);
    }

    @Transactional(readOnly = true) 
    public Optional<Rol> findByNombreRol(String nombreRol) {
        return rolRepository.findByNombreRol(nombreRol);
    }
}