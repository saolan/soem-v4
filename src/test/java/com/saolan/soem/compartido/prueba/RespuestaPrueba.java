package com.saolan.soem.compartido.prueba;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.saolan.soem.compartido.dto.ErrorCampo;
import com.saolan.soem.compartido.dto.Paginacion;
import com.saolan.soem.compartido.dto.RespuestaApi;

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

/**
 * Lectura de {@link RespuestaApi} en los tests de integracion.
 * <p>
 * Yasson 3.0.4 (el proveedor JSON-B de WildFly 40) no deserializa
 * {@code record} genericos: al crear {@code RespuestaApi<T>} por su
 * constructor canonico no resuelve el tipo de {@code datos} y falla con
 * "Error resolving runtime type for type: T". Si deserializa clases
 * genericas, asi que aqui se lee el cuerpo en {@link CuerpoRespuesta} con
 * el tipo concreto de {@code datos} y luego se arma el {@code record}.
 * <p>
 * Solo afecta a la lectura desde los tests: el servidor unicamente
 * serializa {@code RespuestaApi}, lo cual funciona sin problema.
 * <p>
 * Cada IT debe agregar esta clase a su despliegue
 * ({@code war.addClass(RespuestaPrueba.class)}): los tests corren dentro del
 * contenedor.
 */
public final class RespuestaPrueba {

    private RespuestaPrueba() {
    }

    /**
     * @param respuesta respuesta HTTP cuyo cuerpo sigue el contrato {@code RespuestaApi}
     * @param tipoDatos tipo concreto de {@code datos}, p. ej. {@code new GenericType<List<PrecioDTO>>() { }}
     * @return la respuesta deserializada
     */
    public static <T> RespuestaApi<T> leer(Response respuesta, GenericType<T> tipoDatos) {
        Objects.requireNonNull(respuesta, "respuesta");
        Objects.requireNonNull(tipoDatos, "tipoDatos");
        Type tipoCuerpo = new TipoParametrizado(CuerpoRespuesta.class, new Type[] { tipoDatos.getType() });
        CuerpoRespuesta<T> cuerpo = respuesta.readEntity(new GenericType<CuerpoRespuesta<T>>(tipoCuerpo));
        return new RespuestaApi<>(cuerpo.exito, cuerpo.mensaje, cuerpo.datos,
                cuerpo.errores == null ? List.of() : cuerpo.errores, cuerpo.paginacion);
    }

    /**
     * Espejo mutable de {@link RespuestaApi}: JSON-B lo instancia con el
     * constructor sin argumentos y asigna los campos publicos.
     */
    public static class CuerpoRespuesta<T> {
        public boolean exito;
        public String mensaje;
        public T datos;
        public List<ErrorCampo> errores;
        public Paginacion paginacion;
    }

    /** {@code CuerpoRespuesta<X>} construido en tiempo de ejecucion a partir del tipo de {@code datos}. */
    private record TipoParametrizado(Type tipoCrudo, Type[] argumentos) implements ParameterizedType {

        @Override
        public Type[] getActualTypeArguments() {
            return argumentos.clone();
        }

        @Override
        public Type getRawType() {
            return tipoCrudo;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }

        @Override
        public boolean equals(Object otro) {
            return otro instanceof ParameterizedType tipo
                    && tipoCrudo.equals(tipo.getRawType())
                    && tipo.getOwnerType() == null
                    && Arrays.equals(argumentos, tipo.getActualTypeArguments());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(argumentos) ^ tipoCrudo.hashCode();
        }

        @Override
        public String toString() {
            return tipoCrudo.getTypeName() + "<" + Arrays.toString(argumentos) + ">";
        }
    }
}
