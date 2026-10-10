# UC04 — Consultar Información de Embarcación (Internal REST)

| Campo                    | Valor                                                                                   |
| ------------------------ | --------------------------------------------------------------------------------------- |
| **Caso de uso**          | UC04 Consultar Información de Embarcación                                               |
| **SPEC**                 | `docs/features/004-consultar-informacion-embarcacion/1-functional/spec.md`              |
| **Dirección**            | Sistema de Reservas y Operaciones (Módulo 2) → Módulo 1 (Gestión de Flota)              |
| **¿Responde?**           | Sí (síncrono)                                                                           |
| **Quién puede llamarlo** | Exclusivamente el Módulo 2 (`CU-09 Proveer información de embarcación`) `[SPEC FR-004]` |
| **Efectos secundarios**  | Ninguno (operaciones de solo lectura / idempotentes)                                    |

---

## 1. Propósito

Proporcionar al **Módulo de Reservas y Operaciones (Módulo 2)** la ficha de datos completos, características técnicas, capacidad de pasajeros, ubicación GPS del puerto de atraque con su zona horaria IANA, catálogo de servicios e identidad del propietario `[SPEC FR-003, FR-004]`. Expone tanto la consulta **individual** de una embarcación como la consulta **en lote paginada** para poblar el catálogo de búsqueda de M2 `[SPEC M2]`.

---

## 2. Operación 1 — Consulta Individual (`GET /internal/v1/vessels/{vessel_id}`)

### Petición HTTP

`GET /internal/v1/vessels/{vessel_id}` `[CONV]`

#### Headers

| Header                     | Obligatorio | Valor                                        | Origen   |
| -------------------------- | ----------- | -------------------------------------------- | -------- |
| `X-Internal-Service-Token` | Sí          | Token de servicio interno backend-to-backend | `[PEND]` |
| `Accept`                   | No          | `application/json`                           | `[CONV]` |
| `X-Correlation-Id`         | No          | Cadena libre para trazabilidad de logs       | `[CONV]` |

#### Parámetros de Ruta (Path Variables)

| Parámetro   | Tipo    | Oblig. | Descripción                                 | Origen          |
| ----------- | ------- | ------ | ------------------------------------------- | --------------- |
| `vessel_id` | UUID v4 | Sí     | Identificador único de la embarcación en M1 | `[SPEC FR-004]` |

---

### Respuesta Exitosa Operación 1

`200 OK` — `Content-Type: application/json`

| Campo                      | Tipo          | Descripción                                                                           | Origen                  |
| -------------------------- | ------------- | ------------------------------------------------------------------------------------- | ----------------------- |
| `vessel_id`                | UUID v4       | Identificador único de la embarcación                                                 | `[SPEC FR-003]`         |
| `owner_id`                 | UUID v4       | Identificador único del propietario                                                   | `[SPEC FR-003]`         |
| `name`                     | string        | Nombre comercial de la embarcación                                                    | `[SPEC FR-003]`         |
| `registration_number`      | string        | Matrícula legal única (`CP-NN-NNNN-X`)                                                | `[SPEC FR-003]`         |
| `vessel_type`              | string (enum) | Tipo: `"MOTORBOAT"`, `"SAILBOAT"`, `"CATAMARAN"`,`"YACHT"`                            | `[SPEC FR-003]`         |
| `max_capacity`             | integer       | Capacidad máxima de pasajeros permitida                                               | `[SPEC FR-003, FR-004]` |
| `operational_status`       | string (enum) | Estado actual: `"AVAILABLE"`, `"RESERVED"`, `"NAVIGATION"`,`"MAINTENANCE"`, `"DRAFT"` | `[SPEC FR-003]`         |
| `photo_url`                | string (URL)  | Ruta/URL de la fotografía representativa                                              | `[CONV]`                |
| `berth_location`           | objeto        | Ubicación GPS y zona horaria del puerto                                               | `[SPEC FR-003, M2]`     |
| `berth_location.port_name` | string        | Nombre del puerto de atraque                                                          | `[SPEC FR-003]`         |
| `berth_location.latitude`  | decimal       | Latitud geográfica                                                                    | `[SPEC FR-003]`         |
| `berth_location.longitude` | decimal       | Longitud geográfica                                                                   | `[SPEC FR-003]`         |
| `berth_location.time_zone` | string        | Zona horaria IANA del puerto (ej. `"America/Bogota"`)                                 | `[SPEC M2]`             |
| `services`                 | array objetos | Lista de servicios vinculados a la embarcación                                        | `[SPEC FR-003]`         |
| `services[].name`          | string        | Nombre del servicio (ej. `"Capitán"`, `"Combustible"`)                                | `[SPEC FR-003]`         |
| `services[].is_base`       | boolean       | `true` si es obligatorio, `false` si es adicional                                     | `[SPEC FR-003]`         |

#### Ejemplo JSON

