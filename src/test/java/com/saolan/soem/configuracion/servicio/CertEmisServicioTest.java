package com.saolan.soem.configuracion.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.configuracion.repositorio.CertEmisRepositorio;

/**
 * Test unitario de {@link CertEmisServicio}: es un pass-through de un solo
 * método, se confirma que delega correctamente en {@link CertEmisRepositorio}.
 */
@ExtendWith(MockitoExtension.class)
class CertEmisServicioTest {

    @Mock
    private CertEmisRepositorio certEmisRepositorio;

    @InjectMocks
    private CertEmisServicio certEmisServicio;

    @Test
    void combo_deberiaDelegarEnElRepositorio() {
        when(certEmisRepositorio.buscarCombo()).thenReturn(List.of(new ComboDTO(1, "Certificado Test")));

        List<ComboDTO> resultado = certEmisServicio.combo();

        assertEquals(1, resultado.size());
        assertEquals("Certificado Test", resultado.get(0).etiqueta());
    }
}
