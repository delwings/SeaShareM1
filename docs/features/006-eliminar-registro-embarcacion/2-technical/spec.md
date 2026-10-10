# Implementation Plan: UC06 - Dar de Baja Embarcación (Eliminación Lógica)

**Date**: 2026-10-10  
**Spec**: `docs/features/006-dar-de-baja-embarcacion/2-technical/spec.md`  
**Contract**: `docs/architecture/contracts/rest/UC06-dar-de-baja.md`
 
---

## Summary

UC06 es el caso de uso por el cual un **Propietario** (rol `OWNER`) retira de circulación una embarcación previamente registrada y publicada (`is_draft = false`). A diferencia de la cancelación de borrador (UC02), dar de baja una embarcación activa aplica un **borrado lógico (Soft Delete)** estableciendo `is_deleted = true`, `operational_status = OUT_OF_SERVICE` e inhabilitando su aparición en búsquedas del catálogo (UC04) o nuevas reservas.

Enfoque técnico: servicio backend Spring Boot 4.1.1 bajo Arquitectura Hexagonal de tres capas (`domain` / `application` / `infrastructure`) en el paquete base `com.seashare.seasharem1`. Se expone mediante el endpoint `DELETE /api/v1/fleet/vessels/{vessel_id}`. Antes de aplicar la baja lógica, el servicio consulta al puerto de salida de integración con Módulo 3 (`ReservationIntegrationPort`) para validar que la embarcación no posea reservas activas o futuras confirmadas. Si existen reservas vigentes, se rechaza la solicitud con `409 Conflict`. La respuesta exitosa retorna `204 No Content`.

### Trazabilidad RF/RNF/HU -> Componente / Tarea

| Requisito | Componente (rutas en §Project Structure) | Tareas | Prueba |
|---|---|---|---|
| FR-001 Rol exclusivo OWNER | JwtSecurityFilter, DeactivateVesselController | T002, T008 | T010, T012 |
| FR-002 Verificación de propiedad (ownership check) | DeactivateVesselService, VesselRepositoryPort | T006, T007 | T009, T010 |
| FR-003 Validación de reservas activas pendientes | ReservationIntegrationPort, DeactivateVesselService | T004, T007 | T009, T011 |
| FR-004 Aplicación de eliminación lógica (Soft Delete) | Vessel (Domain Entity), VesselRepositoryAdapter | T003, T007 | T005, T011 |
| RNF-001 Respuesta idempotente 204 No Content | DeactivateVesselController | T008 | T010, T012 |
 
---

## Technical Context

