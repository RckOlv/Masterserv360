package com.masterserv.productos.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CarritoDTO {
    private Long id;
    private Long vendedorId;
    private List<ItemCarritoDTO> items;
    private BigDecimal total;
}