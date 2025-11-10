package com.masterserv.productos.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO para exponer la información de un Producto al público (Portal de Cliente / Chatbot).
 * * IMPORTANTE: Este DTO NO DEBE contener campos sensibles como
 * 'precioCosto' o información completa del 'proveedor'.
 */
@Data
public class ProductoPublicoDTO {

    private Long id;
    private String nombre;
    private String descripcion;

    /**
     * El precio de VENTA al público.
     */
    private BigDecimal precioVenta;

    /**
     * El stock disponible.
     * (Nota: Algunos negocios prefieren un boolean 'disponible' en lugar del número exacto.
     * Para una tienda de repuestos, mostrar el número ("Quedan 2") es bueno).
     */
    private Integer stockActual;

    /**
     * El nombre de la categoría (aplanado desde la entidad Categoria).
     */
    private String nombreCategoria;

    /**
     * La URL de la imagen principal del producto.
     * (Asegúrate de que tu entidad Producto tenga un campo 'imagenUrl' o similar).
     */
    private String imagenUrl;
}