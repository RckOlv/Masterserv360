package com.masterserv.productos.service;

import com.masterserv.productos.dto.DashboardStatsDTO;
import com.masterserv.productos.repository.ProductoRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import com.masterserv.productos.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // ¡Añadir!

import java.math.BigDecimal;

@Service
public class DashboardService { // (Tu archivo ya existe)

    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private VentaRepository ventaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true) // ¡Añadir!
    public DashboardStatsDTO getEstadisticas() {
        
        long productosBajoStock = productoRepository.countProductosBajoStock();
        BigDecimal totalVentasMes = ventaRepository.sumTotalVentasMesActual();
        long clientesActivos = usuarioRepository.countClientesActivos();
        
        DashboardStatsDTO dto = new DashboardStatsDTO();
        dto.setProductosBajoStock(productosBajoStock);
        // Si no hay ventas, sum() devuelve null. Lo convertimos a CERO.
        dto.setTotalVentasMes(totalVentasMes != null ? totalVentasMes : BigDecimal.ZERO);
        dto.setClientesActivos(clientesActivos);
        
        return dto;
    }
}