package com.masterserv.productos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "interacciones_chatbot")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InteraccionChatbot extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob // Para textos largos
    @Column(name = "mensaje_usuario", columnDefinition = "TEXT")
    private String mensajeUsuario;

    @Lob
    @Column(name = "respuesta_bot", columnDefinition = "TEXT")
    private String respuestaBot;

    @Column(nullable = false)
    private LocalDateTime fecha;

    // --- Relaciones ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id") // Anulable (para chats anónimos)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id") // Anulable (si la consulta no es de un producto)
    private Producto producto;
}