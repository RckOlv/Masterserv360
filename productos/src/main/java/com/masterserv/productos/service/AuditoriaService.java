package com.masterserv.productos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.masterserv.productos.entity.Auditoria;
import com.masterserv.productos.repository.AuditoriaRepository;

@Service
public class AuditoriaService {

    @Autowired
    private AuditoriaRepository repo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardar(Auditoria log) {
        repo.save(log);
    }
}
