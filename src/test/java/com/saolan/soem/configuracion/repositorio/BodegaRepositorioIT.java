package com.saolan.soem.configuracion.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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
import com.saolan.soem.configuracion.dto.BodegaFiltroDTO;
import com.saolan.soem.configuracion.dto.SucuBodeDTO;
import com.saolan.soem.configuracion.modelo.Bodega;
import com.saolan.soem.configuracion.modelo.SucuBode;
import com.saolan.soem.configuracion.modelo.Sucursal;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

/**
 * Test de integración de {@link BodegaRepositorio} y de
 * {@link SucursalRepositorio#buscarBodegas(Integer)}. Misma precondición y
 * patrón transaccional que {@code CertEmisRepositorioIT}.
 */
@ExtendWith(ArquillianExtension.class)
class BodegaRepositorioIT {

    @Deployment
    public static Archive<?> crearDespliegue() {
        File archivo = new File("./target/soem.war");
        if (!archivo.exists()) {
            throw new RuntimeException("File " + archivo.getAbsolutePath() + " does not exist.");
        }
        WebArchive war = ShrinkWrap.create(ZipImporter.class, "soem.war").importFrom(archivo).as(WebArchive.class);
        war.addPackage(BodegaRepositorioIT.class.getPackage());
        return war;
    }

    @Inject
    private BodegaRepositorio bodegaRepositorio;

    @Inject
    private SucursalRepositorio sucursalRepositorio;

    @PersistenceContext(unitName = "soemPU")
    private EntityManager entityManager;

    @Resource
    private UserTransaction userTransaction;

    private final List<Integer> idsCreados = new ArrayList<>();
    private final List<Integer> idsSucursalesCreadas = new ArrayList<>();

    @AfterEach
    void limpiar() {
        ejecutarEnTransaccion(() -> {
            // Primero las sucursales: su orphanRemoval borra SUCU_BODE, que referencia a BODEGA.
            for (Integer id : idsSucursalesCreadas) {
                Sucursal sucursal = entityManager.find(Sucursal.class, id);
                if (sucursal != null) {
                    entityManager.remove(sucursal);
                }
            }
            entityManager.flush();
            for (Integer id : idsCreados) {
                Bodega bodega = entityManager.find(Bodega.class, id);
                if (bodega != null) {
                    entityManager.remove(bodega);
                }
            }
        });
    }

    @Test
    void buscarCombo_deberiaIncluirSoloBodegasActivas() {
        Bodega activa = crear(nuevaBodega(true));
        Bodega inactiva = crear(nuevaBodega(false));

        List<ComboDTO> combo = bodegaRepositorio.buscarCombo();

        assertTrue(combo.stream().anyMatch(c -> c.valor().equals(activa.getBodegaId())));
        assertTrue(combo.stream().noneMatch(c -> c.valor().equals(inactiva.getBodegaId())));
    }

    @Test
    void buscarPorDescri_deberiaIgnorarMayusculas_yRespetarExclusion() {
        Bodega bodega = crear(nuevaBodega(true));
        String descriEnMayusculas = bodega.getDescri().toUpperCase(Locale.ROOT);

        assertTrue(bodegaRepositorio.buscarPorDescri(descriEnMayusculas, null).isPresent());
        assertFalse(bodegaRepositorio.buscarPorDescri(descriEnMayusculas, bodega.getBodegaId()).isPresent());
    }

    @Test
    void buscarPaginado_deberiaFiltrarPorDescri_yContarCoherentemente() {
        Bodega bodega = crear(nuevaBodega(true));
        BodegaFiltroDTO filtro = new BodegaFiltroDTO(bodega.getDescri(), true);

        List<Bodega> resultado = bodegaRepositorio.buscarPaginado(filtro, new ParametrosBusqueda(1, 20, "descri", "asc"));

        assertEquals(1, resultado.size());
        assertEquals(1L, bodegaRepositorio.contar(filtro));
    }

    @Test
    void contarSucursalesAsignadas_deberiaContarSoloLasHabilitadas() {
        Bodega habilitada = crear(nuevaBodega(true));
        Bodega deshabilitada = crear(nuevaBodega(true));
        crearSucursalConBodegas(habilitada, deshabilitada);

        assertEquals(1L, bodegaRepositorio.contarSucursalesAsignadas(habilitada.getBodegaId()));
        assertEquals(0L, bodegaRepositorio.contarSucursalesAsignadas(deshabilitada.getBodegaId()));
    }

    @Test
    void buscarBodegas_deberiaProyectarLasAsignacionesDeLaSucursal() {
        Bodega habilitada = crear(nuevaBodega(true));
        Bodega deshabilitada = crear(nuevaBodega(true));
        Integer sucursalId = crearSucursalConBodegas(habilitada, deshabilitada);

        List<SucuBodeDTO> bodegas = sucursalRepositorio.buscarBodegas(sucursalId);

        assertEquals(2, bodegas.size());
        assertTrue(bodegas.stream().anyMatch(b -> b.bodegaId().equals(habilitada.getBodegaId()) && b.acceso()));
        assertTrue(bodegas.stream().anyMatch(b -> b.bodegaId().equals(deshabilitada.getBodegaId()) && !b.acceso()));
    }

    private Bodega crear(Bodega bodega) {
        ejecutarEnTransaccion(() -> entityManager.persist(bodega));
        idsCreados.add(bodega.getBodegaId());
        return bodega;
    }

    /** Sucursal con la primera bodega habilitada y la segunda deshabilitada. */
    private Integer crearSucursalConBodegas(Bodega habilitada, Bodega deshabilitada) {
        String ruc = String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000_000L, 9_999_999_999_999L));
        Sucursal sucursal = new Sucursal();
        sucursal.setCodigo("BO-" + UUID.randomUUID().toString().substring(0, 8));
        sucursal.setRuc(ruc);
        sucursal.setMatriz(Boolean.FALSE);
        sucursal.setEstado(Boolean.TRUE);
        sucursal.setDescri("Sucursal de prueba bodegas");
        sucursal.setRazonSoci("Razón Social de Prueba");
        sucursal.setCiudad("Ambato");
        sucursal.setDirecc("Av. de Prueba 123");
        sucursal.setDireccEsta("Av. de Prueba 123");
        sucursal.setTelef1("0999999999");
        sucursal.setCorreo("prueba@test.com");
        sucursal.setRepres("Representante de Prueba");
        sucursal.setRepresCedu("0999999999");
        sucursal.setRepresDire("Dirección Representante de Prueba");
        sucursal.setRucCont(ruc);
        sucursal.getBodegas().add(nuevaAsignacion(sucursal, habilitada, true));
        sucursal.getBodegas().add(nuevaAsignacion(sucursal, deshabilitada, false));

        ejecutarEnTransaccion(() -> entityManager.persist(sucursal));
        idsSucursalesCreadas.add(sucursal.getSucursalId());
        return sucursal.getSucursalId();
    }

    private SucuBode nuevaAsignacion(Sucursal sucursal, Bodega bodega, boolean acceso) {
        SucuBode sucuBode = new SucuBode();
        sucuBode.setSucursal(sucursal);
        sucuBode.setBodega(bodega);
        sucuBode.setAcceso(acceso);
        return sucuBode;
    }

    private Bodega nuevaBodega(boolean estado) {
        Bodega bodega = new Bodega();
        bodega.setDescri("Bodega de prueba " + UUID.randomUUID().toString().substring(0, 8));
        bodega.setEstado(estado);
        return bodega;
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
