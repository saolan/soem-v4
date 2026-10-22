/*==============================================================================
  V6 - AUDITORIA CON EL USUARIO DE LA APLICACION
  ------------------------------------------------------------------------------
  WildFly se conecta con un unico usuario de pool, por lo que current_user no
  identifica a la persona que opera. La aplicacion publica el usuario
  autenticado al inicio de cada transaccion con
      SELECT set_config('soem.usuario', '<usuario>', true)
  (alcance de transaccion: no se filtra a otra solicitud que reutilice la
  conexion del pool). Ver com.saolan.soem.compartido.seguridad.InterceptorUsuarioAuditoria.

  - soem_usuario_actual(): ese valor, o current_user si no hay usuario de
    aplicacion (scripts manuales, tareas programadas, login).
  - DEFAULT de usuari_regi en todas las tablas que la tienen.
  - trg_marca_modificacion() usa la misma funcion para usuari_modi.
==============================================================================*/

CREATE OR REPLACE FUNCTION soem_usuario_actual() RETURNS VARCHAR AS $fn$
  SELECT coalesce(nullif(current_setting('soem.usuario', true), ''), current_user)::VARCHAR;
$fn$ LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION trg_marca_modificacion() RETURNS trigger AS $tg$
BEGIN
  NEW.usuari_modi     := soem_usuario_actual();
  NEW.fecha_hora_modi := now();
  NEW.usuari_regi     := OLD.usuari_regi;       -- inmutables
  NEW.fecha_hora_regi := OLD.fecha_hora_regi;
  RETURN NEW;
END; $tg$ LANGUAGE plpgsql;

DO $def$
DECLARE r record; n int := 0;
BEGIN
  FOR r IN
    SELECT table_name AS tabla
    FROM information_schema.columns
    WHERE table_schema = current_schema()
      AND column_name = 'usuari_regi'
  LOOP
    EXECUTE format('ALTER TABLE %I ALTER COLUMN usuari_regi SET DEFAULT soem_usuario_actual()', r.tabla);
    n := n + 1;
  END LOOP;
  RAISE NOTICE 'DEFAULT de usuari_regi actualizado en % tablas', n;
END $def$;
