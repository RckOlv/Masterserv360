package com.masterserv.productos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List; // Usamos List en DTOs

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarritoDTO {

    private Long id; // ID del Carrito
    private Long vendedorId; // ID del Usuario vendedor dueño del carrito
    private List<ItemCarritoDTO> items; // La lista de productos en el carrito
    private BigDecimal totalCarrito; // Calculado (suma de subtotales)
    private int cantidadItems; // Calculado (suma de cantidades)
	public void setFechaModificacion(LocalDateTime fechaModificacion) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'setFechaModificacion'");
	}
	public void setFechaCreacion(LocalDateTime fechaCreacion) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'setFechaCreacion'");
	}

}