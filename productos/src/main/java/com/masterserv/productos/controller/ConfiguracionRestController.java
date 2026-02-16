package com.masterserv.productos.controller;

import com.masterserv.productos.entity.EmpresaConfig;
import com.masterserv.productos.service.EmpresaConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configuracion")
public class ConfiguracionRestController {

    @Autowired
    private EmpresaConfigService empresaConfigService;

    @GetMapping("/publica") // Endpoint público para que el login/sidebar lo consuma
    public ResponseEntity<EmpresaConfig> obtenerConfiguracionPublica() {
        return ResponseEntity.ok(empresaConfigService.obtenerConfiguracion());
    }
}