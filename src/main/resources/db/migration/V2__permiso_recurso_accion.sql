/*==============================================================================
  V2 - PERMISO: modelo "recurso:accion"
  ------------------------------------------------------------------------------
  El esquema base modela PERMISO solo como DESCRI + VALOR (numerico), sin
  columna de recurso ni de accion. El filtro de autorizacion ya implementado
  (com.saolan.soem.compartido.seguridad.FiltroAutorizacion) exige que el JWT
  transporte permisos con el formato "recurso:accion" (todo en minusculas,
  ej. "sucursales:crear"). Se extiende PERMISO para que cada fila represente
  un permiso puntual de ese tipo, reutilizando ROL_PERM(rol_id, permiso_id,
  acceso) tal cual ya existe, sin agregar tablas nuevas.

  No hay datos productivos en PERMISO en este punto del esquema (base
  greenfield, sin INSERT en V1), por lo que las columnas nuevas pueden
  agregarse directamente como NOT NULL sin necesidad de backfill previo.
==============================================================================*/

ALTER TABLE permiso ADD COLUMN recurso VARCHAR(100);
ALTER TABLE permiso ADD COLUMN accion  VARCHAR(20);

-- VALOR deja de ser obligatorio: su semantica original (peso/costo numerico
-- del permiso) queda sin definir para el modelo "recurso:accion". No se
-- elimina la columna (decision pendiente de confirmacion, ver plan del
-- modulo seguridad); se documenta como nullable hasta que se resuelva.
ALTER TABLE permiso ALTER COLUMN valor DROP NOT NULL;
COMMENT ON COLUMN permiso.valor IS
  'Semantica original (peso/costo del permiso) sin uso definido para el modelo recurso:accion introducido en V2. Nullable a la espera de una decision futura.';

ALTER TABLE permiso ALTER COLUMN recurso SET NOT NULL;
ALTER TABLE permiso ALTER COLUMN accion  SET NOT NULL;

ALTER TABLE permiso ADD CONSTRAINT ck_permiso_accion
  CHECK (accion IN ('BUSCAR', 'CREAR', 'MODIFICAR', 'ELIMINAR', 'ANULAR', 'DESPROCESAR', 'CONVERTIR', 'IMPRIMIR'));

ALTER TABLE permiso ADD CONSTRAINT uq_permiso_recurso_accion UNIQUE (recurso, accion);

COMMENT ON COLUMN permiso.recurso IS 'Nombre del recurso protegido, en kebab-case (ej. sucursales), igual al usado en @RequierePermiso.';
COMMENT ON COLUMN permiso.accion  IS 'Accion de negocio (com.saolan.soem.compartido.seguridad.Accion), igual al usado en @RequierePermiso.';
