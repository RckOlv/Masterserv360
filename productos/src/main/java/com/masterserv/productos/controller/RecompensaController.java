package com.masterserv.productos.controller;

import com.masterserv.productos.dto.RecompensaDTO;
import com.masterserv.productos.service.RecompensaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recompensas")
public class RecompensaController {

    @Autowired
    private RecompensaService recompensaService;

    // --- PÚBLICO / CLIENTES ---
    
    @GetMapping("/disponibles")
    public ResponseEntity<List<RecompensaDTO>> obtenerDisponibles() {
        return ResponseEntity.ok(recompensaService.findDisponibles());
    }

    // --- ADMIN ---

    @GetMapping
    @PreAuthorize("hasAuthority('ROLES_MANAGE')")
    public ResponseEntity<List<RecompensaDTO>> listarTodas() {
        return ResponseEntity.ok(recompensaService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLES_MANAGE')")
    public ResponseEntity<RecompensaDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(recompensaService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLES_MANAGE')")
    public ResponseEntity<RecompensaDTO> crearRecompensa(@Valid @RequestBody RecompensaDTO dto) {
        RecompensaDTO creada = recompensaService.crear(dto);
        return new ResponseEntity<>(creada, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLES_MANAGE')")
    public ResponseEntity<RecompensaDTO> actualizarRecompensa(@PathVariable Long id, @Valid @RequestBody RecompensaDTO dto) {
        RecompensaDTO actualizada = recompensaService.actualizar(id, dto);
        return ResponseEntity.ok(actualizada);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLES_MANAGE')")
    public ResponseEntity<Void> eliminarRecompensa(@PathVariable Long id) {
        recompensaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}