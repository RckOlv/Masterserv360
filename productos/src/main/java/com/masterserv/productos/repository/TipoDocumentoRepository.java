package com.masterserv.productos.repository;

import com.masterserv.productos.entity.TipoDocumento;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoDocumentoRepository extends JpaRepository<TipoDocumento, Long> {
    Optional<TipoDocumento> findByNombreCorto(String nombreCorto);
}