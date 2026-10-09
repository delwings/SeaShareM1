# Feature Specification: Eliminar Registro Embarcación

**Created**: 2026-09-07

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Eliminar (desactivar) una embarcación propia (Priority: P1)

El Propietario elimina una de sus embarcaciones que ya fue creada. La eliminación es una **desactivación** (no un borrado definitivo de los datos): la embarcación deja de aparecer en las búsquedas y deja de poder reservarse. Esta acción es **definitiva** y no se puede deshacer ni reactivar la embarcación.

**Why this priority**: Permite al Propietario retirar del servicio una embarcación propia de forma segura y controlada, impidiendo que se siga ofreciendo o reservando.

**Independent Test**: Puede probarse eliminando una embarcación propia en estado Disponible o En Mantenimiento/Limpieza, confirmando la acción, y verificando que deja de aparecer y de poder reservarse.

**Acceptance Scenarios**:

1. **Scenario**: Eliminación confirmada de una embarcación propia
   - **Given** el Propietario está autenticado y tiene una embarcación propia en estado Disponible o En Mantenimiento/Limpieza sin reservas activas
   - **When** inicia la eliminación y confirma la advertencia de que la acción es definitiva e irreversible
   - **Then** el sistema desactiva la embarcación, muestra una confirmación de éxito y lleva al Propietario a la lista de embarcaciones

2. **Scenario**: Eliminación cancelada en la confirmación
   - **Given** el Propietario inició la eliminación de una embarcación propia
   - **When** el sistema muestra la advertencia de irreversibilidad y el Propietario elige cancelar
   - **Then** el sistema no elimina la embarcación y permanece donde estaba, sin cambios

3. **Scenario**: Eliminación bloqueada por reservas activas
   - **Given** el Propietario tiene una embarcación propia con reservas futuras o en curso
   - **When** intenta eliminarla
   - **Then** el sistema bloquea la eliminación y muestra un mensaje indicando que la embarcación tiene reservas activas y no puede eliminarse

4. **Scenario**: Eliminación bloqueada por estado no permitido
   - **Given** el Propietario tiene una embarcación propia en estado Reservada o En Navegación
   - **When** intenta eliminarla
   - **Then** el sistema bloquea la eliminación y no permite ejecutarla

---

### User Story 2 - Eliminar (desactivar) cualquier embarcación como Administrador (Priority: P1)

El Administrador elimina la embarcación de cualquier propietario del sistema. A diferencia del Propietario, el Administrador puede eliminar una embarcación que está Reservada, gestionando las reservas existentes. Al igual que con el Propietario, la eliminación es definitiva e irreversible, y requiere confirmación explícita.

**Why this priority**: Permite al Administrador retirar del servicio embarcaciones problemáticas (por incumplimiento, seguridad o fraude del Propietario) aunque tengan reservas activas.

**Independent Test**: Puede probarse eliminando, como Administrador, una embarcación de otro propietario en estado Disponible, Reservada o En Mantenimiento/Limpieza y verificando que se desactiva y deja de estar disponible.

**Acceptance Scenarios**:



1. **Scenario**: Eliminación con reservas activas por el Administrador
   - **Given** el Administrador está autenticado y la embarcación tiene reservas activas
   - **When** decide eliminarla (por ejemplo, por incumplimiento, seguridad o fraude del Propietario) y confirma
   - **Then** el sistema desactiva la embarcación y se gestionan las reservas existentes (cancelación y notificación al cliente; el mecanismo exacto se define con el Módulo 2 de Reservas)

2. **Scenario**: Eliminación bloqueada en estado En Navegación
   - **Given** el Administrador intenta eliminar una embarcación en estado En Navegación
   - **When** intenta iniciar su eliminación
   - **Then** el sistema bloquea la eliminación por tratarse de un uso activo en tiempo real con riesgo operativo o de seguridad

3. **Scenario**: Eliminación cancelada en la confirmación
   - **Given** el Administrador inició la eliminación de una embarcación
   - **When** el sistema muestra la advertencia de irreversibilidad y el Administrador elige cancelar
   - **Then** el sistema no elimina la embarcación y permanece donde estaba, sin cambios

---

