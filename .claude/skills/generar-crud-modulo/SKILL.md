---
name: generar-crud-modulo
description: Genera el CRUD backend de un módulo (agregado) del ERP siguiendo el estándar de CLAUDE.md
argument-hint: <modulo> <tabla_raiz>
---

# Tarea: generar el CRUD backend del módulo indicado en `$ARGUMENTS`

`$ARGUMENTS` = **`<modulo> <tabla_raiz>`** (p. ej. `caja caja`, `inventario producto`).
Módulo objetivo: el primer término. Tabla raíz del agregado: el segundo. Paquete base:
`com.saolan.soem.<modulo>`.

Fuentes de verdad, **no las repitas**:
- **`CLAUDE.md`**: convenciones generales del ERP (idioma, capas y entregables —Entity, DTO,
  Repository, Service, Controller, Mapper—, contrato `RespuestaApi`, paginación, seguridad por
  permiso, CORS, manejo de errores, auditoría/borrado lógico, forma de trabajar). Se cargan solas.
- **`soem-postgres-4.sql`**: el esquema. No lo regeneres ni inventes columnas; modela contra él.

El primer módulo generado es el **canónico de referencia**: máxima calidad, será copiado por el
resto. Al generar un módulo nuevo, imita la estructura del módulo ya existente que sirva de patrón.

---

## Solo backend

**No generes frontend.** La UI se construye en **React**, en un proyecto separado que consumirá
esta API. Deja el backend autosuficiente para ese consumo: CORS al origen del frontend, combos
expuestos, y `RespuestaApi` / paginación / autenticación (Bearer + refresh) según CLAUDE.md.

## Procedimiento (analiza y CONFÍRMAME antes de escribir código)

### Paso 1 — Descubrir el agregado desde el DDL
Partiendo de `<tabla_raiz>`, recorre las FKs hacia abajo (la hija es la que tiene la FK) hasta las
hojas. Para cada tabla anota PK, FKs, cardinalidad, autoreferencias, presencia de `estado` y de
columnas de auditoría (`USUARI_REGI/FECHA_HORA_REGI/USUARI_MODI/FECHA_HORA_MODI`).

**Clasifica el tipo de PK de cada tabla:**
- **PK propia con `IDENTITY`** → entidad normal con `@GeneratedValue(strategy = IDENTITY)`.
- **PK que es a la vez FK** (la PK referencia a otra tabla) → **tabla de especialización 1:1**
  (el DDL las conserva "sin identity, con PK heredada"). Es una relación 1:1 con su padre, no una
  hija 1:N. Ver Paso 2 para su mapeo.

**Frontera del agregado (crítico):** el módulo posee **solo** las tablas que le pertenecen
funcionalmente. **Detente** en cualquier tabla que (a) pertenezca a otro módulo, (b) sea una tabla
transaccional de alto volumen referenciada desde fuera, o (c) sea un catálogo/maestro de
referencia. Esas son **combos**, no hijas editables. Sin esta frontera, seguir FKs a ciegas
arrastra medio ERP.

### Paso 2 — Detectar excepciones al estándar de CLAUDE.md
El estándar dice "toda entidad hereda de `EntidadAuditable`" y "borrado lógico por `estado`", pero
el DDL no es uniforme. Resuelve por tabla:
- **Con** columnas de auditoría → la entidad extiende `EntidadAuditable` (mapeado a esos nombres
  reales); coordina con el trigger `trg_marca_modificacion()` (gobierna las columnas `_modi`).
- **Sin** columnas de auditoría → la entidad **no** extiende `EntidadAuditable`.
- **Con** `estado` → borrado lógico. **Sin** `estado` → es detalle del agregado: composición
  (`cascade = ALL` + `orphanRemoval = true`), sin endpoints ni CRUD propios.

**Tablas de especialización (PK = FK, detectadas en el Paso 1):**
- No llevan `IDENTITY`: comparten la PK con el padre. Mapea la relación 1:1 con `@OneToOne` +
  `@MapsId` (o `@PrimaryKeyJoinColumn`), y el `@Id` sin `@GeneratedValue`.
- Decide y **confírmame**: ¿es parte del agregado del padre (composición 1:1, `orphanRemoval`,
  editable dentro de la pantalla del padre) o un sub-recurso independiente (p. ej.
  `/api/v1/<padre-plural>/{id}/<especializacion>`)? Elige según su ciclo de vida y sensibilidad.

**Campos sensibles.** Detecta columnas de credenciales (claves, contraseñas, tokens; p. ej.
`CLAVE`): nunca deben aparecer en DTO de salida, respuestas ni logs. El DTO de entrada las acepta
solo si el flujo las necesita; el mapper (MapStruct) las ignora explícitamente en la salida
(ya normado en CLAUDE.md).

### Paso 3 — Validaciones de negocio comentadas en el DDL
Revisa "PUNTO 2" del DDL: las restricciones `UNIQUE`/`CHECK` allí comentadas NO existen en la BD.
Las que apliquen a las tablas del agregado, **impleméntalas en la capa `Service`**.

