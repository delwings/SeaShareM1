# Implementation Plan: UC02 - Cancelar Registro de Embarcación

**Date**: 2026-10-10  
**Spec**: `docs/features/002-cancelar-registro-embarcacion/2-technical/spec.md`  
**Contract**: `docs/architecture/contracts/rest/UC02-cancelar-registro.md`[cite: 3]
 
---

## Summary

UC02 es el caso de uso por el cual un **Propietario** (rol `OWNER`) descarta o descarta voluntariamente un registro incompleto de embarcación que se encuentra en estado borrador (`is_draft = true`, `operational_status = DRAFT`)[cite: 3]. A diferencia de las embarcaciones publicadas activas (que aplican borrado lógico con `is_deleted = true`), la cancelación de un borrador ejecuta una **eliminación física permanente (`DELETE`)** en la base de datos MySQL de la fila en la tabla `vessel` y sus relaciones dependientes en `vessel_service`[cite: 3]. Esto asegura que no se acumulen registros huérfanos y se libere inmediatamente la matrícula legal (`legal_registration`) para un eventual registro futuro[cite: 3].

Enfoque técnico: servicio backend Spring Boot 4.1.1 bajo **Arquitectura Hexagonal de tres capas** (`domain` / `application` / `infrastructure`) en el paquete base `com.seashare.seasharem1`. La petición es expuesta como una operación HTTP idempotente `DELETE /api/v1/fleet/vessels/drafts/{vessel_id}`[cite: 3]. Se realiza la verificación estricta de propiedad (`ownership check`) para garantizar que el `owner_id` del token JWT coincida con el dueño del borrador[cite: 3]. Si la embarcación ya no está en borrador (`is_draft = false`), se rechaza con `409 Conflict` (`INVALID_STATUS_TRANSITION`)[cite: 3]. La respuesta exitosa devuelve un cuerpo vacío con código `204 No Content`[cite: 3].

### Trazabilidad RF/RNF/HU → Componente / Tarea

| Requisito | Componente (rutas en §Project Structure) | Tareas | Prueba |
|---|---|---|---|
| **FR-001** Rol exclusivo `OWNER` | `JwtSecurityFilter`, `CancelDraftController` | T002, T008 | T010, T012 |
| **FR-002** Verificación de propiedad (`ownership check`) | `CancelDraftService`, `VesselRepositoryPort` | T007, T008 | T009, T010 |
| **FR-003** Eliminación física permanente (`DELETE`) | `VesselRepositoryAdapter`, `SpringDataVesselRepository` | T004, T007 | T009, T012 |
| **FR-004** Rechazo de embarcaciones ya publicadas (`409 Conflict`) | `CancelDraftService`, `Vessel` (Domain Entity) | T005, T007 | T006, T009 |
| **RNF-001** Respuesta idempotente `204 No Content` | `CancelDraftController` | T008 | T010, T012 |
 
---

## Technical Context

