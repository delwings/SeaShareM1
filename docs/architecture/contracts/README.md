# Contratos de Interfaz del Módulo 1 — Gestión de Flota y Activos P2P

Un archivo `.md` por contrato en la carpeta `rest/`. Todo el contenido de este directorio se deriva directamente de los SPEC funcionales en `docs/features/` (única fuente de verdad). Lo que un SPEC no defina expresamente **no se inventa**: se marca con la etiqueta `[PEND]` y se aplica la propuesta técnica por defecto documentada en el contrato.

**Qué prevalece en caso de conflicto**

- **Rutas y Parámetros**: Prevalece el archivo de contrato específico dentro de `rest/`. El índice de la §2 solo las replica; si difieren, se corrige el índice.
- **Códigos de error**: Prevalece la [tabla de decisión de la §4](#4-tabla-de-decisión-de-errores). Cada contrato debe utilizar el `code` y el estado HTTP del catálogo unificado de la §3.4.

---

## 1. Leyenda de Trazabilidad

| Etiqueta   | Significado                                                                                                                                          |
| ---------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| **[SPEC]** | Lo exige o menciona textualmente un SPEC funcional. Se cita el número de UC y el requisito (`FR-xx`, `RNF-xx`, `SC-xx` o escenario).                 |
| **[CONV]** | Convención técnica necesaria para materializar el SPEC (estructura de rutas, formato de JSON, tipo de header HTTP). No altera las reglas de negocio. |
| **[PEND]** | El SPEC no lo define de forma explícita. Se aplica una propuesta por defecto justificada y se deja sujeto a refinamiento técnico.                    |

---

## 2. Índice de Contratos REST

Todos los contratos del Módulo 1 se especifican en archivos individuales dentro de la carpeta `rest/`:

| Archivo                                                                                      | Operación / Método                                                                      | Quién → Quién                 | UC   |
| -------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------- | ----------------------------- | ---- |
| [`rest/UC01-registrar-embarcacion.md`](rest/UC01-registrar-embarcacion.md)                   | `POST /api/v1/fleet/vessels/draft`<br>`POST /api/v1/fleet/vessels/{vessel_id}/complete` | Propietario → Sistema         | UC01 |
| `rest/UC02-cancelar-registro.md`                                                             | Sin endpoint (acción de interfaz)                                                       | Propietario → Frontend        | UC02 |
| [`rest/UC03-editar-informacion.md`](rest/UC03-editar-informacion.md)                         | `PUT /api/v1/fleet/vessels/{vessel_id}`                                                 | Propietario → Sistema         | UC03 |
| [`rest/UC04-consultar-informacion-web.md`](rest/UC04-consultar-informacion-web.md)           | `GET /api/v1/fleet/vessels/{vessel_id}`                                                 | Propietario / Admin → Sistema | UC04 |
| [`rest/UC04-consultar-informacion-internal.md`](rest/UC04-consultar-informacion-internal.md) | `GET /internal/v1/vessels/{vessel_id}`<br>`GET /internal/v1/vessels` (Lote/Catálogo)    | Reservas (M2) → Sistema       | UC04 |
| [`rest/UC05-enviar-a-mantenimiento.md`](rest/UC05-enviar-a-mantenimiento.md)                 | `POST /api/v1/fleet/vessels/{vessel_id}/maintenance`                                    | Propietario / Admin → Sistema | UC05 |
| [`rest/UC06-eliminar-registro.md`](rest/UC06-eliminar-registro.md)                           | `DELETE /api/v1/fleet/vessels/{vessel_id}`                                              | Propietario / Admin → Sistema | UC06 |
| [`rest/UC07-asignar-estado-operativo.md`](rest/UC07-asignar-estado-operativo.md)             | `PATCH /internal/v1/vessels/{vessel_id}/status`                                         | Reservas (M2) → Sistema       | UC07 |
| [`rest/UC08-consultar-tarifa-base.md`](rest/UC08-consultar-tarifa-base.md)                   | `POST /internal/v1/fleet/base-rates`                                                    | Financiero (M3) → Sistema     | UC08 |
| [`rest/UC09-revisar-propietario.md`](rest/UC09-revisar-propietario.md)                       | `GET /api/v1/admin/owners`                                                              | Administrador → Sistema       | UC09 |

---

## 3. Convenciones Comunes

### 3.1 Formatos de Datos

- **Rutas**: Prefijo `/api/v1/` para Web y `/internal/v1/` para integraciones entre módulos. Segmentos en minúsculas con guiones. [CONV]
- **Cuerpos JSON**: Codificación UTF-8, claves en `snake_case` (`registration_number`, `max_capacity`, `base_rate_cop`). [CONV]
- **Enumeraciones**: En español e idénticas a los SPEC (`BORRADOR`, `DISPONIBLE`, `RESERVADO`, `EN_NAVEGACION`, `EN_MANTENIMIENTO`). [SPEC] + [CONV]
- **Identificadores**: UUID v4 representado como cadena de texto. [CONV]
- **Fechas e Instantes**: Fechas: `YYYY-MM-DD`. Instantes: ISO-8601 en UTC (`YYYY-MM-DDThh:mm:ssZ`). [CONV]
- **Dinero / Tarifa**: **String decimal** con dos decimales (`"250000.00"`), no número JSON flotante; backend procesa con `BigDecimal`. [SPEC RNF-002] + [CONV]
- **Matrícula Legal**: Expresión regular exacta: `^CP-\d{2}-\d{4}-[A-Z]$` (ej. `CP-12-0891-B`). [SPEC UC01 FR-006]

### 3.2 Headers Comunes

- **Peticiones Web (`/api/v1/*`)**: `Authorization: Bearer <JWT_TOKEN>` [PEND], `Content-Type: application/json` [CONV], `Accept: application/json` [CONV], `X-Correlation-Id` [CONV].
- **Peticiones Internas (`/internal/v1/*`)**: `X-Internal-Service-Token: <SECRET_TOKEN>` [CONV], `X-Correlation-Id` [CONV].

### 3.3 Formato Estándar de Error (Problem Details - RFC 9457 / RFC 7807)

Respuestas `4xx` y `5xx` devuelven `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Transición de estado no permitida",
  "status": 409,
  "detail": "No se puede pasar la embarcación de estado RESERVADO a EN_MANTENIMIENTO directamente.",
  "code": "INVALID_STATUS_TRANSITION",
  "retryable": false
}
```

### 3.4 Catálogo de Códigos de Error

| `code`                           | HTTP | Cuándo ocurre                                                                              | `retryable` | Origen                    | Fila Tabla |
| -------------------------------- | ---- | ------------------------------------------------------------------------------------------ | ----------- | ------------------------- | ---------- |
| `VALIDATION_ERROR`               | 400  | Formato JSON inválido, campos vacíos obligatorios o fuera de rango.                        | No          | [SPEC FR-009]             | E4         |
| `INVALID_REGISTRATION_FORMAT`    | 400  | La matrícula no cumple la regex `^CP-\d{2}-\d{4}-[A-Z]\$`.                                 | No          | [SPEC UC01 FR-006]        | E4         |
| `EXCEEDED_MAX_CAPACITY`          | 400  | La capacidad de pasajeros excede el límite del tipo de embarcación.                        | No          | [SPEC UC01 FR-008]        | E4         |
| `UNAUTHENTICATED`                | 401  | Credencial de acceso (JWT o Token Interno) ausente, inválida o expirada.                   | No          | [PEND]                    | E1         |
| `FORBIDDEN`                      | 403  | El usuario intenta modificar o acceder a una embarcación de otro propietario.              | No          | [SPEC UC03 FR-002]        | E2         |
| `VESSEL_NOT_FOUND`               | 404  | La embarcación con el ID indicado no existe o fue desactivada (`is_deleted = true`).       | No          | [SPEC UC04 esc. 3]        | E5         |
| `VESSEL_NOT_APT`                 | 422  | La embarcación está en estado `BORRADOR` o `EN_MANTENIMIENTO` y no puede operar en M2.     | No          | [SPEC UC04 esc. de borde] | E7         |
| `REGISTRATION_NUMBER_DUPLICATED` | 409  | Intento concurrente o existente de registrar una matrícula que ya existe en la BD.         | No          | [SPEC UC01 FR-011]        | E8         |
| `DRAFT_ALREADY_EXISTS`           | 409  | El propietario ya posee un borrador incompleto y no puede crear otro simultáneo.           | No          | [SPEC UC01 FR-004]        | E8         |
| `INVALID_STATUS_TRANSITION`      | 409  | Intento de realizar un cambio de estado no soportado por la máquina de estados.            | No          | [SPEC UC07 FR-004]        | E6         |
| `VESSEL_HAS_ACTIVE_RESERVATIONS` | 409  | Intento de enviar a mantenimiento o eliminar una embarcación con reservas futuras/activas. | No          | [SPEC UC06 FR-007]        | E6         |
| `INTERNAL_ERROR`                 | 500  | Error no previsto o fallo de infraestructura interna en el backend.                        | Sí          | [CONV]                    | E9         |

---

## 4. Tabla de Decisión de Errores

### 4.1 Criterio de Clasificación Principal

- **Errores `4xx` (Cliente)**: La petición es incorrecta o viola una regla de negocio del dominio. El cliente debe modificar los datos enviados antes de volver a intentar.
- **Errores `5xx` (Servidor)**: La petición era válida pero falló la infraestructura o el sistema. El cliente puede reintentar la solicitud con estrategia de _backoff_.

### 4.2 Tabla de Evaluación Estricta

Se evalúa **en estricto orden de arriba a abajo**; aplica la primera situación que se cumpla:

| Fila   | Situación                                                                                                                                                            | HTTP            | `code` por defecto                                             | `retryable` | Quién lo corrige         |
| ------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------- | -------------------------------------------------------------- | ----------- | ------------------------ |
| **E1** | Credencial de autenticación ausente, vencida o inválida.                                                                                                             | 401             | `UNAUTHENTICATED`                                              | No          | Cliente (reautenticarse) |
| **E2** | Credencial válida, pero el rol o usuario no tiene permisos sobre la embarcación (`ownership check`).                                                                 | 403             | `FORBIDDEN`                                                    | No          | Cliente / Permisos       |
| **E3** | Método HTTP (`POST`, `GET`, `PUT`, `DELETE`), `Content-Type` o `Accept` no soportado.                                                                                | 405 / 415 / 406 | `METHOD_NOT_ALLOWED`                                           | No          | Cliente                  |
| **E4** | Petición mal formada o datos que violan reglas individuales **sin consultar la BD**: formato de matrícula, capacidad negativa o excedida, campos obligatorios nulos. | 400             | `VALIDATION_ERROR`                                             | No          | Cliente                  |
| **E5** | El ID de la embarcación especificado en la **URL** no existe en el sistema o fue desactivado.                                                                        | 404             | `VESSEL_NOT_FOUND`                                             | No          | Cliente                  |
| **E6** | La embarcación existe pero su **estado operativo actual** rechaza la acción solicitada (ej. intentar editar en estado _Reservado_, o eliminar con reservas activas). | 409             | `INVALID_STATUS_TRANSITION` / `VESSEL_HAS_ACTIVE_RESERVATIONS` | No          | Cliente                  |
| **E7** | La embarcación se encuentra en estado _Borrador_ o _Mantenimiento_ al ser consultada por M2.                                                                         | 422             | `VESSEL_NOT_APT`                                               | No          | Módulo Externo (M2)      |
| **E8** | Violación de restricciones únicas a nivel de BD (matrícula duplicada o borrador previo activo).                                                                      | 409             | `REGISTRATION_NUMBER_DUPLICATED` / `DRAFT_ALREADY_EXISTS`      | No          | Cliente                  |
| **E9** | Excepción imprevista, fallo en la base de datos o fallo en el sistema de archivos local de imágenes.                                                                 | 500             | `INTERNAL_ERROR`                                               | Sí          | Desarrollador / Operador |

### 4.3 Reglas de Desempate

1. **Prioridad de Validación**: Se evalúan primero errores de formato y sintaxis (E4) antes de consultar el estado de la base de datos (E5, E6, E7, E8).
2. **`404` exclusivo para recurso en la URL**: Si el identificador que no existe viene en la URL de la ruta se devuelve `404`.
3. **Detalles en Respuestas**: En errores `4xx`, el campo `detail` especifica claramente el campo o motivo de la falla. En errores `5xx`, el campo `detail` muestra un mensaje genérico para proteger la arquitectura interna y no exponer la pila de excepciones (_stacktrace_).
