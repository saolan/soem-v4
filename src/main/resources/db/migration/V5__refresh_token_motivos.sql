/*==============================================================================
  V5 - REFRESH_TOKEN: nuevos motivos de revocacion
  ------------------------------------------------------------------------------
  - cambio_clave    : al cambiar la clave se revocan todas las sesiones vigentes
                      de la persona (la sesion actual se rota a un token nuevo).
  - cambio_sucursal : POST /auth/cambiar-sucursal rota el refresh token actual
                      para fijar la nueva sucursal activa de la sesion.

  Se recrea el CHECK con el prefijo estandar chk_ (AGENTS.md).
==============================================================================*/

ALTER TABLE refresh_token DROP CONSTRAINT ck_refresh_token_motivo;

ALTER TABLE refresh_token
   ADD CONSTRAINT chk_refresh_token_motivo CHECK (motivo_revocacion is null
      or motivo_revocacion in ('rotacion', 'logout', 'reuso_detectado', 'cambio_clave', 'cambio_sucursal'));
