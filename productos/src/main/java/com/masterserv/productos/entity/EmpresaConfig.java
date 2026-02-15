package com.masterserv.productos.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "empresa_config")
@Data
public class EmpresaConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Datos Básicos
    private String razonSocial;
    private String nombreFantasia;
    private String cuit;
    
    // Contacto
    private String direccion;
    private String telefono;
    private String emailContacto;
    private String sitioWeb;

    // Visual
    @Column(columnDefinition = "TEXT") 
    private String logoUrl;
    
    private String colorPrincipal; // Ej: #E41E26 (Para el PDF o Frontend)
    
    // Textos Legales
    @Column(length = 2000)
    private String piePaginaPresupuesto; // "Oferta válida por 7 días..."
}