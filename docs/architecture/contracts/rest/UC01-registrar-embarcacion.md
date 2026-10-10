# UC01 — Registrar Embarcación (Web REST)

| Campo                    | Valor                                                                                                          |
| ------------------------ | -------------------------------------------------------------------------------------------------------------- |
| **Caso de uso**          | UC01 Registrar Embarcación                                                                                     |
| **SPEC**                 | `docs/features/001-registrar-embarcacion/1-functional/spec.md`                                                 |
| **Dirección**            | Propietario (Frontend Web) → Módulo 1 (Gestión de Flota)                                                       |
| **¿Responde?**           | Sí (síncrono)                                                                                                  |
| **Quién puede llamarlo** | Exclusivamente usuarios autenticados con rol `OWNER` `[SPEC FR-001]`                                           |
| **Efectos secundarios**  | Persiste/actualiza la embarcación en BD y almacena el archivo de fotografía localmente `[SPEC FR-003, FR-009]` |

---

## 1. Propósito

Permite a un **Propietario** dar de alta una nueva embarcación en la plataforma mediante un proceso guiado en dos etapas `[SPEC HU1, HU2]`:

1. **Paso 1 (Borrador Temporal)**: Guarda únicamente los datos básicos de la embarcación (nombre, matrícula legal, capacidad máxima y tipo de embarcación), sin exigir puerto, tarifa, fotografía ni servicios adicionales. El registro queda marcado como borrador (`is_draft = true`, sin estado operativo) `[SPEC FR-…]`.
2. **Paso 2 (Completar Registro)**: Solicita la fotografía, el puerto de atraque (nombre y coordenadas GPS), la tarifa base y los servicios adicionales. Al confirmarse, el registro deja de ser borrador (`is_draft = false`) y pasa a `AVAILABLE` `[SPEC FR-…]`.

---

## 2. Petición HTTP — Paso 1: Crear / Actualizar Borrador

`POST /api/v1/fleet/vessels/draft` `[CONV]`

### Headers

| Header          | Obligatorio | Valor                | Origen   |
| --------------- | ----------- | -------------------- | -------- | ---------------------- |
| `Authorization` | Sí          | `Bearer <JWT_TOKEN>` | `[PEND]` |
| `Content-Type`  | Sí          | `application/json`   | `[CONV]` | `, `, `CATAMARAN`, ``) |
| `Accept`        | No          | `application/json`   | `[CONV]` |

### Cuerpo de la Petición (Request Body - Paso 1)

| Campo                      | Tipo           | Oblig. | Descripción / Reglas                                                   | Origen          |
| -------------------------- | -------------- | ------ | ---------------------------------------------------------------------- | --------------- |
| `name`                     | string         | Sí     | Nombre comercial de la embarcación (1 a 100 caracteres)                | `[SPEC FR-005]` |
| `legal_registration`       | string         | Sí     | Matrícula legal. Regex: `^CP-\d{2}-\d{4}-[A-Z]$`                       | `[SPEC FR-006]` |
| `vessel_type`              | string (enum)  | Sí     | `"MOTORBOAT"`, `"SAILBOAT"`, `"CATAMARAN"`,`"YACHT"`                   | `[SPEC FR-007]` |
| `max_capacity`             | integer        | Sí     | Entero > 0. Topes: Lancha ≤ 12, Velero ≤ 15, Catamarán ≤ 30, Yate ≤ 40 | `[SPEC FR-008]` |
| `base_rate_cop`            | string decimal | Sí     | Tarifa base por hora en COP (número > 0)                               | `[SPEC FR-005]` |
| `berth_location`           | objeto         | Sí     | Datos del puerto de atraque                                            | `[SPEC FR-005]` |
| `berth_location.port_name` | string         | Sí     | Nombre del puerto                                                      | `[SPEC FR-005]` |
| `berth_location.latitude`  | decimal        | Sí     | Coordenada latitud (-90.0 a 90.0)                                      | `[SPEC FR-005]` |
| `berth_location.longitude` | decimal        | Sí     | Coordenada longitud (-180.0 a 180.0)                                   | `[SPEC FR-005]` |
| `selected_service_ids`     | array de UUID  | No     | Lista de IDs de servicios opcionales adicionales                       | `[SPEC FR-005]` |

#### Ejemplo JSON Paso 1

```json
{
  "name": "Yate Tayrona Sea Breeze",
  "legal_registration": "CP-04-2021-0892",
  "vessel_type": "YACHT",
  "max_capacity": 25,
  "base_rate": "450000.00",
  "berth_location": {
    "port_name": "Marina Internacional de Santa Marta",
    "latitude": 11.2443,
    "longitude": -74.2125
  },
  "selected_service_ids": ["b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e"]
}
```

---

## 3. Petición HTTP — Paso 2: Finalizar Registro (Carga de Foto y Publicación)

`POST /api/v1/fleet/vessels/{vessel_id}/complete` `[CONV]`

### Headers

| Header          | Obligatorio | Valor                 | Origen          |
| --------------- | ----------- | --------------------- | --------------- |
| `Authorization` | Sí          | `Bearer <JWT_TOKEN>`  | `[PEND]`        |
| `Content-Type`  | Sí          | `multipart/form-data` | `[SPEC FR-009]` |

### Form Data (Paso 2)

| Parámetro Form | Tipo             | Oblig. | Descripción / Restricciones                    | Origen          |
| -------------- | ---------------- | ------ | ---------------------------------------------- | --------------- |
| `photo`        | Archivo (Binary) | Sí     | Imagen en formato JPG o PNG, peso máximo 10 MB | `[SPEC FR-009]` |

