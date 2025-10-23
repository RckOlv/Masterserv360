package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Carrito;
import com.masterserv.productos.entity.ItemCarrito;
import com.masterserv.productos.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ItemCarritoRepository extends JpaRepository<ItemCarrito, Long> {

    /**
     * Busca un item de carrito específico basado en el carrito y el producto.
     * Fundamental para la lógica de "agregar al carrito":
     * - Si existe, sumamos la cantidad.
     * - Si no existe, creamos un nuevo ItemCarrito.
     */
    Optional<ItemCarrito> findByCarritoAndProducto(Carrito carrito, Producto producto);
}