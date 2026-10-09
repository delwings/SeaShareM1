# Feature Specification: Enviar a Mantenimiento

**Created:** 07/09/2026

## User Scenarios & Testing

### User Story 1 - Propietario envía embarcación a mantenimiento/limpieza rutinario (Priority: P1)

El Propietario puede enviar su embarcación a mantenimiento después de finalizar un alquiler, para realizar limpieza, revisión o adecuación antes de volver a ponerla disponible. Esta acción es inmediata, sin necesidad de proporcionar información adicional, y el estado de la embarcación cambia automáticamente de estado En navegacion a Mantenimiento.

**Why this priority:** Es la funcionalidad principal y más frecuente del sistema. Sin ella, el Propietario no podría gestionar el mantenimiento rutinario de sus embarcaciones después de cada alquiler.

**Independent Test:** Puede ser probada independientemente enviando una embarcación a mantenimiento desde la cuenta del Propietario y verificando que el estado cambie a Mantenimiento y que la embarcación deje de estar disponible para nuevos alquileres.

**Acceptance Scenarios:**

1. **Scenario:** Propietario envía embarcación a mantenimiento al finalizar un alquiler
   - **Given** que el Propietario tiene una embarcación con estado "En Navegación" cuyo alquiler acaba de finalizar
   - **When** el Propietario hace clic en "Enviar a mantenimiento"
   - **Then** el estado de la embarcación cambia inmediatamente de "En Navegación" a "Mantenimiento", la embarcación deja de estar disponible para nuevos alquileres, y no se envía ninguna notificación.

### User Story 2 - Administrador envía embarcación a mantenimiento con motivo (Priority: P1)

El Administrador puede enviar una embarcación a mantenimiento cuando detecta una situación que requiere atención. Al hacerlo, debe indicar el motivo por el cual la embarcación debe ser retirada del servicio. El Propietario recibe una notificación con el motivo y puede consultarlo en la información de la embarcación.

**Why this priority:** Es una funcionalidad esencial para la gestión operativa. El Administrador necesita controlar el estado de las embarcaciones cuando existen problemas que el Propietario no ha identificado o no ha reportado.

**Independent Test:** Puede ser probada enviando una embarcación a mantenimiento desde la cuenta del Administrador, verificando que el estado cambie, que el Propietario reciba la notificación con el motivo, y que el motivo quede visible en la información de la embarcación.

**Acceptance Scenarios:**

1. **Scenario:** Administrador envía embarcación a mantenimiento
   - **Given** que existe una embarcación con estado "Disponible"
   - **When** el Administrador envía la embarcación a mantenimiento indicando un motivo
   - **Then** el estado de la embarcación cambia a "Mantenimiento", la embarcación deja de estar disponible para nuevos alquileres, y el Propietario recibe una notificación con el motivo indicado.

### User Story 3 - Administrador pone embarcación a Disponible después de mantenimiento (Priority: P2)

Después de que el Administrador envía una embarcación a mantenimiento, solo él puede devolverla a estado Disponible una vez que el problema haya sido resuelto por el Propietario. No existe un límite de tiempo para permanecer en estado Mantenimiento cuando el Administrador inicia la acción.

**Why this priority:** Es la contraparte de la funcionalidad de envío por parte del Administrador. Sin ella, las embarcaciones enviadas a mantenimiento por el Administrador no podrían volver al servicio.

**Independent Test:** Puede ser probada verificando que el Administrador pueda cambiar el estado de Mantenimiento a Disponible, y que el Propietario no pueda realizar esta acción cuando el mantenimiento fue iniciado por el Administrador.

**Acceptance Scenarios:**

1. **Scenario:** Administrador pone embarcación a Disponible
   - **Given** que una embarcación está en estado "Mantenimiento" porque el Administrador la envió
   - **When** el Administrador cambia el estado a "Disponible"
   - **Then** el estado de la embarcación cambia a "Disponible" y vuelve a estar disponible para nuevos alquileres.

2. **Scenario:** Propietario no puede cambiar estado de mantenimiento iniciado por Administrador
   - **Given** que una embarcación está en estado "Mantenimiento" porque el Administrador la envió
   - **When** el Propietario revisa la información de la embarcación
   - **Then** no visualiza la opción de poner la embarcación a Disponible.

### User Story 4 - Propietario pone embarcación a Disponible después de mantenimiento rutinario (Priority: P2)

Cuando el Propietario envía su embarcación a mantenimiento rutinario (después de un alquiler), él mismo puede devolverla a estado Disponible una vez que haya terminado la limpieza, revisión o adecuación. Esta acción se realiza con un botón llamado "Poner a Disponibilidad".

**Why this priority:** Permite que el Propietario gestione de forma autónoma el ciclo de mantenimiento rutinario de sus embarcaciones, sin depender del Administrador.

**Independent Test:** Puede ser probada verificando que el Propietario pueda cambiar el estado de Mantenimiento a Disponible haciendo clic en "Poner a Disponibilidad", y que la embarcación vuelva a estar disponible para alquileres.

**Acceptance Scenarios:**

