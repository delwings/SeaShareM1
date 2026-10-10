# Implementation Plan: UC01 - Registrar Embarcación

**Date**: 2026-10-10  
**Spec**: `docs/features/001-registrar-embarcacion/2-technical/spec.md`[cite: 2]
**Contract**: `docs/architecture/contracts/rest/UC01-registrar-embarcacion.md`[cite: 2]
 
---

## Summary

UC01 es el caso de uso por el cual un **Propietario** (rol `OWNER`) da de alta una nueva embarcación en la plataforma en un flujo asistido de dos etapas [SPEC HU1, HU2]:
1. **Paso 1 (Creación / Reemplazo de Borrador)**: Registra los atributos comerciales iniciales (`name`, `legal_registration`, `vessel_type`, `max_capacity`, `base_rate_cop`, `berth_location` y `selected_service_ids`). La embarcación se guarda en BD con `is_draft = true` y `operational_status = DRAFT` [SPEC FR-004, FR-005]. Si el propietario ya poseía un borrador activo no completado, **se elimina físicamente el anterior** garantizando el principio de **Unicidad de Borrador por Propietario** [SPEC FR-004].
2. **Paso 2 (Completar Registro y Publicación)**: Carga la fotografía principal (multipart/form-data, JPG/PNG, $\le 10 \text{ MB}$), la almacena en el sistema de archivos local (`/uploads/embarcaciones/`), conmuta el estado a `is_draft = false` y `operational_status = AVAILABLE`, e incorpora automáticamente los servicios obligatorios base (*Capitán* y *Combustible*) [SPEC FR-005, FR-009, FR-010].

Enfoque técnico: servicio backend Spring Boot 4.1.1 bajo **Arquitectura Hexagonal de tres capas** (`domain` / `application` / `infrastructure`) bajo el paquete base `com.seashare.seasharem1` [GTS §3.2]. La persistencia se realiza sobre **MySQL 8** mediante Spring Data JPA y Flyway (`V1__init_vessel_schema.sql`). La unicidad de un solo borrador por propietario a nivel de BD se asegura mediante una columna generada `draft_owner_id` con índice `UNIQUE`. Los errores se responden bajo el estándar **Problem Details (RFC 9457)** alineados con el catálogo general (`README.md` §3.4).

### Trazabilidad RF/RNF/HU → Componente / Tarea

| Requisito | Componente (rutas en §Project Structure) | Tareas | Prueba |
|---|---|---|---|
| **FR-001** Rol exclusivo `OWNER` | `JwtSecurityFilter`, `VesselDraftController`, `VesselRegistrationController` | T002, T010, T014 | T012, T016 |
| **FR-004** Unicidad de borrador activo por propietario | `VesselJpaEntity` (`draft_owner_id`), `VesselRepositoryAdapter`, `RegisterVesselService` | T004, T008, T009 | T007, T011, T012 |
| **FR-005** Inclusión automática de servicios base (*Capitán*, *Combustible*) | `Vessel` (Domain Entity), `RegisterVesselService` | T005, T009 | T006, T011 |
| **FR-006** Formato regex de matrícula (`^CP-\d{2}-\d{4}-[A-Z]$`) | `LegalRegistration` (Value Object) | T005 | T006, T012 |
| **FR-008** Validar topes de capacidad por `VesselType` | `VesselType` (Enum) / `Capacity` (Value Object) | T005 | T006, T012 |
| **FR-009** Carga y almacenamiento de fotografía ($\le 10 \text{ MB}$, JPG/PNG) | `LocalPhotoStorageAdapter`, `VesselRegistrationController` | T013, T014 | T015, T016 |
| **FR-010** Transición a `AVAILABLE` al completar | `Vessel.completeRegistration()`, `RegisterVesselService` | T005, T009 | T006, T011, T016 |
| **FR-011** Unicidad global de matrícula en BD | `VesselRepositoryPort.existsByLegalRegistration()`, `VesselJpaEntity` | T008, T009 | T011, T012 |
| **RNF-002** Tarifa base procesada como decimal exacto en COP | `BaseRate` (Value Object / `BigDecimal`), DTOs Jackson | T005, T010 | T006, T012 |
 
---

## Technical Context

