package com.masterserv.productos.controller;

import com.masterserv.productos.dto.AuditoriaDTO;
import com.masterserv.productos.dto.AuditoriaFiltroDTO; // <--- Import DTO Filtro
import com.masterserv.productos.entity.Auditoria;
import com.masterserv.productos.service.AuditoriaService; // <--- Usamos el Service
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auditoria")
@PreAuthorize("hasRole('ADMIN')")
public class AuditoriaController {

    @Autowired
    private AuditoriaService auditoriaService; // <--- Inyección del Service

    // Endpoint GET normal (Listar todos)
    @GetMapping
    public ResponseEntity<Page<AuditoriaDTO>> getLogs(
            @PageableDefault(page = 0, size = 20) Pageable pageable) {
        
        Page<Auditoria> page = auditoriaService.getLogs(pageable);
        
        // Mapeo a DTO
        return ResponseEntity.ok(mapToDto(page));
    }

    // --- NUEVO ENDPOINT DE FILTRADO ---
    @PostMapping("/filtrar")
    public ResponseEntity<Page<AuditoriaDTO>> filtrarLogs(
            @RequestBody AuditoriaFiltroDTO filtro,
            @PageableDefault(size = 20, sort = "fecha") Pageable pageable) {
        
        Page<Auditoria> page = auditoriaService.filtrarAuditoria(filtro, pageable);
        
        // Mapeo a DTO (Reutilizamos lógica)
        return ResponseEntity.ok(mapToDto(page));
    }

    // Método auxiliar para no repetir código de conversión DTO
    private Page<AuditoriaDTO> mapToDto(Page<Auditoria> page) {
        return page.map(a -> new AuditoriaDTO(
                a.getId(),
                a.getEntidad(),
                a.getEntidadId(),
                a.getAccion(),
                a.getUsuario(),
                a.getFecha(),
                a.getDetalle(),
                a.getValorAnterior(),
                a.getValorNuevo()
        ));
    }
}