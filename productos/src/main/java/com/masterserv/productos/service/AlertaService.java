package com.masterserv.productos.service;

import com.masterserv.productos.entity.Alerta;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.repository.AlertaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertaService {

    private final AlertaRepository alertaRepository;

    public void crearAlerta(String titulo, String mensaje, Usuario cliente, String url) {
        Alerta alerta = new Alerta();
        alerta.setTitulo(titulo);
        alerta.setMensaje(mensaje);
        alerta.setClienteOrigen(cliente);
        alerta.setUrlRedireccion(url);
        alerta.setLeida(false);
        alerta.setFechaCreacion(LocalDateTime.now());
        
        alertaRepository.save(alerta);
    }

    public List<Alerta> obtenerNoLeidas() {
        return alertaRepository.findByLeidaFalseOrderByFechaCreacionDesc();
    }

    public void marcarComoLeida(Long id) {
        alertaRepository.findById(id).ifPresent(alerta -> {
            alerta.setLeida(true);
            alertaRepository.save(alerta);
        });
    }
}