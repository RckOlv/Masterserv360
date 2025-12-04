package com.masterserv.productos.service;

import com.masterserv.productos.dto.DashboardStatsDTO;
import com.masterserv.productos.dto.TopProductoDTO;
import com.masterserv.productos.dto.VentasPorCategoriaDTO;
import com.masterserv.productos.dto.VentasPorDiaDTO;
import com.masterserv.productos.repository.ProductoRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import com.masterserv.productos.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class DashboardService {

    @Autowired
    private VentaRepository ventaRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    // --- MÉTODO ORIGINAL (Sin filtros, usa mes actual) ---
    public DashboardStatsDTO getEstadisticas() {
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMes = LocalDate.now().plusMonths(1).withDayOfMonth(1).atStartOfDay().minusNanos(1);
        return getStats(inicioMes, finMes);
    }

    // --- MÉTODO NUEVO (Con filtros personalizados) ---
    public DashboardStatsDTO getEstadisticasFiltradas(LocalDate inicio, LocalDate fin) {
        LocalDateTime fechaInicio = (inicio != null) ? inicio.atStartOfDay() : LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime fechaFin = (fin != null) ? fin.atTime(LocalTime.MAX) : LocalDate.now().atTime(LocalTime.MAX);
        return getStats(fechaInicio, fechaFin);
    }

    // Lógica común para calcular stats
    private DashboardStatsDTO getStats(LocalDateTime inicio, LocalDateTime fin) {
        // Ventas en el rango seleccionado
        BigDecimal totalVentasRango = ventaRepository.findTotalVentasEntreFechas(inicio, fin)
                .orElse(BigDecimal.ZERO);
        
        // Ventas de HOY (siempre fijo para KPI rápido)
        LocalDateTime inicioHoy = LocalDate.now().atStartOfDay();
        LocalDateTime finHoy = LocalDate.now().atTime(LocalTime.MAX);
        BigDecimal totalVentasHoy = ventaRepository.findTotalVentasEntreFechas(inicioHoy, finHoy)
                .orElse(BigDecimal.ZERO);

        long productosBajoStock = productoRepository.countProductosBajoStock();
        long clientesActivos = usuarioRepository.countClientesActivos();

        DashboardStatsDTO dto = new DashboardStatsDTO();
        dto.setTotalVentasMes(totalVentasRango); 
        dto.setProductosBajoStock(productosBajoStock);
        dto.setClientesActivos(clientesActivos);
        dto.setTotalVentasHoy(totalVentasHoy);
        
        return dto;
    }

    // --- GRÁFICO: VENTAS POR RANGO (Líneas) ---
    public List<VentasPorDiaDTO> getVentasPorRango(LocalDate inicio, LocalDate fin) {
        LocalDateTime fechaInicio = (inicio != null) ? inicio.atStartOfDay() : LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime fechaFin = (fin != null) ? fin.atTime(LocalTime.MAX) : LocalDate.now().atTime(LocalTime.MAX);
        
        return ventaRepository.findVentasSumarizadasPorDia(fechaInicio, fechaFin);
    }
    
    // --- TOP PRODUCTOS POR RANGO (Barras) ---
    public List<TopProductoDTO> getTopProductosPorRango(LocalDate inicio, LocalDate fin) {
        LocalDateTime fechaInicio = (inicio != null) ? inicio.atStartOfDay() : LocalDate.now().withDayOfMonth(1).atStartOfDay();
        // Nota: El repositorio actualmente solo filtra por fechaInicio, idealmente debería filtrar por rango también,
        // pero mantenemos la lógica actual para no romper.
        return ventaRepository.findTop5ProductosVendidos(fechaInicio);
    }

    // --- GRÁFICO: VENTAS POR CATEGORÍA (Dona) ---
    public List<VentasPorCategoriaDTO> getVentasPorCategoria(LocalDate inicio, LocalDate fin) {
        LocalDateTime fechaInicio = (inicio != null) ? inicio.atStartOfDay() : LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime fechaFin = (fin != null) ? fin.atTime(LocalTime.MAX) : LocalDate.now().atTime(LocalTime.MAX);
        
        return ventaRepository.findVentasPorCategoria(fechaInicio, fechaFin);
    }
    
    // --- MÉTODOS LEGACY ---
    public List<VentasPorDiaDTO> getVentasUltimos7Dias() {
        return getVentasPorRango(LocalDate.now().minusDays(7), LocalDate.now());
    }

    public List<TopProductoDTO> getTop5ProductosDelMes() {
        return getTopProductosPorRango(LocalDate.now().withDayOfMonth(1), LocalDate.now());
    }
}