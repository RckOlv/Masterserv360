package com.masterserv.productos.dto;

import com.masterserv.productos.enums.EstadoPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set; // Usamos Set para evitar duplicados

@Data
@EqualsAndHashCode(callSuper = true) // Hereda de AuditableDTO
public class PedidoDTO extends AuditableDTO {
    
    private Long id;

    @NotNull(message = "El ID del proveedor es obligatorio")
    private Long proveedorId;
    
    @NotNull(message = "El ID del usuario (empleado) es obligatorio")
    private Long usuarioId; // El empleado que crea el pedido

    // Campos de solo lectura (se cargan en el backend)
    private String proveedorRazonSocial;
    private String usuarioNombre;
    private LocalDateTime fechaPedido;
    private EstadoPedido estado;
    private BigDecimal totalPedido;
    private String token;

    // Lista de detalles
    @Valid // Valida los DTOs anidados
    @NotEmpty(message = "El pedido debe tener al menos un detalle")
    private Set<DetallePedidoDTO> detalles;
}