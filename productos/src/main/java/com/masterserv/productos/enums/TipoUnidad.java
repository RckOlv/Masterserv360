package com.masterserv.productos.enums;

public enum TipoUnidad {
    UNIDAD("Unidad"),
    CAJA("Caja"),
    BOLSA("Bolsa"),
    PACK("Pack"),
    METRO("Metro"),
    LITRO("Litro"),
    JUEGO("Juego/Kit");

    private final String descripcion;

    TipoUnidad(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}