# Feature Specification: Asignar Estado Operativo
Created: 2026-09-08

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cambio automático de estado a Reservado o En Navegación por parte del sistema de reservas (Priority: P1)

El sistema de reservas necesita actualizar de forma automática el estado operativo de una embarcación (pasándola a "Reservado" o "En Navegación" cuando corresponda según el ciclo de vida del alquiler) para reflejar su disponibilidad real en la plataforma.

**Why this priority**: Es fundamental para evitar la sobreventa o el alquiler simultáneo de una embarcación que ya está ocupada o apartada por un cliente.

**Independent Test**: Puede probarse enviando una señal simulada desde el módulo de reservas y verificando que el estado de la embarcación cambie correctamente a "Reservado" o "En Navegación".

**Acceptance Scenarios**:

1. **Scenario**: Cambio de estado a Reservado al concretar una reserva
   - **Given** una embarcación se encuentra en estado Disponible
   - **When** el módulo de reservas confirma una reserva para dicha embarcación
   - **Then** el sistema actualiza automáticamente su estado operativo a Reservado

2. **Scenario**: Cambio de estado a En Navegación al iniciar el servicio
   - **Given** una embarcación se encuentra en estado Reservado
   - **When** el módulo de reservas notifica el inicio de la actividad de alquiler
   - **Then** el sistema actualiza automáticamente su estado operativo a En Navegación

---

### User Story 2 - Liberación de la embarcación a estado Disponible tras finalizar el alquiler o mantenimiento (Priority: P2)

Una vez que el servicio de alquiler finaliza, o cuando se completa una tarea de mantenimiento o limpieza, el sistema o el administrador actualizan la embarcación para que vuelva a estar disponible para nuevos alquileres.

**Why this priority**: Permite que las embarcaciones retornen al ciclo productivo de la plataforma una vez finalizada su ocupación o labores técnicas.

**Independent Test**: Puede probarse cambiando una embarcación que estaba en mantenimiento o navegación hacia el estado Disponible y verificando que quede habilitada para la renta.

**Acceptance Scenarios**:

1. **Scenario**: Retorno a Disponible al finalizar el alquiler y pasar por limpieza
   - **Given** una embarcación se encuentra en estado En Navegación
   - **When** el servicio de alquiler concluye y se registra el fin de la navegación
   - **Then** el sistema cambia su estado a En Mantenimiento/Limpieza y posteriormente permite dejarla en estado Disponible

2. **Scenario**: Retorno a Disponible tras finalizar el mantenimiento
   - **Given** una embarcación se encuentra en estado En Mantenimiento/Limpieza
   - **When** el administrador o el propietario registran en el sistema que han concluido las labores de mantenimiento y limpieza
   - **Then** el sistema actualiza el estado operativo de la embarcación a Disponible

---

### Edge Cases

- ¿Qué pasa si el módulo de reservas intenta cambiar el estado de una embarcación que ya no está disponible (por ejemplo, ya fue reservada por otro usuario en simultáneo)? El sistema rechaza la transición de estado y devuelve un error de conflicto al módulo externo para que se gestione la cancelación o reubicación.
- ¿Qué pasa si una embarcación en estado Borrador intenta recibir un cambio de estado operativo? El sistema bloquea la acción, ya que las embarcaciones en borrador no pueden ser reservadas ni operar hasta completar su registro.

## Requirements *(mandatory)*

### Functional Requirements

### Functional Requirements

- **FR-001**: El sistema DEBE permitir transiciones de estado operativo de la embarcación de acuerdo con su ciclo de vida definido: Disponible ⇄ Reservado ⇄ En Navegación ⇄ En Mantenimiento/Limpieza.
- **FR-002**: El sistema DEBE proveer un endpoint o mecanismo interno seguro para que el Módulo de Reservas pueda actualizar automáticamente el estado de la embarcación a "Reservado" o "En Navegación" al concretar o iniciar un alquiler.
- **FR-003**: El sistema DEBE proveer un mecanismo interno para actualizar el estado de la embarcación a "Disponible" una vez finalizado el ciclo de alquiler o concluidas las labores de mantenimiento y limpieza.
- **FR-004**: El sistema DEBE validar y rechazar cualquier intento de cambiar a estado "Reservado" o "En Navegación" una embarcación que no se encuentre previamente en estado "Disponible".
- **FR-005**: El sistema DEBE almacenar el registro de los cambios de estado operativo junto con la fecha y hora en que se realizaron.

### Key Entities

- **Embarcación**: Objeto principal afectado por la asignación de estados. Sus estados operativos posibles son: Disponible, Reservado, En Navegación y En Mantenimiento/Limpieza.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las solicitudes de cambio de estado enviadas por el módulo de reservas se procesan y reflejan en menos de 1 segundo.
- **SC-002**: Se previene en un 100% la asignación de reservas a embarcaciones que no se encuentran en estado Disponible.