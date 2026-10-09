# Feature Specification: Revisar Información Propietario

**Created**: 2026-09-07

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar el control general de flota fluvial (Priority: P1)

El Administrador autenticado ingresa a "Control General de Flota Fluvial" y consulta el listado de las embarcaciones registradas en la plataforma, con el nombre de la embarcación, su propietario, matrícula, tipo y estado operativo. Desde esa vista dispone de un filtro por propietario, un filtro por estado y un buscador de propietarios.

**Why this priority**: Es la pantalla principal del módulo de administración general y el punto desde donde el Administrador accede a la búsqueda de propietarios y a los detalles.

**Independent Test**: Ingresar como Administrador y verificar que la vista muestra encabezado, título, filtros, buscador y la tabla con las embarcaciones de todos los propietarios, y que al elegir un propietario en el filtro la tabla muestra solo sus embarcaciones.

**Acceptance Scenarios**:

1. **Scenario**: Encabezado de la vista
   - **Given** el Administrador está autenticado
   - **When** ingresa a "Control General de Flota Fluvial"
   - **Then** el sistema muestra en la parte superior el logo "SeaShare", el texto "Módulo de Administración General", y a la derecha un círculo con las iniciales del Administrador, su nombre completo y el rol "Administrador"
2. **Scenario**: Título de la vista
   - **Given** el Administrador ingresa a la vista
   - **When** se carga
   - **Then** el sistema muestra el título "Control General de Flota Fluvial" y el subtítulo "Listado completo de embarcaciones registradas en la plataforma a nivel global."
3. **Scenario**: Listado de embarcaciones
   - **Given** el Administrador ingresa a "Control General de Flota Fluvial"
   - **When** se carga la vista
   - **Then** el sistema muestra una tabla con las embarcaciones de todos los propietarios y las columnas Embarcación, Propietario, Matrícula, Tipo, Estado operativo y Acciones (botón "Ver Detalles")
4. **Scenario**: Estado operativo
   - **Given** el Administrador ve la tabla con embarcaciones
   - **When** observa la columna "Estado operativo"
   - **Then** cada embarcación muestra el nombre de su estado operativo actual dentro de una etiqueta con un punto de color, y cada estado tiene su propio color (por ejemplo, "Disponible" en verde y "En Mantenimiento" en amarillo)
5. **Scenario**: Botón "Ver Detalles"
   - **Given** el Administrador ve la tabla con embarcaciones
   - **When** observa la columna "Acciones"
   - **Then** cada fila muestra un botón "Ver Detalles"
6. **Scenario**: Controles de la vista
   - **Given** el Administrador ingresa a "Control General de Flota Fluvial"
   - **When** se carga la vista
   - **Then** el sistema muestra encima de la tabla un filtro con el texto "Filtrar por Propietario...", un filtro con el texto "Estado: Todos" y, a la derecha, un buscador con el texto "Buscar propietario por nombre o cédula..."
7. **Scenario**: Opciones del filtro por propietario
   - **Given** el Administrador está en "Control General de Flota Fluvial"
   - **When** abre el filtro "Filtrar por Propietario..."
   - **Then** el sistema despliega la opción "Todos los propietarios" y una opción por cada propietario, y marca con un check la opción seleccionada
8. **Scenario**: Filtrar por un propietario
   - **Given** el filtro por propietario está desplegado
   - **When** el Administrador selecciona un propietario
   - **Then** el campo del filtro muestra el nombre de ese propietario y la tabla muestra solo las embarcaciones de ese propietario
9. **Scenario**: Buscador de propietarios
   - **Given** el Administrador está en "Control General de Flota Fluvial"
   - **When** utiliza el buscador "Buscar propietario por nombre o cédula..."
   - **Then** el comportamiento es el definido en los escenarios de búsqueda de propietario de la User Story 2

---

### User Story 2 - Consultar la información de un propietario (Priority: P1)

El Administrador busca propietarios por nombre, correo o documento, ve una lista de resultados y selecciona uno para consultar su información en detalle. La funcionalidad es de solo lectura: no permite modificar ningún dato del propietario.

**Why this priority**: Es la funcionalidad que permite al Administrador identificar y revisar la información de los propietarios del sistema, útil como apoyo en otras gestiones (por ejemplo, identificar al responsable de una embarcación).

**Independent Test**: Puede probarse buscando un propietario por nombre, correo o documento, viendo la lista de resultados y entrando al detalle de un propietario para ver su información completa.

**Acceptance Scenarios**:

1. **Scenario**: Búsqueda de propietario por nombre
   - **Given** el Administrador está autenticado
   - **When** busca propietarios ingresando un nombre como criterio
   - **Then** el sistema muestra una lista de resultados con los propietarios que coinciden, mostrando nombre, correo y teléfono
2. **Scenario**: Búsqueda de propietario por correo
   - **Given** el Administrador está autenticado
   - **When** busca propietarios ingresando un correo como criterio
   - **Then** el sistema muestra una lista de resultados con los propietarios que coinciden, mostrando nombre, correo y teléfono
