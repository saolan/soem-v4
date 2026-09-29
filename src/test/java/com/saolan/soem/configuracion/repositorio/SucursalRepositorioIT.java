package com.saolan.soem.configuracion.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
import com.saolan.soem.configuracion.dto.SucursalFiltroDTO;
import com.saolan.soem.configuracion.modelo.Sucursal;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

/**
 * Test de integración de {@link SucursalRepositorio} contra un WildFly y un
 * PostgreSQL reales (via Arquillian, perfil {@code arq-managed}/{@code arq-remote}).
 * <p>
 * <b>Precondición</b>: se asume una base de datos de pruebas dedicada, recién
 * migrada con Flyway. La tabla {@code sucursal} puede no estar vacía (p. ej.
 * la sucursal de {@code db/seed-usuario-prueba.sql}, con {@code matriz=false}
 * y ciudad "Ambato" a propósito, para no interferir con
 * {@code buscarMatrizActual} ni con los filtros por ciudad de este archivo):
 * las aserciones sobre listados comparan por pertenencia
 * ({@code anyMatch}/{@code noneMatch}), no por tamaño total. No correr contra
 * un ambiente con datos de negocio reales de todos modos.
 * <p>
 * {@link SucursalRepositorio#crear(Sucursal)} usa {@code entityManager.persist(...)}
 * sin demarcación propia (por diseño: {@code @Transactional} va solo en la
 * capa Service, según AGENTS.md), así que este test abre su propia
 * transacción JTA con {@link UserTransaction} para cada operación de
 * escritura.
 */
@ExtendWith(ArquillianExtension.class)
class SucursalRepositorioIT {

    @Deployment
    public static Archive<?> crearDespliegue() {
        File archivo = new File("./target/soem.war");
        if (!archivo.exists()) {
            throw new RuntimeException("File " + archivo.getAbsolutePath() + " does not exist.");
        }
        WebArchive war = ShrinkWrap.create(ZipImporter.class, "soem.war").importFrom(archivo).as(WebArchive.class);
        war.addPackage(SucursalRepositorioIT.class.getPackage());
        return war;
    }

    @Inject
    private SucursalRepositorio sucursalRepositorio;

    @PersistenceContext(unitName = "soemPU")
    private EntityManager entityManager;

    @Resource
    private UserTransaction userTransaction;

    private final List<Integer> idsCreados = new ArrayList<>();

    @AfterEach
    void limpiarSucursalesCreadas() {
        ejecutarEnTransaccion(() -> {
            for (Integer id : idsCreados) {
                Sucursal sucursal = entityManager.find(Sucursal.class, id);
                if (sucursal != null) {
                    entityManager.remove(sucursal);
                }
            }
        });
    }

    @Test
    void crear_yBuscarPorId_deberianFuncionarComoRoundTrip() {
        Sucursal sucursal = crear(nuevaSucursal(codigoAleatorio(), rucAleatorio(), false));

        Optional<Sucursal> encontrada = sucursalRepositorio.buscarPorId(sucursal.getSucursalId());

        assertTrue(encontrada.isPresent());
        assertEquals(sucursal.getCodigo(), encontrada.get().getCodigo());
        assertEquals(sucursal.getRuc(), encontrada.get().getRuc());
    }

    @Test
    void buscarPorCodigo_deberiaEncontrarYExcluirIdIndicado() {
        String codigo = codigoAleatorio();
        Sucursal sucursal = crear(nuevaSucursal(codigo, rucAleatorio(), false));

        assertTrue(sucursalRepositorio.buscarPorCodigo(codigo, null).isPresent());
        assertTrue(sucursalRepositorio.buscarPorCodigo(codigo, sucursal.getSucursalId()).isEmpty());
    }

    @Test
    void buscarPorRuc_deberiaEncontrarYExcluirIdIndicado() {
        String ruc = rucAleatorio();
        Sucursal sucursal = crear(nuevaSucursal(codigoAleatorio(), ruc, false));

        assertTrue(sucursalRepositorio.buscarPorRuc(ruc, null).isPresent());
        assertTrue(sucursalRepositorio.buscarPorRuc(ruc, sucursal.getSucursalId()).isEmpty());
    }

    @Test
    void buscarMatrizActual_deberiaEncontrarLaSucursalMarcadaComoMatriz() {
        Sucursal matriz = crear(nuevaSucursal(codigoAleatorio(), rucAleatorio(), true));

        Optional<Sucursal> resultado = sucursalRepositorio.buscarMatrizActual(null);

        assertTrue(resultado.isPresent());
        assertEquals(matriz.getSucursalId(), resultado.get().getSucursalId());
    }

