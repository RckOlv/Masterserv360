package com.masterserv.productos.service;

import com.masterserv.productos.dto.CotizacionAdminDTO;
import com.masterserv.productos.dto.CotizacionPublicaDTO;
import com.masterserv.productos.dto.ItemOfertaDTO;
import com.masterserv.productos.dto.OfertaProveedorDTO;
import com.masterserv.productos.entity.Cotizacion;
import com.masterserv.productos.entity.DetallePedido;
import com.masterserv.productos.entity.ItemCotizacion;
import com.masterserv.productos.entity.Pedido;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.enums.EstadoCotizacion;
import com.masterserv.productos.enums.EstadoItemCotizacion;
import com.masterserv.productos.enums.EstadoPedido;
import com.masterserv.productos.repository.CotizacionRepository;
import com.masterserv.productos.repository.ItemCotizacionRepository;
import com.masterserv.productos.repository.PedidoRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
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
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    

    @Transactional(readOnly = true)
    public CotizacionPublicaDTO findCotizacionPublicaByToken(String token) {
        Cotizacion cotizacion = cotizacionRepository.findByToken(token)
            .orElseThrow(() -> new EntityNotFoundException("Solicitud de cotización no encontrada o inválida."));

        if (cotizacion.getEstado() != EstadoCotizacion.PENDIENTE_PROVEEDOR) {
            throw new IllegalStateException("Esta solicitud de cotización ya ha sido procesada o ha expirado.");
        }

        return new CotizacionPublicaDTO(cotizacion);
    }
    
    @Transactional
    public void submitOfertaProveedor(String token, OfertaProveedorDTO ofertaDTO) {
        
        Cotizacion cotizacion = cotizacionRepository.findByToken(token)
            .orElseThrow(() -> new EntityNotFoundException("Solicitud de cotización no encontrada o inválida."));

        if (cotizacion.getEstado() != EstadoCotizacion.PENDIENTE_PROVEEDOR) {
            throw new IllegalStateException("Esta solicitud de cotización ya ha sido procesada o ha expirado.");
        }

        Map<Long, ItemCotizacion> itemsMap = cotizacion.getItems().stream()
            .collect(Collectors.toMap(ItemCotizacion::getId, Function.identity()));

        BigDecimal precioTotalOfertado = BigDecimal.ZERO;

        for (ItemOfertaDTO itemOferta : ofertaDTO.getItems()) {
            ItemCotizacion itemDB = itemsMap.get(itemOferta.getItemCotizacionId());
            
            if (itemDB == null || !itemDB.getCotizacion().getId().equals(cotizacion.getId())) {
                throw new SecurityException("Intento de cotizar un item inválido.");
            }

            // Lógica de Disponibilidad
            if (!itemOferta.isDisponible()) {
                itemDB.setEstado(EstadoItemCotizacion.NO_DISPONIBLE_PROVEEDOR);
                itemDB.setPrecioUnitarioOfertado(BigDecimal.ZERO);
                continue; 
            }

            // Proveedor cotiza
            itemDB.setPrecioUnitarioOfertado(itemOferta.getPrecioUnitarioOfertado());
            itemDB.setEstado(EstadoItemCotizacion.COTIZADO);
            
            // Cantidad final
            int cantidadFinal = (itemOferta.getCantidadOfertada() != null && itemOferta.getCantidadOfertada() > 0) 
                    ? itemOferta.getCantidadOfertada() 
                    : itemDB.getCantidadSolicitada();
            
            BigDecimal subtotal = itemOferta.getPrecioUnitarioOfertado()
                    .multiply(new BigDecimal(cantidadFinal));
            
            precioTotalOfertado = precioTotalOfertado.add(subtotal);
        }

        cotizacion.setFechaEntregaOfertada(ofertaDTO.getFechaEntregaOfertada());
        cotizacion.setPrecioTotalOfertado(precioTotalOfertado);
        cotizacion.setEstado(EstadoCotizacion.RECIBIDA);

        cotizacionRepository.save(cotizacion);
        
        recalcularRecomendacion(cotizacion);
    }

    @Transactional(readOnly = true)
    public List<CotizacionAdminDTO> findCotizacionesRecibidas() {
        return cotizacionRepository.findByEstado(EstadoCotizacion.RECIBIDA).stream()
            .map(CotizacionAdminDTO::new)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CotizacionAdminDTO findCotizacionAdminById(Long id) {
        Cotizacion cotizacion = cotizacionRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Cotización no encontrada: " + id));
        return new CotizacionAdminDTO(cotizacion);
    }

    @Transactional
    public void cancelarItem(Long itemId) {
        ItemCotizacion item = itemCotizacionRepository.findById(itemId)
            .orElseThrow(() -> new EntityNotFoundException("Item no encontrado: " + itemId));
        
        if (item.getCotizacion().getEstado() != EstadoCotizacion.RECIBIDA) {
            throw new IllegalStateException("Solo se pueden cancelar items en estado RECIBIDA.");
        }
        
        item.setEstado(EstadoItemCotizacion.CANCELADO_ADMIN);
        itemCotizacionRepository.save(item);
        
        recalcularTotalCotizacion(item.getCotizacion());
    }

    @Transactional
    public void cancelarCotizacion(Long id) {
        Cotizacion cotizacion = cotizacionRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Cotización no encontrada: " + id));
        
        if (cotizacion.getEstado() != EstadoCotizacion.RECIBIDA) {
            throw new IllegalStateException("Solo se pueden cancelar cotizaciones RECIBIDAS.");
        }
        
        cotizacion.setEstado(EstadoCotizacion.CANCELADA_ADMIN);
        cotizacionRepository.save(cotizacion);
    }
    
    /**
     * Confirma una cotización, genera el pedido y cancela items en otras cotizaciones.
     * Si una cotización rival se queda sin items activos, se cancela completamente.
     */
    @Transactional
    public Pedido confirmarCotizacion(Long id, String adminEmail) {
        Usuario adminUsuario = usuarioRepository.findByEmail(adminEmail)
            .orElseThrow(() -> new EntityNotFoundException("Admin no encontrado: " + adminEmail));
        
        Cotizacion cotizacionGanadora = cotizacionRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Cotización no encontrada: " + id));

        if (cotizacionGanadora.getEstado() != EstadoCotizacion.RECIBIDA) {
            throw new IllegalStateException("Solo se pueden confirmar cotizaciones RECIBIDAS.");
        }
        
        Pedido pedido = new Pedido();
        pedido.setFechaPedido(LocalDateTime.now());
        pedido.setEstado(EstadoPedido.PENDIENTE); 
        pedido.setProveedor(cotizacionGanadora.getProveedor());
        pedido.setUsuario(adminUsuario); 
        
        Set<DetallePedido> detallesPedido = new HashSet<>();
        BigDecimal totalPedido = BigDecimal.ZERO;

        // Definimos qué items de la competencia están "vivos" y deben morir
        List<EstadoItemCotizacion> estadosVivosCompetencia = Arrays.asList(
                EstadoItemCotizacion.PENDIENTE,
                EstadoItemCotizacion.COTIZADO
        );

        for (ItemCotizacion itemGanador : cotizacionGanadora.getItems()) {
            
            // Solo procesamos lo que el proveedor cotizó
            if (itemGanador.getEstado() == EstadoItemCotizacion.COTIZADO) {
                
                // 1. Crear detalle de pedido
                DetallePedido detalle = new DetallePedido();
                detalle.setPedido(pedido);
                detalle.setProducto(itemGanador.getProducto());
                detalle.setCantidad(itemGanador.getCantidadSolicitada());
                detalle.setPrecioUnitario(itemGanador.getPrecioUnitarioOfertado()); 
                
                detallesPedido.add(detalle);
                
                BigDecimal subtotal = itemGanador.getPrecioUnitarioOfertado()
                        .multiply(new BigDecimal(itemGanador.getCantidadSolicitada()));
                totalPedido = totalPedido.add(subtotal);

                // 2. Marcar el item ganador como CONFIRMADO
                itemGanador.setEstado(EstadoItemCotizacion.CONFIRMADO);

                // 3. BUSCAR Y ELIMINAR LA COMPETENCIA
                List<ItemCotizacion> itemsPerdedores = itemCotizacionRepository.findItemsRivales(
                        itemGanador.getProducto().getId(),
                        cotizacionGanadora.getId(),
                        estadosVivosCompetencia
                );

                if (!itemsPerdedores.isEmpty()) {
                    // Usamos un Set para identificar las cotizaciones padres afectadas sin repetir
                    Set<Cotizacion> cotizacionesAfectadas = new HashSet<>();

                    itemsPerdedores.forEach(perdedor -> {
                        perdedor.setEstado(EstadoItemCotizacion.CANCELADO_SISTEMA);
                        cotizacionesAfectadas.add(perdedor.getCotizacion());
                    });
                    
                    itemCotizacionRepository.saveAll(itemsPerdedores);
                    
                    // 4. VERIFICAR SI LAS COTIZACIONES AFECTADAS DEBEN MORIR
                    verificarYAutoCancelarCotizaciones(cotizacionesAfectadas);
                }
            }
        }
        
        if (detallesPedido.isEmpty()) {
            throw new IllegalStateException("No hay items cotizados válidos para crear el pedido.");
        }

        pedido.setDetalles(detallesPedido);
        pedido.setTotalPedido(totalPedido);

        pedidoRepository.save(pedido);

        // Actualizamos la cotización ganadora
        cotizacionGanadora.setEstado(EstadoCotizacion.CONFIRMADA_ADMIN);
        cotizacionRepository.save(cotizacionGanadora);
        
        // Guardamos los cambios de estado de los items ganadores
        itemCotizacionRepository.saveAll(cotizacionGanadora.getItems());
        
        return pedido;
    }
    
    /**
     * Verifica si una lista de cotizaciones se ha quedado sin items activos.
     * Si no tienen items pendientes ni cotizados, se marcan como CANCELADA_SISTEMA.
     */
    private void verificarYAutoCancelarCotizaciones(Set<Cotizacion> cotizaciones) {
        for (Cotizacion cot : cotizaciones) {
            // Buscamos si queda ALGÚN item vivo
            boolean tieneItemsVivos = cot.getItems().stream()
                .anyMatch(i -> 
                    i.getEstado() == EstadoItemCotizacion.PENDIENTE || 
                    i.getEstado() == EstadoItemCotizacion.COTIZADO
                );

            if (!tieneItemsVivos) {
                // Si está vacía de items útiles, la cerramos
                cot.setEstado(EstadoCotizacion.CANCELADA_SISTEMA);
                cotizacionRepository.save(cot);
            } else {
                // Si aún vive, actualizamos su total por si acaso bajó de precio
                recalcularTotalCotizacion(cot);
            }
        }
    }
    
    private void recalcularTotalCotizacion(Cotizacion cotizacion) {
        BigDecimal nuevoTotal = BigDecimal.ZERO;
        for (ItemCotizacion item : cotizacion.getItems()) {
            if ((item.getEstado() == EstadoItemCotizacion.COTIZADO || item.getEstado() == EstadoItemCotizacion.CONFIRMADO) 
                    && item.getPrecioUnitarioOfertado() != null) {
                nuevoTotal = nuevoTotal.add(
                    item.getPrecioUnitarioOfertado().multiply(new BigDecimal(item.getCantidadSolicitada()))
                );
            }
        }
        cotizacion.setPrecioTotalOfertado(nuevoTotal);
        cotizacionRepository.save(cotizacion);
        
        recalcularRecomendacion(cotizacion);
    }

    private void recalcularRecomendacion(Cotizacion cotizacionRef) {
        List<Cotizacion> competidoras = cotizacionRepository.findByEstado(EstadoCotizacion.RECIBIDA);
        
        if (competidoras.isEmpty()) return;

        for (Cotizacion c : competidoras) {
            c.setEsRecomendada(false);
        }

        Cotizacion mejorOpcion = competidoras.stream()
            .max((c1, c2) -> {
                long itemsC1 = c1.getItems().stream().filter(i -> i.getEstado() == EstadoItemCotizacion.COTIZADO).count();
                long itemsC2 = c2.getItems().stream().filter(i -> i.getEstado() == EstadoItemCotizacion.COTIZADO).count();
                int compareItems = Long.compare(itemsC1, itemsC2);
                if (compareItems != 0) return compareItems;

                BigDecimal p1 = c1.getPrecioTotalOfertado() != null ? c1.getPrecioTotalOfertado() : BigDecimal.valueOf(Long.MAX_VALUE);
                BigDecimal p2 = c2.getPrecioTotalOfertado() != null ? c2.getPrecioTotalOfertado() : BigDecimal.valueOf(Long.MAX_VALUE);
                int comparePrecio = p2.compareTo(p1); 
                if (comparePrecio != 0) return comparePrecio;

                LocalDate f1 = c1.getFechaEntregaOfertada() != null ? c1.getFechaEntregaOfertada() : LocalDate.MAX;
                LocalDate f2 = c2.getFechaEntregaOfertada() != null ? c2.getFechaEntregaOfertada() : LocalDate.MAX;
                return f2.compareTo(f1); 
            })
            .orElse(null);

        if (mejorOpcion != null) {
            mejorOpcion.setEsRecomendada(true);
        }
        
        cotizacionRepository.saveAll(competidoras);
    }
}