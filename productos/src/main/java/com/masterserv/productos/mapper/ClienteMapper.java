package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.ClientePerfilDTO;
import com.masterserv.productos.dto.ClientePerfilUpdateDTO;
import com.masterserv.productos.entity.TipoDocumento;
import com.masterserv.productos.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface ClienteMapper {

    /**
     * Mapea la Entidad Usuario al DTO de Perfil (para LEER).
     */
    @Mappings({
        // --- ¡CORRECCIÓN AQUÍ! ---
        // Mapeamos el ID
        @Mapping(source = "tipoDocumento.id", target = "tipoDocumentoId"),
        // Mapeamos el Nombre (usando 'nombreCorto' como detectaste)
        @Mapping(source = "tipoDocumento.nombreCorto", target = "tipoDocumento")
        // -------------------------
    })
    ClientePerfilDTO toClientePerfilDTO(Usuario usuario);

    /**
     * Actualiza una Entidad Usuario existente desde un DTO (para ESCRIBIR).
     */
    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "email", ignore = true),
        @Mapping(target = "passwordHash", ignore = true),
        @Mapping(target = "estado", ignore = true),
        @Mapping(target = "roles", ignore = true), 
        @Mapping(target = "fechaCreacion", ignore = true),
        @Mapping(target = "fechaModificacion", ignore = true),
        @Mapping(source = "tipoDocumentoId", target = "tipoDocumento")
    })
    void updateUsuarioFromDTO(ClientePerfilUpdateDTO dto, @MappingTarget Usuario usuario);

    /**
     * Helper para MapStruct
     */
    default TipoDocumento map(Long tipoDocumentoId) {
        if (tipoDocumentoId == null) {
            return null;
        }
        TipoDocumento tipoDocumento = new TipoDocumento();
        tipoDocumento.setId(tipoDocumentoId);
        return tipoDocumento;
    }
}