3. **Scenario**: Búsqueda de propietario por documento
   - **Given** el Administrador está autenticado
   - **When** busca propietarios ingresando un documento de identidad como criterio
   - **Then** el sistema muestra una lista de resultados con los propietarios que coinciden, mostrando nombre, correo y teléfono
4. **Scenario**: Consulta del detalle de un propietario
   - **Given** el Administrador vio la lista de resultados
   - **When** selecciona un propietario de la lista
   - **Then** el sistema muestra la información completa de ese propietario: nombre, correo, teléfono, documento, fecha de registro y estado de cuenta
5. **Scenario**: Búsqueda sin resultados
   - **Given** el Administrador está autenticado
   - **When** busca con un criterio que no coincide con ningún propietario
   - **Then** el sistema muestra un mensaje indicando que no se encontraron propietarios con ese criterio
6. **Scenario**: Búsqueda sin criterio
   - **Given** el Administrador está en la pantalla de búsqueda de propietarios
   - **When** intenta buscar sin ingresar ningún criterio (ni nombre, ni correo, ni documento)
   - **Then** el sistema no permite buscar y le solicita ingresar al menos un dato de búsqueda

---

### Edge Cases

- **Pérdida de conexión durante la búsqueda**: si la petición de búsqueda falla (por ejemplo, por pérdida de conexión), el sistema muestra un error y permite al Administrador reintentar.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al Administrador buscar propietarios por nombre
- **FR-002**: El sistema DEBE permitir al Administrador buscar propietarios por correo
- **FR-003**: El sistema DEBE permitir al Administrador buscar propietarios por documento de identidad
- **FR-004**: El sistema DEBE permitir al Administrador revisar la información de cualquier propietario del sistema
- **FR-005**: El sistema DEBE mostrar una lista de resultados con los propietarios que coinciden con el criterio de búsqueda, mostrando para cada uno: nombre, correo y teléfono
- **FR-006**: El sistema DEBE mostrar la información completa de un propietario al seleccionarlo de la lista: nombre, correo, teléfono, documento, fecha de registro y estado de cuenta
- **FR-007**: El sistema DEBE ser de solo lectura; NO DEBE permitir al Administrador modificar ningún dato del propietario en esta funcionalidad
- **FR-008**: El sistema DEBE solicitar al Administrador ingresar al menos un criterio de búsqueda y NO permitir buscar sin haberlo ingresado
- **FR-009**: El sistema DEBE mostrar un mensaje indicando que no se encontraron propietarios cuando el criterio de búsqueda no arroja resultados
- **FR-010**: El sistema DEBE mostrar un error y permitir reintentar si la petición de búsqueda falla (por ejemplo, por pérdida de conexión)
- **FR-011**: El sistema DEBE mostrar en "Control General de Flota Fluvial" un encabezado con el logo "SeaShare", el texto "Módulo de Administración General" y, a la derecha, las iniciales, el nombre completo y el rol "Administrador" del usuario autenticado
- **FR-012**: El sistema DEBE mostrar el título "Control General de Flota Fluvial" y el subtítulo "Listado completo de embarcaciones registradas en la plataforma a nivel global."
- **FR-013**: El sistema DEBE mostrar una tabla con las embarcaciones de todos los propietarios, con las columnas Embarcación, Propietario, Matrícula, Tipo, Estado operativo y Acciones ("Ver Detalles")
- **FR-014**: El sistema DEBE mostrar el estado operativo actual de cada embarcación con su nombre dentro de una etiqueta con un punto de color, con un color propio para cada estado
- **FR-015**: El sistema DEBE mostrar encima de la tabla el filtro "Filtrar por Propietario...", el filtro "Estado: Todos" y el buscador "Buscar propietario por nombre o cédula..."
- **FR-016**: El sistema DEBE ofrecer en el filtro por propietario la opción "Todos los propietarios" y una opción por cada propietario, permitir seleccionar una sola opción a la vez y marcar con un check la opción seleccionada
- **FR-017**: El sistema DEBE mostrar en la tabla solo las embarcaciones del propietario seleccionado en el filtro, y mostrar el nombre de ese propietario en el campo del filtro

### Key Entities

- **Propietario**: Usuario del sistema dueño de embarcaciones. Atributos consultables en esta funcionalidad: nombre, correo, teléfono, documento de identidad, fecha de registro y estado de cuenta. Su registro de cuenta se define en una especificación aparte.
- **Administrador**: Usuario autenticado con permiso para consultar la información de cualquier propietario del sistema, sin poder modificarla.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El Administrador puede encontrar a un propietario y ver su información completa en menos de 1 minuto
- **SC-002**: El 100% de las búsquedas se pueden realizar por nombre, correo o documento
- **SC-003**: El 100% de las revisiones del detalle muestran todos los datos del propietario (nombre, correo, teléfono, documento, fecha de registro y estado de cuenta)
- **SC-004**: El 100% de las búsquedas sin criterio son bloqueadas con la solicitud correspondiente
- **SC-005**: El 100% de las búsquedas sin resultados muestran el mensaje correspondiente
- **SC-006**: El 100% de las consultas son de solo lectura; ningún dato del propietario puede modificarse desde esta funcionalidad
