package com.masterserv.productos.service;

import com.masterserv.productos.dto.MovimientoStockDTO;
import com.masterserv.productos.entity.MovimientoStock;
import com.masterserv.productos.entity.Producto;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.enums.TipoMovimiento;
import com.masterserv.productos.mapper.MovimientoStockMapper;
import com.masterserv.productos.repository.MovimientoStockRepository;
import com.masterserv.productos.repository.ProductoRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MovimientoStockService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MovimientoStockRepository movimientoStockRepository;

    @Autowired
    private MovimientoStockMapper movimientoStockMapper;

    @Transactional
    public void registrarMovimiento(MovimientoStockDTO dto) {
        
        Producto producto = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + dto.getProductoId()));
        
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + dto.getUsuarioId()));

        int cantidadAjuste = dto.getCantidad();
        
        switch (dto.getTipoMovimiento()) {
            case ENTRADA_PEDIDO:
            case CARGA_INICIAL:
            case AJUSTE_MANUAL: 
            case DEVOLUCION:
                producto.setStockActual(producto.getStockActual() + cantidadAjuste);
                break;

            case SALIDA_VENTA:
                if (producto.getStockActual() < cantidadAjuste) {
                    throw new RuntimeException("Stock insuficiente para el producto: " + producto.getNombre());
                }
                producto.setStockActual(producto.getStockActual() - cantidadAjuste);
                cantidadAjuste = -cantidadAjuste; // Guardamos el movimiento como negativo
                break;
                
            default:
                throw new RuntimeException("Tipo de movimiento no soportado: " + dto.getTipoMovimiento());
        }

        productoRepository.save(producto);

        MovimientoStock movimiento = movimientoStockMapper.toMovimientoStock(dto);
        movimiento.setProducto(producto);
        movimiento.setUsuario(usuario);
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setCantidad(cantidadAjuste); 

        movimientoStockRepository.save(movimiento);
    }
}