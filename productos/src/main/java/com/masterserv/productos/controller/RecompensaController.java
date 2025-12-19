package com.masterserv.productos.controller;

import com.masterserv.productos.dto.RecompensaDTO;
import com.masterserv.productos.service.RecompensaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recompensas")
@PreAuthorize("hasAuthority('ROLES_MANAGE')") // Solo Admins pueden gestionar recompensas
public class RecompensaController {

    @Autowired
    private RecompensaService recompensaService;

    @PostMapping
    public ResponseEntity<RecompensaDTO> crearRecompensa(@Valid @RequestBody RecompensaDTO dto) {
        RecompensaDTO creada = recompensaService.crear(dto);
        return new ResponseEntity<>(creada, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecompensaDTO> actualizarRecompensa(@PathVariable Long id, @Valid @RequestBody RecompensaDTO dto) {
        RecompensaDTO actualizada = recompensaService.actualizar(id, dto);
        return ResponseEntity.ok(actualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarRecompensa(@PathVariable Long id) {
        recompensaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}