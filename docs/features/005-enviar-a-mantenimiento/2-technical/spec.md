# Implementation Plan: UC05 - Ver Detalle de Embarcación

**Date**: 2026-10-10  
**Spec**: `docs/features/005-ver-detalle-embarcacion/2-technical/spec.md`  
**Contract**: `docs/architecture/contracts/rest/UC05-ver-detalle.md`
 
---

## Summary

UC05 es el caso de uso por el cual cualquier usuario de la plataforma (navegante, visitante o propietario) consulta la información pública detallada de una embarcación específica identificada por su `vessel_id`. El detalle incluye la información comercial completa (nombre, tipo, capacidad máxima, tarifa base diaria en COP, puerto base y coordenadas), la fotografía principal, la lista completa de servicios asociados (obligatorios e incluidos) y las reglas de cancelación/operación.

Enfoque técnico: servicio backend Spring Boot 4.1.1 bajo Arquitectura Hexagonal de tres capas (`domain` / `application` / `infrastructure`) en el paquete base `com.seashare.seasharem1`. Se expone a través del endpoint público `GET /api/v1/fleet/vessels/{vessel_id}`. El flujo consulta el repositorio de persistencia asegurando que la embarcación se encuentre publicada (`is_draft = false` e `is_deleted = false`). Si la embarcación está en borrador o eliminada, se deniega la consulta pública con error `404 Not Found` (a menos que el solicitante sea el propietario autenticado en sesión).

### Trazabilidad RF/RNF/HU -> Componente / Tarea

| Requisito | Componente (rutas en §Project Structure) | Tareas | Prueba |
|---|---|---|---|
| FR-001 Acceso público al detalle de embarcación | SecurityConfig, GetVesselDetailController | T002, T007 | T009, T011 |
| FR-002 Restricción de visibilidad para borradores | GetVesselDetailService, Vessel | T004, T006 | T005, T008 |
| FR-003 Mapeo de servicios incluidos y base | VesselMapper, ServiceOption | T004, T006 | T005, T008 |
| FR-004 Respuesta estructurada completa | GetVesselDetailResponseDTO | T007 | T009, T011 |
| RNF-001 Consulta de alta velocidad (< 50 ms) | MySQL 8 DB Schema, SpringDataVesselRepository | T001, T004 | T008, T011 |
 
---

## Technical Context

