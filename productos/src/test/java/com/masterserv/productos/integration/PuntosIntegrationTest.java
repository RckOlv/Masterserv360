package com.masterserv.productos.integration;

import com.masterserv.productos.entity.*;
import com.masterserv.productos.enums.EstadoUsuario; // <--- Importante
import com.masterserv.productos.enums.TipoDescuento;
import com.masterserv.productos.repository.*;
import com.masterserv.productos.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;
import java.math.BigDecimal;
import java.util.HashSet; // <--- Importante

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
public class PuntosIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RecompensaRepository recompensaRepository;

    @Autowired
    private CuentaPuntosRepository cuentaPuntosRepository;

    @Autowired
    private CuponRepository cuponRepository;

    @MockBean
    private EmailService emailService;

    private Usuario cliente;
    private Recompensa recompensa;

    @BeforeEach
    void setup() {
        // Limpieza de tablas
        cuponRepository.deleteAll();
        cuentaPuntosRepository.deleteAll();
        recompensaRepository.deleteAll();
        usuarioRepository.deleteAll();

        // 1. Crear Cliente (ADAPTADO A TU ENTIDAD)
        cliente = new Usuario();
        cliente.setNombre("Juan");
        cliente.setApellido("Test");
        cliente.setEmail("juan@test.com");
        cliente.setTelefono("123456789");
        
        // CORRECCIÓN 1: Usar passwordHash
        cliente.setPasswordHash("123456"); 
        
        // CORRECCIÓN 2: Asignar estado (es obligatorio en tu BD)
        cliente.setEstado(EstadoUsuario.ACTIVO); 
        
        // CORRECCIÓN 3: Inicializar Roles como un Set vacío (o con roles si fuera necesario)
        cliente.setRoles(new HashSet<>()); 

        usuarioRepository.save(cliente);

        // 2. Darle Puntos
        CuentaPuntos cuenta = new CuentaPuntos();
        cuenta.setCliente(cliente);
        cuenta.setSaldoPuntos(1000);
        cuentaPuntosRepository.save(cuenta);

        // 3. Crear Recompensa
        recompensa = new Recompensa();
        recompensa.setDescripcion("Gorra Oficial");
        recompensa.setPuntosRequeridos(500);
        recompensa.setStock(10);
        recompensa.setValor(new BigDecimal("2000"));
        recompensa.setTipoDescuento(TipoDescuento.FIJO);
        recompensa.setActivo(true);
        recompensaRepository.save(recompensa);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testCanjePuntosDesdePos_Exitoso() throws Exception {
        mockMvc.perform(post("/puntos/canje-pos")
                        .param("clienteId", cliente.getId().toString())
                        .param("recompensaId", recompensa.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").exists())
                .andExpect(jsonPath("$.valor").value(2000));

        // Verificaciones
        CuentaPuntos cuentaActualizada = cuentaPuntosRepository.findByCliente(cliente).get();
        assert(cuentaActualizada.getSaldoPuntos() == 500); // 1000 - 500

        Recompensa recompensaActualizada = recompensaRepository.findById(recompensa.getId()).get();
        assert(recompensaActualizada.getStock() == 9); // 10 - 1
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testCanjePuntos_SinSaldo_Falla() throws Exception {
        // Forzar saldo bajo
        CuentaPuntos cuenta = cuentaPuntosRepository.findByCliente(cliente).get();
        cuenta.setSaldoPuntos(100);
        cuentaPuntosRepository.save(cuenta);

        mockMvc.perform(post("/puntos/canje-pos")
                        .param("clienteId", cliente.getId().toString())
                        .param("recompensaId", recompensa.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict()) 
                .andExpect(result -> {
                    // Opcional: imprimir error para depurar
                    // System.out.println(result.getResponse().getContentAsString());
                });
    }
}