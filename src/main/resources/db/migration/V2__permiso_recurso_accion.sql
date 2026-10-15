/*==============================================================================
  V2 - PERMISO: modelo "recurso:accion"
  ------------------------------------------------------------------------------
  El esquema base modela PERMISO solo como DESCRI + VALOR (numerico), sin
  columna de recurso ni de accion. El filtro de autorizacion
  (com.saolan.soem.compartido.seguridad.FiltroAutorizacion) exige que el JWT
  transporte permisos con el formato "recurso:accion" (todo en minusculas,
  ej. "configuracion-crud:crear"). Se extiende PERMISO para que cada fila represente
  un permiso puntual de ese tipo, reutilizando ROL_PERM(rol_id, permiso_id,
  acceso) tal cual ya existe, sin agregar tablas nuevas.

  Solo adapta la estructura; no inserta permisos ni los asigna a roles.
  Es idempotente y debe funcionar en dos escenarios:
    - Base nueva (greenfield): V1 ya crea PERMISO con este modelo; esta
      migracion no altera nada.
    - Base productiva 3.0 (baseline en V1): PERMISO ya contiene los permisos
      legados (IDs 1000-8999, ver db/importFinal-3.0.sql). Se completan
      RECURSO y ACCION de cada uno antes del NOT NULL, conservando su
      PERMISO_ID para que las asignaciones existentes en ROL_PERM sigan
      siendo validas.
==============================================================================*/

ALTER TABLE permiso ADD COLUMN IF NOT EXISTS recurso VARCHAR(100);
ALTER TABLE permiso ADD COLUMN IF NOT EXISTS accion  VARCHAR(20);

-- VALOR deja de ser obligatorio: su semantica original (peso/costo numerico
-- del permiso) queda sin definir para el modelo "recurso:accion". No se
-- elimina la columna para no perder el dato de las bases productivas.
ALTER TABLE permiso ALTER COLUMN valor DROP NOT NULL;

