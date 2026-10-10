# Implementation Plan: UC04 - Consultar Catálogo de Embarcaciones

**Date**: 2026-10-10  
**Spec**: `docs/features/004-consultar-catalogo-embarcaciones/2-technical/spec.md`  
**Contract**: `docs/architecture/contracts/rest/UC04-consultar-catalogo.md`
 
---

## Summary

UC04 es el caso de uso por el cual los clientes navegantes o usuarios de la plataforma consultan de forma pública y paginada el catálogo de embarcaciones disponibles (`is_draft = false`, `operational_status = AVAILABLE`, `is_deleted = false`). El catálogo permite filtrar por criterios de búsqueda combinables: rango de capacidad de pasajeros (`min_capacity`, `max_capacity`), tipo de embarcación (`vessel_type`), rango de tarifa base diaria en COP (`min_rate`, `max_rate`), puerto/ubicación base (`port_name`) y disponibilidad por fechas.

Enfoque técnico: servicio backend Spring Boot 4.1.1 bajo Arquitectura Hexagonal de tres capas (`domain` / `application` / `infrastructure`) en el paquete base `com.seashare.seasharem1`. La consulta se expone a través del endpoint público `GET /api/v1/fleet/vessels` soportando paginación (`page`, `size`), ordenamiento (`sort_by`, `direction`) y un objeto DTO de criterios de búsqueda. La persistencia utiliza JPA Specifications sobre MySQL 8 para construir consultas dinámicas eficientes con índices en `operational_status`, `vessel_type` y `base_rate_cop`.

### Trazabilidad RF/RNF/HU -> Componente / Tarea

| Requisito | Componente (rutas en §Project Structure) | Tareas | Prueba |
|---|---|---|---|
| FR-001 Acceso público sin autenticación requerida | SecurityConfig, SearchVesselsController | T002, T008 | T010, T012 |
| FR-002 Exclusión estricta de borradores y eliminadas | VesselSpecification, SpringDataVesselRepository | T004, T005 | T006, T009 |
| FR-003 Filtrado dinámico multi-criterio | VesselSearchCriteria, VesselSpecification | T004, T005 | T006, T009 |
| FR-004 Integración de disponibilidad por fechas | ReservationIntegrationPort, SearchVesselsService | T006, T007 | T009, T011 |
| FR-005 Paginación y ordenamiento estandarizado | SearchVesselsController, Pageable Spring Data | T008 | T010, T012 |
| RNF-001 Respuesta de alto rendimiento con índices | MySQL 8 DB Schema (V1__init_vessel_schema.sql) | T001, T005 | T011, T012 |
 
---

## Technical Context

