package com.saolan.soem.compartido.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.saolan.soem.compartido.dto.AsignacionAccesoDTO;
import com.saolan.soem.compartido.excepcion.ExcepcionValidacion;

/**
 * Tests unitarios de {@link ReemplazoPorDiferencia} sobre una fila de prueba
 * mutable: conserva las filas que siguen, quita las ausentes, agrega solo las
 * nuevas y rechaza ids repetidos sin modificar la coleccion.
 */
class ReemplazoPorDiferenciaTest {

    @Test
    void aplicar_deberiaConservarActualizarQuitarYAgregar() {
        Fila conservada = new Fila(1, true);
        List<Fila> asignadas = new ArrayList<>(List.of(conservada, new Fila(2, true)));
        List<Integer> creadas = new ArrayList<>();

        aplicar(asignadas, List.of(new AsignacionAccesoDTO(1, false), new AsignacionAccesoDTO(3, true)), creadas);

        assertEquals(2, asignadas.size());
        assertSame(conservada, asignadas.get(0));
        assertEquals(Boolean.FALSE, conservada.acceso);
        assertEquals(3, asignadas.get(1).id);
        assertEquals(List.of(3), creadas);
    }

    @Test
    void aplicar_conListaVacia_deberiaQuitarTodas() {
        List<Fila> asignadas = new ArrayList<>(List.of(new Fila(1, true), new Fila(2, false)));

        aplicar(asignadas, List.of(), new ArrayList<>());

        assertTrue(asignadas.isEmpty());
    }

    @Test
    void aplicar_deberiaRechazarIdsRepetidosSinModificarLaColeccion() {
        List<Fila> asignadas = new ArrayList<>(List.of(new Fila(1, true)));
        List<AsignacionAccesoDTO> repetidas = List.of(new AsignacionAccesoDTO(5, true), new AsignacionAccesoDTO(5, false));

        ExcepcionValidacion excepcion = assertThrows(ExcepcionValidacion.class,
                () -> aplicar(asignadas, repetidas, new ArrayList<>()));

        assertEquals("filas", excepcion.getErrores().get(0).campo());
        assertEquals(1, asignadas.size());
    }

    private void aplicar(List<Fila> asignadas, List<AsignacionAccesoDTO> asignaciones, List<Integer> creadas) {
        ReemplazoPorDiferencia.aplicar(asignadas, asignaciones, "filas",
                fila -> fila.id,
                (fila, acceso) -> fila.acceso = acceso,
                (id, acceso) -> {
                    creadas.add(id);
                    return new Fila(id, acceso);
                });
    }

    private static final class Fila {
        private final Integer id;
        private Boolean acceso;

        private Fila(Integer id, Boolean acceso) {
            this.id = id;
            this.acceso = acceso;
        }
    }
}
