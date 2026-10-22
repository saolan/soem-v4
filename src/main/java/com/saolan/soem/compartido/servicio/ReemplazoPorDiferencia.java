package com.saolan.soem.compartido.servicio;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.saolan.soem.compartido.dto.AsignacionAccesoDTO;
import com.saolan.soem.compartido.dto.ErrorCampo;
import com.saolan.soem.compartido.excepcion.ExcepcionValidacion;

/**
 * Reemplazo en bloque de una composicion "duenio-recurso" con flag
 * {@code acceso} (p. ej. {@code rol_perm}, {@code rol_menu},
 * {@code sucu_bode}), aplicado por diferencia: actualiza el {@code acceso}
 * de las filas que siguen, quita las ausentes y agrega solo las nuevas.
 * <p>
 * No se usa {@code clear()} + {@code add}: Hibernate ejecuta los INSERT
 * antes que los DELETE de huerfanos, y reenviar un recurso ya asignado
 * violaria la unicidad {@code (<duenio>_id, <recurso>_id)} de la tabla. Debe
 * invocarse dentro de la transaccion del Service duenio de la coleccion.
 */
public final class ReemplazoPorDiferencia {

    private ReemplazoPorDiferencia() {
    }

    /**
     * @param asignadas     coleccion actual del duenio (se modifica en sitio)
     * @param asignaciones  estado deseado; un id repetido se rechaza
     * @param campo         campo reportado en los errores de validacion
     * @param idDe          id del recurso referenciado por una fila
     * @param asignarAcceso actualiza el {@code acceso} de una fila existente
     * @param crear         construye la fila nueva (valida que el recurso exista)
     */
    public static <T> void aplicar(List<T> asignadas, List<AsignacionAccesoDTO> asignaciones, String campo,
            Function<T, Integer> idDe, BiConsumer<T, Boolean> asignarAcceso, BiFunction<Integer, Boolean, T> crear) {
        Map<Integer, Boolean> accesoPorId = new LinkedHashMap<>();
        for (AsignacionAccesoDTO asignacion : asignaciones) {
            if (accesoPorId.put(asignacion.valor(), asignacion.acceso()) != null) {
                String mensaje = "El identificador " + asignacion.valor() + " está repetido";
                throw new ExcepcionValidacion(mensaje, List.of(new ErrorCampo(campo, mensaje)));
            }
        }

        asignadas.removeIf(fila -> !accesoPorId.containsKey(idDe.apply(fila)));
        Map<Integer, T> existentes = new HashMap<>();
        for (T fila : asignadas) {
            existentes.put(idDe.apply(fila), fila);
        }

        for (Map.Entry<Integer, Boolean> entrada : accesoPorId.entrySet()) {
            T existente = existentes.get(entrada.getKey());
            if (existente != null) {
                asignarAcceso.accept(existente, entrada.getValue());
            } else {
                asignadas.add(crear.apply(entrada.getKey(), entrada.getValue()));
            }
        }
    }
}