/*------------------------------------------------------------------------------
  1. Permisos legados (solo afecta a bases productivas 3.0)
  ------------------------------------------------------------------------------
  Equivalencia por PERMISO_ID entre el permiso legado (por modulo) y el par
  recurso:accion. Convencion de nombres del recurso:
    - <modulo>-crud               (X000-X003): paginas que solo hacen CRUD
                                   sobre tablas independientes (ej. caja).
    - <modulo>-movimientos        (X100...): paginas de agregados
                                   cabecera/detalle (ej. caja_movi).
    - <modulo>-movimientos-<sufijo>: variantes de la pagina de movimientos
                                   que requieren recurso propio por la
                                   unicidad (recurso, accion).
  Los permisos de acciones especificas (3150-3210, 4200-4383) conservan su
  nombre. Parametro (6000-6003) pasa a configuracion-crud y Seguridad
  (7000-7003) a seguridad-crud, los recursos que usan los controladores de
  esos modulos en @RequierePermiso.
------------------------------------------------------------------------------*/
UPDATE permiso p
   SET recurso = m.recurso,
       accion  = m.accion
  FROM (VALUES
   (1000, 'caja-crud',                                 'CREAR'),
   (1001, 'caja-crud',                                 'MODIFICAR'),
   (1002, 'caja-crud',                                 'ELIMINAR'),
   (1003, 'caja-crud',                                 'BUSCAR'),
   (1100, 'caja-movimientos',                          'CREAR'),
   (1101, 'caja-movimientos',                          'MODIFICAR'),
   (1102, 'caja-movimientos',                          'ELIMINAR'),
   (1103, 'caja-movimientos',                          'BUSCAR'),
   (1104, 'caja-movimientos',                          'GESTIONAR'),
   (1106, 'caja-movimientos',                          'DESCARGAR'),
   (1110, 'caja-movimientos-otros-usuarios',           'BUSCAR'),
   (2000, 'contabilidad-crud',                         'CREAR'),
   (2001, 'contabilidad-crud',                         'MODIFICAR'),
   (2002, 'contabilidad-crud',                         'ELIMINAR'),
   (2003, 'contabilidad-crud',                         'BUSCAR'),
   (2100, 'contabilidad-movimientos',                  'CREAR'),
   (2101, 'contabilidad-movimientos',                  'MODIFICAR'),
   (2102, 'contabilidad-movimientos',                  'ELIMINAR'),
   (2103, 'contabilidad-movimientos',                  'BUSCAR'),
   (2107, 'contabilidad-movimientos',                  'ANULAR'),
   (3000, 'egreso-crud',                               'CREAR'),
   (3001, 'egreso-crud',                               'MODIFICAR'),
   (3002, 'egreso-crud',                               'ELIMINAR'),
   (3003, 'egreso-crud',                               'BUSCAR'),
   (3100, 'egreso-movimientos',                        'CREAR'),
   (3101, 'egreso-movimientos',                        'MODIFICAR'),
   (3102, 'egreso-movimientos',                        'ELIMINAR'),
   (3103, 'egreso-movimientos',                        'BUSCAR'),
   (3104, 'egreso-movimientos',                        'PROCESAR'),
   (3105, 'egreso-movimientos',                        'PAGAR'),
   (3106, 'egreso-movimientos',                        'DESCARGAR'),
   (3107, 'egreso-movimientos',                        'ANULAR'),
   (3108, 'egreso-movimientos',                        'DESPROCESAR'),
   (3109, 'egreso-movimientos',                        'IMPRIMIR'),
   (3110, 'egreso-movimientos-documentos',             'GESTIONAR'),
   (3111, 'egreso-movimientos-precio',                 'MODIFICAR'),
   (3112, 'egreso-movimientos',                        'CONVERTIR'),
   (3150, 'egresos-cliente-cedula',                    'BLOQUEAR'),
   (3151, 'egresos-cliente-cupo',                      'BLOQUEAR'),
   (3152, 'egresos-cliente-descuento-maximo',          'BLOQUEAR'),
   (3180, 'egresos-descuento-cliente',                 'BLOQUEAR'),
   (3200, 'egresos-credito-cliente',                   'MOSTRAR'),
   (3210, 'egresos-clientes-por-vendedor',             'FILTRAR'),
   (4000, 'ingreso-crud',                              'CREAR'),
   (4001, 'ingreso-crud',                              'MODIFICAR'),
   (4002, 'ingreso-crud',                              'ELIMINAR'),
   (4003, 'ingreso-crud',                              'BUSCAR'),
   (4100, 'ingreso-movimientos',                       'CREAR'),
   (4101, 'ingreso-movimientos',                       'MODIFICAR'),
   (4102, 'ingreso-movimientos',                       'ELIMINAR'),
   (4103, 'ingreso-movimientos',                       'BUSCAR'),
   (4104, 'ingreso-movimientos',                       'PROCESAR'),
   (4105, 'ingreso-movimientos',                       'PAGAR'),
   (4106, 'ingreso-movimientos',                       'DESCARGAR'),
   (4107, 'ingreso-movimientos',                       'ANULAR'),
   (4108, 'ingreso-movimientos',                       'DESPROCESAR'),
   (4112, 'ingreso-movimientos',                       'CONVERTIR'),
   (4113, 'ingreso-movimientos-compra',                'CARGAR'),
   (4200, 'ingresos-factor-1-costo',                   'MODIFICAR'),
   (4201, 'ingresos-factor-1-costo-ultimo',            'ACTUALIZAR'),
   (4202, 'ingresos-factor-1-precios',                 'ACTUALIZAR'),
   (4203, 'ingresos-factor-1-precio',                  'MODIFICAR'),
   (4204, 'ingresos-factor-1-utilidad',                'MODIFICAR'),
   (4250, 'ingresos-factor-0-costo',                   'MODIFICAR'),
   (4251, 'ingresos-factor-0-costo-ultimo',            'ACTUALIZAR'),
   (4252, 'ingresos-factor-0-precios',                 'ACTUALIZAR'),
   (4253, 'ingresos-factor-0-precio',                  'MODIFICAR'),
   (4254, 'ingresos-factor-0-utilidad',                'MODIFICAR'),
   (4300, 'ingresos-factor-menos-1-costo',             'MODIFICAR'),
   (4301, 'ingresos-factor-menos-1-costo-ultimo',      'ACTUALIZAR'),
   (4302, 'ingresos-factor-menos-1-precios',           'ACTUALIZAR'),
   (4303, 'ingresos-factor-menos-1-precio',            'MODIFICAR'),
   (4304, 'ingresos-factor-menos-1-utilidad',          'MODIFICAR'),
   (4350, 'ingresos-fecha-registro',                   'BLOQUEAR'),
   (4360, 'ingresos-total-precio-cabecera',            'MOSTRAR'),
   (4380, 'ingresos-precio',                           'MOSTRAR'),
   (4381, 'ingresos-descuento-porcentaje-detalle',     'MOSTRAR'),
   (4382, 'ingresos-descuento-valor-detalle',          'MOSTRAR'),
   (4383, 'ingresos-total-precio-detalle',             'MOSTRAR'),
   (5000, 'inventario-crud',                           'CREAR'),
   (5001, 'inventario-crud',                           'MODIFICAR'),
   (5002, 'inventario-crud',                           'ELIMINAR'),
   (5003, 'inventario-crud',                           'BUSCAR'),
   (5050, 'inventario-movimientos',                    'ELIMINAR'),
   (5051, 'inventario-movimientos',                    'PROCESAR'),
   (5052, 'inventario-movimientos',                    'DESCARGAR'),
   (5053, 'inventario-movimientos-productos-sin-toma', 'CREAR'),
   (5060, 'inventario-movimientos-costo-promedio',     'MOSTRAR'),
   (5061, 'inventario-movimientos-costo-conteo',       'MOSTRAR'),
   (5062, 'inventario-movimientos-cantidad-actual',    'MOSTRAR'),
   (6000, 'configuracion-crud',                        'CREAR'),
   (6001, 'configuracion-crud',                        'MODIFICAR'),
   (6002, 'configuracion-crud',                        'ELIMINAR'),
   (6003, 'configuracion-crud',                        'BUSCAR'),
   (7000, 'seguridad-crud',                            'CREAR'),
   (7001, 'seguridad-crud',                            'MODIFICAR'),
   (7002, 'seguridad-crud',                            'ELIMINAR'),
   (7003, 'seguridad-crud',                            'BUSCAR'),
   (8000, 'tesoreria-crud',                            'CREAR'),
   (8001, 'tesoreria-crud',                            'MODIFICAR'),
   (8002, 'tesoreria-crud',                            'ELIMINAR'),
   (8003, 'tesoreria-crud',                            'BUSCAR'),
   (8100, 'tesoreria-movimientos',                     'CREAR'),
   (8101, 'tesoreria-movimientos',                     'MODIFICAR'),
   (8102, 'tesoreria-movimientos',                     'ELIMINAR'),
   (8103, 'tesoreria-movimientos',                     'BUSCAR'),
   (8104, 'tesoreria-movimientos',                     'PROCESAR'),
   (8107, 'tesoreria-movimientos',                     'ANULAR')
       ) AS m (permiso_id, recurso, accion)
 WHERE p.permiso_id = m.permiso_id;

