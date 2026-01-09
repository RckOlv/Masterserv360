package com.masterserv.productos.service;

import com.masterserv.productos.dto.CuponDTO;
import com.masterserv.productos.entity.Cupon;
import com.masterserv.productos.entity.Recompensa;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.entity.Venta;
import com.masterserv.productos.enums.EstadoCupon;
import com.masterserv.productos.enums.TipoDescuento;
import com.masterserv.productos.exceptions.CuponNoValidoException;
import com.masterserv.productos.mapper.CuponMapper;
import com.masterserv.productos.repository.CuponRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class CuponService {

    @Autowired
    private CuponRepository cuponRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private CuponMapper cuponMapper;

    /**
     * Genera un cupón MANUALMENTE (sin restar puntos).
     * Útil para atención al cliente, sorteos o regalos del administrador.
     */
    @Transactional
    public CuponDTO crearCuponManual(Long usuarioId, BigDecimal valor, TipoDescuento tipo, int diasValidez, String motivo) {
        
        // 1. Buscar al cliente
        Usuario cliente = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado para asignar el cupón manual."));

        // 2. Crear la entidad Cupón
        Cupon cupon = new Cupon();
        cupon.setCliente(cliente);
        cupon.setValor(valor);
        cupon.setTipoDescuento(tipo); // Enum: PORCENTAJE o FIJO
        cupon.setEstado(EstadoCupon.VIGENTE);
        cupon.setFechaVencimiento(LocalDate.now().plusDays(diasValidez));
        
        // Los cupones manuales no suelen tener categoría específica (sirven para todo)
        cupon.setCategoria(null); 
        
        // 3. Generar código único (Prefijo 'ADM' para identificar que fue manual)
        String codigo = "ADM-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        cupon.setCodigo(codigo);

        // 4. Guardar y devolver DTO
        Cupon cuponGuardado = cuponRepository.save(cupon);
        return cuponMapper.toCuponDTO(cuponGuardado);
    }

    // --- MÉTODOS EXISTENTES (USADOS EN VENTAS) ---

    /**
     * Valida si un cupón es legítimo, vigente y pertenece al cliente.
     * NO cambia el estado del cupón, solo lo valida.
     */
    @Transactional(readOnly = true) 
    public Cupon validarCupon(String codigoCupon, Usuario cliente) {
        
        // 1. Buscar el cupón
        Cupon cupon = cuponRepository.findByCodigo(codigoCupon)
                .orElseThrow(() -> new CuponNoValidoException("El cupón '" + codigoCupon + "' no existe."));

        // 2. Validar Estado
        if (cupon.getEstado() != EstadoCupon.VIGENTE) {
            throw new CuponNoValidoException("El cupón '" + codigoCupon + "' ya fue " + cupon.getEstado() + ".");
        }

        // 3. Validar Fecha de Vencimiento
        if (cupon.getFechaVencimiento().isBefore(LocalDate.now())) {
            throw new CuponNoValidoException("El cupón '" + codigoCupon + "' ha vencido.");
        }

        // 4. ¡Validación de Seguridad! (Que el cupón sea de ESTE cliente)
        if (!cupon.getCliente().getId().equals(cliente.getId())) {
            throw new CuponNoValidoException("Este cupón no pertenece a este cliente.");
        }

        return cupon;
    }

    /**
     * "Quema" el cupón (lo marca como USADO) y lo asocia a la Venta.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void marcarCuponComoUsado(Cupon cupon, Venta venta) {
        cupon.setEstado(EstadoCupon.USADO);
        cupon.setVenta(venta);
        // Al estar en una transacción, Hibernate guarda esto automáticamente al final.
    }

    @Transactional(readOnly = true)
    public List<CuponDTO> obtenerCuponesPorUsuario(String email) {
        // Usamos el método optimizado del repositorio
        List<Cupon> cupones = cuponRepository.findByCliente_EmailOrderByFechaVencimientoDesc(email);
        
        // Convertimos a DTO usando el mapper
        return cupones.stream()
                .map(cuponMapper::toCuponDTO)
                .toList();
    }

    /**
     * 🔥 MÉTODO CENTRALIZADO (DRY)
     * Crea un cupón de recompensa estandarizado.
     * Usado por: PuntosService (Web) y ChatbotService (WhatsApp).
     */
    @Transactional(propagation = Propagation.MANDATORY) // Requiere transacción activa del llamador
    public Cupon crearCuponPorCanje(Usuario cliente, Recompensa recompensa) {
        Cupon cupon = new Cupon();
        cupon.setCliente(cliente);
        
        // 1. Generación de Código Única (Formato: CANJE-IDCLIENTE-RANDOM)
        String randomSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String codigo = String.format("CANJE-%d-%s", cliente.getId(), randomSuffix);
        cupon.setCodigo(codigo);

        // 2. Datos de la Recompensa
        cupon.setValor(recompensa.getValor());
        cupon.setTipoDescuento(recompensa.getTipoDescuento());
        cupon.setCategoria(recompensa.getCategoria());
        
        // 3. Reglas de Negocio Únicas (Vencimiento y Estado)
        cupon.setEstado(EstadoCupon.VIGENTE);
        cupon.setFechaVencimiento(LocalDate.now().plusDays(90)); // Unificamos a 90 días para todos

        return cuponRepository.save(cupon);
    }
}