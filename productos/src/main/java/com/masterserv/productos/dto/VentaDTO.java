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
    private String clienteNombre; 
    private String vendedorNombre;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;

    // (Opcional) El código del cupón que el cliente quiere usar
    private String codigoCupon; 

    // --- Campos de Entrada (Para creación POST) ---
    @NotNull(message = "El ID del cliente es obligatorio")
    private Long clienteId; 

    @NotEmpty(message = "La venta debe tener al menos un producto")
    @Valid 
    private List<DetalleVentaDTO> detalles;

    // --- ¡INICIO DE LA MODIFICACIÓN! ---
    /**
     * Campo transitorio para transportar el PDF recién generado al frontend.
     * No se mapea a la base de datos, solo se usa en la respuesta del POST.
     */
    private byte[] comprobantePdf;
    // --- FIN DE LA MODIFICACIÓN ---
}