Language/Version: Java 21 LTS (`java.version` 21)  
Primary Dependencies: Spring Boot 4.1.1 (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`), Flyway Migration, MapStruct, Lombok, Testcontainers (MySQL 8), ArchUnit.  
Storage: MySQL 8. Consultas optimizadas mediante índices compuestos en `(operational_status, is_draft, is_deleted)`.  
Testing: JUnit 5, AssertJ, Mockito, MockMvc, Testcontainers (MySQL 8), ArchUnit.  
Target Platform: Contenedores Docker (Linux).  
Performance Goals: Tiempo de respuesta < 100 ms para consultas paginadas (`size` <= 20).  
Constraints:
- Solo retornar embarcaciones en estado `AVAILABLE` (`is_draft = false` e `is_deleted = false`).
- Paginación obligatoria por defecto (`page = 0`, `size = 10`, `max_size = 50`).
- No expone datos sensibles del propietario.

---

## Project Structure

src/main/java/com/seashare/seasharem1/
├── domain/
│   ├── model/
│   │   └── Vessel.java                   # Entidad Raíz de Agregado [T004]
│   └── valueobject/
│       └── VesselSearchCriteria.java     # VO con filtros combinables [T004]
│
├── application/
│   ├── port/in/
│   │   └── SearchVesselsUseCase.java     # Puerto de Entrada UC04 [T006]
│   ├── port/out/
│   │   ├── VesselRepositoryPort.java     # Puerto de Salida Persistencia [T006]
│   │   └── ReservationIntegrationPort.java # Consulta de ocupación por fechas [T006]
│   ├── service/
│   │   └── SearchVesselsService.java     # Servicio de Aplicación `@Transactional(readOnly = true)` [T007]
│   └── dto/
│       ├── VesselSearchQuery.java        # Query DTO con filtros y paginación [T006]
│       └── VesselSearchResult.java       # Result DTO paginado [T006]
│
└── infrastructure/
├── adapter/in/web/
│   ├── SearchVesselsController.java  # REST Controller GET /api/v1/fleet/vessels [T008]
│   └── dto/
│       ├── VesselSummaryResponseDTO.java # DTO resumen de catálogo [T008]
│       └── PagedCatalogResponseDTO.java  # DTO con metadata de paginación [T008]
└── adapter/out/persistence/
├── VesselSpecification.java      # JPA Specifications para filtros dinámicos [T005]
├── VesselRepositoryAdapter.java  # Implementación de búsquedas paginadas [T005]
└── VesselMapper.java             # MapStruct Dominio <-> JPA <-> DTO [T007]
 
---

## Reglas de Negocio

1. Visibilidad Pública: Endpoint de libre acceso sin token JWT requerido.
2. Filtro Invariable de Estado: Toda consulta aplica implícitamente `is_draft = false AND operational_status = 'AVAILABLE' AND is_deleted = false`.
3. Filtros Opcionales Combinables:
    - `vessel_type`: Filtra por tipo de embarcación (`MOTORBOAT`, `SAILBOAT`, `CATAMARAN`, `YACHT`).
    - `min_capacity` / `max_capacity`: Rango de pasajeros admitidos.
    - `min_rate` / `max_rate`: Rango de tarifa base diaria en COP.
    - `port_name`: Búsqueda parcial (`LIKE %port_name%`) insensible a mayúsculas sobre la ubicación base.
    - `start_date` / `end_date`: Consulta al puerto de integración con Reservas para excluir embarcaciones con reservas confirmadas o bloqueos en el rango.
4. Paginación y Ordenamiento:
    - Parámetros: `page` (default `0`), `size` (default `10`, máx `50`).
    - Campos de ordenamiento permitidos: `base_rate_cop`, `max_capacity`, `created_at`, `name`. Dirección: `ASC` o `DESC`.

---

## Contrato HTTP

### Consultar Catálogo
GET /api/v1/fleet/vessels

Query Parameters:
- `vessel_type` (opcional): YACHT
- `min_capacity` (opcional): 10
- `max_capacity` (opcional): 30
- `min_rate` (opcional): 500000.00
- `max_rate` (opcional): 2000000.00
- `port_name` (opcional): Santa Marta
- `start_date` (opcional): 2026-12-01
- `end_date` (opcional): 2026-12-05
- `page` (opcional, default 0): 0
- `size` (opcional, default 10): 10
- `sort_by` (opcional, default base_rate_cop): base_rate_cop
- `direction` (opcional, default ASC): ASC

Response 200 OK:
{
"content": [
{
"vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
"name": "Yate Tayrona Sea Breeze",
"vessel_type": "YACHT",
"max_capacity": 30,
"base_rate_cop": 1800000.00,
"berth_port_name": "Marina Internacional de Santa Marta",
"photo_url": "/uploads/embarcaciones/d3b07384-yate.jpg",
"rating": 4.8
}
],
"page_number": 0,
"page_size": 10,
"total_elements": 1,
"total_pages": 1,
"is_last": true
}

Errores Posibles:
- 400 Bad Request: Parámetros de rango inválidos (ej. `min_rate > max_rate` o `end_date < start_date`).
- 500 Internal Error: Fallo de base de datos o indisponibilidad del servicio de consultas.

---

## Discrepancias y Preguntas Abiertas

| ID | Descripción | Decisión para Avanzar |
|---|---|---|
| D-UC04-01 | Búsqueda por fechas sin servicio M3 | Si `start_date` y `end_date` son provistos pero el servicio de Reservas no responde, se aplica un degradado elegante (graceful degradation) respondiendo el catálogo con una advertencia en la cabecera HTTP. |
| D-UC04-02 | Límite Máximo de Tamaño de Página | Se fuerza un techo duro de `size = 50` para evitar saturación de memoria en consultas masivas. |
 
---

## Implementation Phases

### Phase 1: Specifications & Persistence Layer
- [ ] T001: Asegurar la presencia de índices en MySQL (`idx_vessel_status_draft` en `operational_status`, `is_draft`, `is_deleted`).
- [ ] T002: Configurar `SecurityConfig` para permitir acceso anónimo a `GET /api/v1/fleet/vessels`.
- [ ] T003: Crear el Value Object `VesselSearchCriteria` encapsulado en la capa de dominio.
- [ ] T004: Crear `VesselSpecification.java` utilizando Spring Data JPA Criteria API para armar predicados dinámicos.
- [ ] T005: Extender `VesselRepositoryPort` e implementar las consultas paginadas en `VesselRepositoryAdapter`.

### Phase 2: Application Service & Integration
- [ ] T006: Crear `VesselSearchQuery`, `VesselSearchResult` y la interfaz del puerto de entrada `SearchVesselsUseCase`.
- [ ] T007: Implementar `SearchVesselsService` (`@Transactional(readOnly = true)`):
    1. Validar consistencia de los rangos (lanzar `DomainException` si `min > max`).
    2. Consultar `ReservationIntegrationPort` si existen fechas de filtro para obtener lista de `vessel_ids` ocupados.
    3. Invocar `VesselRepositoryPort` enviando la especificación JPA y los datos de paginación.
    4. Mapear resultados a DTOs de salida.

### Phase 3: Infrastructure REST & E2E Testing
- [ ] T008: Implementar `SearchVesselsController` para atender `GET /api/v1/fleet/vessels`.
- [ ] T009: Pruebas unitarias de las especificaciones y del servicio (`SearchVesselsServiceTest`).
- [ ] T010: Pruebas de contrato MockMvc para el controlador (`SearchVesselsControllerTest`).
- [ ] T011: Pruebas de integración de extremo a extremo (E2E) con Testcontainers (MySQL 8) validando rendimiento, paginación y filtros combinados.