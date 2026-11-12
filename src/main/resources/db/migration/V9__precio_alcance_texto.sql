/*==============================================================================
  V9 - PRECIO.ALCANC: DE ENTERO A TEXTO (TODOS / NINGUNO)
  ------------------------------------------------------------------------------
  ALCANC decide, solo al crear una lista de precio, si se generan los precios
  en cero de todos los productos activos (TODOS) o ninguno (NINGUNO). V1 lo
  guarda como INT4 (1 / 0); se pasa a texto en mayusculas con CHECK, igual que
  los demas dominios cerrados del esquema (PERMISO.ACCION, MOTIVO_REVOCACION),
  para que el dato se lea sin conocer el codigo y la base rechace valores
  fuera del dominio.

  El CASE no tiene ELSE a proposito: un codigo distinto de 0/1 queda en NULL,
  viola el NOT NULL de la columna y la migracion se detiene. Decidir que
  alcance le corresponde es una decision sobre los datos, no de la migracion.
==============================================================================*/

ALTER TABLE precio
  ALTER COLUMN alcanc TYPE VARCHAR(12)
  USING CASE alcanc WHEN 0 THEN 'NINGUNO' WHEN 1 THEN 'TODOS' END;

ALTER TABLE precio
  ADD CONSTRAINT chk_precio_alcanc CHECK (alcanc IN ('TODOS', 'NINGUNO'));

comment on column PRECIO.ALCANC is
'TODOS: al crear la lista se generan los precios en cero de los productos activos en las sucursales activas. NINGUNO: no se genera nada. Inmutable.';
