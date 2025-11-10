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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;

@Service
public class VentaService {

    @Autowired
    private VentaRepository ventaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private VentaMapper ventaMapper;
    @Autowired
    private VentaSpecification ventaSpecification;
    
    // --- ¡CAMBIOS DE ARQUITECTURA! ---
    @Autowired
    private ProductoService productoService; // ¡Inyectamos el servicio de Producto!
    @Autowired
    private MovimientoStockService movimientoStockService;
    // --- Fin Cambios ---

    @Autowired
    private PuntosService puntosService; 
    @Autowired
    private CuponService cuponService;

    @Transactional 
    public VentaDTO create(VentaDTO ventaDTO, String vendedorEmail) {

        Usuario vendedor = usuarioRepository.findByEmail(vendedorEmail)
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado: " + vendedorEmail));

        Usuario cliente = usuarioRepository.findById(ventaDTO.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado: ID " + ventaDTO.getClienteId()));

        Venta venta = new Venta();
        venta.setFechaVenta(LocalDateTime.now());
        venta.setEstado(EstadoVenta.COMPLETADA);
        venta.setVendedor(vendedor);
        venta.setCliente(cliente);
        venta.setDetalles(new HashSet<>());

        // --- 1. LÓGICA DE CUPÓN ---
        BigDecimal descuentoCupon = BigDecimal.ZERO;
        
        if (ventaDTO.getCodigoCupon() != null && !ventaDTO.getCodigoCupon().isBlank()) {
            // ¡Quitamos el try-catch! Si el cupón falla, la venta debe fallar.
            Cupon cuponAplicado = cuponService.validarYAplicarCupon(
                ventaDTO.getCodigoCupon(), 
                venta, 
                cliente
            );
            descuentoCupon = cuponAplicado.getDescuento();
        }

        // --- 2. Procesar Detalles y Stock ---
        BigDecimal subTotalVenta = BigDecimal.ZERO;

        for (DetalleVentaDTO detalleDTO : ventaDTO.getDetalles()) {
            
            // 1. Delegamos el descuento de stock.
            //    ¡Esto valida y descuenta atómicamente!
            //    Si falla, lanza StockInsuficienteException y revierte TODO.
            Producto productoActualizado = productoService.descontarStock(
                detalleDTO.getProductoId(), 
                detalleDTO.getCantidad()
            );

            // 2. Construir el detalle (ya sabemos que el stock es válido)
            DetalleVenta detalle = new DetalleVenta();
            detalle.setProducto(productoActualizado);
            detalle.setCantidad(detalleDTO.getCantidad());
            detalle.setPrecioUnitario(productoActualizado.getPrecioVenta()); // Usamos el precio del producto actualizado
            detalle.setVenta(venta);
            venta.getDetalles().add(detalle);

            // 3. Calcular subtotal
            subTotalVenta = subTotalVenta.add(
                detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad()))
            );

            // 4. Registrar el movimiento (el servicio de log)
            //    Este método ahora también es parte de la transacción principal
            registrarMovimientoStockSalida(venta, detalle, vendedor);
        } // --- Fin del loop for ---
        
        // --- 3. Cálculo de Total Final ---
        BigDecimal totalVenta = subTotalVenta.subtract(descuentoCupon);
        venta.setTotalVenta(totalVenta.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : totalVenta);
        
        // 4. Guardar la Venta (y los detalles, y el cupón)
        Venta ventaGuardada = ventaRepository.save(venta);

        // --- 5. Asignación de Puntos ---
        // ¡QUITAMOS EL TRY-CATCH! 
        // Si la asignación de puntos falla, la transacción entera hará rollback.
        puntosService.asignarPuntosPorVenta(ventaGuardada);
        // ------------------------------------

