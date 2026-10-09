# Feature Specification: Editar Información Embarcación

**Created**: 2026-09-07



## User Scenarios & Testing _(mandatory)_

### User Story 1 - Editar la información de una embarcación propia (Priority: P1)

   El propietario accede a la pantalla de edición de una de sus embarcaciones y modifica los datos que se permiten cambiar. El sistema muestra la información actual de la embarcación, deja editar únicamente los campos permitidos y, al guardar, aplica los cambios de forma inmediata y muestra el resultado en la pantalla de detalle de la embarcación.

**Why this priority**: Es la funcionalidad central que permite al propietario mantener actualizada la información de su embarcación para ofrecerla y administrarla correctamente. Sin ella, los datos de la embarcación quedarían fijos desde el registro.

**Independent Test**: Puede probarse accediendo a la pantalla de edición de una embarcación propia en estado Disponible, modificando los campos permitidos y verificando que los cambios quedan guardados y se reflejan en la pantalla de detalle.

**Acceptance Scenarios**:

1. **Scenario**: Edición guardada correctamente
   - **Given** el propietario está autenticado y tiene una embarcación propia en estado Disponible
   - **When** modifica uno o más de los campos editables y pulsa Guardar
   - **Then** el sistema aplica los cambios de inmediato, muestra una confirmación de éxito y lleva al propietario a la pantalla de detalle de la embarcación con la información actualizada

2. **Scenario**: Edición de un solo campo conservando los demás
   - **Given** el propietario tiene una embarcación propia en estado Disponible con toda su información ya registrada
   - **When** modifica un único campo (por ejemplo, la tarifa base) y no toca los demás campos editables, que están precargados con sus valores actuales
   - **Then** el sistema guarda el nuevo valor del campo modificado y conserva sin cambios los valores de los demás campos editables

3. **Scenario**: Campo editable dejado en blanco conserva su valor anterior
   - **Given** el propietario tiene una embarcación propia en estado Disponible
   - **When** borra el contenido de un campo editable (por ejemplo, la tarifa base) y pulsa Guardar
   - **Then** el sistema conserva el valor anterior de ese campo tal como estaba registrado, sin dejarlo vacío, y guarda la edición

---

### User Story 2 - Restricción de edición según el estado de la embarcación (Priority: P1)

El sistema permite editar la información de una embarcación únicamente cuando la embarcación está en estado **Disponible** o **matenimiento/limpieza**. Si la embarcación está en otro estado, el sistema no permite acceder a la pantalla de edición.

**Why this priority**: Evita que se modifique información de una embarcación que está en uso (reservada, navegando o en borrador), lo que podría afectar reservas o servicios en curso.

**Independent Test**: Puede probarse intentando acceder a la pantalla de edición de una embarcación en cada estado (Disponible, Reservada, En Navegación, En Mantenimiento/Limpieza) y verificando que solo se permite acceder en Disponible y mantenimiento/limpieza.

**Acceptance Scenarios**:

1. **Scenario**: Edición permitida en estado Disponible
   - **Given** una embarcación propia en estado Disponible
   - **When** el propietario quiere editar su información
   - **Then** el sistema permite acceder a la pantalla de edición

2. **Scenario**: Edición permitida en estado mantenimiento/limpieza
   - **Given** una embarcación propia en estado mantenimiento/limpieza
   - **When** el propietario quiere editar su información
   - **Then** el sistema permite acceder a la pantalla de edición

3. **Scenario**: Edición bloqueada en estado Reservada
   - **Given** una embarcación propia en estado Reservada
   - **When** el propietario quiere editar su información
   - **Then** el sistema no permite acceder a la pantalla de edición

4. **Scenario**: Edición bloqueada en estado En Navegación
   - **Given** una embarcación propia en estado En Navegación
   - **When** el propietario quiere editar su información
   - **Then** el sistema no permite acceder a la pantalla de edición

---

### User Story 3 - Salir de la edición sin guardar (Priority: P2)

Cuando el propietario ha realizado cambios en la pantalla de edición y quiere salir sin guardarlos, el sistema le pregunta si desea guardar o descartar los cambios antes de abandonar la pantalla.

**Why this priority**: Evita que el propietario pierda por accidente los cambios que realizó en la edición.

**Independent Test**: Puede probarse modificando algún campo editable y saliendo de la pantalla de edición, verificando que el sistema pregunta si desea guardar o descartar los cambios.

**Acceptance Scenarios**:

1. **Scenario**: Salir de la edición sin guardar
   - **Given** el propietario realizó cambios en la pantalla de edición pero no los ha guardado
   - **When** intenta salir de la pantalla de edición
   - **Then** el sistema pregunta si desea guardar o descartar los cambios

2. **Scenario**: Guardar los cambios al salir
   - **Given** el propietario tiene cambios sin guardar en la pantalla de edición
   - **When** el sistema le pregunta si quiere guardar o descartar y elige Guardar
   - **Then** el sistema guarda los cambios como si hubiera pulsado Guardar en la pantalla de edición

3. **Scenario**: Descartar los cambios al salir
   - **Given** el propietario tiene cambios sin guardar en la pantalla de edición
   - **When** el sistema le pregunta si quiere guardar o descartar y elige Descartar
   - **Then** el sistema descarta los cambios y sale de la pantalla de edición sin guardarlos

---

### Edge Cases

- **Cambio de estado durante la edición**: si el estado operativo de la embarcación cambia (por ejemplo, pasa a Reservado por una nueva reserva, o a un estado bloqueado esperando confirmación de pago) mientras el propietario tiene abierta la pantalla de edición, el sistema revalida el estado al momento de guardar. Si ya no es un estado editable, rechaza el guardado y muestra un mensaje indicando que la embarcación cambió de estado y ya no puede editarse.

