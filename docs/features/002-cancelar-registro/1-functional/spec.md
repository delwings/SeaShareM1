# Feature Specification: Cancelar Registro

**Created:** 07/09/2026

## User Scenarios & Testing (mandatory)

### User Story 1 - Cancelar registro durante el formulario de datos básicos (Priority: P1)

El propietario decide abandonar el proceso de registro de una embarcación mientras está llenando el primer formulario (datos básicos: nombre, matrícula legal, tipo y capacidad máxima). Al cancelar, el sistema elimina toda la información que el propietario haya ingresado en ese formulario.

**Why this priority:** Es la funcionalidad principal y más básica de cancelar registro. Sin ella, el propietario no tendría forma de descartar un registro que ya no desea completar.

**Independent Test:** Puede ser probada completamente al iniciar un registro de embarcación, ingresar datos en el primer formulario, hacer clic en "Cancelar registro", confirmar la acción y verificar que los datos se eliminan y el sistema regresa a la pantalla principal.

#### Acceptance Scenarios

**1. Scenario: Propietario cancela registro sin datos ingresados**

- **Given** que el propietario acaba de abrir el formulario de datos básicos de la embarcación y no ha ingresado ningún dato
- **When** el propietario hace clic en el botón "Cancelar registro"
- **Then** el sistema muestra un mensaje de confirmación preguntando "¿Está seguro que desea cancelar el registro?"
- **When** el propietario confirma haciendo clic en "Sí"
- **Then** el sistema cierra el formulario sin guardar ningún dato y redirige al propietario a la pantalla principal

**2. Scenario: Propietario cancela registro con datos ingresados**

- **Given** que el propietario está en el formulario de datos básicos y ha ingresado uno o más campos (nombre, matrícula legal, tipo o capacidad máxima)
- **When** el propietario hace clic en el botón "Cancelar registro"
- **Then** el sistema muestra un mensaje de confirmación preguntando "¿Está seguro que desea cancelar el registro?"
- **When** el propietario confirma haciendo clic en "Sí"
- **Then** el sistema elimina permanentemente todos los datos ingresados en el formulario y redirige al propietario a la pantalla principal

**3. Scenario: Propietario decide no cancelar el registro**

- **Given** que el propietario está en el formulario de datos básicos y ha hecho clic en "Cancelar registro"
- **When** el sistema muestra el mensaje de confirmación y el propietario hace clic en "No"
- **Then** el sistema cierra el mensaje de confirmación y el propietario permanece en el formulario con toda la información que había ingresado intacta en los campos

### User Story 2 - Cancelar registro durante el formulario de información complementaria (Priority: P2)

El propietario decide abandonar el proceso de registro mientras está llenando el segundo formulario (información complementaria: nombre del puerto de atraque, latitud, longitud, foto de la embarcación , tarifa base y servicios incluidos), después de haber guardado exitosamente los datos básicos en el primer formulario.

**Why this priority:** Es una funcionalidad necesaria porque el propietario puede decidir no completar el registro después de haber guardado los datos básicos. En este caso, los datos del primer formulario se conservan porque ya fueron guardados.

**Independent Test:** Puede ser probada al completar el primer formulario, guardar los datos (embarcación en estado BORRADOR), abrir el segundo formulario, ingresar información, hacer clic en "Cancelar registro", confirmar y verificar que la información ingresada en el segundo formulario se descarta mientras los datos del primer formulario permanecen guardado.

#### Acceptance Scenarios

**1. Scenario: Propietario cancela segundo formulario con datos ingresados**

- **Given** que el propietario ha completado y guardado el primer formulario (embarcación en estado BORRADOR) y está en el segundo formulario con información del puerto de atraque
- **When** el propietario hace clic en el botón "Cancelar registro"
- **Then** el sistema muestra un mensaje de confirmación preguntando "¿Está seguro que desea cancelar el registro?"
- **When** el propietario confirma haciendo clic en "Sí"
- **Then** el sistema descarta toda la información ingresada en el segundo formulario (nombre del puerto, latitud, longitud, foto y tarifa base), conserva los datos del primer formulario que ya estaban guardados y redirige al propietario a la pantalla principal.

**2. Scenario: Propietario cancela segundo formulario sin datos ingresados**

