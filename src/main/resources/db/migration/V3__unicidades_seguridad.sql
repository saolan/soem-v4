/*==============================================================================
  V3 - Restricciones UNIQUE necesarias para el modulo de seguridad
  ------------------------------------------------------------------------------
  El esquema base solo propone estas restricciones en un bloque comentado
  ("PUNTO 2 - PARA FUTURAS ACTUALIZACIONES"), sin aplicarlas. El modulo de
  seguridad depende de ellas a nivel de base de datos (no solo de aplicacion)
  para garantizar login unico y evitar asignaciones rol-recurso duplicadas
  bajo condiciones de carrera.
==============================================================================*/
-- Borra registro repetido
delete from rol_perm where rol_perm_id = 295;

-- Login unico: PERS_USUA.USUARI es el nombre de usuario.
ALTER TABLE pers_usua ADD CONSTRAINT uq_pers_usua_usuari UNIQUE (usuari);

-- Un mismo permiso no puede asignarse dos veces al mismo rol.
ALTER TABLE rol_perm ADD CONSTRAINT uq_rol_perm_rol_permiso UNIQUE (rol_id, permiso_id);

-- Una misma sucursal no puede habilitarse dos veces para el mismo rol.
ALTER TABLE rol_sucu ADD CONSTRAINT uq_rol_sucu_rol_sucursal UNIQUE (rol_id, sucursal_id);

-- Un mismo rol no puede asignarse dos veces a la misma persona.
ALTER TABLE rol_pers_usua ADD CONSTRAINT uq_rol_pers_usua_rol_persona UNIQUE (rol_id, persona_id);
