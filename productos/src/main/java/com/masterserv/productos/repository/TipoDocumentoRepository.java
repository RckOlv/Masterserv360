package com.masterserv.productos.repository;

import com.masterserv.productos.entity.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoDocumentoRepository extends JpaRepository<TipoDocumento, Long> {
    // Spring Data JPA crea automáticamente los métodos CRUD
}