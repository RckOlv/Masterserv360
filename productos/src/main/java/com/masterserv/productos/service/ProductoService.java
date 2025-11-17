package com.masterserv.productos.service;

import com.masterserv.productos.event.StockActualizadoEvent;
import com.masterserv.productos.exceptions.StockInsuficienteException;

import org.springframework.context.ApplicationEventPublisher;
// ... (otros imports)
import com.masterserv.productos.dto.ProductoDTO;
import com.masterserv.productos.dto.ProductoFiltroDTO;
import com.masterserv.productos.dto.ProductoPublicoDTO;
import com.masterserv.productos.dto.ProductoPublicoFiltroDTO;
// ... (otros imports)
import com.masterserv.productos.entity.Producto;
import com.masterserv.productos.entity.Categoria;
// ... (otros imports)
import com.masterserv.productos.mapper.ProductoMapper;
import com.masterserv.productos.repository.ProductoRepository;
import com.masterserv.productos.specification.ProductoSpecification;
import com.masterserv.productos.repository.CategoriaRepository;
// ... (otros imports)
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.persistence.EntityNotFoundException;

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

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // ... (filter, findById, create - sin cambios, están correctos) ...
    @Transactional(readOnly = true)
    public Page<ProductoDTO> filter(ProductoFiltroDTO filtro, Pageable pageable) {
        Specification<Producto> spec = productoSpecification.getProductosByFilters(filtro);
        Page<Producto> productosPage = productoRepository.findAll(spec, pageable);
        return productosPage.map(productoMapper::toProductoDTO);
    }

    @Transactional(readOnly = true)
    public ProductoDTO findById(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + id));
        return productoMapper.toProductoDTO(producto);
    }

    @Transactional
    public ProductoDTO create(ProductoDTO productoDTO) {
        if (productoRepository.existsByCodigo(productoDTO.codigo())) {
            throw new IllegalArgumentException("Ya existe un producto con el código: " + productoDTO.codigo());
        }
        Categoria categoria = categoriaRepository.findById(productoDTO.categoriaId())
                .orElseThrow(() -> new EntityNotFoundException("Error al crear producto: La Categoría con ID " + productoDTO.categoriaId() + " no existe."));
        
        Producto producto = productoMapper.toProducto(productoDTO);
        producto.setCategoria(categoria);
        producto.setEstado("ACTIVO");
        // El nuevo 'loteReposicion' se mapea automáticamente desde el DTO
        
        Producto productoGuardado = productoRepository.save(producto);
        return productoMapper.toProductoDTO(productoGuardado);
    }

    // --- Mentor: INICIO DE CAMBIOS (Soluciona el bug de Stock=0) ---
    @Transactional
    public ProductoDTO update(Long id, ProductoDTO productoDTO) {
        Producto productoExistente = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + id));

        // 1. Usamos el Mapper para actualizar solo los campos seguros.
        // (Esto ignora stockActual pero actualiza loteReposicion, precioVenta, etc.)
        productoMapper.updateProductoFromDto(productoDTO, productoExistente);

        // 2. Manejamos la lógica de Categoría (que es compleja)
        if (productoDTO.categoriaId() != null && 
            !productoDTO.categoriaId().equals(productoExistente.getCategoria().getId())) {
            
            Categoria categoria = categoriaRepository.findById(productoDTO.categoriaId())
                    .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada: " + productoDTO.categoriaId()));
            productoExistente.setCategoria(categoria);
        }
        
        // 3. Manejamos el Estado (si se incluyó en el DTO)
        if (productoDTO.estado() != null) {
            productoExistente.setEstado(productoDTO.estado());
        }

        Producto productoActualizado = productoRepository.save(productoExistente);
        
        return productoMapper.toProductoDTO(productoActualizado);
    }
    // --- Mentor: FIN DE CAMBIOS ---

    // ... (softDelete, findByProveedorId, etc. - sin cambios) ...
    @Transactional
    public void softDelete(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + id));
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


    // --- Métodos de Stock (sin cambios) ---

    @Transactional(propagation = Propagation.REQUIRED)
    public Producto descontarStock(Long productoId, int cantidadADescontar) {
        if (cantidadADescontar <= 0) {
             throw new IllegalArgumentException("La cantidad a descontar debe ser positiva.");
        }
        
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: ID " + productoId));

        int stockAnterior = producto.getStockActual(); 

        if (stockAnterior < cantidadADescontar) {
            throw new StockInsuficienteException(
                String.format("Stock insuficiente para '%s' (ID:%d). Disponible: %d, Solicitado: %d",
                             producto.getNombre(), producto.getId(),
                             stockAnterior, cantidadADescontar)
            );
        }

        int stockNuevo = stockAnterior - cantidadADescontar;
        producto.setStockActual(stockNuevo);
        Producto productoGuardado = productoRepository.save(producto);

        eventPublisher.publishEvent(new StockActualizadoEvent(
            productoId,
            stockAnterior,
            stockNuevo
        ));
        
        return productoGuardado;
    }
    
    @Transactional(propagation = Propagation.REQUIRED)
    public Producto reponerStock(Long productoId, int cantidadAReponer) {
         if (cantidadAReponer <= 0) {
             throw new IllegalArgumentException("La cantidad a reponer debe ser positiva.");
         }

         Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: ID " + productoId));

        int stockAnterior = producto.getStockActual();
        int stockNuevo = stockAnterior + cantidadAReponer;

        producto.setStockActual(stockNuevo);
        Producto productoGuardado = productoRepository.save(producto);

        eventPublisher.publishEvent(new StockActualizadoEvent(
            productoId,
            stockAnterior,
            stockNuevo
        ));

         return productoGuardado;
    }
}