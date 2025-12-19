package com.masterserv.productos.controller;

import com.masterserv.productos.entity.TipoDocumento;
import com.masterserv.productos.repository.TipoDocumentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/tipos-documento")
public class TipoDocumentoController {

    @Autowired
    private TipoDocumentoRepository tipoDocumentoRepository;

    @GetMapping
    public ResponseEntity<List<TipoDocumento>> getAllTiposDocumento() {
        return ResponseEntity.ok(tipoDocumentoRepository.findAll());
    }
}