package com.saolan.soem.configuracion.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.configuracion.dto.ParametroFiltroDTO;
import com.saolan.soem.configuracion.modelo.Parametro;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

/**
 * Test de integración de {@link ParametroRepositorio}. Misma precondición y
 * patrón transaccional que {@code CertEmisRepositorioIT}.
 */
@ExtendWith(ArquillianExtension.class)
class ParametroRepositorioIT {

    @Deployment
    public static Archive<?> crearDespliegue() {
        File archivo = new File("./target/soem.war");
        if (!archivo.exists()) {
            throw new RuntimeException("File " + archivo.getAbsolutePath() + " does not exist.");
        }
        WebArchive war = ShrinkWrap.create(ZipImporter.class, "soem.war").importFrom(archivo).as(WebArchive.class);
        war.addPackage(ParametroRepositorioIT.class.getPackage());
        return war;
    }

    @Inject
    private ParametroRepositorio parametroRepositorio;

    @PersistenceContext(unitName = "soemPU")
    private EntityManager entityManager;

    @Resource
    private UserTransaction userTransaction;

    private final List<Integer> idsCreados = new ArrayList<>();

    @AfterEach
    void limpiar() {
        ejecutarEnTransaccion(() -> {
            for (Integer id : idsCreados) {
                Parametro parametro = entityManager.find(Parametro.class, id);
                if (parametro != null) {
                    entityManager.remove(parametro);
                }
            }
        });
    }

    @Test
    void buscarPorId_deberiaEncontrarElParametroCreado() {
        Parametro parametro = crear(nuevoParametro(true));

        Optional<Parametro> encontrado = parametroRepositorio.buscarPorId(parametro.getParametroId());

        assertTrue(encontrado.isPresent());
    }

    @Test
    void buscarCombo_deberiaIncluirSoloParametrosActivos() {
        Parametro activo = crear(nuevoParametro(true));
        Parametro inactivo = crear(nuevoParametro(false));

        List<ComboDTO> combo = parametroRepositorio.buscarCombo();

        assertTrue(combo.stream().anyMatch(c -> c.valor().equals(activo.getParametroId())));
        assertTrue(combo.stream().noneMatch(c -> c.valor().equals(inactivo.getParametroId())));
    }

    @Test
    void buscarPaginado_deberiaFiltrarPorCodigo_yContarCoherentemente() {
        Parametro parametro = crear(nuevoParametro(true));
        ParametroFiltroDTO filtro = new ParametroFiltroDTO(parametro.getCodigo(), null, true);

        List<Parametro> resultado = parametroRepositorio.buscarPaginado(filtro, new ParametrosBusqueda(1, 20, "codigo", "asc"));

        assertEquals(1, resultado.size());
        assertEquals(parametro.getParametroId(), resultado.get(0).getParametroId());
        assertEquals(1L, parametroRepositorio.contar(filtro));
    }

    private Parametro crear(Parametro parametro) {
        ejecutarEnTransaccion(() -> entityManager.persist(parametro));
        idsCreados.add(parametro.getParametroId());
        return parametro;
    }

    private Parametro nuevoParametro(boolean estado) {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Parametro parametro = new Parametro();
        parametro.setCodigo("PT-" + sufijo);
        parametro.setDescri("Parámetro de prueba " + sufijo);
        parametro.setEstado(estado);
        return parametro;
    }

    private void ejecutarEnTransaccion(Runnable accion) {
        try {
            userTransaction.begin();
            accion.run();
            userTransaction.commit();
        } catch (Exception excepcion) {
            try {
                userTransaction.rollback();
            } catch (Exception excepcionRollback) {
                excepcion.addSuppressed(excepcionRollback);
            }
            throw new IllegalStateException("Error ejecutando operación transaccional en el test", excepcion);
        }
    }
}
