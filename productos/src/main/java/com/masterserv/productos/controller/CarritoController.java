package com.masterserv.productos.controller;

import com.masterserv.productos.dto.AddItemCarritoDTO;
import com.masterserv.productos.dto.CarritoDTO;
import com.masterserv.productos.dto.UpdateCantidadCarritoDTO; // Nuevo DTO para actualizar cantidad
import com.masterserv.productos.service.CarritoService;
import jakarta.validation.Valid; // Para validar DTOs
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal; // Para obtener el usuario logueado


@RestController
@RequestMapping("/carrito")
// Solo usuarios autenticados (vendedores/admins) pueden acceder a su carrito
@PreAuthorize("isAuthenticated()")
public class CarritoController {

    @Autowired
    private CarritoService carritoService;

    /**
     * Obtiene el carrito de compras actual del usuario autenticado (vendedor).
     * Si no existe, el servicio lo crea.
     *
     * @param principal Objeto de seguridad con la info del usuario.
     * @return ResponseEntity con el CarritoDTO.
     */
    @GetMapping
    public ResponseEntity<CarritoDTO> getCarrito(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String vendedorEmail = principal.getName();
        CarritoDTO carrito = carritoService.getCarritoByVendedorEmail(vendedorEmail);
        return ResponseEntity.ok(carrito);
    }

    /**
     * Agrega un item (producto y cantidad) al carrito del usuario autenticado.
     *
     * @param principal Objeto de seguridad.
     * @param itemDTO   DTO con productoId y cantidad.
     * @return ResponseEntity con el CarritoDTO actualizado.
     */
    @PostMapping("/items")
    public ResponseEntity<CarritoDTO> agregarItemAlCarrito(Principal principal, @Valid @RequestBody AddItemCarritoDTO itemDTO) {
        if (principal == null || principal.getName() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String vendedorEmail = principal.getName();
        // Podríamos envolver la llamada en try-catch si StockInsuficienteException
        // debe devolver un código de error específico (ej. 409 Conflict)
        CarritoDTO carritoActualizado = carritoService.agregarItem(vendedorEmail, itemDTO);
        return ResponseEntity.ok(carritoActualizado);
    }

    /**
     * Elimina un item específico del carrito del usuario autenticado.
     *
     * @param principal   Objeto de seguridad.
     * @param itemCarritoId ID del ItemCarrito a eliminar (viene de la URL).
     * @return ResponseEntity con el CarritoDTO actualizado.
     */
    @DeleteMapping("/items/{itemCarritoId}")
    public ResponseEntity<CarritoDTO> quitarItemDelCarrito(Principal principal, @PathVariable Long itemCarritoId) {
        if (principal == null || principal.getName() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String vendedorEmail = principal.getName();
        CarritoDTO carritoActualizado = carritoService.quitarItem(vendedorEmail, itemCarritoId);
        return ResponseEntity.ok(carritoActualizado);
    }

    /**
     * Actualiza la cantidad de un item específico en el carrito del usuario autenticado.
     *
     * @param principal   Objeto de seguridad.
     * @param itemCarritoId ID del ItemCarrito a actualizar (viene de la URL).
     * @param cantidadDTO DTO que contiene la nueva cantidad.
     * @return ResponseEntity con el CarritoDTO actualizado.
     */
    @PutMapping("/items/{itemCarritoId}")
    public ResponseEntity<CarritoDTO> actualizarCantidadItem(Principal principal,
                                                             @PathVariable Long itemCarritoId,
                                                             @Valid @RequestBody UpdateCantidadCarritoDTO cantidadDTO) {
        if (principal == null || principal.getName() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String vendedorEmail = principal.getName();
        // Podríamos envolver en try-catch para StockInsuficienteException
        CarritoDTO carritoActualizado = carritoService.actualizarCantidadItem(vendedorEmail, itemCarritoId, cantidadDTO.getNuevaCantidad());
        return ResponseEntity.ok(carritoActualizado);
    }

    /**
     * Vacía completamente el carrito del usuario autenticado.
     *
     * @param principal Objeto de seguridad.
     * @return ResponseEntity con el CarritoDTO vacío.
     */
    @DeleteMapping
    public ResponseEntity<CarritoDTO> vaciarCarrito(Principal principal) {
         if (principal == null || principal.getName() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String vendedorEmail = principal.getName();
        CarritoDTO carritoVacio = carritoService.vaciarCarrito(vendedorEmail);
        return ResponseEntity.ok(carritoVacio);
    }

    // --- Manejo de Excepciones Específico (Opcional pero recomendado) ---
    // Puedes añadir un @ExceptionHandler aquí para capturar StockInsuficienteException
    // y devolver un ResponseEntity con HttpStatus.CONFLICT (409) y un mensaje claro.
    //
    // @ExceptionHandler(StockInsuficienteException.class)
    // public ResponseEntity<String> handleStockInsuficiente(StockInsuficienteException ex) {
    //     return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    // }

}