---

## 4. Reglas de Procesamiento

1. **Invariante de Unicidad de Borradores**:
   - Si un propietario ya posee un borrador activo e intenta iniciar uno nuevo sin completarlo, el backend **elimina físicamente el borrador anterior** para mantener la regla de unicidad de un solo borrador activo por propietario `[SPEC FR-004]`.
2. **Validaciones Formales e Invariantes**:
   - **Matrícula Legal**: Verifica el formato regex `^CP-\d{2}-\d{4}-[A-Z]$`. Si no cumple, responde `400 Bad Request` (`INVALID_REGISTRATION_FORMAT`) `[SPEC FR-006]`.
   - **Capacidad Máxima**: Valida el tope del tipo seleccionado (Lancha ≤ 12, Velero ≤ 15, Catamarán ≤ 30, Yate ≤ 40). Si excede, responde `400 Bad Request` (`EXCEEDED_MAX_CAPACITY`) `[SPEC FR-008]`.
   - **Unicidad de Matrícula**: Se comprueba en BD que no exista otra embarcación (activa o borrador) con el mismo `registration_number`. Si existe, responde `409 Conflict` (`REGISTRATION_NUMBER_DUPLICATED`) `[SPEC FR-011]`.
3. **Inclusión de Servicios Base**:
   - Los servicios obligatorios (_Capitán_ y _Combustible_) se vinculan automáticamente a la embarcación en la BD, independientemente de los adicionales seleccionados por el usuario `[SPEC FR-005]`.
4. **Completitud en Paso 2**:
   - Al recibir la imagen en `/complete`, se valida el formato (JPG/PNG) y peso (≤ 10 MB). Se almacena localmente en `/uploads/embarcaciones/` y la embarcación cambia de estado: `is_draft = false`, `operational_status = AVAILABLE` `[SPEC FR-009, FR-010]`.

---

## 5. Respuestas Exitosas

### Respuesta Paso 1 (Creación de Borrador)

`201 Created` — `Content-Type: application/json`

```json
{
  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "is_draft": true,
  "operational_status": "DRAFT",
  "created_at": "2026-10-09T19:00:00Z"
}
```

### Respuesta Paso 2 (Publicación Confirmada)

`200 OK` — `Content-Type: application/json`

```json
{
  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "name": "Yate Tayrona Sea Breeze",
  "registration_number": "CP-04-2021-0892",
  "is_draft": false,
  "operational_status": "AVAILABLE",
  "photo_url": "/uploads/embarcaciones/d3b07384-yate.jpg",
  "published_at": "2026-10-09T19:05:00Z"
}
```

---

## 6. Respuestas de Error

| HTTP  | `code`                           | Cuándo ocurre                                                                  | `retryable` | Origen          | Fila Tabla |
| ----- | -------------------------------- | ------------------------------------------------------------------------------ | ----------- | --------------- | ---------- |
| `400` | `INVALID_REGISTRATION_FORMAT`    | La matrícula no cumple el patrón regex `^CP-\d{2}-\d{4}-[A-Z]$`                | No          | `[SPEC FR-006]` | E4         |
| `400` | `EXCEEDED_MAX_CAPACITY`          | La capacidad de pasajeros supera el tope permitido para el tipo de embarcación | No          | `[SPEC FR-008]` | E4         |
| `400` | `INVALID_FILE_FORMAT`            | La fotografía no es de tipo JPG/PNG o excede los 10 MB de tamaño               | No          | `[SPEC FR-009]` | E4         |
| `401` | `UNAUTHENTICATED`                | Token JWT de usuario ausente o expirado                                        | No          | `[PEND]`        | E1         |
| `403` | `FORBIDDEN`                      | El usuario autenticado no posee el rol `PROPIETARIO`                           | No          | `[SPEC FR-001]` | E2         |
| `404` | `VESSEL_NOT_FOUND`               | En el Paso 2, la embarcación con `vessel_id` no existe en estado borrador      | No          | `[SPEC FR-009]` | E5         |
| `409` | `REGISTRATION_NUMBER_DUPLICATED` | La matrícula enviada ya se encuentra registrada por otro propietario           | No          | `[SPEC FR-011]` | E8         |
| `500` | `INTERNAL_ERROR`                 | Error imprevisto, fallo en BD o error guardando la imagen en disco             | Sí          | `[CONV]`        | E9         |

### Ejemplo Error (400 Bad Request - Capacidad Excedida)

```json
{
  "type": "about:blank",
  "title": "Capacidad máxima excedida",
  "status": 400,
  "detail": "Para el tipo LANCHA, la capacidad máxima no puede ser mayor a 12 pasajeros.",
  "code": "EXCEEDED_MAX_CAPACITY",
  "retryable": false
}
```

---

## 7. Idempotencia y Reintentos

- **Paso 1 (Borrador)**: Si se envía nuevamente una solicitud de borrador por el mismo propietario, el sistema reemplaza el borrador anterior de forma idempotente `[SPEC FR-004]`.
- **Paso 2 (Confirmación)**: Operación no idempotente por carga de archivos. Si la petición falla con error `500`, el cliente web debe solicitar al usuario reintentar la subida `[CONV]`.

---

## 8. Trazabilidad

`HU1`, `HU2` · `FR-001` a `FR-011` · `SC-001`, `SC-002` · `Casos extremos`.
