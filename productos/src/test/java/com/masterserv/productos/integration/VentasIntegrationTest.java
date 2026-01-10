package com.masterserv.productos.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masterserv.productos.dto.DetalleVentaDTO;
import com.masterserv.productos.dto.VentaDTO;
import com.masterserv.productos.entity.*;
import com.masterserv.productos.enums.EstadoUsuario;
import com.masterserv.productos.repository.*;
import com.masterserv.productos.service.EmailService;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("🛒 Pruebas de Integración - Módulo de Ventas")
public class VentasIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @MockBean
    private EmailService emailService;
    
    @Autowired 
    private CarritoRepository carritoRepository;

    @Autowired
    private CuentaPuntosRepository cuentaPuntosRepository; 
    
    @Autowired
    private MovimientoPuntosRepository movimientoPuntosRepository;

    @Autowired
    private CuponRepository cuponRepository; // <--- (1) Inyectar CuponRepository

    // Datos de prueba
    private Usuario cliente;
    private Usuario vendedor;
    private Producto producto;

    @BeforeEach
    void setup() {
        
        // --- LIMPIEZA DE DATOS (Orden Crítico: Hijos -> Padres) ---
        
        // 1. Limpiar módulo de Puntos (Hijos de Usuarios y Ventas)
        movimientoPuntosRepository.deleteAll();
        cuponRepository.deleteAll(); // <--- (2) Borrar cupones antes de usuarios
        cuentaPuntosRepository.deleteAll();
        
        // 2. Limpiar módulo de Ventas
        // DetalleVenta se borra en cascada con Venta
        ventaRepository.deleteAll();
        carritoRepository.deleteAll(); 
        
        // 3. Limpiar módulo de Productos
        productoRepository.deleteAll();
        categoriaRepository.deleteAll();
        
        // 4. Limpiar Usuarios (Padres de todo)
        usuarioRepository.deleteAll();

        // --- CREACIÓN DE DATOS ---

        // 1. Crear VENDEDOR
        vendedor = new Usuario();
        vendedor.setNombre("Vendedor");
        vendedor.setApellido("Test");
        vendedor.setEmail("vendedor@test.com"); // Email válido
        vendedor.setTelefono("999888777");
        vendedor.setPasswordHash("123456");
        vendedor.setEstado(EstadoUsuario.ACTIVO);
        vendedor.setRoles(new HashSet<>());
        usuarioRepository.save(vendedor);

        // 2. Crear CLIENTE
        cliente = new Usuario();
        cliente.setNombre("Cliente");
        cliente.setApellido("Ventas");
        cliente.setEmail("cliente@ventas.com");
        cliente.setTelefono("111222333");
        cliente.setPasswordHash("123456");
        cliente.setEstado(EstadoUsuario.ACTIVO);
        cliente.setRoles(new HashSet<>());
        usuarioRepository.save(cliente);

        // 3. Crear Categoría
        Categoria categoria = new Categoria();
        categoria.setNombre("Repuestos Test");
        categoria.setEstado("ACTIVO");
        categoriaRepository.save(categoria);

        // 4. Crear Producto con Stock
        producto = new Producto();
        producto.setNombre("Bujía NGK");
        producto.setCodigo("BUJ-001");
        producto.setCategoria(categoria);
        producto.setPrecioCosto(new BigDecimal("500"));
        producto.setPrecioVenta(new BigDecimal("1000"));
        producto.setStockActual(10); // Stock inicial
        producto.setStockMinimo(2);
        producto.setLoteReposicion(5);
        producto.setEstado("ACTIVO");
        productoRepository.save(producto);

        // 5. Crear Carrito para el Vendedor
        Carrito carrito = new Carrito();
        carrito.setVendedor(vendedor); // <--- (3) Usar setVendedor
        carrito.setFechaCreacion(java.time.LocalDateTime.now());
        carrito.setFechaModificacion(java.time.LocalDateTime.now());
        carrito.setItems(new HashSet<>());
        carritoRepository.save(carrito);
    }

    @Test
    @WithMockUser(username = "vendedor@test.com", roles = {"VENDEDOR"}) 
    @DisplayName("✅ Debe registrar venta y descontar stock correctamente")
    void testRegistrarVenta_Exitoso() throws Exception {
        VentaDTO ventaDTO = new VentaDTO();
        ventaDTO.setClienteId(cliente.getId());
        
        List<DetalleVentaDTO> detalles = new ArrayList<>();
        DetalleVentaDTO det = new DetalleVentaDTO();
        det.setProductoId(producto.getId());
        det.setCantidad(3);
        det.setPrecioUnitario(new BigDecimal("1000")); 
        det.setSubtotal(new BigDecimal("3000"));
        detalles.add(det);
        
        ventaDTO.setDetalles(detalles);

        mockMvc.perform(post("/ventas") 
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ventaDTO)))
                .andExpect(status().isCreated()) 
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.totalVenta").value(3000));

        // Verificaciones
        Producto productoActualizado = productoRepository.findById(producto.getId()).get();
        assert(productoActualizado.getStockActual() == 7); // 10 - 3 = 7

        assert(ventaRepository.count() == 1);
    }

    @Test
    @WithMockUser(username = "vendedor@test.com", roles = {"VENDEDOR"})
    @DisplayName("🛑 Debe fallar si no hay suficiente stock")
    void testRegistrarVenta_SinStock() throws Exception {
        VentaDTO ventaDTO = new VentaDTO();
        ventaDTO.setClienteId(cliente.getId());
        
        List<DetalleVentaDTO> detalles = new ArrayList<>();
        DetalleVentaDTO det = new DetalleVentaDTO();
        det.setProductoId(producto.getId());
        det.setCantidad(20); // Exceso
        detalles.add(det);
        ventaDTO.setDetalles(detalles);

        mockMvc.perform(post("/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ventaDTO)))
                .andExpect(status().isBadRequest());

        // Verificaciones
        Producto productoIntacto = productoRepository.findById(producto.getId()).get();
        assert(productoIntacto.getStockActual() == 10);
        
        assert(ventaRepository.count() == 0);
    }
}