### User Story 3 - Iniciar eliminación desde la lista o el detalle de la embarcación (Priority: P2)

Tanto el Propietario como el Administrador pueden iniciar la eliminación de una embarcación desde la lista de embarcaciones o desde la pantalla de detalle de una embarcación.

**Why this priority**: Ofrece flexibilidad para acceder a la eliminación según el contexto en el que se esté trabajando.

**Independent Test**: Puede probarse iniciando la eliminación tanto desde la lista de embarcaciones como desde el detalle de una embarcación, verificando que en ambos casos funciona igual.

**Acceptance Scenarios**:

1. **Scenario**: Eliminación iniciada desde la lista (Propietario)
   - **Given** el Propietario está en la lista de sus embarcaciones registradas
   - **And** la embarcación seleccionada está en un estado permitido (Disponible o En Mantenimiento/Limpieza) y sin reservas activas ni futuras
   - **When** elige la opción de eliminar una embarcación desde la lista
   - **Then** el sistema solicita confirmación y, al confirmarse, desactiva la embarcación

2. **Scenario**: Eliminación iniciada desde la lista (Administrador, sin reservas activas)
   - **Given** el Administrador está en la lista general de embarcaciones del sistema
   - **And** la embarcación seleccionada está en un estado permitido (Disponible o En Mantenimiento/Limpieza) y sin reservas activas
   - **When** elige la opción de eliminar una embarcación desde la lista
   - **Then** el sistema solicita confirmación y, al confirmarse, desactiva la embarcación

3. **Scenario**: Eliminación iniciada desde la lista (Administrador, con reservas activas)
   - **Given** el Administrador está en la lista general de embarcaciones del sistema
   - **And** la embarcación seleccionada está en estado Reservada, con reservas activas
   - **When** elige la opción de eliminar una embarcación desde la lista
   - **Then** el sistema solicita confirmación y, al confirmarse, desactiva la embarcación
   - **And** el sistema gestiona las reservas existentes (cancelación/notificación al cliente)

4. **Scenario**: Eliminación iniciada desde el detalle (Propietario)
   - **Given** el Propietario está en la pantalla de detalle de una de sus embarcaciones
   - **And** la embarcación está en un estado permitido (Disponible o En Mantenimiento/Limpieza) y sin reservas activas ni futuras
   - **When** elige la opción de eliminar desde el detalle
   - **Then** el sistema solicita confirmación y, al confirmarse, desactiva la embarcación

5. **Scenario**: Eliminación iniciada desde el detalle (Administrador, sin reservas activas)
   - **Given** el Administrador está en la pantalla de detalle de una embarcación, al que accedió mediante "Consultar información embarcación".
   - **And** la embarcación está en un estado permitido (Disponible o En Mantenimiento/Limpieza) y sin reservas activas
   - **When** elige la opción de eliminar desde el detalle
   - **Then** el sistema solicita confirmación y, al confirmarse, desactiva la embarcación

6. **Scenario**: Eliminación iniciada desde el detalle (Administrador, con reservas activas)
   - **Given** el Administrador está en la pantalla de detalle de una embarcación, al que accedió mediante "Consultar información embarcación".
   - **And** la embarcación está en estado Reservada, con reservas activas
   - **When** elige la opción de eliminar desde el detalle
   - **Then** el sistema solicita confirmación y, al confirmarse, desactiva la embarcación
   - **And** el sistema gestiona las reservas existentes (cancelación/notificación al cliente)

---

### Edge Cases

- **Eliminación con reservas activas**: el Propietario no puede eliminar si la embarcación tiene reservas futuras o en curso (se bloquea y se muestra mensaje). El Administrador sí puede, y las reservas existentes se gestionan con el Módulo 2 de Reservas (mecanismo pendiente de definir).
- **Eliminación en uso activo**: la eliminación en estado En Navegación está bloqueada para ambos actores por el riesgo operativo y de seguridad inmediato.
- **Irreversibilidad**: una vez eliminada (desactivada), la embarcación no se puede reactivar ni deshacer la eliminación.
- **Pérdida de conexión al confirmar la eliminación**: si la petición de eliminación falla (por ejemplo, por pérdida de conexión), el sistema no desactiva la embarcación, muestra un error y permite reintentar.

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al Propietario eliminar únicamente las embarcaciones que le pertenecen
- **FR-002**: El sistema DEBE permitir al Administrador eliminar la embarcación de cualquier propietario

