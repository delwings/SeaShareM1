# Implementation Plan: UC08 - Cambiar Estado Operativo de Embarcación

**Date**: 2026-10-10  
**Spec**: `docs/features/008-cambiar-estado-operativo/2-technical/spec.md`  
**Contract**: `docs/architecture/contracts/rest/UC08-cambiar-estado-operativo.md`
 
---

## Summary

UC08 es el caso de uso por el cual un **Propietario** (rol `OWNER`) actualiza manualmente el estado operativo de una de sus embarcaciones publicadas (`is_draft = false`). Los estados operativos permitidos para transición manual son `AVAILABLE` (Disponible para reservas) y `MAINTENANCE` (En mantenimiento / fuera de servicio temporal). El estado `NAVIGATION` es gestionado automáticamente por el Módulo de Operaciones/Reservas (M3) durante un viaje activo.

Enfoque técnico: servicio backend Spring Boot 4.1.1 bajo Arquitectura Hexagonal de tres capas (`domain` / `application` / `infrastructure`) en el paquete base `com.seashare.seasharem1`. La operación se expone mediante el endpoint `PATCH /api/v1/fleet/vessels/{vessel_id}/status`. Antes de transicionar a `MAINTENANCE`, el servicio valida mediante `ReservationIntegrationPort` que la embarcación no tenga reservas confirmadas en las fechas del bloqueo. La respuesta exitosa retorna la entidad con el nuevo estado actualizado.

### Trazabilidad RF/RNF/HU -> Componente / Tarea

| Requisito | Componente (rutas en §Project Structure) | Tareas | Prueba |
|---|---|---|---|
| FR-001 Rol exclusivo OWNER | JwtSecurityFilter, ChangeVesselStatusController | T002, T007 | T009, T011 |
| FR-002 Verificación de propiedad (ownership check) | ChangeVesselStatusService, VesselRepositoryPort | T005, T006 | T008, T009 |
| FR-003 Transiciones válidas de estado operativo | OperationalStatus, Vessel (Domain Entity) | T003, T004 | T004, T008 |
| FR-004 Validación de reservas al pasar a Mantenimiento | ReservationIntegrationPort, ChangeVesselStatusService | T005, T006 | T008, T010 |
| RNF-001 Actualización atómica con tiempo de respuesta bajo | VesselRepositoryAdapter | T005 | T010, T011 |
 
---

## Technical Context

