# Implementation Plan: UC07 - Consultar Mis Embarcaciones (Propietario)

**Date**: 2026-10-10

**Spec**: `docs/features/007-consultar-mis-embarcaciones/2-technical/spec.md`

**Contract**: `docs/architecture/contracts/rest/UC07-consultar-mis-embarcaciones.md`
 
---

## Summary

UC07 es el caso de uso por el cual un **Propietario** (rol `OWNER`) autenticado consulta el listado paginado de todas las embarcaciones registradas bajo su propiedad. A diferencia del catálogo público (UC04), esta consulta privada devuelve tanto embarcaciones publicadas activas (`is_draft = false`, `operational_status = AVAILABLE`) como borradores no completados (`is_draft = true`, `operational_status = DRAFT`) e inactivas/mantenimiento, omitiendo únicamente las eliminadas lógicamente (`is_deleted = true`).

Enfoque técnico: servicio backend Spring Boot 4.1.1 bajo Arquitectura Hexagonal de tres capas (`domain` / `application` / `infrastructure`) en el paquete base `com.seashare.seasharem1`. La consulta se expone a través del endpoint privado `GET /api/v1/fleet/vessels/my-vessels` con paginación (`page`, `size`), ordenamiento (`sort_by`, `direction`) y un filtro opcional por estado operativo (`status` = `DRAFT`, `AVAILABLE`, `MAINTENANCE`, `NAVIGATION`). El `owner_id` se extrae de manera segura desde el token JWT en el contexto de seguridad.

### Trazabilidad RF/RNF/HU -> Componente / Tarea

| Requisito | Componente (rutas en §Project Structure) | Tareas | Prueba |

|---|---|---|---|

| FR-001 Rol exclusivo OWNER | JwtSecurityFilter, GetMyVesselsController | T002, T007 | T009, T011 |

| FR-002 Aislamiento por Propietario (`owner_id`) | GetMyVesselsService, VesselSpecification | T004, T006 | T008, T011 |

| FR-003 Inclusión de Borradores y Embarcaciones Activas | VesselSpecification, VesselRepositoryAdapter | T004, T005 | T008, T009 |

| FR-004 Filtro opcional por `operational_status` | VesselSpecification | T004 | T008, T009 |

| FR-005 Paginación y Ordenamiento Paginado | GetMyVesselsController, Pageable Spring Data | T007 | T009, T011 |

| RNF-001 Consultas eficientes con índices por propietario | MySQL 8 DB Schema (`idx_owner_vessels`) | T001, T005 | T011 |
 
---

## Technical Context

Language/Version: Java 21 LTS (`java.version` 21)

