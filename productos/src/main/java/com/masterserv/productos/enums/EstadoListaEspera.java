package com.masterserv.productos.enums;

public enum EstadoListaEspera {
    PENDIENTE,  // El cliente espera
    NOTIFICADA, // Ya le avisamos
    CANCELADA   // El cliente se cansó de esperar
}