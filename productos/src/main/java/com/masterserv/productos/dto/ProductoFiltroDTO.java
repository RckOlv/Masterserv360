package com.masterserv.productos.dto;

import java.time.LocalDate;

public class ProductoFiltroDTO {
    private String nombre;
    private Long categoriaId;
    private Boolean activo;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;

    // Getters y setters
    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    public Long getCategoriaId() {
        return categoriaId;
    }
    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }
    public Boolean getActivo() {
        return activo;
    }
    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
    public LocalDate getFechaDesde() {
        return fechaDesde;
    }
    public void setFechaDesde(LocalDate fechaDesde) {
        this.fechaDesde = fechaDesde;
    }
    public LocalDate getFechaHasta() {
        return fechaHasta;
    }
    public void setFechaHasta(LocalDate fechaHasta) {
        this.fechaHasta = fechaHasta;
    }
}
