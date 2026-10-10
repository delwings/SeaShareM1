# UC03 — Editar Información (Web REST)

| Campo                    | Valor                                                                                                                  |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------- |
| **Caso de uso**          | UC03 Editar Información de Embarcación                                                                                 |
| **SPEC**                 | `docs/features/003-editar-informacion-embarcacion/1-functional/spec.md`                                                |
| **Dirección**            | Propietario (Frontend Web) → Módulo 1 (Gestión de Flota)                                                               |
| **¿Responde?**           | Sí (síncrono)                                                                                                          |
| **Quién puede llamarlo** | Exclusivamente el Propietario dueño de la embarcación `[SPEC FR-001, FR-002]`                                          |
| **Efectos secundarios**  | Actualiza los atributos de la embarcación en BD y opcionalmente reemplaza el archivo de imagen `[SPEC FR-003, FR-007]` |

---

## 1. Propósito

Permite a un **Propietario** actualizar la información comercial de una embarcación previamente registrada `[SPEC HU1]`: cambiar nombre, ajustar la tarifa base COP, modificar la capacidad dentro de los topes permitidos, actualizar la ubicación del puerto de atraque, gestionar servicios adicionales o reemplazar la fotografía principal `[SPEC FR-003, FR-007]`.

---

## 2. Petición HTTP

`PUT /api/v1/fleet/vessels/{vessel_id}` `[CONV]`

### Headers

| Header          | Obligatorio | Valor                 | Origen          |
| --------------- | ----------- | --------------------- | --------------- |
| `Authorization` | Sí          | `Bearer <JWT_TOKEN>`  | `[PEND]`        |
| `Content-Type`  | Sí          | `multipart/form-data` | `[SPEC FR-007]` |
| `Accept`        | No          | `application/json`    | `[CONV]`        |

### Parámetros de Ruta (Path Variables)

| Parámetro   | Tipo    | Oblig. | Descripción                                    | Origen          |
| ----------- | ------- | ------ | ---------------------------------------------- | --------------- |
| `vessel_id` | UUID v4 | Sí     | Identificador único de la embarcación a editar | `[SPEC FR-002]` |

### Form Data / Body (Multipart)

| Campo Form | Tipo             | Oblig. | Descripción / Reglas de Validación                                                   | Origen          |
| ---------- | ---------------- | ------ | ------------------------------------------------------------------------------------ | --------------- |
| `data`     | string (JSON)    | Sí     | Cadena JSON con los datos actualizados de la embarcación                             | `[CONV]`        |
| `photo`    | Archivo (Binary) | No     | Imagen en formato JPG/PNG (≤ 10 MB). Opcional: si se envía, reemplaza la foto actual | `[SPEC FR-007]` |

#### Estructura del JSON interno en `data`

| Campo                      | Tipo           | Oblig. | Reglas de Validación                              | Origen          |
| -------------------------- | -------------- | ------ | ------------------------------------------------- | --------------- |
| `name`                     | string         | Sí     | Nombre comercial (1 a 100 caracteres)             | `[SPEC FR-003]` |
| `max_capacity`             | integer        | Sí     | Entero > 0. Respeta topes por tipo de barco       | `[SPEC FR-004]` |
| `base_rate_cop`            | string decimal | Sí     | Tarifa base por hora en COP (número > 0)          | `[SPEC FR-003]` |
| `berth_location`           | objeto         | Sí     | Ubicación GPS del puerto de atraque               | `[SPEC FR-003]` |
| `berth_location.port_name` | string         | Sí     | Nombre del puerto                                 | `[SPEC FR-003]` |
| `berth_location.latitude`  | decimal        | Sí     | Latitud geográfica (-90.0 a 90.0)                 | `[SPEC FR-003]` |
| `berth_location.longitude` | decimal        | Sí     | Longitud geográfica (-180.0 a 180.0)              | `[SPEC FR-003]` |
| `selected_service_ids`     | array de UUID  | No     | Lista de IDs de servicios opcionales actualizados | `[SPEC FR-003]` |

##### Ejemplo de contenido para la parte `data`:

```json
{
  "name": "Yate Tayrona Sea Breeze II",
  "max_capacity": 30,
  "base_rate_cop": "500000.00",
  "berth_location": {
    "port_name": "Marina Internacional de Santa Marta",
    "latitude": 11.2443,
    "longitude": -74.2125
  },
  "selected_service_ids": [
    "b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e",
    "c3d4e5f6-a7b8-9c0d-1e2f-3a4b5c6d7e8f"
  ]
}
```

---

## 3. Reglas de Procesamiento

1. **Autenticación y Autorización de Rol**: Se valida el token JWT. El usuario debe tener el rol `OWNER` `[SPEC FR-001]`.
2. **Verificación de Propiedad (`Ownership Check`)**: Se comprueba que el `owner_id` de la embarcación coincida con el usuario autenticado. Si pertenece a otro propietario, responde `403 Forbidden` `[SPEC FR-002]`.
3. **Existencia y Estado Lógico**: Si la embarcación no existe o tiene la marca `is_deleted = true`, responde `404 Not Found` `[SPEC casos de borde]`.
4. **Validación de Estado Operativo Permitido**:
   - La edición solo se autoriza si el estado operativo actual de la embarcación es `AVAILABLE` o `MAINTENANCE` `[SPEC FR-005]`.
   - Si la embarcación se encuentra en estado `RESERVED` o `NAVIGATION`, la modificación se rechaza con `409 Conflict` (`INVALID_STATUS_TRANSITION`) para proteger la integridad de los alquileres en curso en M2 `[SPEC FR-005]`.
