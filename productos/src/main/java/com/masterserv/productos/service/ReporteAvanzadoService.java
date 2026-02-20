package com.masterserv.productos.service;

import com.masterserv.productos.dto.reporte.StockInmovilizadoDTO;
import com.masterserv.productos.dto.reporte.StockInmovilizadoResponse;
import com.masterserv.productos.dto.reporte.ValorizacionInventarioDTO;
import com.masterserv.productos.dto.reporte.VariacionCostoDTO;
import com.masterserv.productos.enums.EstadoPedido;
import com.masterserv.productos.repository.DetallePedidoRepository;
import com.masterserv.productos.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

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

	@Transactional(readOnly = true)
	public List<VariacionCostoDTO> getUltimosCostosGenerales() {
    	// Traemos los últimos 50 movimientos de compra
    	return detallePedidoRepository.obtenerUltimosCostos(EstadoPedido.COMPLETADO, PageRequest.of(0, 50));
	}

	@Transactional(readOnly = true)
	public List<VariacionCostoDTO> buscarCostosPorNombre(String nombre) {
    	// Busca coincidencias parciales (ej: "Bat" encuentra "Batería")
    	return detallePedidoRepository.buscarHistorialPorNombre(nombre, EstadoPedido.COMPLETADO);
	}

    @Transactional(readOnly = true)
    public List<StockInmovilizadoResponse> obtenerStockInmovilizado(int diasMinimos) {
        LocalDateTime fechaLimite = LocalDateTime.now().minusDays(diasMinimos);
        
        // 1. Obtenemos los datos puros de la BD (sin los días calculados)
        List<StockInmovilizadoDTO> resultadosBd = productoRepository.obtenerStockInmovilizado(fechaLimite);

        // 2. Calculamos los días exactos usando Java
        return resultadosBd.stream().map(dto -> {
            Integer diasSinVenta = 9999; // Por defecto (si nunca se vendió)
            
            if (dto.getUltimaVenta() != null) {
                // Restamos HOY menos la Fecha de Última Venta
                diasSinVenta = (int) ChronoUnit.DAYS.between(dto.getUltimaVenta(), LocalDateTime.now());
            }
            
            return new StockInmovilizadoResponse(dto, diasSinVenta);
        }).collect(Collectors.toList());
    }
}