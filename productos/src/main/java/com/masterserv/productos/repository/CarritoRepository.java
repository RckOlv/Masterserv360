package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Carrito;
import com.masterserv.productos.entity.Usuario;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    
    /**
     * Busca el carrito de un vendedor incluyendo los items (fetch join).
     */
    @org.springframework.data.jpa.repository.Query("SELECT c FROM Carrito c LEFT JOIN FETCH c.items WHERE c.vendedor = :vendedor")
    Optional<Carrito> findByVendedorWithItems(com.masterserv.productos.entity.Usuario vendedor);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Carrito c LEFT JOIN FETCH c.items WHERE c.vendedor = :vendedor")
    Optional<Carrito> findByVendedorWithItemsLock(@Param("vendedor") Usuario vendedor);

    
}