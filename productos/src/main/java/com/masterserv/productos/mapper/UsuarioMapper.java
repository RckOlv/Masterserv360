package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.UsuarioDTO;
import com.masterserv.productos.entity.Rol;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.entity.TipoDocumento; // <-- NECESITAS ESTE IMPORT
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    // --- Mapeo Entidad -> DTO ---
    @Mappings({
        @Mapping(source = "tipoDocumento.id", target = "tipoDocumentoId"), // OK
        @Mapping(target = "passwordHash", ignore = true),
        @Mapping(target = "fechaCreacion", ignore = true),
        @Mapping(target = "fechaModificacion", ignore = true)
    })
    UsuarioDTO toUsuarioDTO(Usuario usuario);

    List<UsuarioDTO> toUsuarioDTOList(List<Usuario> usuarios);

    // --- Mapeo DTO -> Entidad (Creación/Guardado) ---
    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "passwordHash", ignore = true),
        @Mapping(target = "roles", ignore = true),
        @Mapping(target = "estado", ignore = true),
        @Mapping(source = "tipoDocumentoId", target = "tipoDocumento"), // OK, llama al helper
        @Mapping(target = "fechaCreacion", ignore = true),
        @Mapping(target = "fechaModificacion", ignore = true)
    })
    Usuario toUsuario(UsuarioDTO usuarioDTO);
    
    // --- Mapeo Actualización (UPDATE) ---
    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "passwordHash", ignore = true),
        @Mapping(target = "roles", ignore = true),
        @Mapping(target = "estado", ignore = true),
        @Mapping(source = "tipoDocumentoId", target = "tipoDocumento"), // OK, llama al helper
        @Mapping(target = "fechaCreacion", ignore = true),
        @Mapping(target = "fechaModificacion", ignore = true)
    })
    void updateUsuarioFromDto(UsuarioDTO dto, @MappingTarget Usuario entity);

    // --- MÉTODO HELPER AÑADIDO ---
    /**
     * Convierte el ID (Long) de tipoDocumento a una Entidad TipoDocumento para JPA.
     * @param tipoDocumentoId El ID del tipo de documento.
     * @return Una entidad TipoDocumento con solo el ID seteado.
     */
    default TipoDocumento mapTipoDocumento(Long tipoDocumentoId) {
        if (tipoDocumentoId == null) {
            return null;
        }
        TipoDocumento tipoDocumento = new TipoDocumento();
        tipoDocumento.setId(tipoDocumentoId);
        return tipoDocumento;
    }
}