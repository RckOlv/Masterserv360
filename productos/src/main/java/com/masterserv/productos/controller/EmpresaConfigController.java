package com.masterserv.productos.controller;

import com.masterserv.productos.entity.EmpresaConfig;
import com.masterserv.productos.service.EmpresaConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/configuracion")
@CrossOrigin(origins = "*") // Ajusta según tu seguridad
public class EmpresaConfigController {

    @Autowired
    private EmpresaConfigService service;

    @GetMapping
    public ResponseEntity<EmpresaConfig> obtener() {
        return ResponseEntity.ok(service.obtenerConfiguracion());
    }

    @PutMapping
    public ResponseEntity<EmpresaConfig> actualizar(@RequestBody EmpresaConfig config) {
        return ResponseEntity.ok(service.actualizarConfiguracion(config));
    }
}