package com.masterserv.productos.service;

import com.masterserv.productos.dto.ProductoDTO;
import com.masterserv.productos.dto.ProductoFiltroDTO;
import com.masterserv.productos.entity.Categoria;
import com.masterserv.productos.entity.Producto;
import com.masterserv.productos.mapper.ProductoMapper;
import com.masterserv.productos.repository.CategoriaRepository;
import com.masterserv.productos.repository.ProductoRepository;
import com.masterserv.productos.specification.ProductoSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoMapper productoMapper;

    @Autowired
    private ProductoSpecification productoSpecification;

    @Transactional(readOnly = true)
    public Page<ProductoDTO> findAll(Pageable pageable) {
        // Obtenemos la página de Entidades
        Page<Producto> productoPage = productoRepository.findAll(pageable);
        // Convertimos la página de Entidades a una página de DTOs
        return productoPage.map(productoMapper::toProductoDTO);
    }

    @Transactional(readOnly = true)
    public ProductoDTO findById(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));
        return productoMapper.toProductoDTO(producto);
    }

    @Transactional
    public ProductoDTO create(ProductoDTO productoDTO) {
        // Validación de negocio
        if (productoRepository.existsByCodigo(productoDTO.codigo())) {
            throw new RuntimeException("Error: El código de producto ya existe.");
        }

        // Convertimos DTO a Entidad
        Producto producto = productoMapper.toProducto(productoDTO);

        // Buscamos y asignamos la entidad Categoria completa
        Categoria categoria = categoriaRepository.findById(productoDTO.categoriaId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + productoDTO.categoriaId()));
        producto.setCategoria(categoria);
        
        // Ponemos el stock en 0 por defecto (como hablamos)
        producto.setStockActual(0);

        // Guardamos la entidad
        Producto productoGuardado = productoRepository.save(producto);

        // Retornamos el DTO
        return productoMapper.toProductoDTO(productoGuardado);
    }

    @Transactional
    public ProductoDTO update(Long id, ProductoDTO productoDTO) {
        // 1. Verificar que el producto exista
        Producto productoExistente = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));

        // 2. Validar que el nuevo código (si cambió) no esté tomado por OTRO producto
        if (!productoExistente.getCodigo().equals(productoDTO.codigo()) && 
            productoRepository.existsByCodigo(productoDTO.codigo())) {
            throw new RuntimeException("Error: El nuevo código de producto ya está en uso por otro producto.");
        }

        // 3. Actualizar los campos (mapeo DTO -> Entidad existente)
        productoExistente.setCodigo(productoDTO.codigo());
        productoExistente.setNombre(productoDTO.nombre());
        productoExistente.setDescripcion(productoDTO.descripcion());
        productoExistente.setPrecioVenta(productoDTO.precioVenta());
        productoExistente.setPrecioCosto(productoDTO.precioCosto());
        productoExistente.setImagenUrl(productoDTO.imagenUrl());
        productoExistente.setStockMinimo(productoDTO.stockMinimo());
        productoExistente.setEstado(productoDTO.estado());
        // El stockActual no se actualiza por esta vía, se usa MovimientoStockService

        // 4. Verificar si la categoría cambió
        if (!productoExistente.getCategoria().getId().equals(productoDTO.categoriaId())) {
            Categoria nuevaCategoria = categoriaRepository.findById(productoDTO.categoriaId())
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + productoDTO.categoriaId()));
            productoExistente.setCategoria(nuevaCategoria);
        }

        // 5. Guardar
        Producto productoActualizado = productoRepository.save(productoExistente);
        return productoMapper.toProductoDTO(productoActualizado);
    }

    @Transactional
    public void softDelete(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));

        // Validación adicional (opcional pero recomendada):
        // ¿Está este producto en alguna venta activa? ¿O en stock?
        // Si es así, quizás no deberías permitir borrarlo lógicamente.
        producto.setEstado("INACTIVO"); // O "DESCONTINUADO"
        productoRepository.save(producto);
    }

    // Opcional: Método para reactivar
    @Transactional
    public void activate(Long id) {
       Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));
       producto.setEstado("ACTIVO");
       productoRepository.save(producto);
    }

    @Transactional(readOnly = true)
    public Page<ProductoDTO> filter(ProductoFiltroDTO filtro, Pageable pageable) {
        // Usamos la Specification para crear la consulta dinámica
        Specification<Producto> spec = productoSpecification.getProductosByFilters(filtro);
        
        // Ejecutamos la consulta con paginación
        Page<Producto> productoPage = productoRepository.findAll(spec, pageable);
        
        // Mapeamos y devolvemos
        return productoPage.map(productoMapper::toProductoDTO);
    }
}