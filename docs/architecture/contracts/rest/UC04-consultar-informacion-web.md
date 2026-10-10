# UC04 — Consultar Información (Web REST)

| Campo                    | Valor                                                                      |
| ------------------------ | -------------------------------------------------------------------------- |
| **Caso de uso**          | UC04 Consultar Información de Embarcación                                  |
| **SPEC**                 | `docs/features/004-consultar-informacion-embarcacion/1-functional/spec.md` |
| **Dirección**            | Propietario / Administrador (Frontend Web) → Módulo 1 (Gestión de Flota)   |
| **¿Responde?**           | Sí (síncrono)                                                              |
| **Quién puede llamarlo** | Usuarios autenticados con rol `OWNER` o `ADMIN` `[SPEC FR-001, FR-002]`    |
| **Efectos secundarios**  | Ninguno (operación de solo lectura / idempotente)                          |

---

## 1. Propósito

Permite a un **Propietario** consultar la ficha detallada de sus propias embarcaciones registradas o a un **Administrador** revisar cualquier embarcación del catálogo `[SPEC HU1, HU2]`. Retorna los atributos comerciales, la ubicación geográfica GPS del puerto de atraque para renderizar en mapa Leaflet, la lista de servicios y el estado operativo actual `[SPEC FR-003, FR-005]`.

---

## 2. Petición HTTP

`GET /api/v1/fleet/vessels/{vessel_id}` `[CONV]`

### Headers

| Header             | Obligatorio | Valor                                  | Origen   |
| ------------------ | ----------- | -------------------------------------- | -------- |
| `Authorization`    | Sí          | `Bearer <JWT_TOKEN>`                   | `[PEND]` |
| `Accept`           | No          | `application/json`                     | `[CONV]` |
| `X-Correlation-Id` | No          | Cadena libre para trazabilidad en logs | `[CONV]` |

### Parámetros de Ruta (Path Variables)

| Parámetro   | Tipo    | Oblig. | Descripción                                       | Origen          |
| ----------- | ------- | ------ | ------------------------------------------------- | --------------- |
| `vessel_id` | UUID v4 | Sí     | Identificador único de la embarcación a consultar | `[SPEC FR-003]` |

---

## 3. Reglas de Procesamiento

1. **Autenticación y Control de Acceso (`Ownership Check`)**:
   - Se valida el token JWT del usuario.
   - Si el usuario posee rol `OWNER`, solo puede consultar embarcaciones que le pertenezcan (`owner_id = user_id`). Si intenta consultar una embarcación de otro propietario, se rechaza con `403 Forbidden` `[SPEC FR-002]`.
   - Si el usuario posee rol `ADMIN`, puede consultar la información de cualquier embarcación del sistema `[SPEC FR-001]`.
2. **Existencia y Estado Lógico**:
   - Si la embarcación no existe o tiene la marca `is_deleted = true`, el sistema responde `404 Not Found` `[SPEC HU1 esc. 3]`.
3. **Respuesta de Ficha Técnica Completa**:
   - Retorna todos los campos de la embarcación, incluyendo la tarifa base por hora en COP, la capacidad de pasajeros, el puerto con sus coordenadas GPS para el mapa, y los servicios clasificados entre base y adicionales `[SPEC FR-003, FR-005]`.
4. **SLA de Rendimiento**:
   - La respuesta debe procesarse en menos de 1 segundo `[SPEC SC-002]`.

---

## 4. Respuesta Exitosa

`200 OK` — `Content-Type: application/json`

