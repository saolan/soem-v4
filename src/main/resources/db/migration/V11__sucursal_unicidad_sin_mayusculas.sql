/*==============================================================================
  V11 - SUCURSAL: CODIGO Y DESCRIPCION UNICOS SIN DISTINGUIR MAYUSCULAS
  ------------------------------------------------------------------------------
  SucursalServicio valida que CODIGO y DESCRI no se repitan sin distinguir
  mayusculas ("ABC" = "abc") y los guarda sin espacios en los extremos. Esa
  validacion es de aplicacion: dos altas simultaneas podrian pasarla a la vez.
  Estos indices unicos de expresion la garantizan tambien en la base.

  Ademas son los indices que usa la validacion: SucursalRepositorio busca con
  lower(codigo) = ? y lower(descri) = ?, que un indice sobre la columna sin
  lower() no puede resolver.

  UQ_SUCURSAL_CODIGO (V1, UNIQUE (codigo) con mayusculas) se elimina: el nuevo
  indice es mas estricto, asi que la restriccion queda redundante y solo
  costaria mantener un segundo indice en cada escritura. UQ_SUCURSAL_RUC no se
  toca: el RUC son solo digitos y se compara exacto.

  Si ya existen duplicados la migracion se detiene con un mensaje claro:
  decidir que sucursal renombrar es una decision sobre los datos, no de la
  migracion. Para ubicarlos:

      SELECT lower(codigo), count(*) FROM sucursal GROUP BY 1 HAVING count(*) > 1;
      SELECT lower(descri), count(*) FROM sucursal GROUP BY 1 HAVING count(*) > 1;

  Flyway ejecuta la migracion dentro de una transaccion, asi que no es posible
  CREATE INDEX CONCURRENTLY; SUCURSAL tiene pocas filas y el bloqueo dura
  milisegundos.
==============================================================================*/

DO $chk$
DECLARE
  codigos_duplicados BIGINT;
  descripciones_duplicadas BIGINT;
BEGIN
  SELECT count(*) INTO codigos_duplicados
    FROM (SELECT 1 FROM sucursal GROUP BY lower(codigo) HAVING count(*) > 1) d;

  SELECT count(*) INTO descripciones_duplicadas
    FROM (SELECT 1 FROM sucursal GROUP BY lower(descri) HAVING count(*) > 1) d;

  IF codigos_duplicados > 0 OR descripciones_duplicadas > 0 THEN
    RAISE EXCEPTION 'SUCURSAL tiene % codigos y % descripciones repetidos sin distinguir mayusculas; depurarlos antes de aplicar V11',
      codigos_duplicados, descripciones_duplicadas;
  END IF;
END $chk$;

CREATE UNIQUE INDEX uq_sucursal_codigo_sin_mayusculas ON sucursal (lower(codigo));

CREATE UNIQUE INDEX uq_sucursal_descri_sin_mayusculas ON sucursal (lower(descri));

ALTER TABLE sucursal DROP CONSTRAINT uq_sucursal_codigo;

comment on index UQ_SUCURSAL_CODIGO_SIN_MAYUSCULAS is
'Codigo de sucursal unico sin distinguir mayusculas. Resuelve la validacion lower(codigo) = ? de SucursalRepositorio.';

comment on index UQ_SUCURSAL_DESCRI_SIN_MAYUSCULAS is
'Descripcion de sucursal unica sin distinguir mayusculas. Resuelve la validacion lower(descri) = ? de SucursalRepositorio.';
