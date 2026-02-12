package com.masterserv.productos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "alertas_admin") // Tabla separada para no ensuciar la de notificaciones al cliente
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo; // Ej: "Solicitud de Humano"

    @Column(columnDefinition = "TEXT")
    private String mensaje; // Ej: "Juan Perez pide ayuda..."

    private String urlRedireccion; // Opcional: Para hacer clic e ir al perfil del cliente

    @Column(nullable = false)
    private boolean leida = false; // ¡CLAVE! Para el puntito rojo

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    // Relación opcional: ¿Quién generó la alerta? (El cliente)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_origen_id")
    private Usuario clienteOrigen; 
}