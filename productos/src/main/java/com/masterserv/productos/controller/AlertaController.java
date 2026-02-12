package com.masterserv.productos.controller;

import com.masterserv.productos.entity.Alerta;
import com.masterserv.productos.service.AlertaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
public class AlertaController {

    private final AlertaService alertaService;

    // GET: Para ver si hay puntito rojo
    @GetMapping("/no-leidas")
    public ResponseEntity<List<Alerta>> getNoLeidas() {
        return ResponseEntity.ok(alertaService.obtenerNoLeidas());
    }

    // PUT: Cuando haces clic en la campana o en "Marcar leída"
    @PutMapping("/{id}/leer")
    public ResponseEntity<Void> marcarLeida(@PathVariable Long id) {
        alertaService.marcarComoLeida(id);
        return ResponseEntity.ok().build();
    }
}