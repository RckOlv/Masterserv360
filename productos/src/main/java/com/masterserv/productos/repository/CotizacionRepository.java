package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Cotizacion;
import com.masterserv.productos.entity.Proveedor;
import com.masterserv.productos.enums.EstadoCotizacion;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {
    Optional<Cotizacion> findByToken(String token);
    List<Cotizacion> findByEstado(EstadoCotizacion estado);
    
    // --- NUEVO: Freno de mano para el proceso automático ---
    // Verifica si ya existe una cotización 'PENDIENTE' para este proveedor
    boolean existsByProveedorAndEstado(Proveedor proveedor, EstadoCotizacion estado);

    boolean existsByProveedorAndEstadoIn(Proveedor proveedor, Collection<EstadoCotizacion> estados);
}