package com.masterserv.productos.dto;

import com.masterserv.productos.enums.EstadoVenta; // Importa tu enum
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat; // Para parsear fechas

import java.time.LocalDate; // Usamos LocalDate para rangos de días completos

@Data // @Data está bien aquí, no es una entidad
public class VentaFiltroDTO {

    private Long clienteId; // Para filtrar por cliente específico
    private Long vendedorId; // Para filtrar por vendedor específico

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) // Espera formato YYYY-MM-DD
    private LocalDate fechaDesde; // Fecha de inicio del rango

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) // Espera formato YYYY-MM-DD
    private LocalDate fechaHasta; // Fecha de fin del rango

    private EstadoVenta estado; // Para filtrar por estado (COMPLETADA, CANCELADA)

    // Podrías añadir otros campos si los necesitas, ej:
    // private String terminoBusqueda; // Para buscar en nombre cliente/vendedor
}