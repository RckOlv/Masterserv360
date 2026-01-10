package com.masterserv.productos.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masterserv.productos.dto.DetallePedidoDTO;
import com.masterserv.productos.dto.PedidoDTO;
import com.masterserv.productos.entity.*;
import com.masterserv.productos.enums.EstadoUsuario; // <--- Importante
import com.masterserv.productos.repository.*;
import com.masterserv.productos.service.EmailService;
import com.masterserv.productos.service.PdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set; // <--- Importar Set

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("📦 Pruebas de Integración - Módulo de Pedidos (Compras)")
public class PedidosIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private PedidoRepository pedidoRepository;

    @MockBean private EmailService emailService;
    @MockBean private PdfService pdfService;

    private Usuario admin;
    private Proveedor proveedor;
    private Producto producto;

    @BeforeEach
    void setup() {
        // Limpieza en orden
        pedidoRepository.deleteAll();
        productoRepository.deleteAll();
        categoriaRepository.deleteAll();
        proveedorRepository.deleteAll(); // Borrar proveedores antes de usuarios si hay relación
        usuarioRepository.deleteAll();

        // 1. Admin que hace el pedido
        admin = new Usuario();
        admin.setNombre("Admin");
        admin.setApellido("Compras");
        admin.setEmail("admin@masterserv.com");
        admin.setPasswordHash("123456");
        admin.setTelefono("111");
        admin.setEstado(EstadoUsuario.ACTIVO);
        admin.setRoles(new HashSet<>());
        usuarioRepository.save(admin);

        // 2. Proveedor
        proveedor = new Proveedor();
        proveedor.setRazonSocial("Honda Oficial");
        proveedor.setCuit("30-12345678-9");
        proveedor.setEmail("ventas@honda.com");
        
        proveedor.setEstado(EstadoUsuario.ACTIVO); 
        
        proveedorRepository.save(proveedor);

        // 3. Producto
        Categoria cat = new Categoria();
        cat.setNombre("Motores");
        cat.setEstado("ACTIVO"); // String o Enum según tu entidad Categoria
        categoriaRepository.save(cat);

        producto = new Producto();
        producto.setNombre("Pistón 110cc");
        producto.setCodigo("PIS-110");
        producto.setCategoria(cat);
        producto.setPrecioCosto(new BigDecimal("5000"));
        producto.setPrecioVenta(new BigDecimal("10000"));
        producto.setStockActual(5);
        producto.setStockMinimo(10);
        producto.setEstado("ACTIVO");
        productoRepository.save(producto);
    }

    @Test
    @WithMockUser(username = "admin@masterserv.com", roles = {"ADMIN"})
    @DisplayName("✅ Debe crear pedido PENDIENTE y notificar al proveedor")
    void testCrearPedido_Exitoso() throws Exception {
        // --- DATA DEL PEDIDO ---
        PedidoDTO dto = new PedidoDTO();
        dto.setUsuarioId(admin.getId());
        dto.setProveedorId(proveedor.getId());
        
        DetallePedidoDTO detalle = new DetallePedidoDTO();
        detalle.setProductoId(producto.getId());
        detalle.setCantidad(50); 
        detalle.setPrecioUnitario(new BigDecimal("5000")); 
        
        // --- CORRECCIÓN 2: Convertir a Set ---
        // Tu DTO espera un Set<DetallePedidoDTO>, no una List.
        Set<DetallePedidoDTO> detallesSet = new HashSet<>();
        detallesSet.add(detalle);
        dto.setDetalles(detallesSet);

        // --- EJECUCIÓN ---
        // Nota: Ajusta "/pedidos" si tu controlador usa "/api/pedidos" o diferente
        mockMvc.perform(post("/pedidos") 
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                
                // --- VALIDACIONES HTTP ---
                .andExpect(status().isCreated()) // O isOk()
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.token").isNotEmpty()); 

        // --- VALIDACIONES DE INTEGRACIÓN (MOCKS) ---
        verify(emailService).enviarEmailConAdjunto(
                eq("ventas@honda.com"), 
                contains("Nueva Orden de Compra"), 
                any(), 
                any(), 
                any()
        );
    }
}