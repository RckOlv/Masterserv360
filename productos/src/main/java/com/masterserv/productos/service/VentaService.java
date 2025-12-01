package com.masterserv.productos.service;

import com.masterserv.productos.dto.DetalleVentaDTO;
import com.masterserv.productos.dto.MovimientoStockDTO;
import com.masterserv.productos.dto.VentaDTO;
import com.masterserv.productos.dto.VentaFiltroDTO;
import com.masterserv.productos.dto.VentaResumenDTO;
import com.masterserv.productos.entity.*;
import com.masterserv.productos.enums.EstadoCupon;
import com.masterserv.productos.enums.EstadoVenta;
import com.masterserv.productos.enums.TipoDescuento;
import com.masterserv.productos.enums.TipoMovimiento;
import com.masterserv.productos.exceptions.CuponNoValidoException;
import com.masterserv.productos.mapper.VentaMapper;
import com.masterserv.productos.repository.UsuarioRepository;
import com.masterserv.productos.repository.VentaRepository;
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

    @Transactional
    public VentaDTO create(VentaDTO ventaDTO, String vendedorEmail) {
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
        venta.setMontoDescuento(null);

        // 1) Validar Cupón
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

        // 2) Procesar Detalles y calcular Subtotal
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

        // 3) Calcular Descuento
        BigDecimal descuentoTotal = calcularDescuento(cuponAplicado, subtotal, detallesVenta);
        
        // 4) Calcular Total Final
        venta.setTotalVenta(subtotal.subtract(descuentoTotal).max(BigDecimal.ZERO));

        // 5) Marcar Cupón como USADO
        if (cuponAplicado != null) {
            cuponService.marcarCuponComoUsado(cuponAplicado, venta);
        }

        // 6) Guardar venta
        Venta ventaGuardada = ventaRepository.save(venta);

        // 7) Registrar Movimientos de Stock
        for (DetalleVenta det : ventaGuardada.getDetalles()) {
            registrarMovimientoStockSalida(ventaGuardada, det, vendedor);
        }

        // 8) Asignar puntos
        puntosService.asignarPuntosPorVenta(ventaGuardada);

        // 9) Vaciar carrito
        carritoService.vaciarCarrito(vendedorEmail);

        // 10) PUBLICAR EVENTO
        eventPublisher.publishEvent(new VentaRealizadaEvent(this, ventaGuardada.getId()));
        
        return ventaMapper.toVentaDTO(ventaGuardada);
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
                        subtotalAplicable = subtotalAplicable.add(
                            det.getPrecioUnitario().multiply(new BigDecimal(det.getCantidad()))
                        );
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
        mov.setMotivo("Salida por Venta #" + venta.getId());
        mov.setVentaId(venta.getId());
        mov.setMotivo("Venta"); 
        movimientoStockService.registrarMovimiento(mov);
    }

    // --- CANCELAR VENTA ---
    @Transactional
    public void cancelarVenta(Long id, String emailCancela) {
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
            if (c.getFechaVencimiento().isAfter(LocalDate.now())) {
                c.setEstado(EstadoCupon.VIGENTE);
            } else {
                c.setEstado(EstadoCupon.VENCIDO);
            }
            c.setVenta(null);
            venta.setCupon(null);
        }
        ventaRepository.save(venta);
    }

    private void registrarMovimientoStockReposicion(Venta venta, DetalleVenta det, Usuario user) {
        MovimientoStockDTO mov = new MovimientoStockDTO();
        mov.setProductoId(det.getProducto().getId());
        mov.setUsuarioId(user.getId());
        mov.setTipoMovimiento(TipoMovimiento.DEVOLUCION);
        mov.setCantidad(det.getCantidad());
        mov.setMotivo("Reposición por cancelación Venta #" + venta.getId());
        mov.setVentaId(venta.getId());
        movimientoStockService.registrarMovimiento(mov);
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

    // ------------------------------------------------------------
    // GENERACIÓN DE PDF COMPROBANTE (CORREGIDO Y DEFINITIVO)
    // ------------------------------------------------------------
    @Transactional(readOnly = true)
    public byte[] generarComprobantePdf(Long ventaId) {
        Venta venta = findVentaByIdWithDetails(ventaId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. CABECERA DE LA EMPRESA
            Font fontTitulo = new Font(Font.HELVETICA, 20, Font.BOLD);
            Paragraph titulo = new Paragraph("MASTERSERV360", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);

            Font fontSubtitulo = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);
            Paragraph datosEmpresa = new Paragraph(
                "Razón Social: Masterserv S.A.\n" + 
                "CUIT: 30-12345678-9\n" + 
                "Inicio de Actividades: 01/01/2020\n" +
                "Dirección: Av. San Martín 1234, El Soberbio, Misiones\n" +
                "Tel: (3755) 12-3456", 
                fontSubtitulo
            );
            datosEmpresa.setAlignment(Element.ALIGN_CENTER);
            datosEmpresa.setSpacingAfter(20);
            document.add(datosEmpresa);

            LineSeparator separator = new LineSeparator();
            separator.setLineColor(Color.LIGHT_GRAY);
            document.add(separator);

            // 2. DATOS DE LA VENTA
            Font fontCuerpo = new Font(Font.HELVETICA, 11, Font.NORMAL);
            Paragraph infoVenta = new Paragraph();
            infoVenta.setSpacingBefore(15);
            infoVenta.setSpacingAfter(15);
            
            // --- AQUÍ ESTÁ EL CAMBIO A "Nº VENTA" ---
            infoVenta.add(new Chunk("Nº Venta: " + venta.getId() + "\n", new Font(Font.HELVETICA, 14, Font.BOLD)));
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            infoVenta.add("Fecha: " + venta.getFechaVenta().format(formatter) + "\n");
            infoVenta.add("Cliente: " + venta.getCliente().getNombre() + " " + venta.getCliente().getApellido() + "\n");
            
            if(venta.getCliente().getDocumento() != null) {
                infoVenta.add("DNI/CUIT: " + venta.getCliente().getDocumento() + "\n");
            }
            
            infoVenta.add("Atendido por: " + (venta.getVendedor() != null ? venta.getVendedor().getNombre() : "Sistema") + "\n");
            document.add(infoVenta);

            // 3. TABLA DE PRODUCTOS
            PdfPTable table = new PdfPTable(4); 
            table.setWidthPercentage(100);
            table.setWidths(new float[]{45f, 10f, 20f, 25f});
            table.setSpacingBefore(10f);

            // Cabeceras
            Font fontHeader = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
            String[] headers = {"Producto", "Cant.", "Precio Unit.", "Subtotal"};
            
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, fontHeader));
                cell.setBackgroundColor(Color.DARK_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(6);
                table.addCell(cell);
            }

            // Filas
            Font fontCell = new Font(Font.HELVETICA, 10);
            BigDecimal subtotalSinDescuento = BigDecimal.ZERO;

            for (DetalleVenta det : venta.getDetalles()) {
                PdfPCell cellProd = new PdfPCell(new Phrase(det.getProducto().getNombre(), fontCell));
                cellProd.setPadding(5);
                table.addCell(cellProd);
                
                PdfPCell cellCant = new PdfPCell(new Phrase(String.valueOf(det.getCantidad()), fontCell));
                cellCant.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellCant.setPadding(5);
                table.addCell(cellCant);
                
                PdfPCell cellPrecio = new PdfPCell(new Phrase("$" + det.getPrecioUnitario(), fontCell));
                cellPrecio.setHorizontalAlignment(Element.ALIGN_RIGHT);
                cellPrecio.setPadding(5);
                table.addCell(cellPrecio);
                
                BigDecimal subItem = det.getPrecioUnitario().multiply(BigDecimal.valueOf(det.getCantidad()));
                subtotalSinDescuento = subtotalSinDescuento.add(subItem);
                
                PdfPCell cellSub = new PdfPCell(new Phrase("$" + subItem, fontCell));
                cellSub.setHorizontalAlignment(Element.ALIGN_RIGHT);
                cellSub.setPadding(5);
                table.addCell(cellSub);
            }

            document.add(table);

            // 4. TOTALES Y DESCUENTOS
            Paragraph totales = new Paragraph();
            totales.setAlignment(Element.ALIGN_RIGHT);
            totales.setSpacingBefore(15);

            BigDecimal descuento = subtotalSinDescuento.subtract(venta.getTotalVenta());
            
            if (descuento.compareTo(BigDecimal.ZERO) > 0) {
                totales.add(new Chunk("Subtotal: $" + subtotalSinDescuento + "\n", new Font(Font.HELVETICA, 10)));
                String cuponTexto = (venta.getCupon() != null) ? " (" + venta.getCupon().getCodigo() + ")" : "";
                totales.add(new Chunk("Descuento aplicado" + cuponTexto + ": -$" + descuento + "\n", new Font(Font.HELVETICA, 10, Font.BOLD, Color.RED)));
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