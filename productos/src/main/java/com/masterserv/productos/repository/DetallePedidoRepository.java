package com.masterserv.productos.repository;

import com.masterserv.productos.entity.DetallePedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {
    
    // Generalmente, no necesitamos métodos custom aquí, 
    // ya que los detalles casi siempre se acceden a través de su 'Pedido' padre.
}