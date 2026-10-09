# Feature Specification: Consultar Información de Embarcación
Created: 2026-09-08

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Propietario o Administrador consulta los detalles de una embarcación (Priority: P1)

El propietario o el administrador ingresan al sistema, buscan o seleccionan una embarcación dentro del listado y visualizan su ficha completa con toda la información registrada (datos básicos, puerto, tarifas, servicios, estado actual y fotografía).

**Why this priority**: Es la funcionalidad principal de consulta; permite a los usuarios internos verificar el estado y los datos de las embarcaciones de forma individual sin alterar la información.

**Independent Test**: Puede probarse seleccionando cualquier embarcación existente en el sistema y verificando que la pantalla muestre correctamente todos sus datos registrados.

**Acceptance Scenarios**:

1. **Scenario**: Consulta exitosa por parte del propietario
   - **Given** el propietario está autenticado en el sistema
   - **When** selecciona una de sus embarcaciones registradas para ver sus detalles
   - **Then** el sistema muestra la información completa de la embarcación (datos básicos, puerto, tarifa, servicios, fotografía y estado actual)

2. **Scenario**: Consulta exitosa por parte del administrador
   - **Given** el administrador está autenticado en el sistema
   - **When** busca y selecciona cualquier embarcación registrada en la plataforma
   - **Then** el sistema muestra la información completa de la embarcación independientemente de quién sea el propietario

3. **Scenario**: Embarcación no encontrada o eliminada
   - **Given** el usuario intenta acceder a una embarcación mediante un enlace directo o ID que ya no existe
   - **When** carga la página de detalle
   - **Then** el sistema muestra un mensaje indicando que la embarcación no está disponible o no existe

---

### User Story 2 - El módulo de reservas consulta la información completa de la embarcación (Priority: P2)

El sistema de reservas necesita consultar de forma automática toda la información de la embarcación (características, capacidad, puerto, servicios incluidos, tarifas y estado) para procesar correctamente las solicitudes de alquiler de los clientes.

**Why this priority**: Permite la integración y comunicación fluida con el módulo de reservas para que disponga de todos los datos reales y actualizados al momento de cotizar o gestionar un alquiler.

**Independent Test**: Puede probarse simulando una solicitud desde el módulo de reservas hacia una embarcación y verificando que el sistema retorna su ficha informativa completa.

**Acceptance Scenarios**:

1. **Scenario**: Consulta de datos completos para el módulo de reservas
   - **Given** el módulo de reservas requiere procesar una solicitud de alquiler
   - **When** realiza una solicitud de consulta sobre una embarcación específica
   - **Then** el sistema retorna toda la información de la embarcación (datos básicos, capacidad, puerto de atraque, tarifa base, servicios incluidos y estado operativo actual)

---

### Edge Cases

- ¿Qué pasa si el sistema de reservas intenta consultar una embarcación que se encuentra en mantenimiento o en estado borrador? El sistema responde al módulo externo indicando que la embarcación no está apta para operar en ese momento.
- ¿Qué pasa si hay fallas de conexión al momento en que el módulo de reservas intenta consultar la información? El sistema externo maneja un tiempo de espera (timeout) y reintenta la consulta para evitar bloqueos en el flujo del usuario.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir a los propietarios consultar el detalle de sus propias embarcaciones registradas.
- **FR-002**: El sistema DEBE permitir a los administradores consultar el detalle de cualquier embarcación registrada en la plataforma.
- **FR-003**: El sistema DEBE mostrar en la vista de consulta todos los atributos de la embarcación: nombre, matrícula legal, tipo, capacidad máxima de pasajeros, puerto de atraque con su ubicación y coordenadas GPS, tarifa base, servicios incluidos (base y adicionales), fotografía y estado actual.
- **FR-004**: El sistema DEBE proveer un mecanismo o API interna para que el Módulo de Reservas pueda consultar en tiempo real la información completa y actualizada de la embarcación (incluyendo capacidad máxima y ubicación GPS del puerto) para sus validaciones operativas.
- **FR-005**: El sistema DEBE restringir la consulta de detalles de una embarcación si el usuario autenticado no es el propietario ni un administrador (salvo las consultas automatizadas permitidas hacia el módulo de reservas).

### Key Entities

- **Embarcación**: Objeto central de la consulta. Atributos visibles: nombre, matrícula legal, tipo, capacidad máxima de pasajeros, tarifa base, estado, fotografía, puerto de atraque con coordenadas GPS y servicios asociados.
- **Puerto de Atraque**: Ubicación geográfica vinculada a la embarcación (nombre, latitud y longitud) utilizada para la sincronización de zonas horarias y reglas de tiempo en las reservas.
- **Servicio**: Inclusiones de la embarcación divididas en servicios base (Capitán y Combustible) y servicios adicionales (chalecos, equipo de pesca, sonido, nevera, buceo).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Los usuarios internos (propietarios y administradores) pueden visualizar la información completa de una embarcación en menos de 2 segundos.
- **SC-002**: El 100% de las consultas provenientes del módulo de reservas obtienen respuestas correctas y sincronizadas con el estado actual de la embarcación.
- **SC-003**: Ningún usuario no autorizado puede acceder a la consulta detallada de una embarcación que no le pertenece.