| Campo                      | Tipo           | Descripción                                                                              | Origen                  |
| -------------------------- | -------------- | ---------------------------------------------------------------------------------------- | ----------------------- |
| `vessel_id`                | UUID v4        | Identificador de la embarcación                                                          | `[SPEC FR-003]`         |
| `owner_id`                 | UUID v4        | Identificador del propietario de la embarcación                                          | `[SPEC FR-003]`         |
| `name`                     | string         | Nombre comercial de la embarcación                                                       | `[SPEC FR-003]`         |
| `registration_number`      | string         | Matrícula legal única (`CP-NN-NNNN-X`)                                                   | `[SPEC FR-003]`         |
| `vessel_type`              | string (enum)  | Tipo: `"MOTORBOAT"`, `"SAILBOAT"`, `"CATAMARAN"`,`"YACHT"`                               | `[SPEC FR-003]`         |
| `max_capacity`             | integer        | Capacidad máxima de pasajeros permitida                                                  | `[SPEC FR-003]`         |
| `base_rate_cop`            | string decimal | Tarifa base por hora en COP (ej. `"450000.00"`)                                          | `[SPEC FR-003]`         |
| `operational_status`       | string (enum)  | Estado actual: `"AVAILABLE"`, `"RESERVADED"`, `"NAVIGATION"`, `"MAINTENANCE"`, `"DRAFT"` | `[SPEC FR-003]`         |
| `is_draft`                 | boolean        | `true` si el registro está incompleto, `false` si fue publicado                          | `[SPEC FR-003]`         |
| `photo_url`                | string (URL)   | Ruta relativa o URL pública de la fotografía principal                                   | `[SPEC FR-003]`         |
| `berth_location`           | objeto         | Datos del puerto para visualización en mapa                                              | `[SPEC FR-003, FR-005]` |
| `berth_location.port_name` | string         | Nombre del puerto de atraque                                                             | `[SPEC FR-003]`         |
| `berth_location.latitude`  | decimal        | Latitud geográfica para mapa                                                             | `[SPEC FR-005]`         |
| `berth_location.longitude` | decimal        | Longitud geográfica para mapa                                                            | `[SPEC FR-005]`         |
| `services`                 | array objetos  | Lista de servicios asignados a la embarcación                                            | `[SPEC FR-003]`         |
| `services[].id`            | UUID v4        | Identificador del servicio                                                               | `[SPEC FR-003]`         |
| `services[].name`          | string         | Nombre del servicio (ej. `"Capitán"`, `"Combustible"`)                                   | `[SPEC FR-003]`         |
| `services[].is_base`       | boolean        | `true` si es obligatorio, `false` si es opcional                                         | `[SPEC FR-003]`         |

### Ejemplo JSON

```json
{
  "vessel_id": "3f2c1a54-8b3e-4d7a-9c10-5a2b7e6f1d01",
  "owner_id": "a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d",
  "name": "Yate Tayrona Sea Breeze",
  "registration_number": "CP-04-2021-0892",
  "vessel_type": "YATE",
  "max_capacity": 25,
  "base_rate_cop": "450000.00",
  "operational_status": "DISPONIBLE",
  "is_draft": false,
  "photo_url": "/uploads/embarcaciones/3f2c1a54-yate.jpg",
  "berth_location": {
    "port_name": "Marina Internacional de Santa Marta",
    "latitude": 11.2443,
    "longitude": -74.2125
  },
  "services": [
    {
      "id": "f1e2d3c4-b5a6-9788-1122-334455667788",
      "name": "Capitán",
      "is_base": true
    },
    {
      "id": "e2d3c4b5-a697-8811-2233-445566778899",
      "name": "Combustible",
      "is_base": true
    },
    {
      "id": "d3c4b5a6-9788-1122-3344-556677889900",
      "name": "Equipo de sonido",
      "is_base": false
    }
  ]
}
```

---

## 5. Respuestas de Error

| HTTP  | `code`             | Cuándo ocurre                                                                            | `retryable` | Origen              | Fila Tabla |
| ----- | ------------------ | ---------------------------------------------------------------------------------------- | ----------- | ------------------- | ---------- |
| `401` | `UNAUTHENTICATED`  | Token JWT ausente, expirado o inválido                                                   | No          | `[PEND]`            | E1         |
| `403` | `FORBIDDEN`        | Un propietario intenta consultar una embarcación de otro propietario                     | No          | `[SPEC FR-002]`     | E2         |
| `404` | `VESSEL_NOT_FOUND` | La embarcación con el `vessel_id` dado no existe o fue desactivada (`is_deleted = true`) | No          | `[SPEC HU1 esc. 3]` | E5         |
| `500` | `INTERNAL_ERROR`   | Error imprevisto o fallo en la base de datos                                             | Sí          | `[CONV]`            | E9         |

### Ejemplo Error (403 Forbidden)

```json
{
  "type": "about:blank",
  "title": "Acceso no autorizado",
  "status": 403,
  "detail": "No tiene permisos para consultar la información de esta embarcación.",
  "code": "FORBIDDEN",
  "retryable": false
}
```

---

## 6. Idempotencia y Reintentos

- **Idempotente**: Sí (operación de consulta `GET` pura de lectura).
- **Estrategia Frontend**: Si la llamada falla por error `500` o timeout, el cliente Web puede reintentar la solicitud de forma transparente `[CONV]`.

---

## 7. Trazabilidad

`HU1`, `HU2` · `FR-001`, `FR-002`, `FR-003`, `FR-005` · `SC-002` · `Casos de borde`.
