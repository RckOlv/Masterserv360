package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.DetallePedidoDTO;
import com.masterserv.productos.dto.PedidoDTO;
import com.masterserv.productos.entity.DetallePedido;
import com.masterserv.productos.entity.Pedido;
import com.masterserv.productos.entity.Producto; // Importar
import com.masterserv.productos.entity.Proveedor; // Importar
import com.masterserv.productos.entity.Usuario; // Importar
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Mapper(
    componentModel = "spring",
    imports = { BigDecimal.class } // Importa BigDecimal para las expresiones
)
public interface PedidoMapper {

    // --- Mapeo DetallePedido ---
    @Mappings({
        @Mapping(source = "producto.id", target = "productoId"),
        @Mapping(source = "producto.nombre", target = "productoNombre"),
        @Mapping(source = "producto.codigo", target = "productoCodigo"),
        // Calcula el subtotal (precio * cantidad)
        @Mapping(target = "subtotal", expression = "java(detalle.getPrecioUnitario().multiply(new BigDecimal(detalle.getCantidad())))")
    })
    DetallePedidoDTO toDetallePedidoDTO(DetallePedido detalle);

    // --- Mapeo Pedido (Entidad -> DTO) ---
    @Mappings({
        @Mapping(source = "proveedor.id", target = "proveedorId"),
        @Mapping(source = "proveedor.razonSocial", target = "proveedorRazonSocial"),
        @Mapping(source = "usuario.id", target = "usuarioId"),
        @Mapping(source = "usuario.nombre", target = "usuarioNombre"), // Asume que Usuario tiene 'nombre'
        @Mapping(source = "detalles", target = "detalles") // Mapea la lista de detalles
    })
    PedidoDTO toPedidoDTO(Pedido pedido);

    List<PedidoDTO> toPedidoDTOList(List<Pedido> pedidos);

    // --- Mapeo Pedido (DTO -> Entidad) ---
    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "fechaPedido", ignore = true),
        @Mapping(target = "estado", ignore = true),
        @Mapping(target = "totalPedido", ignore = true),
        @Mapping(target = "fechaCreacion", ignore = true),
        @Mapping(target = "fechaModificacion", ignore = true),
        @Mapping(source = "proveedorId", target = "proveedor"), // Usa helper
        @Mapping(source = "usuarioId", target = "usuario"),     // Usa helper
        @Mapping(source = "detalles", target = "detalles")  // Usa helper de detalles
    })
    Pedido toPedido(PedidoDTO pedidoDTO);

    // --- Mapeo DetallePedido (DTO -> Entidad) ---
    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "pedido", ignore = true), // El Service lo asignará
        @Mapping(target = "subtotal", ignore = true),
        @Mapping(source = "productoId", target = "producto") // Usa helper
    })
    DetallePedido toDetallePedido(DetallePedidoDTO detalleDTO);

    // --- Helpers para Mapear IDs a Entidades ---
    default Proveedor mapProveedor(Long id) {
        if (id == null) return null;
        Proveedor p = new Proveedor();
        p.setId(id);
        return p;
    }
    
    default Usuario mapUsuario(Long id) {
        if (id == null) return null;
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }

    default Producto mapProducto(Long id) {
        if (id == null) return null;
        Producto p = new Producto();
        p.setId(id);
        return p;
    }
}