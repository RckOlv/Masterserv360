package com.masterserv.productos.controller;

import com.masterserv.productos.dto.RolDTO;
import com.masterserv.productos.entity.Rol; // ¡Asegúrate de importar Rol!
import com.masterserv.productos.service.RolService;
import com.masterserv.productos.mapper.RolMapper; // Importar RolMapper
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional; // Importar Optional si no está

@RestController
@RequestMapping("/api/roles")
public class RolController {

    @Autowired
    private RolService rolService;

    @Autowired
    private RolMapper rolMapper;

    // --- Tus Endpoints CRUD (Están perfectos) ---
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RolDTO>> getAllRoles() {
        return ResponseEntity.ok(rolService.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RolDTO> createRol(@Valid @RequestBody RolDTO rolDTO) {
        RolDTO nuevoRol = rolService.create(rolDTO);
        return new ResponseEntity<>(nuevoRol, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RolDTO> updateRol(@PathVariable Long id, @Valid @RequestBody RolDTO rolDTO) {
        RolDTO rolActualizado = rolService.update(id, rolDTO);
        return ResponseEntity.ok(rolActualizado);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteRol(@PathVariable Long id) {
        rolService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Rol eliminado exitosamente"));
    }


    // --- Endpoint /by-nombre (CON CORRECCIÓN) ---
    /**
     * Busca un rol por su nombre exacto.
     * Usado por el frontend para obtener el ID dinámicamente.
     * URL: /api/roles/by-nombre?nombre=ROLE_CLIENTE
     */
    @GetMapping("/by-nombre")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RolDTO> getRolByNombre(@RequestParam String nombre) {
        // Obtenemos el Optional<Rol> del servicio
        Optional<Rol> rolOptional = rolService.findByNombreRol(nombre);

        // --- CORRECCIÓN AQUÍ ---
        // Añadimos el tipo explícito '(Rol rol)' a la lambda para ayudar al compilador
        return rolOptional.map((Rol rol) -> ResponseEntity.ok(rolMapper.toRolDTO(rol)))
                .orElse(ResponseEntity.notFound().build()); // Si el Optional está vacío, devuelve 404
        // -----------------------
    }
}