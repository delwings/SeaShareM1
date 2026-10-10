# Implementation Plan: UC09 - Consultar Opciones de Servicios Base y Adicionales

**Date**: 2026-10-10  
**Spec**: `docs/features/009-consultar-opciones-servicios/2-technical/spec.md`  
**Contract**: `docs/architecture/contracts/rest/UC09-consultar-servicios.md`
 
---

## Summary

UC09 es el caso de uso por el cual los propietarios (durante la creación o edición de una embarcación en los UC01 y UC03) o cualquier usuario de la plataforma consultan el catálogo maestro de servicios parametrizados disponibles en el sistema. Los servicios se categorizan en dos tipos: servicios base obligatorios (`is_base = true`, tales como Capitán y Combustible) y servicios adicionales opcionales (`is_base = false`, tales como Cenas a bordo, Deportes acuáticos, Tripulación adicional, etc.).

Enfoque técnico: servicio backend Spring Boot 4.1.1 bajo Arquitectura Hexagonal de tres capas (`domain` / `application` / `infrastructure`) en el paquete base `com.seashare.seasharem1`. La consulta se expone a través del endpoint público `GET /api/v1/fleet/services` soportando filtrado opcional por categoría o tipo (`is_base`). Al ser un catálogo maestro de lectura frecuente con baja tasa de mutación, el servicio implementa un mecanismo de almacenamiento en caché en memoria (`@Cacheable`) para garantizar tiempos de respuesta ultra rápidos (< 20 ms).

### Trazabilidad RF/RNF/HU -> Componente / Tarea

| Requisito | Componente (rutas en §Project Structure) | Tareas | Prueba |
|---|---|---|---|
| FR-001 Acceso público al catálogo maestro de servicios | SecurityConfig, GetServiceOptionsController | T002, T006 | T008, T010 |
| FR-002 Diferenciación entre Servicios Base y Adicionales | ServiceOption (Domain Entity), ServiceOptionMapper | T003, T005 | T007, T008 |
| FR-003 Filtro opcional por tipo (`is_base`) | GetServiceOptionsService, SpringDataServiceOptionRepository | T004, T005 | T007, T008 |
| RNF-001 Respuesta ultra rápida con caché (< 20 ms) | GetServiceOptionsService (`@Cacheable`), Spring Cache | T002, T005 | T009, T010 |
 
---

## Technical Context

