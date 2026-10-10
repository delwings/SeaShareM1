# UC08 — Consultar Tarifa Base (Internal REST)

| Campo                    | Valor                                                           |
| ------------------------ | --------------------------------------------------------------- |
| **Caso de uso**          | UC08 Consultar Tarifa Base                                      |
| **SPEC**                 | `docs/features/008-consultar-tarifa-base/1-functional/spec.md`  |
| **Dirección**            | Sistema Financiero (Módulo 3) → Módulo 1 (Gestión de Flota)     |
| **¿Responde?**           | Sí (síncrono)                                                   |
| **Quién puede llamarlo** | Exclusivamente el Módulo 3 (Sistema Financiero) `[SPEC FR-001]` |
| **Efectos secundarios**  | Ninguno (operación de solo lectura / idempotente)               |

---

## 1. Propósito

Permite al **Sistema Financiero (Módulo 3)** consultar en tiempo real y en lote las tarifas base vigentes por hora (en COP) de una o varias embarcaciones `[SPEC FR-001, FR-002]`. M1 actúa como la fuente autoritativa del precio base fijado por el propietario, el cual es utilizado por M3 para aplicar tarifas dinámicas y procesar estimaciones monetarias `[SPEC HU1]`.

---

## 2. Petición HTTP

`POST /internal/v1/fleet/base-rates` `[CONV]` _(Se utiliza POST para aceptar un listado de IDs en el cuerpo de la petición, optimizando las consultas en lote y evitando restricciones de longitud en la URL)_.

### Headers

| Header                     | Obligatorio | Valor                                        | Origen   |
| -------------------------- | ----------- | -------------------------------------------- | -------- |
| `X-Internal-Service-Token` | Sí          | Token de servicio interno backend-to-backend | `[PEND]` |
| `Content-Type`             | Sí          | `application/json`                           | `[CONV]` |
| `Accept`                   | No          | `application/json`                           | `[CONV]` |
| `X-Correlation-Id`         | No          | Cadena libre para trazabilidad de logs       | `[CONV]` |

### Cuerpo de la Petición (Request Body)

| Campo        | Tipo             | Oblig. | Descripción                                                                      | Origen           |
| ------------ | ---------------- | ------ | -------------------------------------------------------------------------------- | ---------------- |
| `vessel_ids` | array de UUID v4 | Sí     | Listado de identificadores de las embarcaciones a consultar (máximo 50 por lote) | `[SPEC RNF-001]` |

```json
{
  "vessel_ids": [
    "3f2c1a54-8b3e-4d7a-9c10-5a2b7e6f1d01",
    "9c10-5a2b7e6f1d01-3f2c1a54-8b3e-4d7a"
  ]
}
```

---

## 3. Reglas de Procesamiento

1. **Autenticación Interna**: Se valida el header `X-Internal-Service-Token`. Si no es válido o está ausente, se rechaza la petición con `401 Unauthenticated` `[PEND]`.
2. **Validación del Lote**: El arreglo `vessel_ids` no puede estar vacío ni superar los 50 elementos. En caso contrario, se responde con `400 Bad Request` (`VALIDATION_ERROR`) `[SPEC RNF-003]`.
3. **Filtrado de Disponibilidad**: Para cada `vessel_id` recibido, M1 verifica que la embarcación exista, no haya sido desactivada (`is_deleted = false`) y no se encuentre en estado `DRAFT`.
4. **Manejo de Ausentes / Inactivas**: Si una embarcación no existe, está en borrador o fue eliminada, M1 **simplemente no la incluye en el arreglo de respuesta**, permitiendo que M3 identifique de forma transparente que la tarifa para ese ID "no está disponible" `[SPEC casos de borde]`.
5. **Formato Monetario Estricto**: Las tarifas devueltas en `base_rate` se expresan como un string decimal con dos decimales (ej. `"350000.00"`). Backend procesa con `BigDecimal` `[SPEC RNF-002]`.
6. **SLA de Rendimiento**: M1 debe responder la consulta en menos de 500 ms para un lote de hasta 50 embarcaciones `[SPEC RNF-003]`.

---

## 4. Respuesta Exitosa

`200 OK` — `Content-Type: application/json`

| Campo               | Tipo           | Descripción                                               | Origen                    |
| ------------------- | -------------- | --------------------------------------------------------- | ------------------------- |
| `rates`             | array objetos  | Lista de tarifas devueltas para las embarcaciones activas | `[SPEC RNF-001]`          |
| `rates[].vessel_id` | UUID v4        | Identificador de la embarcación                           | `[SPEC RNF-001]`          |
| `rates[].base_rate` | string decimal | Tarifa base por hora en COP (exacta y positiva)           | `[SPEC RNF-001, RNF-002]` |

### Ejemplo JSON

```json
{
  "rates": [
    {
      "vessel_id": "3f2c1a54-8b3e-4d7a-9c10-5a2b7e6f1d01",
      "base_rate": "350000.00"
    },
    {
      "vessel_id": "9c10-5a2b7e6f1d01-3f2c1a54-8b3e-4d7a",
      "base_rate": "180000.00"
    }
  ]
}
```

---

## 5. Respuestas de Error

| HTTP  | `code`             | Cuándo ocurre                                                                           | `retryable` | Origen           | Fila Tabla |
| ----- | ------------------ | --------------------------------------------------------------------------------------- | ----------- | ---------------- | ---------- |
| `400` | `VALIDATION_ERROR` | El cuerpo de la petición no incluye `vessel_ids`, viene vacío o supera los 50 elementos | No          | `[SPEC RNF-003]` | E4         |
| `401` | `UNAUTHENTICATED`  | Token de servicio interno ausente o inválido                                            | No          | `[PEND]`         | E1         |
| `500` | `INTERNAL_ERROR`   | Error no previsto o fallo de conectividad con la BD                                     | Sí          | `[CONV]`         | E9         |

### Ejemplo Error (400 Bad Request)

```json
{
  "type": "about:blank",
  "title": "Error de validación",
  "status": 400,
  "detail": "El listado de embarcaciones a consultar no puede estar vacío ni superar los 50 elementos.",
  "code": "VALIDATION_ERROR",
  "retryable": false
}
```

---

## 6. Idempotencia y Reintentos

- **Idempotente**: Sí (operación de consulta `POST` puramente de lectura).
- **Estrategia M3**: Si M3 experimenta un error `500` o un _timeout_, puede reintentar la petición de forma segura aplicando _exponential backoff_ `[SPEC casos de borde]`.

---

## 7. Trazabilidad

`HU1` · `FR-001`, `FR-002` · `RNF-001`, `RNF-002`, `RNF-003` · `Casos de borde`.
