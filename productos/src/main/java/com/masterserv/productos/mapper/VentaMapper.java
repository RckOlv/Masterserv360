package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.DetalleVentaDTO;
import com.masterserv.productos.dto.VentaDTO;
import com.masterserv.productos.dto.VentaResumenDTO; // <-- ¡IMPORTAR NUEVO DTO!
import com.masterserv.productos.entity.DetalleVenta;
import com.masterserv.productos.entity.Producto;
import com.masterserv.productos.entity.Usuario; 
import com.masterserv.productos.entity.Venta;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Mapper(
    componentModel = "spring",
    imports = { BigDecimal.class },
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface VentaMapper {

    // --- Mapeo DetalleVenta (Entidad -> DTO Admin) ---
    @Mappings({
        @Mapping(source = "producto.id", target = "productoId"),
        @Mapping(source = "producto.nombre", target = "productoNombre"),
        @Mapping(source = "producto.codigo", target = "productoCodigo"),
        @Mapping(target = "subtotal", expression = "java(detalle.getPrecioUnitario() != null ? detalle.getPrecioUnitario().multiply(new BigDecimal(detalle.getCantidad())) : BigDecimal.ZERO)")
    })
    DetalleVentaDTO toDetalleVentaDTO(DetalleVenta detalle);

    // --- Mapeo Venta (Entidad -> DTO Admin) ---
    @Mappings({
        @Mapping(source = "cliente.id", target = "clienteId"),
        @Mapping(source = "cliente.nombre", target = "clienteNombre"),
        @Mapping(source = "vendedor.nombre", target = "vendedorNombre"),
        @Mapping(source = "detalles", target = "detalles"),
        @Mapping(source = "cupon.codigo", target = "codigoCupon") // Asumiendo que VentaDTO tiene 'codigoCupon'
    })
    VentaDTO toVentaDTO(Venta venta);

    List<VentaDTO> toVentaDTOList(List<Venta> ventas);

    // --- Mapeo Venta (DTO Admin -> Entidad) ---
    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "fechaVenta", ignore = true),
        @Mapping(target = "estado", ignore = true),
        @Mapping(target = "totalVenta", ignore = true),
        @Mapping(target = "vendedor", ignore = true),
        @Mapping(target = "fechaCreacion", ignore = true),
        @Mapping(target = "fechaModificacion", ignore = true),
        @Mapping(target = "cupon", ignore = true), // El Service lo maneja
        @Mapping(source = "clienteId", target = "cliente"),
        @Mapping(source = "detalles", target = "detalles")
    })
    Venta toVenta(VentaDTO ventaDTO);

    // --- Mapeo DetalleVenta (DTO Admin -> Entidad) ---
    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "precioUnitario", ignore = true),
        @Mapping(target = "venta", ignore = true),
        @Mapping(source = "productoId", target = "producto")
    })
    DetalleVenta toDetalleVenta(DetalleVentaDTO detalleDTO);

    // --- Helper Methods (Default methods in interface) ---
    default Usuario mapUsuario(Long id) {
        if (id == null) return null;
        Usuario usuario = new Usuario();
        usuario.setId(id);
        return usuario;
    }

    default Producto mapProducto(Long id) {
        if (id == null) return null;
        Producto producto = new Producto();
        producto.setId(id);
        return producto;
    }


    // --- ¡NUEVOS MÉTODOS PARA EL HISTORIAL DEL CLIENTE! ---

    /**
     * Mapea la Entidad Venta al DTO de Resumen (Vista del Cliente).
     */
    @Mappings({
        @Mapping(source = "cupon.codigo", target = "codigoCuponUsado"),
        // Calcula cuántas *líneas* de producto tuvo la venta
        @Mapping(target = "cantidadItems", expression = "java(venta.getDetalles() != null ? venta.getDetalles().size() : 0)")
    })
    VentaResumenDTO toVentaResumenDTO(Venta venta);

    // (MapStruct usará el método de arriba automáticamente para mapear listas)
}