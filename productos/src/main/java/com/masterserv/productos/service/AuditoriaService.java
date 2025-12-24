package com.masterserv.productos.service;

import com.masterserv.productos.dto.AuditoriaFiltroDTO;
import com.masterserv.productos.entity.Auditoria;
import com.masterserv.productos.repository.AuditoriaRepository;
import com.masterserv.productos.specification.AuditoriaSpecification; // <--- Importante
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {

    @Autowired
    private AuditoriaRepository auditoriaRepository; // Cambié 'repo' por 'auditoriaRepository'

    @Autowired
    private AuditoriaSpecification auditoriaSpecification; // <--- Faltaba esto

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardar(Auditoria log) {
        auditoriaRepository.save(log);
    }

    // Método para obtener todos (sin filtro)
    @Transactional(readOnly = true)
    public Page<Auditoria> getLogs(Pageable pageable) {
        return auditoriaRepository.findAllByOrderByFechaDesc(pageable);
    }

    // --- NUEVO MÉTODO DE FILTRADO ---
    @Transactional(readOnly = true)
    public Page<Auditoria> filtrarAuditoria(AuditoriaFiltroDTO filtro, Pageable pageable) {
        // Obtenemos la especificación
        Specification<Auditoria> spec = auditoriaSpecification.getByFilters(filtro);
        // Buscamos usando filtros + paginación
        return auditoriaRepository.findAll(spec, pageable);
    }
}