package com.saolan.soem.configuracion.repositorio;

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
import com.saolan.soem.configuracion.modelo.CertEmis;

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

    @AfterEach
    void limpiar() {
        ejecutarEnTransaccion(() -> {
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

    private CertEmis crear(CertEmis certEmis) {
        ejecutarEnTransaccion(() -> entityManager.persist(certEmis));
        idsCreados.add(certEmis.getCertEmisId());
        return certEmis;
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
