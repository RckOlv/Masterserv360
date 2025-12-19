package com.masterserv.productos.service;

import com.masterserv.productos.dto.DashboardStatsDTO;
import com.masterserv.productos.dto.TopProductoDTO;
import com.masterserv.productos.dto.VentasPorCategoriaDTO;
import com.masterserv.productos.dto.VentasPorDiaDTO;
import com.masterserv.productos.enums.EstadoPedido; 
import com.masterserv.productos.repository.PedidoRepository; 
import com.masterserv.productos.repository.ProductoRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import com.masterserv.productos.repository.VentaRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit; 
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private VentaRepository ventaRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PedidoRepository pedidoRepository; 

    // --- MÉTODO ORIGINAL ---
    public DashboardStatsDTO getEstadisticas() {
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMes = LocalDate.now().plusMonths(1).withDayOfMonth(1).atStartOfDay().minusNanos(1);
        return getStats(inicioMes, finMes);
    }

    // --- MÉTODO CON FILTROS ---
    public DashboardStatsDTO getEstadisticasFiltradas(LocalDate inicio, LocalDate fin) {
        LocalDateTime fechaInicio = (inicio != null) ? inicio.atStartOfDay() : LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime fechaFin = (fin != null) ? fin.atTime(LocalTime.MAX) : LocalDate.now().atTime(LocalTime.MAX);
        return getStats(fechaInicio, fechaFin);
    }

    // Lógica común para calcular stats
    private DashboardStatsDTO getStats(LocalDateTime inicio, LocalDateTime fin) {
        // 1. Ventas en el rango seleccionado ($)
        BigDecimal totalVentasRango = ventaRepository.findTotalVentasEntreFechas(inicio, fin)
                .orElse(BigDecimal.ZERO);
        
        // 2. Cantidad de Ventas (#) - NUEVO DATO
        long cantidadVentas = ventaRepository.countVentasEntreFechas(inicio, fin);
        
        // 3. Ventas de HOY (KPI rápido)
        LocalDateTime inicioHoy = LocalDate.now().atStartOfDay();
        LocalDateTime finHoy = LocalDate.now().atTime(LocalTime.MAX);
        BigDecimal totalVentasHoy = ventaRepository.findTotalVentasEntreFechas(inicioHoy, finHoy)
                .orElse(BigDecimal.ZERO);

        // 4. Contadores básicos
        long productosBajoStock = productoRepository.countProductosBajoStock();
        long clientesActivos = usuarioRepository.countClientesActivos();

        DashboardStatsDTO dto = new DashboardStatsDTO();
        dto.setTotalVentasMes(totalVentasRango); 
        dto.setCantidadVentasPeriodo(cantidadVentas); // <--- Asignamos el nuevo dato
        dto.setProductosBajoStock(productosBajoStock);
        dto.setClientesActivos(clientesActivos);
        dto.setTotalVentasHoy(totalVentasHoy);
        
        // 5. OBTENER PEDIDOS EN CAMINO
        List<DashboardStatsDTO.PedidoEnCaminoDTO> enCamino = pedidoRepository
            .findByEstado(EstadoPedido.EN_CAMINO) 
            .stream()
            .map(p -> {
                DashboardStatsDTO.PedidoEnCaminoDTO item = new DashboardStatsDTO.PedidoEnCaminoDTO();
                if (p.getProveedor() != null) {
                    item.setProveedor(p.getProveedor().getRazonSocial());
                } else {
                    item.setProveedor("Proveedor Desconocido");
                }
                item.setFechaEntrega(p.getFechaEntregaEstimada());
                
                if (p.getFechaEntregaEstimada() != null) {
                    long dias = ChronoUnit.DAYS.between(LocalDate.now(), p.getFechaEntregaEstimada());
                    item.setDiasRestantes(dias);
                } else {
                    item.setDiasRestantes(0L);
                }
                return item;
            })
            .sorted((p1, p2) -> {
                if(p1.getFechaEntrega() == null) return 1;
                if(p2.getFechaEntrega() == null) return -1;
                return p1.getFechaEntrega().compareTo(p2.getFechaEntrega());
            })
            .limit(5)
            .collect(Collectors.toList());

        dto.setPedidosEnCamino(enCamino);
        
        return dto;
    }

    // --- GRÁFICO: VENTAS POR RANGO ---
    public List<VentasPorDiaDTO> getVentasPorRango(LocalDate inicio, LocalDate fin) {
        LocalDateTime fechaInicio = (inicio != null) ? inicio.atStartOfDay() : LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime fechaFin = (fin != null) ? fin.atTime(LocalTime.MAX) : LocalDate.now().atTime(LocalTime.MAX);
        return ventaRepository.findVentasSumarizadasPorDia(fechaInicio, fechaFin);
    }
    
    // --- TOP PRODUCTOS ---
    public List<TopProductoDTO> getTopProductosPorRango(LocalDate inicio, LocalDate fin) {
        LocalDateTime fechaInicio = (inicio != null) ? inicio.atStartOfDay() : LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return ventaRepository.findTop5ProductosVendidos(fechaInicio);
    }

    // --- GRÁFICO: VENTAS POR CATEGORÍA ---
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