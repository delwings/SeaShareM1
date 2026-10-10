# Implementation Plan: UC03 - Editar Información de Embarcación

**Date**: 2026-10-10  
**Spec**: `docs/features/003-editar-informacion-embarcacion/2-technical/spec.md`  
**Contract**: `docs/architecture/contracts/rest/UC03-editar-informacion.md`
 
---

## Summary

UC03 es el caso de uso por el cual un **Propietario** (rol `OWNER`) actualiza los atributos comerciales y operativos de una embarcación previamente registrada y publicada (`is_draft = false`). La edición permite actualizar el nombre comercial (`name`), la capacidad máxima (`max_capacity`), la tarifa base diaria (`base_rate_cop`), la ubicación de atracadero/puerto base (`berth_location`), la lista de servicios adicionales ofrecidos (`selected_service_ids`) y opcionalmente la fotografía principal mediante una nueva carga `multipart/form-data`.

Enfoque técnico: servicio backend Spring Boot 4.1.1 bajo **Arquitectura Hexagonal de tres capas** (`domain` / `application` / `infrastructure`) en el paquete base `com.seashare.seasharem1`. Se expone mediante el endpoint `PUT /api/v1/fleet/vessels/{vessel_id}` soportando consumo `multipart/form-data`. El flujo valida la inmutabilidad estricta de la matrícula legal (`legal_registration`) y del tipo de embarcación (`vessel_type`), la propiedad del recurso (`ownership check`), las restricciones de capacidad máxima según el tipo original y la existencia de reservas activas o futuras mediante consulta al cliente de integración/puerto de salida para evaluar si la modificación tarifaria o de capacidad afecta compromisos adquiridos.

### Trazabilidad RF/RNF/HU → Componente / Tarea

| Requisito | Componente (rutas en §Project Structure) | Tareas | Prueba |
|---|---|---|---|
| **FR-001** Rol exclusivo `OWNER` | `JwtSecurityFilter`, `UpdateVesselController` | T002, T008 | T010, T012 |
| **FR-002** Verificación de propiedad (`ownership check`) | `UpdateVesselService`, `VesselRepositoryPort` | T006, T007 | T009, T010 |
| **FR-003** Inmutabilidad de Matrícula y Tipo | `UpdateVesselRequestDTO`, `Vessel` (Domain Entity) | T004, T008 | T005, T010 |
| **FR-004** Validación de topes de capacidad según `VesselType` | `VesselType` (Enum) / `Capacity` (Value Object) | T004 | T005, T009 |
| **FR-005** Actualización de foto opcional ($\le 10 \text{ MB}$, JPG/PNG) | `LocalPhotoStorageAdapter`, `UpdateVesselController` | T007, T008 | T010, T012 |
| **FR-006** Rechazo si hay reservas activas en conflicto | `ReservationIntegrationPort`, `UpdateVesselService` | T006, T007 | T009, T011 |
| **RNF-001** Respuesta estandarizada con entidad actualizada | `UpdateVesselResponseDTO` | T008 | T010, T012 |
 
---

## Technical Context

