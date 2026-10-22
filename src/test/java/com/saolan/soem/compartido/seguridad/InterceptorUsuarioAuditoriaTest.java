package com.saolan.soem.compartido.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.inject.Instance;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionSynchronizationRegistry;

/**
 * Tests unitarios de {@link InterceptorUsuarioAuditoria}: publica el usuario
 * del JWT una sola vez por transaccion y no hace nada sin usuario
 * autenticado o sin transaccion activa.
 */
@ExtendWith(MockitoExtension.class)
class InterceptorUsuarioAuditoriaTest {

    private static final String CLAVE_RECURSO = InterceptorUsuarioAuditoria.class.getName();

    @Mock
    private EntityManager entityManager;

    @Mock
    private TransactionSynchronizationRegistry registroTransaccion;

    @Mock
    private Instance<JsonWebToken> jsonWebToken;

    @Mock
    private JsonWebToken token;

    @Mock
    private Query query;

    @Mock
    private InvocationContext contexto;

    @InjectMocks
    private InterceptorUsuarioAuditoria interceptor;

    @Test
    void deberiaPublicarElUsuario_cuandoHayTransaccionYTokenYAunNoSePublico() throws Exception {
        when(registroTransaccion.getTransactionStatus()).thenReturn(Status.STATUS_ACTIVE);
        when(registroTransaccion.getResource(CLAVE_RECURSO)).thenReturn(null);
        when(jsonWebToken.get()).thenReturn(token);
        when(token.getRawToken()).thenReturn("jwt");
        when(token.getName()).thenReturn("ss");
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(1, "ss")).thenReturn(query);
        when(contexto.proceed()).thenReturn("resultado");

        assertEquals("resultado", interceptor.publicarUsuario(contexto));

        verify(query).getSingleResult();
        verify(registroTransaccion).putResource(CLAVE_RECURSO, "ss");
    }

    @Test
    void noDeberiaRepetirLaConsulta_cuandoYaSePublicoEnLaTransaccion() throws Exception {
        when(registroTransaccion.getTransactionStatus()).thenReturn(Status.STATUS_ACTIVE);
        when(registroTransaccion.getResource(CLAVE_RECURSO)).thenReturn("ss");

        interceptor.publicarUsuario(contexto);

        verify(entityManager, never()).createNativeQuery(anyString());
        verify(contexto).proceed();
    }

    @Test
    void noDeberiaPublicar_cuandoNoHayTokenEnLaSolicitud() throws Exception {
        when(registroTransaccion.getTransactionStatus()).thenReturn(Status.STATUS_ACTIVE);
        when(jsonWebToken.get()).thenReturn(token);
        when(token.getRawToken()).thenReturn(null);

        interceptor.publicarUsuario(contexto);

        verify(entityManager, never()).createNativeQuery(anyString());
        verify(registroTransaccion, never()).putResource(any(), any());
        verify(contexto).proceed();
    }

    @Test
    void noDeberiaPublicar_cuandoNoHayContextoDeSolicitud() throws Exception {
        when(registroTransaccion.getTransactionStatus()).thenReturn(Status.STATUS_ACTIVE);
        when(jsonWebToken.get()).thenThrow(new ContextNotActiveException("sin solicitud"));

        interceptor.publicarUsuario(contexto);

        verify(entityManager, never()).createNativeQuery(anyString());
        verify(contexto).proceed();
    }

    @Test
    void noDeberiaPublicar_cuandoNoHayTransaccionActiva() throws Exception {
        when(registroTransaccion.getTransactionStatus()).thenReturn(Status.STATUS_NO_TRANSACTION);

        interceptor.publicarUsuario(contexto);

        verify(entityManager, never()).createNativeQuery(anyString());
        verify(contexto).proceed();
    }
}
