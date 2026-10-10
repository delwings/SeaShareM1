# UC02 — Cancelar Registro (Web REST)

| Campo                    | Valor                                                                                            |
| ------------------------ | ------------------------------------------------------------------------------------------------ |
| **Caso de uso**          | UC02 Cancelar Registro de Embarcación                                                            |
| **SPEC**                 | `docs/features/002-cancelar-registro-embarcacion/1-functional/spec.md`                           |
| **Dirección**            | Propietario (Frontend Web) → Módulo 1 (Gestión de Flota)                                         |
| **¿Responde?**           | Sí (síncrono)                                                                                    |
| **Quién puede llamarlo** | Exclusivamente el Propietario dueño del borrador `[SPEC FR-001, FR-002]`                         |
| **Efectos secundarios**  | Eliminación física permanente del registro borrador y sus tablas asociadas en BD `[SPEC FR-003]` |

---

## 1. Propósito

Permite a un **Propietario** descartar voluntariamente el proceso de alta de una embarcación que se encuentra incompleto (en estado `DRAFT`) `[SPEC HU1]`. A diferencia de las embarcaciones publicadas activas, el borrador se elimina físicamente de la base de datos para no dejar registros huérfanos ni bloquear prematuramente la matrícula legal `[SPEC FR-003]`.

---

## 2. Petición HTTP

`DELETE /api/v1/fleet/vessels/drafts/{vessel_id}` `[CONV]`

### Headers

| Header          | Obligatorio | Valor                | Origen   |
| --------------- | ----------- | -------------------- | -------- |
| `Authorization` | Sí          | `Bearer <JWT_TOKEN>` | `[PEND]` |
| `Accept`        | No          | `application/json`   | `[CONV]` |

### Parámetros de Ruta (Path Variables)

| Parámetro   | Tipo    | Oblig. | Descripción                                              | Origen          |
| ----------- | ------- | ------ | -------------------------------------------------------- | --------------- |
| `vessel_id` | UUID v4 | Sí     | Identificador único de la embarcación en estado borrador | `[SPEC FR-002]` |

---

## 3. Reglas de Procesamiento

1. **Autenticación y Autorización de Rol**: Se verifica el token JWT. El usuario debe tener rol `OWNER` `[SPEC FR-001]`.
2. **Verificación de Propiedad (`Ownership Check`)**: Se valida que la embarcación con `vessel_id` pertenezca al `owner_id` autenticado. Si pertenece a otro usuario, se rechaza con `403 Forbidden` `[SPEC FR-002]`.
3. **Existencia del Registro**: Se busca la embarcación por `vessel_id`. Si no existe, se devuelve `404 Not Found` `[SPEC casos de borde]`.
4. **Validación de Estado Borrador**:
   - Se comprueba que la embarcación tenga la marca `is_draft = true` y estado `DRAFT`.
   - Si la embarcación ya fue publicada (`is_draft = false` o estado `AVAILABLE`), **no se puede cancelar mediante esta operación** y se responde con `409 Conflict` (`INVALID_STATUS_TRANSITION`), indicando que debe usarse la eliminación de registro activo (UC06) `[SPEC FR-004]`.
5. **Eliminación Física Cascaded**: Se efectúa el borrado físico (`DELETE`) en BD del registro en la tabla `vessel` y sus relaciones dependientes (`berth_location`, `vessel_service`). Liberando inmediatamente la matrícula legal para ser registrada en el futuro `[SPEC FR-003]`.

---

## 4. Respuesta Exitosa

`204 No Content` — Petición procesada exitosamente sin cuerpo de respuesta.

---

## 5. Respuestas de Error

| HTTP  | `code`                      | Cuándo ocurre                                                              | `retryable` | Origen                  | Fila Tabla |
| ----- | --------------------------- | -------------------------------------------------------------------------- | ----------- | ----------------------- | ---------- |
| `401` | `UNAUTHENTICATED`           | Token JWT ausente, expirado o inválido                                     | No          | `[PEND]`                | E1         |
| `403` | `FORBIDDEN`                 | El usuario no posee rol `PROPIETARIO` o intenta cancelar un borrador ajeno | No          | `[SPEC FR-002]`         | E2         |
| `404` | `VESSEL_NOT_FOUND`          | No existe un borrador con el `vessel_id` proporcionado                     | No          | `[SPEC casos de borde]` | E5         |
| `409` | `INVALID_STATUS_TRANSITION` | La embarcación ya no está en borrador (ya fue publicada)                   | No          | `[SPEC FR-004]`         | E6         |
| `500` | `INTERNAL_ERROR`            | Error imprevisto o fallo en la base de datos durante la eliminación        | Sí          | `[CONV]`                | E9         |

### Ejemplo Error (409 Conflict)

```json
{
  "type": "about:blank",
  "title": "Operación no permitida",
  "status": 409,
  "detail": "No se puede descartar el borrador porque la embarcación ya ha sido publicada oficialmente.",
  "code": "INVALID_STATUS_TRANSITION",
  "retryable": false
}
```

---

## 6. Idempotencia y Reintentos

- **Idempotente**: Sí (método HTTP `DELETE`). Si el cliente reintenta una llamada tras un borrado exitoso previo, obtendrá `404 Not Found`, lo cual confirma que el recurso ya no existe en el sistema `[CONV]`.

---

## 7. Trazabilidad

`HU1` · `FR-001`, `FR-002`, `FR-003`, `FR-004` · `SC-001` · `Casos de borde`.
