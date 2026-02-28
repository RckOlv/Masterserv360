package com.masterserv.productos.service;

import com.masterserv.productos.dto.MovimientoStockDTO;
import com.masterserv.productos.dto.ProductoDTO;
import com.masterserv.productos.dto.ProductoFiltroDTO;
import com.masterserv.productos.dto.ProductoPublicoDTO;
import com.masterserv.productos.dto.ProductoPublicoFiltroDTO;
import com.masterserv.productos.entity.*;
import com.masterserv.productos.enums.EstadoListaEspera;
import com.masterserv.productos.enums.TipoMovimiento;
import com.masterserv.productos.repository.*;
import com.masterserv.productos.mapper.ProductoMapper;
import com.masterserv.productos.specification.ProductoSpecification;
import com.masterserv.productos.exceptions.StockInsuficienteException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy; 
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate; 
import java.time.LocalDateTime;

@Service
public class ProductoService {

    @Autowired private ProductoRepository productoRepository;
    @Autowired private ProductoMapper productoMapper;
    @Autowired private ProductoSpecification productoSpecification;
    @Autowired private CategoriaRepository categoriaRepository; 
    @Autowired private MovimientoStockRepository movimientoStockRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private SolicitudProductoRepository solicitudProductoRepository;
    @Autowired private ListaEsperaRepository listaEsperaRepository;
    @Autowired private AuditoriaRepository auditoriaRepository;

    @Autowired 
    @Lazy
    private ProcesoAutomaticoService procesoAutomaticoService;

    @Transactional(readOnly = true)
    public String generarCodigoAutomatico(Long categoriaId, String nombreProducto) {
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada"));
        
        String catNombre = categoria.getNombre().trim().toUpperCase().replaceAll("[^A-Z]", "");
        String prodNombre = nombreProducto.trim().toUpperCase().replaceAll("[^A-Z]", "");
        
        if (catNombre.length() < 1) catNombre = "CAT";
        if (prodNombre.length() < 1) prodNombre = "PROD";

        String catPrefix = catNombre.substring(0, Math.min(2, catNombre.length()));
        String prodPrefix = prodNombre.substring(0, Math.min(2, prodNombre.length()));
        
        String prefixCompleto = catPrefix + prodPrefix;

        Optional<Producto> ultimo = productoRepository.findTopByCodigoStartingWithOrderByCodigoDesc(prefixCompleto);

        int siguienteNumero = 1;

        if (ultimo.isPresent()) {
            String ultimoCodigo = ultimo.get().getCodigo();
            try {
                String numeroStr = ultimoCodigo.substring(prefixCompleto.length());
                siguienteNumero = Integer.parseInt(numeroStr) + 1;
            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                siguienteNumero = 1;
            }
        }

        return prefixCompleto + String.format("%02d", siguienteNumero);
    }