        // 6. Devolver el DTO
        return ventaMapper.toVentaDTO(ventaGuardada);
    }

    private void registrarMovimientoStockSalida(Venta venta, DetalleVenta detalle, Usuario vendedor) {
        MovimientoStockDTO movDto = new MovimientoStockDTO();
        movDto.setProductoId(detalle.getProducto().getId());
        movDto.setUsuarioId(vendedor.getId());
        movDto.setTipoMovimiento(TipoMovimiento.SALIDA_VENTA);
        movDto.setCantidad(detalle.getCantidad());
        String motivoVenta = "Salida por Venta" + (venta.getId() != null ? " #" + venta.getId() : " (pendiente)");
        movDto.setMotivo(motivoVenta);

        // Sin try-catch, se une a la transacción principal
        movimientoStockService.registrarMovimiento(movDto);
    }

    /**
     * Cancela una venta y repone el stock (optimizado).
     */
    @Transactional // ¡TODO O NADA!
    public void cancelarVenta(Long ventaId, String usuarioEmailCancela) {
        Venta venta = ventaRepository.findByIdWithDetails(ventaId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada: " + ventaId));

        if (venta.getEstado() != EstadoVenta.COMPLETADA) {
                throw new RuntimeException("Solo se pueden cancelar ventas que estén COMPLETADAS.");
        }

        Usuario usuarioCancela = usuarioRepository.findByEmail(usuarioEmailCancela)
                        .orElseThrow(() -> new RuntimeException("Usuario que cancela no encontrado: " + usuarioEmailCancela));

        // 1. Reponer Stock (delegando)
        for (DetalleVenta detalle : venta.getDetalles()) {
            productoService.reponerStock(detalle.getProducto().getId(), detalle.getCantidad());
            // Registramos el movimiento de reposición
            registrarMovimientoStockReposicion(venta, detalle, usuarioCancela);
        }

        venta.setEstado(EstadoVenta.CANCELADA);
        
        // 2. Revertir Puntos (¡Sin try-catch!)
        puntosService.revertirPuntosPorVenta(venta); 

        // 3. Revertir Cupón
        if (venta.getCupon() != null) {
            Cupon cuponUsado = venta.getCupon();
            
            if (cuponUsado.getFechaVencimiento().isAfter(LocalDate.now())) {
                cuponUsado.setEstado(EstadoCupon.VIGENTE);
            } else {
                cuponUsado.setEstado(EstadoCupon.VENCIDO);
            }
            
            cuponUsado.setVenta(null);
            venta.setCupon(null); 
            // El @Transactional se encargará de guardar el estado de cuponUsado
        }
        
        // El @Transactional guarda automáticamente los cambios en 'venta' y 'cuponUsado'
        // al finalizar el método, pero un save explícito no hace daño.
        ventaRepository.save(venta);
    }

    private void registrarMovimientoStockReposicion(Venta venta, DetalleVenta detalle, Usuario usuarioCancela) {
        MovimientoStockDTO movDto = new MovimientoStockDTO();
        movDto.setProductoId(detalle.getProducto().getId());
        movDto.setUsuarioId(usuarioCancela.getId());
        movDto.setTipoMovimiento(TipoMovimiento.AJUSTE_MANUAL); // O puedes crear un TipoMovimiento.DEVOLUCION
        movDto.setCantidad(detalle.getCantidad()); // Positivo
        movDto.setMotivo("Reposición por cancelación Venta #" + venta.getId());

        // Sin try-catch, se une a la transacción principal
        movimientoStockService.registrarMovimiento(movDto);
    }
    
    // --- MÉTODOS DE CONSULTA (Estos ya estaban bien) ---

    @Transactional(readOnly = true)
    public VentaDTO findById(Long id) {
         Venta venta = ventaRepository.findByIdWithDetails(id)
                 .orElseThrow(() -> new RuntimeException("Venta no encontrada: " + id));
         return ventaMapper.toVentaDTO(venta);
    }

    @Transactional(readOnly = true)
    public Page<VentaDTO> findAll(Pageable pageable) {
         Page<Venta> ventaPage = ventaRepository.findAll(pageable);
         return ventaPage.map(ventaMapper::toVentaDTO);
    }

    @Transactional(readOnly = true)
    public Page<VentaDTO> findByCriteria(VentaFiltroDTO filtro, Pageable pageable) {
         Specification<Venta> spec = ventaSpecification.build(filtro);
         Page<Venta> ventaPage = ventaRepository.findAll(spec, pageable);
         return ventaPage.map(ventaMapper::toVentaDTO);
    }
    
    @Transactional(readOnly = true)
    public Page<VentaResumenDTO> findVentasByClienteEmail(String clienteEmail, Pageable pageable) {
        Usuario cliente = usuarioRepository.findByEmail(clienteEmail)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + clienteEmail));
        
        Pageable pageableOrdenado = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by("fechaVenta").descending()
        );

        Page<Venta> ventasPage = ventaRepository.findByCliente(cliente, pageableOrdenado);
        return ventasPage.map(ventaMapper::toVentaResumenDTO);
    }
}