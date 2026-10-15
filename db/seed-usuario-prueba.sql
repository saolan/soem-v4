/*==============================================================================
  Usuario semilla para los tests de integracion (*ControladorIT)
  ------------------------------------------------------------------------------
  Dato de prueba, NO es un cambio de esquema: no va en
  src/main/resources/db/migration (Flyway), se corre una unica vez a mano
  contra la base de pruebas, despues de aplicar las migraciones V1-V4.

  Crea:
    - Una sucursal de prueba en Ambato (ciudad que ningun test usa) con
      matriz=false, para no interferir con los tests de "buscarMatrizActual".
    - persona + pers_usua con usuari='admin.test' y clave = hash BCrypt de
      'Prueba$2026' (el valor por defecto ya usado en todos los *ControladorIT
      via -Dtest.sucursales.usuario/-Dtest.sucursales.clave). El hash se
      genero con la misma libreria que usa ClaveServicio (at.favre.lib:bcrypt,
      costo 12) y se verifico que efectivamente valida contra ese texto plano.
    - Un rol "Administrador de pruebas" con TODOS los permisos que existan
      hoy en la tabla permiso (incluye el catalogo sembrado por V2) y acceso a la
      sucursal de prueba creada arriba.

  Se puede correr una sola vez: 'admin.test' choca con la UNIQUE de V3 si se
  ejecuta dos veces. Para volver a sembrar, borrar antes las filas creadas
  (rol_perm/rol_sucu/rol_pers_usua del rol "Administrador de pruebas", el rol,
  pers_usua/persona de 'admin.test' y la sucursal 'ADMIN-TEST') o cambiar los
  valores identificatorios de este script.
==============================================================================*/

WITH nueva_sucursal AS (
    INSERT INTO sucursal (codigo, matriz, descri, ruc, razon_soci, nombre_come, ciudad, direcc, direcc_esta,
        telef1, telef2, correo, repres, repres_cedu, repres_dire, ruc_cont, contri_espe, obliga_cont, logoti, estado)
    VALUES ('ADMIN-TEST', false, 'Sucursal de Pruebas (admin.test)', '0999999999001', 'Soem Pruebas S.A.',
        NULL, 'Ambato', 'Av. de Pruebas 100', 'Av. de Pruebas 100', '0999999999', NULL,
        'pruebas@soem.local', 'Representante de Pruebas', '0999999999', 'Direccion Representante de Pruebas',
        '0999999999001', NULL, NULL, NULL, true)
    RETURNING sucursal_id
),
nueva_persona AS (
    INSERT INTO persona (cedula_ruc, apelli, nombre, direcc, telefo, correo, fecha_naci, estado)
    VALUES ('0999999999', 'Administrador', 'De Pruebas', NULL, NULL, 'admin.test@soem.local', NULL, true)
    RETURNING persona_id
),
nuevo_usuario AS (
    INSERT INTO pers_usua (persona_id, usuari, clave, caduca_clav, caduca_fech_hora, estado)
    SELECT persona_id, 'admin.test', '$2a$12$wauqLDtIXhlPcMdFX2AAeeCvZ0upSeKK7xxKgsYPTYgZ/C4IRw0vW', false, NULL, true
    FROM nueva_persona
    RETURNING persona_id
),
nuevo_rol AS (
    INSERT INTO rol (descri, estado)
    VALUES ('Administrador de pruebas', true)
    RETURNING rol_id
),
asignacion_rol AS (
    INSERT INTO rol_pers_usua (rol_id, persona_id)
    SELECT nuevo_rol.rol_id, nuevo_usuario.persona_id
    FROM nuevo_rol, nuevo_usuario
    RETURNING rol_id
),
asignacion_sucursal AS (
    INSERT INTO rol_sucu (sucursal_id, rol_id, acceso)
    SELECT nueva_sucursal.sucursal_id, nuevo_rol.rol_id, true
    FROM nueva_sucursal, nuevo_rol
    RETURNING rol_sucu_id
)
INSERT INTO rol_perm (rol_id, permiso_id, acceso)
SELECT nuevo_rol.rol_id, permiso.permiso_id, true
FROM nuevo_rol, permiso;

/*------------------------------------------------------------------------------
  Resincronizacion de permisos (idempotente)
  Correr este bloque solo, cada vez que una migracion nueva agregue permisos
  (V5 en adelante), para que el rol de pruebas tambien los tenga.
------------------------------------------------------------------------------*/
INSERT INTO rol_perm (rol_id, permiso_id, acceso)
SELECT rol.rol_id, permiso.permiso_id, true
FROM rol
CROSS JOIN permiso
WHERE rol.descri = 'Administrador de pruebas'
  AND NOT EXISTS (
      SELECT 1 FROM rol_perm existente
      WHERE existente.rol_id = rol.rol_id
        AND existente.permiso_id = permiso.permiso_id);
