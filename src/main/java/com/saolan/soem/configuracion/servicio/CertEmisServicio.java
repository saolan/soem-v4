package com.saolan.soem.configuracion.servicio;

import java.util.List;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.configuracion.repositorio.CertEmisRepositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Servicio minimo del catalogo externo CertEmis: solo expone el combo de
 * solo lectura que consume el agregado Sucursal. El CRUD completo de
 * "cert-emis" queda fuera del alcance de este modulo (ver
 * {@link com.saolan.soem.configuracion.modelo.CertEmis}).
 */
@ApplicationScoped
public class CertEmisServicio {

    @Inject
    private CertEmisRepositorio certEmisRepositorio;

    public List<ComboDTO> combo() {
        return certEmisRepositorio.buscarCombo();
    }
}
