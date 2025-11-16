package com.masterserv.productos.service;

import com.masterserv.productos.dto.AddItemCarritoDTO;
import com.masterserv.productos.dto.CarritoDTO;
import com.masterserv.productos.dto.ItemCarritoDTO;
import com.masterserv.productos.entity.*;
import com.masterserv.productos.mapper.CarritoMapper;
import com.masterserv.productos.repository.CarritoRepository;
import com.masterserv.productos.repository.ItemCarritoRepository;
import com.masterserv.productos.repository.ProductoRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.masterserv.productos.exceptions.StockInsuficienteException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CarritoService {

    @Autowired
    private CarritoRepository carritoRepository;
    
    @Autowired
    private ItemCarritoRepository itemCarritoRepository;
    
    @Autowired
    private ProductoRepository productoRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private CarritoMapper carritoMapper; 

    /**
     * Obtiene el carrito activo para un vendedor. Si no existe, lo crea.
     * Mentor: Usamos el método con bloqueo para consistencia.
     */
    @Transactional
    public CarritoDTO getCarritoByVendedorEmail(String vendedorEmail) {
        Usuario vendedor = usuarioRepository.findByEmail(vendedorEmail)
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado: " + vendedorEmail));

        // Mentor: Cambiado a findByVendedorWithItemsLock para ser consistente
        Carrito carrito = carritoRepository.findByVendedorWithItemsLock(vendedor)
                .orElseGet(() -> {
                    Carrito nuevoCarrito = new Carrito();
                    nuevoCarrito.setVendedor(vendedor);
                    nuevoCarrito.setItems(new HashSet<>());
                    return carritoRepository.save(nuevoCarrito);
                });

        return mapAndCalculateTotals(carrito);
    }

    /**
     * Agrega un item al carrito del vendedor.
     */
    @Transactional
    public CarritoDTO agregarItem(String vendedorEmail, AddItemCarritoDTO itemDTO) {
        // Mentor: Llama al helper que SÍ usa el bloqueo
        Carrito carrito = findCarritoByVendedorEmailOrFail(vendedorEmail); 

        Producto producto = productoRepository.findById(itemDTO.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: ID " + itemDTO.getProductoId()));

        if (producto.getStockActual() < itemDTO.getCantidad()) {
            throw new StockInsuficienteException(
                String.format("Stock insuficiente para agregar '%s'. Disponible: %d, Solicitado: %d",
                            producto.getNombre(), producto.getStockActual(), itemDTO.getCantidad())
            );
        }

        Optional<ItemCarrito> itemExistenteOpt = itemCarritoRepository.findByCarritoAndProducto(carrito, producto);

        if (itemExistenteOpt.isPresent()) {
            ItemCarrito itemExistente = itemExistenteOpt.get();
            int nuevaCantidad = itemExistente.getCantidad() + itemDTO.getCantidad();

            if (producto.getStockActual() < nuevaCantidad) {
                throw new StockInsuficienteException(
                    String.format("Stock insuficiente para actualizar '%s'. Disponible: %d, Solicitado total: %d",
                                    producto.getNombre(), producto.getStockActual(), nuevaCantidad)
                );
            }
            itemExistente.setCantidad(nuevaCantidad);
            itemCarritoRepository.save(itemExistente);

        } else {
            ItemCarrito nuevoItem = new ItemCarrito();
            nuevoItem.setCarrito(carrito);
            nuevoItem.setProducto(producto);
            nuevoItem.setCantidad(itemDTO.getCantidad());
            
            ItemCarrito itemGuardado = itemCarritoRepository.save(nuevoItem); 
            
            if (carrito.getItems() == null) {
                carrito.setItems(new HashSet<>());
            }
            carrito.getItems().add(itemGuardado);
            // Mentor: No es necesario un save(carrito) aquí porque la relación 
            // se guarda desde el 'hijo' (itemGuardado)
        }

        // Mentor: mapAndCalculateTotals es llamado sobre el 'carrito' 
        // que está "en vivo" en la transacción.
        return mapAndCalculateTotals(carrito); 
    }

    /**
     * Quita un item específico del carrito del vendedor.
     */
    @Transactional
    public CarritoDTO quitarItem(String vendedorEmail, Long itemCarritoId) {
        Carrito carrito = findCarritoByVendedorEmailOrFail(vendedorEmail);

        ItemCarrito itemParaQuitar = itemCarritoRepository.findById(itemCarritoId)
                .orElseThrow(() -> new RuntimeException("Item de carrito no encontrado: ID " + itemCarritoId));

        if (!itemParaQuitar.getCarrito().getId().equals(carrito.getId())) {
            throw new SecurityException("Intento de eliminar un item de un carrito ajeno.");
        }

        // Mentor: Esta es la forma correcta de eliminar:
        // 1. Quitarlo de la colección del 'padre'
        if (carrito.getItems() != null) {
            carrito.getItems().remove(itemParaQuitar);
        }
        // 2. Borrar la entidad 'hijo' explícitamente (o dejar que orphanRemoval lo haga)
        // Borrarlo explícitamente es más seguro.
        itemCarritoRepository.delete(itemParaQuitar);

        return mapAndCalculateTotals(carrito);
    }

    /**
     * Actualiza la cantidad de un item específico en el carrito.
     */
    @Transactional
    public CarritoDTO actualizarCantidadItem(String vendedorEmail, Long itemCarritoId, int nuevaCantidad) {
        if (nuevaCantidad <= 0) {
            return quitarItem(vendedorEmail, itemCarritoId);
        }

        Carrito carrito = findCarritoByVendedorEmailOrFail(vendedorEmail);

        ItemCarrito itemParaActualizar = itemCarritoRepository.findById(itemCarritoId)
                .orElseThrow(() -> new RuntimeException("Item de carrito no encontrado: ID " + itemCarritoId));

        if (!itemParaActualizar.getCarrito().getId().equals(carrito.getId())) {
            throw new SecurityException("Intento de actualizar un item de un carrito ajeno.");
        }

        Producto producto = itemParaActualizar.getProducto(); 

        if (producto.getStockActual() < nuevaCantidad) {
             throw new StockInsuficienteException(
                String.format("Stock insuficiente para actualizar '%s'. Disponible: %d, Solicitado: %d",
                            producto.getNombre(), producto.getStockActual(), nuevaCantidad)
            );
        }

        itemParaActualizar.setCantidad(nuevaCantidad);
        itemCarritoRepository.save(itemParaActualizar);

        return mapAndCalculateTotals(carrito);
    }

    /**
     * Mentor: MÉTODO CORREGIDO
     * Vacía completely el carrito confiando en JPA (orphanRemoval).
     */
    @Transactional
    public CarritoDTO vaciarCarrito(String vendedorEmail) {
        Carrito carrito = findCarritoByVendedorEmailOrFail(vendedorEmail);

        // Mentor: Se ELIMINÓ la línea conflictiva:
        // itemCarritoRepository.deleteAllByCarritoId(carrito.getId()); 

        // Mentor: Esta es la ÚNICA orden de borrado.
        // Asume que tu entidad Carrito tiene @OneToMany(..., orphanRemoval = true)
        if (carrito.getItems() != null) {
            carrito.getItems().clear();
        }

        // Mentor: Guardamos el 'padre' para propagar el .clear()
        carritoRepository.save(carrito); 

        return mapAndCalculateTotals(carrito);
    }


    // --- Métodos Helper ---

    /**
     * Helper centralizado que obtiene el carrito Y lo bloquea
     * para operaciones de escritura (add, remove, update, clear).
     */
    private Carrito findCarritoByVendedorEmailOrFail(String vendedorEmail) {
        Usuario vendedor = usuarioRepository.findByEmail(vendedorEmail)
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado: " + vendedorEmail));

        // Esta llamada (asumo) tiene @Lock(LockModeType.PESSIMISTIC_WRITE)
        // en el Repository, lo cual es excelente.
        return carritoRepository.findByVendedorWithItemsLock(vendedor)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado para el vendedor: " + vendedorEmail));
    }

    /**
     * Mapea la entidad Carrito a CarritoDTO y calcula los totales.
     * Mentor: Este método no necesita transacción, es solo un mapeo.
     */
    private CarritoDTO mapAndCalculateTotals(Carrito carrito) {
        CarritoDTO dto = carritoMapper.toCarritoDTO(carrito);

        BigDecimal total = BigDecimal.ZERO;
        int cantidadTotalItems = 0;
        
        // Mentor: El Set de items ya debería estar cargado por
        // la consulta findByVendedorWithItemsLock (FetchType.EAGER o JOIN FETCH)
        Set<ItemCarrito> items = carrito.getItems() != null ? carrito.getItems() : new HashSet<>();

        // Mentor: map(item -> ...) es más limpio que un for-loop
        List<ItemCarritoDTO> itemDTOs = items.stream()
            .map(item -> {
                ItemCarritoDTO itemDto = new ItemCarritoDTO();
                itemDto.setId(item.getId());
                itemDto.setProductoId(item.getProducto().getId());
                itemDto.setProductoNombre(item.getProducto().getNombre());
                itemDto.setProductoCodigo(item.getProducto().getCodigo());
                itemDto.setCantidad(item.getCantidad());
                itemDto.setPrecioUnitarioVenta(item.getProducto().getPrecioVenta());
                itemDto.setStockDisponible(item.getProducto().getStockActual());
                
                BigDecimal sub = item.getProducto().getPrecioVenta().multiply(BigDecimal.valueOf(item.getCantidad()));
                itemDto.setSubtotal(sub);
                
                // Acumulamos los totales aquí mismo
                // total = total.add(sub); // ¡Ojo! No se puede modificar variables 'externas' en un stream así
                // cantidadTotalItems += item.getCantidad();
                
                return itemDto;
            })
            .collect(Collectors.toList());
        
        dto.setItems(itemDTOs);

        // Mentor: Calculamos los totales *después* de tener la lista de DTOs
        if (dto.getItems() != null) {
            for (ItemCarritoDTO item : dto.getItems()) {
                if (item.getSubtotal() != null) {
                    total = total.add(item.getSubtotal());
                }
                cantidadTotalItems += item.getCantidad();
            }
        }

        dto.setTotalCarrito(total);
        dto.setCantidadItems(cantidadTotalItems);
        return dto;
    }
}