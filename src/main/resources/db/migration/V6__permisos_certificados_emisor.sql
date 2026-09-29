/*==============================================================================
  V6 - Permisos del CRUD de certificados emisores (CERT_EMIS)
  ------------------------------------------------------------------------------
  V5 sembro solo 'certificados-emisor:BUSCAR' (combo). El CRUD completo
  agrega alta, modificacion y baja logica.
==============================================================================*/

INSERT INTO permiso (descri, recurso, accion) VALUES
   ('Crear certificados emisores',     'certificados-emisor',  'CREAR'),
   ('Modificar certificados emisores', 'certificados-emisor',  'MODIFICAR'),
   ('Eliminar certificados emisores',  'certificados-emisor',  'ELIMINAR');
