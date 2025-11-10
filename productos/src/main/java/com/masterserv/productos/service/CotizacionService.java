package com.masterserv.productos.service;

// DTOs
import com.masterserv.productos.dto.CotizacionAdminDTO;
import com.masterserv.productos.dto.CotizacionPublicaDTO;
import com.masterserv.productos.dto.ItemOfertaDTO;
import com.masterserv.productos.dto.OfertaProveedorDTO;

// Entidades
import com.masterserv.productos.entity.Cotizacion;
import com.masterserv.productos.entity.DetallePedido;
import com.masterserv.productos.entity.ItemCotizacion;
import com.masterserv.productos.entity.Pedido;

// Enums
import com.masterserv.productos.enums.EstadoCotizacion;
import com.masterserv.productos.enums.EstadoItemCotizacion;
import com.masterserv.productos.enums.EstadoPedido;

// Repositorios
import com.masterserv.productos.repository.CotizacionRepository;
import com.masterserv.productos.repository.ItemCotizacionRepository;
import com.masterserv.productos.repository.PedidoRepository;

// Excepciones y Spring
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Java Utils
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CotizacionService {

    @Autowired
    private CotizacionRepository cotizacionRepository;
    
    @Autowired
    private ItemCotizacionRepository itemCotizacionRepository;

    @Autowired
    private PedidoRepository pedidoRepository;
    
    // (No necesitamos ProductoRepository aquí, ya que el 'itemGanador'
    // ya tiene la referencia al producto)

    // --- MÉTODOS PÚBLICOS (Para el Portal de Proveedor) ---

    /**
     * Busca una cotización por su token público (GET).
     */
    @Transactional(readOnly = true)
    public CotizacionPublicaDTO findCotizacionPublicaByToken(String token) {
        
        Cotizacion cotizacion = cotizacionRepository.findByToken(token)
            .orElseThrow(() -> new EntityNotFoundException("Solicitud de cotización no encontrada o inválida."));

        if (cotizacion.getEstado() != EstadoCotizacion.PENDIENTE_PROVEEDOR) {
            throw new IllegalStateException("Esta solicitud de cotización ya ha sido procesada o ha expirado.");
        }

        return new CotizacionPublicaDTO(cotizacion);
    }
    
    /**
     * Procesa y guarda la oferta enviada por el proveedor (POST).
     */
    @Transactional
    public void submitOfertaProveedor(String token, OfertaProveedorDTO ofertaDTO) {
        
        // 1. Buscar y validar la Cotizacion (Padre)
        Cotizacion cotizacion = cotizacionRepository.findByToken(token)
            .orElseThrow(() -> new EntityNotFoundException("Solicitud de cotización no encontrada o inválida."));

        if (cotizacion.getEstado() != EstadoCotizacion.PENDIENTE_PROVEEDOR) {
            throw new IllegalStateException("Esta solicitud de cotización ya ha sido procesada o ha expirado.");
        }

        // 2. Cargar los items de esta cotización en un Mapa para acceso rápido
        Map<Long, ItemCotizacion> itemsMap = cotizacion.getItems().stream()
            .collect(Collectors.toMap(ItemCotizacion::getId, Function.identity()));

        BigDecimal precioTotalOfertado = BigDecimal.ZERO;

        // 3. Procesar cada item de la oferta
        for (ItemOfertaDTO itemOferta : ofertaDTO.getItems()) {
            
            ItemCotizacion itemDB = itemsMap.get(itemOferta.getItemCotizacionId());
            if (itemDB == null || !itemDB.getCotizacion().getId().equals(cotizacion.getId())) {
                throw new SecurityException("Intento de cotizar un item inválido o que no pertenece a esta solicitud.");
            }

            itemDB.setPrecioUnitarioOfertado(itemOferta.getPrecioUnitarioOfertado());
            itemDB.setEstado(EstadoItemCotizacion.COTIZADO);
            
            precioTotalOfertado = precioTotalOfertado.add(
                itemOferta.getPrecioUnitarioOfertado().multiply(new BigDecimal(itemDB.getCantidadSolicitada()))
            );
        }

        // 4. Actualizar la Cotizacion (Padre)
        cotizacion.setFechaEntregaOfertada(ofertaDTO.getFechaEntregaOfertada());
        cotizacion.setPrecioTotalOfertado(precioTotalOfertado);
        cotizacion.setEstado(EstadoCotizacion.RECIBIDA); // ¡Lista para el Admin!

        // 5. Guardar todo
        cotizacionRepository.save(cotizacion);
    }

    // --- MÉTODOS DE ADMIN (Para el Panel de Control) ---

    /**
     * Busca todas las cotizaciones que el Admin necesita revisar.
     */
    @Transactional(readOnly = true)
    public List<CotizacionAdminDTO> findCotizacionesRecibidas() {
        return cotizacionRepository.findByEstado(EstadoCotizacion.RECIBIDA).stream()
            .map(CotizacionAdminDTO::new)
            .collect(Collectors.toList());
    }

    /**
     * Busca una cotización por ID para la vista de detalle del Admin.
     */
    @Transactional(readOnly = true)
    public CotizacionAdminDTO findCotizacionAdminById(Long id) {
        Cotizacion cotizacion = cotizacionRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Cotización no encontrada: " + id));
        return new CotizacionAdminDTO(cotizacion);
    }

    /**
     * Cancela un solo item (requisito de cancelación parcial).
     */
    @Transactional
    public void cancelarItem(Long itemId) {
        ItemCotizacion item = itemCotizacionRepository.findById(itemId)
            .orElseThrow(() -> new EntityNotFoundException("Item de cotización no encontrado: " + itemId));
        
        if (item.getCotizacion().getEstado() != EstadoCotizacion.RECIBIDA) {
            throw new IllegalStateException("Solo se pueden cancelar items de cotizaciones que estén en estado 'RECIBIDA'.");
        }
        
        item.setEstado(EstadoItemCotizacion.CANCELADO_ADMIN);
        itemCotizacionRepository.save(item);
        
        // Recalculamos el total de la cotización padre
        recalcularTotalCotizacion(item.getCotizacion());
    }

    /**
     * Cancela una cotización completa.
     */
    @Transactional
    public void cancelarCotizacion(Long id) {
        Cotizacion cotizacion = cotizacionRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Cotización no encontrada: " + id));
        
        if (cotizacion.getEstado() != EstadoCotizacion.RECIBIDA) {
            throw new IllegalStateException("Solo se pueden cancelar cotizaciones en estado 'RECIBIDA'.");
        }
        
        cotizacion.setEstado(EstadoCotizacion.CANCELADA_ADMIN);
        cotizacionRepository.save(cotizacion);
    }
    
    /**
     * ¡La acción principal! Confirma la cotización ganadora.
     */
    @Transactional
    public Pedido confirmarCotizacion(Long id) {
        // 1. Obtener la cotización ganadora
        Cotizacion cotizacionGanadora = cotizacionRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Cotización no encontrada: " + id));

        if (cotizacionGanadora.getEstado() != EstadoCotizacion.RECIBIDA) {
            throw new IllegalStateException("Solo se pueden confirmar cotizaciones en estado 'RECIBIDA'.");
        }
        
        // 2. Crear el Pedido (la entidad que ya tenías)
        Pedido pedido = new Pedido();
        pedido.setFechaPedido(LocalDateTime.now());
        pedido.setEstado(EstadoPedido.PENDIENTE); // Pasa a PENDIENTE (de recibir)
        pedido.setProveedor(cotizacionGanadora.getProveedor());
        // (Nota: Faltaría el 'Usuario' admin que confirmó. 
        //  Necesitaríamos pasarlo como argumento desde el controller)
        // pedido.setUsuario(usuarioAdmin); 
        
        Set<DetallePedido> detallesPedido = new HashSet<>();
        BigDecimal totalPedido = BigDecimal.ZERO;

        // 3. Convertir Items de Cotización a Detalles de Pedido
        for (ItemCotizacion itemGanador : cotizacionGanadora.getItems()) {
            
            // ¡Solo añadimos items que estén COTIZADOS (no PENDIENTES o CANCELADOS)!
            if (itemGanador.getEstado() == EstadoItemCotizacion.COTIZADO) {
                
                DetallePedido detalle = new DetallePedido();
                detalle.setPedido(pedido);
                detalle.setProducto(itemGanador.getProducto());
                detalle.setCantidad(itemGanador.getCantidadSolicitada());
                detalle.setPrecioUnitario(itemGanador.getPrecioUnitarioOfertado()); // ¡El precio de COSTO!
                
                detallesPedido.add(detalle);
                totalPedido = totalPedido.add(
                    itemGanador.getPrecioUnitarioOfertado().multiply(new BigDecimal(itemGanador.getCantidadSolicitada()))
                );
            }
        }
        
        if (detallesPedido.isEmpty()) {
            throw new IllegalStateException("No se puede confirmar una cotización sin items válidos (cotizados).");
        }

        pedido.setDetalles(detallesPedido);
        pedido.setTotalPedido(totalPedido);

        // 4. Guardar el nuevo Pedido
        Pedido pedidoGuardado = pedidoRepository.save(pedido);

        // 5. Actualizar estados de las cotizaciones
        cotizacionGanadora.setEstado(EstadoCotizacion.CONFIRMADA_ADMIN);
        cotizacionRepository.save(cotizacionGanadora);
        
        // (Futura mejora: buscar y cancelar las otras cotizaciones competidoras)
        
        return pedidoGuardado;
    }
    
    /**
     * Helper para recalcular el total de una cotización si se cancela un item.
     */
    private void recalcularTotalCotizacion(Cotizacion cotizacion) {
        BigDecimal nuevoTotal = BigDecimal.ZERO;
        for (ItemCotizacion item : cotizacion.getItems()) {
            // Suma solo si el item fue cotizado (tiene precio) Y no está cancelado
            if (item.getEstado() == EstadoItemCotizacion.COTIZADO && item.getPrecioUnitarioOfertado() != null) {
                nuevoTotal = nuevoTotal.add(
                    item.getPrecioUnitarioOfertado().multiply(new BigDecimal(item.getCantidadSolicitada()))
                );
            }
        }
        cotizacion.setPrecioTotalOfertado(nuevoTotal);
        cotizacionRepository.save(cotizacion);
    }
}