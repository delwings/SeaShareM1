# UC05 — Enviar a Mantenimiento (Web REST)

| Campo                    | Valor                                                                                                                                                               |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Caso de uso**          | UC05 Enviar a Mantenimiento                                                                                                                                         |
| **SPEC**                 | `docs/features/005-enviar-a-mantenimiento/1-functional/spec.md`                                                                                                     |
| **Dirección**            | Propietario / Administrador (Frontend Web) → Módulo 1 (Gestión de Flota)                                                                                            |
| **¿Responde?**           | Sí (síncrono)                                                                                                                                                       |
| **Quién puede llamarlo** | Propietario de la embarcación o Administrador `[SPEC FR-001, FR-002]`                                                                                               |
| **Efectos secundarios**  | Transiciona el estado a `MAINTENANCE`, genera un registro en `status_change_log` y notifica al Propietario si la acción la ejecuta un Admin `[SPEC FR-004, FR-006]` |

---

## 1. Propósito

Permite a un **Propietario** (por revisión de rutina o reparación técnica) o a un **Administrador** (por fiscalización o reporte operativo) cambiar voluntariamente el estado de una embarcación a `MAINTENANCE` `[SPEC HU1, HU2]`. Durante este período, la embarcación queda inhabilitada para recibir nuevas cotizaciones o reservas en el sistema `[SPEC FR-005]`.

---

## 2. Petición HTTP

`POST /api/v1/fleet/vessels/{vessel_id}/maintenance` `[CONV]`

### Headers

| Header          | Obligatorio | Valor                | Origen   |
| --------------- | ----------- | -------------------- | -------- |
| `Authorization` | Sí          | `Bearer <JWT_TOKEN>` | `[PEND]` |
| `Content-Type`  | Sí          | `application/json`   | `[CONV]` |
| `Accept`        | No          | `application/json`   | `[CONV]` |

### Parámetros de Ruta (Path Variables)

| Parámetro   | Tipo    | Oblig. | Descripción                                                    | Origen          |
| ----------- | ------- | ------ | -------------------------------------------------------------- | --------------- |
| `vessel_id` | UUID v4 | Sí     | Identificador único de la embarcación a enviar a mantenimiento | `[SPEC FR-003]` |

### Cuerpo de la Petición (Request Body)

| Campo    | Tipo   | Oblig. | Descripción / Reglas de Validación                                | Origen                  |
| -------- | ------ | ------ | ----------------------------------------------------------------- | ----------------------- |
| `reason` | string | Sí     | Motivo detallado del mantenimiento/limpieza (10 a 500 caracteres) | `[SPEC FR-003, FR-004]` |

#### Ejemplo JSON

```json
{
  "reason": "Mantenimiento preventivo semestral de motor de popa y pintura de casco."
}
```

---

## 3. Reglas de Procesamiento

1. **Autenticación y Control de Acceso (`Ownership Check`)**:
   - Se verifica el token JWT.
   - Si el usuario es `PROPIETARIO`, se valida que sea el dueño de la embarcación (`owner_id = user_id`). Si intenta modificar una embarcación ajena, responde `403 Forbidden` `[SPEC FR-002]`.
   - Si el usuario es `ADMINISTRADOR`, se autoriza la acción directamente sobre cualquier embarcación `[SPEC FR-001]`.
2. **Existencia del Registro**:
   - Si la embarcación no existe o está eliminada (`is_deleted = true`), responde `404 Not Found` `[SPEC casos de borde]`.
3. **Validación de Estado Operativo Previo**:
   - La transición solo está permitida si el estado actual es `AVAILABLE` `[SPEC FR-003]`.
   - Si la embarcación ya está en `MAINTENANCE`, responde `409 Conflict` (`INVALID_STATUS_TRANSITION`) `[SPEC FR-003]`.
   - Si se encuentra en `RESERVED` o `NAVIGATION`, el backend consulta al Módulo 2 o valida sus registros; de existir compromisos o reservas activas/futuras, se rechaza con `409 Conflict` (`VESSEL_HAS_ACTIVE_RESERVATIONS`) `[SPEC FR-003]`.
