package com.masterserv.productos.service;

import com.masterserv.productos.event.StockActualizadoEvent;
import com.masterserv.productos.exceptions.StockInsuficienteException;

import org.springframework.context.ApplicationEventPublisher;

import com.masterserv.productos.dto.MovimientoStockDTO;
import com.masterserv.productos.dto.ProductoDTO;
import com.masterserv.productos.dto.ProductoFiltroDTO;
import com.masterserv.productos.dto.ProductoPublicoDTO;
import com.masterserv.productos.dto.ProductoPublicoFiltroDTO;
import com.masterserv.productos.entity.Producto;
import com.masterserv.productos.entity.Categoria;
// --- MENTOR: Imports nuevos necesarios para el flujo Chatbot ---
import com.masterserv.productos.entity.SolicitudProducto;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.entity.ListaEspera;
import com.masterserv.productos.entity.MovimientoStock;
import com.masterserv.productos.enums.EstadoListaEspera;
import com.masterserv.productos.enums.TipoMovimiento;
import com.masterserv.productos.repository.SolicitudProductoRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import com.masterserv.productos.repository.ListaEsperaRepository;
import com.masterserv.productos.repository.MovimientoStockRepository;
// ---------------------------------------------------------------
import com.masterserv.productos.mapper.ProductoMapper;
import com.masterserv.productos.repository.ProductoRepository;
import com.masterserv.productos.specification.ProductoSpecification;
import com.masterserv.productos.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate; // Importante para la fecha de inscripción
import java.time.LocalDateTime;

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
   
    @Autowired 
    private MovimientoStockRepository movimientoStockRepository;
    
    @Autowired 
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SolicitudProductoRepository solicitudProductoRepository;
    
    @Autowired
    private ListaEsperaRepository listaEsperaRepository;


    // --- MENTOR: GENERADOR DE CÓDIGO INTELIGENTE ---
    @Transactional(readOnly = true)
    public String generarCodigoAutomatico(Long categoriaId, String nombreProducto) {
        // 1. Obtener Categoría
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada"));
        
        // 2. Normalizar textos (Mayúsculas, sin espacios extra)
        String catNombre = categoria.getNombre().trim().toUpperCase().replaceAll("[^A-Z]", "");
        String prodNombre = nombreProducto.trim().toUpperCase().replaceAll("[^A-Z]", "");
        
        // Validación de seguridad para nombres muy cortos
        if (catNombre.length() < 1) catNombre = "CAT";
        if (prodNombre.length() < 1) prodNombre = "PROD";

        // 3. Construir Prefijo (2 Letras Cat + 2 Letras Prod)
        String catPrefix = catNombre.substring(0, Math.min(2, catNombre.length()));
        String prodPrefix = prodNombre.substring(0, Math.min(2, prodNombre.length()));
        
        String prefixCompleto = catPrefix + prodPrefix;

        // 4. Buscar el último en la BD
        Optional<Producto> ultimo = productoRepository.findTopByCodigoStartingWithOrderByCodigoDesc(prefixCompleto);

        int siguienteNumero = 1;

        if (ultimo.isPresent()) {
            String ultimoCodigo = ultimo.get().getCodigo();
            try {
                // Extraemos la parte numérica
                String numeroStr = ultimoCodigo.substring(prefixCompleto.length());
                siguienteNumero = Integer.parseInt(numeroStr) + 1;
            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                siguienteNumero = 1; // Si el formato no coincide, reiniciamos
            }
        }

        // 5. Formatear con ceros a la izquierda (Ej: 01, 05, 10)
        return prefixCompleto + String.format("%02d", siguienteNumero);
    }
    // ----------------------------------------------

    @Transactional(readOnly = true)
    public Page<ProductoDTO> findAll(Pageable pageable) {
        Page<Producto> productosPage = productoRepository.findAll(pageable);
        return productosPage.map(productoMapper::toProductoDTO);
    }

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
        
        Producto productoGuardado = productoRepository.save(producto);
        
        // --- MENTOR: LÓGICA DE VINCULACIÓN AUTOMÁTICA (Chatbot -> Producto) ---
        // Si el DTO trae un ID de solicitud, cerramos el circuito
        if (productoDTO.getSolicitudId() != null) {
            procesarSolicitudPendiente(productoDTO.getSolicitudId(), productoGuardado);
        }
        // ----------------------------------------------------------------------

        return productoMapper.toProductoDTO(productoGuardado);
    }
    
    // --- Helper privado para crear la lista de espera ---
    private void procesarSolicitudPendiente(Long solicitudId, Producto producto) {
        solicitudProductoRepository.findById(solicitudId).ifPresent(solicitud -> {
            // 1. Crear registro en ListaEspera (Evitamos duplicados por seguridad)
            boolean yaEstaEnEspera = listaEsperaRepository.existsByUsuarioIdAndProductoIdAndEstado(
                    solicitud.getUsuario().getId(), producto.getId(), EstadoListaEspera.PENDIENTE);
            
            if (!yaEstaEnEspera) {
                ListaEspera espera = new ListaEspera();
                espera.setUsuario(solicitud.getUsuario());
                espera.setProducto(producto);
                espera.setFechaInscripcion(LocalDate.now());
                espera.setEstado(EstadoListaEspera.PENDIENTE);
                
                listaEsperaRepository.save(espera);
            }
            
            // 2. Marcar solicitud como procesada (cerrar el ciclo)
            solicitud.setProcesado(true);
            solicitudProductoRepository.save(solicitud);
        });
    }

    @Transactional
    public ProductoDTO update(Long id, ProductoDTO productoDTO) {
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

    // --- MENTOR: MÉTODO DE AJUSTE MANUAL CON HISTORIAL ---
    @Transactional
    public void ajustarStock(MovimientoStockDTO dto, String emailUsuario) {
        Producto producto = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));

        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + emailUsuario));

        // 1. Calcular nuevo stock
        int stockAnterior = producto.getStockActual();
        int cantidadAjuste = dto.getCantidad(); // Puede ser + o -
        int nuevoStock = stockAnterior + cantidadAjuste;

        if (nuevoStock < 0) {
            throw new StockInsuficienteException("El ajuste dejaría el stock en negativo (" + nuevoStock + ").");
        }

        // 2. Actualizar producto
        producto.setStockActual(nuevoStock);
        productoRepository.save(producto);

        // 3. Guardar Movimiento (Historial)
        MovimientoStock movimiento = new MovimientoStock();
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setCantidad(cantidadAjuste);
        movimiento.setTipoMovimiento(TipoMovimiento.AJUSTE_MANUAL); // O dto.getTipoMovimiento()
        movimiento.setMotivo(dto.getMotivo());
        movimiento.setProducto(producto);
        movimiento.setUsuario(usuario);
        
        movimientoStockRepository.save(movimiento);

        // 4. Publicar evento (para alertas de stock bajo, etc.)
        eventPublisher.publishEvent(new StockActualizadoEvent(producto.getId(), stockAnterior, nuevoStock));
    }
    
}
