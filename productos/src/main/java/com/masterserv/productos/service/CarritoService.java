package com.masterserv.productos.service;

import com.masterserv.productos.dto.AddItemCarritoDTO; // Necesitaremos este DTO simple
import com.masterserv.productos.dto.CarritoDTO;
import com.masterserv.productos.dto.ItemCarritoDTO; // Para mapear la respuesta
import com.masterserv.productos.entity.*; // Importar Carrito, ItemCarrito, Producto, Usuario
import com.masterserv.productos.mapper.CarritoMapper; // ¡NECESITAS CREAR ESTE MAPPER!
import com.masterserv.productos.repository.CarritoRepository;
import com.masterserv.productos.repository.ItemCarritoRepository;
import com.masterserv.productos.repository.ProductoRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal; // Para cálculos de total
import java.util.HashSet;
import java.util.Optional; // Para manejar búsquedas opcionales
import java.util.stream.Collectors; // Para mapear la lista de items

// Excepción definida previamente
// class StockInsuficienteException extends RuntimeException { ... }

// Nuevo DTO simple para recibir la petición de agregar item
// Puedes crearlo en el paquete dto
// import lombok.Data;
// @Data
// public class AddItemCarritoDTO {
//     @NotNull private Long productoId;
//     @NotNull @Min(1) private Integer cantidad;
// }


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
    private CarritoMapper carritoMapper; // ¡Asegúrate de crear esta interfaz!

    /**
     * Obtiene el carrito activo para un vendedor. Si no existe, lo crea.
     * Es transaccional para asegurar la creación atómica si es necesario.
     *
     * @param vendedorEmail Email del vendedor.
     * @return El CarritoDTO del vendedor.
     */
    @Transactional
    public CarritoDTO getCarritoByVendedorEmail(String vendedorEmail) {
        Usuario vendedor = usuarioRepository.findByEmail(vendedorEmail)
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado: " + vendedorEmail));

        // Busca el carrito o crea uno nuevo si no existe
        Carrito carrito = carritoRepository.findByVendedor(vendedor)
                .orElseGet(() -> {
                    Carrito nuevoCarrito = new Carrito();
                    nuevoCarrito.setVendedor(vendedor);
                    return carritoRepository.save(nuevoCarrito);
                });

        // Mapear y calcular totales antes de devolver
        return mapAndCalculateTotals(carrito);
    }

    /**
     * Agrega un item (producto y cantidad) al carrito del vendedor.
     * Valida stock ANTES de agregar/actualizar.
     * Si el producto ya existe en el carrito, actualiza la cantidad.
     *
     * @param vendedorEmail Email del vendedor.
     * @param itemDTO       DTO con productoId y cantidad.
     * @return El CarritoDTO actualizado.
     */
    @Transactional
    public CarritoDTO agregarItem(String vendedorEmail, AddItemCarritoDTO itemDTO) {
        Carrito carrito = findCarritoByVendedorEmailOrFail(vendedorEmail); // Helper para buscar o fallar

        Producto producto = productoRepository.findById(itemDTO.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: ID " + itemDTO.getProductoId()));

        // --- VALIDACIÓN DE STOCK INICIAL ---
        if (producto.getStockActual() < itemDTO.getCantidad()) {
            throw new StockInsuficienteException(
                String.format("Stock insuficiente para agregar '%s'. Disponible: %d, Solicitado: %d",
                              producto.getNombre(), producto.getStockActual(), itemDTO.getCantidad())
            );
        }

        // Buscar si el item ya existe en el carrito
        Optional<ItemCarrito> itemExistenteOpt = itemCarritoRepository.findByCarritoAndProducto(carrito, producto);

        if (itemExistenteOpt.isPresent()) {
            // --- ACTUALIZAR CANTIDAD ---
            ItemCarrito itemExistente = itemExistenteOpt.get();
            int nuevaCantidad = itemExistente.getCantidad() + itemDTO.getCantidad();

            // --- RE-VALIDACIÓN DE STOCK (para la nueva cantidad total) ---
            if (producto.getStockActual() < nuevaCantidad) {
                throw new StockInsuficienteException(
                    String.format("Stock insuficiente para actualizar '%s'. Disponible: %d, Solicitado total: %d",
                                  producto.getNombre(), producto.getStockActual(), nuevaCantidad)
                );
            }
            itemExistente.setCantidad(nuevaCantidad);
            itemCarritoRepository.save(itemExistente); // Guardar el item actualizado

        } else {
            // --- CREAR NUEVO ITEM ---
            ItemCarrito nuevoItem = new ItemCarrito();
            nuevoItem.setCarrito(carrito);
            nuevoItem.setProducto(producto);
            nuevoItem.setCantidad(itemDTO.getCantidad());
            itemCarritoRepository.save(nuevoItem); // Guardar el nuevo item
            // No es necesario añadirlo explícitamente a carrito.getItems() si la relación es bidireccional
            // y está bien configurada, pero hacerlo asegura consistencia en el objeto actual.
             if (carrito.getItems() == null) carrito.setItems(new HashSet<>()); // Asegurar inicialización
             carrito.getItems().add(nuevoItem);
        }

        // Recargar el carrito para obtener el estado actualizado de la BD
        // Ojo: Esto puede ser ineficiente. Alternativa: actualizar manualmente el objeto 'carrito'.
        Carrito carritoActualizado = findCarritoByVendedorEmailOrFail(vendedorEmail);
        return mapAndCalculateTotals(carritoActualizado); // Mapear y calcular totales
    }

    /**
     * Quita un item específico del carrito del vendedor.
     *
     * @param vendedorEmail Email del vendedor.
     * @param itemCarritoId ID del ItemCarrito a eliminar.
     * @return El CarritoDTO actualizado.
     */
    @Transactional
    public CarritoDTO quitarItem(String vendedorEmail, Long itemCarritoId) {
        Carrito carrito = findCarritoByVendedorEmailOrFail(vendedorEmail);

        ItemCarrito itemParaQuitar = itemCarritoRepository.findById(itemCarritoId)
                .orElseThrow(() -> new RuntimeException("Item de carrito no encontrado: ID " + itemCarritoId));

        // Validar que el item pertenezca al carrito del vendedor (seguridad/consistencia)
        if (!itemParaQuitar.getCarrito().getId().equals(carrito.getId())) {
            throw new SecurityException("Intento de eliminar un item de un carrito ajeno.");
        }

        itemCarritoRepository.delete(itemParaQuitar);
        // Opcional: remover del Set en la entidad Carrito si es necesario
        // carrito.getItems().remove(itemParaQuitar);

        // Recargar o recalcular
        Carrito carritoActualizado = findCarritoByVendedorEmailOrFail(vendedorEmail);
        return mapAndCalculateTotals(carritoActualizado);
    }

    /**
     * Actualiza la cantidad de un item específico en el carrito.
     * Valida stock para la nueva cantidad.
     *
     * @param vendedorEmail Email del vendedor.
     * @param itemCarritoId ID del ItemCarrito a actualizar.
     * @param nuevaCantidad La nueva cantidad deseada.
     * @return El CarritoDTO actualizado.
     */
    @Transactional
    public CarritoDTO actualizarCantidadItem(String vendedorEmail, Long itemCarritoId, int nuevaCantidad) {
        if (nuevaCantidad <= 0) {
            // Si la nueva cantidad es 0 o menos, simplemente quitamos el item
            return quitarItem(vendedorEmail, itemCarritoId);
        }

        Carrito carrito = findCarritoByVendedorEmailOrFail(vendedorEmail);

        ItemCarrito itemParaActualizar = itemCarritoRepository.findById(itemCarritoId)
                .orElseThrow(() -> new RuntimeException("Item de carrito no encontrado: ID " + itemCarritoId));

        // Validar pertenencia al carrito
        if (!itemParaActualizar.getCarrito().getId().equals(carrito.getId())) {
            throw new SecurityException("Intento de actualizar un item de un carrito ajeno.");
        }

        Producto producto = itemParaActualizar.getProducto(); // Producto ya está cargado (o se carga LAZY)

        // --- VALIDACIÓN DE STOCK ---
        if (producto.getStockActual() < nuevaCantidad) {
             throw new StockInsuficienteException(
                String.format("Stock insuficiente para actualizar '%s'. Disponible: %d, Solicitado: %d",
                              producto.getNombre(), producto.getStockActual(), nuevaCantidad)
            );
        }

        itemParaActualizar.setCantidad(nuevaCantidad);
        itemCarritoRepository.save(itemParaActualizar);

        // Recargar o recalcular
        Carrito carritoActualizado = findCarritoByVendedorEmailOrFail(vendedorEmail);
        return mapAndCalculateTotals(carritoActualizado);
    }

     /**
     * Vacía completamente el carrito de un vendedor.
     * Se usa típicamente después de finalizar una venta.
     *
     * @param vendedorEmail Email del vendedor.
     * @return El CarritoDTO vacío.
     */
    @Transactional
    public CarritoDTO vaciarCarrito(String vendedorEmail) {
        Carrito carrito = findCarritoByVendedorEmailOrFail(vendedorEmail);

        // Eliminar todos los items asociados a este carrito
        // Opción 1: Iterar y eliminar (puede ser menos eficiente si hay muchos items)
        // Set<ItemCarrito> itemsAEliminar = new HashSet<>(carrito.getItems()); // Copiar para evitar ConcurrentModificationException
        // itemsAEliminar.forEach(item -> itemCarritoRepository.delete(item));

        // Opción 2: Consulta de eliminación masiva (más eficiente)
        itemCarritoRepository.deleteAllByCarritoId(carrito.getId()); // ¡Necesitas añadir este método al repo!

        // Limpiar la colección en la entidad para consistencia del objeto actual
        if (carrito.getItems() != null) {
             carrito.getItems().clear();
        }

        return mapAndCalculateTotals(carrito); // Devolverá un carrito vacío
    }


    // --- Métodos Helper ---

    /**
     * Busca el carrito de un vendedor o lanza una excepción si no se encuentra.
     * (Usado internamente para evitar repetir código).
     */
    private Carrito findCarritoByVendedorEmailOrFail(String vendedorEmail) {
         Usuario vendedor = usuarioRepository.findByEmail(vendedorEmail)
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado: " + vendedorEmail));
         // Usamos el método que asume que el carrito ya DEBERÍA existir para estas operaciones
         return carritoRepository.findByVendedor(vendedor)
                 .orElseThrow(() -> new RuntimeException("Carrito no encontrado para el vendedor: " + vendedorEmail + ". Debería haberse creado."));
    }

    /**
     * Mapea la entidad Carrito a CarritoDTO y calcula los totales.
     * Centraliza la lógica de mapeo y cálculo.
     */
    private CarritoDTO mapAndCalculateTotals(Carrito carrito) {
        // Usa el mapper para la estructura básica
        CarritoDTO dto = carritoMapper.toCarritoDTO(carrito);

        // Calcular totales manualmente (o podrías hacerlo en el mapper si prefieres)
        BigDecimal total = BigDecimal.ZERO;
        int cantidadTotalItems = 0;

        if (dto.getItems() != null) {
            for (ItemCarritoDTO item : dto.getItems()) {
                // Asegurarse de que el subtotal se calcule correctamente en el ItemCarritoMapper
                // Si no, calcularlo aquí:
                // BigDecimal sub = item.getPrecioUnitarioVenta().multiply(BigDecimal.valueOf(item.getCantidad()));
                // item.setSubtotal(sub);
                if (item.getSubtotal() != null) { // Añadir chequeo de nulidad
                   total = total.add(item.getSubtotal());
                }
                cantidadTotalItems += item.getCantidad();
            }
        }

        dto.setTotalCarrito(total);
        dto.setCantidadItems(cantidadTotalItems);
        return dto;
    }

     // --- ¡Método Necesario para el deleteAllByCarritoId! ---
     // Añade esto a tu ItemCarritoRepository.java:
     //
     // import org.springframework.data.jpa.repository.Modifying;
     // import org.springframework.data.jpa.repository.Query;
     // ...
     // @Modifying // Indica que es una consulta de modificación (DELETE/UPDATE)
     // @Query("DELETE FROM ItemCarrito ic WHERE ic.carrito.id = :carritoId")
     // void deleteAllByCarritoId(@Param("carritoId") Long carritoId);

}