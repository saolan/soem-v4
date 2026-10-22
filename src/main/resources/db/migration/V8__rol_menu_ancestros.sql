/*==============================================================================
  V8 - ROL_MENU: CADENA DE ANCESTROS COMPLETA
  ------------------------------------------------------------------------------
  El menu de la sesion muestra un nodo solo si el rol tiene acceso a toda su
  cadena de grupos superiores. La semilla heredada tiene paginas asignadas
  sin alguno de sus grupos (p. ej. roles 5 y 6: 9810/9811 "Toma Fisica" sin
  9750 "Procesos"), que con esa regla quedarian ocultas.

  Por cada rol_menu con acceso se agregan, con acceso, los grupos superiores
  que falten. Las filas existentes no se tocan: un grupo asignado
  explicitamente con acceso = false sigue ocultando su subarbol.

  Antes se crea la unicidad (rol_id, menu_id): V1 solo la propone en su
  bloque comentado y V3 aplico las de rol_perm, rol_sucu y rol_pers_usua,
  no esta. La necesitan el ON CONFLICT de abajo y el reemplazo en bloque de
  RolServicio. Si hay pares repetidos la migracion se detiene: elegir cual
  conservar (pueden diferir en acceso) es una decision sobre los datos.
==============================================================================*/

DO $dup$
DECLARE
  repetidos INT;
BEGIN
  SELECT count(*) INTO repetidos
    FROM (SELECT 1 FROM rol_menu GROUP BY rol_id, menu_id HAVING count(*) > 1) d;
  IF repetidos > 0 THEN
    RAISE EXCEPTION 'rol_menu tiene % pares (rol_id, menu_id) repetidos; depurelos antes de aplicar V8', repetidos;
  END IF;
END $dup$;

-- Un mismo menu no puede asignarse dos veces al mismo rol.
ALTER TABLE rol_menu ADD CONSTRAINT uq_rol_menu_rol_menu UNIQUE (rol_id, menu_id);

WITH RECURSIVE ancestros (rol_id, menu_id) AS (
    SELECT rm.rol_id, m.menu_supe_id
      FROM rol_menu rm
      JOIN menu m ON m.menu_id = rm.menu_id
     WHERE rm.acceso = true
       AND m.menu_supe_id IS NOT NULL
    UNION
    SELECT a.rol_id, m.menu_supe_id
      FROM ancestros a
      JOIN menu m ON m.menu_id = a.menu_id
     WHERE m.menu_supe_id IS NOT NULL
)
INSERT INTO rol_menu (rol_id, menu_id, acceso)
SELECT a.rol_id, a.menu_id, true
  FROM ancestros a
ON CONFLICT (rol_id, menu_id) DO NOTHING;
