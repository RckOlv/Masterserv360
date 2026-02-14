package com.masterserv.productos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DetalleComparativaDTO(
    Long itemCotizacionId,
    Long cotizacionId,
    String proveedorNombre,
    BigDecimal precioOferta,
    Integer cantidadOfrecida,
    LocalDate fechaEntrega,
    boolean esRecomendada
) {}