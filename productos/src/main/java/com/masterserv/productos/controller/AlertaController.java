package com.masterserv.productos.controller;

import com.masterserv.productos.entity.Alerta;
import com.masterserv.productos.repository.AlertaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/alertas")
public class AlertaController {

    @Autowired
    private AlertaRepository alertaRepository;

    // Obtener solo las NO leídas (para el numerito rojo)
    @GetMapping("/no-leidas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Alerta>> getAlertasNoLeidas() {
        return ResponseEntity.ok(alertaRepository.findByLeidaFalseOrderByFechaCreacionDesc());
    }

    // Marcar una como leída (al hacer click)
    @PostMapping("/{id}/leer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> marcarComoLeida(@PathVariable Long id) {
        return alertaRepository.findById(id).map(alerta -> {
            alerta.setLeida(true);
            alertaRepository.save(alerta);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
    
    // Marcar TODAS como leídas
    @PostMapping("/leer-todas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> marcarTodasLeidas() {
        List<Alerta> alertas = alertaRepository.findByLeidaFalseOrderByFechaCreacionDesc();
        alertas.forEach(a -> a.setLeida(true));
        alertaRepository.saveAll(alertas);
        return ResponseEntity.ok().build();
    }
}