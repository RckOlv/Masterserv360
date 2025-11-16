package com.masterserv.productos.service;

import com.masterserv.productos.dto.DetalleVentaDTO;
import com.masterserv.productos.dto.MovimientoStockDTO;
import com.masterserv.productos.dto.VentaDTO;
import com.masterserv.productos.dto.VentaFiltroDTO;
import com.masterserv.productos.dto.VentaResumenDTO;
import com.masterserv.productos.entity.*;
import com.masterserv.productos.enums.EstadoCupon;
import com.masterserv.productos.enums.EstadoVenta;
import com.masterserv.productos.enums.TipoMovimiento;
import com.masterserv.productos.exceptions.CuponNoValidoException;
import com.masterserv.productos.exceptions.StockInsuficienteException;
import com.masterserv.productos.mapper.VentaMapper;
import com.masterserv.productos.repository.ProductoRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import com.masterserv.productos.repository.VentaRepository;
import com.masterserv.productos.specification.VentaSpecification;

import com.masterserv.productos.event.VentaRealizadaEvent;
import org.springframework.context.ApplicationEventPublisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;

@Service
public class VentaService {

    private static final Logger logger = LoggerFactory.getLogger(VentaService.class);

    @Autowired private VentaRepository ventaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private VentaMapper ventaMapper;
    @Autowired private VentaSpecification ventaSpecification;
    @Autowired private ProductoService productoService;
    @Autowired private MovimientoStockService movimientoStockService;
    @Autowired private PuntosService puntosService;
    @Autowired private CuponService cuponService;
    @Autowired private CarritoService carritoService;
    @Autowired private ApplicationEventPublisher eventPublisher;

    
    @Transactional
    public VentaDTO create(VentaDTO ventaDTO, String vendedorEmail) {
        // ... (Tu método create() queda exactamente igual) ...
        Usuario vendedor = usuarioRepository.findByEmail(vendedorEmail)
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado: " + vendedorEmail));

        Usuario cliente = usuarioRepository.findById(ventaDTO.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + ventaDTO.getClienteId()));

        Venta venta = new Venta();
        venta.setFechaVenta(LocalDateTime.now());
        venta.setEstado(EstadoVenta.COMPLETADA);
        venta.setVendedor(vendedor);
        venta.setCliente(cliente);
        venta.setDetalles(new HashSet<>());

        // 1) Cupón
        BigDecimal descuento = BigDecimal.ZERO;
        if (ventaDTO.getCodigoCupon() != null && !ventaDTO.getCodigoCupon().isBlank()) {
            try {
                Cupon cupon = cuponService.validarYAplicarCupon(ventaDTO.getCodigoCupon(), venta, cliente);
                if (cupon != null && cupon.getDescuento() != null) {
                    descuento = cupon.getDescuento();
                    venta.setCupon(cupon);
                }
            } catch (CuponNoValidoException ex) {
                logger.warn("Cupón no válido: {}", ex.getMessage());
                throw ex;
            }
        }

        // 2) Detalles y stock
        BigDecimal subtotal = BigDecimal.ZERO;
        for (DetalleVentaDTO d : ventaDTO.getDetalles()) {
            Producto p = productoService.descontarStock(d.getProductoId(), d.getCantidad());

            DetalleVenta det = new DetalleVenta();
            det.setProducto(p);
            det.setCantidad(d.getCantidad());
            det.setPrecioUnitario(p.getPrecioVenta());
            det.setVenta(venta);
            venta.getDetalles().add(det);

            subtotal = subtotal.add(det.getPrecioUnitario().multiply(BigDecimal.valueOf(det.getCantidad())));

            registrarMovimientoStockSalida(venta, det, vendedor);
        }

        venta.setTotalVenta(subtotal.subtract(descuento).max(BigDecimal.ZERO));

        // 3) Guardar venta
        Venta ventaGuardada = ventaRepository.save(venta);

        // 4) Asignar puntos
        puntosService.asignarPuntosPorVenta(ventaGuardada);

        // 5) Vaciar carrito
        carritoService.vaciarCarrito(vendedorEmail);

        // 6) PUBLICAR EVENTO
        eventPublisher.publishEvent(new VentaRealizadaEvent(this, ventaGuardada.getId()));
        
        logger.info("Venta #{} creada y evento publicado. Respondiendo al cliente.", ventaGuardada.getId());
        
