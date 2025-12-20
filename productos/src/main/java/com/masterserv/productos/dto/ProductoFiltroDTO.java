package com.masterserv.productos.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List; // <--- No olvides importar List

/**
 * DTO para recibir los criterios de filtrado de productos.
 * Actualizado para soportar filtros del catálogo público.
 */
@Data
public class ProductoFiltroDTO {
    
    // --- Campos Originales (Admin) ---
    private String nombre;
    private String codigo;
    private Long categoriaId;       // Para filtro simple (Admin)
    private BigDecimal precioMax;
    private Boolean conStock;       // Nombre usado en Admin
    private String estado;
    private String estadoStock;

    // --- NUEVOS CAMPOS (Agregados para compatibilidad con Catálogo Público) ---
    
    private List<Long> categoriaIds; // <--- ESTE ES EL QUE CAUSA EL ERROR 500
    private BigDecimal precioMin;    // <--- También te va a faltar si filtras por rango
    private Boolean soloConStock;    // <--- Alias para el catálogo público

    // Nota: Lombok (@Data) generará los getters y setters automáticamente.
}