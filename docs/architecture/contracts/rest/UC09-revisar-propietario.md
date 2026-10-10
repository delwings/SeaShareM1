# UC09 — Revisar Propietario (Web REST)

| Campo                    | Valor                                                                |
| ------------------------ | -------------------------------------------------------------------- |
| **Caso de uso**          | UC09 Revisar Propietario                                             |
| **SPEC**                 | `docs/features/009-revisar-propietario/1-functional/spec.md`         |
| **Dirección**            | Administrador (Frontend Web) → Módulo 1 (Gestión de Flota)           |
| **¿Responde?**           | Sí (síncrono)                                                        |
| **Quién puede llamarlo** | Exclusivamente usuarios autenticados con rol `ADMIN` `[SPEC FR-001]` |
| **Efectos secundarios**  | Ninguno (operación de solo lectura / idempotente)                    |

---

## 1. Propósito

Permite a los **Administradores** de la plataforma auditar a los propietarios registrados y consultar la flota completa de embarcaciones vinculadas a cada uno de ellos `[SPEC HU1]`. Facilita la fiscalización administrativa, el seguimiento de la capacidad instalada y la gestión de soporte ante cualquier novedad operativa reportada en M2 o M3 `[SPEC FR-002, FR-003]`.

---

## 2. Petición HTTP

`GET /api/v1/admin/owners` `[CONV]`

### Headers

| Header             | Obligatorio | Valor                                        | Origen          |
| ------------------ | ----------- | -------------------------------------------- | --------------- |
| `Authorization`    | Sí          | `Bearer <JWT_TOKEN>` — Token con rol `ADMIN` | `[SPEC FR-001]` |
| `Accept`           | No          | `application/json`                           | `[CONV]`        |
| `X-Correlation-Id` | No          | Cadena libre para trazabilidad de logs       | `[CONV]`        |

### Parámetros de Consulta (Query Parameters)

| Parámetro  | Tipo    | Oblig. | Descripción                                                        | Origen          |
| ---------- | ------- | ------ | ------------------------------------------------------------------ | --------------- |
| `page`     | integer | No     | Número de página solicitada (base 1, defecto: 1)                   | `[CONV]`        |
| `size`     | integer | No     | Cantidad de propietarios por página (defecto: 20, máximo: 50)      | `[CONV]`        |
| `q`        | string  | No     | Búsqueda por coincidencia de nombre, email o número de documento   | `[SPEC FR-002]` |
| `owner_id` | UUID v4 | No     | Filtro directo para consultar un propietario específico y su flota | `[SPEC FR-003]` |

---

## 3. Reglas de Procesamiento

1. **Autenticación y Autorización de Rol Exclusivo**:
   - Se verifica el token JWT del usuario.
   - Si el usuario no está autenticado o su rol es `OWNER`, la petición es rechazada de inmediato con `403 Forbidden` (`FORBIDDEN`), bloqueando cualquier acceso a la vista administrativa `[SPEC FR-001]`.
2. **Consulta Paginada y Búsqueda**:
   - Permite listar a los propietarios registrados en el sistema, retornando su información básica de identidad (nombre completo, correo electrónico, documento de identidad) `[SPEC FR-002]`.
   - Si se envía el parámetro `owner_id`, se retornan los detalles del propietario especificado junto con el arreglo de sus embarcaciones asociadas `[SPEC FR-003]`.
3. **Manejo de Flota Asociada**:
   - Para cada propietario, se lista el resumen de sus embarcaciones registradas (tanto activas como en borrador o en mantenimiento) `[SPEC FR-003]`.
   - Las embarcaciones con eliminación lógica (`is_deleted = true`) se excluyen por defecto o se marcan expresamente como inactivas según la consulta de auditoría `[SPEC FR-003]`.
4. **SLA de Rendimiento**:
   - Las consultas administrativas paginadas deben responder en menos de 1 segundo `[SPEC SC-001]`.

---

## 4. Respuesta Exitosa

`200 OK` — `Content-Type: application/json`