```json
{
  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "owner_id": "a1c2e3f4-5678-90ab-cdef-1234567890ab",
  "name": "Yate Tayrona Sea Breeze",
  "registration_number": "CP-04-2021-0892",
  "vessel_type": "YATE",
  "max_capacity": 12,
  "operational_status": "DISPONIBLE",
  "photo_url": "/uploads/embarcaciones/d3b07384-yate.jpg",
  "berth_location": {
    "port_name": "Marina Internacional de Santa Marta",
    "latitude": 11.2443,
    "longitude": -74.2125,
    "time_zone": "America/Bogota"
  },
  "services": [
    { "name": "Capitán", "is_base": true },
    { "name": "Combustible", "is_base": true },
    { "name": "Aire Acondicionado", "is_base": false },
    { "name": "Equipo de Esnórquel", "is_base": false }
  ]
}
```

---

## 3. Operación 2 — Consulta en Lote / Catálogo (`GET /internal/v1/vessels`)

### Petición HTTP

`GET /internal/v1/vessels?page=1&size=20&vessel_type=YATE` `[CONV]`

#### Query Parameters

| Parámetro | Tipo    | Oblig. | Descripción                                | Origen      |
| --------- | ------- | ------ | ------------------------------------------ | ----------- |
| `page`    | integer | No     | Número de página (base 1, defecto: 1)      | `[SPEC M2]` |
| `size`    | integer | No     | Tamaño de página (defecto: 20, máximo: 50) | `[SPEC M2]` |

### Respuesta Exitosa Operación 2

`200 OK` — `Content-Type: application/json`

```json
{
  "pagination": {
    "page": 1,
    "size": 20,
    "total_elements": 1,
    "total_pages": 1
  },
  "vessels": [
    {
      "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
      "name": "Yate Tayrona Sea Breeze",
      "vessel_type": "YATE",
      "max_capacity": 12,
      "photo_url": "/uploads/embarcaciones/d3b07384-yate.jpg",
      "berth_location": {
        "port_name": "Marina Internacional de Santa Marta",
        "latitude": 11.2443,
        "longitude": -74.2125,
        "time_zone": "America/Bogota"
      },
      "has_captain": true
    }
  ]
}
```

---

## 4. Reglas de Procesamiento (Ambas Operaciones)

1. **Autenticación Interna**: Se valida el header `X-Internal-Service-Token`. Si el token es inválido o está ausente, se rechaza la petición con `401 Unauthenticated` `[PEND]`.
2. **Existencia y Ocultamiento**: Si en la consulta individual el `vessel_id` no existe o tiene `is_deleted = true`, M1 responde `404 Not Found` `[SPEC HU1 esc. 3]`.
3. **Validación de Aptitud Operativa**:
   - Si la embarcación consultada individualmente está en estado `BORRADOR` o `EN_MANTENIMIENTO`, M1 responde `422 Unprocessable Entity` (`VESSEL_NOT_APT`), indicando a M2 que no puede recibir reservas `[SPEC casos de borde]`.
   - En la consulta en lote (Operación 2), M1 automáticamente excluye del listado las embarcaciones eliminadas, en borrador o en mantenimiento `[CONV]`.
4. **Ausencia Estricta de Datos Monetarios**: De acuerdo con la regla `FR-009` de M2, las respuestas de M1 en estos endpoints omiten tarifas monetarias para asegurar la separación de responsabilidades con el Módulo 3 `[SPEC M2 FR-009]`.
5. **SLA de Rendimiento**: M1 responde las consultas en menos de 300 ms `[SPEC M2 SC-001]`.

---

## 5. Respuestas de Error

| HTTP  | `code`             | Cuándo ocurre                                                                               | `retryable` | Origen                  | Fila Tabla |
| ----- | ------------------ | ------------------------------------------------------------------------------------------- | ----------- | ----------------------- | ---------- |
| `400` | `VALIDATION_ERROR` | Parámetros de paginación o UUID con sintaxis inválida                                       | No          | `[CONV]`                | E4         |
| `401` | `UNAUTHENTICATED`  | Token de servicio interno ausente o inválido                                                | No          | `[PEND]`                | E1         |
| `404` | `VESSEL_NOT_FOUND` | La embarcación individual solicitada no existe o fue eliminada (`is_deleted = true`)        | No          | `[SPEC HU1 esc. 3]`     | E5         |
| `422` | `VESSEL_NOT_APT`   | La embarcación individual solicitada se encuentra en estado `BORRADOR` o `EN_MANTENIMIENTO` | No          | `[SPEC casos de borde]` | E7         |
| `500` | `INTERNAL_ERROR`   | Error imprevisto o fallo de conectividad en la base de datos                                | Sí          | `[CONV]`                | E9         |

### Ejemplo Error (404 Not Found)

```json
{
  "type": "about:blank",
  "title": "Embarcación no encontrada",
  "status": 404,
  "detail": "La embarcación solicitada no se encuentra registrada en el sistema de flota.",
  "code": "VESSEL_NOT_FOUND",
  "retryable": false
}
```

---

## 6. Idempotencia y Reintentos

- **Idempotencia**: Garantizada por tratarse de métodos de lectura `GET`.
- **Estrategia M2**: M2 aplica un tiempo límite (_read timeout_) de 300 ms y realiza máximo 1 reintento rápido ante errores `5xx` o fallos de red. Ante respuestas `4xx`, M2 aborta sin reintentar `[SPEC M2 FR-008]`.

---

## 7. Trazabilidad

`HU2` · `FR-003`, `FR-004`, `FR-005` · `SC-002` · `Casos de borde`.
