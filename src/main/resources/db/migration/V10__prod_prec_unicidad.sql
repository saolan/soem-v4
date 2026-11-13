/*==============================================================================
  V10 - PROD_PREC: UN SOLO PRECIO POR SUCURSAL, LISTA Y PRODUCTO
  ------------------------------------------------------------------------------
  V1 no declara la unicidad de (SUCURSAL_ID, PRECIO_ID, PRODUCTO_ID): hoy solo
  la protege el NOT EXISTS de la generacion masiva de precios (alcance TODOS).
  Se declara en la base para que ningun otro flujo pueda duplicar un precio.

  Orden de columnas: sucursal -> lista -> producto, el mismo con que se busca
  el precio en ventas (sucursal del usuario + lista + producto). Con igualdad
  en las tres el orden no influye; por prefijo izquierdo tambien sirve
  "lista de una sucursal" y "todo lo de una sucursal", por eso se elimina
  IX_PROD_PREC_SUCURSAL_ID (redundante). Se conservan IX_PROD_PREC_PRECIO_ID
  (operaciones sobre una lista completa) e IX_PROD_PREC_PRODUCTO_ID.

  Si ya existen duplicados la migracion se detiene con un mensaje claro:
  decidir que fila conservar es una decision sobre los datos, no de la
  migracion. Para ubicarlos:

      SELECT sucursal_id, precio_id, producto_id, count(*)
        FROM prod_prec
       GROUP BY sucursal_id, precio_id, producto_id
      HAVING count(*) > 1;

  Flyway ejecuta la migracion dentro de una transaccion, asi que no es posible
  CREATE INDEX CONCURRENTLY: mientras se construye el indice la tabla queda
  bloqueada para escritura (segundos con cientos de miles de filas). Aplicar
  en una ventana sin operacion.
==============================================================================*/

DO $chk$
DECLARE
  duplicados BIGINT;
BEGIN
  SELECT count(*) INTO duplicados
    FROM (SELECT 1
            FROM prod_prec
           GROUP BY sucursal_id, precio_id, producto_id
          HAVING count(*) > 1) d;

  IF duplicados > 0 THEN
    RAISE EXCEPTION 'PROD_PREC tiene % combinaciones (sucursal_id, precio_id, producto_id) duplicadas; depurarlas antes de aplicar V10', duplicados;
  END IF;
END $chk$;

ALTER TABLE prod_prec
  ADD CONSTRAINT uq_prod_prec UNIQUE (sucursal_id, precio_id, producto_id);

DROP INDEX IF EXISTS ix_prod_prec_sucursal_id;

comment on constraint UQ_PROD_PREC on PROD_PREC is
'Un solo precio por sucursal, lista y producto. Su indice resuelve la busqueda del precio en ventas y, por prefijo, las consultas por sucursal.';
