/*==============================================================================
  V7 - MENU: RUTAS DEL FRONTEND (SPA) Y ELIMINACION DE MENU_NIVE
  ------------------------------------------------------------------------------
  - pagina_ruta deja de apuntar a paginas JSF heredadas (/ppsj/...xhtml,
    /jasperReportes/...xhtml) y pasa a rutas del frontend React con la forma
    /<modulo>/<recurso-plural-kebab> (reportes: /reportes/<area>/<reporte>).
    Los grupos (detall = false) no tienen ruta: el texto 'null' de la semilla
    heredada pasa a NULL real.
  - MENU_NIVE es redundante: el nivel se deriva de la jerarquia menu_supe_id
    y el tipo de nodo (grupo/pagina) lo indica detall. Se elimina junto con
    la pagina de menu 1100 que la administraba.
  - 9810 "Toma Fisica" estaba declarado nivel 2 pero colgaba de otro nivel 2
    (9775); se reubica bajo su grupo de nivel 1 (9750), coherente con el
    nivel que declaraba y con la profundidad maxima de 3 niveles.
  - Restricciones: ruta obligatoria solo en paginas (chk_menu_pagina_ruta) y
    unica (uq_menu_pagina_ruta), porque el frontend la usa como clave de
    navegacion.

  Las sentencias sobre datos no fallan en una base vacia: en una instalacion
  nueva la semilla (db/insert-inicial.sql) ya trae las rutas nuevas.
==============================================================================*/

-- 1. Pagina de administracion de MENU_NIVE (deja de existir)
DELETE FROM rol_menu WHERE menu_id = 1100;
DELETE FROM menu     WHERE menu_id = 1100;

-- 2. Correccion de jerarquia heredada
UPDATE menu SET menu_supe_id = 9750 WHERE menu_id = 9810 AND menu_supe_id = 9775;

-- 3. Grupos sin ruta
UPDATE menu SET pagina_ruta = NULL WHERE detall = false OR pagina_ruta = 'null';

