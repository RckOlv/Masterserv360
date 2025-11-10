package com.masterserv.productos.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción de negocio que se lanza cuando un cupón no es válido.
 * Devuelve un 400 (Bad Request) al cliente.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST) // 400 Bad Request
public class CuponNoValidoException extends RuntimeException {
    public CuponNoValidoException(String message) {
        super(message);
    }
}