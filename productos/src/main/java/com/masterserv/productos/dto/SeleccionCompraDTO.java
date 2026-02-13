package com.masterserv.productos.dto;

import java.util.List;

public class SeleccionCompraDTO {
    private List<Long> itemCotizacionIds; // IDs de los items ganadores

    // Getters y Setters
    public List<Long> getItemCotizacionIds() { return itemCotizacionIds; }
    public void setItemCotizacionIds(List<Long> itemCotizacionIds) { this.itemCotizacionIds = itemCotizacionIds; }
}