5. **Inmutabilidad de Matrícula y Tipo**:
   - La matrícula legal (`registration_number`) y el tipo de embarcación (`vessel_type`) no están incluidos en la carga útil de edición; **son inmutables** tras la publicación y permanecen intactos en la BD `[SPEC FR-006]`.
6. **Topes de Capacidad**:
   - La nueva `max_capacity` se valida contra el tipo inmutable de la embarcación (Lancha ≤ 12, Velero ≤ 15, Catamarán ≤ 30, Yate ≤ 40). Si supera el tope, se rechaza con `400 Bad Request` (`EXCEEDED_MAX_CAPACITY`) `[SPEC FR-004]`.
7. **Servicios Base Inmutables**:
   - Los servicios base (_Capitán_ y _Combustible_) se conservan automáticamente como obligatorios en la BD. La lista `selected_service_ids` solo actualiza los servicios adicionales `[SPEC FR-003]`.
8. **Reemplazo Opcional de Fotografía**:
   - Si se adjunta un archivo en el campo `photo`, se valida formato (JPG/PNG) y tamaño (≤ 10 MB). Se reemplaza el archivo local previo en `/uploads/embarcaciones/` y se actualiza la ruta `[SPEC FR-007]`. Si no se envía archivo, la fotografía existente se conserva sin cambios `[SPEC FR-007]`.

---

## 4. Respuesta Exitosa

`200 OK` — `Content-Type: application/json`

| Campo                 | Tipo              | Descripción                             | Origen          |
| --------------------- | ----------------- | --------------------------------------- | --------------- |
| `vessel_id`           | UUID v4           | Identificador de la embarcación         | `[SPEC FR-002]` |
| `name`                | string            | Nombre comercial actualizado            | `[SPEC FR-003]` |
| `registration_number` | string            | Matrícula legal intacta (inmutable)     | `[SPEC FR-006]` |
| `vessel_type`         | string (enum)     | Tipo de embarcación intacto (inmutable) | `[SPEC FR-006]` |
| `max_capacity`        | integer           | Nueva capacidad de pasajeros            | `[SPEC FR-004]` |
| `base_rate_cop`       | string decimal    | Nueva tarifa base por hora en COP       | `[SPEC FR-003]` |
| `photo_url`           | string (URL)      | Ruta de la imagen (nueva o conservada)  | `[SPEC FR-007]` |
| `updated_at`          | string (ISO-8601) | Instante UTC de la actualización        | `[CONV]`        |

### Ejemplo JSON

```json
{
  "vessel_id": "3f2c1a54-8b3e-4d7a-9c10-5a2b7e6f1d01",
  "name": "Yate Tayrona Sea Breeze II",
  "registration_number": "CP-04-2021-0892",
  "vessel_type": "YACHT",
  "max_capacity": 30,
  "base_rate_cop": "500000.00",
  "photo_url": "/uploads/embarcaciones/3f2c1a54-yate.jpg",
  "updated_at": "2026-10-09T19:30:00Z"
}
```

---

## 5. Respuestas de Error

| HTTP  | `code`                      | Cuándo ocurre                                                         | `retryable` | Origen                  | Fila Tabla |
| ----- | --------------------------- | --------------------------------------------------------------------- | ----------- | ----------------------- | ---------- |
| `400` | `EXCEEDED_MAX_CAPACITY`     | La capacidad modificada excede el límite del tipo de embarcación      | No          | `[SPEC FR-004]`         | E4         |
| `400` | `INVALID_FILE_FORMAT`       | El archivo adjunto no es JPG/PNG o supera los 10 MB                   | No          | `[SPEC FR-007]`         | E4         |
| `401` | `UNAUTHENTICATED`           | Token JWT de usuario ausente o inválido                               | No          | `[PEND]`                | E1         |
| `403` | `FORBIDDEN`                 | El usuario no posee rol `PROPIETARIO` o intenta editar un barco ajeno | No          | `[SPEC FR-002]`         | E2         |
| `404` | `VESSEL_NOT_FOUND`          | La embarcación solicitada no existe o tiene `is_deleted = true`       | No          | `[SPEC casos de borde]` | E5         |
| `409` | `INVALID_STATUS_TRANSITION` | La embarcación está en estado `RESERVADO` o `EN_NAVEGACION`           | No          | `[SPEC FR-005]`         | E6         |
| `500` | `INTERNAL_ERROR`            | Error imprevisto o fallo en la base de datos al guardar               | Sí          | `[CONV]`                | E9         |

### Ejemplo Error (409 Conflict)

```json
{
  "type": "about:blank",
  "title": "Embarcación no editable en este momento",
  "status": 409,
  "detail": "No se puede modificar la embarcación porque actualmente se encuentra en estado RESERVADO.",
  "code": "INVALID_STATUS_TRANSITION",
  "retryable": false
}
```

---

## 6. Idempotencia y Reintentos

- **Idempotente**: La modificación de atributos mediante `PUT` con la misma carga útil produce el mismo estado final en BD.
- **Estrategia Frontend**: Si se envía un nuevo archivo de imagen y la llamada falla con `500`, el frontend solicita al usuario reintentar la acción `[CONV]`.

---

## 7. Trazabilidad

`HU1` · `FR-001` a `FR-007` · `SC-001` · `Casos de borde`.
