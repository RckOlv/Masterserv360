package com.masterserv.productos.service;

import com.masterserv.productos.entity.EmpresaConfig;
import com.masterserv.productos.repository.EmpresaConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmpresaConfigService {

    @Autowired
    private EmpresaConfigRepository repository;

    @Transactional
    public EmpresaConfig obtenerConfiguracion() {
        // Buscamos el ID 1, si no existe, creamos uno por defecto
        return repository.findById(1L).orElseGet(() -> {
            EmpresaConfig config = new EmpresaConfig();
            config.setRazonSocial("MasterServ Repuestos S.A.");
            config.setNombreFantasia("MasterServ");
            config.setCuit("30-12345678-9");
            config.setDireccion("Calle Falsa 123, Buenos Aires");
            config.setTelefono("+54 11 1234 5678");
            config.setEmailContacto("info@masterserv.com");
            config.setColorPrincipal("#E41E26");
            config.setPiePaginaPresupuesto("Presupuesto válido por 10 días. Sujeto a disponibilidad.");
            return repository.save(config);
        });
    }

    @Transactional
    public EmpresaConfig actualizarConfiguracion(EmpresaConfig nuevaConfig) {
        EmpresaConfig actual = obtenerConfiguracion(); // Obtiene la ID 1
        
        actual.setRazonSocial(nuevaConfig.getRazonSocial());
        actual.setNombreFantasia(nuevaConfig.getNombreFantasia());
        actual.setCuit(nuevaConfig.getCuit());
        actual.setDireccion(nuevaConfig.getDireccion());
        actual.setTelefono(nuevaConfig.getTelefono());
        actual.setEmailContacto(nuevaConfig.getEmailContacto());
        actual.setSitioWeb(nuevaConfig.getSitioWeb());
        actual.setLogoUrl(nuevaConfig.getLogoUrl());
        actual.setColorPrincipal(nuevaConfig.getColorPrincipal());
        actual.setPiePaginaPresupuesto(nuevaConfig.getPiePaginaPresupuesto());
        
        return repository.save(actual);
    }
}