- **Pérdida de conexión al guardar**: si la petición de guardado falla (por ejemplo, por pérdida de conexión), el sistema no guarda datos parciales, muestra un error y permite al propietario reintentar sin perder lo ya ingresado en el formulario.

- **Edición concurrente**: si el propietario tiene la misma embarcación abierta en dos sesiones o pestañas y guarda cambios desde ambas, el sistema aplica el último guardado exitoso (last-write-wins), sin combinar ni detectar conflicto entre los cambios de una sesión y otra.

- **Sesión expirada durante la edición**: si la sesión de autenticación del propietario expira mientras tiene abierta la pantalla de edición, el sistema rechaza el guardado y le solicita autenticarse nuevamente, sin perder lo ya ingresado en el formulario.

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: El sistema DEBE permitir que solo el propietario de la embarcación acceda a la edición de su información
- **FR-002**: El sistema NO DEBE permitir que un propietario acceda a la pantalla de edición de una embarcación que no le pertenece
- **FR-003**: El sistema DEBE permitir la edición únicamente cuando la embarcación está en estado Disponible o Mantenimiento/Limpieza
- **FR-004**: El sistema NO DEBE permitir acceder a la pantalla de edición cuando la embarcación está Reservada o En Navegación
- **FR-005**: El sistema DEBE mostrar en la pantalla de edición la información actual de la embarcación
- **FR-006**: El sistema DEBE permitir editar los siguientes campos: nombre de la embarcación, capacidad máxima de pasajeros, tarifa base, puerto de atraque, servicios adicionales y fotografía
- **FR-007**: El sistema DEBE aplicar las mismas reglas de validación que en el registro al editar y guardar: la capacidad debe ser un número entero mayor a 0 que no exceda el máximo según el tipo (Lancha ≤12, Velero ≤15, Catamarán ≤30, Yate ≤40), la tarifa debe ser un valor numérico positivo, y la fotografía debe ser JPG o PNG con tamaño máximo de 5 MB
- **FR-008**: El sistema DEBE mostrar cada campo editable precargado con su valor actual al abrir la pantalla de edición
- **FR-009**: El sistema DEBE conservar el valor actual de todo campo editable que el propietario no modifique al guardar
- **FR-010**: El sistema DEBE conservar la información ya registrada de un campo editable cuando el propietario lo deja en blanco; ningún campo editable se considera obligatorio en la edición
- **FR-011**: El sistema DEBE bloquear el guardado y mostrar un error únicamente cuando un campo editable incumple la regla propia de ese campo (por ejemplo, una tarifa negativa, una capacidad que excede el máximo del tipo, o una fotografía en formato o tamaño no permitido); el hecho de dejar un campo en blanco no bloquea el guardado
- **FR-012**: El sistema DEBE aplicar los cambios de forma inmediata al pulsar Guardar, sin pedir una confirmación previa
- **FR-013**: El sistema DEBE mostrar una confirmación de éxito al guardar la edición correctamente
- **FR-014**: El sistema DEBE llevar al propietario a la pantalla de detalle de la embarcación con la información actualizada después de guardar correctamente
- **FR-015**: El sistema DEBE preguntar al propietario si desea guardar o descartar los cambios cuando intenta salir de la pantalla de edición con cambios sin guardar
- **FR-016**: El sistema DEBE mantener los servicios base (Capitán y Combustible) siempre presentes y sin posibilidad de quitarlos; al editar los servicios solo se pueden modificar los servicios adicionales
- **FR-017**: El sistema DEBE limitar la edición del puerto de atraque mediante la misma selección en un mapa interactivo utilizada en el registro, reemplazando la ubicación guardada por la nueva seleccionada
- **FR-018**: El sistema DEBE aplicar los cambios solo a los datos de la embarcación en este sistema, sin avisar ni actualizar a los módulos externos (reservas, liquidación)
- **FR-019**: El sistema DEBE permitir reintentar el guardado sin perder lo ya ingresado si la petición de guardado falla (por ejemplo, por pérdida de conexión)

### Key Entities

- **Embarcación**: Representa una embarcación registrada. Atributos relevantes para edición: nombre, capacidad máxima de pasajeros, tarifa base, puerto de atraque, servicios adicionales, fotografía, matrícula legal, tipo, estado operativo, fecha de creación. Estados operativos posibles: Borrador, Disponible, Reservado, En Navegación, En Mantenimiento/Limpieza.
- **Propietario**: Usuario autenticado que posee la embarcación. Relación: un propietario posee una o más embarcaciones.
- **Puerto de Atraque**: Ubicación geográfica de la embarcación (nombre, latitud, longitud), obtenida mediante selección en un mapa interactivo.


## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: El propietario puede guardar la edición de una embarcación en menos de 2 minutos
- **SC-002**: El 100% de los intentos de editar una embarcación en estado Reservada o En Navegación son bloqueados por el sistema
- **SC-003**: Ninguna edición guardada deja un campo editable vacío; todo campo que el propietario no modifique o deje en blanco conserva su información ya registrada
- **SC-004**: Los servicios base (Capitán y Combustible) permanecen presentes en el 100% de las embarcaciones editadas
- **SC-005**: El 100% de los guardados exitosos llevan al propietario a la pantalla de detalle de la embarcación con la información actualizada
- **SC-006**: El 100% de los guardados son rechazados si el estado de la embarcación cambió a uno no editable entre la apertura del formulario y el guardado

