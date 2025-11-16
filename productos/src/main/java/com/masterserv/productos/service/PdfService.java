package com.masterserv.productos.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.masterserv.productos.entity.DetalleVenta;
import com.masterserv.productos.entity.Venta;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.awt.Color;
import java.time.format.DateTimeFormatter;

/**
 * Servicio dedicado a la generación de documentos PDF.
 * Utiliza la librería OpenPDF (com.lowagie.text).
 */
@Service
public class PdfService {

    // Define las fuentes que usaremos
    private static final Font FONT_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
    private static final Font FONT_SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
    private static final Font FONT_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font FONT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

    /**
     * Genera un comprobante de venta en formato PDF y lo devuelve como un array de bytes.
     *
     * @param venta La entidad Venta (completa, con cliente, vendedor y detalles)
     * @return un byte[] que representa el archivo PDF.
     */
    public byte[] generarComprobanteVenta(Venta venta) {
        
        // 1. Usamos un ByteArrayOutputStream para escribir el PDF en memoria, no en un archivo
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // --- Encabezado ---
            Paragraph titulo = new Paragraph("Masterserv360", FONT_TITULO);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);

            Paragraph subtitulo = new Paragraph("Comprobante de Venta (No Fiscal)", FONT_SUBTITULO);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            subtitulo.setSpacingAfter(10);
            document.add(subtitulo);

            // --- Datos de la Venta ---
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            document.add(new Paragraph("Venta ID: #" + venta.getId(), FONT_NORMAL));
            document.add(new Paragraph("Fecha: " + venta.getFechaVenta().format(formatter), FONT_NORMAL));
            
            // Datos del Cliente (¡Asegúrate de que 'cliente' no sea nulo!)
            if (venta.getCliente() != null) {
                document.add(new Paragraph("Cliente: " + venta.getCliente().getNombre() + " " + venta.getCliente().getApellido(), FONT_NORMAL));
                document.add(new Paragraph("Email: " + venta.getCliente().getEmail(), FONT_NORMAL));
            }
            
            // Datos del Vendedor
            if (venta.getVendedor() != null) {
                document.add(new Paragraph("Atendido por: " + venta.getVendedor().getNombre(), FONT_NORMAL));
            }
            
            document.add(Chunk.NEWLINE); // Espacio

            // --- Tabla de Detalles ---
            PdfPTable table = new PdfPTable(4); // 4 columnas
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 3f, 1f, 1.5f, 1.5f }); // Ancho de columnas

            // Encabezados de la tabla
            table.addCell(crearCeldaHeader("Producto"));
            table.addCell(crearCeldaHeader("Cant."));
            table.addCell(crearCeldaHeader("Precio Unit."));
            table.addCell(crearCeldaHeader("Subtotal"));

            // Contenido de la tabla (los items)
            for (DetalleVenta detalle : venta.getDetalles()) {
                table.addCell(new Paragraph(detalle.getProducto().getNombre(), FONT_NORMAL));
                table.addCell(new Paragraph(String.valueOf(detalle.getCantidad()), FONT_NORMAL));
                table.addCell(new Paragraph(String.format("$%.2f", detalle.getPrecioUnitario()), FONT_NORMAL));
                
                // Calculamos el subtotal del item
                BigDecimal subtotalItem = detalle.getPrecioUnitario().multiply(new BigDecimal(detalle.getCantidad()));
                table.addCell(new Paragraph(String.format("$%.2f", subtotalItem), FONT_NORMAL));
            }

            document.add(table);
            document.add(Chunk.NEWLINE);

            // --- Total ---
            Paragraph total = new Paragraph("TOTAL: $" + String.format("%.2f", venta.getTotalVenta()), FONT_TITULO);
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);
            
            // (Aquí podrías añadir si se usó un cupón)
            if (venta.getCupon() != null) {
                Paragraph cuponInfo = new Paragraph("Descuento aplicado: " + venta.getCupon().getCodigo(), FONT_BOLD);
                cuponInfo.setAlignment(Element.ALIGN_RIGHT);
                document.add(cuponInfo);
            }


        } catch (DocumentException e) {
            // En un caso real, loguearíamos este error
            System.err.println("Error al generar PDF: " + e.getMessage());
            throw new RuntimeException("Error al generar el comprobante PDF", e);
        } finally {
            document.close();
        }

        // 2. Devolvemos los bytes del PDF que está en memoria
        return baos.toByteArray();
    }

    /**
     * Método helper para crear celdas de encabezado de tabla bonitas
     */
    private PdfPCell crearCeldaHeader(String texto) {
        PdfPCell cell = new PdfPCell(new Paragraph(texto, FONT_BOLD));
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        return cell;
    }
}