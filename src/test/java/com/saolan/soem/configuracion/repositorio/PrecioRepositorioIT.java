package com.saolan.soem.configuracion.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

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
import com.saolan.soem.configuracion.dto.PrecioFiltroDTO;
import com.saolan.soem.configuracion.modelo.Precio;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

/**
 * Test de integracion de {@link PrecioRepositorio}, incluida la generacion
 * masiva del alcance "todos" contra los productos y sucursales reales de la
 * base de pruebas. Misma precondicion y patron transaccional que
 * {@code BodegaRepositorioIT}.
 */
@ExtendWith(ArquillianExtension.class)
class PrecioRepositorioIT {

    @Deployment
    public static Archive<?> crearDespliegue() {
        File archivo = new File("./target/soem.war");
        if (!archivo.exists()) {
            throw new RuntimeException("File " + archivo.getAbsolutePath() + " does not exist.");
        }
        WebArchive war = ShrinkWrap.create(ZipImporter.class, "soem.war").importFrom(archivo).as(WebArchive.class);
        war.addPackage(PrecioRepositorioIT.class.getPackage());
        return war;
    }

    @Inject
    private PrecioRepositorio precioRepositorio;

    @PersistenceContext(unitName = "soemPU")
    private EntityManager entityManager;

    @Resource
    private UserTransaction userTransaction;

    private final List<Integer> idsCreados = new ArrayList<>();

    @AfterEach
    void limpiar() {
        ejecutarEnTransaccion(() -> {
            for (Integer id : idsCreados) {
                // Las filas generadas por el alcance "todos" referencian a PRECIO.
                entityManager.createNativeQuery("DELETE FROM prod_prec WHERE precio_id = ?1").setParameter(1, id).executeUpdate();
                entityManager.createNativeQuery("DELETE FROM sucu_prec WHERE precio_id = ?1").setParameter(1, id).executeUpdate();
                Precio precio = entityManager.find(Precio.class, id);
                if (precio != null) {
                    entityManager.remove(precio);
                }
            }
            return null;
        });
    }

    @Test
    void buscarCombo_deberiaIncluirSoloListasActivas() {
        Precio activa = crear(nuevoPrecio(true));
        Precio inactiva = crear(nuevoPrecio(false));

        List<ComboDTO> combo = precioRepositorio.buscarCombo();

        assertTrue(combo.stream().anyMatch(c -> c.valor().equals(activa.getPrecioId())));
        assertTrue(combo.stream().noneMatch(c -> c.valor().equals(inactiva.getPrecioId())));
    }

    @Test
    void buscarPorCodigoYDescri_deberianIgnorarMayusculas_yRespetarExclusion() {
        Precio precio = crear(nuevoPrecio(true));
        String codigo = precio.getCodigo().toLowerCase(Locale.ROOT);
        String descri = precio.getDescri().toUpperCase(Locale.ROOT);

        assertTrue(precioRepositorio.buscarPorCodigo(codigo, null).isPresent());
        assertFalse(precioRepositorio.buscarPorCodigo(codigo, precio.getPrecioId()).isPresent());
        assertTrue(precioRepositorio.buscarPorDescri(descri, null).isPresent());
        assertFalse(precioRepositorio.buscarPorDescri(descri, precio.getPrecioId()).isPresent());
    }

    @Test
    void buscarPaginado_deberiaFiltrarPorCodigo_yContarCoherentemente() {
        Precio precio = crear(nuevoPrecio(true));
        PrecioFiltroDTO filtro = new PrecioFiltroDTO(precio.getCodigo(), null, true);

        List<Precio> resultado = precioRepositorio.buscarPaginado(filtro, new ParametrosBusqueda(1, 20, "codigo", "desc"));

        assertEquals(1, resultado.size());
        assertEquals(1L, precioRepositorio.contar(filtro));
    }

    @Test
    void alcanceTodos_deberiaGenerarPreciosEnCeroParaProductosYSucursalesActivos() {
        Precio precio = crear(nuevoPrecio(true));
        long sucursalesActivas = contarNativo("SELECT count(*) FROM sucursal WHERE estado = true");
        long productosActivos = contarNativo("SELECT count(*) FROM producto WHERE estado = true");

        int sucursales = ejecutarEnTransaccion(() -> precioRepositorio.habilitarEnSucursalesActivas(precio.getPrecioId()));
        int precios = ejecutarEnTransaccion(() -> precioRepositorio.crearPreciosEnCeroDeProductosActivos(precio.getPrecioId()));

        assertEquals(sucursalesActivas, sucursales);
        assertEquals(sucursalesActivas * productosActivos, precios);
        assertEquals(sucursalesActivas, precioRepositorio.contarSucursalesAsignadas(precio.getPrecioId()));
        assertEquals(0L, contarNativo("SELECT count(*) FROM prod_prec WHERE precio_id = " + precio.getPrecioId()
                + " AND (factor <> 1 OR utilid <> 0 OR precio_sin_impu <> 0 OR precio_con_impu <> 0)"));
    }

    private long contarNativo(String sql) {
        return ((Number) entityManager.createNativeQuery(sql).getSingleResult()).longValue();
    }

    private Precio crear(Precio precio) {
        ejecutarEnTransaccion(() -> {
            entityManager.persist(precio);
            return null;
        });
        idsCreados.add(precio.getPrecioId());
        return precio;
    }

    private Precio nuevoPrecio(boolean estado) {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Precio precio = new Precio();
        precio.setCodigo("PR-" + sufijo);
        precio.setDescri("Precio de prueba " + sufijo);
        precio.setAlcanc(Precio.ALCANCE_NINGUNO);
        precio.setFactor(BigDecimal.ONE);
        precio.setEstado(estado);
        return precio;
    }

    private <T> T ejecutarEnTransaccion(Supplier<T> accion) {
        try {
            userTransaction.begin();
            T resultado = accion.get();
            userTransaction.commit();
            return resultado;
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
