package com.masterserv.productos.controller; // Ajusta el paquete

import com.masterserv.productos.dto.AbrirCajaDTO;
import com.masterserv.productos.dto.CerrarCajaDTO;
import com.masterserv.productos.dto.RetiroCajaDTO;
import com.masterserv.productos.entity.Caja;
import com.masterserv.productos.service.CajaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/caja")
@CrossOrigin(origins = "*") // Ajusta los orígenes según tu seguridad
public class CajaController {

    @Autowired
    private CajaService cajaService;

    @GetMapping("/abierta/{usuarioId}")
    public ResponseEntity<?> verificarCajaAbierta(@PathVariable Long usuarioId) {
        Caja caja = cajaService.obtenerCajaAbierta(usuarioId);
        if (caja == null) {
            return ResponseEntity.ok(null); // Devuelve null si no hay caja abierta
        }
        return ResponseEntity.ok(caja);
    }

    @PostMapping("/abrir")
    public ResponseEntity<Caja> abrirCaja(@RequestBody AbrirCajaDTO dto) {
        return ResponseEntity.ok(cajaService.abrirCaja(dto));
    }

    @PostMapping("/cerrar")
    public ResponseEntity<Caja> cerrarCaja(@RequestBody CerrarCajaDTO dto) {
        return ResponseEntity.ok(cajaService.cerrarCaja(dto));
    }

	@PostMapping("/retiro")
    public ResponseEntity<Caja> registrarRetiro(@RequestBody RetiroCajaDTO dto) {
        return ResponseEntity.ok(cajaService.registrarRetiro(dto));
    }
}