package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.CarritoDTO;
import com.masterserv.productos.dto.ItemCarritoDTO;
import com.masterserv.productos.entity.Carrito;
import com.masterserv.productos.entity.ItemCarrito;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Mapper(
    componentModel = "spring",
    imports = { BigDecimal.class },
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CarritoMapper {

    @Mappings({
        @Mapping(source = "producto.id", target = "productoId"),
        @Mapping(source = "producto.nombre", target = "productoNombre"),
        @Mapping(source = "producto.codigo", target = "productoCodigo"),
        @Mapping(source = "producto.precioVenta", target = "precioUnitarioVenta"),
        @Mapping(source = "producto.stockActual", target = "stockDisponible"),
        
        // Intentamos el mapeo automático, pero si falla, el AfterMapping lo arregla
        @Mapping(source = "producto.categoria.id", target = "productoCategoriaId"),

        @Mapping(target = "subtotal", expression = "java(item.getProducto() != null && item.getProducto().getPrecioVenta() != null ? item.getProducto().getPrecioVenta().multiply(new BigDecimal(item.getCantidad())) : BigDecimal.ZERO)")
    })
    ItemCarritoDTO toItemCarritoDTO(ItemCarrito item);

    List<ItemCarritoDTO> toItemCarritoDTOList(Set<ItemCarrito> items);

    @Mappings({
        @Mapping(source = "vendedor.id", target = "vendedorId"),
        @Mapping(source = "items", target = "items"),
        @Mapping(target = "totalCarrito", ignore = true),
        @Mapping(target = "cantidadItems", ignore = true)
    })
    CarritoDTO toCarritoDTO(Carrito carrito);

    // --- MENTOR: MAPEO MANUAL FORZADO (FIX PARA NULL) ---
    @AfterMapping
    default void afterToItemCarritoDTO(ItemCarrito source, @MappingTarget ItemCarritoDTO target) {
        if (source.getProducto() != null && source.getProducto().getCategoria() != null) {
            // Forzamos la asignación manual del ID de categoría
            target.setProductoCategoriaId(source.getProducto().getCategoria().getId());
            // System.out.println("DEBUG MAPPER: Prod " + source.getProducto().getNombre() + " -> CatID: " + target.getProductoCategoriaId());
        }
    }
    // ----------------------------------------------------
}