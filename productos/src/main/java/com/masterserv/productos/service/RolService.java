package com.masterserv.productos.service;

import com.masterserv.productos.dto.RolDTO;
import com.masterserv.productos.entity.Rol;
import com.masterserv.productos.mapper.RolMapper; // Asumo que crearás este mapper
import com.masterserv.productos.repository.RolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RolService {

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private RolMapper rolMapper; // Necesario para mapear

    /**
     * Obtiene todos los roles disponibles (para formularios y listados).
     */
    @Transactional(readOnly = true)
    public List<RolDTO> findAll() {
        List<Rol> roles = rolRepository.findAll();
        return rolMapper.toRolDTOList(roles);
    }

    /**
     * Crea un nuevo rol.
     */
    @Transactional
    public RolDTO create(RolDTO rolDTO) {
        // Validación de negocio: Evitar duplicados por nombre
        rolRepository.findByNombreRol(rolDTO.getNombreRol()).ifPresent(r -> {
            throw new RuntimeException("Ya existe un rol con el nombre: " + rolDTO.getNombreRol());
        });

        Rol rol = rolMapper.toRol(rolDTO);
        Rol rolGuardado = rolRepository.save(rol);
        return rolMapper.toRolDTO(rolGuardado);
    }

    /**
     * Actualiza un rol existente.
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

        // Actualiza la entidad existente usando el mapper
        rolMapper.updateRolFromDto(rolDTO, rolExistente);
        Rol rolActualizado = rolRepository.save(rolExistente);
        return rolMapper.toRolDTO(rolActualizado);
    }

    /**
     * Elimina un rol por su ID (Borrado Físico - CUIDADO).
     */
    @Transactional
    public void delete(Long id) {
        // En un sistema real, primero se verificaría que ningún usuario esté asignado a este rol.
        if (!rolRepository.existsById(id)) {
            throw new RuntimeException("Rol no encontrado con ID: " + id);
        }
        rolRepository.deleteById(id);
    }
}