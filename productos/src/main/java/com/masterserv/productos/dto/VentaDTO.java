package com.masterserv.productos.dto;

import com.masterserv.productos.enums.EstadoVenta; // Asegúrate de tener este Enum
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime; // Importar LocalDateTime
import java.util.List; // Usamos List en DTOs, más flexible que Set

@Data // @Data está bien aquí
public class VentaDTO {

    // --- Campos de Lectura (Para respuestas GET) ---
    private Long id;
    private LocalDateTime fechaVenta;
    private EstadoVenta estado;
    private BigDecimal totalVenta;
    // Nombres para mostrar en la UI
    private String clienteNombre; // Asume que Usuario tiene nombre/apellido
    private String vendedorNombre; // Asume que Usuario tiene nombre/apellido
    // Campos de auditoría
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;

    // --- Campos de Entrada (Para creación POST) ---
    @NotNull(message = "El ID del cliente es obligatorio")
    private Long clienteId; // Corresponde a cliente_usuario_id en la entidad

    // El vendedorId NO viene en el DTO, se tomará del usuario autenticado (Principal)

    @NotEmpty(message = "La venta debe tener al menos un producto")
    @Valid // ¡Importante! Valida cada DetalleVentaDTO dentro de la lista
    private List<DetalleVentaDTO> detalles;

}