    @Test
    void buscarMatrizActual_deberiaExcluirIdIndicado() {
        Sucursal matriz = crear(nuevaSucursal(codigoAleatorio(), rucAleatorio(), true));

        Optional<Sucursal> resultado = sucursalRepositorio.buscarMatrizActual(matriz.getSucursalId());

        assertTrue(resultado.isEmpty());
    }

    @Test
    void buscarPaginado_deberiaFiltrarPorCiudad() {
        crear(nuevaSucursal(codigoAleatorio(), rucAleatorio(), false, "Quito"));
        crear(nuevaSucursal(codigoAleatorio(), rucAleatorio(), false, "Guayaquil"));

        SucursalFiltroDTO filtro = new SucursalFiltroDTO(null, null, null, "Quito", null, null);
        List<Sucursal> resultado = sucursalRepositorio.buscarPaginado(filtro, new ParametrosBusqueda(1, 20, "descri", "asc"));

        assertEquals(1, resultado.size());
        assertEquals("Quito", resultado.get(0).getCiudad());
    }

    @Test
    void buscarPaginado_deberiaUsarCampoPorDefecto_cuandoOrdenarPorNoEstaPermitido() {
        // No se asume tabla vacia: puede existir la sucursal semilla de
        // db/seed-usuario-prueba.sql (u otras). Solo se confirma que la
        // consulta no falla con un "ordenarPor" invalido y que devuelve la
        // sucursal recien creada.
        Sucursal creada = crear(nuevaSucursal(codigoAleatorio(), rucAleatorio(), false));

        List<Sucursal> resultado = sucursalRepositorio.buscarPaginado(
                new SucursalFiltroDTO(null, null, null, null, null, null),
                new ParametrosBusqueda(1, 200, "campoInexistente; DROP TABLE sucursal", "asc"));

        assertTrue(resultado.stream().anyMatch(s -> s.getSucursalId().equals(creada.getSucursalId())));
    }

    @Test
    void contar_deberiaContarSoloLoQueCoincideConElFiltro() {
        crear(nuevaSucursal(codigoAleatorio(), rucAleatorio(), false, "Cuenca"));
        crear(nuevaSucursal(codigoAleatorio(), rucAleatorio(), false, "Cuenca"));
        crear(nuevaSucursal(codigoAleatorio(), rucAleatorio(), false, "Loja"));

        long total = sucursalRepositorio.contar(new SucursalFiltroDTO(null, null, null, "Cuenca", null, null));

        assertEquals(2L, total);
    }

    @Test
    void buscarCombo_deberiaIncluirSoloSucursalesActivas() {
        // No se asume tabla vacia (ver nota de buscarPaginado_...): se
        // compara por pertenencia, no por tamano total del combo.
        Sucursal activa = crear(nuevaSucursal(codigoAleatorio(), rucAleatorio(), false));
        Sucursal inactiva = nuevaSucursal(codigoAleatorio(), rucAleatorio(), false);
        inactiva.setEstado(Boolean.FALSE);
        crear(inactiva);

        List<ComboDTO> combo = sucursalRepositorio.buscarCombo();

        assertTrue(combo.stream().anyMatch(c -> c.valor().equals(activa.getSucursalId())));
        assertTrue(combo.stream().noneMatch(c -> c.valor().equals(inactiva.getSucursalId())));
    }

    // ---------- helpers ----------

    private Sucursal crear(Sucursal sucursal) {
        ejecutarEnTransaccion(() -> sucursalRepositorio.crear(sucursal));
        idsCreados.add(sucursal.getSucursalId());
        return sucursal;
    }

    private Sucursal nuevaSucursal(String codigo, String ruc, boolean matriz) {
        return nuevaSucursal(codigo, ruc, matriz, "Quito");
    }

    private Sucursal nuevaSucursal(String codigo, String ruc, boolean matriz, String ciudad) {
        Sucursal sucursal = new Sucursal();
        sucursal.setCodigo(codigo);
        sucursal.setRuc(ruc);
        sucursal.setMatriz(matriz);
        sucursal.setEstado(Boolean.TRUE);
        sucursal.setDescri("Sucursal de prueba");
        sucursal.setRazonSoci("Razón Social de Prueba");
        sucursal.setCiudad(ciudad);
        sucursal.setDirecc("Av. de Prueba 123");
        sucursal.setDireccEsta("Av. de Prueba 123");
        sucursal.setTelef1("0999999999");
        sucursal.setCorreo("prueba@test.com");
        sucursal.setRepres("Representante de Prueba");
        sucursal.setRepresCedu("0999999999");
        sucursal.setRepresDire("Dirección Representante de Prueba");
        sucursal.setRucCont(ruc);
        return sucursal;
    }

    private String codigoAleatorio() {
        return "T" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String rucAleatorio() {
        long numero = Math.abs(UUID.randomUUID().getLeastSignificantBits()) % 10_000_000_000L;
        return String.format(Locale.ROOT, "%013d", numero);
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
