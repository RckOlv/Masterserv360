package com.masterserv.productos.service;

import com.masterserv.productos.dto.reporte.StockInmovilizadoDTO;
import com.masterserv.productos.dto.reporte.ValorizacionInventarioDTO;
import com.masterserv.productos.dto.reporte.VariacionCostoDTO;
import com.masterserv.productos.enums.EstadoPedido;
import com.masterserv.productos.repository.DetallePedidoRepository;
import com.masterserv.productos.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReporteAvanzadoService {

    @Autowired private ProductoRepository productoRepository;
    @Autowired private DetallePedidoRepository detallePedidoRepository;

    // 1. Valorización
    @Transactional(readOnly = true)
    public List<ValorizacionInventarioDTO> getValorizacionInventario() {
        return productoRepository.obtenerValorizacionPorCategoria();
    }

    // 2. Stock Inmovilizado (El "Hueso")
    @Transactional(readOnly = true)
    public List<StockInmovilizadoDTO> getStockInmovilizado(int diasSinMovimiento) {
        // Calculamos la fecha límite (Hoy - X días)
        LocalDateTime fechaLimite = LocalDateTime.now().minusDays(diasSinMovimiento);
        return productoRepository.obtenerStockInmovilizado(fechaLimite);
    }

    // 3. Historial de Costos (Inflación)
    @Transactional(readOnly = true)
    public List<VariacionCostoDTO> getHistorialCostos(Long productoId) {
        // ✅ Pasamos el Enum aquí
        return detallePedidoRepository.obtenerHistorialCostos(productoId, EstadoPedido.COMPLETADO);
    }
}