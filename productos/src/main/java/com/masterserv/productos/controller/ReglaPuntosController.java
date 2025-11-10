package com.masterserv.productos.controller;

import com.masterserv.productos.dto.ReglaPuntosDTO;
import com.masterserv.productos.entity.ReglaPuntos;
import com.masterserv.productos.service.ReglaPuntosService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reglas-puntos")
public class ReglaPuntosController {

    @Autowired
    private ReglaPuntosService reglaPuntosService;

    @GetMapping("/activa")
    public ResponseEntity<ReglaPuntos> getReglaActiva() {
        Optional<ReglaPuntos> regla = reglaPuntosService.getReglaActiva();
        return regla.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReglaPuntosDTO>> getAllReglas() {
        List<ReglaPuntosDTO> reglas = reglaPuntosService.findAll();
        return ResponseEntity.ok(reglas);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReglaPuntosDTO> createOrUpdateRegla(@Valid @RequestBody ReglaPuntosDTO reglaPuntosDTO) {
        ReglaPuntosDTO nuevaRegla = reglaPuntosService.createOrUpdateRegla(reglaPuntosDTO);
        return new ResponseEntity<>(nuevaRegla, HttpStatus.CREATED);
    }
}
