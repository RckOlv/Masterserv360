package com.masterserv.productos.service;

import com.masterserv.productos.entity.Cupon;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.entity.Venta;
import com.masterserv.productos.enums.EstadoCupon;
import com.masterserv.productos.exceptions.CuponNoValidoException;
import com.masterserv.productos.repository.CuponRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class CuponService {

    @Autowired
    private CuponRepository cuponRepository;

    /**
     * Valida y aplica un cupón a una Venta.
     * Este método se llama DESDE VentaService, por lo que se une a su transacción.
     * Si este método falla (ej. CuponNoValidoException), toda la Venta hará rollback.
     *
     * @param codigoCupon El código del cupón.
     * @param venta La entidad Venta (aún no guardada) a la que se aplicará.
     * @param cliente El Usuario (cliente) que realiza la compra.
     * @return El Cupón validado y listo para ser guardado.
     */
    @Transactional(propagation = Propagation.MANDATORY) // Se une a la transacción de VentaService
    public Cupon validarYAplicarCupon(String codigoCupon, Venta venta, Usuario cliente) {
        
        // 1. Buscar el cupón
        Cupon cupon = cuponRepository.findByCodigo(codigoCupon)
            .orElseThrow(() -> new CuponNoValidoException("El cupón '" + codigoCupon + "' no existe."));

        // 2. Validar Estado
        if (cupon.getEstado() != EstadoCupon.VIGENTE) { // Usando tu Enum 'VIGENTE'
            throw new CuponNoValidoException("El cupón '" + codigoCupon + "' ya fue " + cupon.getEstado() + ".");
        }

        // 3. Validar Fecha de Vencimiento
        if (cupon.getFechaVencimiento().isBefore(LocalDate.now())) {
            cupon.setEstado(EstadoCupon.VENCIDO); // Lo marcamos como vencido
            cuponRepository.save(cupon);
            throw new CuponNoValidoException("El cupón '" + codigoCupon + "' ha vencido.");
        }

        // 4. ¡Validación de Seguridad CRÍTICA!
        if (!cupon.getCliente().getId().equals(cliente.getId())) {
            throw new CuponNoValidoException("Este cupón no pertenece a este cliente.");
        }

        // --- ¡Éxito! Aplicamos el cupón ---
        
        // 5. "Quemar" el cupón para que no se vuelva a usar
        cupon.setEstado(EstadoCupon.USADO);
        cupon.setVenta(venta); // Vinculamos el cupón a esta venta

        // 6. Vincular la Venta al Cupón (si tienes la relación bidireccional)
        venta.setCupon(cupon); // (Añade este campo @OneToOne en tu entidad Venta)

        // Nota: No necesitamos 'cuponRepository.save(cupon)' aquí.
        // Como la Venta es la dueña de la transacción, al guardar la Venta
        // (o al hacer commit), JPA guardará los cambios en la entidad 'cupon'
        // que está gestionando (managed).
        
        return cupon;
    }
}