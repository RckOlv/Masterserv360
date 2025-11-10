package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.ReglaPuntosDTO;
import com.masterserv.productos.entity.ReglaPuntos;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReglaPuntosMapper {

    @Mapping(source = "fechaCreacion", target = "fechaInicioVigencia")
    ReglaPuntosDTO toReglaPuntosDTO(ReglaPuntos reglaPuntos);

    @Mapping(source = "fechaInicioVigencia", target = "fechaCreacion")
    ReglaPuntos toReglaPuntos(ReglaPuntosDTO reglaPuntosDTO);

    List<ReglaPuntosDTO> toReglaPuntosDTOList(List<ReglaPuntos> reglaPuntosList);

    List<ReglaPuntos> toReglaPuntosList(List<ReglaPuntosDTO> reglaPuntosDTOList);
}
