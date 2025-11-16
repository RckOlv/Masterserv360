package com.masterserv.productos.service;

import com.masterserv.productos.dto.PermisoDTO;
import com.masterserv.productos.entity.Permiso;
import com.masterserv.productos.mapper.PermisoMapper;
import com.masterserv.productos.repository.PermisoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PermisoService {

    @Autowired
    private PermisoRepository permisoRepository;

    @Autowired
    private PermisoMapper permisoMapper;

    @Transactional(readOnly = true)
    public List<PermisoDTO> listarPermisos() {
        List<Permiso> permisos = permisoRepository.findAll(); 
        return permisoMapper.toPermisoDTOList(permisos); 
    }

    // --- Mentor: MÉTODO NUEVO (Para el Formulario) ---
    @Transactional
    public PermisoDTO crear(PermisoDTO permisoDTO) {
        permisoRepository.findByNombrePermiso(permisoDTO.getNombrePermiso()).ifPresent(p -> {
            throw new RuntimeException("Ya existe un permiso con el nombre: " + permisoDTO.getNombrePermiso());
        });
        
        Permiso permiso = permisoMapper.toPermiso(permisoDTO);
        // Asumimos que los permisos nuevos nacen 'ACTIVOS' (si tuvieras estado)
        Permiso guardado = permisoRepository.save(permiso);
        return permisoMapper.toPermisoDTO(guardado);
    }

    // --- Mentor: MÉTODO NUEVO (Para el Formulario) ---
    @Transactional
    public PermisoDTO actualizar(PermisoDTO permisoDTO) {
        Permiso permisoExistente = permisoRepository.findById(permisoDTO.getId())
                .orElseThrow(() -> new RuntimeException("Permiso no encontrado"));

        // Validar nombre duplicado (excluyendo el actual)
        Optional<Permiso> checkNombre = permisoRepository.findByNombrePermiso(permisoDTO.getNombrePermiso());
        if (checkNombre.isPresent() && !checkNombre.get().getId().equals(permisoDTO.getId())) {
             throw new RuntimeException("Ya existe otro permiso con ese nombre.");
        }

        permisoExistente.setNombrePermiso(permisoDTO.getNombrePermiso());
        permisoExistente.setDescripcion(permisoDTO.getDescripcion());
        
        Permiso actualizado = permisoRepository.save(permisoExistente);
        return permisoMapper.toPermisoDTO(actualizado);
    }

    // --- Mentor: MÉTODO NUEVO (Para el Formulario) ---
    @Transactional
    public void softDelete(Long id) {
        // OJO: Tu Permiso.java hereda de AuditableEntity, pero no tiene 'Estado'.
        // Si no tienes 'Estado', el softDelete no es posible y debe ser un delete físico.
        // Asumiremos que quieres un delete físico (como en RolService).
        Permiso permiso = permisoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permiso no encontrado"));
        
        // ADVERTENCIA: Eliminar permisos puede romper el sistema si están asignados.
        // Tu frontend ya tiene un confirm(), lo cual es bueno.
        permisoRepository.delete(permiso);
    }
}