Language/Version: Java 21 LTS (`java.version` 21)  
Primary Dependencies: Spring Boot 4.1.1 (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`), Flyway Migration, MapStruct, Lombok, Testcontainers (MySQL 8), ArchUnit.  
Storage: MySQL 8. Lectura directa por clave primaria (`vessel_id`) con relación `FETCH JOIN` sobre `vessel_service`.  
Testing: JUnit 5, AssertJ, Mockito, MockMvc, Testcontainers (MySQL 8), ArchUnit.  
Target Platform: Contenedores Docker (Linux).  
Performance Goals: Tiempo de respuesta < 50 ms.  
Constraints:
- Retornar 404 Not Found si la embarcación no existe, está en borrador (`is_draft = true`) o fue eliminada (`is_deleted = true`).
- Mapear correctamente todos los servicios base (Capitán, Combustible) y adicionales seleccionados.

---

## Project Structure

src/main/java/com/seashare/seasharem1/
├── domain/
│   ├── model/
│   │   ├── Vessel.java                   # Entidad Raíz de Agregado [T004]
│   │   └── ServiceOption.java            # Entidad de Servicios [T004]
│   └── exception/
│       ├── DomainException.java          # [T003]
│       └── VesselNotFoundException.java  # Mapea a 404 Not Found [T003]
│
├── application/
│   ├── port/in/
│   │   └── GetVesselDetailUseCase.java   # Puerto de Entrada UC05 [T005]
│   ├── port/out/
│   │   └── VesselRepositoryPort.java     # Puerto de Salida Persistencia [T005]
│   ├── service/
│   │   └── GetVesselDetailService.java   # Servicio `@Transactional(readOnly = true)` [T006]
│   └── dto/
│       └── VesselDetailResult.java       # Result DTO completo [T005]
│
└── infrastructure/
├── adapter/in/web/
│   ├── GetVesselDetailController.java # REST Controller GET /{vessel_id} [T007]
│   └── dto/
│       └── GetVesselDetailResponseDTO.java # DTO de Respuesta JSON [T007]
└── adapter/out/persistence/
├── VesselRepositoryAdapter.java  # Implementación con Fetch Join [T004]
└── VesselMapper.java             # MapStruct Dominio <-> JPA <-> DTO [T006]
 
---

## Reglas de Negocio

1. Acceso Público General: Endpoint accesible sin autenticación para embarcaciones en estado `AVAILABLE` (`is_draft = false` e `is_deleted = false`).
2. Restricción de Borradores: Si `is_draft = true` o `is_deleted = true`, el sistema responde `404 Not Found` (`VESSEL_NOT_FOUND`) para proteger datos no publicados.
3. Excepción de Propietario: Si la petición incluye un token JWT válido perteneciente al propietario de la embarcación, se permite la lectura del borrador.
4. Desglose de Servicios: La respuesta debe categorizar claramente los servicios obligatorios base (`is_base = true`) y los servicios adicionales incluidos (`is_base = false`).

---

## Contrato HTTP

### Ver Detalle de Embarcación
GET /api/v1/fleet/vessels/{vessel_id}

Path Variables:
- `vessel_id`: UUID v4 (Identificador único de la embarcación)

Response 200 OK:
{
"vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
"name": "Yate Tayrona Sea Breeze",
"registration_number": "CP-04-2021-0892",
"vessel_type": "YACHT",
"max_capacity": 30,
"base_rate_cop": 1800000.00,
"berth_location": {
"port_name": "Marina Internacional de Santa Marta",
"latitude": 11.2422,
"longitude": -74.2155
},
"photo_url": "/uploads/embarcaciones/d3b07384-yate.jpg",
"operational_status": "AVAILABLE",
"is_draft": false,
"services": [
{
"service_id": "e5f82194-2b11-4f33-8a03-7a911a3b8101",
"name": "Capitán Profesional",
"is_base": true
},
{
"service_id": "f1a93205-3c22-5e44-9b14-8b022b4c9202",
"name": "Combustible Incluido",
"is_base": true
}
],
"owner_id": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
"created_at": "2026-10-10T12:00:00Z"
}

Errores Posibles:
- 400 Bad Request: Formato de `vessel_id` no es un UUID válido.
- 404 Not Found: La embarcación no existe, está eliminada o se encuentra en estado borrador.
- 500 Internal Error: Error de base de datos o fallo interno.

---

## Discrepancias y Preguntas Abiertas

| ID | Descripción | Decisión para Avanzar |
|---|---|---|
| D-UC05-01 | Exposición de Matrícula Legal | La matrícula legal (`registration_number`) se incluye en la respuesta del detalle público para brindar transparencia y confianza al cliente navegante. |
 
---

## Implementation Phases

### Phase 1: Persistence & Domain Query
- [ ] T001: Configurar consulta en `SpringDataVesselRepository` utilizando `FETCH JOIN` para cargar la embarcación y sus servicios en una sola consulta SQL.
- [ ] T002: Configurar `SecurityConfig` para permitir acceso público a `GET /api/v1/fleet/vessels/{vessel_id}`.
- [ ] T003: Crear el método `Optional<Vessel> findByIdWithServices(UUID vesselId)` en `VesselRepositoryPort`.

### Phase 2: Application Service
- [ ] T004: Crear `GetVesselDetailUseCase` y DTO `VesselDetailResult`.
- [ ] T005: Implementar `GetVesselDetailService` (`@Transactional(readOnly = true)`):
    1. Consultar la embarcación en `VesselRepositoryPort`.
    2. Verificar si `is_deleted == true` o (`is_draft == true` y no es el dueño).
    3. Lanza `VesselNotFoundException` en caso positivo.
    4. Mapear y retornar el resultado.
- [ ] T006: Pruebas unitarias con Mockito (`GetVesselDetailServiceTest`).

### Phase 3: Infrastructure REST & E2E
- [ ] T007: Implementar `GetVesselDetailController` para el endpoint `GET /api/v1/fleet/vessels/{vessel_id}`.
- [ ] T008: Pruebas de contrato MockMvc (`GetVesselDetailControllerTest`).
- [ ] T009: Pruebas de integración E2E con Testcontainers (MySQL 8) comprobando rendimiento y respuestas 404 para borradores.