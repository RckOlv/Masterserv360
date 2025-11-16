package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.UsuarioDTO;
import com.masterserv.productos.entity.Rol;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.entity.TipoDocumento; 
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring", uses = {RolMapper.class})
public interface UsuarioMapper {

    @Mappings({
        @Mapping(source = "tipoDocumento.id", target = "tipoDocumentoId"),
        @Mapping(source = "tipoDocumento.nombreCorto", target = "tipoDocumentoNombre"),
        @Mapping(target = "passwordHash", ignore = true),
        
        // --- Mentor: CORRECCIÓN CRÍTICA ---
        // ¡Quitamos estos Mappings de aquí!
        // @Mapping(target = "fechaCreacion", ignore = true),
        // @Mapping(target = "fechaModificacion", ignore = true)
        // --- Fin Corrección ---
    })
    UsuarioDTO toUsuarioDTO(Usuario usuario);

    List<UsuarioDTO> toUsuarioDTOList(List<Usuario> usuarios);

    // ESTE ESTÁ CORRECTO
    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "passwordHash", ignore = true),
        @Mapping(target = "roles", ignore = true), 
        @Mapping(target = "estado", ignore = true),
        @Mapping(source = "tipoDocumentoId", target = "tipoDocumento"), 
        @Mapping(target = "fechaCreacion", ignore = true),
        @Mapping(target = "fechaModificacion", ignore = true)
    })
    Usuario toUsuario(UsuarioDTO usuarioDTO);
    
    // ESTE ESTÁ CORRECTO
    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "passwordHash", ignore = true),
        @Mapping(target = "roles", ignore = true), 
        @Mapping(target = "estado", ignore = true),
        @Mapping(source = "tipoDocumentoId", target = "tipoDocumento"), 
        @Mapping(target = "fechaCreacion", ignore = true),
        @Mapping(target = "fechaModificacion", ignore = true)
    })
    void updateUsuarioFromDto(UsuarioDTO dto, @MappingTarget Usuario entity);

    // --- MÉTODO HELPER (ya lo tenías, está perfecto) ---
    default TipoDocumento mapTipoDocumento(Long tipoDocumentoId) {
        if (tipoDocumentoId == null) {
            return null;
        }
        TipoDocumento tipoDocumento = new TipoDocumento();
        tipoDocumento.setId(tipoDocumentoId);
        return tipoDocumento;
    }
}