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
import com.masterserv.productos.entity.EmpresaConfig;
import com.masterserv.productos.entity.Pedido;
import com.masterserv.productos.entity.Venta;

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

    // 🔥 INYECCIÓN NUEVA: Para obtener datos dinámicos
    @Autowired
    private EmpresaConfigService empresaConfigService;

    // ========================================================================
    // MÉTODO AUXILIAR PARA FORMATEO DE MONEDA (ARGENTINA)
    // ========================================================================
    private String formatearMoneda(BigDecimal valor) {
        if (valor == null) valor = BigDecimal.ZERO;
        
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
        simbolos.setGroupingSeparator('.'); 
        simbolos.setDecimalSeparator(','); 
        
        DecimalFormat df = new DecimalFormat("#,##0.00", simbolos);
        return "$ " + df.format(valor);
    }

    // ========================================================================
    // MÉTODO AUXILIAR PARA CABECERA DINÁMICA (REUTILIZABLE)
    // ========================================================================
    private void construirCabeceraEmpresa(Document document, String subtituloReporte) throws DocumentException {
        // 1. Obtener configuración de la DB
        EmpresaConfig config = empresaConfigService.obtenerConfiguracion();

        // 2. LOGO (Soporte Híbrido: URL o Base64)
        if (config.getLogoUrl() != null && !config.getLogoUrl().isEmpty()) {
            try {
                Image logo = null;
                String logoData = config.getLogoUrl();

                if (logoData.startsWith("http")) {
                    // CASO A: Es una URL (ej: una imagen de internet)
                    logo = Image.getInstance(logoData);
                } else if (logoData.contains("base64,")) {
                    // CASO B: Es Base64 (Guardado en BD)
                    // Limpiamos el encabezado "data:image/png;base64,"
                    String base64String = logoData.split(",")[1];
                    // Decodificamos a bytes
                    byte[] imageBytes = Base64.getDecoder().decode(base64String);
                    logo = Image.getInstance(imageBytes);
                }

                if (logo != null) {
                    logo.scaleToFit(120, 60); 
                    logo.setAlignment(Element.ALIGN_CENTER);
                    logo.setSpacingAfter(10);
                    document.add(logo);
                }
            } catch (Exception e) {
                // Logueamos error pero no rompemos el PDF
                System.err.println("No se pudo cargar el logo en el PDF: " + e.getMessage());
            }
        }

        // 3. Título (Nombre Fantasía)
        String tituloTexto = (config.getNombreFantasia() != null) ? config.getNombreFantasia() : "EMPRESA SIN NOMBRE";
        Paragraph titulo = new Paragraph(tituloTexto.toUpperCase(), FONT_TITULO);
        titulo.setAlignment(Element.ALIGN_CENTER);
        document.add(titulo);

        // 4. Subtítulo del Reporte (Ej: "Orden de Compra")
        Paragraph subtitulo = new Paragraph(subtituloReporte, FONT_SUBTITULO);
        subtitulo.setAlignment(Element.ALIGN_CENTER);
        document.add(subtitulo);

        // 5. Datos de la Empresa Dinámicos
        StringBuilder datosSb = new StringBuilder();
        if (config.getRazonSocial() != null) datosSb.append("Razón Social: ").append(config.getRazonSocial()).append("\n");
        if (config.getCuit() != null) datosSb.append("CUIT: ").append(config.getCuit()).append("\n");
        if (config.getDireccion() != null) datosSb.append("Dirección: ").append(config.getDireccion()).append("\n");
        
        String contacto = "";
        if (config.getTelefono() != null) contacto += "Tel: " + config.getTelefono() + "  ";
        if (config.getEmailContacto() != null) contacto += "Email: " + config.getEmailContacto();
        if (!contacto.isEmpty()) datosSb.append(contacto).append("\n");
        
        if (config.getSitioWeb() != null) datosSb.append("Web: ").append(config.getSitioWeb());

        Paragraph datosEmpresa = new Paragraph(datosSb.toString(), FONT_DATA_EMPRESA);
        datosEmpresa.setAlignment(Element.ALIGN_CENTER);
        datosEmpresa.setSpacingAfter(10);
        document.add(datosEmpresa);

        document.add(new LineSeparator());
    }

    // ========================================================================
    // 1. COMPROBANTE INTERNO (CON PRECIOS) - PARA EL ADMIN
    // ========================================================================
    public byte[] generarComprobantePedido(Pedido pedido) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // 🔥 CABECERA DINÁMICA
            construirCabeceraEmpresa(document, "Orden de Compra (Interno)");

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
                PdfPCell cellPrecio = new PdfPCell(new Paragraph(formatearMoneda(precio), FONT_NORMAL));
                cellPrecio.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cellPrecio);
                
                BigDecimal subtotalItem = precio.multiply(new BigDecimal(detalle.getCantidad()));
                totalCalculado = totalCalculado.add(subtotalItem);
                
                PdfPCell cellSub = new PdfPCell(new Paragraph(formatearMoneda(subtotalItem), FONT_NORMAL));
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
            
            totales.add(new Chunk("TOTAL: " + formatearMoneda(totalFinal), fontTotal));
            
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

            // 🔥 CABECERA DINÁMICA
            construirCabeceraEmpresa(document, "Orden de Compra / Solicitud de Mercadería");

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

            PdfPTable table = new PdfPTable(2); 
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 4f, 1f });
            table.setSpacingBefore(10f);

            table.addCell(crearCeldaHeader("Descripción del Producto"));
            table.addCell(crearCeldaHeader("Cantidad"));

            for (DetallePedido detalle : pedido.getDetalles()) {
                PdfPCell cellProd = new PdfPCell(new Paragraph(detalle.getProducto().getNombre(), FONT_NORMAL));
                cellProd.setPadding(5);
                table.addCell(cellProd);
                
                PdfPCell cellCant = new PdfPCell(new Paragraph(String.valueOf(detalle.getCantidad()), FONT_NORMAL));
                cellCant.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellCant.setPadding(5);
                table.addCell(cellCant);
            }

            document.add(table);

            // Obtener configuración para pie de página legal si fuera necesario
            EmpresaConfig config = empresaConfigService.obtenerConfiguracion();
            String textoPie = (config.getPiePaginaPresupuesto() != null && !config.getPiePaginaPresupuesto().isEmpty()) 
                            ? config.getPiePaginaPresupuesto() 
                            : "Por favor confirmar recepción, disponibilidad y fecha estimada de entrega.";

            Paragraph pie = new Paragraph("\nNota: " + textoPie, FONT_DATA_EMPRESA);
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

            // 🔥 CABECERA DINÁMICA
            construirCabeceraEmpresa(document, "Comprobante de Venta (No Fiscal)");

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
                
                PdfPCell cellPrecio = new PdfPCell(new Paragraph(formatearMoneda(detalle.getPrecioUnitario()), FONT_NORMAL));
                cellPrecio.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cellPrecio);
                
                BigDecimal subtotalItem = detalle.getPrecioUnitario().multiply(new BigDecimal(detalle.getCantidad()));
                subtotalSinDescuento = subtotalSinDescuento.add(subtotalItem);
                
                PdfPCell cellSub = new PdfPCell(new Paragraph(formatearMoneda(subtotalItem), FONT_NORMAL));
                cellSub.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cellSub);
            }

            document.add(table);

            Paragraph totales = new Paragraph();
            totales.setAlignment(Element.ALIGN_RIGHT);
            totales.setSpacingBefore(15);

            BigDecimal descuento = subtotalSinDescuento.subtract(venta.getTotalVenta());
            
            if (descuento.compareTo(BigDecimal.ZERO) > 0) {
                totales.add(new Chunk("Subtotal: " + formatearMoneda(subtotalSinDescuento) + "\n", FONT_NORMAL));
                Font fontRojo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.RED);
                totales.add(new Chunk("Descuento: -" + formatearMoneda(descuento) + "\n", fontRojo));
            }

            Font fontTotal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
            totales.add(new Chunk("TOTAL: " + formatearMoneda(venta.getTotalVenta()), fontTotal));
            
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

            // 🔥 CABECERA DINÁMICA
            construirCabeceraEmpresa(document, "Reporte de Gestión");

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
            PdfPCell cellVentas = new PdfPCell(new Paragraph(formatearMoneda(stats.getTotalVentasMes()), FONT_NORMAL));
            cellVentas.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellVentas.setPadding(8);
            tableMetrics.addCell(cellVentas);

            // 2. Cantidad de Ventas (#)
            PdfPCell cellCant = new PdfPCell(new Paragraph(String.valueOf(stats.getCantidadVentasPeriodo()), FONT_NORMAL));
            cellCant.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellCant.setPadding(8);
            tableMetrics.addCell(cellCant);

            // 3. Ticket Promedio ($)
            BigDecimal ticketPromedio = BigDecimal.ZERO;
            if (stats.getCantidadVentasPeriodo() > 0) {
                ticketPromedio = stats.getTotalVentasMes()
                    .divide(BigDecimal.valueOf(stats.getCantidadVentasPeriodo()), 2, RoundingMode.HALF_UP);
            }

            PdfPCell cellTicket = new PdfPCell(new Paragraph(formatearMoneda(ticketPromedio), FONT_NORMAL));
            cellTicket.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellTicket.setPadding(8);
            tableMetrics.addCell(cellTicket);
            
            document.add(tableMetrics);

            // --- TOP PRODUCTOS ---
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