**Language/Version**: Java 21 LTS (`java.version` 21) [GTS Context]  
**Primary Dependencies**: Spring Boot 4.1.1 (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-security`), Flyway Migration, MapStruct, Lombok, Testcontainers (MySQL 8), ArchUnit.  
**Storage**: MySQL 8. Almacenamiento local de fotos en disco `/uploads/embarcaciones/` (montado como volumen en Docker).  
**Testing**: JUnit 5, AssertJ, Mockito, MockMvc, Testcontainers, ArchUnit.  
**Target Platform**: Contenedores Docker (Linux).  
**Performance Goals**: Paso 1 $< 200 \text{ ms}$; Paso 2 (con I/O de disco) $< 500 \text{ ms}$.  
**Constraints**:
* Configurar en Spring `spring.servlet.multipart.max-file-size=10MB` y `spring.servlet.multipart.max-request-size=10MB`.
* Operación de Paso 1 reemplaza borradores previos de forma transparente.
* Matrícula legal inmutable y única globalmente.

---

## Project Structure

```text
src/main/java/com/seashare/seasharem1/
├── domain/
│   ├── model/
│   │   ├── Vessel.java                   # Entidad Raíz de Agregado [T005]
│   │   └── ServiceOption.java            # Entidad Value Object de Servicio [T005]
│   ├── valueobject/
│   │   ├── LegalRegistration.java        # Validador de Regex CP-XX-XXXX-X [T005]
│   │   ├── Capacity.java                 # Validador de topes según tipo [T005]
│   │   ├── BaseRate.java                 # Encapsula BigDecimal COP > 0 [T005]
│   │   ├── Berth.java                    # Embedded: portName, lat, long [T005]
│   │   ├── VesselType.java               # Enum con reglas de tope [T005]
│   │   └── OperationalStatus.java        # Enum (DRAFT, AVAILABLE, etc.) [T005]
│   └── exception/
│       ├── DomainException.java          # Excepción base de dominio [T003]
│       ├── InvalidRegistrationException.java # [T003]
│       ├── ExceededCapacityException.java    # [T003]
│       └── DuplicateRegistrationException.java # [T003]
│
├── application/
│   ├── port/in/
│   │   └── RegisterVesselUseCase.java    # Puerto de Entrada UC01 [T008]
│   ├── port/out/
│   │   ├── VesselRepositoryPort.java     # Puerto de Salida Persistencia [T008]
│   │   └── PhotoStoragePort.java         # Puerto de Salida Disco [T008]
│   ├── service/
│   │   └── RegisterVesselService.java    # Servicio de Aplicación `@Transactional` [T009]
│   └── dto/
│       ├── CreateDraftCommand.java       # Command DTO Paso 1 [T008]
│       ├── CompleteRegistrationCommand.java # Command DTO Paso 2 [T008]
│       ├── VesselDraftResult.java        # Result DTO Paso 1 [T008]
│       └── VesselCompletedResult.java    # Result DTO Paso 2 [T008]
│
└── infrastructure/
    ├── adapter/in/web/
    │   ├── VesselDraftController.java    # REST Controller POST /draft [T010]
    │   ├── VesselRegistrationController.java # REST Controller POST /{id}/complete [T014]
    │   ├── GlobalExceptionHandler.java   # Mapeador RFC 9457 Problem Details [T007]
    │   └── dto/
    │       ├── PostDraftVesselRequestDTO.java  # [T010]
    │       ├── PostDraftVesselResponseDTO.java # [T010]
    │       └── CompleteVesselResponseDTO.java  # [T014]
    ├── adapter/out/persistence/
    │   ├── VesselJpaEntity.java          # Mapeo ORM MySQL [T004]
    │   ├── SpringDataVesselRepository.java # JpaRepository [T004]
    │   ├── VesselRepositoryAdapter.java  # Implementación de VesselRepositoryPort [T009]
    │   └── VesselMapper.java             # MapStruct Dominio <-> JPA [T009]
    ├── adapter/out/storage/
    │   └── LocalPhotoStorageAdapter.java # Guarda imagen en disco local [T013]
    └── config/
        └── FileUploadConfig.java         # Configura límites de Multipart 10MB [T002]
```
 
---

## Reglas de Negocio

1. **Unicidad de Borrador Activo (Paso 1)**: Si un propietario (`owner_id`) solicita un nuevo borrador teniendo uno en `is_draft = true`, el sistema elimina físicamente el borrador previo en BD antes de crear el nuevo [SPEC FR-004].
2. **Formato Estricto de Matrícula**: Formato Regex `^CP-\d{2}-\d{4}-[A-Z]$`. Si viola el patrón $\rightarrow$ Error `400 Bad Request` (`INVALID_REGISTRATION_FORMAT`).
3. **Topes de Capacidad por Tipo**:
    * `MOTORBOAT` (Lancha): $\le 12$
    * `SAILBOAT` (Velero): $\le 15$
    * `CATAMARAN` (Catamarán): $\le 30$
    * `YACHT` (Yate): $\le 40$  
      Si excede el tope $\rightarrow$ Error `400 Bad Request` (`EXCEEDED_MAX_CAPACITY`).
4. **Unicidad de Matrícula Legal**: No se permite registrar una matrícula que ya exista en la BD (sea borrador o embarcación activa). Si existe $\rightarrow$ Error `409 Conflict` (`REGISTRATION_NUMBER_DUPLICATED`).
5. **Carga y Validación de Fotografía (Paso 2)**: Formatos permitidos: JPG, JPEG, PNG. Tamaño máximo: $10 \text{ MB}$. Si el archivo es inválido o excede el peso $\rightarrow$ Error `400 Bad Request` (`INVALID_FILE_FORMAT`).
6. **Inclusión de Servicios Base**: Al completar el registro, los servicios obligatorios (*Capitán* y *Combustible*) se asocian automáticamente en la BD con `is_base = true`.

---

## Contratos HTTP

### Paso 1: Crear / Reemplazar Borrador
`POST /api/v1/fleet/vessels/draft`

**Response 201 Created**:
```json
{
  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "is_draft": true,
  "operational_status": "DRAFT",
  "created_at": "2026-10-10T12:00:00Z"
}
```

### Paso 2: Finalizar Registro y Publicar
`POST /api/v1/fleet/vessels/{vessel_id}/complete` (Multipart Form: `photo`)

**Response 200 OK**:
```json
{
  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "name": "Yate Tayrona Sea Breeze",
  "registration_number": "CP-04-2021-0892",
  "is_draft": false,
  "operational_status": "AVAILABLE",
  "photo_url": "/uploads/embarcaciones/d3b07384-yate.jpg",
  "published_at": "2026-10-10T12:05:00Z"
}
```
 
---

## Discrepancias y Preguntas Abiertas

| ID | Descripción | Decisión para Avanzar |
|---|---|---|
| **D-UC01-01** | Manejo de borrador previo activo | Se elimina físicamente en el Paso 1 para mantener 1 solo borrador por propietario (Invariante D-03 del GTS). |
| **D-UC01-02** | Nombres de Estados en API vs Backend | API expone en inglés (`DRAFT`, `AVAILABLE`). El Frontend gestiona traducciones. |
 
---

## Implementation Phases

### Phase 1: Setup & Data Schema
- [ ] **T001**: Crear script Flyway `V1__init_vessel_schema.sql` con tablas `users`, `vessel`, `vessel_service` y columna generada `draft_owner_id` con índice `UNIQUE`.
- [ ] **T002**: Configurar `FileUploadConfig.java` para ajustar el tamaño máximo de archivo a $10 \text{ MB}$.
- [ ] **T003**: Crear árbol de excepciones de dominio (`DomainException`, `InvalidRegistrationException`, `ExceededCapacityException`).

### Phase 2: Domain Layer
- [ ] **T004**: Crear Value Objects (`LegalRegistration`, `Capacity`, `BaseRate`, `Berth`, `VesselType`).
- [ ] **T005**: Crear Entidad de Dominio `Vessel` con métodos `createDraft()` y `completeRegistration()`.
- [ ] **T006**: Pruebas unitarias de Dominio (`VesselTest`, `CapacityTest`, `LegalRegistrationTest`).

### Phase 3: Application Layer & Infrastructure Adapters
- [ ] **T007**: Configurar `GlobalExceptionHandler` con soporte Problem Details (RFC 9457).
- [ ] **T008**: Definir puertos de entrada y salida (`RegisterVesselUseCase`, `VesselRepositoryPort`, `PhotoStoragePort`).
- [ ] **T009**: Implementar `RegisterVesselService` con lógica de reemplazo de borrador y guardado en BD.
- [ ] **T010**: Implementar `VesselDraftController` para el Paso 1 (`POST /draft`).
- [ ] **T011**: Pruebas unitarias del servicio con Mockito (`RegisterVesselServiceTest`).
- [ ] **T012**: Pruebas de contrato MockMvc para Paso 1 (`VesselDraftControllerTest`).

### Phase 4: Multipart Storage & Paso 2 Completion
- [ ] **T013**: Implementar `LocalPhotoStorageAdapter` para almacenamiento físico en `/uploads/embarcaciones/`.
- [ ] **T014**: Implementar `VesselRegistrationController` para el Paso 2 (`POST /{id}/complete`).
- [ ] **T015**: Pruebas de integración de almacenamiento de archivos.
- [ ] **T016**: Pruebas de integración de extremo a extremo (E2E) del UC01 con Testcontainers (MySQL 8).