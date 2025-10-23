package com.masterserv.productos.service;

import com.masterserv.productos.dto.AddItemCarritoDTO;
import com.masterserv.productos.dto.CarritoDTO;
import com.masterserv.productos.dto.ItemCarritoDTO;
import com.masterserv.productos.entity.Carrito;
import com.masterserv.productos.entity.ItemCarrito;
import com.masterserv.productos.entity.Producto;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.mapper.CarritoMapper;
import com.masterserv.productos.repository.CarritoRepository;
import com.masterserv.productos.repository.ItemCarritoRepository;
import com.masterserv.productos.repository.ProductoRepository;
import com.masterserv.productos.repository.UsuarioRepository;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

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
     * Obtiene el carrito del vendedor. Si no existe, lo crea.
     * Esta es la lógica 1:1 de "carrito de trabajo".
     */
    @Transactional
    public Carrito getOrCreateCarritoVendedor(Long vendedorId) {
        Optional<Carrito> optCarrito = carritoRepository.findByVendedor_Id(vendedorId);
        
        if (optCarrito.isPresent()) {
            return optCarrito.get();
        } else {
            Usuario vendedor = usuarioRepository.findById(vendedorId)
                    .orElseThrow(() -> new RuntimeException("Vendedor no encontrado: " + vendedorId));
            
            Carrito nuevoCarrito = new Carrito();
            nuevoCarrito.setVendedor(vendedor);
            nuevoCarrito.setFechaCreacion(LocalDateTime.now());
            return carritoRepository.save(nuevoCarrito);
        }
    }

    /**
     * Agrega un producto al carrito del vendedor.
     * Si el producto ya existe, actualiza la cantidad (CU-07).
     */
    @Transactional
    public CarritoDTO agregarProducto(AddItemCarritoDTO addItemDTO) {
        Carrito carrito = getOrCreateCarritoVendedor(addItemDTO.getVendedorId());
        
        Producto producto = productoRepository.findById(addItemDTO.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + addItemDTO.getProductoId()));

        // Validamos stock antes de agregar
        if (producto.getStockActual() < addItemDTO.getCantidad()) {
            throw new RuntimeException("Stock insuficiente. Stock actual: " + producto.getStockActual());
        }

        // Lógica de CU-07: ¿Ya existe este producto en el carrito?
        Optional<ItemCarrito> optItem = itemCarritoRepository.findByCarritoAndProducto(carrito, producto);

        if (optItem.isPresent()) {
            // --- Caso 1: El producto ya está, actualizamos la cantidad ---
            ItemCarrito itemExistente = optItem.get();
            int nuevaCantidad = itemExistente.getCantidad() + addItemDTO.getCantidad();

            // Re-validamos stock con la nueva cantidad total
            if (producto.getStockActual() < nuevaCantidad) {
                throw new RuntimeException("Stock insuficiente. Ud. ya tiene " + itemExistente.getCantidad() + " en el carrito.");
            }
            itemExistente.setCantidad(nuevaCantidad);
            itemCarritoRepository.save(itemExistente);

        } else {
            // --- Caso 2: Producto nuevo, creamos el ItemCarrito ---
            ItemCarrito nuevoItem = new ItemCarrito();
            nuevoItem.setCarrito(carrito);
            nuevoItem.setProducto(producto);
            nuevoItem.setCantidad(addItemDTO.getCantidad());
            itemCarritoRepository.save(nuevoItem);
        }

        return getCarritoDTO(carrito.getId());
    }

    /**
     * Quita un item (una línea de producto) del carrito.
     */
    @Transactional
    public CarritoDTO quitarProducto(Long itemCarritoId) {
        ItemCarrito item = itemCarritoRepository.findById(itemCarritoId)
                .orElseThrow(() -> new RuntimeException("Item de carrito no encontrado: " + itemCarritoId));
        
        Long carritoId = item.getCarrito().getId();
        itemCarritoRepository.delete(item);
        
        return getCarritoDTO(carritoId);
    }
    
    /**
     * Obtiene el Carrito (con DTOs) y calcula el total.
     */
    @Transactional(readOnly = true)
    public CarritoDTO getCarritoDTO(Long carritoId) {
        Carrito carrito = carritoRepository.findById(carritoId)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado: " + carritoId));

        // Refrescamos la entidad para cargar los items (si son LAZY)
        // Opcional si la carga de 'items' no es EAGER
        // carrito = carritoRepository.findById(carritoId).get(); 

        CarritoDTO carritoDTO = carritoMapper.toCarritoDTO(carrito);
        
        // Mapeamos los items
        List<ItemCarritoDTO> itemDTOs = carrito.getItems().stream()
                .map(carritoMapper::toItemCarritoDTO)
                .toList();
        carritoDTO.setItems(itemDTOs);

        // Calculamos el total
        BigDecimal total = itemDTOs.stream()
                .map(ItemCarritoDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        carritoDTO.setTotal(total);

        return carritoDTO;
    }
    
    /**
     * Vacía el carrito de un vendedor (borra todos sus ItemCarrito).
     * Se usa después de concretar una Venta.
     */
    @Transactional
    public void vaciarCarrito(Long carritoId) {
        Carrito carrito = carritoRepository.findById(carritoId)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado: " + carritoId));
        
        // Borramos todos los items asociados
        itemCarritoRepository.deleteAll(carrito.getItems());
        
        // Actualizamos el set en la entidad Carrito
        carrito.getItems().clear();
        carritoRepository.save(carrito);
    }
}