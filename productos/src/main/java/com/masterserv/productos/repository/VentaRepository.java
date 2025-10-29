package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime; // Importar si añades métodos con fecha

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    /**
     * Sobrescribe el findAll de JpaRepository para optimizarlo.
     * Carga el cliente y el vendedor asociados a cada venta en la
     * misma consulta para evitar N+1 en el listado.
     * NO cargamos los 'detalles' aquí, sería ineficiente para un listado.
     */
    @Override
    @EntityGraph(attributePaths = {"cliente", "vendedor"})
    Page<Venta> findAll(Pageable pageable);

    // --- Métodos de Búsqueda Adicionales (Ejemplos para el futuro) ---

    /**
     * Busca ventas para un cliente específico, paginado.
     * Carga cliente y vendedor eficientemente.
     */
    @EntityGraph(attributePaths = {"cliente", "vendedor"})
    Page<Venta> findByClienteId(Long clienteId, Pageable pageable);

    /**
     * Busca ventas realizadas por un vendedor específico, paginado.
     * Carga cliente y vendedor eficientemente.
     */
    @EntityGraph(attributePaths = {"cliente", "vendedor"})
    Page<Venta> findByVendedorId(Long vendedorId, Pageable pageable);

    /**
     * Busca ventas dentro de un rango de fechas, paginado.
     * Carga cliente y vendedor eficientemente.
     */
    @EntityGraph(attributePaths = {"cliente", "vendedor"})
    Page<Venta> findByFechaVentaBetween(LocalDateTime inicio, LocalDateTime fin, Pageable pageable);

    // Podrías necesitar un método para cargar una Venta CON sus detalles,
    // similar al 'findByIdWithDetails' de PedidoRepository.
    // @Query("SELECT v FROM Venta v LEFT JOIN FETCH v.detalles d LEFT JOIN FETCH d.producto WHERE v.id = :id")
    // @EntityGraph(attributePaths = {"cliente", "vendedor", "detalles", "detalles.producto"})
    // Optional<Venta> findByIdWithDetails(@Param("id") Long id);

}