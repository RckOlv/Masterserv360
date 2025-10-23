package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.entity.Venta;
import com.masterserv.productos.enums.EstadoVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    // --- Métodos para Reportes ---

    // Buscar ventas registradas por un vendedor
    List<Venta> findByVendedor(Usuario vendedor);

    // Buscar todas las compras de un cliente
    List<Venta> findByCliente(Usuario cliente);

    // Buscar ventas por estado
    List<Venta> findByEstado(EstadoVenta estado);

    // Buscar ventas en un rango de fechas
    List<Venta> findByFechaVentaBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);
}