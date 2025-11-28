package com.masterserv.productos.dto;

import com.masterserv.productos.enums.EstadoCupon;
import com.masterserv.productos.enums.TipoDescuento; // <-- Mentor: IMPORTADO
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CuponDTO {

    private Long id;
    private String codigo;
    private LocalDate fechaVencimiento;
    private EstadoCupon estado;
    private String clienteEmail; // Para mostrar a quién pertenece

    // --- Mentor: INICIO DE LA MODIFICACIÓN (V2) ---
    // (Reemplazamos 'descuento' por estos campos)
    
    private BigDecimal valor; // (Ej: 500 o 20)
    
    private TipoDescuento tipoDescuento; // (FIJO o PORCENTAJE)
    
    private Long categoriaId; // (Opcional, ej: 4)
    
    private String categoriaNombre; // (Opcional, ej: "Frenos")
    // --- Mentor: FIN DE LA MODIFICACIÓN ---
}