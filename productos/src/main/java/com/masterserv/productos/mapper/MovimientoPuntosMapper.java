package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.MovimientoPuntosDTO;
import com.masterserv.productos.entity.MovimientoPuntos;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MovimientoPuntosMapper {

    MovimientoPuntosMapper INSTANCE = Mappers.getMapper(MovimientoPuntosMapper.class);

    @Mapping(source = "cuentaPuntos.id", target = "cuentaPuntosId")
    @Mapping(source = "venta.id", target = "ventaId")
    MovimientoPuntosDTO toDTO(MovimientoPuntos movimientoPuntos);

    @Mapping(source = "cuentaPuntosId", target = "cuentaPuntos.id")
    @Mapping(source = "ventaId", target = "venta.id")
    MovimientoPuntos toEntity(MovimientoPuntosDTO movimientoPuntosDTO);
}
