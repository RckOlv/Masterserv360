package com.masterserv.productos.repository;

import com.masterserv.productos.entity.ListaEspera;
import com.masterserv.productos.entity.Producto;
import com.masterserv.productos.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListaEsperaRepository extends JpaRepository<ListaEspera, Long> {

    /**
     * Busca a todos los usuarios que están esperando un producto específico.
     * Clave para el servicio de notificación.
     */
    List<ListaEspera> findByProducto(Producto producto);

    /**
     * Busca a todos los usuarios que están esperando un producto y cuyo estado es "ACTIVA".
     */
    List<ListaEspera> findByProductoAndEstado(Producto producto, String estado);

    /**
     * Verifica si un usuario ya está en la lista de espera de un producto.
     * Evita que el usuario se inscriba dos veces.
     */
    Optional<ListaEspera> findByUsuarioAndProducto(Usuario usuario, Producto producto);
}