**Language/Version**: Java 21 LTS (`java.version` 21)  
**Primary Dependencies**: Spring Boot 4.1.1 (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-security`), Flyway Migration, MapStruct, Lombok, Testcontainers (MySQL 8), ArchUnit.  
**Storage**: MySQL 8. Almacenamiento local de fotos en disco `/uploads/embarcaciones/`.  
**Testing**: JUnit 5, AssertJ, Mockito, MockMvc, Testcontainers (MySQL 8), ArchUnit.  
**Target Platform**: Contenedores Docker (Linux).  
**Performance Goals**: Procesamiento de actualización $< 300 \text{ ms}$ (sin cambio de foto) y $< 500 \text{ ms}$ (con reemplazo de foto).  
**Constraints**:
* `legal_registration` y `vessel_type` NO son modificables bajo ningún concepto.
* Si se envía nueva fotografía, se reemplaza la anterior en disco y se actualiza la URL.
* La tarifa base debe mantenerse como decimal exacto positivo en COP.

---

## Project Structure

```text
src/main/java/com/seashare/seasharem1/
├── domain/
│   ├── model/
│   │   └── Vessel.java                   # Contiene método updateInformation(...) [T004]
│   └── exception/
│       ├── DomainException.java          # [T003]
│       ├── VesselNotFoundException.java  # Mapea a 404 Not Found [T003]
│       ├── ExceededCapacityException.java    # Mapea a 400 Bad Request [T003]
│       ├── ImmutableFieldException.java  # Mapea a 400 Bad Request [T003]
│       └── ActiveReservationConflictException.java # Mapea a 409 Conflict [T003]
│
├── application/
│   ├── port/in/
│   │   └── UpdateVesselUseCase.java      # Puerto de Entrada UC03 [T006]
│   ├── port/out/
│   │   ├── VesselRepositoryPort.java     # Puerto de Salida Persistencia [T006]
│   │   ├── PhotoStoragePort.java         # Puerto de Salida Almacenamiento [T006]
│   │   └── ReservationIntegrationPort.java # Consulta de reservas activas [T006]
│   ├── service/
│   │   └── UpdateVesselService.java      # Servicio de Aplicación `@Transactional` [T007]
│   └── dto/
│       ├── UpdateVesselCommand.java      # Command DTO [T006]
│       └── VesselUpdatedResult.java      # Result DTO [T006]
│
└── infrastructure/
    ├── adapter/in/web/
    │   ├── UpdateVesselController.java   # REST Controller PUT /{vessel_id} [T008]
    │   └── dto/
    │       ├── UpdateVesselRequestDTO.java  # DTO Multipart [T008]
    │       └── UpdateVesselResponseDTO.java # DTO Respuesta [T008]
    └── adapter/out/persistence/
        ├── VesselRepositoryAdapter.java  # Persistencia JPA [T007]
        └── VesselMapper.java             # MapStruct Dominio <-> JPA [T007]
```
 
---

## Reglas de Negocio

1. **Autenticación y Propiedad**: Solo el propietario (`owner_id` del token JWT) que creó la embarcación puede actualizarla. Si no coincide $\rightarrow$ Error `403 Forbidden` (`FORBIDDEN`).
2. **Campos Inmutables**: La matrícula legal (`legal_registration`) y el tipo de embarcación (`vessel_type`) no pueden ser modificados. Si el DTO intenta enviar un valor distinto al almacenado $\rightarrow$ Error `400 Bad Request` (`IMMUTABLE_FIELD_MODIFICATION`).
3. **Validación de Capacidad**: La nueva capacidad máxima debe cumplir con los topes según el `vessel_type` original:
    * `MOTORBOAT` $\le 12$
    * `SAILBOAT` $\le 15$
    * `CATAMARAN` $\le 30$
    * `YACHT` $\le 40$  
      Si excede $\rightarrow$ Error `400 Bad Request` (`EXCEEDED_MAX_CAPACITY`).
4. **Conflicto con Reservas Activas**: Si se reduce la capacidad máxima a un valor inferior al número de pasajeros de una reserva confirmada o futura $\rightarrow$ Error `409 Conflict` (`ACTIVE_RESERVATION_CAPACITY_CONFLICT`).
5. **Reemplazo de Fotografía (Opcional)**: Si se proporciona un nuevo archivo de imagen (JPG/PNG, $\le 10 \text{ MB}$), se elimina la imagen previa del almacenamiento local y se guarda la nueva imagen generando una nueva URL pública.

---

## Contrato HTTP

### Editar Embarcación
`PUT /api/v1/fleet/vessels/{vessel_id}` (Multipart Form: `data` [JSON] + `photo` [Archivo opcional])

**Request Body (Part `data` - application/json)**:
```json
{
  "name": "Yate Tayrona Sea Breeze II",
  "max_capacity": 35,
  "base_rate_cop": 1800000.00,
  "berth_location": {
    "port_name": "Marina Internacional de Santa Marta",
    "latitude": 11.2422,
    "longitude": -74.2155
  },
  "selected_service_ids": [
    "e5f82194-2b11-4f33-8a03-7a911a3b8101",
    "f1a93205-3c22-5e44-9b14-8b022b4c9202"
  ]
}
```

**Response 200 OK**:
```json
{
  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
  "name": "Yate Tayrona Sea Breeze II",
  "registration_number": "CP-04-2021-0892",
  "vessel_type": "YACHT",
  "max_capacity": 35,
  "base_rate_cop": 1800000.00,
  "berth_location": {
    "port_name": "Marina Internacional de Santa Marta",
    "latitude": 11.2422,
    "longitude": -74.2155
  },
  "photo_url": "/uploads/embarcaciones/d3b07384-yate-updated.jpg",
  "updated_at": "2026-10-10T14:30:00Z"
}
```

**Errores Posibles**:
* `400 Bad Request`: Inmutabilidad violada, capacidad excedida o formato de imagen inválido.
* `403 Forbidden`: Intento de edición por un usuario que no es el propietario.
* `404 Not Found`: Embarcación no encontrada o eliminada.
* `409 Conflict`: Reducción de capacidad entra en conflicto con reservas futuras confirmadas.

---

## Discrepancias y Preguntas Abiertas

| ID | Descripción | Decisión para Avanzar |
|---|---|---|
| **D-UC03-01** | Formato del Request Multipart | Se utiliza un endpoint `PUT` que recibe una parte JSON `data` con la metadata estructurada y opcionalmente la parte `photo` para el archivo binario. |
| **D-UC03-02** | Manejo de Fotos Anteriores | Al actualizar la foto exitosamente, el adaptador de almacenamiento elimina físicamente el archivo anterior de disco para evitar acumulación de basura. |
 
---

## Implementation Phases

### Phase 1: Domain Logic & Validations
- [ ] **T001**: Crear excepciones de dominio (`ImmutableFieldException`, `ActiveReservationConflictException`).
- [ ] **T002**: Agregar regla de actualización en la entidad de dominio `Vessel.updateInformation(...)` con validaciones de topes de capacidad y consistencia interna.
- [ ] **T003**: Pruebas unitarias de Dominio (`VesselUpdateTest`) verificando rechazo por inmutabilidad o exceso de capacidad.

### Phase 2: Application Ports & Services
- [ ] **T004**: Definir `ReservationIntegrationPort` para verificar si existen reservas futuras comprometidas con capacidades mayores a la deseada.
- [ ] **T005**: Crear `UpdateVesselCommand`, `VesselUpdatedResult` y el puerto de entrada `UpdateVesselUseCase`.
- [ ] **T006**: Implementar `UpdateVesselService` encapsulado en `@Transactional`:
    1. Recuperar la embarcación por `vessel_id`.
    2. Validar propiedad con `owner_id`.
    3. Consultar `ReservationIntegrationPort` si disminuye la capacidad.
    4. Procesar la nueva foto en `PhotoStoragePort` (si está presente) y borrar la anterior.
    5. Ejecutar `Vessel.updateInformation(...)` y guardar en BD.
- [ ] **T007**: Pruebas unitarias del servicio con Mockito (`UpdateVesselServiceTest`).

### Phase 3: Infrastructure REST Adapter & Integration
- [ ] **T008**: Implementar `UpdateVesselController` recibiendo `PUT /api/v1/fleet/vessels/{vessel_id}` con `@RequestPart("data")` y `@RequestPart(value = "photo", required = false)`.
- [ ] **T009**: Configurar el mapeo de respuestas en `UpdateVesselResponseDTO` y actualización en `GlobalExceptionHandler`.
- [ ] **T010**: Pruebas de contrato MockMvc para `UpdateVesselControllerTest`.
- [ ] **T011**: Pruebas de integración de extremo a extremo (E2E) con Testcontainers (MySQL 8), simulando la actualización con y sin reemplazo de archivo fotográfico.