-- Permisos propios de una instalacion que no figuran en la equivalencia: se
-- conservan como recurso "legado-<id>" (unico por fila) para no romper sus
-- asignaciones en ROL_PERM; se reclasifican luego desde el modulo seguridad.
UPDATE permiso
   SET recurso = 'legado-' || permiso_id,
       accion  = 'BUSCAR'
 WHERE recurso IS NULL;

/*------------------------------------------------------------------------------
  2. Restricciones
------------------------------------------------------------------------------*/
ALTER TABLE permiso ALTER COLUMN recurso SET NOT NULL;
ALTER TABLE permiso ALTER COLUMN accion  SET NOT NULL;

-- Acciones en sincronia con el enum com.saolan.soem.compartido.seguridad.Accion.
-- Las nueve ultimas cubren operaciones heredadas del sistema anterior.
ALTER TABLE permiso DROP CONSTRAINT IF EXISTS ck_permiso_accion;
ALTER TABLE permiso ADD CONSTRAINT ck_permiso_accion
  CHECK (accion IN ('BUSCAR', 'CREAR', 'MODIFICAR', 'ELIMINAR', 'ANULAR', 'DESPROCESAR', 'CONVERTIR', 'IMPRIMIR',
                    'PROCESAR', 'PAGAR', 'DESCARGAR', 'GESTIONAR', 'BLOQUEAR', 'MOSTRAR', 'FILTRAR', 'ACTUALIZAR', 'CARGAR'));

ALTER TABLE permiso DROP CONSTRAINT IF EXISTS uq_permiso_recurso_accion;
ALTER TABLE permiso ADD CONSTRAINT uq_permiso_recurso_accion UNIQUE (recurso, accion);

COMMENT ON COLUMN permiso.valor IS
  'Semantica original (peso/costo del permiso) sin uso definido para el modelo recurso:accion introducido en V2. Nullable a la espera de una decision futura.';
COMMENT ON COLUMN permiso.recurso IS 'Nombre del recurso protegido, en kebab-case (ej. configuracion-crud), igual al usado en @RequierePermiso.';
COMMENT ON COLUMN permiso.accion  IS 'Accion de negocio (com.saolan.soem.compartido.seguridad.Accion), igual al usado en @RequierePermiso.';
