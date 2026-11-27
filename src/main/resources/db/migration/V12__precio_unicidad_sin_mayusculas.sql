/*==============================================================================
  V12 - PRECIO: CODIGO Y DESCRIPCION UNICOS SIN DISTINGUIR MAYUSCULAS
  ------------------------------------------------------------------------------
  PrecioServicio valida que CODIGO y DESCRI no se repitan sin distinguir
  mayusculas ("ABC" = "abc") y los guarda sin espacios en los extremos, pero
  V1 no declara ninguna unicidad en PRECIO: dos altas simultaneas, o un INSERT
  directo, podrian duplicar una lista. Estos indices unicos de expresion
  garantizan la regla tambien en la base, igual que V11 en SUCURSAL.

  Ademas son los indices que usa la validacion: PrecioRepositorio busca con
  lower(codigo) = ? y lower(descri) = ?.

  Si ya existen duplicados la migracion se detiene con un mensaje claro:
  decidir que lista renombrar es una decision sobre los datos, no de la
  migracion. Para ubicarlos:

      SELECT lower(codigo), count(*) FROM precio GROUP BY 1 HAVING count(*) > 1;
      SELECT lower(descri), count(*) FROM precio GROUP BY 1 HAVING count(*) > 1;

  Flyway ejecuta la migracion dentro de una transaccion, asi que no es posible
  CREATE INDEX CONCURRENTLY; PRECIO tiene pocas filas y el bloqueo dura
  milisegundos.
==============================================================================*/

DO $chk$
DECLARE
  codigos_duplicados BIGINT;
  descripciones_duplicadas BIGINT;
BEGIN
  SELECT count(*) INTO codigos_duplicados
    FROM (SELECT 1 FROM precio GROUP BY lower(codigo) HAVING count(*) > 1) d;

  SELECT count(*) INTO descripciones_duplicadas
    FROM (SELECT 1 FROM precio GROUP BY lower(descri) HAVING count(*) > 1) d;

  IF codigos_duplicados > 0 OR descripciones_duplicadas > 0 THEN
    RAISE EXCEPTION 'PRECIO tiene % codigos y % descripciones repetidos sin distinguir mayusculas; depurarlos antes de aplicar V12',
      codigos_duplicados, descripciones_duplicadas;
  END IF;
END $chk$;

CREATE UNIQUE INDEX uq_precio_codigo_sin_mayusculas ON precio (lower(codigo));

CREATE UNIQUE INDEX uq_precio_descri_sin_mayusculas ON precio (lower(descri));

comment on index UQ_PRECIO_CODIGO_SIN_MAYUSCULAS is
'Codigo de lista de precio unico sin distinguir mayusculas. Resuelve la validacion lower(codigo) = ? de PrecioRepositorio.';

comment on index UQ_PRECIO_DESCRI_SIN_MAYUSCULAS is
'Descripcion de lista de precio unica sin distinguir mayusculas. Resuelve la validacion lower(descri) = ? de PrecioRepositorio.';
