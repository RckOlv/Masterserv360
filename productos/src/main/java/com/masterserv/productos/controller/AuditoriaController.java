package com.masterserv.productos.controller;

import com.masterserv.productos.dto.AuditoriaDTO;
import com.masterserv.productos.entity.Auditoria;
import com.masterserv.productos.repository.AuditoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("hasRole('ADMIN')")
public class AuditoriaController {

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @GetMapping
    public ResponseEntity<Page<AuditoriaDTO>> getLogs(
            @PageableDefault(page = 0, size = 20) Pageable pageable) {
        
        Page<Auditoria> page = auditoriaRepository.findAllByOrderByFechaDesc(pageable);
        
        // MENTOR: CORRECCIÓN AQUÍ - Agregamos los campos nuevos al constructor
        Page<AuditoriaDTO> dtoPage = page.map(a -> new AuditoriaDTO(
                a.getId(),
                a.getEntidad(),
                a.getEntidadId(),
                a.getAccion(),
                a.getUsuario(),
                a.getFecha(),
                a.getDetalle(),
                a.getValorAnterior(), // Nuevo campo
                a.getValorNuevo()     // Nuevo campo
        ));

        return ResponseEntity.ok(dtoPage);
    }
}