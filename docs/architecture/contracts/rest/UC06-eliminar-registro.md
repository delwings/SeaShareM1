# UC06 — Eliminar Registro (Web REST)

| Campo                    | Valor                                                                                                                |
| ------------------------ | -------------------------------------------------------------------------------------------------------------------- |
| **Caso de uso**          | UC06 Eliminar Registro de Embarcación                                                                                |
| **SPEC**                 | `docs/features/006-eliminar-registro-embarcacion/1-functional/spec.md`                                               |
| **Dirección**            | Propietario / Administrador (Frontend Web) → Módulo 1 (Gestión de Flota)                                             |
| **¿Responde?**           | Sí (síncrono)                                                                                                        |
| **Quién puede llamarlo** | Propietario de la embarcación o Administrador `[SPEC FR-001, FR-002]`                                                |
| **Efectos secundarios**  | Aplica baja lógica (`is_deleted = true`), invalida el catálogo comercial y asienta auditoría `[SPEC FR-003, FR-005]` |

---

## 1. Propósito

Permite a un **Propietario** o a un **Administrador** retirar permanentemente una embarcación del catálogo comercial activo de la plataforma `[SPEC HU1, HU2]`. Para mantener la integridad referencial e histórica de reservas procesadas (M2) e informes contables (M3), la operación ejecuta un **borrado lógico** (`is_deleted = true`), haciendo que la embarcación deje de ser visible o almacenable para futuras operaciones `[SPEC FR-003]`.

---

## 2. Petición HTTP

`DELETE /api/v1/fleet/vessels/{vessel_id}` `[CONV]`

### Headers

| Header             | Obligatorio | Valor                                  | Origen   |
| ------------------ | ----------- | -------------------------------------- | -------- |
| `Authorization`    | Sí          | `Bearer <JWT_TOKEN>`                   | `[PEND]` |
| `Accept`           | No          | `application/json`                     | `[CONV]` |
| `X-Correlation-Id` | No          | Cadena libre para trazabilidad en logs | `[CONV]` |

### Parámetros de Ruta (Path Variables)

| Parámetro   | Tipo    | Oblig. | Descripción                                      | Origen          |
| ----------- | ------- | ------ | ------------------------------------------------ | --------------- |
| `vessel_id` | UUID v4 | Sí     | Identificador único de la embarcación a eliminar | `[SPEC FR-002]` |

---

## 3. Reglas de Procesamiento

1. **Autenticación y Control de Acceso (`Ownership Check`)**:
   - Se verifica el token JWT.
   - Si el usuario posee el rol `OWNER`, se comprueba que sea el dueño de la embarcación (`owner_id = user_id`). Si intenta eliminar una embarcación de otro usuario, responde `403 Forbidden` `[SPEC FR-002]`.
   - Si el usuario posee el rol `ADMIN`, se autoriza la baja sobre cualquier embarcación del sistema `[SPEC FR-001]`.
2. **Existencia y Estado Lógico Previo**:
   - Si la embarcación no existe o ya posee la marca `is_deleted = true`, el sistema responde `404 Not Found` `[SPEC casos de borde]`.
3. **Restricción de Estado Operativo Permitido**:
   - La eliminación únicamente se autoriza si la embarcación se encuentra en estado `AVAILABLE` o `` `[SPEC FR-004]`.
   - Si la embarcación está en estado `RESERVED` o `NAVIGATION` (tiene compromisos o viajes activos), la operación se rechaza con `409 Conflict` (`VESSEL_HAS_ACTIVE_RESERVATIONS`) `[SPEC FR-004]`.
4. **Baja Lógica y Conservación Histórica**:
   - Se actualiza la marca `is_deleted = true` en la tabla `vessel` `[SPEC FR-003]`.
   - El registro **no se elimina físicamente** de la base de datos ni se borran sus fotos guardadas, asegurando que M2 y M3 puedan seguir consultando de forma pasiva su historial de auditoría e informes financieros pasados `[SPEC FR-003]`.
   - La matrícula legal (`registration_number`) permanece vinculada al registro histórico y no se libera para nuevas embarcaciones para evitar colisiones de auditoría `[SPEC FR-003]`.
5. **Auditoría de Baja**:
   - Se asienta un registro de auditoría en la tabla `status_change_log` indicando la desactivación lógica y la identidad del usuario que la solicitó `[SPEC FR-005]`.

---

## 4. Respuesta Exitosa

`204 No Content` — Petición procesada exitosamente. La embarcación ha sido desactivada lógicamente y no devuelve cuerpo en la respuesta `[CONV]`.

---

## 5. Respuestas de Error

| HTTP  | `code`                           | Cuándo ocurre                                                                 | `retryable` | Origen                  | Fila Tabla |
| ----- | -------------------------------- | ----------------------------------------------------------------------------- | ----------- | ----------------------- | ---------- |
| `401` | `UNAUTHENTICATED`                | Token JWT ausente, expirado o inválido                                        | No          | `[PEND]`                | E1         |
| `403` | `FORBIDDEN`                      | Un propietario intenta eliminar una embarcación de otro propietario           | No          | `[SPEC FR-002]`         | E2         |
| `404` | `VESSEL_NOT_FOUND`               | La embarcación no existe o ya fue eliminada previamente (`is_deleted = true`) | No          | `[SPEC casos de borde]` | E5         |
| `409` | `VESSEL_HAS_ACTIVE_RESERVATIONS` | La embarcación se encuentra en estado `RESERVED` o `NAVIGATION`               | No          | `[SPEC FR-004]`         | E6         |
| `500` | `INTERNAL_ERROR`                 | Error imprevisto o fallo en la base de datos al guardar el cambio             | Sí          | `[CONV]`                | E9         |

### Ejemplo Error (409 Conflict)

```json
{
  "type": "about:blank",
  "title": "Embarcación con reservas o viajes en curso",
  "status": 409,
  "detail": "No se puede eliminar la embarcación porque actualmente se encuentra en estado RESERVED o NAVIGATION.",
  "code": "VESSEL_HAS_ACTIVE_RESERVATIONS",
  "retryable": false
}
```

---

## 6. Idempotencia y Reintentos

- **Idempotente**: Sí (método HTTP `DELETE`). Si se reintenta eliminar una embarcación que ya fue desactivada lógicamente previa y exitosamente, la segunda llamada devolverá `404 Not Found`, confirmando que el recurso activo ya no existe en el catálogo `[CONV]`.

---

## 7. Trazabilidad

`HU1`, `HU2` · `FR-001`, `FR-002`, `FR-003`, `FR-004`, `FR-005` · `SC-001` · `Casos de borde`.
