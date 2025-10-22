package com.masterserv.productos.service;

import com.masterserv.productos.dto.ProductoFiltroDTO;
import com.masterserv.productos.entity.Producto;
import com.masterserv.productos.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final Random random = new Random();

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    public List<Producto> listarActivos() {
        return productoRepository.findByActivoTrue();
    }

    public Producto creaProducto(Producto producto) {
        if (productoRepository.existsByCodigo(producto.getCodigo())) {
            throw new RuntimeException("Código de producto ya existe");
        }
        return guardarProducto(producto);
    }

    public Producto actualizarProducto(Producto producto) {
        if (!productoRepository.existsById(producto.getIdProducto())) {
            throw new RuntimeException("Producto no encontrado");
        }
        return guardarProducto(producto);
    }

    public void cambiarActivo(Long idProducto, boolean activo) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        producto.setActivo(activo);
        productoRepository.save(producto);
    }

    public Producto guardarProducto(Producto producto) {
        if (producto.getCodigo() == null || producto.getCodigo().isBlank()) {
            producto.setCodigo(generarCodigoProducto(producto));
        }

        if (producto.getPrecioCosto() == null)
            producto.setPrecioCosto(0.0);
        if (producto.getPrecioVenta() == null)
            producto.setPrecioVenta(0.0);
        if (producto.getStockActual() == null)
            producto.setStockActual(0);
        if (producto.getStockMinimo() == null)
            producto.setStockMinimo(0);
        if (producto.getActivo() == null)
            producto.setActivo(true);

        return productoRepository.save(producto);
    }

    private String generarCodigoProducto(Producto producto) {
        String catPart = (producto.getCategoria() != null && producto.getCategoria().getNombreCategoria() != null)
                ? String.valueOf(producto.getCategoria().getNombreCategoria().charAt(0)).toUpperCase()
                : "C";

        String prodPart = (producto.getNombreProducto() != null)
                ? producto.getNombreProducto().substring(0, Math.min(2, producto.getNombreProducto().length()))
                        .toUpperCase()
                : "PR";

        int num = 1 + random.nextInt(99);
        return catPart + prodPart + String.format("%02d", num);
    }

    //Filtrado por DTO
    public List<Producto> filtrarProductos(ProductoFiltroDTO filtro) {
    // Evitar pasar null al LOWER()
    String nombre = filtro.getNombre();
    if (nombre != null && nombre.isBlank()) {
        nombre = null;
    }

    return productoRepository.filtrarProductos(
        nombre,
        filtro.getCategoriaId(),
        filtro.getActivo(),
        filtro.getFechaDesde(),
        filtro.getFechaHasta()
    );
}

    public List<Producto> filtrarPorFechas(LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null)
            return productoRepository.findAll();
        if (desde != null && hasta != null)
            return productoRepository.findByFechaAltaBetween(desde, hasta);
        return productoRepository.findByFechaAltaAfter(desde != null ? desde : hasta);
    }

    public List<Producto> obtenerUltimosProductos() {
        return productoRepository.findTop5ByOrderByFechaAltaDesc();
    }
}
