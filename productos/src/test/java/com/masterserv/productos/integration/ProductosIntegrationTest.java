package com.masterserv.productos.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masterserv.productos.dto.ProductoDTO;
import com.masterserv.productos.entity.Categoria;
import com.masterserv.productos.repository.CategoriaRepository;
import com.masterserv.productos.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("🛠️ Pruebas de Integración - Módulo de Productos")
public class ProductosIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private CategoriaRepository categoriaRepository;

    private Categoria categoria;

    @BeforeEach
    void setup() {
        productoRepository.deleteAll();
        categoriaRepository.deleteAll();

        categoria = new Categoria();
        categoria.setNombre("Accesorios");
        categoria.setDescripcion("Cascos y guantes");
        categoria.setEstado("ACTIVO");
        categoriaRepository.save(categoria);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("✅ Admin puede crear un producto nuevo")
    void testCrearProducto_Exitoso() throws Exception {
        // --- CORRECCIÓN: Usar Constructor de Record ---
        // Record no tiene setters, se pasa todo en el constructor.
        // Orden: id, codigo, nombre, descripcion, precioVenta, precioCosto, imagenUrl, 
        //        stockActual, stockMinimo, loteReposicion, estado, categoriaId, categoriaNombre, solicitudId
        
        ProductoDTO nuevoProd = new ProductoDTO(
            null, // ID (se genera auto)
            "CAS-LS2-BLK", // Codigo
            "Casco LS2",   // Nombre
            "Casco negro mate", // Descripcion
            new BigDecimal("90000"), // Precio Venta
            new BigDecimal("50000"), // Precio Costo
            null, // Imagen URL
            10,   // Stock Actual
            2,    // Stock Minimo
            5,    // Lote Reposicion
            "ACTIVO", // Estado
            categoria.getId(), // Categoria ID
            null,  // Categoria Nombre (opcional)
            null   // Solicitud ID (opcional)
        );

        mockMvc.perform(post("/productos") 
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nuevoProd)))
                
                .andExpect(status().isCreated()) // O isOk()
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombre").value("Casco LS2"));

        // Verificar persistencia
        assert(productoRepository.count() == 1);
    }
}