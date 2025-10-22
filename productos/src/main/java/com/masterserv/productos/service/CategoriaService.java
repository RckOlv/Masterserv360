package com.masterserv.productos.service;

import com.masterserv.productos.entity.Categoria;
import com.masterserv.productos.entity.Producto;
import com.masterserv.productos.repository.CategoriaRepository;
import com.masterserv.productos.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, ProductoRepository productoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
    }

    // 🔹 Listar todas las categorías
    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAll();
    }

    // 🔹 Listar solo las activas
    public List<Categoria> listarActivas() {
        return categoriaRepository.findByActivoTrue();
    }

    // 🔹 Buscar por ID
    public Optional<Categoria> buscarPorId(Long id) {
        return categoriaRepository.findById(id);
    }

    // 🔹 Crear nueva categoría
    public Categoria crearCategoria(Categoria categoria) {
        categoria.setActivo(true);
        return categoriaRepository.save(categoria);
    }

    // 🔹 Actualizar categoría
    public Optional<Categoria> actualizar(Long id, Categoria categoriaActualizada) {
        return categoriaRepository.findById(id)
                .map(categoria -> {
                    categoria.setNombreCategoria(categoriaActualizada.getNombreCategoria());
                    categoria.setDescripcion(categoriaActualizada.getDescripcion());
                    categoria.setActivo(categoriaActualizada.isActivo());
                    return categoriaRepository.save(categoria);
                });
    }

    // 🔹 Eliminar categoría (baja lógica o eliminación total si no tiene productos)
    public void eliminarCategoria(Long idCategoria) {
        categoriaRepository.findById(idCategoria).ifPresent(categoria -> {
            boolean tieneProductos = productoRepository.existsByCategoria_IdCategoria(idCategoria);

            if (tieneProductos) {
                // Baja lógica
                categoria.setActivo(false);
                categoriaRepository.save(categoria);

                List<Producto> productos = productoRepository.findByCategoria_IdCategoria(idCategoria);
                for (Producto producto : productos) {
                    producto.setActivo(false);
                    productoRepository.save(producto);
                }

            } else {
                // Eliminación total
                categoriaRepository.delete(categoria);
            }
        });
    }

    // 🔹 Cambiar estado (activar o desactivar)
    public void cambiarEstado(Long idCategoria, boolean activo) {
        Categoria categoria = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        categoria.setActivo(activo);
        categoriaRepository.save(categoria);

        // Sincroniza el estado de los productos relacionados
        List<Producto> productos = productoRepository.findByCategoria_IdCategoria(idCategoria);
        for (Producto producto : productos) {
            producto.setActivo(activo);
            productoRepository.save(producto);
        }
    }
}