| Campo                                    | Tipo          | Descripción                                      | Origen                  |
| ---------------------------------------- | ------------- | ------------------------------------------------ | ----------------------- |
| `pagination`                             | objeto        | Metadata de paginación                           | `[CONV]`                |
| `pagination.page`                        | integer       | Página actual devuelta                           | `[CONV]`                |
| `pagination.size`                        | integer       | Cantidad de registros por página                 | `[CONV]`                |
| `pagination.total_elements`              | integer       | Total de propietarios coincidentes en BD         | `[CONV]`                |
| `pagination.total_pages`                 | integer       | Total de páginas disponibles                     | `[CONV]`                |
| `owners`                                 | array objetos | Listado de propietarios y sus embarcaciones      | `[SPEC FR-002, FR-003]` |
| `owners[].owner_id`                      | UUID v4       | Identificador del propietario                    | `[SPEC FR-002]`         |
| `owners[].full_name`                     | string        | Nombre completo del propietario                  | `[SPEC FR-002]`         |
| `owners[].email`                         | string        | Correo electrónico de contacto                   | `[SPEC FR-002]`         |
| `owners[].document_number`               | string        | Número de documento de identidad                 | `[SPEC FR-002]`         |
| `owners[].vessels_count`                 | integer       | Cantidad total de embarcaciones que posee        | `[SPEC FR-003]`         |
| `owners[].vessels`                       | array objetos | Resumen de la flota del propietario              | `[SPEC FR-003]`         |
| `owners[].vessels[].vessel_id`           | UUID v4       | Identificador de la embarcación                  | `[SPEC FR-003]`         |
| `owners[].vessels[].name`                | string        | Nombre comercial de la embarcación               | `[SPEC FR-003]`         |
| `owners[].vessels[].registration_number` | string        | Matrícula legal única                            | `[SPEC FR-003]`         |
| `owners[].vessels[].vessel_type`         | string (enum) | Tipo de embarcación (`"LANCHA"`, `"YATE"`, etc.) | `[SPEC FR-003]`         |
| `owners[].vessels[].operational_status`  | string (enum) | Estado operativo actual                          | `[SPEC FR-003]`         |
| `owners[].vessels[].is_draft`            | boolean       | `true` si es borrador, `false` si está publicada | `[SPEC FR-003]`         |

### Ejemplo JSON

```json
{
  "pagination": {
    "page": 1,
    "size": 20,
    "total_elements": 1,
    "total_pages": 1
  },
  "owners": [
    {
      "owner_id": "a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d",
      "full_name": "Inversiones Náuticas del Caribe S.A.S.",
      "email": "contacto@nauticascaribe.com",
      "document_number": "900123456-1",
      "vessels_count": 2,
      "vessels": [
        {
          "vessel_id": "3f2c1a54-8b3e-4d7a-9c10-5a2b7e6f1d01",
          "name": "Yate Tayrona Sea Breeze",
          "registration_number": "CP-04-2021-0892",
          "vessel_type": "YATE",
          "operational_status": "DISPONIBLE",
          "is_draft": false
        },
        {
          "vessel_id": "8e7f6a5b-4c3d-2e1f-0a9b-8c7d6e5f4a3b",
          "name": "Catamarán Sol del Caribe",
          "registration_number": "CP-04-2023-1102",
          "vessel_type": "CATAMARAN",
          "operational_status": "MANTENIMIENTO",
          "is_draft": false
        }
      ]
    }
  ]
}
```

---

## 5. Respuestas de Error

| HTTP  | `code`             | Cuándo ocurre                                                      | `retryable` | Origen                  | Fila Tabla |
| ----- | ------------------ | ------------------------------------------------------------------ | ----------- | ----------------------- | ---------- |
| `400` | `VALIDATION_ERROR` | Parámetros de consulta o UUID con formato inválido                 | No          | `[CONV]`                | E4         |
| `401` | `UNAUTHENTICATED`  | Token JWT ausente, expirado o con firma inválida                   | No          | `[PEND]`                | E1         |
| `403` | `FORBIDDEN`        | El usuario autenticado no tiene rol `ADMIN`                        | No          | `[SPEC FR-001]`         | E2         |
| `404` | `OWNER_NOT_FOUND`  | Se filtró por un `owner_id` específico que no existe en el sistema | No          | `[SPEC casos de borde]` | E5         |
| `500` | `INTERNAL_ERROR`   | Error imprevisto o fallo en la base de datos                       | Sí          | `[CONV]`                | E9         |

### Ejemplo Error (403 Forbidden)

```json
{
  "type": "about:blank",
  "title": "Acceso restringido",
  "status": 403,
  "detail": "Solo los usuarios con rol ADMINISTRADOR pueden realizar la revisión de propietarios.",
  "code": "FORBIDDEN",
  "retryable": false
}
```

---

## 6. Idempotencia y Reintentos

- **Idempotente**: Sí (operación de lectura `GET` pura).
- **Estrategia Frontend**: Ante errores de red o códigos `500`, el panel administrativo puede reintentar la solicitud de forma segura `[CONV]`.

---

## 7. Trazabilidad

`HU1` · `FR-001`, `FR-002`, `FR-003` · `SC-001` · `Casos de borde`.
