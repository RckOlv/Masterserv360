package com.masterserv.productos.service;

import com.masterserv.productos.dto.CuponDTO;
import com.masterserv.productos.dto.RecompensaDTO;
import com.masterserv.productos.dto.SaldoPuntosDTO;
import com.masterserv.productos.entity.*;
import com.masterserv.productos.enums.EstadoCupon;
import com.masterserv.productos.enums.EstadoVenta;
import com.masterserv.productos.enums.TipoMovimientoPuntos;
import com.masterserv.productos.mapper.CuponMapper;
import com.masterserv.productos.mapper.RecompensaMapper;
import com.masterserv.productos.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.masterserv.productos.dto.ClienteFidelidadDTO; 
import jakarta.persistence.EntityNotFoundException; 
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PuntosService {

    @Autowired
    private ReglaPuntosService reglaPuntosService;
    
    @Autowired
    private CuentaPuntosRepository cuentaPuntosRepository;
    
    @Autowired
    private MovimientoPuntosRepository movimientoPuntosRepository;

    @Autowired 
    private CuponRepository cuponRepository;

    @Autowired
    private UsuarioRepository usuarioRepository; 
    
    @Autowired
    private CuponMapper cuponMapper; 
    
    @Autowired 
    private RecompensaRepository recompensaRepository;
    
    @Autowired 
    private RecompensaMapper recompensaMapper;
    
    @Autowired
    private CuponService cuponService; 

    /**
     * Lógica principal para asignar puntos después de una venta.
     */
    @Transactional 
    public void asignarPuntosPorVenta(Venta venta) {
        
        Optional<ReglaPuntos> reglaOpt = reglaPuntosService.getReglaActiva();
        if (reglaOpt.isEmpty()) {
            System.err.println("WARN: No se asignaron puntos para la Venta #" + venta.getId() + 
                                ". Motivo: No hay regla de puntos ACTIVA configurada.");
            return; 
        }
        ReglaPuntos regla = reglaOpt.get();
        Usuario cliente = venta.getCliente();
        if (cliente == null) {
            System.err.println("WARN: No se asignaron puntos. La Venta #" + venta.getId() + " no tiene cliente asociado.");
            return;
        }
        CuentaPuntos cuenta = cuentaPuntosRepository.findByCliente(cliente)
                .orElseGet(() -> {
                    CuentaPuntos nuevaCuenta = new CuentaPuntos();
                    nuevaCuenta.setCliente(cliente);
                    nuevaCuenta.setSaldoPuntos(0);
                    return cuentaPuntosRepository.save(nuevaCuenta);
                });
        
        int puntosGanados = calcularPuntos(venta.getTotalVenta(), regla);
        
        if (puntosGanados <= 0) {
            return;
        }
        
        MovimientoPuntos movimiento = new MovimientoPuntos();
        movimiento.setPuntos(puntosGanados);
        movimiento.setTipoMovimiento(TipoMovimientoPuntos.GANADO);
        movimiento.setDescripcion("Puntos ganados por Venta #" + venta.getId());
        movimiento.setCuentaPuntos(cuenta);
        movimiento.setVenta(venta);
        
        if (regla.getCaducidadPuntosMeses() != null && regla.getCaducidadPuntosMeses() > 0) {
            movimiento.setFechaCaducidadPuntos(
                LocalDateTime.now().plusMonths(regla.getCaducidadPuntosMeses())
            );
        }
        
        cuenta.setSaldoPuntos(cuenta.getSaldoPuntos() + puntosGanados);
        
        cuentaPuntosRepository.save(cuenta);
        movimientoPuntosRepository.save(movimiento);
    }

    /**
     * Helper para calcular los puntos según la regla.
     */
    private int calcularPuntos(BigDecimal totalVenta, ReglaPuntos regla) {
        if (regla.getMontoGasto() == null || regla.getMontoGasto().compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        BigDecimal puntosDecimal = totalVenta.divide(regla.getMontoGasto(), 0, RoundingMode.FLOOR);
        return puntosDecimal.multiply(new BigDecimal(regla.getPuntosGanados())).intValue();
    }
    
    /**
     * Revierte los puntos ganados por una Venta que ha sido CANCELADA.
     */
    @Transactional
    public void revertirPuntosPorVenta(Venta venta) {
        if (venta.getEstado() != EstadoVenta.CANCELADA) {
            System.err.println("WARN: Se intentó revertir puntos de una venta no cancelada. Venta #" + venta.getId());
            return;
        }
        
        boolean yaRevertido = movimientoPuntosRepository.existsByVentaAndTipoMovimiento(
            venta, TipoMovimientoPuntos.REVERSION
        );
        
        if (yaRevertido) {
            System.out.println("INFO: Los puntos para la Venta #" + venta.getId() + " ya habían sido revertidos.");
            return;
        }
        
        Optional<MovimientoPuntos> movOriginalOpt = movimientoPuntosRepository.findByVentaAndTipoMovimiento(
            venta, TipoMovimientoPuntos.GANADO
        );
        
        if (movOriginalOpt.isEmpty()) {
            System.out.println("INFO: La Venta #" + venta.getId() + " no generó puntos. No se revierte nada.");
            return;
        }
        
        MovimientoPuntos movOriginal = movOriginalOpt.get();
        CuentaPuntos cuenta = movOriginal.getCuentaPuntos();
        int puntosARevertir = movOriginal.getPuntos();
        
        MovimientoPuntos movReversion = new MovimientoPuntos();
        movReversion.setPuntos(-puntosARevertir);
        movReversion.setTipoMovimiento(TipoMovimientoPuntos.REVERSION);
        movReversion.setDescripcion("Reversión por cancelación Venta #" + venta.getId());
        movReversion.setCuentaPuntos(cuenta);
        movReversion.setVenta(venta); 
        
        cuenta.setSaldoPuntos(cuenta.getSaldoPuntos() - puntosARevertir);
        
        cuentaPuntosRepository.save(cuenta);
        movimientoPuntosRepository.save(movReversion);
        
        System.out.println("INFO: Revertidos " + puntosARevertir + " puntos de la Venta #" + venta.getId());
    }

    /**
     * Canjea puntos de un cliente por una Recompensa específica (que se convierte en Cupón).
     * REFACTORIZADO: Usa CuponService para generar el cupón.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public CuponDTO canjearPuntos(String clienteEmail, Long recompensaId) {
        
        // 1. Validar la recompensa
        Recompensa recompensa = recompensaRepository.findById(recompensaId)
                .orElseThrow(() -> new RuntimeException("La recompensa seleccionada no existe."));
        
        // --- VALIDACIÓN DE STOCK ---
        if (recompensa.getStock() <= 0) {
            throw new RuntimeException("Lo sentimos, esta recompensa se ha agotado (Stock 0).");
        }

        int puntosRequeridos = recompensa.getPuntosRequeridos();

        // 2. Validar la cuenta y el saldo del cliente
        CuentaPuntos cuenta = cuentaPuntosRepository.findByCliente_Email(clienteEmail)
                .orElseThrow(() -> new RuntimeException("No se encontró una cuenta de puntos para el cliente: " + clienteEmail));
        
        if (cuenta.getSaldoPuntos() < puntosRequeridos) {
            throw new IllegalArgumentException(String.format(
                "Saldo insuficiente. Tienes %d puntos, necesitas %d.",
                cuenta.getSaldoPuntos(), puntosRequeridos
            ));
        }
        
        // --- DESCONTAR STOCK ---
        recompensa.setStock(recompensa.getStock() - 1);
        recompensaRepository.save(recompensa); // Guardamos el nuevo stock en la BD

        // 3. Crear el Movimiento (restar puntos al usuario)
        MovimientoPuntos movimiento = new MovimientoPuntos();
        movimiento.setPuntos(-puntosRequeridos);
        movimiento.setTipoMovimiento(TipoMovimientoPuntos.CANJEADO);
        movimiento.setDescripcion(String.format(
            "Canje por recompensa: %s", recompensa.getDescripcion()
        ));
        movimiento.setCuentaPuntos(cuenta);
        
        // 4. Actualizar el saldo de la cuenta
        cuenta.setSaldoPuntos(cuenta.getSaldoPuntos() - puntosRequeridos);
        
        // 5. Guardar cambios en puntos
        cuentaPuntosRepository.save(cuenta);
        movimientoPuntosRepository.save(movimiento);
        
        // --- 🔥 REFACTOR: DELEGAMOS CREACIÓN AL EXPERTO ---
        Cupon cuponGuardado = cuponService.crearCuponPorCanje(cuenta.getCliente(), recompensa);
        // -------------------------------------------------
        
        return cuponMapper.toCuponDTO(cuponGuardado);
    }
    
    /**
     * Obtiene el saldo de puntos y su equivalencia en dinero para un cliente específico.
     * También incluye la lista de recompensas disponibles (GLOBALES).
     */
    @Transactional(readOnly = true) 
    public SaldoPuntosDTO getSaldoByEmail(String clienteEmail) {
        SaldoPuntosDTO saldoDTO = new SaldoPuntosDTO();
        
        // 1. Buscar la cuenta de puntos
        Optional<CuentaPuntos> cuentaOpt = cuentaPuntosRepository.findByCliente_Email(clienteEmail);
        
        if (cuentaOpt.isEmpty()) {
            saldoDTO.setSaldoPuntos(0);
            saldoDTO.setValorMonetario(BigDecimal.ZERO);
            saldoDTO.setEquivalenciaActual("N/A");
            saldoDTO.setRecompensasDisponibles(new ArrayList<>()); 
            return saldoDTO;
        }
        
        CuentaPuntos cuenta = cuentaOpt.get();
        saldoDTO.setSaldoPuntos(cuenta.getSaldoPuntos());
        
        // 2. Buscar la regla activa (SOLO para calcular el valor monetario)
        Optional<ReglaPuntos> reglaOpt = reglaPuntosService.getReglaActiva();
        
        if (reglaOpt.isPresent()) {
            ReglaPuntos regla = reglaOpt.get();
            if (regla.getEquivalenciaPuntos() != null && regla.getEquivalenciaPuntos().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal valorMonetario = regla.getEquivalenciaPuntos()
                        .multiply(new BigDecimal(cuenta.getSaldoPuntos()));
                valorMonetario = valorMonetario.setScale(2, RoundingMode.HALF_UP);
                saldoDTO.setValorMonetario(valorMonetario);
                
                String equivalenciaStr = String.format("1 Punto = $%.2f ARS", regla.getEquivalenciaPuntos());
                saldoDTO.setEquivalenciaActual(equivalenciaStr);
            } else {
                saldoDTO.setValorMonetario(BigDecimal.ZERO);
                saldoDTO.setEquivalenciaActual("Ver catálogo de canjes");
            }
        } else {
            saldoDTO.setValorMonetario(BigDecimal.ZERO);
            saldoDTO.setEquivalenciaActual("Sin regla activa");
        }

        // 3. Cargar recompensas (INDEPENDIENTE DE LA REGLA)
        // Buscamos todas las que tengan stock y estén activas
        List<Recompensa> recompensasActivas = recompensaRepository.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getActivo()) && r.getStock() > 0)
                .collect(Collectors.toList());

        List<RecompensaDTO> recompensasDTO = recompensasActivas.stream()
                .map(recompensaMapper::toDto)
                .collect(Collectors.toList());
        
        saldoDTO.setRecompensasDisponibles(recompensasDTO);

        return saldoDTO;
    }

    @Transactional(readOnly = true)
    public ClienteFidelidadDTO obtenerInfoFidelidadCliente(Long clienteId) {
        
        // 1. Buscar Cliente y Cuenta (Igual que antes)
        Usuario cliente = usuarioRepository.findById(clienteId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + clienteId));

        CuentaPuntos cuenta = cuentaPuntosRepository.findByCliente(cliente)
                .orElse(null);
        
        int saldoPuntos = (cuenta != null) ? cuenta.getSaldoPuntos() : 0;

        // 2. Buscar Cupones VIGENTES (Igual que antes)
        List<Cupon> cuponesVigentes = cuponRepository.findByCliente_IdAndEstadoOrderByFechaVencimientoAsc(
                clienteId, EstadoCupon.VIGENTE
        );
        List<CuponDTO> cuponesDto = cuponesVigentes.stream()
                .map(cuponMapper::toCuponDTO)
                .toList();

        // 3. Calcular Equivalencia (Igual que antes)
        String equivalencia = "Sin valor";
        Optional<ReglaPuntos> reglaOpt = reglaPuntosService.getReglaActiva();
        // ... (Tu lógica de equivalencia monetaria se mantiene igual) ...
        if (reglaOpt.isPresent() && saldoPuntos > 0) {
             // ... lógica de cálculo de $ ...
             ReglaPuntos regla = reglaOpt.get();
             if (regla.getEquivalenciaPuntos() != null && regla.getEquivalenciaPuntos().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal valor = regla.getEquivalenciaPuntos().multiply(new BigDecimal(saldoPuntos));
                valor = valor.setScale(2, RoundingMode.HALF_UP);
                equivalencia = String.format("$%,.0f", valor); 
            }
        }

        // --- 🔥 4. BUSCAR RECOMPENSAS ALCANZABLES ---
        // Buscamos todas las activas con stock y filtramos las que el cliente puede pagar
        List<Recompensa> recompensasCandidatas = recompensaRepository.findAll(); 
        
        List<RecompensaDTO> recompensasAlcanzables = recompensasCandidatas.stream()
            .filter(r -> Boolean.TRUE.equals(r.getActivo()) && r.getStock() > 0) // Que existan
            .filter(r -> saldoPuntos >= r.getPuntosRequeridos()) // Que le alcance el saldo
            .map(recompensaMapper::toDto)
            .collect(Collectors.toList());
        // ---------------------------------------------

        // 5. Armar respuesta
        ClienteFidelidadDTO dto = new ClienteFidelidadDTO();
        dto.setClienteId(cliente.getId());
        dto.setNombreCompleto(cliente.getNombre() + " " + cliente.getApellido());
        dto.setPuntosAcumulados(saldoPuntos);
        dto.setEquivalenciaMonetaria(equivalencia);
        dto.setCuponesDisponibles(cuponesDto);
        dto.setRecompensasAlcanzables(recompensasAlcanzables); // <--- Seteamos la nueva lista

        return dto;
    }
}