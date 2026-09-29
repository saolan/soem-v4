/*==============================================================================
  V5 - Catalogo inicial de permisos (recurso:accion)
  ------------------------------------------------------------------------------
  Siembra los permisos correspondientes a los endpoints ya protegidos con
  @RequierePermiso en el codigo actual. Cada vez que se agregue un endpoint
  protegido nuevo (en cualquier modulo), debe sumarse una migracion Flyway
  nueva con el INSERT correspondiente (decision de diseno del modulo
  seguridad: el catalogo se mantiene por migracion SQL versionada, no por
  escaneo en runtime).
==============================================================================*/

INSERT INTO permiso (descri, recurso, accion) VALUES
   ('Buscar sucursales',             'sucursales',           'BUSCAR'),
   ('Crear sucursales',              'sucursales',           'CREAR'),
   ('Modificar sucursales',          'sucursales',           'MODIFICAR'),
   ('Eliminar sucursales',           'sucursales',           'ELIMINAR'),
   ('Buscar certificados emisores',  'certificados-emisor',  'BUSCAR'),
   ('Buscar usuarios',               'usuarios',             'BUSCAR'),
   ('Crear usuarios',                'usuarios',             'CREAR'),
   ('Modificar usuarios',            'usuarios',             'MODIFICAR'),
   ('Eliminar usuarios',             'usuarios',             'ELIMINAR'),
   ('Buscar roles',                  'roles',                'BUSCAR'),
   ('Crear roles',                   'roles',                'CREAR'),
   ('Modificar roles',               'roles',                'MODIFICAR'),
   ('Eliminar roles',                'roles',                'ELIMINAR'),
   ('Buscar permisos',               'permisos',             'BUSCAR'),
   ('Crear permisos',                'permisos',             'CREAR'),
   ('Modificar permisos',            'permisos',             'MODIFICAR');
