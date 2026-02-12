package com.masterserv.productos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "sesiones_chat")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SesionChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String telefono; // Clave para identificar al usuario

    private String estadoActual; // MENU, BUSCANDO, ETC.

    private Long ultimoProductoId; // ID del producto en contexto

    private LocalDateTime fechaUltimaInteraccion;
}