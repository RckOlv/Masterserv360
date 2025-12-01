package com.masterserv.productos.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.masterserv.productos.entity.DetalleVenta;
import com.masterserv.productos.entity.Venta;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.awt.Color;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    // Definimos fuentes estáticas usando la Fábrica (Safe)
    private static final Font FONT_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.BLACK);
    private static final Font FONT_SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
    private static final Font FONT_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font FONT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font FONT_DATA_EMPRESA = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);

    public byte[] generarComprobanteVenta(Venta venta) {
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // --- 1. CABECERA DE LA EMPRESA ---
            Paragraph titulo = new Paragraph("MASTERSERV360", FONT_TITULO);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);

            Paragraph subtitulo = new Paragraph("Comprobante de Venta (No Fiscal)", FONT_SUBTITULO);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitulo);

            // --- MENTOR: AGREGADO EMAIL DE EMPRESA AQUÍ ---
            Paragraph datosEmpresa = new Paragraph(
                "Razón Social: Masterserv S.A.\n" + 
                "CUIT: 30-12345678-9\n" + 
                "Inicio de Actividades: 01/01/2020\n" +
                "Dirección: Av. San Martín 1234, El Soberbio, Misiones\n" +
                "Tel: (3755) 12-3456\n" + 
                "Email: contacto@masterserv360.com", // <--- NUEVO DATO
                FONT_DATA_EMPRESA
            );
            datosEmpresa.setAlignment(Element.ALIGN_CENTER);
            datosEmpresa.setSpacingAfter(20);
            document.add(datosEmpresa);

            LineSeparator separator = new LineSeparator();
            separator.setLineColor(Color.LIGHT_GRAY);
            document.add(separator);

            // --- 2. DATOS DE LA VENTA ---
            Paragraph infoVenta = new Paragraph();
            infoVenta.setSpacingBefore(15);
            infoVenta.setSpacingAfter(15);
            
            Font fontNroVenta = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
            infoVenta.add(new Chunk("Nº Venta: " + venta.getId() + "\n", fontNroVenta));
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            infoVenta.add(new Chunk("Fecha: " + venta.getFechaVenta().format(formatter) + "\n", FONT_NORMAL));
            
            if (venta.getCliente() != null) {
                infoVenta.add(new Chunk("Cliente: " + venta.getCliente().getNombre() + " " + venta.getCliente().getApellido() + "\n", FONT_NORMAL));
                if(venta.getCliente().getDocumento() != null) {
                    infoVenta.add(new Chunk("DNI/CUIT: " + venta.getCliente().getDocumento() + "\n", FONT_NORMAL));
                }
                // --- MENTOR: ELIMINADO EMAIL DEL CLIENTE (POR PEDIDO) ---
                // infoVenta.add(new Chunk("Email: " + venta.getCliente().getEmail() + "\n", FONT_NORMAL));
            }
            
            if (venta.getVendedor() != null) {
                infoVenta.add(new Chunk("Atendido por: " + venta.getVendedor().getNombre() + "\n", FONT_NORMAL));
            }
            
            document.add(infoVenta);

            // --- 3. TABLA DE PRODUCTOS ---
            PdfPTable table = new PdfPTable(4); 
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 3f, 1f, 1.5f, 1.5f });
            table.setSpacingBefore(10f);

            table.addCell(crearCeldaHeader("Producto"));
            table.addCell(crearCeldaHeader("Cant."));
            table.addCell(crearCeldaHeader("Precio Unit."));
            table.addCell(crearCeldaHeader("Subtotal"));

            BigDecimal subtotalSinDescuento = BigDecimal.ZERO;

            for (DetalleVenta detalle : venta.getDetalles()) {
                // Producto
                PdfPCell cellProd = new PdfPCell(new Paragraph(detalle.getProducto().getNombre(), FONT_NORMAL));
                cellProd.setPadding(5);
                table.addCell(cellProd);
                
                // Cantidad (Centrada)
                PdfPCell cellCant = new PdfPCell(new Paragraph(String.valueOf(detalle.getCantidad()), FONT_NORMAL));
                cellCant.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellCant.setPadding(5);
                table.addCell(cellCant);
                
                // Precio Unitario (Derecha)
                PdfPCell cellPrecio = new PdfPCell(new Paragraph(String.format("$%.2f", detalle.getPrecioUnitario()), FONT_NORMAL));
                cellPrecio.setHorizontalAlignment(Element.ALIGN_RIGHT);
                cellPrecio.setPadding(5);
                table.addCell(cellPrecio);
                
                // Subtotal Item
                BigDecimal subtotalItem = detalle.getPrecioUnitario().multiply(new BigDecimal(detalle.getCantidad()));
                subtotalSinDescuento = subtotalSinDescuento.add(subtotalItem);
                
                PdfPCell cellSub = new PdfPCell(new Paragraph(String.format("$%.2f", subtotalItem), FONT_NORMAL));
                cellSub.setHorizontalAlignment(Element.ALIGN_RIGHT);
                cellSub.setPadding(5);
                table.addCell(cellSub);
            }

            document.add(table);

            // --- 4. TOTALES Y DESCUENTOS ---
            Paragraph totales = new Paragraph();
            totales.setAlignment(Element.ALIGN_RIGHT);
            totales.setSpacingBefore(15);

            // Cálculo del descuento
            BigDecimal descuento = subtotalSinDescuento.subtract(venta.getTotalVenta());
            
            if (descuento.compareTo(BigDecimal.ZERO) > 0) {
                totales.add(new Chunk("Subtotal: $" + String.format("%.2f", subtotalSinDescuento) + "\n", FONT_NORMAL));
                
                String cuponTexto = (venta.getCupon() != null) ? " (" + venta.getCupon().getCodigo() + ")" : "";
                
                // Descuento en Rojo (Usamos FontFactory)
                Font fontRojo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.RED);
                totales.add(new Chunk("Descuento aplicado" + cuponTexto + ": -$" + String.format("%.2f", descuento) + "\n", fontRojo));
            }

            // Total Final en Grande (Usamos FontFactory)
            Font fontTotal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            totales.add(new Chunk("TOTAL: $" + String.format("%.2f", venta.getTotalVenta()), fontTotal));
            
            document.add(totales);

        } catch (DocumentException e) {
            System.err.println("Error al generar PDF: " + e.getMessage());
            throw new RuntimeException("Error al generar el comprobante PDF", e);
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    private PdfPCell crearCeldaHeader(String texto) {
        PdfPCell cell = new PdfPCell(new Paragraph(texto, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
        cell.setBackgroundColor(Color.DARK_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        return cell;
    }
}