### Paso 4 — Combos para las FKs externas
Cada FK a una tabla fuera del agregado necesita un combo de solo lectura
`GET /api/v1/<recurso-plural-kebab-case>/combo`, que devuelve activos sin paginar proyectando solo
`ComboDTO { valor, etiqueta }` (proyección con Criteria). **Cada combo pertenece al módulo dueño de
esa tabla, NO a este módulo**; no coloques su ruta bajo `com.saolan.soem.<modulo>`. Lo único
compartido es `ComboDTO` en `comun`. Si el módulo dueño aún no existe, crea **solo** el endpoint
mínimo de combo (Controller + Service + Repository, más la entidad mínima necesaria para la
proyección), sin su CRUD completo. Si dudas del dueño, pregúntame.

**Expón el combo propio (maestros/catálogos).** Si alguna tabla del agregado que estás generando
es referenciada por FKs desde OTRAS tablas del DDL (es decir, otros módulos la usarán como combo),
expón proactivamente su propio `GET /api/v1/<recurso>/combo` como parte de este CRUD, para que los
consumidores solo lo usen y no tengan que crear un stub. Verifícalo buscando en el DDL las FKs que
apuntan a la PK de cada tabla del agregado.

**Idempotencia (evita sobrescritura y duplicación).** Antes de crear cualquier archivo de un
combo, **lee el módulo dueño** y verifica si ya existe:
- Si el módulo dueño **ya expone** un endpoint `combo` (típicamente un *stub* creado antes por un
  módulo consumidor), **extiende** esos archivos —`Controller`, `Service`, `Repository` y la
  entidad— agregando lo que falte y **conservando** el endpoint existente. **No los reemplaces ni
  regeneres desde cero**, y **nunca** dupliques el endpoint, el método ni la entidad.
- Si vas a generar el CRUD completo del módulo que ya tenía el stub, **expande** la entidad mínima
  a la entidad completa y suma los demás métodos (búsqueda paginada, alta, modificación, baja),
  dejando intacto el `combo` que ya consumían otros módulos.
- Como generar un combo implica escribir en un módulo ajeno, si el módulo dueño ya existe con
  código propio, **no lo modifiques sin avisar**: descríbeme el cambio mínimo y pídeme confirmación
  (coherente con "no modifiques módulos ajenos" de CLAUDE.md).
- Recomendación de orden: genera antes los módulos maestros/de catálogo dueños de los combos
  (p. ej. `parametro`, `seguridad`) y después sus consumidores; así el consumidor solo *usa* el
  combo y no hay ningún stub que reconciliar.

### Paso 5 — Confirmar y generar
Preséntame: el árbol del agregado con cardinalidades, la frontera (qué dejaste fuera y por qué),
las excepciones del Paso 2, las validaciones del Paso 3 y los combos con su módulo dueño. **Espera
mi visto bueno.** Luego genera `comun` (si falta) y el módulo capa por capa, con código completo y
funcional (sin omitir imports, sin pseudocódigo), aplicando lo definido en CLAUDE.md. Pregunta
antes de asumir reglas de negocio no especificadas (semántica de `anular`/`desprocesar`/`convertir`
para este módulo, bloqueos por estado, predeterminados únicos, etc.).

---

## Ejemplo resuelto: módulo `caja` (referencia del resultado esperado)

Invocación: `/generar-crud-modulo caja caja`

- **Paso 1 — Agregado:**
  ```
  CAJA (raíz · FK → SUCURSAL · tiene ESTADO)
  ├── CAJA_DOCU_EGRE (1:N · FK → DOCU_EGRE · sin ESTADO · sin auditoría)
  │   └── SALI_ARCH  (1:N · hoja · sin ESTADO · sin auditoría · flag PREDET)
  └── CAJA_PERI      (1:N · hoja · FK → PERIFERICO · sin ESTADO · sin auditoría · flag ACCESO)
  ```
  **Frontera:** `CAJA_MOVI` también es hija de `CAJA` por FK, pero queda **fuera** (transaccional,
  de otro módulo, referenciada por `EGRESO`). No seguir sus FKs.
- **Paso 2 — Excepciones:** ninguna tabla del agregado tiene columnas de auditoría → **ninguna
  extiende `EntidadAuditable`**. Solo `CAJA` tiene `ESTADO` → borrado lógico. `CAJA_DOCU_EGRE`,
  `SALI_ARCH`, `CAJA_PERI` sin `ESTADO` → composición con `orphanRemoval`.
- **Paso 3 — Validaciones al Service:** unicidad `(sucursal_id, codigo)` en `Caja`; coherencia de
  la numeración `(serie1, serie2, numero)` en `CajaDocuEgre`.
- **Paso 4 — Combos (cada uno en su módulo dueño):** `sucursales/combo` (`CAJA.sucursal_id`),
  `documentos-egreso/combo` (`CAJA_DOCU_EGRE.documento_id`), `perifericos/combo`
  (`CAJA_PERI.periferico_id`).
