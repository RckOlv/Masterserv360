package com.masterserv.productos.service;

import com.masterserv.productos.dto.AddItemCarritoDTO;
import com.masterserv.productos.dto.CarritoDTO;
import com.masterserv.productos.dto.ItemCarritoDTO;
import com.masterserv.productos.dto.UpdateCantidadCarritoDTO;
import com.masterserv.productos.entity.*;
import com.masterserv.productos.mapper.CarritoMapper;
import com.masterserv.productos.repository.CarritoRepository;
import com.masterserv.productos.repository.ItemCarritoRepository;
import com.masterserv.productos.repository.ProductoRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.masterserv.productos.exceptions.StockInsuficienteException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CarritoService {

    @Autowired private CarritoRepository carritoRepository;
    @Autowired private ItemCarritoRepository itemCarritoRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CarritoMapper carritoMapper; 

    @Transactional
    public CarritoDTO getCarritoByVendedorEmail(String vendedorEmail) {
        Usuario vendedor = usuarioRepository.findByEmail(vendedorEmail)
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado: " + vendedorEmail));

        Carrito carrito = carritoRepository.findByVendedorWithItemsLock(vendedor)
                .orElseGet(() -> {
                    Carrito nuevoCarrito = new Carrito();
                    nuevoCarrito.setVendedor(vendedor);
                    nuevoCarrito.setItems(new HashSet<>());
                    return carritoRepository.save(nuevoCarrito);
                });

        return mapAndCalculateTotals(carrito);
    }

    @Transactional
    public CarritoDTO agregarItem(String vendedorEmail, AddItemCarritoDTO itemDTO) {
        Carrito carrito = findCarritoByVendedorEmailOrFail(vendedorEmail); 

        Producto producto = productoRepository.findById(itemDTO.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: ID " + itemDTO.getProductoId()));

        if (producto.getStockActual() < itemDTO.getCantidad()) {
            throw new StockInsuficienteException("Stock insuficiente");
        }

        Optional<ItemCarrito> itemExistenteOpt = itemCarritoRepository.findByCarritoAndProducto(carrito, producto);

        if (itemExistenteOpt.isPresent()) {
            ItemCarrito itemExistente = itemExistenteOpt.get();
            int nuevaCantidad = itemExistente.getCantidad() + itemDTO.getCantidad();
            if (producto.getStockActual() < nuevaCantidad) {
                throw new StockInsuficienteException("Stock insuficiente");
            }
            itemExistente.setCantidad(nuevaCantidad);
            itemCarritoRepository.save(itemExistente);
        } else {
            ItemCarrito nuevoItem = new ItemCarrito();
            nuevoItem.setCarrito(carrito);
            nuevoItem.setProducto(producto);
            nuevoItem.setCantidad(itemDTO.getCantidad());
            ItemCarrito itemGuardado = itemCarritoRepository.save(nuevoItem); 
            if (carrito.getItems() == null) carrito.setItems(new HashSet<>());
            carrito.getItems().add(itemGuardado);
        }
        return mapAndCalculateTotals(carrito); 
    }

    @Transactional
    public CarritoDTO quitarItem(String vendedorEmail, Long itemCarritoId) {
        Carrito carrito = findCarritoByVendedorEmailOrFail(vendedorEmail);
        ItemCarrito itemParaQuitar = itemCarritoRepository.findById(itemCarritoId)
                .orElseThrow(() -> new RuntimeException("Item no encontrado"));

        if (!itemParaQuitar.getCarrito().getId().equals(carrito.getId())) {
            throw new SecurityException("Acceso denegado al item");
        }
        if (carrito.getItems() != null) carrito.getItems().remove(itemParaQuitar);
        itemCarritoRepository.delete(itemParaQuitar);
        return mapAndCalculateTotals(carrito);
    }

    @Transactional
    public CarritoDTO actualizarCantidadItem(String vendedorEmail, Long itemCarritoId, int nuevaCantidad) {
        if (nuevaCantidad <= 0) return quitarItem(vendedorEmail, itemCarritoId);
        Carrito carrito = findCarritoByVendedorEmailOrFail(vendedorEmail);
        ItemCarrito item = itemCarritoRepository.findById(itemCarritoId)
                .orElseThrow(() -> new RuntimeException("Item no encontrado"));

        if (!item.getCarrito().getId().equals(carrito.getId())) throw new SecurityException("Acceso denegado");
        if (item.getProducto().getStockActual() < nuevaCantidad) throw new StockInsuficienteException("Stock insuficiente");

        item.setCantidad(nuevaCantidad);
        itemCarritoRepository.save(item);
        return mapAndCalculateTotals(carrito);
    }

    @Transactional
    public CarritoDTO vaciarCarrito(String vendedorEmail) {
        Carrito carrito = findCarritoByVendedorEmailOrFail(vendedorEmail);
        if (carrito.getItems() != null) carrito.getItems().clear();
        carritoRepository.save(carrito); 
        return mapAndCalculateTotals(carrito);
    }

    private Carrito findCarritoByVendedorEmailOrFail(String vendedorEmail) {
        Usuario vendedor = usuarioRepository.findByEmail(vendedorEmail)
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado"));
        return carritoRepository.findByVendedorWithItemsLock(vendedor)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado"));
    }

    // --- MENTOR: MÉTODO CORREGIDO PARA RECORDS ---
    private CarritoDTO mapAndCalculateTotals(Carrito carrito) {
        BigDecimal total = BigDecimal.ZERO;
        int cantidadTotalItems = 0;
        
        Set<ItemCarrito> items = carrito.getItems() != null ? carrito.getItems() : new HashSet<>();
        List<ItemCarritoDTO> itemDTOs = new ArrayList<>();

        for (ItemCarrito item : items) {
            BigDecimal sub = item.getProducto().getPrecioVenta().multiply(BigDecimal.valueOf(item.getCantidad()));
            
            // Calculamos totales
            total = total.add(sub);
            cantidadTotalItems += item.getCantidad();

            // Extraemos ID Categoría
            Long catId = (item.getProducto().getCategoria() != null) ? item.getProducto().getCategoria().getId() : null;

            // Construimos DTO manualmente (Record no tiene setters)
            ItemCarritoDTO itemDto = new ItemCarritoDTO(
                item.getId(),
                item.getProducto().getId(),
                item.getProducto().getNombre(),
                item.getProducto().getCodigo(),
                item.getProducto().getPrecioVenta(),
                item.getCantidad(),
                sub,
                item.getProducto().getStockActual(),
                catId // <--- AQUÍ VA EL ID QUE NECESITAMOS
            );
            itemDTOs.add(itemDto);
        }

        // Construimos CarritoDTO manualmente
        return new CarritoDTO(
            carrito.getId(),
            carrito.getVendedor().getId(),
            itemDTOs,
            total,
            cantidadTotalItems
        );
    }
}