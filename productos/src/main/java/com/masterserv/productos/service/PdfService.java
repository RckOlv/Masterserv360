package com.masterserv.productos.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.masterserv.productos.dto.DashboardFilterDTO;
import com.masterserv.productos.dto.DashboardStatsDTO;
import com.masterserv.productos.dto.TopProductoDTO;
import com.masterserv.productos.entity.DetallePedido;
import com.masterserv.productos.entity.DetalleVenta;
import com.masterserv.productos.entity.Pedido;
import com.masterserv.productos.entity.Venta;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode; // <--- Importante para calcular el promedio
import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

@Service
public class PdfService {

    // Fuentes
    private static final Font FONT_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
    private static final Font FONT_SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
    private static final Font FONT_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font FONT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font FONT_DATA_EMPRESA = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);
    private static final Font FONT_HEADER_TABLA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Font FONT_LEYENDA = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.DARK_GRAY);

    @Autowired 
    private DashboardService dashboardService;

    // ========================================================================
    // 1. COMPROBANTE INTERNO (CON PRECIOS) - PARA EL ADMIN
    // ========================================================================
    public byte[] generarComprobantePedido(Pedido pedido) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // CABECERA
            Paragraph titulo = new Paragraph("MASTERSERV360", FONT_TITULO);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);

            Paragraph subtitulo = new Paragraph("Orden de Compra (Interno)", FONT_SUBTITULO);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitulo);

            Paragraph datosEmpresa = new Paragraph(
                "Razón Social: Masterserv S.A.\n" + 
                "Dirección: Av. San Martín 1234, El Soberbio, Misiones\n" +
                "Email: contacto@masterserv360.com", 
                FONT_DATA_EMPRESA
            );
            datosEmpresa.setAlignment(Element.ALIGN_CENTER);
            datosEmpresa.setSpacingAfter(10);
            document.add(datosEmpresa);

            document.add(new LineSeparator());

            // DATOS PEDIDO Y PROVEEDOR
            Paragraph infoPedido = new Paragraph();
            infoPedido.setSpacingBefore(10);
            infoPedido.setSpacingAfter(10);
            
            infoPedido.add(new Chunk("Nº Pedido: " + pedido.getId() + "\n", FONT_BOLD));
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            infoPedido.add(new Chunk("Fecha Emisión: " + pedido.getFechaPedido().format(formatter) + "\n", FONT_NORMAL));
            infoPedido.add(new Chunk("Estado: " + pedido.getEstado() + "\n\n", FONT_NORMAL));
            
            if (pedido.getProveedor() != null) {
                infoPedido.add(new Chunk("PROVEEDOR:\n", FONT_BOLD));
                infoPedido.add(new Chunk("Razón Social: " + pedido.getProveedor().getRazonSocial() + "\n", FONT_NORMAL));
                infoPedido.add(new Chunk("CUIT: " + pedido.getProveedor().getCuit() + "\n", FONT_NORMAL));
            }
            
            document.add(infoPedido);

            // TABLA CON PRECIOS (4 Columnas)
            PdfPTable table = new PdfPTable(4); 
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 4f, 1f, 2f, 2f });
            table.setSpacingBefore(10f);

            table.addCell(crearCeldaHeader("Producto"));
            table.addCell(crearCeldaHeader("Cant."));
            table.addCell(crearCeldaHeader("Costo Unit."));
            table.addCell(crearCeldaHeader("Subtotal"));

            BigDecimal totalCalculado = BigDecimal.ZERO;

            for (DetallePedido detalle : pedido.getDetalles()) {
                table.addCell(new Paragraph(detalle.getProducto().getNombre(), FONT_NORMAL));
                
                PdfPCell cellCant = new PdfPCell(new Paragraph(String.valueOf(detalle.getCantidad()), FONT_NORMAL));
                cellCant.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cellCant);
                
                BigDecimal precio = (detalle.getPrecioUnitario() != null) ? detalle.getPrecioUnitario() : BigDecimal.ZERO;
                PdfPCell cellPrecio = new PdfPCell(new Paragraph(String.format("$%.2f", precio), FONT_NORMAL));
                cellPrecio.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cellPrecio);
                
                BigDecimal subtotalItem = precio.multiply(new BigDecimal(detalle.getCantidad()));
                totalCalculado = totalCalculado.add(subtotalItem);
                
                PdfPCell cellSub = new PdfPCell(new Paragraph(String.format("$%.2f", subtotalItem), FONT_NORMAL));
                cellSub.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cellSub);
            }

            document.add(table);

            // TOTALES
            Paragraph totales = new Paragraph();
            totales.setAlignment(Element.ALIGN_RIGHT);
            totales.setSpacingBefore(15);

            Font fontTotal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
            BigDecimal totalFinal = (pedido.getTotalPedido() != null) ? pedido.getTotalPedido() : totalCalculado;
            totales.add(new Chunk("TOTAL: $" + String.format("%.2f", totalFinal), fontTotal));
            
            document.add(totales);

        } catch (DocumentException e) {
            throw new RuntimeException("Error al generar el comprobante de pedido PDF", e);
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    // ========================================================================
    // 2. ORDEN DE COMPRA EXTERNA (SIN PRECIOS) - PARA EL PROVEEDOR
    // ========================================================================
    public byte[] generarOrdenCompraProveedor(Pedido pedido) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // CABECERA
            Paragraph titulo = new Paragraph("MASTERSERV360", FONT_TITULO);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);

            Paragraph subtitulo = new Paragraph("Orden de Compra / Solicitud de Mercadería", FONT_SUBTITULO);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitulo);

            Paragraph datosEmpresa = new Paragraph(
                "Razón Social: Masterserv S.A.\n" + 
                "Dirección: Av. San Martín 1234, El Soberbio, Misiones\n" +
                "Email: compras@masterserv360.com", 
                FONT_DATA_EMPRESA
            );
            datosEmpresa.setAlignment(Element.ALIGN_CENTER);
            datosEmpresa.setSpacingAfter(10);
            document.add(datosEmpresa);

            document.add(new LineSeparator());

            // DATOS DE LA ORDEN
            Paragraph infoPedido = new Paragraph();
            infoPedido.setSpacingBefore(10);
            infoPedido.setSpacingAfter(10);
            
            infoPedido.add(new Chunk("Orden de Compra Nº: " + pedido.getId() + "\n", FONT_BOLD));
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            infoPedido.add(new Chunk("Fecha Emisión: " + pedido.getFechaPedido().format(formatter) + "\n\n", FONT_NORMAL));
            
            if (pedido.getProveedor() != null) {
                infoPedido.add(new Chunk("SEÑORES PROVEEDORES:\n", FONT_BOLD));
                infoPedido.add(new Chunk("Razón Social: " + pedido.getProveedor().getRazonSocial() + "\n", FONT_NORMAL));
                infoPedido.add(new Chunk("CUIT: " + pedido.getProveedor().getCuit() + "\n", FONT_NORMAL));
            }
            
            document.add(infoPedido);

            // --- TABLA LIMPIA (SOLO 2 COLUMNAS: PRODUCTO Y CANTIDAD) ---
            PdfPTable table = new PdfPTable(2); 
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 4f, 1f }); // 80% Nombre, 20% Cantidad
            table.setSpacingBefore(10f);

            // Encabezados
            table.addCell(crearCeldaHeader("Descripción del Producto"));
            table.addCell(crearCeldaHeader("Cantidad"));

            // Llenado de filas (SIN PRECIOS)
            for (DetallePedido detalle : pedido.getDetalles()) {
                // Producto
                PdfPCell cellProd = new PdfPCell(new Paragraph(detalle.getProducto().getNombre(), FONT_NORMAL));
                cellProd.setPadding(5);
                table.addCell(cellProd);
                
                // Cantidad
                PdfPCell cellCant = new PdfPCell(new Paragraph(String.valueOf(detalle.getCantidad()), FONT_NORMAL));
                cellCant.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellCant.setPadding(5);
                table.addCell(cellCant);
            }

            document.add(table);

            // Pie de página simple (SIN TOTALES)
            Paragraph pie = new Paragraph("\nNota: Por favor confirmar recepción, disponibilidad y fecha estimada de entrega.", FONT_DATA_EMPRESA);
            pie.setAlignment(Element.ALIGN_CENTER);
            document.add(pie);

        } catch (DocumentException e) {
            throw new RuntimeException("Error al generar la Orden de Compra PDF para proveedor", e);
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    // ========================================================================
    // 3. COMPROBANTE DE VENTA
    // ========================================================================
    public byte[] generarComprobanteVenta(Venta venta) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            Paragraph titulo = new Paragraph("MASTERSERV360", FONT_TITULO);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);

            Paragraph subtitulo = new Paragraph("Comprobante de Venta (No Fiscal)", FONT_SUBTITULO);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitulo);

            Paragraph datosEmpresa = new Paragraph(
                "Razón Social: Masterserv S.A.\n" + 
                "CUIT: 30-12345678-9\n" + 
                "Dirección: Av. San Martín 1234, El Soberbio, Misiones\n" +
                "Email: contacto@masterserv360.com", 
                FONT_DATA_EMPRESA
            );
            datosEmpresa.setAlignment(Element.ALIGN_CENTER);
            datosEmpresa.setSpacingAfter(10);
            document.add(datosEmpresa);

            document.add(new LineSeparator());

            Paragraph infoVenta = new Paragraph();
            infoVenta.setSpacingBefore(10);
            infoVenta.setSpacingAfter(10);
            
            infoVenta.add(new Chunk("Nº Venta: " + venta.getId() + "\n", FONT_BOLD));
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            infoVenta.add(new Chunk("Fecha: " + venta.getFechaVenta().format(formatter) + "\n", FONT_NORMAL));
            
            if (venta.getCliente() != null) {
                infoVenta.add(new Chunk("Cliente: " + venta.getCliente().getNombre() + " " + venta.getCliente().getApellido() + "\n", FONT_NORMAL));
                if(venta.getCliente().getDocumento() != null) {
                    infoVenta.add(new Chunk("DNI/CUIT: " + venta.getCliente().getDocumento() + "\n", FONT_NORMAL));
                }
            }
            
            document.add(infoVenta);

            PdfPTable table = new PdfPTable(4); 
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 4f, 1f, 2f, 2f });
            table.setSpacingBefore(10f);

            table.addCell(crearCeldaHeader("Producto"));
            table.addCell(crearCeldaHeader("Cant."));
            table.addCell(crearCeldaHeader("Precio Unit."));
            table.addCell(crearCeldaHeader("Subtotal"));

            BigDecimal subtotalSinDescuento = BigDecimal.ZERO;

            for (DetalleVenta detalle : venta.getDetalles()) {
                table.addCell(new Paragraph(detalle.getProducto().getNombre(), FONT_NORMAL));
                
                PdfPCell cellCant = new PdfPCell(new Paragraph(String.valueOf(detalle.getCantidad()), FONT_NORMAL));
                cellCant.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cellCant);
                
                PdfPCell cellPrecio = new PdfPCell(new Paragraph(String.format("$%.2f", detalle.getPrecioUnitario()), FONT_NORMAL));
                cellPrecio.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cellPrecio);
                
                BigDecimal subtotalItem = detalle.getPrecioUnitario().multiply(new BigDecimal(detalle.getCantidad()));
                subtotalSinDescuento = subtotalSinDescuento.add(subtotalItem);
                
                PdfPCell cellSub = new PdfPCell(new Paragraph(String.format("$%.2f", subtotalItem), FONT_NORMAL));
                cellSub.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cellSub);
            }

            document.add(table);

            Paragraph totales = new Paragraph();
            totales.setAlignment(Element.ALIGN_RIGHT);
            totales.setSpacingBefore(15);

            BigDecimal descuento = subtotalSinDescuento.subtract(venta.getTotalVenta());
            
            if (descuento.compareTo(BigDecimal.ZERO) > 0) {
                totales.add(new Chunk("Subtotal: $" + String.format("%.2f", subtotalSinDescuento) + "\n", FONT_NORMAL));
                Font fontRojo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.RED);
                totales.add(new Chunk("Descuento: -$" + String.format("%.2f", descuento) + "\n", fontRojo));
            }

            Font fontTotal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
            totales.add(new Chunk("TOTAL: $" + String.format("%.2f", venta.getTotalVenta()), fontTotal));
            
            document.add(totales);

        } catch (DocumentException e) {
            throw new RuntimeException("Error al generar el comprobante PDF", e);
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    // ========================================================================
    // 4. REPORTE DASHBOARD (ACTUALIZADO: MÉTRICAS ÚTILES)
    // ========================================================================
    public byte[] generarReporteDashboard(DashboardFilterDTO filtro) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            Paragraph titulo = new Paragraph("Reporte de Gestión - Masterserv360", FONT_TITULO);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            
            String inicioStr = (filtro.getFechaInicio() != null) ? filtro.getFechaInicio().format(fmt) : "Inicio";
            String finStr = (filtro.getFechaFin() != null) ? filtro.getFechaFin().format(fmt) : "Hoy";
            
            Paragraph contexto = new Paragraph();
            contexto.setAlignment(Element.ALIGN_CENTER);
            contexto.setSpacingAfter(20);
            
            contexto.add(new Chunk("Periodo: " + inicioStr + " al " + finStr + "\n", FONT_SUBTITULO));
            
            if (filtro.getGeneradoPor() != null) {
                contexto.add(new Chunk("Generado por: " + filtro.getGeneradoPor(), FONT_DATA_EMPRESA));
            }
            
            document.add(contexto);

            // --- GRÁFICO (IMAGEN) ---
            if (filtro.getGraficoBase64() != null && !filtro.getGraficoBase64().isEmpty()) {
                try {
                    String base64Image = filtro.getGraficoBase64().split(",")[1];
                    byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                    
                    Image grafico = Image.getInstance(imageBytes);
                    grafico.setAlignment(Element.ALIGN_CENTER);
                    grafico.scaleToFit(500, 250); 
                    grafico.setSpacingAfter(5);
                    document.add(grafico);

                    Paragraph leyenda = new Paragraph(
                        "Gráfico: Evolución de ventas diarias (en pesos) durante el periodo seleccionado.", 
                        FONT_LEYENDA
                    );
                    leyenda.setAlignment(Element.ALIGN_CENTER);
                    leyenda.setSpacingAfter(20);
                    document.add(leyenda);

                } catch (Exception e) {
                    System.err.println("Error al procesar imagen del gráfico: " + e.getMessage());
                }
            }

            // --- OBTENCIÓN DE DATOS ---
            DashboardStatsDTO stats = dashboardService.getEstadisticasFiltradas(filtro.getFechaInicio(), filtro.getFechaFin());
            List<TopProductoDTO> top = dashboardService.getTopProductosPorRango(filtro.getFechaInicio(), filtro.getFechaFin());

            // --- TABLA DE MÉTRICAS REDISEÑADA ---
            PdfPTable tableMetrics = new PdfPTable(3);
            tableMetrics.setWidthPercentage(100);
            tableMetrics.setSpacingAfter(20);

            // Encabezados
            tableMetrics.addCell(crearCeldaHeader("Ventas Totales"));
            tableMetrics.addCell(crearCeldaHeader("Cant. Transacciones"));
            tableMetrics.addCell(crearCeldaHeader("Ticket Promedio"));
            
            // 1. Total Ventas ($)
            PdfPCell cellVentas = new PdfPCell(new Paragraph("$ " + String.format("%.2f", stats.getTotalVentasMes()), FONT_NORMAL));
            cellVentas.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellVentas.setPadding(8);
            tableMetrics.addCell(cellVentas);

            // 2. Cantidad de Ventas (#) - Ahora muestra transacciones reales
            PdfPCell cellCant = new PdfPCell(new Paragraph(String.valueOf(stats.getCantidadVentasPeriodo()), FONT_NORMAL));
            cellCant.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellCant.setPadding(8);
            tableMetrics.addCell(cellCant);

            // 3. Ticket Promedio ($) - Cálculo seguro
            BigDecimal ticketPromedio = BigDecimal.ZERO;
            if (stats.getCantidadVentasPeriodo() > 0) {
                ticketPromedio = stats.getTotalVentasMes()
                    .divide(BigDecimal.valueOf(stats.getCantidadVentasPeriodo()), 2, RoundingMode.HALF_UP);
            }

            PdfPCell cellTicket = new PdfPCell(new Paragraph("$ " + String.format("%.2f", ticketPromedio), FONT_NORMAL));
            cellTicket.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellTicket.setPadding(8);
            tableMetrics.addCell(cellTicket);
            
            document.add(tableMetrics);

            // --- TOP PRODUCTOS (Sin cambios, es útil) ---
            Paragraph subtituloTop = new Paragraph("Top Productos Vendidos", FONT_SUBTITULO);
            subtituloTop.setSpacingAfter(10);
            document.add(subtituloTop);

            PdfPTable tableTop = new PdfPTable(2);
            tableTop.setWidthPercentage(100);
            tableTop.setWidths(new float[] { 3f, 1f });

            tableTop.addCell(crearCeldaHeader("Producto"));
            tableTop.addCell(crearCeldaHeader("Cantidad Vendida"));

            for (TopProductoDTO p : top) {
                PdfPCell cellNombre = new PdfPCell(new Paragraph(p.getNombre(), FONT_NORMAL));
                cellNombre.setPadding(5);
                tableTop.addCell(cellNombre);

                PdfPCell cellCantProd = new PdfPCell(new Paragraph(String.valueOf(p.getCantidadVendida()), FONT_NORMAL));
                cellCantProd.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellCantProd.setPadding(5);
                tableTop.addCell(cellCantProd);
            }
            document.add(tableTop);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF reporte: " + e.getMessage(), e);
        }
    }

    private PdfPCell crearCeldaHeader(String texto) {
        PdfPCell cell = new PdfPCell(new Paragraph(texto, FONT_HEADER_TABLA));
        cell.setBackgroundColor(Color.DARK_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8);
        return cell;
    }
}