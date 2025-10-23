package com.masterserv.productos.enums;

/**
 * Enum para gestionar los estados del usuario.
 * Se mapea a un VARCHAR en la BD usando @Enumerated(EnumType.STRING).
 */
public enum EstadoUsuario {
    ACTIVO,
    INACTIVO,
    PENDIENTE,
    BLOQUEADO
}