package com.saolan.soem.configuracion.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import com.saolan.soem.configuracion.dto.CertEmisFiltroDTO;
import com.saolan.soem.configuracion.modelo.CertEmis;
import com.saolan.soem.configuracion.modelo.SucuCertEmis;
import com.saolan.soem.configuracion.modelo.Sucursal;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

/**
 * Test de integración de {@link CertEmisRepositorio}. Misma precondición y
 * patrón transaccional que {@code SucursalRepositorioIT}.
 */
@ExtendWith(ArquillianExtension.class)
class CertEmisRepositorioIT {

    @Deployment
    public static Archive<?> crearDespliegue() {
        File archivo = new File("./target/soem.war");
        if (!archivo.exists()) {
            throw new RuntimeException("File " + archivo.getAbsolutePath() + " does not exist.");
        }
        WebArchive war = ShrinkWrap.create(ZipImporter.class, "soem.war").importFrom(archivo).as(WebArchive.class);
        war.addPackage(CertEmisRepositorioIT.class.getPackage());
        return war;
    }

    @Inject
    private CertEmisRepositorio certEmisRepositorio;

    @PersistenceContext(unitName = "soemPU")
    private EntityManager entityManager;

    @Resource
    private UserTransaction userTransaction;

    private final List<Integer> idsCreados = new ArrayList<>();
    private final List<Integer> idsSucursalesCreadas = new ArrayList<>();

    @AfterEach
    void limpiar() {
        ejecutarEnTransaccion(() -> {
            // Primero las sucursales: su orphanRemoval borra SUCU_CERT_EMIS, que referencia a CERT_EMIS.
            for (Integer id : idsSucursalesCreadas) {
                Sucursal sucursal = entityManager.find(Sucursal.class, id);
                if (sucursal != null) {
                    entityManager.remove(sucursal);
                }
            }
            entityManager.flush();
            for (Integer id : idsCreados) {
                CertEmis certEmis = entityManager.find(CertEmis.class, id);
                if (certEmis != null) {
                    entityManager.remove(certEmis);
                }
            }
        });
    }

    @Test
    void buscarPorId_deberiaEncontrarElCertificadoCreado() {
        CertEmis certEmis = crear(nuevoCertEmis(true));

        Optional<CertEmis> encontrado = certEmisRepositorio.buscarPorId(certEmis.getCertEmisId());

        assertTrue(encontrado.isPresent());
    }

    @Test
    void buscarCombo_deberiaIncluirSoloCertificadosActivos() {
        CertEmis activo = crear(nuevoCertEmis(true));
        CertEmis inactivo = crear(nuevoCertEmis(false));

        List<ComboDTO> combo = certEmisRepositorio.buscarCombo();

        assertTrue(combo.stream().anyMatch(c -> c.valor().equals(activo.getCertEmisId())));
        assertTrue(combo.stream().noneMatch(c -> c.valor().equals(inactivo.getCertEmisId())));
    }

    @Test
    void buscarPorDescri_deberiaIgnorarMayusculas_yRespetarExclusion() {
        CertEmis certEmis = crear(nuevoCertEmis(true));
        String descriEnMayusculas = certEmis.getDescri().toUpperCase();

        assertTrue(certEmisRepositorio.buscarPorDescri(descriEnMayusculas, null).isPresent());
        assertFalse(certEmisRepositorio.buscarPorDescri(descriEnMayusculas, certEmis.getCertEmisId()).isPresent());
    }

    @Test
    void buscarPaginado_deberiaFiltrarPorDescri_yContarCoherentemente() {
        CertEmis certEmis = crear(nuevoCertEmis(true));
        CertEmisFiltroDTO filtro = new CertEmisFiltroDTO(certEmis.getDescri(), null, null, true);

        List<CertEmis> resultado = certEmisRepositorio.buscarPaginado(filtro, new ParametrosBusqueda(1, 20, "descri", "asc"));

        assertEquals(1, resultado.size());
        assertEquals(certEmis.getCertEmisId(), resultado.get(0).getCertEmisId());
        assertEquals(1L, certEmisRepositorio.contar(filtro));
    }

    @Test
    void contarSucursalesAsignadas_deberiaContarSoloLasQueLoUsan() {
        CertEmis enUso = crear(nuevoCertEmis(true));
        CertEmis libre = crear(nuevoCertEmis(true));
        crearSucursalConCertificado(enUso);

        assertEquals(1L, certEmisRepositorio.contarSucursalesAsignadas(enUso.getCertEmisId()));
        assertEquals(0L, certEmisRepositorio.contarSucursalesAsignadas(libre.getCertEmisId()));
    }

    private CertEmis crear(CertEmis certEmis) {
        ejecutarEnTransaccion(() -> entityManager.persist(certEmis));
        idsCreados.add(certEmis.getCertEmisId());
        return certEmis;
    }

    private void crearSucursalConCertificado(CertEmis certEmis) {
        String ruc = String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000_000L, 9_999_999_999_999L));
        Sucursal sucursal = new Sucursal();
        sucursal.setCodigo("CE-" + UUID.randomUUID().toString().substring(0, 8));
        sucursal.setRuc(ruc);
        sucursal.setMatriz(Boolean.FALSE);
        sucursal.setEstado(Boolean.TRUE);
        sucursal.setDescri("Sucursal de prueba certificado emisor");
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

        SucuCertEmis certificado = new SucuCertEmis();
        certificado.setSucursal(sucursal);
        certificado.setCertEmis(certEmis);
        certificado.setRuta("/certificados/prueba.p12");
        certificado.setDescri("Certificado de prueba");
        certificado.setClave("clave-prueba");
        sucursal.setCertificadoEmisor(certificado);

        ejecutarEnTransaccion(() -> entityManager.persist(sucursal));
        idsSucursalesCreadas.add(sucursal.getSucursalId());
    }

    private CertEmis nuevoCertEmis(boolean estado) {
        CertEmis certEmis = new CertEmis();
        certEmis.setDescri("Certificado de prueba " + UUID.randomUUID().toString().substring(0, 8));
        certEmis.setCertCn("CN Test");
        certEmis.setCertOu("OU Test");
        certEmis.setCertO("O Test");
        certEmis.setCertC("EC");
        certEmis.setCertOid("1.2.3.4");
        certEmis.setEstado(estado);
        return certEmis;
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