- **FR-003**: El sistema DEBE interpretar la eliminación como una desactivación (marcado lógico independiente del estado operativo), de modo que la embarcación deja de aparecer en las búsquedas y deja de poder reservarse, conservando sus datos en el sistema
- **FR-004**: El sistema DEBE tratar la eliminación como definitiva e irreversible; la embarcación eliminada NO se puede reactivar
- **FR-005**: El sistema DEBE permitir al Propietario eliminar una embarcación únicamente en estado Disponible o En Mantenimiento/Limpieza
- **FR-006**: El sistema DEBE bloquear al Propietario la eliminación de una embarcación en estado Reservada o En Navegación
- **FR-007**: El sistema DEBE bloquear al Propietario la eliminación de una embarcación que tenga reservas futuras o en curso, mostrando un mensaje que indique que la embarcación tiene reservas activas y no puede eliminarse
- **FR-008**: El sistema DEBE permitir al Administrador eliminar una embarcación en estado Disponible, Reservada o En Mantenimiento/Limpieza
- **FR-009**: El sistema DEBE bloquear al Administrador la eliminación de una embarcación en estado En Navegación, por tratarse de un uso activo en tiempo real con riesgo operativo o de seguridad
- **FR-010**: El sistema DEBE permitir al Administrador eliminar una embarcación aunque tenga reservas activas, y gestionar esas reservas (cancelación y notificación al cliente; el mecanismo exacto se define con el Módulo 2 de Reservas)
- **FR-011**: El sistema DEBE solicitar una confirmación explícita antes de ejecutar la eliminación
- **FR-012**: El sistema DEBE mostrar en la confirmación una advertencia de que la eliminación es definitiva e irreversible, ofreciendo al usuario confirmar o cancelar
- **FR-013**: El sistema DEBE no eliminar la embarcación y permanecer sin cambios si el usuario cancela la confirmación
- **FR-014**: El sistema DEBE mostrar una confirmación de éxito al eliminar la embarcación correctamente
- **FR-015**: El sistema DEBE llevar al usuario a la lista de embarcaciones después de eliminar correctamente una embarcación
- **FR-016**: El sistema DEBE permitir iniciar la eliminación tanto desde la lista de embarcaciones como desde la pantalla de detalle de una embarcación
- **FR-017**: El sistema DEBE aplicar la eliminación a embarcaciones ya creadas; la baja de embarcaciones en estado Borrador se gestiona mediante el caso de uso "Cancelar registro", no con esta funcionalidad

- **FR-018**: El sistema DEBE permitir reintentar la eliminación sin cambios si la petición falla (por ejemplo, por pérdida de conexión); en ese caso no desactiva la embarcación y muestra un error

### Key Entities

- **Embarcación**: Representa una embarcación registrada. En esta funcionalidad se aplica una desactivación lógica (una marca que la retira de búsquedas y de la reserva) y definitiva. Los estados operativos posibles son: Borrador, Disponible, Reservado, En Navegación y En Mantenimiento/Limpieza.
- **Propietario**: Usuario autenticado que posee embarcaciones y solo puede eliminar las suyas, sin reservas activas.
- **Administrador**: Usuario autenticado con permiso para eliminar la embarcación de cualquier propietario, incluida una con reservas activas (en este caso gestionando las reservas con el Módulo 2 de Reservas).


## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: El 100% de las eliminaciones requieren confirmación explícita con advertencia de irreversibilidad antes de ejecutarse

- **SC-002**: El 100% de los intentos de eliminar una embarcación en estado En Navegación son bloqueados por el sistema, para ambos actores
- **SC-003**: El 100% de los intentos de un Propietario de eliminar una embarcación con reservas activas son bloqueados con el mensaje correspondiente
- **SC-004**: El 100% de las embarcaciones eliminadas (desactivadas) dejan de aparecer en las búsquedas y dejan de poder reservarse
- **SC-005**: El 100% de las eliminaciones confirmadas llevan al usuario a la lista de embarcaciones con una confirmación de éxito
