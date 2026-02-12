package com.masterserv.productos.repository;

import com.masterserv.productos.entity.ListaEspera;
import com.masterserv.productos.entity.Producto;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.enums.EstadoListaEspera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListaEsperaRepository extends JpaRepository<ListaEspera, Long> {

    /**
     * Busca todos los registros de lista de espera para un producto específico
     * filtrando por el estado (ej: solo los PENDIENTE).
     */
    List<ListaEspera> findByProductoAndEstado(Producto producto, EstadoListaEspera estado);

    /**
     * Verifica si un usuario ya está en espera para un producto con cierto estado.
     * Útil para evitar duplicados antes de guardar.
     */
    boolean existsByUsuarioIdAndProductoIdAndEstado(Long usuarioId, Long productoId, EstadoListaEspera estado);

    /**
     * Verifica si el usuario tiene CUALQUIER producto en ese estado.
     * (Este es el que necesitas para el Controlador de Solicitudes)
     */
    boolean existsByUsuarioIdAndEstado(Long usuarioId, EstadoListaEspera estado);

    /**
     * Verifica si un usuario ya está en espera para un producto con cierto estado.
     * Útil para evitar duplicados antes de guardar.
     */
    boolean existsByUsuarioAndProductoAndEstado(Usuario usuario, Producto producto, EstadoListaEspera estado);
}