        return ventaMapper.toVentaDTO(ventaGuardada);
    }

    private void registrarMovimientoStockSalida(Venta venta, DetalleVenta det, Usuario vendedor) {
        // ... (Tu método queda igual) ...
        MovimientoStockDTO mov = new MovimientoStockDTO();
        mov.setProductoId(det.getProducto().getId());
        mov.setUsuarioId(vendedor.getId());
        mov.setTipoMovimiento(TipoMovimiento.SALIDA_VENTA);
        mov.setCantidad(det.getCantidad());
        mov.setMotivo("Salida por Venta" + (venta.getId() != null ? " #" + venta.getId() : ""));
        movimientoStockService.registrarMovimiento(mov);
    }

    // --------------------- CANCELAR VENTA ---------------------
    @Transactional
    public void cancelarVenta(Long id, String emailCancela) {
        // ... (Tu método queda igual) ...
        Venta venta = ventaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada: " + id));

        if (venta.getEstado() != EstadoVenta.COMPLETADA)
            throw new RuntimeException("Solo se pueden cancelar ventas COMPLETADAS.");

        Usuario user = usuarioRepository.findByEmail(emailCancela)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + emailCancela));

        for (DetalleVenta det : venta.getDetalles()) {
            productoService.reponerStock(det.getProducto().getId(), det.getCantidad());
            registrarMovimientoStockReposicion(venta, det, user);
        }

        venta.setEstado(EstadoVenta.CANCELADA);
        puntosService.revertirPuntosPorVenta(venta);

        if (venta.getCupon() != null) {
            Cupon c = venta.getCupon();
            c.setEstado(c.getFechaVencimiento().isAfter(LocalDate.now()) ? EstadoCupon.VIGENTE : EstadoCupon.VENCIDO);
            c.setVenta(null);
            venta.setCupon(null);
        }

        ventaRepository.save(venta);
    }

    private void registrarMovimientoStockReposicion(Venta venta, DetalleVenta det, Usuario user) {
        // ... (Tu método queda igual) ...
        MovimientoStockDTO mov = new MovimientoStockDTO();
        mov.setProductoId(det.getProducto().getId());
        mov.setUsuarioId(user.getId());
        mov.setTipoMovimiento(TipoMovimiento.DEVOLUCION);
        mov.setCantidad(det.getCantidad());
        mov.setMotivo("Reposición por cancelación Venta #" + venta.getId());
        movimientoStockService.registrarMovimiento(mov);
    }

    // ---------------------- CONSULTAS ----------------------

    @Transactional(readOnly = true)
    public VentaDTO findById(Long id) {
        return ventaRepository.findByIdWithDetails(id)
                .map(ventaMapper::toVentaDTO)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public Page<VentaDTO> findAll(Pageable pageable) {
        return ventaRepository.findAll(pageable).map(ventaMapper::toVentaDTO);
    }
    
    /**
     * Mentor: NUEVO MÉTODO PARA VENDEDORES
     * Este método es llamado por el controller cuando el usuario NO es Admin.
     * Ignora el 'vendedorId' del filtro y lo reemplaza con el ID del vendedor logueado.
     */
    @Transactional(readOnly = true)
    public Page<VentaDTO> findByCriteriaForVendedor(VentaFiltroDTO filtro, Pageable pageable, String vendedorEmail) {
        // Buscamos al vendedor por su email (del Principal)
        Usuario vendedor = usuarioRepository.findByEmail(vendedorEmail)
                .orElseThrow(() -> new RuntimeException("Vendedor logueado no encontrado: " + vendedorEmail));

        // Forzamos el ID del vendedor en el filtro, ignorando lo que vino del frontend
        filtro.setVendedorId(vendedor.getId());
        
        // Construimos y ejecutamos la consulta
        Specification<Venta> spec = ventaSpecification.build(filtro);
        return ventaRepository.findAll(spec, pageable).map(ventaMapper::toVentaDTO);
    }

    /**
     * Este es tu método original, ahora es usado solo por el ADMIN
     */
    @Transactional(readOnly = true)
    public Page<VentaDTO> findByCriteria(VentaFiltroDTO filtro, Pageable pageable) {
        Specification<Venta> spec = ventaSpecification.build(filtro);
        return ventaRepository.findAll(spec, pageable).map(ventaMapper::toVentaDTO);
    }

    @Transactional(readOnly = true)
    public Page<VentaResumenDTO> findVentasByClienteEmail(String email, Pageable pageable) {
        // ... (Tu método queda igual) ...
        Usuario cliente = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + email));

        Pageable ordenado = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by("fechaVenta").descending());

        return ventaRepository.findByCliente(cliente, ordenado).map(ventaMapper::toVentaResumenDTO);
    }

    @Transactional(readOnly = true)
    public Venta findVentaByIdWithDetails(Long id) {
        return ventaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada: " + id));
    }
}