Language/Version: Java 21 LTS (`java.version` 21)  
Primary Dependencies: Spring Boot 4.1.1 (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-security`), Flyway Migration, MapStruct, Lombok, Testcontainers (MySQL 8), ArchUnit.  
Storage: MySQL 8 (Persistencia relacional).  
Testing: JUnit 5, AssertJ, Mockito, MockMvc, Testcontainers (MySQL 8), ArchUnit.  
Target Platform: Contenedores Docker (Linux).  
Performance Goals: Tiempo de procesamiento < 150 ms.  
Constraints:
- Rechazar transiciones desde/hacia `DRAFT` mediante este endpoint (usar UC01/UC02).
- Rechazar cambio a `MAINTENANCE` si existen reservas activas o confirmadas en M3.
- No permitir cambio manual si la embarcación está en estado `NAVIGATION`.

---

## Project Structure

src/main/java/com/seashare/seasharem1/
├── domain/
│   ├── model/
│   │   └── Vessel.java                   # Regla changeOperationalStatus() [T004]
│   ├── valueobject/
│   │   └── OperationalStatus.java        # Enum con máquina de estados [T003]
│   └── exception/
│       ├── DomainException.java          # [T001]
│       ├── VesselNotFoundException.java  # Mapea a 404 Not Found [T001]
│       ├── InvalidStatusTransitionException.java # Mapea a 409 Conflict [T001]
│       └── ActiveReservationConflictException.java # Mapea a 409 Conflict [T001]
│
├── application/
│   ├── port/in/
│   │   └── ChangeVesselStatusUseCase.java # Puerto de Entrada UC08 [T005]
│   ├── port/out/
│   │   ├── VesselRepositoryPort.java     # Puerto de Salida Persistencia [T005]
│   │   └── ReservationIntegrationPort.java # Consulta de reservas activas [T005]
│   ├── service/
│   │   └── ChangeVesselStatusService.java # Servicio `@Transactional` [T006]
│   └── dto/
│       ├── ChangeVesselStatusCommand.java # Command DTO (vesselId, ownerId, newStatus) [T005]
│       └── VesselStatusChangedResult.java # Result DTO [T005]
│
└── infrastructure/
├── adapter/in/web/
│   ├── ChangeVesselStatusController.java # REST Controller PATCH /{id}/status [T007]
│   └── dto/
│       ├── PatchVesselStatusRequestDTO.java  # Request DTO [T007]
│       └── PatchVesselStatusResponseDTO.java # Response DTO [T007]
└── adapter/out/persistence/
├── VesselRepositoryAdapter.java  # Persistencia JPA [T005]
└── VesselMapper.java             # MapStruct Dominio <-> JPA <-> DTO [T006]
 
---

## Reglas de Negocio

1. Autenticación y Propiedad: Solo el propietario (`owner_id` del token JWT) puede modificar el estado operativo de la embarcación (`403 Forbidden` si difiere).
2. Prohibición en Borradores: Si `is_draft = true` o `is_deleted = true`, el sistema retorna `404 Not Found` o `409 Conflict`.
3. Máquina de Estados Permitida:
    - `AVAILABLE` -> `MAINTENANCE` (Permitido si no hay reservas confirmadas).
    - `MAINTENANCE` -> `AVAILABLE` (Permitido siempre).
    - Transiciones prohibidas manualmente: Hacia/desde `DRAFT` o `NAVIGATION` (`409 Conflict - INVALID_STATUS_TRANSITION`).
4. Conflicto con Reservas: Si se solicita pasar a `MAINTENANCE` y existen reservas en estado `CONFIRMED` en M3, se rechaza la operación con `409 Conflict` (`ACTIVE_RESERVATIONS_EXIST`).

---

## Contrato HTTP

### Cambiar Estado Operativo
PATCH /api/v1/fleet/vessels/{vessel_id}/status

Headers:
- Authorization: Bearer <JWT_TOKEN>
- Content-Type: application/json

Path Variables:
- `vessel_id`: UUID v4 (Identificador único de la embarcación)

Request Body:
{
"target_status": "MAINTENANCE",
"reason": "Mantenimiento preventivo de motores"
}

Response 200 OK:
{
"vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
"name": "Yate Tayrona Sea Breeze",
"previous_status": "AVAILABLE",
"current_status": "MAINTENANCE",
"updated_at": "2026-10-10T15:00:00Z"
}

Errores Posibles:
- 400 Bad Request: Valor de `target_status` no reconocido o inválido.
- 401 Unauthenticated: Token ausente o expirado.
- 403 Forbidden: El usuario autenticado no es el propietario de la embarcación.
- 404 Not Found: Embarcación no encontrada o eliminada.
- 409 Conflict: Transición de estado no permitida o presencia de reservas activas confirmadas.

---

## Discrepancias y Preguntas Abiertas

| ID | Descripción | Decisión para Avanzar |
|---|---|---|
| D-UC08-01 | Campo de Razón de Mantenimiento | El campo `reason` es opcional en el request y se registra en los logs de auditoría sin alterar el esquema principal de la tabla `vessel`. |
 
---

## Implementation Phases

### Phase 1: Domain State Machine & Exceptions
- [ ] T001: Crear excepciones `InvalidStatusTransitionException` y `ActiveReservationConflictException`.
- [ ] T002: Configurar mapeos en `GlobalExceptionHandler` para respuestas RFC 9457 (`409 Conflict`).
- [ ] T003: Definir en `OperationalStatus` los métodos de validación de transición `boolean canTransitionTo(OperationalStatus target)`.
- [ ] T004: Implementar método `changeOperationalStatus(OperationalStatus target)` en la entidad `Vessel` con pruebas unitarias (`VesselStatusTest`).

### Phase 2: Application Service Layer
- [ ] T005: Definir `ChangeVesselStatusCommand`, `VesselStatusChangedResult` y el puerto `ChangeVesselStatusUseCase`.
- [ ] T006: Implementar `ChangeVesselStatusService` (`@Transactional`):
    1. Obtener la embarcación por `vessel_id`.
    2. Validar propiedad con `owner_id`.
    3. Si el estado destino es `MAINTENANCE`, consultar `ReservationIntegrationPort`.
    4. Invocar `Vessel.changeOperationalStatus(target_status)`.
    5. Persistir cambios en BD mediante `VesselRepositoryPort`.

### Phase 3: Infrastructure REST & E2E Testing
- [ ] T007: Implementar `ChangeVesselStatusController` para atender `PATCH /api/v1/fleet/vessels/{vessel_id}/status`.
- [ ] T008: Pruebas unitarias del servicio con Mockito (`ChangeVesselStatusServiceTest`).
- [ ] T009: Pruebas de contrato MockMvc (`ChangeVesselStatusControllerTest`).
- [ ] T010: Pruebas de integración E2E con Testcontainers (MySQL 8) validando máquinas de estado y conflictos por reservas.