    @Transactional(readOnly = true)
    public Page<ProductoDTO> findAll(Pageable pageable) {
        Page<Producto> productosPage = productoRepository.findAll(pageable);
        return productosPage.map(productoMapper::toProductoDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProductoDTO> filter(ProductoFiltroDTO filtro, Pageable pageable) {
        if (filtro.getNombre() != null && !filtro.getNombre().isBlank()) {
             Page<Producto> productosPage = productoRepository.buscarFlexible(filtro.getNombre(), pageable);
             return productosPage.map(productoMapper::toProductoDTO);
        }
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
        if (productoRepository.existsByNombreIgnoreCase(productoDTO.nombre())) {
            throw new IllegalArgumentException("Ya existe un producto con el nombre '" + productoDTO.nombre() + "'.");
        }
        if (productoRepository.existsByCodigo(productoDTO.codigo())) {
            throw new IllegalArgumentException("Ya existe un producto con el código: " + productoDTO.codigo());
        }
        
        Categoria categoria = categoriaRepository.findById(productoDTO.categoriaId())
                .orElseThrow(() -> new EntityNotFoundException("La Categoría con ID " + productoDTO.categoriaId() + " no existe."));
        
        Producto producto = productoMapper.toProducto(productoDTO);
        producto.setCategoria(categoria);
        producto.setEstado("ACTIVO");
        producto.setPrecioCosto(BigDecimal.ZERO);
        producto.setStockActual(0);
        Producto productoGuardado = productoRepository.save(producto);
            if (productoDTO.solicitudId() != null) {
                procesarSolicitudPorId(productoDTO.solicitudId(), productoGuardado);
            } else {
                vincularSolicitudesPorNombre(productoGuardado);
            }
        return productoMapper.toProductoDTO(productoGuardado);
    }
    
    private void procesarSolicitudPorId(Long solicitudId, Producto producto) {
        solicitudProductoRepository.findById(solicitudId).ifPresent(solicitud -> {
            agregarAListaEspera(solicitud.getUsuario(), producto);
            solicitud.setProcesado(true);
            solicitudProductoRepository.save(solicitud);
        });
    }

    private void vincularSolicitudesPorNombre(Producto producto) {
        List<SolicitudProducto> solicitudes = solicitudProductoRepository
                .findByDescripcionContainingIgnoreCaseAndProcesadoFalse(producto.getNombre());

        if (solicitudes.isEmpty()) return;

        for (SolicitudProducto solicitud : solicitudes) {
            agregarAListaEspera(solicitud.getUsuario(), producto);
            solicitud.setProcesado(true);
        }
        solicitudProductoRepository.saveAll(solicitudes);
    }

    private void agregarAListaEspera(Usuario usuario, Producto producto) {
        boolean yaEsta = listaEsperaRepository.existsByUsuarioIdAndProductoIdAndEstado(
                usuario.getId(), producto.getId(), EstadoListaEspera.PENDIENTE);
        
        if (!yaEsta) {
            ListaEspera espera = new ListaEspera();
            espera.setUsuario(usuario);
            espera.setProducto(producto);
            espera.setFechaInscripcion(LocalDate.now());
            espera.setEstado(EstadoListaEspera.PENDIENTE);
            listaEsperaRepository.save(espera);
        }
    }

    @Transactional
    public ProductoDTO update(Long id, ProductoDTO productoDTO) {
        if (productoRepository.existsByNombreIgnoreCaseAndIdNot(productoDTO.nombre(), id)) {
            throw new IllegalArgumentException("El nombre '" + productoDTO.nombre() + "' ya está en uso.");
        }

        Producto productoExistente = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + id));

        productoMapper.updateProductoFromDto(productoDTO, productoExistente);

        if (productoDTO.categoriaId() != null && 
            !productoDTO.categoriaId().equals(productoExistente.getCategoria().getId())) {
            
            Categoria categoria = categoriaRepository.findById(productoDTO.categoriaId())
                    .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada: " + productoDTO.categoriaId()));
            productoExistente.setCategoria(categoria);
        }
        
        if (productoDTO.estado() != null) {
            productoExistente.setEstado(productoDTO.estado());
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
        ProductoFiltroDTO filtroVacio = new ProductoFiltroDTO();
        filtroVacio.setEstado("ACTIVO");
        Specification<Producto> spec = productoSpecification.getProductosByFilters(filtroVacio);
        Page<Producto> productosPage = productoRepository.findAll(spec, pageable);
        return productosPage.map(productoMapper::toProductoPublicoDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProductoPublicoDTO> findPublicoByCriteria(ProductoPublicoFiltroDTO filtroPublico, Pageable pageable) {
        if (filtroPublico.getNombre() != null && !filtroPublico.getNombre().isBlank()) {
             Page<Producto> productosPage = productoRepository.buscarFlexible(filtroPublico.getNombre(), pageable);
             return productosPage.map(productoMapper::toProductoPublicoDTO);
        }
        ProductoFiltroDTO filtroInterno = new ProductoFiltroDTO();
        filtroInterno.setNombre(filtroPublico.getNombre());
        filtroInterno.setCategoriaIds(filtroPublico.getCategoriaIds());
        filtroInterno.setPrecioMin(filtroPublico.getPrecioMin());
        filtroInterno.setPrecioMax(filtroPublico.getPrecioMax());
        filtroInterno.setSoloConStock(filtroPublico.getSoloConStock());
        filtroInterno.setEstado("ACTIVO"); 
        Specification<Producto> spec = productoSpecification.getProductosByFilters(filtroInterno);
        Page<Producto> productosPage = productoRepository.findAll(spec, pageable);
        return productosPage.map(productoMapper::toProductoPublicoDTO);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Producto descontarStock(Long productoId, int cantidadADescontar) {
        if (cantidadADescontar <= 0) throw new IllegalArgumentException("Cantidad debe ser positiva.");
        
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: ID " + productoId));

        int stockAnterior = producto.getStockActual(); 
        if (stockAnterior < cantidadADescontar) {
            throw new StockInsuficienteException("Stock insuficiente.");
        }

        producto.setStockActual(stockAnterior - cantidadADescontar);
        return productoRepository.save(producto);
    }
    
    @Transactional(propagation = Propagation.REQUIRED)
    public Producto reponerStock(Long productoId, int cantidadAReponer) {
        return reponerStock(productoId, cantidadAReponer, null);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Producto reponerStock(Long productoId, int cantidadAReponer, BigDecimal nuevoCosto) {
         if (cantidadAReponer <= 0) throw new IllegalArgumentException("Cantidad debe ser positiva.");

         Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: ID " + productoId));

        int stockAnterior = producto.getStockActual();
        int stockNuevo = stockAnterior + cantidadAReponer;

        producto.setStockActual(stockNuevo);
        if (nuevoCosto != null && nuevoCosto.compareTo(BigDecimal.ZERO) > 0) {
            producto.setPrecioCosto(nuevoCosto);
        }
        Producto productoGuardado = productoRepository.save(producto);

        if (stockAnterior <= 0 && stockNuevo > 0) {
             try { procesoAutomaticoService.procesarListaEspera(productoId); } catch (Exception e) {}
        }

         return productoGuardado;
    }

    @Transactional
    public void ajustarStock(MovimientoStockDTO dto, String emailUsuario) {
        Producto producto = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));

        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + emailUsuario));

        int stockAnterior = producto.getStockActual();
        int cantidadAjuste = dto.getCantidad(); 
        int nuevoStock = stockAnterior + cantidadAjuste;

        if (nuevoStock < 0) {
            throw new StockInsuficienteException("El ajuste dejaría el stock en negativo.");
        }

        producto.setStockActual(nuevoStock);
        productoRepository.save(producto);

        // Registro detallado en MovimientoStock
        MovimientoStock movimiento = new MovimientoStock();
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setCantidad(cantidadAjuste);
        movimiento.setTipoMovimiento(TipoMovimiento.AJUSTE_MANUAL);
        movimiento.setMotivo(dto.getMotivo());
        movimiento.setProducto(producto);
        movimiento.setUsuario(usuario);
        movimientoStockRepository.save(movimiento);

        // Registro explícito en Auditoría para seguimiento de inventario
        registrarAuditoriaManual(producto, usuario, stockAnterior, nuevoStock, cantidadAjuste, dto.getMotivo());

        if (stockAnterior <= 0 && nuevoStock > 0) {
            try {
                procesoAutomaticoService.procesarListaEspera(producto.getId());
            } catch (Exception e) {
                System.err.println("Error en notificación de lista de espera: " + e.getMessage());
            }
        }
    }

    /**
     * Persiste un registro de auditoría específico para ajustes manuales de stock.
     */
    private void registrarAuditoriaManual(Producto producto, Usuario usuario, int anterior, int nuevo, int variacion, String motivo) {
        try {
            Auditoria audit = new Auditoria();
            audit.setFecha(LocalDateTime.now());
            audit.setUsuario(usuario.getEmail());
            audit.setEntidad("Producto");
            audit.setEntidadId(producto.getId().toString());
            audit.setAccion("AJUSTE_MANUAL");

            String detalle = String.format("⚙️ Ajuste Manual. Prod: %s | Variación: %d | Motivo: %s", 
                    producto.getNombre(), variacion, motivo);
            
            if (detalle.length() > 255) detalle = detalle.substring(0, 255);
            
            audit.setDetalle(detalle);
            audit.setValorAnterior("{ \"Stock\": " + anterior + " }");
            audit.setValorNuevo("{ \"Stock\": " + nuevo + ", \"Variacion\": " + variacion + " }");

            auditoriaRepository.save(audit);
        } catch (Exception e) {
            System.err.println("Error al registrar auditoría de ajuste: " + e.getMessage());
        }
    }
}