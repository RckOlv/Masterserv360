package com.masterserv.productos.service;

import com.masterserv.productos.dto.DashboardStatsDTO;
import com.masterserv.productos.dto.TopProductoDTO;
import com.masterserv.productos.dto.VentasPorDiaDTO;
import com.masterserv.productos.enums.EstadoUsuario; // Asegúrate de tener este import
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
import java.util.Optional; // Mentor: Importar Optional

@Service
public class DashboardService {

    @Autowired
    private VentaRepository ventaRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    public DashboardStatsDTO getEstadisticas() {
     // 1. Fechas para los filtros
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMes = LocalDate.now().plusMonths(1).withDayOfMonth(1).atStartOfDay().minusNanos(1);
        LocalDateTime inicioHoy = LocalDate.now().atStartOfDay();
        LocalDateTime finHoy = LocalDate.now().atTime(LocalTime.MAX);

        // 2. Consultas
        
        // --- Mentor: ASEGÚRATE DE QUE ESTAS LÍNEAS ESTÉN ASÍ ---
        BigDecimal totalVentasMes = ventaRepository.findTotalVentasEntreFechas(inicioMes, finMes)
                .orElse(BigDecimal.ZERO);
        
        BigDecimal totalVentasHoy = ventaRepository.findTotalVentasEntreFechas(inicioHoy, finHoy)
                .orElse(BigDecimal.ZERO);
        
        long productosBajoStock = productoRepository.countProductosBajoStock(); // Asumo que este método SÍ existe
        
        long clientesActivos = usuarioRepository.countClientesActivos(); // Usamos el método que SÍ existe
        // --- Mentor: FIN DE LA CORRECCIÓN ---

        // 3. Construir DTO
        DashboardStatsDTO dto = new DashboardStatsDTO();
        dto.setTotalVentasMes(totalVentasMes);
        dto.setProductosBajoStock(productosBajoStock);
        dto.setClientesActivos(clientesActivos);
        dto.setTotalVentasHoy(totalVentasHoy); // Esta línea ahora funcionará
        
        return dto;
    }

    /**
     * Devuelve el total de ventas de los últimos 7 días.
     */
    public List<VentasPorDiaDTO> getVentasUltimos7Dias() {
        LocalDateTime hace7Dias = LocalDate.now().minusDays(7).atStartOfDay();
        return ventaRepository.findVentasSumarizadasPorDia(hace7Dias);
    }

    /**
     * Devuelve los 5 productos más vendidos este mes.
     */
    public List<TopProductoDTO> getTop5ProductosDelMes() {
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return ventaRepository.findTop5ProductosVendidos(inicioMes);
    }
}