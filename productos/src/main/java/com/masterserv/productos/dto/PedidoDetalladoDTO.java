package com.masterserv.productos.dto;

import com.masterserv.productos.enums.EstadoPedido;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PedidoDetalladoDTO {
    // Cabecera
    private Long id;
    private LocalDateTime fechaPedido;
    private EstadoPedido estado;
    private BigDecimal totalPedido;
    
    // Datos del Proveedor
    private Long proveedorId;
    private String proveedorRazonSocial;
    private String proveedorCuit;
    private String proveedorEmail;
    private String proveedorTelefono; // Agregado por si lo necesitas
    
    // Datos del Usuario
    // ESTE ES EL CAMPO QUE DABA ERROR. 
    // Si en el servicio usas setUsuarioSolicitante, aquí debe llamarse así:
    private String usuarioSolicitante; 

    // Lista de productos (Reutilizamos tu DTO existente)
    private List<DetallePedidoDTO> detalles;
}