Language/Version: Java 21 LTS (`java.version` 21)  
Primary Dependencies: Spring Boot 4.1.1 (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-cache`), Flyway Migration, MapStruct, Lombok, Testcontainers (MySQL 8), ArchUnit.  
Storage: MySQL 8 (Tabla maestra `service_option`). Caché en memoria gestionada por Spring Cache (`ConcurrentMapCacheManager`).  
Testing: JUnit 5, AssertJ, Mockito, MockMvc, Testcontainers (MySQL 8), ArchUnit.  
Target Platform: Contenedores Docker (Linux).  
Performance Goals: Tiempo de respuesta < 20 ms con caché activa.  
Constraints:
- Catálogo de solo lectura expuesto públicamente.
- Los IDs de servicios retornados son los UUIDs válidos a utilizar en la creación (UC01) y edición (UC03) de embarcaciones.

---

## Project Structure

src/main/java/com/seashare/seasharem1/
├── domain/
│   ├── model/
│   │   └── ServiceOption.java            # Entidad Value Object de Servicio [T003]
│   └── exception/
│       └── DomainException.java          # [T001]
│
├── application/
│   ├── port/in/
│   │   └── GetServiceOptionsUseCase.java # Puerto de Entrada UC09 [T004]
│   ├── port/out/
│   │   └── ServiceOptionRepositoryPort.java # Puerto de Salida Persistencia [T004]
│   ├── service/
│   │   └── GetServiceOptionsService.java # Servicio `@Cacheable` `@Transactional(readOnly = true)` [T005]
│   └── dto/
│       └── ServiceOptionResult.java      # Result DTO [T004]
│
└── infrastructure/
├── adapter/in/web/
│   ├── GetServiceOptionsController.java # REST Controller GET /services [T006]
│   └── dto/
│       ├── ServiceOptionResponseDTO.java # DTO de Respuesta JSON [T006]
│       └── ServiceOptionListResponseDTO.java # DTO contenedor de lista [T006]
└── adapter/out/persistence/
├── ServiceOptionJpaEntity.java   # Mapeo JPA [T004]
├── SpringDataServiceOptionRepository.java # JpaRepository [T004]
├── ServiceOptionRepositoryAdapter.java # Implementación del puerto [T004]
└── ServiceOptionMapper.java      # MapStruct Dominio <-> JPA <-> DTO [T005]
 
---

## Reglas de Negocio

1. Acceso Público: Endpoint libre de autenticación para consultar las opciones disponibles.
2. Parametrización Maestra: La tabla `service_option` contiene los registros precargados mediante migraciones Flyway.
3. Categorización de Servicios:
    - `is_base = true`: Servicios obligatorios asociados automáticamente a toda embarcación (Capitán, Combustible).
    - `is_base = false`: Servicios adicionales opcionales que el propietario puede seleccionar en el registro o edición.
4. Filtrado Opcional: El parámetro opcional `is_base` (boolean) permite obtener únicamente servicios base o únicamente adicionales. Si no se provee, retorna la totalidad del catálogo maestro.

---

## Contrato HTTP

### Consultar Opciones de Servicios
GET /api/v1/fleet/services

Query Parameters:
- `is_base` (opcional): true

Response 200 OK:
{
"services": [
{
"service_id": "e5f82194-2b11-4f33-8a03-7a911a3b8101",
"name": "Capitán Profesional",
"description": "Servicio obligatorio de capitán certificado a bordo",
"is_base": true
},
{
"service_id": "f1a93205-3c22-5e44-9b14-8b022b4c9202",
"name": "Combustible Incluido",
"description": "Tanque de combustible cubierto para la ruta especificada",
"is_base": true
},
{
"service_id": "a9b8c7d6-e5f4-3a2b-1c0d-9e8f7a6b5c4d",
"name": "Equipo de Snorkel y Buceo",
"description": "Mascars, aletas y tubos de snorkel para todos los pasajeros",
"is_base": false
}
],
"total_count": 3
}

Errores Posibles:
- 400 Bad Request: Formato inválido en el parámetro `is_base`.
- 500 Internal Error: Error interno del servidor o falla de base de datos.

---

## Discrepancias y Preguntas Abiertas

| ID | Descripción | Decisión para Avanzar |
|---|---|---|
| D-UC09-01 | Invalidez de Caché | Al ser un catálogo maestro estático administrado por migraciones BD/Flyway, la caché no requiere TTL corto y se invalida al reiniciar la aplicación o mediante despliegues. |
 
---

## Implementation Phases

### Phase 1: Setup, Entity & Cache Config
- [ ] T001: Crear script de inserción Flyway con los datos maestros de servicios iniciales (`V2__insert_master_service_options.sql`).
- [ ] T002: Habilitar la anotación `@EnableCaching` en la configuración de Spring Boot y autorizar acceso público en `SecurityConfig`.
- [ ] T003: Crear la entidad de dominio y VO `ServiceOption`.

### Phase 2: Persistence & Application Service
- [ ] T004: Crear `ServiceOptionJpaEntity`, `SpringDataServiceOptionRepository` e implementar `ServiceOptionRepositoryPort`.
- [ ] T005: Implementar `GetServiceOptionsService` con la anotación `@Cacheable(value = "serviceOptions")`:
    1. Evaluar si el filtro `is_base` está presente.
    2. Consultar el repositorio por tipo o traer la lista completa.
    3. Mapear resultados a `ServiceOptionResult`.
- [ ] T006: Pruebas unitarias del servicio con Mockito comprobando la lógica de filtrado (`GetServiceOptionsServiceTest`).

### Phase 3: Infrastructure REST & E2E Testing
- [ ] T007: Implementar `GetServiceOptionsController` para atender `GET /api/v1/fleet/services`.
- [ ] T008: Pruebas de contrato MockMvc para el controlador (`GetServiceOptionsControllerTest`).
- [ ] T009: Pruebas de rendimiento y caché verificando que las consultas consecutivas no golpeen la base de datos MySQL.
- [ ] T010: Pruebas de integración E2E con Testcontainers (MySQL 8) validando la lista completa de servicios precargados.