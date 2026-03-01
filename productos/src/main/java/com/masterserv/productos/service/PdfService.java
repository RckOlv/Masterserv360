package com.masterserv.productos.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.masterserv.productos.dto.DashboardFilterDTO;
import com.masterserv.productos.dto.DashboardStatsDTO;
import com.masterserv.productos.dto.TopProductoDTO;
import com.masterserv.productos.dto.reporte.StockInmovilizadoDTO;
import com.masterserv.productos.dto.reporte.ValorizacionInventarioDTO;
import com.masterserv.productos.dto.reporte.VariacionCostoDTO;
import com.masterserv.productos.entity.DetallePedido;
import com.masterserv.productos.entity.DetalleVenta;
import com.masterserv.productos.entity.EmpresaConfig;
import com.masterserv.productos.entity.Pedido;
import com.masterserv.productos.entity.Venta;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

@Service
public class PdfService {

    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);

    // --- FUENTES ESTÁNDAR ---
    private static final Font FONT_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
    private static final Font FONT_SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
    private static final Font FONT_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font FONT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font FONT_DATA_EMPRESA = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);
    private static final Font FONT_HEADER_TABLA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Font FONT_LEYENDA = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.DARK_GRAY);
    private static final Font FONT_FOOTER = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);

    @Autowired private DashboardService dashboardService;
    @Autowired private EmpresaConfigService empresaConfigService;

    // ========================================================================
    // ✅ CLASE DE EVENTOS: PARA "PÁGINA X DE Y" Y PIE DE PÁGINA
    // ========================================================================
    private static class HeaderFooterPageEvent extends PdfPageEventHelper {
        private final String nombreEmpresa;
        private PdfTemplate totalPaginas;

        public HeaderFooterPageEvent(String nombreEmpresa) {
            this.nombreEmpresa = nombreEmpresa;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPaginas = writer.getDirectContent().createTemplate(30, 16);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfPTable footer = new PdfPTable(3);
            try {
                footer.setWidths(new float[]{24, 2, 1});
                footer.setTotalWidth(527);
                footer.setLockedWidth(true);
                footer.getDefaultCell().setFixedHeight(20);
                footer.getDefaultCell().setBorder(Rectangle.TOP);
                footer.getDefaultCell().setBorderColor(Color.LIGHT_GRAY);

                footer.addCell(new Phrase(nombreEmpresa + " - Reportes y Comprobantes", FONT_FOOTER));

                PdfPCell cellPagina = new PdfPCell(new Phrase(String.format("Página %d de ", writer.getPageNumber()), FONT_FOOTER));
                cellPagina.setBorder(Rectangle.TOP);
                cellPagina.setBorderColor(Color.LIGHT_GRAY);
                cellPagina.setHorizontalAlignment(Element.ALIGN_RIGHT);
                footer.addCell(cellPagina);

                PdfPCell totalCell = new PdfPCell(Image.getInstance(totalPaginas));
                totalCell.setBorder(Rectangle.TOP);
                totalCell.setBorderColor(Color.LIGHT_GRAY);
                footer.addCell(totalCell);

                footer.writeSelectedRows(0, -1, 34, 30, writer.getDirectContent());
            } catch (DocumentException de) {
                throw new ExceptionConverter(de);
            }
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            ColumnText.showTextAligned(totalPaginas, Element.ALIGN_LEFT,
                    new Phrase(String.valueOf(writer.getPageNumber() - 1), FONT_FOOTER), 2, 2, 0);
        }
    }

    // ========================================================================
    // MÉTODOS AUXILIARES
    // ========================================================================
    private String formatearMoneda(BigDecimal valor) {
        if (valor == null) valor = BigDecimal.ZERO;
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
        simbolos.setGroupingSeparator('.'); 
        simbolos.setDecimalSeparator(','); 
        return "$ " + new DecimalFormat("#,##0.00", simbolos).format(valor);
    }

    private String obtenerUsuarioActual() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "Sistema";
        }
    }

    private void construirCabeceraEmpresa(Document document, String subtituloReporte) throws DocumentException {
        EmpresaConfig config = empresaConfigService.obtenerConfiguracion();
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingAfter(10);
        headerTable.setWidths(new float[]{6f, 4f});

        PdfPCell cellLeft = new PdfPCell();
        cellLeft.setBorder(Rectangle.NO_BORDER);

        if (config.getLogoUrl() != null && !config.getLogoUrl().isEmpty()) {
            try {
                Image logo = null;
                String logoData = config.getLogoUrl();
                if (logoData.startsWith("http")) {
                    logo = Image.getInstance(logoData);
                } else if (logoData.contains("base64,")) {
                    byte[] imageBytes = Base64.getDecoder().decode(logoData.split(",")[1]);
                    logo = Image.getInstance(imageBytes);
                }
                if (logo != null) {
                    logo.scaleToFit(90, 45);
                    cellLeft.addElement(logo);
                }
            } catch (Exception e) {
                logger.warn("Error cargando logo en PDF");
            }
        }

        cellLeft.addElement(new Paragraph(config.getNombreFantasia().toUpperCase(), FONT_TITULO));
        cellLeft.addElement(new Paragraph(config.getRazonSocial() + " | CUIT: " + config.getCuit(), FONT_DATA_EMPRESA));
        cellLeft.addElement(new Paragraph(config.getDireccion(), FONT_DATA_EMPRESA));
        headerTable.addCell(cellLeft);

        PdfPCell cellRight = new PdfPCell();
        cellRight.setBorder(Rectangle.NO_BORDER);
        cellRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellRight.setVerticalAlignment(Element.ALIGN_BOTTOM);
        cellRight.addElement(new Paragraph("Fecha: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), FONT_NORMAL));
        cellRight.addElement(new Paragraph("Emitido por: " + obtenerUsuarioActual(), FONT_NORMAL));
        
        Paragraph pDoc = new Paragraph("\n" + subtituloReporte.toUpperCase(), FONT_SUBTITULO);
        pDoc.setAlignment(Element.ALIGN_RIGHT);
        cellRight.addElement(pDoc);
        headerTable.addCell(cellRight);

        document.add(headerTable);
        document.add(new LineSeparator());
    }

    // ========================================================================
    // COMPROBANTES: PEDIDOS Y VENTAS
    // ========================================================================
    public byte[] generarComprobantePedido(Pedido pedido) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 60);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new HeaderFooterPageEvent(empresaConfigService.obtenerConfiguracion().getNombreFantasia()));
            document.open();
            construirCabeceraEmpresa(document, "Orden de Compra (Interno)");

            Paragraph info = new Paragraph();
            info.setSpacingBefore(10);
            info.add(new Chunk("Nº Pedido: " + pedido.getId() + "\n", FONT_BOLD));
            info.add(new Chunk("Estado: " + pedido.getEstado() + "\n", FONT_NORMAL));
            if (pedido.getProveedor() != null) info.add(new Chunk("PROVEEDOR: " + pedido.getProveedor().getRazonSocial() + "\n", FONT_NORMAL));
            document.add(info);

            PdfPTable table = new PdfPTable(4); 
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 4f, 1f, 2f, 2f });
            table.setSpacingBefore(10f);
            table.addCell(crearCeldaHeader("Producto")); table.addCell(crearCeldaHeader("Cant."));
            table.addCell(crearCeldaHeader("Costo Unit.")); table.addCell(crearCeldaHeader("Subtotal"));

            BigDecimal total = BigDecimal.ZERO;
            for (DetallePedido d : pedido.getDetalles()) {
                table.addCell(new Paragraph(d.getProducto().getNombre(), FONT_NORMAL));
                PdfPCell cCant = new PdfPCell(new Paragraph(String.valueOf(d.getCantidad()), FONT_NORMAL));
                cCant.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cCant);
                BigDecimal precio = (d.getPrecioUnitario() != null) ? d.getPrecioUnitario() : BigDecimal.ZERO;
                table.addCell(crearCeldaMoneda(precio));
                BigDecimal sub = precio.multiply(new BigDecimal(d.getCantidad()));
                total = total.add(sub);
                table.addCell(crearCeldaMoneda(sub));
            }
            document.add(table);
            Paragraph pTot = new Paragraph("\nTOTAL: " + formatearMoneda(pedido.getTotalPedido() != null ? pedido.getTotalPedido() : total), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
            pTot.setAlignment(Element.ALIGN_RIGHT);
            document.add(pTot);
        } catch (Exception e) { throw new RuntimeException("Error en Pedido PDF", e); } 
        finally { document.close(); }
        return baos.toByteArray();
    }

    public byte[] generarOrdenCompraProveedor(Pedido pedido) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 60);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            EmpresaConfig config = empresaConfigService.obtenerConfiguracion();
            writer.setPageEvent(new HeaderFooterPageEvent(config.getNombreFantasia()));
            document.open();
            construirCabeceraEmpresa(document, "Solicitud de Mercadería");

            Paragraph info = new Paragraph("\nOrden Nº: " + pedido.getId() + "\nFecha: " + pedido.getFechaPedido().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), FONT_BOLD);
            if (pedido.getProveedor() != null) info.add(new Chunk("\nPROVEEDOR: " + pedido.getProveedor().getRazonSocial(), FONT_NORMAL));
            document.add(info);

            PdfPTable table = new PdfPTable(2); 
            table.setWidthPercentage(100); table.setWidths(new float[] { 5f, 1f }); table.setSpacingBefore(10f);
            table.addCell(crearCeldaHeader("Descripción del Producto")); table.addCell(crearCeldaHeader("Cantidad"));

            for (DetallePedido d : pedido.getDetalles()) {
                table.addCell(new Paragraph(d.getProducto().getNombre(), FONT_NORMAL));
                PdfPCell c = new PdfPCell(new Paragraph(String.valueOf(d.getCantidad()), FONT_NORMAL));
                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(c);
            }
            document.add(table);
            String textoPie = (config.getPiePaginaPresupuesto() != null && !config.getPiePaginaPresupuesto().isEmpty()) ? config.getPiePaginaPresupuesto() : "Favor de confirmar recepción y disponibilidad.";
            Paragraph pPie = new Paragraph("\n" + textoPie, FONT_DATA_EMPRESA);
            pPie.setAlignment(Element.ALIGN_CENTER);
            document.add(pPie);
        } catch (Exception e) { throw new RuntimeException("Error en OC PDF", e); } 
        finally { document.close(); }
        return baos.toByteArray();
    }

    public byte[] generarComprobanteVenta(Venta venta) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 60);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new HeaderFooterPageEvent(empresaConfigService.obtenerConfiguracion().getNombreFantasia()));
            document.open();
            construirCabeceraEmpresa(document, "Comprobante de Venta (No Fiscal)");

            Paragraph info = new Paragraph();
            info.add(new Chunk("\nVenta Nº: " + venta.getId() + "\n", FONT_BOLD));
            info.add(new Chunk("Cliente: " + (venta.getCliente() != null ? venta.getCliente().getNombre() + " " + venta.getCliente().getApellido() : "Consumidor Final") + "\n", FONT_NORMAL));
            document.add(info);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100); table.setWidths(new float[] { 4f, 1f, 2f, 2f }); table.setSpacingBefore(10);
            table.addCell(crearCeldaHeader("Producto")); table.addCell(crearCeldaHeader("Cant."));
            table.addCell(crearCeldaHeader("P. Unit.")); table.addCell(crearCeldaHeader("Subtotal"));

            BigDecimal subtotalReal = BigDecimal.ZERO;
            for (DetalleVenta d : venta.getDetalles()) {
                table.addCell(new Paragraph(d.getProducto().getNombre(), FONT_NORMAL));
                PdfPCell c = new PdfPCell(new Paragraph(String.valueOf(d.getCantidad()), FONT_NORMAL));
                c.setHorizontalAlignment(Element.ALIGN_CENTER); table.addCell(c);
                table.addCell(crearCeldaMoneda(d.getPrecioUnitario()));
                BigDecimal sub = d.getPrecioUnitario().multiply(new BigDecimal(d.getCantidad()));
                subtotalReal = subtotalReal.add(sub);
                table.addCell(crearCeldaMoneda(sub));
            }
            document.add(table);

            Paragraph tot = new Paragraph(); tot.setAlignment(Element.ALIGN_RIGHT);
            BigDecimal desc = subtotalReal.subtract(venta.getTotalVenta());
            if (desc.compareTo(BigDecimal.ZERO) > 0) {
                tot.add(new Chunk("\nSubtotal: " + formatearMoneda(subtotalReal), FONT_NORMAL));
                tot.add(new Chunk("\nDescuento: -" + formatearMoneda(desc), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.RED)));
            }
            tot.add(new Chunk("\nTOTAL: " + formatearMoneda(venta.getTotalVenta()), FONT_TITULO));
            document.add(tot);
        } catch (Exception e) { throw new RuntimeException("Error en Venta PDF", e); } 
        finally { document.close(); }
        return baos.toByteArray();
    }

    // ========================================================================
    // REPORTES AVANZADOS (DASHBOARD)
    // ========================================================================
    public byte[] generarReporteDashboard(DashboardFilterDTO filtro) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 60);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new HeaderFooterPageEvent(empresaConfigService.obtenerConfiguracion().getNombreFantasia()));
            document.open();
            construirCabeceraEmpresa(document, "Reporte Analítico de Gestión");

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String rango = "Periodo: " + (filtro.getFechaInicio() != null ? filtro.getFechaInicio().format(fmt) : "Inicio") + 
                           " al " + (filtro.getFechaFin() != null ? filtro.getFechaFin().format(fmt) : "Hoy");
            Paragraph pRango = new Paragraph(rango, FONT_SUBTITULO); pRango.setAlignment(Element.ALIGN_CENTER); pRango.setSpacingAfter(15);
            document.add(pRango);

            DashboardStatsDTO stats = dashboardService.getEstadisticasFiltradas(filtro.getFechaInicio(), filtro.getFechaFin());
            PdfPTable tableM = new PdfPTable(3); tableM.setWidthPercentage(100); tableM.setSpacingBefore(10);
            tableM.addCell(crearCeldaHeader("Ventas Totales")); tableM.addCell(crearCeldaHeader("Operaciones")); tableM.addCell(crearCeldaHeader("Ticket Promedio"));
            tableM.addCell(crearCeldaDatoCenter(formatearMoneda(stats.getTotalVentasMes())));
            tableM.addCell(crearCeldaDatoCenter(String.valueOf(stats.getCantidadVentasPeriodo())));
            BigDecimal avg = (stats.getCantidadVentasPeriodo() > 0) ? stats.getTotalVentasMes().divide(BigDecimal.valueOf(stats.getCantidadVentasPeriodo()), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            tableM.addCell(crearCeldaDatoCenter(formatearMoneda(avg)));
            document.add(tableM);

        } catch (Exception e) { throw new RuntimeException("Error en Reporte PDF", e); } 
        finally { document.close(); }
        return baos.toByteArray();
    }

    // ✅ NUEVO: REPORTE VALORIZACIÓN
    public byte[] generarReporteValorizacionPdf(List<ValorizacionInventarioDTO> datos) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 60);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new HeaderFooterPageEvent(empresaConfigService.obtenerConfiguracion().getNombreFantasia()));
            document.open();
            construirCabeceraEmpresa(document, "Valorización de Inventario");

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4f, 2f, 3f});
            table.setSpacingBefore(15);
            table.addCell(crearCeldaHeader("Categoría"));
            table.addCell(crearCeldaHeader("Unidades Físicas"));
            table.addCell(crearCeldaHeader("Valor Total Costo"));

            long totalUnidades = 0;
            BigDecimal totalGlobal = BigDecimal.ZERO;

            for (ValorizacionInventarioDTO d : datos) {
                table.addCell(new Paragraph(d.getCategoria(), FONT_NORMAL));
                table.addCell(crearCeldaDatoCenter(String.valueOf(d.getCantidadUnidades())));
                table.addCell(crearCeldaMoneda(d.getValorTotal()));

                totalUnidades += d.getCantidadUnidades();
                totalGlobal = totalGlobal.add(d.getValorTotal() != null ? d.getValorTotal() : BigDecimal.ZERO);
            }
            document.add(table);

            Paragraph pTot = new Paragraph("\nResumen: " + totalUnidades + " unidades | Capital Invertido: " + formatearMoneda(totalGlobal), FONT_TITULO);
            pTot.setAlignment(Element.ALIGN_RIGHT);
            document.add(pTot);
        } catch (Exception e) { throw new RuntimeException("Error PDF Valorizacion", e); } 
        finally { document.close(); }
        return baos.toByteArray();
    }

    // ✅ NUEVO: REPORTE STOCK INMOVILIZADO
    public byte[] generarReporteStockInmovilizadoPdf(List<StockInmovilizadoDTO> datos, int dias) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 60);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new HeaderFooterPageEvent(empresaConfigService.obtenerConfiguracion().getNombreFantasia()));
            document.open();
            construirCabeceraEmpresa(document, "Reporte de Stock Inmovilizado");
            
            Paragraph desc = new Paragraph("Productos sin ventas en los últimos " + dias + " días.", FONT_SUBTITULO);
            desc.setSpacingAfter(10);
            document.add(desc);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4f, 2f, 1f, 2f, 2f});
            table.addCell(crearCeldaHeader("Producto"));
            table.addCell(crearCeldaHeader("Categoría"));
            table.addCell(crearCeldaHeader("Stock"));
            table.addCell(crearCeldaHeader("Capital Parado"));
            table.addCell(crearCeldaHeader("Últ. Venta"));

            BigDecimal capitalTotal = BigDecimal.ZERO;
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            for (StockInmovilizadoDTO d : datos) {
                table.addCell(new Paragraph(d.getNombre(), FONT_NORMAL));
                table.addCell(new Paragraph(d.getCategoria(), FONT_NORMAL));
                table.addCell(crearCeldaDatoCenter(String.valueOf(d.getStockActual())));
                table.addCell(crearCeldaMoneda(d.getCapitalParado()));
                
                String fechaUltima = d.getUltimaVenta() != null ? d.getUltimaVenta().format(fmt) : "Nunca";
                table.addCell(crearCeldaDatoCenter(fechaUltima));

                capitalTotal = capitalTotal.add(d.getCapitalParado() != null ? d.getCapitalParado() : BigDecimal.ZERO);
            }
            document.add(table);

            Paragraph pTot = new Paragraph("\nCapital Total Parado: " + formatearMoneda(capitalTotal), FONT_TITULO);
            pTot.setAlignment(Element.ALIGN_RIGHT);
            document.add(pTot);
        } catch (Exception e) { throw new RuntimeException("Error PDF Inmovilizado", e); } 
        finally { document.close(); }
        return baos.toByteArray();
    }

    // ✅ NUEVO: REPORTE EVOLUCIÓN DE COSTOS
    public byte[] generarReporteEvolucionCostosPdf(List<VariacionCostoDTO> datos, String productoId) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 60);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new HeaderFooterPageEvent(empresaConfigService.obtenerConfiguracion().getNombreFantasia()));
            document.open();
            construirCabeceraEmpresa(document, "Evolución de Costos de Compra");
            
            // Si el nombre del producto viene en el primer elemento, lo mostramos
            String nombreProd = (!datos.isEmpty()) ? datos.get(0).getProducto() : "Producto " + productoId;
            Paragraph desc = new Paragraph("Historial de precios para: " + nombreProd, FONT_SUBTITULO);
            desc.setSpacingAfter(10);
            document.add(desc);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 4f, 2f, 2f});
            table.addCell(crearCeldaHeader("Fecha"));
            table.addCell(crearCeldaHeader("Proveedor"));
            table.addCell(crearCeldaHeader("Nro. Orden"));
            table.addCell(crearCeldaHeader("Costo Pagado"));

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (VariacionCostoDTO d : datos) {
                String fecha = d.getFechaCompra() != null ? d.getFechaCompra().format(fmt) : "N/A";
                table.addCell(crearCeldaDatoCenter(fecha));
                table.addCell(new Paragraph(d.getProveedor(), FONT_NORMAL));
                table.addCell(crearCeldaDatoCenter(d.getNroOrden()));
                table.addCell(crearCeldaMoneda(d.getCostoPagado()));
            }
            document.add(table);

        } catch (Exception e) { throw new RuntimeException("Error PDF Evolucion", e); } 
        finally { document.close(); }
        return baos.toByteArray();
    }

    // ========================================================================
    // HELPERS DE TABLA
    // ========================================================================
    private PdfPCell crearCeldaHeader(String texto) {
        PdfPCell cell = new PdfPCell(new Paragraph(texto, FONT_HEADER_TABLA));
        cell.setBackgroundColor(Color.DARK_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8);
        return cell;
    }

    private PdfPCell crearCeldaMoneda(BigDecimal valor) {
        PdfPCell cell = new PdfPCell(new Paragraph(formatearMoneda(valor), FONT_NORMAL));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        return cell;
    }

    private PdfPCell crearCeldaDatoCenter(String texto) {
        PdfPCell cell = new PdfPCell(new Paragraph(texto, FONT_NORMAL));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        return cell;
    }
}