4. **Cambio de Estado y Auditoría**:
   - Transiciona la embarcación a `operational_status = MAINTENANCE` `[SPEC FR-004]`.
   - Guarda un registro inmutable en `status_change_log` indicando estado anterior (`AVAILABLE`), nuevo estado (`MAINTENANCE`), motivo, usuario y fecha UTC `[SPEC FR-004]`.
5. **Notificación al Propietario**:
   - Si la acción fue ejecutada por un `ADMIN`, el sistema genera automáticamente un registro en la tabla `notification` para alertar al Propietario del motivo del mantenimiento forzado `[SPEC FR-006]`.

---

## 4. Respuesta Exitosa

`200 OK` — `Content-Type: application/json`

| Campo             | Tipo              | Descripción                                            | Origen          |
| ----------------- | ----------------- | ------------------------------------------------------ | --------------- |
| `vessel_id`       | UUID v4           | Identificador de la embarcación                        | `[SPEC FR-003]` |
| `previous_status` | string (enum)     | Estado operativo anterior (`"AVAILABLE"`)              | `[SPEC FR-004]` |
| `current_status`  | string (enum)     | Nuevo estado operativo confirmatorio (`"MAINTENANCE"`) | `[SPEC FR-004]` |
| `reason`          | string            | Motivo del mantenimiento registrado                    | `[SPEC FR-004]` |
| `updated_at`      | string (ISO-8601) | Instante UTC de la transición                          | `[CONV]`        |

### Ejemplo JSON

```json
{
  "vessel_id": "3f2c1a54-8b3e-4d7a-9c10-5a2b7e6f1d01",
  "previous_status": "DISPONIBLE",
  "current_status": "EN_MANTENIMIENTO",
  "reason": "Mantenimiento preventivo semestral de motor de popa y pintura de casco.",
  "updated_at": "2026-10-09T19:40:00Z"
}
```

---

## 5. Respuestas de Error

| HTTP  | `code`                           | Cuándo ocurre                                                                  | `retryable` | Origen                  | Fila Tabla |
| ----- | -------------------------------- | ------------------------------------------------------------------------------ | ----------- | ----------------------- | ---------- |
| `400` | `VALIDATION_ERROR`               | El campo `reason` está ausente, vacío o excede los límites de texto            | No          | `[CONV]`                | E4         |
| `401` | `UNAUTHENTICATED`                | Token JWT ausente, expirado o inválido                                         | No          | `[PEND]`                | E1         |
| `403` | `FORBIDDEN`                      | Un propietario intenta enviar a mantenimiento un barco ajeno                   | No          | `[SPEC FR-002]`         | E2         |
| `404` | `VESSEL_NOT_FOUND`               | La embarcación no existe o fue desactivada (`is_deleted = true`)               | No          | `[SPEC casos de borde]` | E5         |
| `409` | `INVALID_STATUS_TRANSITION`      | La embarcación ya está en `MAINTENANCE` o está en `DRAFT`                      | No          | `[SPEC FR-003]`         | E6         |
| `409` | `VESSEL_HAS_ACTIVE_RESERVATIONS` | La embarcación se encuentra actualmente en estado `RESERVED` o `EN_NAVIGATION` | No          | `[SPEC FR-003]`         | E6         |
| `500` | `INTERNAL_ERROR`                 | Error imprevisto o fallo en la base de datos                                   | Sí          | `[CONV]`                | E9         |

### Ejemplo Error (409 Conflict - Reservas Activas)

```json
{
  "type": "about:blank",
  "title": "Embarcación con reservas activas",
  "status": 409,
  "detail": "No se puede enviar a mantenimiento la embarcación porque posee reservas activas o viajes en navegación en curso.",
  "code": "VESSEL_HAS_ACTIVE_RESERVATIONS",
  "retryable": false
}
```

---

## 6. Idempotencia y Reintentos

- **Idempotencia**: No estricto por método HTTP (`POST`). Sin embargo, si se reintenta enviar a mantenimiento una embarcación que ya pasó exitosamente a `MAINTENANCE`, la segunda llamada responde `409 Conflict` (`INVALID_STATUS_TRANSITION`), lo cual confirma que el recurso ya está en dicho estado `[CONV]`.

---

## 7. Trazabilidad

`HU1`, `HU2` · `FR-001` a `FR-006` · `SC-001`, `SC-002` · `Casos de borde`.
