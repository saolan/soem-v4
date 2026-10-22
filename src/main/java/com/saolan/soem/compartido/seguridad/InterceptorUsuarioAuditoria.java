package com.saolan.soem.compartido.seguridad;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.microprofile.jwt.JsonWebToken;

import jakarta.annotation.Priority;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.Transactional;

/**
 * Publica el usuario autenticado (claim {@code upn} del JWT) en la variable
 * de sesion PostgreSQL {@code soem.usuario}, que la funcion
 * {@code soem_usuario_actual()} (migracion V6) usa como DEFAULT de
 * {@code usuari_regi} y en el trigger {@code trg_marca_modificacion} para
 * {@code usuari_modi}.
 * <p>
 * Se enlaza a {@code @Transactional} (el {@code TxType} es parte del binding:
 * aplica a {@code REQUIRED}, el valor que usan los Service) y su prioridad es
 * posterior a la del interceptor transaccional de Narayana
 * ({@code PLATFORM_BEFORE + 200}), de modo que corre ya dentro de la
 * transaccion. {@code set_config(..., true)} limita el valor a esa
 * transaccion, asi no se filtra a otra solicitud que reutilice la conexion
 * del pool. Se ejecuta una sola vez por transaccion.
 * <p>
 * Sin usuario autenticado (login, tareas programadas) no hace nada y la BD
 * recae en {@code current_user}.
 */
@Transactional
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 300)
public class InterceptorUsuarioAuditoria implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(InterceptorUsuarioAuditoria.class.getName());
    private static final String CLAVE_RECURSO = InterceptorUsuarioAuditoria.class.getName();

    @PersistenceContext
    private EntityManager entityManager;

    @Resource
    private TransactionSynchronizationRegistry registroTransaccion;

    @Inject
    private Instance<JsonWebToken> jsonWebToken;

    @AroundInvoke
    public Object publicarUsuario(InvocationContext contexto) throws Exception {
        if (registroTransaccion.getTransactionStatus() == Status.STATUS_ACTIVE
                && registroTransaccion.getResource(CLAVE_RECURSO) == null) {
            String usuario = usuarioAutenticado();
            if (usuario != null) {
                entityManager.createNativeQuery("SELECT set_config('soem.usuario', ?1, true)")
                        .setParameter(1, usuario)
                        .getSingleResult();
                registroTransaccion.putResource(CLAVE_RECURSO, usuario);
            }
        }
        return contexto.proceed();
    }

    private String usuarioAutenticado() {
        try {
            JsonWebToken token = jsonWebToken.get();
            return token.getRawToken() == null ? null : token.getName();
        } catch (ContextNotActiveException excepcion) {
            // Fuera de una solicitud HTTP (p. ej. temporizador EJB): no hay usuario de aplicacion.
            LOGGER.log(Level.FINE, "Sin contexto de solicitud: se audita con el usuario de la base de datos");
            return null;
        }
    }
}