Language/Version: Java 21 LTS (`java.version` 21)  
Primary Dependencies: Spring Boot 4.1.1 (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-security`), Flyway Migration, MapStruct, Lombok, Testcontainers (MySQL 8), ArchUnit.  
Storage: MySQL 8 (Persistencia relacional).  
Testing: JUnit 5, AssertJ, Mockito, MockMvc, Testcontainers (MySQL 8), ArchUnit.  
Target Platform: Contenedores Docker (Linux).  
Performance Goals: Tiempo de procesamiento < 200 ms.  
Constraints:
- NUNCA realizar borrado físico (`DELETE`) sobre embarcaciones publicadas; aplicar `is_deleted = true`.
- Mantiene la trazabilidad histórica de reservas previas en el sistema.
- Impedir la baja si existen reservas activas/confirmadas en el Módulo 3.

---

## Project Structure

src/main/java/com/seashare/seasharem1/
├── domain/
│   ├── model/
│   │   └── Vessel.java                   # Contiene método markAsDeleted() [T003]
│   └── exception/
│       ├── DomainException.java          # [T001]
│       ├── VesselNotFoundException.java  # Mapea a 404 Not Found [T001]
│       ├── UnauthorizedOwnershipException.java # Mapea a 403 Forbidden [T001]
│       └── ActiveReservationConflictException.java # Mapea a 409 Conflict [T001]
│
├── application/
│   ├── port/in/
│   │   └── DeactivateVesselUseCase.java  # Puerto de Entrada UC06 [T006]
│   ├── port/out/
│   │   ├── VesselRepositoryPort.java     # Puerto de Salida Persistencia [T005]
│   │   └── ReservationIntegrationPort.java # Consulta de reservas activas [T004]
│   ├── service/
│   │   └── DeactivateVesselService.java # Servicio `@Transactional` [T007]
│   └── dto/
│       └── DeactivateVesselCommand.java # Command DTO (vesselId, ownerId) [T006]
│
└── infrastructure/
├── adapter/in/web/
│   ├── DeactivateVesselController.java # REST Controller DELETE /{vessel_id} [T008]
│   └── GlobalExceptionHandler.java   # Mapeador RFC 9457 Problem Details [T002]
└── adapter/out/persistence/
├── VesselRepositoryAdapter.java  # Persistencia JPA [T005]
└── VesselMapper.java             # MapStruct Dominio <-> JPA [T007]
 
---

## Reglas de Negocio

1. Autenticación y Rol: Exclusivamente accesible por usuarios autenticados con rol `OWNER`.
2. Verificación de Propiedad: El `owner_id` del token JWT debe coincidir con el dueño de la embarcación (`403 Forbidden` si difiere).
3. Existencia del Recurso: Si la embarcación no existe o ya posee `is_deleted = true`, retorna `404 Not Found` (`VESSEL_NOT_FOUND`).
4. Rechazo por Reservas Activas: Se consulta a `ReservationIntegrationPort`. Si existen reservas futuras o en curso con estado `CONFIRMED` o `IN_PROGRESS`, se aborta la operación retornando `409 Conflict` (`ACTIVE_RESERVATIONS_EXIST`).
5. Borrado Lógico (Soft Delete): Se actualizan los atributos `is_deleted = true`, `operational_status = OUT_OF_SERVICE` y `deleted_at = LocalDateTime.now()`. La embarcación deja de aparecer inmediatamente en el catálogo.

---

## Contrato HTTP

### Dar de Baja Embarcación
DELETE /api/v1/fleet/vessels/{vessel_id}

Headers:
- Authorization: Bearer <JWT_TOKEN>

Path Variables:
- `vessel_id`: UUID v4 (Identificador único de la embarcación)

Response 204 No Content

Errores Posibles:
- 401 Unauthenticated: Token de autenticación ausente o expirado.
- 403 Forbidden: El usuario autenticado no es el propietario de la embarcación.
- 404 Not Found: La embarcación no existe o ya fue eliminada previamente.
- 409 Conflict: La embarcación no se puede dar de baja porque tiene reservas activas confirmadas.
- 500 Internal Error: Error interno de servidor o falla en la base de datos.

---

## Discrepancias y Preguntas Abiertas

| ID | Descripción | Decisión para Avanzar |
|---|---|---|
| D-UC06-01 | Reutilización de Matrícula tras Soft Delete | Una embarcación en borrado lógico conserva su matrícula en el histórico para evitar suplantaciones o incongruencias contractuales previas. |
| D-UC06-02 | Liberación de Amarre/Puerto | Al dar de baja, la embarcación no libera el historial de ubicación base para fines estadísticos. |
 
---

## Implementation Phases

### Phase 1: Domain Logic & Outbound Ports
- [ ] T001: Crear excepciones de dominio (`ActiveReservationConflictException`, `UnauthorizedOwnershipException`).
- [ ] T002: Configurar mapeos en `GlobalExceptionHandler` para RFC 9457 Problem Details (`409 Conflict`).
- [ ] T003: Implementar regla de negocio `Vessel.markAsDeleted()` en la entidad de dominio.
- [ ] T004: Definir `ReservationIntegrationPort.hasActiveReservations(UUID vesselId)` para consultar compromisos en M3.

### Phase 2: Application Service Layer
- [ ] T005: Extender `VesselRepositoryPort` para soportar actualizaciones de estado de borrado lógico.
- [ ] T006: Definir `DeactivateVesselCommand` y la interfaz del puerto de entrada `DeactivateVesselUseCase`.
- [ ] T007: Implementar `DeactivateVesselService` (`@Transactional`):
    1. Recuperar la embarcación por `vessel_id` (Lanza `VesselNotFoundException` si no existe o `is_deleted == true`).
    2. Verificar propiedad con `owner_id` (Lanza `UnauthorizedOwnershipException` si no es el dueño).
    3. Consultar `ReservationIntegrationPort` (Lanza `ActiveReservationConflictException` si hay reservas activas).
    4. Ejecutar `Vessel.markAsDeleted()`.
    5. Guardar en BD mediante `VesselRepositoryPort`.

### Phase 3: Infrastructure REST & E2E Testing
- [ ] T008: Implementar `DeactivateVesselController` para atender `DELETE /api/v1/fleet/vessels/{vessel_id}` retornando `204 No Content`.
- [ ] T009: Pruebas unitarias con Mockito (`DeactivateVesselServiceTest`).
- [ ] T010: Pruebas de contrato MockMvc (`DeactivateVesselControllerTest`).
- [ ] T011: Pruebas de integración de extremo a extremo (E2E) con Testcontainers (MySQL 8), validando el conflicto por reservas y la persistencia de `is_deleted = true`.