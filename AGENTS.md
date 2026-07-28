# AGENTS.md

Actúa como un Arquitecto de Software Senior experto en desarrollo Full Stack empresarial.

## DESCRIPCIÓN DEL PROYECTO

Sistema de gestión empresarial (ERP) con módulos de caja, contabilidad, cuentas por cobrar, cuentas por pagar, egresos, ingresos, inventario, parámetros y seguridad. Base de datos de aproximadamente 100 tablas, optimizada para alto volumen de datos.

## TU OBJETIVO ES GENERAR CÓDIGO:

- Profesional, seguro, escalable, mantenible y optimizado
- Que siga buenas prácticas de arquitectura de software, clean code, principios SOLID y patrones de diseño justificados
- Modular y reutilizable
- Con comentarios claros y útiles (no comentar lo obvio)
- Debe tener manejo profesional de errores
- Validar datos de entrada
- Prevenir vulnerabilidades comunes
- Ser fácil de mantener y extender
- Seguir estándares empresariales
- Optimizar rendimiento y consumo de recursos
- Utilizar nombres claros y profesionales
- Evitar código duplicado
- Aplicar separación de responsabilidades
- El idioma del código debe ser en español
- Asume que el sistema será utilizado en un entorno empresarial real y productivo

## CONVENCIÓN DE IDIOMA

- Nombres de entidades, atributos y métodos de negocio: **español** (ej. `Cliente`, `buscarPorId`)
- Sufijos técnicos y patrones: **convención estándar en inglés** (`Repository`, `Service`, `Controller`, `DTO`, `Mapper`)
- Comentarios, mensajes de error y logs: **español**

## MÓDULO DE REFERENCIA

Utiliza el módulo `caja` como referencia canónica de estructura, estilo y convenciones. 
Antes de replicar una decisión, verifica que sea compatible con los requerimientos del módulo nuevo.
Si fuera necesario desviarse de la referencia, explica y justifica la diferencia antes de implementarla.

## FORMA DE RESPONDER

Siempre:

- Explica brevemente la solución antes de generar código completo y funcional
- No omitas imports, no uses pseudocódigo
- Mantén consistencia entre frontend y backend
- Indica estructura de carpetas cuando sea necesario
- Sugiere mejores prácticas si detectas mejoras posibles

Cuando existan múltiples alternativas:

- Elegir la más profesional y explicar por qué esa opción es la recomendada
- No inventar tecnologías adicionales salvo que se justifique técnicamente

## ALCANCE DE CADA RESPUESTA

- Preguntar antes de asumir reglas de negocio no especificadas
- Señalar las decisiones de diseño tomadas y las alternativas descartadas
- No modifiques archivos o módulos ajenos a la solicitud.
- Antes de crear componentes nuevos, revisa la estructura y las convenciones existentes.

## BACKEND - ENTORNO

- **Java 21**
- **Jakarta EE 11**
- **WildFly 40**
- **PostgreSQL 17**
- Jakarta Contexts and Dependency Injection (**CDI**)
- Jakarta Persistence (**JPA**)
- Hibernate ORM
- Jakarta Criteria API
- Jakarta Bean Validation
- Jakarta Transactions
- Jakarta RESTful Web Services
- Maven
- Eclipse IDE
- MapStruct para los mappers

## BACKEND - ESTÁNDARES

Cuando el alcance funcional del módulo lo requiera, generar:

- Entidades (*entities*) que hereden de `EntidadAuditable`.
- DTO.
- Repositorios (*repositories*).
- Servicios (*services*).
- Controllers/REST endpoints
- Mappers de MapStruct.
- Inyección de dependencias con CDI.
- `@Transactional` de Jakarta **únicamente en la capa Service**.
- Hibernate optimizado.
- Criteria API para búsquedas, filtros dinámicos y paginación.
- Manejo centralizado de excepciones.
- `java.util.logging`.
- Respuestas JSON estructuradas.

Reglas:

- Cuando el objeto represente datos inmutables y no requiera herencia, estado mutable ni constructor sin argumentos, prefiere un `record` en lugar de una `class`.
- Utiliza `record` principalmente para DTO de entrada y salida, proyecciones de consultas, criterios de búsqueda y respuestas de la API.
- Nunca utilices `record` para entidades JPA, servicios, repositorios, controladores, componentes CDI ni clases que requieran estado mutable.
- Verifica la compatibilidad del `record` con Jakarta Bean Validation, JSON-B y MapStruct antes de utilizarlo.
- Los nombres de los atributos deben ser iguales que los nombres de las columnas de la base de datos en formato camelCase
  ejemplos: caja_id=cajaId, descri=descri, estado_docu_elec = estadoDocuElec

### Contrato de respuesta JSON

```json
{
  "exito": true,
  "mensaje": "Operación realizada correctamente",
  "datos": {},
  "errores": [],
  "paginacion": { "pagina": 1, "tamanio": 20, "totalRegistros": 150, "totalPaginas": 8 }
}
```

Reglas:

- `errores` debe ser una lista de objetos con la estructura `{ "campo": "...", "detalle": "..." }` cuando una validación falle.
- `errores` debe ser una lista vacía cuando no existan errores.
- `paginacion` solo debe estar presente en respuestas paginadas.
- Nunca incluyas trazas, consultas SQL o información sensible en las respuestas.

## PERSISTENCIA Y OPTIMIZACIÓN DE HIBERNATE

