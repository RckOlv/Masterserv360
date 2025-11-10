package com.masterserv.productos.service;

// --- ¡NUEVOS IMPORTS PARA EVENTOS! ---
import com.masterserv.productos.event.StockActualizadoEvent;
import org.springframework.context.ApplicationEventPublisher;
// --- FIN NUEVOS IMPORTS ---

import com.masterserv.productos.dto.ProductoDTO;
import com.masterserv.productos.dto.ProductoFiltroDTO;
import com.masterserv.productos.dto.ProductoPublicoDTO;
import com.masterserv.productos.dto.ProductoPublicoFiltroDTO;
import com.masterserv.productos.entity.Producto;
import com.masterserv.productos.entity.Categoria;
import com.masterserv.productos.exceptions.StockInsuficienteException;
import com.masterserv.productos.mapper.ProductoMapper;
import com.masterserv.productos.repository.ProductoRepository;
import com.masterserv.productos.repository.CategoriaRepository;
import com.masterserv.productos.specification.ProductoSpecification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ProductoMapper productoMapper;
    
    @Autowired
    private ProductoSpecification productoSpecification;

    @Autowired
    private CategoriaRepository categoriaRepository; 

    // --- ¡1. INYECTAR EL PUBLICADOR DE EVENTOS! ---
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // --- 2. MÉTODOS CRUD (ADMIN/VENDEDOR) ---
    // (Estos métodos: filter, findById, create, update, softDelete, findByProveedorId, 
    // searchByProveedor, findAllPublico, findPublicoByCriteria NO CAMBIAN)
    // ... (Tu lógica de CRUD y filtros va aquí, sin cambios) ...

    @Transactional(readOnly = true)
    public Page<ProductoDTO> filter(ProductoFiltroDTO filtro, Pageable pageable) {
        Specification<Producto> spec = productoSpecification.getProductosByFilters(filtro);
        Page<Producto> productosPage = productoRepository.findAll(spec, pageable);
        return productosPage.map(productoMapper::toProductoDTO);
    }

    @Transactional(readOnly = true)
    public ProductoDTO findById(Long id) {
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));
        return productoMapper.toProductoDTO(producto);
    }

    @Transactional
    public ProductoDTO create(ProductoDTO productoDTO) {
        if (productoRepository.existsByCodigo(productoDTO.codigo())) {
            throw new IllegalArgumentException("Ya existe un producto con el código: " + productoDTO.codigo());
        }
        Producto producto = productoMapper.toProducto(productoDTO);
        producto.setEstado("ACTIVO");
        Producto productoGuardado = productoRepository.save(producto);
        return productoMapper.toProductoDTO(productoGuardado);
    }

    @Transactional
    public ProductoDTO update(Long id, ProductoDTO productoDTO) {
        Producto productoExistente = productoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));

        productoExistente.setNombre(productoDTO.nombre());
        productoExistente.setCodigo(productoDTO.codigo());
        // ... (resto de tus .set...() para actualizar)
        productoExistente.setDescripcion(productoDTO.descripcion());
        productoExistente.setPrecioCosto(productoDTO.precioCosto());
        productoExistente.setPrecioVenta(productoDTO.precioVenta());
        productoExistente.setStockActual(productoDTO.stockActual());
        productoExistente.setStockMinimo(productoDTO.stockMinimo());
        productoExistente.setImagenUrl(productoDTO.imagenUrl());
        productoExistente.setEstado(productoDTO.estado());

        if (productoDTO.categoriaId() != null) {
            Categoria categoria = categoriaRepository.findById(productoDTO.categoriaId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + productoDTO.categoriaId()));
            productoExistente.setCategoria(categoria);
        }

        Producto productoActualizado = productoRepository.save(productoExistente);
        
        // --- ¡NOTA DE MENTOR! ---
        // Si actualizas el stock aquí, también deberías publicar el evento.
        // eventPublisher.publishEvent(new StockActualizadoEvent(
        //     productoActualizado.getId(),
        //     stockViejo, // Tendrías que guardar el stock anterior
        //     productoActualizado.getStockActual()
        // ));
        
        return productoMapper.toProductoDTO(productoActualizado);
    }

    @Transactional
    public void softDelete(Long id) {
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));
        producto.setEstado("INACTIVO");
        productoRepository.save(producto);
    }

    @Transactional(readOnly = true)
    public List<ProductoDTO> findByProveedorId(Long proveedorId) {
        List<Producto> productos = productoRepository.findActivosByProveedorId(proveedorId);
        return productos.stream()
            .map(productoMapper::toProductoDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ProductoDTO> searchByProveedor(Long proveedorId, String search, Pageable pageable) {
        Page<Producto> productosPage = productoRepository.searchByProveedor(proveedorId, search, pageable);
        return productosPage.map(productoMapper::toProductoDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProductoPublicoDTO> findAllPublico(Pageable pageable) {
        ProductoPublicoFiltroDTO filtroVacio = new ProductoPublicoFiltroDTO();
        Specification<Producto> spec = productoSpecification.getPublicProductosByFilters(filtroVacio);
        Page<Producto> productosPage = productoRepository.findAll(spec, pageable);
        return productosPage.map(productoMapper::toProductoPublicoDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProductoPublicoDTO> findPublicoByCriteria(ProductoPublicoFiltroDTO filtro, Pageable pageable) {
        Specification<Producto> spec = productoSpecification.getPublicProductosByFilters(filtro);
        Page<Producto> productosPage = productoRepository.findAll(spec, pageable);
        return productosPage.map(productoMapper::toProductoPublicoDTO);
    }


    // --- 3. MÉTODOS DE STOCK (¡MODIFICADOS!) ---

    @Transactional(propagation = Propagation.REQUIRED)
    public Producto descontarStock(Long productoId, int cantidadADescontar) {
        if (cantidadADescontar <= 0) {
             throw new IllegalArgumentException("La cantidad a descontar debe ser positiva.");
        }
        
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new RuntimeException("Producto no encontrado: ID " + productoId));

        // --- ¡INICIO DE LA MODIFICACIÓN! ---
        int stockAnterior = producto.getStockActual(); // 1. Guardamos el valor anterior
        // --- FIN DE LA MODIFICACIÓN ---

        if (stockAnterior < cantidadADescontar) { // Usamos la variable guardada
            throw new StockInsuficienteException(
                String.format("Stock insuficiente para '%s' (ID:%d). Disponible: %d, Solicitado: %d",
                              producto.getNombre(), producto.getId(),
                              stockAnterior, cantidadADescontar)
            );
        }

        // --- ¡INICIO DE LA MODIFICACIÓN! ---
        int stockNuevo = stockAnterior - cantidadADescontar; // 2. Calculamos el nuevo valor
        producto.setStockActual(stockNuevo);
        Producto productoGuardado = productoRepository.save(producto);

        // 3. ¡PUBLICAMOS EL EVENTO!
        // Le decimos al sistema: "El stock de este producto cambió"
        eventPublisher.publishEvent(new StockActualizadoEvent(
            productoId,
            stockAnterior,
            stockNuevo
        ));
        // --- FIN DE LA MODIFICACIÓN! ---
        
        return productoGuardado;
    }
    
    @Transactional(propagation = Propagation.REQUIRED)
    public Producto reponerStock(Long productoId, int cantidadAReponer) {
         if (cantidadAReponer <= 0) {
             throw new IllegalArgumentException("La cantidad a reponer debe ser positiva.");
         }

         Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new RuntimeException("Producto no encontrado: ID " + productoId));

        // --- ¡INICIO DE LA MODIFICACIÓN! ---
        int stockAnterior = producto.getStockActual(); // 1. Guardamos el valor anterior
        int stockNuevo = stockAnterior + cantidadAReponer; // 2. Calculamos el nuevo valor

        producto.setStockActual(stockNuevo);
        Producto productoGuardado = productoRepository.save(producto);

        // 3. ¡PUBLICAMOS EL EVENTO!
        eventPublisher.publishEvent(new StockActualizadoEvent(
            productoId,
            stockAnterior,
            stockNuevo
        ));
        // --- FIN DE LA MODIFICACIÓN! ---

         return productoGuardado;
    }
}