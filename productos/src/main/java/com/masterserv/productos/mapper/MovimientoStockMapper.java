package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.MovimientoStockDTO;
import com.masterserv.productos.entity.MovimientoStock;
import com.masterserv.productos.entity.Producto;
import com.masterserv.productos.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MovimientoStockMapper {

    // DTO -> Entidad
    @Mapping(source = "productoId", target = "producto")
    @Mapping(source = "usuarioId", target = "usuario")
    MovimientoStock toMovimientoStock(MovimientoStockDTO dto);

    @Mapping(source = "producto.id", target = "productoId")
    @Mapping(source = "usuario.id", target = "usuarioId")
    @Mapping(target = "usuarioNombre", ignore = true) // Lo llenamos manualmente en el service para mayor seguridad
    MovimientoStockDTO toMovimientoStockDTO(MovimientoStock entity);

    // --- Helpers para que MapStruct convierta IDs en Entidades vacías ---
    default Producto mapProducto(Long id) {
        if (id == null) return null;
        Producto p = new Producto();
        p.setId(id);
        return p;
    }

    default Usuario mapUsuario(Long id) {
        if (id == null) return null;
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }
}