package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Pedido;
import com.masterserv.productos.entity.Proveedor;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.enums.EstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Método para buscar todos los pedidos de un proveedor
    List<Pedido> findByProveedor(Proveedor proveedor);

    // Método para buscar todos los pedidos registrados por un empleado
    List<Pedido> findByUsuario(Usuario usuario);

    // Método para buscar pedidos por estado
    List<Pedido> findByEstado(EstadoPedido estado);

    // Método para buscar pedidos en un rango de fechas
    List<Pedido> findByFechaPedidoBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);
}