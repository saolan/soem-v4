package com.saolan.soem.configuracion.repositorio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.configuracion.dto.PrecioFiltroDTO;
import com.saolan.soem.configuracion.modelo.Precio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Acceso a datos de {@link Precio} mediante Criteria API: busqueda
 * paginada/filtrada, combo de solo lectura, consultas auxiliares para las
 * reglas de {@code PrecioServicio} (unicidad de {@code codigo}/{@code descri}),
 * la generacion masiva de precios en cero del alcance "todos" y el acceso de
 * los roles a la lista ({@code ROL_PREC}).
 */
@ApplicationScoped
public class PrecioRepositorio {

    private static final Set<String> CAMPOS_ORDEN_PERMITIDOS =
            Set.of("precioId", "codigo", "descri", "alcanc", "factor", "estado");
    private static final String CAMPO_ORDEN_PREDETERMINADO = "descri";

    /*
     * Generacion masiva en SQL nativo (INSERT ... SELECT): la lista puede
     * aplicarse a decenas de miles de productos por sucursal, y cargarlos como
     * entidades costaria memoria y un INSERT por fila. Ademas PRODUCTO es del
     * modulo inventario y aun no tiene entidad. El unico dato variable va como
     * parametro posicional; usuari_regi/fecha_hora_regi los completan los
     * DEFAULT de la tabla (V6, usuario de la aplicacion).
     *
     * Idempotente (NOT EXISTS): un segundo llamado no duplica filas. El
     * anti-join usa ix_prod_prec_precio_id y, para una lista recien creada,
     * no encuentra filas.
     */
    private static final String SQL_CREAR_PRECIOS_EN_CERO = """
            INSERT INTO prod_prec (sucursal_id, precio_id, producto_id, factor, utilid,
                                   precio_sin_impu, precio_con_impu)
            SELECT s.sucursal_id, ?1, p.producto_id, 1, 0, 0, 0
              FROM producto p
             CROSS JOIN sucursal s
             WHERE p.estado = true
               AND s.estado = true
               AND NOT EXISTS (SELECT 1 FROM prod_prec pp
                                WHERE pp.sucursal_id = s.sucursal_id AND pp.precio_id = ?1
                                  AND pp.producto_id = p.producto_id)
            """;

    /*
     * ROL_PREC (acceso de cada rol a la lista por sucursal) aun no tiene
     * entidad: se actualiza en bloque por SQL nativo parametrizado. Solo toca
     * las filas que cambian.
     */
    private static final String SQL_ACTUALIZAR_ACCESO_EN_ROLES = """
            UPDATE rol_prec
               SET acceso = ?2
             WHERE precio_id = ?1
               AND acceso <> ?2
            """;

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<Precio> buscarPorId(Integer precioId) {
        return Optional.ofNullable(entityManager.find(Precio.class, precioId));
    }

    /** Con IDENTITY el INSERT es inmediato, asi que el id queda disponible al retornar. */
    public void crear(Precio precio) {
        entityManager.persist(precio);
    }

    /** Otra lista con el mismo codigo (sin distinguir mayusculas), excluyendo opcionalmente la que se modifica. */
    public Optional<Precio> buscarPorCodigo(String codigo, Integer idExcluido) {
        return buscarPorCampoSinMayusculas("codigo", codigo, idExcluido);
    }

    /** Otra lista con la misma descripcion (sin distinguir mayusculas), excluyendo opcionalmente la que se modifica. */
    public Optional<Precio> buscarPorDescri(String descri, Integer idExcluido) {
        return buscarPorCampoSinMayusculas("descri", descri, idExcluido);
    }

    /**
     * Marca {@code acceso} en todas las filas de {@code ROL_PREC} de la lista:
     * {@code false} al eliminarla (los roles pierden el acceso) y {@code true}
     * al reactivarla.
     *
     * @return cantidad de filas actualizadas
     */
    public int actualizarAccesoEnRoles(Integer precioId, boolean acceso) {
        return entityManager.createNativeQuery(SQL_ACTUALIZAR_ACCESO_EN_ROLES)
                .setParameter(1, precioId)
                .setParameter(2, acceso)
                .executeUpdate();
    }

