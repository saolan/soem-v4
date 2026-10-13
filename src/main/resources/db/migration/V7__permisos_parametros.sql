/*==============================================================================
  V7 - Permisos del CRUD de parametros (PARAMETRO)
  ------------------------------------------------------------------------------
  Busqueda (incluye el combo), alta, modificacion y baja logica.
==============================================================================*/

INSERT INTO permiso (descri, recurso, accion) VALUES
   ('Buscar parametros',    'parametros',  'BUSCAR'),
   ('Crear parametros',     'parametros',  'CREAR'),
   ('Modificar parametros', 'parametros',  'MODIFICAR'),
   ('Eliminar parametros',  'parametros',  'ELIMINAR');
