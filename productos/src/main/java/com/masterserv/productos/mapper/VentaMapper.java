package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.DetalleVentaDTO;
import com.masterserv.productos.dto.VentaDTO;
import com.masterserv.productos.dto.VentaResumenDTO;
import com.masterserv.productos.entity.DetalleVenta;
import com.masterserv.productos.entity.Producto;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.entity.Venta;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring", imports = {BigDecimal.class})
public abstract class VentaMapper {

    // --- 1. Mapeo Venta (Entidad -> DTO) ---
    @Mappings({
        @Mapping(source = "cliente.id", target = "clienteId"),
        // Ignoramos los campos calculados para llenarlos manualmente abajo
        @Mapping(target = "clienteNombre", ignore = true),
        @Mapping(target = "vendedorNombre", ignore = true),
        @Mapping(target = "vendedorId", ignore = true),
        @Mapping(target = "montoDescuento", ignore = true),
        @Mapping(target = "fechaVenta", ignore = true),
        @Mapping(source = "detalles", target = "detalles"),
        @Mapping(source = "cupon.codigo", target = "codigoCupon"),
        @Mapping(target = "comprobantePdf", ignore = true)
    })
    public abstract VentaDTO toVentaDTO(Venta venta);

    @AfterMapping
    protected void completarDatos(@MappingTarget VentaDTO dto, Venta venta) {
        // Nombres
        if (venta.getCliente() != null) {
            dto.setClienteNombre(venta.getCliente().getNombre() + " " + venta.getCliente().getApellido());
        }
        if (venta.getVendedor() != null) {
            dto.setVendedorId(venta.getVendedor().getId());
            dto.setVendedorNombre(venta.getVendedor().getNombre() + " " + venta.getVendedor().getApellido());
        }

        // Fecha
        if (venta.getFechaVenta() != null) {
            dto.setFechaVenta(venta.getFechaVenta());
        }

        // Descuento
        BigDecimal subtotalReal = BigDecimal.ZERO;
        if (venta.getDetalles() != null) {
            for (DetalleVenta d : venta.getDetalles()) {
                BigDecimal subItem = d.getPrecioUnitario().multiply(new BigDecimal(d.getCantidad()));
                subtotalReal = subtotalReal.add(subItem);
            }
        }

        if (venta.getTotalVenta() != null) {
            BigDecimal descuento = subtotalReal.subtract(venta.getTotalVenta());
            if (descuento.compareTo(BigDecimal.ZERO) > 0) {
                dto.setMontoDescuento(descuento);
            } else {
                dto.setMontoDescuento(BigDecimal.ZERO);
            }
        }
    }

    // --- 2. Mapeo Detalle ---
    @Mappings({
        @Mapping(source = "producto.id", target = "productoId"),
        @Mapping(source = "producto.nombre", target = "productoNombre"),
        @Mapping(source = "producto.codigo", target = "productoCodigo"),
        @Mapping(target = "subtotal", expression = "java(detalle.getPrecioUnitario().multiply(new BigDecimal(detalle.getCantidad())))")
    })
    public abstract DetalleVentaDTO toDetalleVentaDTO(DetalleVenta detalle);

    // --- 3. Mapeo Inverso (DTO -> Entidad) ---
    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "fechaVenta", ignore = true),
        @Mapping(target = "estado", ignore = true),
        @Mapping(target = "totalVenta", ignore = true),
        @Mapping(target = "vendedor", ignore = true),
        @Mapping(target = "fechaCreacion", ignore = true),
        @Mapping(target = "fechaModificacion", ignore = true),
        @Mapping(target = "cupon", ignore = true),
        @Mapping(source = "clienteId", target = "cliente"),
        @Mapping(source = "detalles", target = "detalles")
    })
    public abstract Venta toVenta(VentaDTO ventaDTO);

    public abstract List<VentaDTO> toVentaDTOList(List<Venta> ventas);

    // --- 4. Mapeo Resumen ---
    @Mappings({
        @Mapping(source = "cupon.codigo", target = "codigoCuponUsado"),
        @Mapping(target = "cantidadItems", expression = "java(venta.getDetalles() != null ? venta.getDetalles().size() : 0)"),
        @Mapping(target = "clienteNombre", expression = "java(venta.getCliente().getNombre() + \" \" + venta.getCliente().getApellido())")
    })
    public abstract VentaResumenDTO toVentaResumenDTO(Venta venta);

    // --- Helpers ---
    public Usuario mapUsuario(Long id) {
        if (id == null) return null;
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }
    
    public abstract DetalleVenta toDetalleVenta(DetalleVentaDTO dto);
}