package com.masterserv.productos.service;

import com.masterserv.productos.dto.CuponDTO;
import com.masterserv.productos.dto.RecompensaDTO;
import com.masterserv.productos.dto.SaldoPuntosDTO;
import com.masterserv.productos.entity.*;
import com.masterserv.productos.enums.EstadoCupon;
import com.masterserv.productos.enums.EstadoVenta;
import com.masterserv.productos.enums.TipoDescuento;
import com.masterserv.productos.enums.TipoMovimientoPuntos;
import com.masterserv.productos.mapper.CuponMapper;
import com.masterserv.productos.mapper.RecompensaMapper;
import com.masterserv.productos.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public CuponDTO canjearPuntos(String clienteEmail, Long recompensaId) {
        
        // 1. Validar la recompensa
        Recompensa recompensa = recompensaRepository.findById(recompensaId)
                .orElseThrow(() -> new RuntimeException("La recompensa seleccionada no existe."));
        
        int puntosRequeridos = recompensa.getPuntosRequeridos();

        // 2. Validar la cuenta y el saldo
        CuentaPuntos cuenta = cuentaPuntosRepository.findByCliente_Email(clienteEmail)
                .orElseThrow(() -> new RuntimeException("No se encontró una cuenta de puntos para el cliente: " + clienteEmail));
        
        if (cuenta.getSaldoPuntos() < puntosRequeridos) {
            throw new IllegalArgumentException(String.format(
                "Saldo insuficiente. Saldo actual: %d puntos, Puntos requeridos: %d puntos.",
                cuenta.getSaldoPuntos(), puntosRequeridos
            ));
        }
        
        // 3. Crear el Movimiento (restar puntos)
        MovimientoPuntos movimiento = new MovimientoPuntos();
        movimiento.setPuntos(-puntosRequeridos);
        movimiento.setTipoMovimiento(TipoMovimientoPuntos.CANJEADO);
        movimiento.setDescripcion(String.format(
            "Canje por recompensa: %s", recompensa.getDescripcion()
        ));
        movimiento.setCuentaPuntos(cuenta);
        
        // 4. Actualizar el saldo de la cuenta
        cuenta.setSaldoPuntos(cuenta.getSaldoPuntos() - puntosRequeridos);
        
        // 5. Crear el Cupón basado en la Recompensa
        Cupon cupon = new Cupon();
        cupon.setCliente(cuenta.getCliente());
        cupon.setCodigo(generarCodigoCupon(cuenta.getCliente().getId()));
        cupon.setValor(recompensa.getValor());
        cupon.setTipoDescuento(recompensa.getTipoDescuento());
        cupon.setCategoria(recompensa.getCategoria()); // Asigna la categoría (o null)
        cupon.setEstado(EstadoCupon.VIGENTE);
        cupon.setFechaVencimiento(LocalDate.now().plusDays(90)); // 90 días de validez
        
        // 6. Guardar todo
        cuentaPuntosRepository.save(cuenta);
        movimientoPuntosRepository.save(movimiento);
        Cupon cuponGuardado = cuponRepository.save(cupon);
        
        return cuponMapper.toCuponDTO(cuponGuardado);
    }
    
    /**
     * Helper para generar un código de cupón único y legible.
     */
    private String generarCodigoCupon(Long clienteId) {
        String randomChars = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return String.format("CANJE-%d-%s", clienteId, randomChars);
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
}