    /**
     * Crea el precio en cero ({@code factor=1}, utilidad y precios en 0) de
     * cada producto activo en cada sucursal activa.
     *
     * @return cantidad de filas creadas en {@code PROD_PREC}
     */
    public int crearPreciosEnCeroDeProductosActivos(Integer precioId) {
        return entityManager.createNativeQuery(SQL_CREAR_PRECIOS_EN_CERO)
                .setParameter(1, precioId)
                .executeUpdate();
    }

    public List<Precio> buscarPaginado(PrecioFiltroDTO filtro, ParametrosBusqueda parametrosBusqueda) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Precio> query = criteriaBuilder.createQuery(Precio.class);
        Root<Precio> raiz = query.from(Precio.class);

        query.where(construirPredicados(criteriaBuilder, raiz, filtro).toArray(new Predicate[0]));

        String campoOrden = CAMPOS_ORDEN_PERMITIDOS.contains(parametrosBusqueda.ordenarPor())
                ? parametrosBusqueda.ordenarPor()
                : CAMPO_ORDEN_PREDETERMINADO;
        Path<?> rutaOrden = raiz.get(campoOrden);
        query.orderBy(parametrosBusqueda.esDescendente()
                ? criteriaBuilder.desc(rutaOrden)
                : criteriaBuilder.asc(rutaOrden));

        return entityManager.createQuery(query)
                .setFirstResult(parametrosBusqueda.primerRegistro())
                .setMaxResults(parametrosBusqueda.tamanio())
                .getResultList();
    }

    public long contar(PrecioFiltroDTO filtro) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<Precio> raiz = query.from(Precio.class);

        query.select(criteriaBuilder.count(raiz));
        query.where(construirPredicados(criteriaBuilder, raiz, filtro).toArray(new Predicate[0]));

        return entityManager.createQuery(query).getSingleResult();
    }

    public List<ComboDTO> buscarCombo() {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ComboDTO> query = criteriaBuilder.createQuery(ComboDTO.class);
        Root<Precio> raiz = query.from(Precio.class);

        query.select(criteriaBuilder.construct(ComboDTO.class, raiz.get("precioId"), raiz.get("descri")));
        query.where(criteriaBuilder.isTrue(raiz.get("estado")));
        query.orderBy(criteriaBuilder.asc(raiz.get("descri")));

        return entityManager.createQuery(query).getResultList();
    }

    private Optional<Precio> buscarPorCampoSinMayusculas(String campo, String valor, Integer idExcluido) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Precio> query = criteriaBuilder.createQuery(Precio.class);
        Root<Precio> raiz = query.from(Precio.class);

        List<Predicate> predicados = new ArrayList<>();
        predicados.add(criteriaBuilder.equal(criteriaBuilder.lower(raiz.get(campo)),
                valor.toLowerCase(Locale.ROOT)));
        if (idExcluido != null) {
            predicados.add(criteriaBuilder.notEqual(raiz.get("precioId"), idExcluido));
        }

        query.where(predicados.toArray(new Predicate[0]));
        return entityManager.createQuery(query).setMaxResults(1).getResultStream().findFirst();
    }

    private List<Predicate> construirPredicados(CriteriaBuilder criteriaBuilder, Root<Precio> raiz,
            PrecioFiltroDTO filtro) {
        List<Predicate> predicados = new ArrayList<>();
        if (filtro == null) {
            return predicados;
        }
        if (filtro.codigo() != null && !filtro.codigo().isBlank()) {
            predicados.add(criteriaBuilder.like(criteriaBuilder.lower(raiz.get("codigo")),
                    "%" + filtro.codigo().toLowerCase(Locale.ROOT) + "%"));
        }
        if (filtro.descri() != null && !filtro.descri().isBlank()) {
            predicados.add(criteriaBuilder.like(criteriaBuilder.lower(raiz.get("descri")),
                    "%" + filtro.descri().toLowerCase(Locale.ROOT) + "%"));
        }
        if (filtro.estado() != null) {
            predicados.add(criteriaBuilder.equal(raiz.get("estado"), filtro.estado()));
        }
        return predicados;
    }
}
