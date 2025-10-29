package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.DetalleVentaDTO;
import com.masterserv.productos.dto.VentaDTO;
import com.masterserv.productos.entity.DetalleVenta;
import com.masterserv.productos.entity.Producto; // Importar Producto
import com.masterserv.productos.entity.Usuario;  // Importar Usuario
import com.masterserv.productos.entity.Venta;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set; // Importar Set para mapeo a Entidad

@Mapper(
    componentModel = "spring", // Para que Spring lo detecte como un Bean
    imports = { BigDecimal.class }, // Para usar BigDecimal en expressions
    // Evita crear instancias nulas si la fuente es nula
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface VentaMapper {

    // --- Mapeo DetalleVenta (Entidad -> DTO) ---
    @Mappings({
        @Mapping(source = "producto.id", target = "productoId"),
        @Mapping(source = "producto.nombre", target = "productoNombre"),
        @Mapping(source = "producto.codigo", target = "productoCodigo"),
        // Calcula el subtotal (precio * cantidad) con chequeo de nulos
        @Mapping(target = "subtotal", expression = "java(detalle.getPrecioUnitario() != null ? detalle.getPrecioUnitario().multiply(new BigDecimal(detalle.getCantidad())) : BigDecimal.ZERO)")
    })
    DetalleVentaDTO toDetalleVentaDTO(DetalleVenta detalle);

    // --- Mapeo Venta (Entidad -> DTO) ---
    @Mappings({
        @Mapping(source = "cliente.id", target = "clienteId"),
        // Asumiendo que Usuario tiene getNombre() o getNombreCompleto()
        @Mapping(source = "cliente.nombre", target = "clienteNombre"),
        @Mapping(source = "vendedor.nombre", target = "vendedorNombre"),
        // MapStruct sabe cómo mapear Set<DetalleVenta> a List<DetalleVentaDTO>
        // usando el método toDetalleVentaDTO definido arriba.
        @Mapping(source = "detalles", target = "detalles")
    })
    VentaDTO toVentaDTO(Venta venta);

    // Método para convertir listas (útil para paginación)
    List<VentaDTO> toVentaDTOList(List<Venta> ventas);
    // Podrías necesitar uno para Page si usas Page<Venta> -> Page<VentaDTO> directamente

    // --- Mapeo Venta (DTO -> Entidad) ---
    @Mappings({
        // Ignoramos campos que se generan/calculan en el Service
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "fechaVenta", ignore = true),
        @Mapping(target = "estado", ignore = true),
        @Mapping(target = "totalVenta", ignore = true),
        @Mapping(target = "vendedor", ignore = true), // El Service lo asigna desde Principal
        @Mapping(target = "fechaCreacion", ignore = true),
        @Mapping(target = "fechaModificacion", ignore = true),
        // Mapeamos IDs a entidades proxy usando helpers
        @Mapping(source = "clienteId", target = "cliente"), // Usa mapUsuario helper
        // MapStruct convierte List<DTO> a Set<Entity> usando toDetalleVenta
        @Mapping(source = "detalles", target = "detalles")
    })
    Venta toVenta(VentaDTO ventaDTO);

    // --- Mapeo DetalleVenta (DTO -> Entidad) ---
    @Mappings({
        // Ignoramos campos que se generan/calculan en el Service o no vienen del DTO de creación
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "precioUnitario", ignore = true), // El Service lo calcula desde Producto
        @Mapping(target = "venta", ignore = true),          // El Service lo asigna bidireccionalmente
        // Mapeamos ID a entidad proxy usando helper
        @Mapping(source = "productoId", target = "producto") // Usa mapProducto helper
        // Campos que sí mapeamos: 'cantidad' (automático)
    })
    DetalleVenta toDetalleVenta(DetalleVentaDTO detalleDTO);

    // --- Helper Methods (Default methods in interface) ---
    // Estos métodos crean entidades "proxy" solo con el ID.
    // JPA/Hibernate las usará para establecer las relaciones FK al guardar.

    default Usuario mapUsuario(Long id) {
        if (id == null) {
            return null;
        }
        Usuario usuario = new Usuario();
        usuario.setId(id);
        return usuario;
    }

    default Producto mapProducto(Long id) {
        if (id == null) {
            return null;
        }
        Producto producto = new Producto();
        producto.setId(id);
        return producto;
    }
}