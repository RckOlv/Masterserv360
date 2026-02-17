package com.masterserv.productos.dto.reporte;
import java.math.BigDecimal;

public interface ValorizacionInventarioDTO {
    String getCategoria();
    Long getCantidadUnidades(); // Cantidad física de items
    BigDecimal getValorTotal(); // Cantidad * Costo
}