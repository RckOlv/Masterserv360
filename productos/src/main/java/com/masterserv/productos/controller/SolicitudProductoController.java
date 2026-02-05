package com.masterserv.productos.controller;

import com.masterserv.productos.dto.SolicitudProductoDTO;
import com.masterserv.productos.entity.SolicitudProducto;
import com.masterserv.productos.repository.ListaEsperaRepository;
import com.masterserv.productos.repository.SolicitudProductoRepository;
import com.masterserv.productos.enums.EstadoListaEspera;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/solicitudes")
@PreAuthorize("hasRole('ADMIN')")
public class SolicitudProductoController {

    @Autowired
    private SolicitudProductoRepository solicitudRepository;

    @Autowired
    private ListaEsperaRepository listaEsperaRepository;

    /**
     * Lista todas las solicitudes convirtiéndolas a DTO.
     * Usamos @Transactional para mantener la sesión abierta y poder leer el usuario Lazy.
     */
    @GetMapping
    @Transactional(readOnly = true) 
    public ResponseEntity<List<SolicitudProductoDTO>> getAllSolicitudes() {
        List<SolicitudProducto> entidades = solicitudRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaSolicitud"));
        
        List<SolicitudProductoDTO> dtos = entidades.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PatchMapping("/{id}/procesar")
    public ResponseEntity<Void> marcarComoProcesada(@PathVariable Long id) {
        return solicitudRepository.findById(id).map(solicitud -> {
            solicitud.setProcesado(true);
            solicitudRepository.save(solicitud);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarSolicitud(@PathVariable Long id) {
        solicitudRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- Helper de Mapeo Manual ---
    private SolicitudProductoDTO mapToDTO(SolicitudProducto entity) {
        SolicitudProductoDTO dto = new SolicitudProductoDTO();
        dto.setId(entity.getId());
        dto.setDescripcion(entity.getDescripcion());
        dto.setFechaSolicitud(entity.getFechaSolicitud());
        dto.setProcesado(entity.isProcesado());
        
        // Aquí forzamos la carga del usuario
        if (entity.getUsuario() != null) {
            dto.setClienteNombre(entity.getUsuario().getNombre() + " " + entity.getUsuario().getApellido());
            dto.setClienteTelefono(entity.getUsuario().getTelefono());
            dto.setClienteEmail(entity.getUsuario().getEmail());

            // --- LÓGICA DE LISTA DE ESPERA (AGREGADO) ---
            // Verificamos si este usuario tiene algo pendiente
            boolean estaEsperando = listaEsperaRepository.existsByUsuarioIdAndEstado(
                    entity.getUsuario().getId(), 
                    EstadoListaEspera.PENDIENTE
            );

            if (estaEsperando) {
                dto.setEstadoListaEspera("PENDIENTE");
            } else {
                // Si no espera, vemos si ya fue notificado
                boolean fueNotificado = listaEsperaRepository.existsByUsuarioIdAndEstado(
                        entity.getUsuario().getId(), 
                        EstadoListaEspera.NOTIFICADA
                );
                dto.setEstadoListaEspera(fueNotificado ? "NOTIFICADO" : "NINGUNO");
            }
            // ----------------------------------------------

        } else {
            dto.setClienteNombre("Anónimo / No registrado");
            dto.setEstadoListaEspera("NINGUNO");
        }
        
        return dto;
    }
}