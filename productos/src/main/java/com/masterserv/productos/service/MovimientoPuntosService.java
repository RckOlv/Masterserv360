package com.masterserv.productos.service;

import com.masterserv.productos.dto.MovimientoPuntosDTO;
import com.masterserv.productos.entity.MovimientoPuntos;
import com.masterserv.productos.mapper.MovimientoPuntosMapper;
import com.masterserv.productos.repository.MovimientoPuntosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MovimientoPuntosService {

    @Autowired
    private MovimientoPuntosRepository movimientoPuntosRepository;

    private final MovimientoPuntosMapper movimientoPuntosMapper = MovimientoPuntosMapper.INSTANCE;

    public List<MovimientoPuntosDTO> getAllMovimientosPuntos() {
        return movimientoPuntosRepository.findAll().stream()
                .map(movimientoPuntosMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<MovimientoPuntosDTO> getMovimientoPuntosById(Long id) {
        return movimientoPuntosRepository.findById(id)
                .map(movimientoPuntosMapper::toDTO);
    }

    public MovimientoPuntosDTO createMovimientoPuntos(MovimientoPuntosDTO movimientoPuntosDTO) {
        MovimientoPuntos movimientoPuntos = movimientoPuntosMapper.toEntity(movimientoPuntosDTO);
        return movimientoPuntosMapper.toDTO(movimientoPuntosRepository.save(movimientoPuntos));
    }

    public MovimientoPuntosDTO updateMovimientoPuntos(Long id, MovimientoPuntosDTO movimientoPuntosDTO) {
        MovimientoPuntos movimientoPuntos = movimientoPuntosMapper.toEntity(movimientoPuntosDTO);
        movimientoPuntos.setId(id);
        return movimientoPuntosMapper.toDTO(movimientoPuntosRepository.save(movimientoPuntos));
    }

    public void deleteMovimientoPuntos(Long id) {
        movimientoPuntosRepository.deleteById(id);
    }
}
