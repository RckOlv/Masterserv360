package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.CuponDTO;
import com.masterserv.productos.entity.Cupon;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings; // <-- Mentor: IMPORTADO
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Mapper para convertir entre la Entidad Cupon y el DTO CuponDTO.
 * Utiliza MapStruct y se integra con Spring.
 */
@Mapper(componentModel = "spring")
public interface CuponMapper {

    /**
     * Mapea la Entidad Cupon (V2) al DTO CuponDTO (V2).
     */
    // --- Mentor: INICIO DE LA MODIFICACIÓN (V2) ---
    @Mappings({
        @Mapping(source = "cliente.email", target = "clienteEmail"),
        @Mapping(source = "categoria.id", target = "categoriaId"),
        @Mapping(source = "categoria.nombre", target = "categoriaNombre")
        // Los campos 'valor' y 'tipoDescuento' se mapean solos por tener el mismo nombre.
        // El campo 'descuento' (V1) ya no existe, así que se ignora.
    })
    // --- Mentor: FIN DE LA MODIFICACIÓN ---
    CuponDTO toCuponDTO(Cupon cupon);

    /**
     * Mapea una lista de Entidades a una lista de DTOs.
     */
    List<CuponDTO> toCuponDTOList(List<Cupon> cupones);
    
    // (El mapeo inverso no lo necesitamos, ya que 'PuntosService'
    // crea la entidad Cupon manualmente).
}