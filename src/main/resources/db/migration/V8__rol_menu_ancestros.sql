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
==============================================================================*/

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
