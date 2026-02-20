package com.masterserv.productos.dto;

import com.masterserv.productos.enums.EstadoVenta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class VentaDTO {

    // --- Campos de Lectura (Para respuestas GET) ---
    private Long id;
    private LocalDateTime fechaVenta;
    private EstadoVenta estado;
    private BigDecimal totalVenta;
    
    private Long clienteId;
    private String clienteNombre; 
    
    private Long vendedorId;     
    private String vendedorNombre;
    // -------------------------------
    
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;

    private String codigoCupon;      
    private BigDecimal montoDescuento; 

    private String metodoPago;

    // --- Campos de Entrada (Para creación POST) ---
    // Nota: clienteId ya está definido arriba, así que Lombok genera un solo setClienteId para ambos usos.

    @NotEmpty(message = "La venta debe tener al menos un producto")
    @Valid 
    private List<DetalleVentaDTO> detalles;

    private byte[] comprobantePdf;
}