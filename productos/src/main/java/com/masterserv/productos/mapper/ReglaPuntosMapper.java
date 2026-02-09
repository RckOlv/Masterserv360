package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.ReglaPuntosDTO;
import com.masterserv.productos.entity.ReglaPuntos;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.AfterMapping; // <--- IMPORTANTE
import org.mapstruct.MappingTarget; // <--- IMPORTANTE

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReglaPuntosMapper {

    @Mapping(source = "fechaCreacion", target = "fechaInicioVigencia")
    ReglaPuntosDTO toReglaPuntosDTO(ReglaPuntos reglaPuntos);

    @Mapping(source = "fechaInicioVigencia", target = "fechaCreacion")
    ReglaPuntos toReglaPuntos(ReglaPuntosDTO reglaPuntosDTO);

    List<ReglaPuntosDTO> toReglaPuntosDTOList(List<ReglaPuntos> reglaPuntosList);

    List<ReglaPuntos> toReglaPuntosList(List<ReglaPuntosDTO> reglaPuntosDTOList);

    // --- MENTOR: Lógica para calcular vencimiento ---
    @AfterMapping
    default void calcularVencimiento(ReglaPuntos source, @MappingTarget ReglaPuntosDTO target) {
        if (source.getFechaCreacion() != null && source.getCaducidadPuntosMeses() != null) {
            target.setFechaVencimiento(
                source.getFechaCreacion().toLocalDate().plusMonths(source.getCaducidadPuntosMeses())
            );
        }
    }
    // ------------------------------------------------
}