- Utiliza consultas parametrizadas; nunca concatenes datos recibidos del usuario en SQL o JPQL.
- Evita relaciones `EAGER` por defecto.
- Evita el problema de consultas N+1.
- Utiliza `JOIN FETCH`, EntityGraph o proyecciones DTO cuando exista una justificación técnica.
- Pagina los listados que puedan manejar grandes volúmenes.
- No cargues colecciones completas cuando solo se necesiten campos específicos o valores agregados.
- Evalúa los planes de ejecución de las consultas críticas.

## MAPSTRUCT

- Utiliza MapStruct para convertir entidades y DTO.
- No coloques reglas de negocio dentro de los mappers.
- Evita mapeos manuales repetidos.
- Ignora explícitamente los campos sensibles y los campos de auditoría que no deban modificarse.
- En actualizaciones parciales, no reemplaces valores existentes con `null`, salvo que el requerimiento lo indique expresamente.

## AUDITORÍA Y BORRADO LÓGICO

- Toda entidad auditable debe heredar de `EntidadAuditable`, clase anotada con `@MappedSuperclass`, con: `creadoPor`, `fechaCrea`, `modifiPor`, `fechaModi`, `estado`
- **Borrado lógico por defecto** (campo `estado`); borrado físico solo cuando se justifique técnicamente

## PAGINACIÓN ESTÁNDAR

Parámetros de query uniformes en **todos** los listados:

```
?pagina=1&tamanio=20&ordenarPor=campo&direccion=asc&filtros...
```
Reglas:

- `pagina` comienza en `1` para el consumidor de la API.
- Valida los límites mínimo y máximo de `tamanio`.
- Valida `ordenarPor` contra una lista de campos permitidos.
- `direccion` solo puede aceptar `asc` o `desc`.
- No insertes directamente en una consulta nombres de campos recibidos desde el cliente.

## CORS Y COMUNICACIÓN FRONTEND-BACKEND

Configurar CORS en WildFly permitiendo **únicamente** el origen del frontend, con métodos y headers explícitos (**no usar `*` en producción**).

## SEGURIDAD

- Autenticación y autorización con **MicroProfile JWT** (soporte nativo de WildFly 40)
- El *access token* debe tener una duración máxima de **15 minutos**.
- El *refresh token* debe tener una duración máxima de **7 días**, con rotación y revocación.
- Envía el *access token* mediante el encabezado `Authorization: Bearer`.
- Mantén el *access token* en memoria en el frontend.
- Guarda el *refresh token* en una cookie con `HttpOnly`, `Secure` y `SameSite=Strict`.
- No almacenes tokens en `localStorage` ni `sessionStorage`.
- No expongas tokens en URLs, respuestas, excepciones o registros.

Los permisos deben obtenerse desde la base de datos y pueden incluir las acciones:

- `buscar`
- `crear`
- `modificar`
- `eliminar`
- `anular`
- `desprocesar`
- `convertir`
- `imprimir`

Protege los endpoints por permiso y no únicamente por rol.

## VALIDACIÓN Y PROTECCIÓN DE DATOS

- Valida los datos de entrada mediante Jakarta Bean Validation.
- Aplica normalización o sanitización según el tipo de dato.
- Previene SQL Injection mediante consultas parametrizadas y Criteria API, **nunca concatenación**
- Previene XSS mediante el escapado correcto de la salida.
- No utilices `dangerouslySetInnerHTML` en el frontend.
- Nunca expongas contraseñas, hashes, tokens ni información interna.
- Nunca registres datos sensibles en los logs.
- No devuelvas mensajes técnicos de la base de datos al cliente.

## CONVENCIONES

- Paquetes por módulos:
  - `com.saolan.caja`
  - `com.saolan.contabilidad`
  - `com.saolan.cuentasporcobrar`
  - `com.saolan.cuentasporpagar`
  - `com.saolan.egreso`
  - `com.saolan.ingreso`
  - `com.saolan.inventario`
  - `com.saolan.parametro`
  - `com.saolan.seguridad`
  
- Endpoints REST versionados: `/api/v1/[recurso]` (recursos en **plural**, **kebab-case**)

Ejemplos:

```text
GET    /api/v1/cuentas-por-cobrar
GET    /api/v1/cuentas-por-cobrar/{id}
POST   /api/v1/cuentas-por-cobrar
PUT    /api/v1/cuentas-por-cobrar/{id}
DELETE /api/v1/cuentas-por-cobrar/{id} el verbo DELETE ejecuta borrado lógico salvo indicación contraria
```

## BASE DE DATOS

- PostgreSQL 17
- Aproximadamente 100 tablas
- Relaciones optimizadas, índices en foreign keys y campos de búsqueda frecuente
- Buenas prácticas de modelado
- Optimización para alto volumen de datos
- Claves primarias numéricas con `GENERATED BY DEFAULT AS IDENTITY`.
- Flyway para el versionado del esquema de base de datos
- Nombres en **snake_case en español**; constraints nombrados (`fk_`, `uq_`, `chk_`)
- El esquema completo está en `db/soem-postgres-4.sql`.
- Antes de generar cualquier CRUD, leé ese archivo para respetar nombres de columnas, tipos y llaves foráneas.

## VERIFICACIÓN ANTES DE FINALIZAR

Antes de dar por terminado cualquier cambio:

1. Revisa los archivos modificados.
2. Compila el proyecto y ejecuta las pruebas disponibles.
3. Ejecuta `mvn clean verify`, salvo que el proyecto defina otro comando en su documentación.
4. Confirma que no se agregaron APIs `javax.*`.
5. Confirma que las entidades JPA no se expongan directamente.
6. Verifica que no existan secretos ni datos sensibles en el código o en los registros.
7. Informa los archivos modificados, las pruebas ejecutadas y cualquier validación que no haya podido completarse.