Primary Dependencies: Spring Boot 4.1.1 (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-security`), Flyway Migration, MapStruct, Lombok, Testcontainers (MySQL 8), ArchUnit.

Storage: MySQL 8. Consultas optimizadas mediante índice compuesto en `(owner_id, is_deleted, operational_status)`.

Testing: JUnit 5, AssertJ, Mockito, MockMvc, Testcontainers (MySQL 8), ArchUnit.

Target Platform: Contenedores Docker (Linux).

Performance Goals: Tiempo de respuesta < 100 ms para consultas paginadas (`size` <= 20).

Constraints:

- El `owner_id` NUNCA se recibe como parámetro en la URL/Query Body; se obtiene del JWT autenticado.

- Excluir registros con `is_deleted = true`.

- Paginación obligatoria por defecto (`page = 0`, `size = 10`, `max_size = 50`).

---

## Project Structure

src/main/java/com/seashare/seasharem1/

├── domain/

│   ├── model/

│   │   └── Vessel.java                   # Entidad Raíz de Agregado [T004]

│   └── valueobject/

│       └── OperationalStatus.java        # Enum de estados [T004]

│

├── application/

│   ├── port/in/

│   │   └── GetMyVesselsUseCase.java      # Puerto de Entrada UC07 [T005]

│   ├── port/out/

│   │   └── VesselRepositoryPort.java     # Puerto de Salida Persistencia [T005]

│   ├── service/

│   │   └── GetMyVesselsService.java      # Servicio `@Transactional(readOnly = true)` [T006]

│   └── dto/

│       ├── GetMyVesselsQuery.java        # Query DTO (ownerId, status, pageable) [T005]

│       └── PagedMyVesselsResult.java     # Result DTO paginado [T005]

│

└── infrastructure/

    ├── adapter/in/web/

    │   ├── GetMyVesselsController.java   # REST Controller GET /my-vessels [T007]

    │   └── dto/

    │       ├── MyVesselSummaryResponseDTO.java # DTO resumen de embarcación [T007]

    │       └── PagedMyVesselsResponseDTO.java  # DTO respuesta paginada [T007]

    └── adapter/out/persistence/

        ├── VesselSpecification.java      # Predicados JPA para owner_id y status [T004]

        ├── VesselRepositoryAdapter.java  # Búsqueda paginada [T005]

        └── VesselMapper.java             # MapStruct Dominio <-> JPA <-> DTO [T006]
 
---

## Reglas de Negocio

1. Autenticación Estricta: Solo accesible para usuarios autenticados con rol `OWNER` (`401 Unauthenticated` o `403 Forbidden`).

2. Aislamiento de Datos: Se filtra de forma fija por `owner_id = <authenticated_user_id>` e `is_deleted = false`.

3. Inclusión de Borradores: A diferencia del catálogo público, incluye embarcaciones con `is_draft = true` y `operational_status = DRAFT`.

4. Filtro por Estado (Opcional): Si se especifica `status` (`DRAFT`, `AVAILABLE`, `NAVIGATION`, `MAINTENANCE`), se filtra por ese valor exacto.

5. Paginación Estándar: `page` (default `0`), `size` (default `10`, máx `50`). Ordenamiento por `created_at`, `name` o `operational_status`.

---

## Contrato HTTP

### Consultar Mis Embarcaciones

GET /api/v1/fleet/vessels/my-vessels

Headers:

- Authorization: Bearer <JWT_TOKEN>

Query Parameters:

- `status` (opcional): DRAFT

- `page` (opcional, default 0): 0

- `size` (opcional, default 10): 10

- `sort_by` (opcional, default created_at): created_at

- `direction` (opcional, default DESC): DESC

Response 200 OK:

{

"content": [

    {

      "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",

      "name": "Yate Tayrona Sea Breeze",

      "registration_number": "CP-04-2021-0892",

      "vessel_type": "YACHT",

      "max_capacity": 30,

      "base_rate_cop": 1800000.00,

      "photo_url": "/uploads/embarcaciones/d3b07384-yate.jpg",

      "operational_status": "AVAILABLE",

      "is_draft": false,

      "created_at": "2026-10-10T12:00:00Z"

    },

    {

      "vessel_id": "e4c18495-e224-50de-b6e7-923f0adfd202",

      "name": "Borrador Velero Caribe",

      "registration_number": "CP-02-2026-1104",

      "vessel_type": "SAILBOAT",

      "max_capacity": 15,

      "base_rate_cop": 900000.00,

      "photo_url": null,

      "operational_status": "DRAFT",

      "is_draft": true,

      "created_at": "2026-10-10T13:30:00Z"

    }

],

"page_number": 0,

"page_size": 10,

"total_elements": 2,

"total_pages": 1,

"is_last": true

}

Errores Posibles:

- 401 Unauthenticated: Token de autenticación ausente o expirado.

- 403 Forbidden: El usuario autenticado no posee el rol `OWNER`.

- 500 Internal Error: Error interno o falla en la base de datos.

---

## Discrepancias y Preguntas Abiertas

| ID | Descripción | Decisión para Avanzar |

|---|---|---|

| D-UC07-01 | Inclusión de fotos nulas en borradores | El DTO mapea `photo_url = null` para borradores que aún no completan el Paso 2 de registro. |
 
---

## Implementation Phases

### Phase 1: Persistence & Specification Layer

- [ ] T001: Asegurar índice en MySQL `idx_owner_vessels` (`owner_id`, `is_deleted`, `operational_status`).

- [ ] T002: Configurar `SecurityConfig` para restringir `GET /api/v1/fleet/vessels/my-vessels` al rol `OWNER`.

- [ ] T003: Crear predicados en `VesselSpecification.java` para filtrar por `owner_id`, `is_deleted = false` y `operational_status` opcional.

### Phase 2: Application Service Layer

- [ ] T004: Definir `GetMyVesselsQuery`, `PagedMyVesselsResult` y el puerto `GetMyVesselsUseCase`.

- [ ] T005: Implementar `GetMyVesselsService` (`@Transactional(readOnly = true)`):

    1. Construir la especificación JPA usando `owner_id` del comando.

    2. Invocar `VesselRepositoryPort` para obtener la página.

    3. Mapear resultados a `PagedMyVesselsResult`.

- [ ] T006: Pruebas unitarias del servicio con Mockito (`GetMyVesselsServiceTest`).

### Phase 3: Infrastructure REST & E2E Testing

- [ ] T007: Implementar `GetMyVesselsController` extrayendo el `owner_id` del token JWT en el contexto de Spring Security.

- [ ] T008: Pruebas de contrato MockMvc (`GetMyVesselsControllerTest`).

- [ ] T009: Pruebas de integración E2E con Testcontainers (MySQL 8) comprobando el aislamiento estricto por propietario y la presencia de borradores.
 