**Language/Version**: Java 21 LTS (`java.version` 21)  
**Primary Dependencies**: Spring Boot 4.1.1 (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-security`), Flyway Migration, MapStruct, Lombok, Testcontainers (MySQL 8), ArchUnit.  
**Storage**: MySQL 8 (Persistencia relacional).  
**Testing**: JUnit 5, AssertJ, Mockito, MockMvc, Testcontainers (MySQL 8), ArchUnit.  
**Target Platform**: Contenedores Docker (Linux).  
**Performance Goals**: Tiempo de procesamiento $< 150 \text{ ms}$.  
**Constraints**:
* Eliminación física en BD (`DELETE CASCADE` o borrado explícito de relaciones en repositorios).
* Liberación inmediata de la restricción de unicidad de la matrícula legal (`legal_registration`).
* Operación idempotente.

---

## Project Structure

```text
src/main/java/com/seashare/seasharem1/
├── domain/
│   ├── model/
│   │   └── Vessel.java                   # Contiene regla canBeCancelled() [T005]
│   └── exception/
│       ├── DomainException.java          # Excepción base [T003]
│       ├── VesselNotFoundException.java  # Mapea a 404 Not Found [T003]
│       ├── UnauthorizedOwnershipException.java # Mapea a 403 Forbidden [T003]
│       └── InvalidStatusTransitionException.java # Mapea a 409 Conflict [T003]
│
├── application/
│   ├── port/in/
│   │   └── CancelDraftUseCase.java       # Puerto de Entrada UC02 [T007]
│   ├── port/out/
│   │   └── VesselRepositoryPort.java     # Extensión del puerto de persistencia [T004]
│   ├── service/
│   │   └── CancelDraftService.java       # Servicio de Aplicación `@Transactional` [T007]
│   └── dto/
│       └── CancelDraftCommand.java       # Command DTO (vesselId, ownerId) [T007]
│
└── infrastructure/
    ├── adapter/in/web/
    │   ├── CancelDraftController.java    # REST Controller DELETE /drafts/{id} [T008]
    │   └── GlobalExceptionHandler.java   # Mapeador RFC 9457 Problem Details [T002]
    └── adapter/out/persistence/
        ├── VesselJpaEntity.java          # Mapeo JPA [T004]
        ├── SpringDataVesselRepository.java # JpaRepository [T004]
        └── VesselRepositoryAdapter.java  # Implementación de deletePhysicalDraft() [T004]
```
 
---

## Reglas de Negocio

1. **Autenticación y Rol**: Exclusivamente accesible por usuarios autenticados con rol `OWNER`[cite: 3]. De lo contrario $\rightarrow$ Error `403 Forbidden` (`FORBIDDEN`)[cite: 3].
2. **Verificación de Propiedad (`Ownership Check`)**: El `owner_id` extraído del token JWT debe coincidir exactamente con el propietario de la embarcación[cite: 3]. Si la embarcación pertenece a otro usuario $\rightarrow$ Error `403 Forbidden` (`FORBIDDEN`)[cite: 3].
3. **Existencia del Recurso**: Se consulta la BD por el `vessel_id` proporcionado en la URL[cite: 3]. Si no se encuentra registro o tiene `is_deleted = true` $\rightarrow$ Error `404 Not Found` (`VESSEL_NOT_FOUND`)[cite: 3].
4. **Estado Borrador Requerido**:
    * Si la embarcación tiene `is_draft = true` y estado `DRAFT`, se procede con el borrado físico de la BD[cite: 3].
    * Si la embarcación ya fue publicada (`is_draft = false` o estado `AVAILABLE`) $\rightarrow$ Error `409 Conflict` (`INVALID_STATUS_TRANSITION`), indicando que debe utilizarse el proceso de eliminación lógica de activos (UC06)[cite: 3].
5. **Eliminación Física Cascaded**: Se efectúa una instrucción `DELETE` en la BD MySQL[cite: 3]. Se eliminan la fila en `vessel` y las filas dependientes en `vessel_service`[cite: 3]. La matrícula legal queda inmediatamente liberada para nuevos registros[cite: 3].

---

## Contrato HTTP

### Cancelar Borrador
`DELETE /api/v1/fleet/vessels/drafts/{vessel_id}`[cite: 3]

**Headers**:
* `Authorization`: `Bearer <JWT_TOKEN>`[cite: 3]

**Path Variables**:
* `vessel_id`: `UUID v4` (Identificador único del borrador)[cite: 3]

**Response 204 No Content**:  
Petición procesada exitosamente sin cuerpo en la respuesta[cite: 3].

**Errores Posibles**:
* `401 Unauthenticated`: Token faltante o expirado[cite: 3].
* `403 Forbidden`: El usuario no es `OWNER` o intenta borrar un borrador ajeno[cite: 3].
* `404 Not Found`: No existe un borrador con ese `vessel_id`[cite: 3].
* `409 Conflict`: La embarcación ya fue publicada (`INVALID_STATUS_TRANSITION`)[cite: 3].
* `500 Internal Error`: Fallo de conexión o infraestructura de BD[cite: 3].

---

## Discrepancias y Preguntas Abiertas

| ID | Descripción | Decisión para Avanzar |
|---|---|---|
| **D-UC02-01** | Tipo de Borrado | Borrado físico estricto (`DELETE`) para borradores sin publicar (diferente a UC06 que aplica borrado lógico)[cite: 3]. |
| **D-UC02-02** | Manejo de Archivos | Si existiese un borrador con fotografía temporal en disco, se elimina el archivo local en `/uploads/embarcaciones/`. |
 
---

## Implementation Phases

### Phase 1: Setup & Domain Enhancements
- [ ] **T001**: Verificar y actualizar el árbol de excepciones (`VesselNotFoundException`, `UnauthorizedOwnershipException`, `InvalidStatusTransitionException`).
- [ ] **T002**: Asegurar que `GlobalExceptionHandler` soporte los mapeos RFC 9457 para `403 FORBIDDEN` y `409 INVALID_STATUS_TRANSITION`[cite: 3].
- [ ] **T003**: Agregar en la entidad de dominio `Vessel` el método de regla de negocio `boolean isDraft()`.

### Phase 2: Persistence Adapter Layer
- [ ] **T004**: Definir en `VesselRepositoryPort` el método `void deletePhysicalDraft(UUID vesselId)` y su implementación en `VesselRepositoryAdapter` ejecutando `deleteById(vesselId)`.
- [ ] **T005**: Pruebas unitarias de Dominio para validar las condiciones de cancelación de borrador (`VesselTest`).

### Phase 3: Application Service Layer
- [ ] **T006**: Crear `CancelDraftCommand` (con `vesselId` y `ownerId`) y el puerto `CancelDraftUseCase`.
- [ ] **T007**: Implementar `CancelDraftService` con la secuencia:
    1. Consultar borrador por `vesselId` (Lanza `VesselNotFoundException` si no existe)[cite: 3].
    2. Verificar propiedad (`UnauthorizedOwnershipException` si `owner_id != authenticated_user_id`)[cite: 3].
    3. Validar estado borrador (`InvalidStatusTransitionException` si `is_draft == false`)[cite: 3].
    4. Invocar `VesselRepositoryPort.deletePhysicalDraft(vesselId)`[cite: 3].
- [ ] **T008**: Pruebas unitarias del servicio con Mockito (`CancelDraftServiceTest`).

### Phase 4: Infrastructure REST & E2E Testing
- [ ] **T009**: Implementar `CancelDraftController` para exponer `DELETE /api/v1/fleet/vessels/drafts/{vessel_id}` retornando `204 No Content`[cite: 3].
- [ ] **T010**: Pruebas de contrato MockMvc para el controlador (`CancelDraftControllerTest`).
- [ ] **T011**: Pruebas de integración de extremo a extremo (E2E) para UC02 con Testcontainers (MySQL 8), verificando la eliminación real de la fila en BD y la liberación de la matrícula.