- **Given** que el propietario ha completado y guardado el primer formulario y acaba de abrir el segundo formulario sin haber ingresado ningún dato
- **When** el propietario hace clic en el botón "Cancelar registro"
- **Then** el sistema muestra un mensaje de confirmación preguntando "¿Está seguro que desea cancelar el registro?"
- **When** el propietario confirma haciendo clic en "Sí"
- **Then** el sistema cierra el formulario, descarta cualquier información ingresada en el segundo formulario y conserva los datos del primer formulario que ya estaban guardados, y redirige al propietario a la pantalla principal.

**3. Scenario: Propietario decide no cancelar el segundo formulario**

- **Given** que el propietario está en el segundo formulario y ha hecho clic en "Cancelar registro"
- **When** el sistema muestra el mensaje de confirmación y el propietario hace clic en "No"
- **Then** el sistema cierra el mensaje de confirmación y el propietario permanece en el formulario con toda la información que había ingresado intacta en los campos

## Edge Cases

- ¿Qué pasa si el sistema pierde conexión con el servidor mientras el propietario está confirmando la cancelación? El sistema debe mostrar un mensaje indicando que no se pudo procesar la cancelación por un problema de conexión y ofrecer la opción de intentar nuevamente.
- ¿Qué pasa si el propietario hace clic múltiples veces rápidamente en el botón "Cancelar registro"? El sistema solo debe mostrar una vez el mensaje de confirmación, no múltiples veces.
- ¿Qué pasa si el propietario cierra el navegador o la aplicación sin confirmar la cancelación? La información permanece en los campos del formulario hasta que el propietario confirme la cancelación o complete el registro.

## Requirements (mandatory)

### Functional Requirements

- **FR-001:** El sistema DEBE mostrar el botón "Cancelar registro" disponible desde el momento en que el propietario entra al formulario de registro (tanto en el primer como en el segundo formulario).
- **FR-002:** El sistema DEBE mostrar un mensaje de confirmación cada vez que el propietario haga clic en "Cancelar registro", con las opciones "Sí" y "No".
- **FR-003:** El sistema DEBE descartar toda la información ingresada en el formulario actual cuando el propietario confirme la cancelación haciendo clic en "Sí".
- **FR-004:** Cuando el propietario confirme la cancelación desde el segundo formulario, el sistema DEBE descartar toda la información que haya ingresado en ese formulario y que aún no haya sido guardada.
- **FR-005:** El sistema DEBE redirigir al propietario a la pantalla principal después de confirmar la cancelación.
- **FR-006:** Cuando el propietario confirme la cancelación desde un formulario, el sistema NO DEBE guardar ninguna información que haya sido ingresada en ese formulario y que aún no estuviera guardada.
- **FR-007:** Si el propietario cancela durante el segundo formulario, el sistema DEBE conservar los datos del primer formulario que ya estaban guardados. La embarcación debe permanecer en estado BORRADOR.
- **FR-008:** Solo el propietario que está creando el registro DEBE tener acceso al botón "Cancelar registro".
- **FR-009:** El sistema DEBE mostrar el mensaje de confirmación con el texto: "¿Está seguro que desea cancelar el registro?"
- **FR-010:** El sistema NO DEBE requerir que el propietario ingrese un motivo para cancelar el registro.

## Key Entities

- **Embarcación:** Representa la embarcación que el propietario está registrando. Tiene dos formularios asociados: uno con datos básicos (nombre, matrícula legal, tipo, capacidad máxima) y otro con información complementaria (puerto de atraque, foto, tarifa base).
- **Propietario:** Persona que posee embarcaciones registradas en el sistema. es quien ejecuta la accion de cancelar registro.

## Success Criteria (mandatory)

### Measurable Outcomes

- **SC-001:** El propietario puede cancelar el registro en menos de 3 segundos (hacer clic, confirmar y ser redirigido).
- **SC-002:** El 100% de los datos ingresados en el formulario se eliminan cuando se confirma la cancelación.
- **SC-003:** El propietario puede identificar claramente la opción de cancelar registro desde el momento en que entra al formulario.
- **SC-004:** El propietario recibe una confirmación clara antes de que se elimine cualquier dato.
- **SC-005:** No se pierde información del primer formulario si el propietario cancela desde el segundo formulario.
