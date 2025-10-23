package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Carrito;
import com.masterserv.productos.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CarritoRepository extends JpaRepository<Carrito, Long> {

    /**
     * Busca el carrito (único) asociado a un vendedor.
     * Esta es la consulta clave para el flujo de "venta en local".
     */
    Optional<Carrito> findByVendedor(Usuario vendedor);

    Optional<Carrito> findByVendedor_Id(Long vendedorId);
}