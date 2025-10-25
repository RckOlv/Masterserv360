package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.DetalleVentaDTO;
import com.masterserv.productos.dto.VentaDTO;
import com.masterserv.productos.entity.DetalleVenta;
import com.masterserv.productos.entity.Venta;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring",
imports = { BigDecimal.class })

public interface VentaMapper {

    // --- DetalleVenta ---
    @Mappings({
        @Mapping(source = "producto.id", target = "productoId"),
        @Mapping(source = "producto.nombre", target = "productoNombre"),
        //@Mapping(target = "subtotal", expression = "java(detalle.getPrecioUnitario().multiply(new BigDecimal(detalle.getCantidad())))")
    })
    DetalleVentaDTO toDetalleVentaDTO(DetalleVenta detalle);

    List<DetalleVentaDTO> toDetalleVentaDTOList(List<DetalleVenta> detalles);

    // --- Venta ---
    @Mappings({
        @Mapping(source = "vendedor.id", target = "vendedorId"),
        @Mapping(source = "vendedor.nombre", target = "vendedorNombre"),
        @Mapping(source = "cliente.id", target = "clienteId"),
        @Mapping(source = "cliente.nombre", target = "clienteNombre"),
        @Mapping(source = "detalles", target = "detalles") // Mapea la lista de detalles
    })
    VentaDTO toVentaDTO(Venta venta);

    List<VentaDTO> toVentaDTOList(List<Venta> ventas);
}