-- 4. Rutas del frontend por pagina
UPDATE menu m
   SET pagina_ruta = r.ruta
  FROM (VALUES
    -- Configuracion > Seguridad
    (1150, '/seguridad/menus'),
    (1200, '/seguridad/permisos'),
    (1250, '/seguridad/usuarios'),
    (1300, '/seguridad/roles'),
    (1350, '/seguridad/roles-usuarios'),
    (1400, '/seguridad/roles-permisos'),
    (1450, '/seguridad/roles-menus'),
    (1500, '/seguridad/roles-sucursales'),
    (1550, '/seguridad/roles-bodegas'),
    (1600, '/seguridad/roles-precios'),
    (1650, '/seguridad/roles-documentos'),
    (1700, '/seguridad/roles-formas-pago'),
    (1750, '/seguridad/secuenciales'),
    (1800, '/seguridad/auditorias'),
    (1830, '/configuracion/certificados-emisor'),
    (1840, '/seguridad/respaldos'),
    -- Configuracion > Parametro
    (1900, '/configuracion/bodegas'),
    (1950, '/configuracion/precios'),
    (1960, '/configuracion/mesas'),
    (2000, '/configuracion/sucursales'),
    (2050, '/configuracion/sucursales-bodegas'),
    (2100, '/configuracion/sucursales-precios'),
    (2149, '/configuracion/sucursales-parametros'),
    (2150, '/configuracion/parametros'),
    (2200, '/configuracion/personas-parametros'),
    (2250, '/configuracion/personas'),
    (2300, '/configuracion/dimms'),
    (2350, '/configuracion/formas-pago'),
    (2400, '/configuracion/mapas-niveles'),
    (2450, '/configuracion/mapas'),
    (2480, '/configuracion/dimms/modificar-iva'),
    (2500, '/configuracion/parametros/registrar-licencia'),
    -- Configuracion > Tipo Documento
    (2600, '/configuracion/documentos'),
    (2650, '/configuracion/documentos-ingreso'),
    (2700, '/configuracion/documentos-egreso'),
    (2750, '/configuracion/documentos-caja'),
    (2800, '/configuracion/documentos-movimiento-ingreso'),
    (2850, '/configuracion/documentos-movimiento-egreso'),
    (2900, '/configuracion/documentos-transaccion'),
    -- Configuracion > Caja
    (3000, '/caja/perifericos'),
    (3050, '/caja/cajeros'),
    (3100, '/caja/cajas'),
    (3150, '/caja/cajas-documentos'),
    (3200, '/caja/cajas-perifericos'),
    (3250, '/caja/cajas-formas-pago'),
    -- Inventario > Producto
    (3400, '/inventario/producto-grupos-niveles'),
    (3450, '/inventario/producto-grupos'),
    (3500, '/inventario/producto-tipos'),
    (3550, '/inventario/producto-tipos-plan-cuentas'),
    (3600, '/inventario/productos'),
    (3650, '/inventario/productos-dimm'),
    (3700, '/inventario/productos-bodegas'),
    (3750, '/inventario/productos-precios'),
    (3800, '/inventario/productos-costos'),
    -- Inventario > Compra
    (3900, '/ingreso/proveedor-grupos'),
    (3950, '/ingreso/proveedor-grupos-plan-cuentas'),
    (4000, '/ingreso/proveedores'),
    (4050, '/ingreso/ingresos'),
    (4100, '/ingreso/compras'),
    (4150, '/ingreso/ingresos-detalles-impuestos'),
    (4200, '/ingreso/retenciones'),
    (4250, '/ingreso/retenciones-detalles'),
    -- Inventario > Venta
    (4350, '/caja/movimientos'),
    (4400, '/egreso/cliente-grupos'),
    (4450, '/egreso/cliente-grupos-plan-cuentas'),
    (4500, '/egreso/clientes'),
    (4550, '/egreso/vendedores'),
    (4600, '/egreso/cobradores'),
    (4650, '/egreso/egresos'),
    (4700, '/egreso/ventas'),
    (4750, '/egreso/egresos-detalles-impuestos'),
    -- Tesoreria
    (4900, '/cuentas-por-pagar/cuentas'),
    (4950, '/cuentas-por-pagar/pagos'),
    (5000, '/cuentas-por-pagar/pagos-detalles'),
    (5100, '/cuentas-por-cobrar/cuentas'),
    (5150, '/cuentas-por-cobrar/cobros'),
    (5200, '/cuentas-por-cobrar/cobros-detalles'),
    -- Contabilidad
    (5350, '/contabilidad/plan-cuentas-niveles'),
    (5400, '/contabilidad/plan-cuentas'),
    (5425, '/contabilidad/transacciones-plantillas'),
    (5450, '/contabilidad/transacciones'),
    (5500, '/contabilidad/transacciones-detalles'),
    -- Procesos
    (9800, '/procesos/documentos-electronicos'),
    (9811, '/inventario/tomas-fisicas'),
    -- Reportes
    (10200, '/reportes/configuracion/personas'),
    (10400, '/reportes/inventario/productos'),
    (10500, '/reportes/inventario/precios'),
    (10600, '/reportes/inventario/costos-saldos'),
    (10700, '/reportes/inventario/kardex'),
    (10720, '/reportes/inventario/pedidos'),
    (10750, '/reportes/inventario/tomas-fisicas'),
    (10900, '/reportes/ingreso/proveedores'),
    (11000, '/reportes/ingreso/compras'),
    (11050, '/reportes/ingreso/retenciones'),
    (11051, '/reportes/ingreso/retenciones-codigos'),
    (11200, '/reportes/egreso/clientes'),
    (11300, '/reportes/caja/movimientos'),
    (11400, '/reportes/egreso/ventas'),
    (11401, '/reportes/egreso/ventas-sesion'),
    (11402, '/reportes/egreso/ventas-formas-pago'),
    (11410, '/reportes/egreso/ventas-vendedores'),
    (11419, '/reportes/egreso/ventas-cliente-producto'),
    (11420, '/reportes/egreso/ventas-productos'),
    (11421, '/reportes/egreso/productos-mas-menos-vendidos'),
    (11451, '/reportes/egreso/retenciones-codigos'),
    (11600, '/reportes/cuentas-por-pagar/saldos'),
    (11700, '/reportes/cuentas-por-pagar/pagos'),
    (11900, '/reportes/cuentas-por-cobrar/saldos'),
    (11901, '/reportes/cuentas-por-cobrar/saldos-grupos'),
    (12000, '/reportes/cuentas-por-cobrar/cobros'),
    (12101, '/reportes/contabilidad/plan-cuentas'),
    (12200, '/reportes/contabilidad/libro-banco'),
    (12300, '/reportes/contabilidad/libro-diario'),
    (12400, '/reportes/contabilidad/libro-mayor'),
    (12500, '/reportes/contabilidad/balance-comprobacion'),
    (12600, '/reportes/contabilidad/balance-resultados'),
    (12700, '/reportes/contabilidad/balance-general'),
    (12900, '/reportes/anexo-transaccional/compras'),
    (13000, '/reportes/anexo-transaccional/retenciones'),
    (13100, '/reportes/anexo-transaccional/ventas'),
    (13200, '/reportes/anexo-transaccional/anulados'),
    (13250, '/reportes/anexo-transaccional/xml')
  ) AS r(menu_id, ruta)
 WHERE m.menu_id = r.menu_id;

-- 5. Eliminacion de MENU_NIVE
ALTER TABLE menu DROP CONSTRAINT IF EXISTS fk_menunive_menu;
DROP INDEX IF EXISTS ix_menu_menu_nive_id;
ALTER TABLE menu DROP COLUMN menu_nive_id;
DROP TABLE menu_nive;
DELETE FROM secuencial WHERE tabla = 'menu_nive';

-- 6. Restricciones de ruta
ALTER TABLE menu ADD CONSTRAINT chk_menu_pagina_ruta CHECK (
    (detall = true  AND pagina_ruta IS NOT NULL)
 OR (detall = false AND pagina_ruta IS NULL));

ALTER TABLE menu ADD CONSTRAINT uq_menu_pagina_ruta UNIQUE (pagina_ruta);
