package com.masterserv.productos.service;

import com.masterserv.productos.dto.DetalleVentaDTO;
import com.masterserv.productos.dto.MovimientoStockDTO;
import com.masterserv.productos.dto.VentaDTO;
import com.masterserv.productos.dto.VentaFiltroDTO;
import com.masterserv.productos.dto.VentaResumenDTO;
import com.masterserv.productos.entity.*;
import com.masterserv.productos.entity.Caja; 
import com.masterserv.productos.enums.EstadoCupon;
import com.masterserv.productos.enums.EstadoVenta;
import com.masterserv.productos.enums.TipoDescuento;
import com.masterserv.productos.enums.TipoMovimiento;
import com.masterserv.productos.exceptions.CuponNoValidoException;
import com.masterserv.productos.mapper.VentaMapper;
import com.masterserv.productos.repository.UsuarioRepository;
import com.masterserv.productos.repository.VentaRepository;
import com.masterserv.productos.repository.AuditoriaRepository;
import com.masterserv.productos.repository.CajaRepository; 
import com.masterserv.productos.specification.VentaSpecification;

import com.masterserv.productos.event.VentaRealizadaEvent;
import org.springframework.context.ApplicationEventPublisher;

// --- IMPORTS PARA PDF (OpenPDF / iText) ---
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
// ------------------------------------------

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

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

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
    @Autowired private AuditoriaRepository auditoriaRepository; 
    @Autowired private CajaRepository cajaRepository; 
    
    @Autowired private ProcesoAutomaticoService procesoAutomaticoService;

    @Transactional
    public VentaDTO create(VentaDTO ventaDTO, String vendedorEmail) {
        Usuario vendedor = usuarioRepository.findByEmail(vendedorEmail)
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado: " + vendedorEmail));

        Caja cajaAbierta = cajaRepository.findCajaAbiertaByUsuario(vendedor.getId())
                .orElseThrow(() -> new RuntimeException("¡ALTO! No puedes vender. Debes abrir tu caja primero."));

        Usuario cliente = usuarioRepository.findById(ventaDTO.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + ventaDTO.getClienteId()));

        Venta venta = new Venta();
        venta.setFechaVenta(LocalDateTime.now());
        venta.setEstado(EstadoVenta.COMPLETADA);
        venta.setVendedor(vendedor);
        venta.setCliente(cliente);
        venta.setDetalles(new HashSet<>());
        venta.setMontoDescuento(null);
        
        String metodoPago = ventaDTO.getMetodoPago() != null ? ventaDTO.getMetodoPago().toUpperCase() : "EFECTIVO";
        venta.setMetodoPago(metodoPago); 

        Cupon cuponAplicado = null;
        if (ventaDTO.getCodigoCupon() != null && !ventaDTO.getCodigoCupon().isBlank()) {
            try {
                cuponAplicado = cuponService.validarCupon(ventaDTO.getCodigoCupon(), cliente);
                venta.setCupon(cuponAplicado);
            } catch (CuponNoValidoException ex) {
                logger.warn("Cupón no válido: {}", ex.getMessage());
                throw ex;
            }
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        Set<DetalleVenta> detallesVenta = new HashSet<>(); 

        for (DetalleVentaDTO d : ventaDTO.getDetalles()) {
            Producto p = productoService.descontarStock(d.getProductoId(), d.getCantidad());

            DetalleVenta det = new DetalleVenta();
            det.setProducto(p);
            det.setCantidad(d.getCantidad());
            det.setPrecioUnitario(p.getPrecioVenta()); 
            det.setVenta(venta);
            
            detallesVenta.add(det);
            subtotal = subtotal.add(det.getPrecioUnitario().multiply(BigDecimal.valueOf(det.getCantidad())));
        }
        
        venta.setDetalles(detallesVenta);

        BigDecimal descuentoTotal = calcularDescuento(cuponAplicado, subtotal, detallesVenta);
        
        BigDecimal totalFinal = subtotal.subtract(descuentoTotal).max(BigDecimal.ZERO);
        venta.setTotalVenta(totalFinal);

        if ("TARJETA".equals(metodoPago) || "DEBITO".equals(metodoPago) || "CREDITO".equals(metodoPago)) {
            cajaAbierta.setVentasTarjeta(cajaAbierta.getVentasTarjeta().add(totalFinal));
        } else if ("TRANSFERENCIA".equals(metodoPago)) {
            cajaAbierta.setVentasTransferencia(cajaAbierta.getVentasTransferencia().add(totalFinal));
        } else {
            cajaAbierta.setVentasEfectivo(cajaAbierta.getVentasEfectivo().add(totalFinal));
        }
        cajaRepository.save(cajaAbierta); 

        if (cuponAplicado != null) {
            cuponService.marcarCuponComoUsado(cuponAplicado, venta);
        }

        Venta ventaGuardada = ventaRepository.save(venta);

        for (DetalleVenta det : ventaGuardada.getDetalles()) {
            registrarMovimientoStockSalida(ventaGuardada, det, vendedor);
        }

        puntosService.asignarPuntosPorVenta(ventaGuardada);
        carritoService.vaciarCarrito(vendedorEmail);
        eventPublisher.publishEvent(new VentaRealizadaEvent(this, ventaGuardada.getId()));
        registrarAuditoriaVenta(ventaGuardada, vendedor);

        new Thread(() -> {
            try {
                Thread.sleep(2000); 
                procesoAutomaticoService.generarPrePedidosAgrupados();
            } catch (Exception e) {
                logger.error("Error trigger automático reposición: " + e.getMessage());
            }
        }).start();
        
        return ventaMapper.toVentaDTO(ventaGuardada);
    }

    private void registrarAuditoriaVenta(Venta venta, Usuario vendedor) {
        try {
            Auditoria audit = new Auditoria();
            audit.setFecha(LocalDateTime.now());
            audit.setUsuario(vendedor.getEmail());
            audit.setEntidad("Venta");
            audit.setEntidadId(venta.getId().toString());
            audit.setAccion("NUEVA_VENTA");
            
            String detalle = String.format("Venta #%d realizada. Cliente: %s | Total: $%.2f | Ítems: %d",
                    venta.getId(),
                    venta.getCliente().getNombre() + " " + venta.getCliente().getApellido(),
                    venta.getTotalVenta(),
                    venta.getDetalles().size());
            
            audit.setDetalle(detalle);
            audit.setValorNuevo("{ \"total\": " + venta.getTotalVenta() + ", \"estado\": \"COMPLETADA\" }");
            
            auditoriaRepository.save(audit);
        } catch (Exception e) {
            logger.error("🔴 Error al auditar la venta: " + e.getMessage());
        }
    }

    private BigDecimal calcularDescuento(Cupon cupon, BigDecimal subtotal, Set<DetalleVenta> detalles) {
        if (cupon == null) return BigDecimal.ZERO;
        if (cupon.getTipoDescuento() == TipoDescuento.FIJO) {
            return cupon.getValor().min(subtotal);
        }
        if (cupon.getTipoDescuento() == TipoDescuento.PORCENTAJE) {
            BigDecimal porcentaje = cupon.getValor().divide(new BigDecimal(100), 4, RoundingMode.HALF_UP);
            if (cupon.getCategoria() != null) {
                Long categoriaIdDescuento = cupon.getCategoria().getId();
                BigDecimal subtotalAplicable = BigDecimal.ZERO;
                for (DetalleVenta det : detalles) {
                    if (det.getProducto().getCategoria().getId().equals(categoriaIdDescuento)) {
                        subtotalAplicable = subtotalAplicable.add(det.getPrecioUnitario().multiply(new BigDecimal(det.getCantidad())));
                    }
                }
                return subtotalAplicable.multiply(porcentaje).setScale(2, RoundingMode.HALF_UP);
            } else {
                return subtotal.multiply(porcentaje).setScale(2, RoundingMode.HALF_UP);
            }
        }
        return BigDecimal.ZERO;
    }

    private void registrarMovimientoStockSalida(Venta venta, DetalleVenta det, Usuario vendedor) {
        MovimientoStockDTO mov = new MovimientoStockDTO();
        mov.setProductoId(det.getProducto().getId());
        mov.setUsuarioId(vendedor.getId());
        mov.setTipoMovimiento(TipoMovimiento.SALIDA_VENTA);
        mov.setCantidad(det.getCantidad());
        mov.setVentaId(venta.getId());
        mov.setMotivo("Salida por Venta #" + venta.getId()); 
        movimientoStockService.registrarMovimiento(mov);
    }

    // ✅ NUEVO: CANCELAR VENTA CON MOTIVO INCLUIDO
    @Transactional
    public void cancelarVenta(Long id, String emailCancela, String motivo) {
        Venta venta = ventaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada: " + id));

        if (venta.getEstado() != EstadoVenta.COMPLETADA)
            throw new RuntimeException("Solo se pueden cancelar ventas COMPLETADAS.");

        Usuario user = usuarioRepository.findByEmail(emailCancela)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + emailCancela));

        // ✅ REVERTIR DINERO DE LA CAJA AL CANCELAR
        Caja cajaAbierta = cajaRepository.findCajaAbiertaByUsuario(user.getId()).orElse(null);
        if (cajaAbierta != null) {
            String metodo = venta.getMetodoPago() != null ? venta.getMetodoPago().toUpperCase() : "EFECTIVO";
            BigDecimal total = venta.getTotalVenta();
            
            if ("TARJETA".equals(metodo) || "DEBITO".equals(metodo) || "CREDITO".equals(metodo)) {
                cajaAbierta.setVentasTarjeta(cajaAbierta.getVentasTarjeta().subtract(total));
            } else if ("TRANSFERENCIA".equals(metodo)) {
                cajaAbierta.setVentasTransferencia(cajaAbierta.getVentasTransferencia().subtract(total));
            } else {
                cajaAbierta.setVentasEfectivo(cajaAbierta.getVentasEfectivo().subtract(total));
            }
            cajaRepository.save(cajaAbierta);
        }

        for (DetalleVenta det : venta.getDetalles()) {
            productoService.reponerStock(det.getProducto().getId(), det.getCantidad());
            registrarMovimientoStockReposicion(venta, det, user, motivo); // <- PASAMOS MOTIVO ACÁ
        }

        // ✅ ACÁ AGREGAMOS EL SETEO DE LA OBSERVACIÓN EN LA ENTIDAD
        venta.setEstado(EstadoVenta.CANCELADA);
        venta.setObservacionCancelacion(motivo);
        
        puntosService.revertirPuntosPorVenta(venta);

        if (venta.getCupon() != null) {
            Cupon c = venta.getCupon();
            c.setEstado(c.getFechaVencimiento().isAfter(LocalDate.now()) ? EstadoCupon.VIGENTE : EstadoCupon.VENCIDO);
            c.setVenta(null);
            venta.setCupon(null);
        }

        Venta ventaCancelada = ventaRepository.save(venta);
        
        // REGISTRO DE AUDITORÍA CON MOTIVO
        registrarAuditoriaCancelacion(ventaCancelada, user, motivo); 
    }

    // ✅ NUEVO: REGISTRAR REPOSICIÓN CON MOTIVO RECIBIDO
    private void registrarMovimientoStockReposicion(Venta venta, DetalleVenta det, Usuario user, String motivo) {
        MovimientoStockDTO mov = new MovimientoStockDTO();
        mov.setProductoId(det.getProducto().getId());
        mov.setUsuarioId(user.getId());
        mov.setTipoMovimiento(TipoMovimiento.DEVOLUCION);
        mov.setCantidad(det.getCantidad());
        mov.setMotivo("Cancelación Venta #" + venta.getId() + " - " + motivo);
        mov.setVentaId(venta.getId());
        movimientoStockService.registrarMovimiento(mov);
    }

    // ✅ NUEVO: AUDITORÍA DE CANCELACIÓN CON MOTIVO RECIBIDO
    private void registrarAuditoriaCancelacion(Venta venta, Usuario usuario, String motivo) {
        try {
            Auditoria audit = new Auditoria();
            audit.setFecha(LocalDateTime.now());
            audit.setUsuario(usuario.getEmail());
            audit.setEntidad("Venta");
            audit.setEntidadId(venta.getId().toString());
            audit.setAccion("CANCELACION_VENTA");
            
            audit.setDetalle("Venta #" + venta.getId() + " ANULADA. Motivo: " + motivo + ". (Stock repuesto y plata restada de caja)");
            audit.setValorAnterior("{ \"estado\": \"COMPLETADA\" }");
            audit.setValorNuevo("{ \"estado\": \"CANCELADA\" }");
            
            auditoriaRepository.save(audit);
        } catch (Exception e) {
            logger.error("🔴 Error al auditar cancelación: " + e.getMessage());
        }
    }


    // --- CONSULTAS ---
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
    
    @Transactional(readOnly = true)
    public Page<VentaDTO> findByCriteriaForVendedor(VentaFiltroDTO filtro, Pageable pageable, String vendedorEmail) {
        Usuario vendedor = usuarioRepository.findByEmail(vendedorEmail)
                .orElseThrow(() -> new RuntimeException("Vendedor logueado no encontrado: " + vendedorEmail));
        filtro.setVendedorId(vendedor.getId());
        Specification<Venta> spec = ventaSpecification.build(filtro);
        return ventaRepository.findAll(spec, pageable).map(ventaMapper::toVentaDTO);
    }

    @Transactional(readOnly = true)
    public Page<VentaDTO> findByCriteria(VentaFiltroDTO filtro, Pageable pageable) {
        Specification<Venta> spec = ventaSpecification.build(filtro);
        return ventaRepository.findAll(spec, pageable).map(ventaMapper::toVentaDTO);
    }

    @Transactional(readOnly = true)
    public Page<VentaResumenDTO> findVentasByClienteEmail(String email, Pageable pageable) {
        Usuario cliente = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + email));
        Pageable ordenado = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("fechaVenta").descending());
        return ventaRepository.findByCliente(cliente, ordenado).map(ventaMapper::toVentaResumenDTO);
    }

    @Transactional(readOnly = true)
    public Venta findVentaByIdWithDetails(Long id) {
        return ventaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada: " + id));
    }

    // --- PDF GENERATION ---
    @Transactional(readOnly = true)
    public byte[] generarComprobantePdf(Long ventaId) {
        Venta venta = findVentaByIdWithDetails(ventaId);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. CABECERA
            Font fontTitulo = new Font(Font.HELVETICA, 20, Font.BOLD);
            Paragraph titulo = new Paragraph("MASTERSERV360", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);

            Font fontSubtitulo = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);
            Paragraph datosEmpresa = new Paragraph("Razón Social: Masterserv S.A.\nCUIT: 30-12345678-9\nDirección: Av. San Martín 1234, El Soberbio, Misiones\nTel: (3755) 12-3456", fontSubtitulo);
            datosEmpresa.setAlignment(Element.ALIGN_CENTER);
            datosEmpresa.setSpacingAfter(20);
            document.add(datosEmpresa);

            LineSeparator separator = new LineSeparator();
            separator.setLineColor(Color.LIGHT_GRAY);
            document.add(separator);

            // 2. INFO VENTA
            Paragraph infoVenta = new Paragraph();
            infoVenta.setSpacingBefore(15);
            infoVenta.setSpacingAfter(15);
            infoVenta.add(new Chunk("Nº Venta: " + venta.getId() + "\n", new Font(Font.HELVETICA, 14, Font.BOLD)));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            infoVenta.add("Fecha: " + venta.getFechaVenta().format(formatter) + "\n");
            
            String metodo = venta.getMetodoPago() != null ? venta.getMetodoPago() : "EFECTIVO";
            infoVenta.add("Método de Pago: " + metodo + "\n");
            
            infoVenta.add("Cliente: " + venta.getCliente().getNombre() + " " + venta.getCliente().getApellido() + "\n");
            if(venta.getCliente().getDocumento() != null) infoVenta.add("DNI/CUIT: " + venta.getCliente().getDocumento() + "\n");
            infoVenta.add("Atendido por: " + (venta.getVendedor() != null ? venta.getVendedor().getNombre() : "Sistema") + "\n");
            document.add(infoVenta);

            // 3. TABLA PRODUCTOS
            PdfPTable table = new PdfPTable(4); 
            table.setWidthPercentage(100);
            table.setWidths(new float[]{45f, 10f, 20f, 25f});
            String[] headers = {"Producto", "Cant.", "Precio Unit.", "Subtotal"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE)));
                cell.setBackgroundColor(Color.DARK_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6);
                table.addCell(cell);
            }

            BigDecimal subtotalSinDescuento = BigDecimal.ZERO;
            for (DetalleVenta det : venta.getDetalles()) {
                table.addCell(new PdfPCell(new Phrase(det.getProducto().getNombre(), new Font(Font.HELVETICA, 10))));
                
                PdfPCell cCant = new PdfPCell(new Phrase(String.valueOf(det.getCantidad()), new Font(Font.HELVETICA, 10)));
                cCant.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cCant);
                
                PdfPCell cPrec = new PdfPCell(new Phrase("$" + det.getPrecioUnitario(), new Font(Font.HELVETICA, 10)));
                cPrec.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cPrec);
                
                BigDecimal subItem = det.getPrecioUnitario().multiply(BigDecimal.valueOf(det.getCantidad()));
                subtotalSinDescuento = subtotalSinDescuento.add(subItem);
                
                PdfPCell cSub = new PdfPCell(new Phrase("$" + subItem, new Font(Font.HELVETICA, 10)));
                cSub.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cSub);
            }
            document.add(table);

            // 4. TOTALES
            Paragraph totales = new Paragraph();
            totales.setAlignment(Element.ALIGN_RIGHT);
            totales.setSpacingBefore(15);
            BigDecimal descuento = subtotalSinDescuento.subtract(venta.getTotalVenta());
            if (descuento.compareTo(BigDecimal.ZERO) > 0) {
                totales.add(new Chunk("Subtotal: $" + subtotalSinDescuento + "\n", new Font(Font.HELVETICA, 10)));
                String cuponT = (venta.getCupon() != null) ? " (" + venta.getCupon().getCodigo() + ")" : "";
                totales.add(new Chunk("Descuento aplicado" + cuponT + ": -$" + descuento + "\n", new Font(Font.HELVETICA, 10, Font.BOLD, Color.RED)));
            }
            totales.add(new Chunk("TOTAL: $" + venta.getTotalVenta(), new Font(Font.HELVETICA, 18, Font.BOLD)));
            document.add(totales);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            logger.error("Error al generar PDF", e);
            throw new RuntimeException("Error al generar comprobante PDF");
        }
    }
}