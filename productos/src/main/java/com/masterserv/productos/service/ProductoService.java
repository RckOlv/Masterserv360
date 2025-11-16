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

// Mentor: Importamos la excepción para errores 404
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

    // ... (Tu lógica de filtros: filter, findById, etc. quedan igual) ...

    @Transactional(readOnly = true)
    public Page<ProductoDTO> filter(ProductoFiltroDTO filtro, Pageable pageable) {
        Specification<Producto> spec = productoSpecification.getProductosByFilters(filtro);
        Page<Producto> productosPage = productoRepository.findAll(spec, pageable);
        return productosPage.map(productoMapper::toProductoDTO);
    }

    @Transactional(readOnly = true)
    public ProductoDTO findById(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + id)); // Mentor: Mejor usar EntityNotFound
        return productoMapper.toProductoDTO(producto);
    }

    // --- Mentor: ¡MÉTODO 'CREATE' CORREGIDO! ---
    @Transactional
    public ProductoDTO create(ProductoDTO productoDTO) {
        // 1. Validar código duplicado
        if (productoRepository.existsByCodigo(productoDTO.codigo())) {
            throw new IllegalArgumentException("Ya existe un producto con el código: " + productoDTO.codigo());
        }

        // 2. VALIDAR Y OBTENER LA CATEGORÍA (¡LA LÓGICA QUE FALTABA!)
        // Buscamos la entidad Categoria. Si no existe, lanzamos un error claro.
        Categoria categoria = categoriaRepository.findById(productoDTO.categoriaId())
            .orElseThrow(() -> new EntityNotFoundException("Error al crear producto: La Categoría con ID " + productoDTO.categoriaId() + " no existe."));

        // 3. Mapear el DTO a la entidad
        Producto producto = productoMapper.toProducto(productoDTO);
        
        // 4. Asignar la categoría y estado (El servicio es responsable, no el mapper)
        producto.setCategoria(categoria); // Asignamos la entidad Categoria que SÍ existe
        producto.setEstado("ACTIVO");
        // (El stockActual se setea a 0 por defecto si no viene en el DTO, lo cual es correcto al crear)
        
        // 5. Guardar
        Producto productoGuardado = productoRepository.save(producto);
        return productoMapper.toProductoDTO(productoGuardado);
    }
    // --- FIN DE LA CORRECCIÓN ---

    @Transactional
    public ProductoDTO update(Long id, ProductoDTO productoDTO) {
        Producto productoExistente = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + id)); // Mentor: Mejor usar EntityNotFound

        productoExistente.setNombre(productoDTO.nombre());
        productoExistente.setCodigo(productoDTO.codigo());
        productoExistente.setDescripcion(productoDTO.descripcion());
        productoExistente.setPrecioCosto(productoDTO.precioCosto());
        productoExistente.setPrecioVenta(productoDTO.precioVenta());
        productoExistente.setStockActual(productoDTO.stockActual());
        productoExistente.setStockMinimo(productoDTO.stockMinimo());
        productoExistente.setImagenUrl(productoDTO.imagenUrl());
        productoExistente.setEstado(productoDTO.estado());

        if (productoDTO.categoriaId() != null) {
            // Validamos que la nueva categoría también exista
            Categoria categoria = categoriaRepository.findById(productoDTO.categoriaId())
                    .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada: " + productoDTO.categoriaId()));
            productoExistente.setCategoria(categoria);
        }

        Producto productoActualizado = productoRepository.save(productoExistente);
        
        return productoMapper.toProductoDTO(productoActualizado);
    }

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


    // --- 3. MÉTODOS DE STOCK (¡MODIFICADOS!) ---

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