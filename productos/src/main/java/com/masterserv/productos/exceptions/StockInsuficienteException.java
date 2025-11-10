package com.masterserv.productos.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción de negocio que se lanza cuando se solicita más stock del disponible.
 * Devuelve un 409 (Conflict) al cliente, ya que la solicitud es válida
 * pero entra en conflicto con el estado actual del inventario.
 */
@ResponseStatus(HttpStatus.CONFLICT) // 409 Conflict
public class StockInsuficienteException extends RuntimeException {
    public StockInsuficienteException(String message) {
        super(message);
    }
}