package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.CuponDTO;
import com.masterserv.productos.entity.Cupon;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Mapper para convertir entre la Entidad Cupon y el DTO CuponDTO.
 * Utiliza MapStruct y se integra con Spring.
 */
@Mapper(componentModel = "spring") // ¡Clave! Lo convierte en un Bean de Spring
public interface CuponMapper {

    // No necesitas esta línea si usas componentModel="spring"
    // CuponMapper INSTANCE = Mappers.getMapper(CuponMapper.class);

    /**
     * Mapea la Entidad Cupon al DTO CuponDTO.
     * La lógica especial aquí es "aplanar" el objeto Cliente.
     */
    // --- ¡LA LÍNEA MÁS IMPORTANTE! ---
    // Le dice a MapStruct: "Toma el objeto 'cliente' de la Entidad, 
    // navega a su campo 'email', y pon ese valor en el campo 'clienteEmail' del DTO."
    @Mapping(source = "cliente.email", target = "clienteEmail")
    CuponDTO toCuponDTO(Cupon cupon);

    /**
     * Mapea una lista de Entidades a una lista de DTOs.
     * MapStruct maneja esto automáticamente.
     */
    List<CuponDTO> toCuponDTOList(List<Cupon> cupones);
    
    // (Opcional) Mapeo inverso. 
    // Lo ignoramos porque creamos el Cupón manualmente en el servicio.
    // @Mapping(target = "cliente", ignore = true)
    // @Mapping(target = "venta", ignore = true)
    // Cupon toCupon(CuponDTO cuponDTO);
}