1. **Scenario:** Propietario pone embarcación a Disponible después de mantenimiento rutinario
   - **Given** que una embarcación está en estado "Mantenimiento" porque el Propietario la envió
   - **When** el Propietario hace clic en "Poner a Disponibilidad"
   - **Then** el estado de la embarcación cambia a "Disponible" y vuelve a estar disponible para nuevos alquileres.

2. **Scenario:** Propietario visualiza botón de disponibilidad solo para mantenimiento propio
   - **Given** que una embarcación está en estado "Mantenimiento"
   - **When** el Propietario revisa la información de la embarcación
   - **Then** visualiza el botón "Poner a Disponibilidad" solo si el mantenimiento fue iniciado por él; si fue iniciado por el Administrador, no visualiza este botón.

## Edge Cases

- ¿Qué ocurre si el Administrador envía a mantenimiento una embarcación que tiene reservas activas del Propietario? → El sistema muestra una alerta y notifica al Propietario de las reservas afectadas.

- ¿Qué ocurre si el Propietario intenta poner a Disponible una embarcación que el Administrador envió a mantenimiento? → El sistema no permite la acción y muestra un mensaje indicando que solo el Administrador puede realizar este cambio.

## Requirements

### Functional Requirements

- **FR-001:** El sistema DEBE permitir al Propietario enviar una embarcación a mantenimiento haciendo clic en "Enviar a mantenimiento".
- **FR-002:** El sistema DEBE cambiar el estado de la embarcación de "En Navegación" (tras finalizar el alquiler) a "Mantenimiento" inmediatamente cuando el Propietario realiza la acción.
- **FR-003:** El sistema DEBE impedir que la embarcación esté disponible para nuevos alquileres mientras esté en estado "Mantenimiento".
- **FR-004:** El sistema DEBE permitir al Administrador enviar una embarcación a mantenimiento indicando un motivo.
- **FR-005:** El sistema DEBE enviar una notificación al Propietario con el motivo cuando el Administrador envía la embarcación a mantenimiento.
- **FR-006:** El sistema DEBE mostrar el motivo del mantenimiento en la información de la embarcación cuando fue iniciado por el Administrador.
- **FR-007:** El sistema DEBE permitir al Propietario cambiar el estado de "Mantenimiento" a "Disponible" usando el botón "Poner a Disponibilidad" solo cuando el mantenimiento fue iniciado por él.
- **FR-008:** El sistema DEBE permitir al Administrador cambiar el estado de "Mantenimiento" a "Disponible" cuando el mantenimiento fue iniciado por él.
- **FR-009:** El sistema DEBE impedir que el Propietario cambie a "Disponible" una embarcación que fue enviada a mantenimiento por el Administrador.
- **FR-010:** El sistema DEBE mostrar una alerta cuando el Propietario envía a mantenimiento una embarcación con reservas activas.
- **FR-011:** El sistema DEBE mantener al Propietario como propietario de la embarcación durante el estado "Mantenimiento"; el Propietario no pierde acceso ni se desvincula de ella.
- **FR-012:** El sistema DEBE permitir al Propietario ver que su embarcación está en estado "Mantenimiento".
- **FR-013:** El sistema DEBE mostrar una alerta cuando el Administrador envía a mantenimiento una embarcación con reservas activas del Propietario.

### Key Entities

- **Embarcación:** Representa la unidad de alquiler. Tiene un estado que puede ser "Disponible", "En Navegación" o "Mantenimiento". El mantenimiento rutinario (Historia 1) se origina típicamente al finalizar un alquiler, es decir, desde el estado "En Navegación". Pertenece a un Propietario y puede tener reservas asociadas.
- **Propietario:** Persona que posee la embarcación. Puede enviar su embarcación a mantenimiento rutinario y ponerla a Disponible después.
- **Administrador:** Persona con permisos para enviar embarcaciones a mantenimiento con motivo y para ponerlas a Disponible después.

## Success Criteria

### Measurable Outcomes

- **SC-001:** El Propietario puede enviar una embarcación a mantenimiento en menos de 5 segundos haciendo clic en un botón.
- **SC-002:** El estado de la embarcación cambia inmediatamente a "Mantenimiento" después de la acción del Propietario.
- **SC-003:** El 100% de las embarcaciones en estado "Mantenimiento" aparecen como no disponibles para nuevos alquileres.
- **SC-004:** El Propietario recibe la notificación con el motivo en el momento en que el Administrador envía la embarcación a mantenimiento.
- **SC-005:** El Propietario puede ver el motivo del mantenimiento en la información de la embarcación el 100% de las veces que fue enviado por el Administrador.
- **SC-006:** El 100% de las embarcaciones enviadas a mantenimiento por el Propietario pueden ser puestas a Disponible por el Propietario usando el botón "Poner a Disponibilidad".
- **SC-007:** El 0% de las embarcaciones enviadas a mantenimiento por el Administrador pueden ser puestas a Disponible por el Propietario.
- **SC-008:** El Propietario puede consultar el estado de su embarcación y ver si está en "Mantenimiento" en todo momento.
