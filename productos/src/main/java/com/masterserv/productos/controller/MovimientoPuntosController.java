package com.masterserv.productos.controller;

import com.masterserv.productos.dto.MovimientoPuntosDTO;
import com.masterserv.productos.service.MovimientoPuntosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movimientos-puntos")
public class MovimientoPuntosController {

    @Autowired
    private MovimientoPuntosService movimientoPuntosService;

    @GetMapping
    public List<MovimientoPuntosDTO> getAllMovimientosPuntos() {
        return movimientoPuntosService.getAllMovimientosPuntos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovimientoPuntosDTO> getMovimientoPuntosById(@PathVariable Long id) {
        return movimientoPuntosService.getMovimientoPuntosById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public MovimientoPuntosDTO createMovimientoPuntos(@RequestBody MovimientoPuntosDTO movimientoPuntosDTO) {
        return movimientoPuntosService.createMovimientoPuntos(movimientoPuntosDTO);
    }

    @PutMapping("/{id}")
    public MovimientoPuntosDTO updateMovimientoPuntos(@PathVariable Long id, @RequestBody MovimientoPuntosDTO movimientoPuntosDTO) {
        return movimientoPuntosService.updateMovimientoPuntos(id, movimientoPuntosDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovimientoPuntos(@PathVariable Long id) {
        movimientoPuntosService.deleteMovimientoPuntos(id);
        return ResponseEntity.noContent().build();
    }
}
