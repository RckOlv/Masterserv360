package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.CarritoDTO;
import com.masterserv.productos.dto.ItemCarritoDTO;
import com.masterserv.productos.entity.Carrito;
import com.masterserv.productos.entity.ItemCarrito;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CarritoMapper {

    // --- ItemCarrito ---
    @Mappings({
        @Mapping(source = "producto.id", target = "productoId"),
        @Mapping(source = "producto.nombre", target = "productoNombre"),
        @Mapping(source = "producto.precioVenta", target = "precioUnitario"),
        //@Mapping(target = "subtotal", expression = "java(item.getProducto().getPrecioVenta().multiply(new BigDecimal(item.getCantidad())))")
    })
    ItemCarritoDTO toItemCarritoDTO(ItemCarrito item);

    List<ItemCarritoDTO> toItemCarritoDTOList(List<ItemCarrito> items);

    // --- Carrito ---
    @Mappings({
        @Mapping(source = "vendedor.id", target = "vendedorId"),
        // El total se calculará en el servicio
        @Mapping(target = "total", ignore = true) 
    })
    CarritoDTO toCarritoDTO(Carrito carrito);
}