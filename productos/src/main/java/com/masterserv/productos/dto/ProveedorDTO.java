package com.masterserv.productos.dto;

import jakarta.validation.constraints.Email; // Importar
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Set; // Importar

@Data
@EqualsAndHashCode(callSuper = true) // Necesario por heredar de AuditableDTO
public class ProveedorDTO extends AuditableDTO { // <-- 1. Hereda auditoría

    private Long id;

    @NotEmpty(message = "La Razón Social es obligatoria")
    @Size(max = 255)
    private String razonSocial;

    @NotEmpty(message = "El CUIT es obligatorio")
    @Size(max = 20) // (ej. 30-11223344-5)
    private String cuit;

    @Size(max = 100)
    @Email(message = "El formato del email no es válido") // Buena práctica
    private String email;

    @Size(max = 20)
    private String telefono;

    @Size(max = 255)
    private String direccion;
    
    private String estado;
    
    // --- ¡AQUÍ ESTÁ EL CAMPO QUE FALTABA! ---
    private Set<Long> categoriaIds; 
}