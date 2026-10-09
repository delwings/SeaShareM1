# Feature Specification: Registrar Embarcación

Created: 2026-09-05 | Updated: 2026-10-09

## User Scenarios & Testing (mandatory)

### User Story 1 - Consultar mis embarcaciones registradas (Priority: P1)

El propietario autenticado ingresa a "Mis Embarcaciones Registradas" y consulta la lista de sus embarcaciones con nombre, matrícula, tipo y estado operativo. Desde esa vista puede pulsar "Ver Detalles" en una embarcación o pulsar "Registrar Nueva Embarcación".

**Why this priority:** Es la pantalla de entrada del módulo de gestión de flota y el punto desde donde se inicia el registro.

**Independent Test:** Ingresar con un propietario que tenga embarcaciones y verificar que la vista muestra encabezado, título, botón y tabla con sus embarcaciones, la de registro completado más reciente primero.

**Acceptance Scenarios:**

- **Scenario: Encabezado de la vista**
  - Given el propietario está autenticado
  - When ingresa a "Mis Embarcaciones Registradas"
  - Then el sistema muestra en la parte superior el logo "SeaShare", el texto "Módulo de Gestión de Flota", y a la derecha un círculo con las iniciales del propietario, su nombre completo y el rol "Propietario"
- **Scenario: Título y botón de la vista**
  - Given el propietario ingresa a la vista
  - When se carga
  - Then el sistema muestra el título "Mis Embarcaciones Registradas", el subtítulo "Gestiona el estado operativo y consulta los detalles de tus vehículos fluviales" y el botón "Registrar Nueva Embarcación"
- **Scenario: Listado con embarcaciones**
  - Given el propietario tiene al menos una embarcación en estado distinto de Borrador
  - When ingresa a "Mis Embarcaciones Registradas"
  - Then el sistema muestra solo las embarcaciones de ese propietario en una tabla con las columnas Nombre de la embarcación, Matrícula, Tipo, Estado operativo y Acciones (botón "Ver Detalles")
- **Scenario: Estado operativo**
  - Given el propietario ve la tabla con embarcaciones
  - When observa la columna "Estado operativo"
  - Then cada embarcación muestra el nombre de su estado operativo actual dentro de una etiqueta con un punto de color, y cada estado tiene su propio color (por ejemplo, "Disponible" en verde y "En Mantenimiento" en amarillo)
- **Scenario: Botón "Ver Detalles"**
  - Given el propietario ve la tabla con embarcaciones
  - When observa la columna "Acciones"
  - Then cada fila muestra un botón "Ver Detalles"
- **Scenario: Orden del listado**
  - Given el propietario tiene varias embarcaciones registradas
  - When visualiza la tabla
  - Then las filas se ordenan por fecha de finalización del registro, de la más reciente (primera fila) a la más antigua (última fila)
- **Scenario: Etiqueta "NUEVA"**
  - Given el propietario tiene varias embarcaciones registradas
  - When visualiza el listado
  - Then solo la embarcación cuyo registro se completó más recientemente muestra la etiqueta "NUEVA" junto a su nombre; al completarse otro registro, la etiqueta pasa a la nueva embarcación y desaparece de la anterior
- **Scenario: Listado sin embarcaciones**
  - Given el propietario no tiene embarcaciones registradas
  - When ingresa al listado
  - Then el sistema muestra el mensaje "Sin embarcaciones registradas" y el botón "Registrar Nueva Embarcación"
- **Scenario: Registrar nueva embarcación**
  - Given el propietario está en la vista
  - When pulsa "Registrar Nueva Embarcación"
  - Then el sistema aplica los escenarios "Registrar sin Borrador existente" o "Borrador detectado" de la User Story 4, según corresponda

### User Story 2 - Registrar datos básicos (Paso 1 de 2) (Priority: P1)

El propietario pulsa "Registrar Nueva Embarcación" en el listado "Mis Embarcaciones Registradas" y completa el Paso 1 "Datos Básicos": nombre, matrícula legal, tipo y capacidad máxima de pasajeros. Al pulsar "Siguiente", la embarcación se guarda en estado Borrador y el sistema avanza al Paso 2.

**Why this priority:** Es el primer paso del registro; sin él no se puede continuar.

**Independent Test:** Completar el Paso 1 con datos válidos y verificar que la embarcación queda guardada en estado Borrador y se muestra el Paso 2.

**Acceptance Scenarios:**

- **Scenario: Datos básicos válidos**
  - Given el propietario está autenticado, no tiene un Borrador y está en el Paso 1
  - When completa nombre, matrícula con formato CP-NN-NNNN-X, tipo y capacidad válida, y pulsa "Siguiente"
  - Then el sistema guarda la embarcación en estado Borrador asociada a ese propietario, marca con ✓ "Datos Básicos" en el indicador de pasos y muestra el Paso 2
- **Scenario: Matrícula con formato inválido**
  - Given el propietario está en el Paso 1
  - When ingresa una matrícula que no sigue el formato CP-NN-NNNN-X y pulsa "Siguiente"
  - Then el sistema muestra un error con el formato correcto y no avanza
- **Scenario: Campos obligatorios vacíos**
  - Given el propietario está en el Paso 1
  - When pulsa "Siguiente" con algún campo vacío
  - Then el sistema resalta los campos faltantes y no avanza
- **Scenario: Matrícula ya registrada**
  - Given el propietario está en el Paso 1
  - When ingresa una matrícula que ya existe en el sistema y pulsa "Siguiente"
  - Then el sistema muestra un error indicando que la matrícula ya está registrada y no avanza
- **Scenario: Capacidad excede el máximo del tipo**
  - Given el propietario está en el Paso 1
  - When ingresa una capacidad mayor al máximo del tipo seleccionado (Lancha: 12, Velero: 15, Catamarán: 30, Yate: 40)
  - Then el sistema muestra un error indicando el máximo permitido para ese tipo y no avanza
- **Scenario: Cancelar en el Paso 1**
  - Given el propietario está en el Paso 1
  - When pulsa "Cancelar"
  - Then el comportamiento es el definido en la especificación "Cancelar Registro"

### User Story 3 - Completar puerto, tarifa, servicios y foto (Paso 2 de 2) (Priority: P1)

En el Paso 2 "Datos Complementarios", el propietario coloca una etiqueta en el mapa para definir el puerto de atraque, ingresa la tarifa base por hora en COP, selecciona servicios adicionales (opcional) y adjunta la fotografía oficial. Al pulsar "Guardar Registro", la embarcación pasa de Borrador a Disponible.

**Why this priority:** Sin esta información la embarcación no puede ubicarse, cotizarse ni ofrecerse.

**Independent Test:** Sobre una embarcación en Borrador, completar el Paso 2 y verificar que pasa a Disponible y aparece en el listado.

**Acceptance Scenarios:**

- **Scenario: Puerto seleccionado en el mapa**
  - Given el propietario está en el Paso 2
  - When coloca una etiqueta en el mapa
  - Then el sistema obtiene el nombre del puerto, la latitud y la longitud de ese punto y los muestra en una etiqueta con el texto "Nombre — Lat: X° N, Long: Y° W (Seleccionado)"
- **Scenario: Cambiar la ubicación seleccionada**
  - Given ya hay una etiqueta colocada en el mapa
  - When el propietario coloca la etiqueta en otro punto
  - Then el sistema reemplaza la ubicación anterior por la nueva y actualiza nombre, latitud y longitud
- **Scenario: Servicios base**
  - Given el propietario está en el Paso 2
  - When visualiza la sección "Servicios base obligatorios (incluidos)"
  - Then Capitán y Combustible aparecen como elementos no editables con el texto "Estos servicios vienen predeterminados y no se pueden remover", y quedan asociados a la embarcación aunque no seleccione servicios adicionales
- **Scenario: Servicios adicionales**
  - Given el propietario está en el Paso 2
  - When marca cero o más servicios adicionales (Chalecos salvavidas, Nevera con hielo, Equipo de sonido, Equipo de pesca, Equipo de buceo)
  - Then los servicios marcados quedan asociados a la embarcación además de los base
- **Scenario: Tarifa inválida**
  - Given el propietario está en el Paso 2
  - When ingresa una tarifa no numérica, cero o negativa y pulsa "Guardar Registro"
  - Then el sistema muestra un error indicando que la tarifa debe ser un valor numérico positivo y no guarda
- **Scenario: Foto con formato no permitido**
  - Given el propietario está adjuntando la fotografía
  - When selecciona un archivo que no es JPG ni PNG
  - Then el sistema muestra un error indicando los formatos aceptados y no adjunta el archivo
- **Scenario: Foto excede el tamaño máximo**
  - Given el propietario está adjuntando la fotografía
  - When selecciona una imagen mayor a 10 MB
  - Then el sistema muestra un error indicando el tamaño máximo permitido y no adjunta el archivo
- **Scenario: Campos obligatorios incompletos**
  - Given el propietario está en el Paso 2
  - When pulsa "Guardar Registro" sin puerto seleccionado, sin tarifa o sin foto
  - Then el sistema resalta lo faltante y no guarda
- **Scenario: Registro completado**
  - Given el propietario completó puerto, tarifa y foto en el Paso 2
  - When pulsa "Guardar Registro"
  - Then el sistema guarda puerto, tarifa, servicios y foto, cambia el estado de Borrador a Disponible y muestra el modal "¡Embarcación registrada!" con el texto "El nuevo vehículo fluvial ha sido registrado con éxito y se encuentra disponible en tu flota."
- **Scenario: Aceptar confirmación**
  - Given se muestra el modal "¡Embarcación registrada!"
  - When el propietario pulsa "Aceptar"
  - Then el sistema vuelve al listado "Mis Embarcaciones Registradas", donde la embarcación aparece como Disponible con la etiqueta "NUEVA"
- **Scenario: Cancelar en el Paso 2**
  - Given el propietario está en el Paso 2
  - When pulsa "Cancelar"
  - Then el sistema muestra dos opciones: "Continuar registro" (permanece en el Paso 2 con lo ya ingresado) y "Salir" (vuelve al listado y conserva el Borrador)

### User Story 4 - Gestión del Borrador (Priority: P1)

Si el propietario intenta registrar una nueva embarcación teniendo un Borrador pendiente, el sistema le permite continuarlo o reemplazarlo.

**Why this priority:** Define el punto de entrada al registro y evita perder o duplicar borradores.

**Independent Test:** Con un Borrador existente, pulsar "Registrar Nueva Embarcación" y verificar los dos caminos (continuar o reemplazar).

**Acceptance Scenarios:**

- **Scenario: Registrar sin Borrador existente**
  - Given el propietario no tiene un Borrador
  - When pulsa "Registrar Nueva Embarcación"
  - Then el sistema muestra el Paso 1 vacío
- **Scenario: Borrador detectado**
  - Given el propietario tiene un Borrador
  - When pulsa "Registrar Nueva Embarcación"
  - Then el sistema muestra el modal "Borrador detectado" con el texto "Tienes un registro incompleto guardado bajo el nombre:", el nombre del Borrador y las opciones "Empezar un registro nuevo", "Continuar registro pendiente" y cerrar (X)
- **Scenario: Continuar registro pendiente**
  - Given se muestra el modal "Borrador detectado"
  - When el propietario pulsa "Continuar registro pendiente"
  - Then el sistema muestra el Paso 2 del Borrador
- **Scenario: Cerrar el modal**
  - Given se muestra el modal "Borrador detectado"
  - When el propietario pulsa la X
  - Then el modal se cierra, el Borrador se conserva y el propietario permanece en el listado
- **Scenario: Empezar un registro nuevo**
  - Given se muestra el modal "Borrador detectado"
  - When el propietario pulsa "Empezar un registro nuevo"
  - Then el sistema muestra el modal "¿Iniciar nuevo registro?" con el texto "Si inicias un formulario nuevo, el borrador actual se eliminará de forma permanente y no podrás recuperar la información diligenciada." y las opciones "Regresar" y "Sí, eliminar y empezar"
- **Scenario: Confirmar reemplazo del Borrador**
  - Given se muestra el modal "¿Iniciar nuevo registro?"
  - When el propietario pulsa "Sí, eliminar y empezar"
  - Then el sistema elimina el Borrador de forma permanente, libera su matrícula y muestra el Paso 1 vacío
- **Scenario: Regresar desde la confirmación**
  - Given se muestra el modal "¿Iniciar nuevo registro?"
  - When el propietario pulsa "Regresar"
  - Then el sistema vuelve al modal "Borrador detectado" sin eliminar nada

## Requirements (mandatory)

### Functional Requirements

**Listado y punto de entrada**

- **FR-001:** El sistema DEBE mostrar en "Mis Embarcaciones Registradas" solo las embarcaciones del propietario autenticado que no estén en estado Borrador, con las columnas Nombre de la embarcación, Matrícula, Tipo, Estado operativo y Acciones ("Ver Detalles")
- **FR-002:** El sistema DEBE mostrar "Sin embarcaciones registradas" cuando el propietario no tenga embarcaciones fuera de estado Borrador
- **FR-003:** El sistema DEBE mostrar la etiqueta "NUEVA" únicamente en la embarcación cuyo registro se completó más recientemente, retirándola de la anterior cuando otra embarcación complete su registro
- **FR-004:** El sistema DEBE permitir un único Borrador por propietario
- **FR-032:** El sistema DEBE mostrar en la vista un encabezado con el logo "SeaShare", el texto "Módulo de Gestión de Flota" y, a la derecha, las iniciales, el nombre completo y el rol "Propietario" del usuario autenticado
- **FR-033:** El sistema DEBE mostrar el título "Mis Embarcaciones Registradas", el subtítulo "Gestiona el estado operativo y consulta los detalles de tus vehículos fluviales" y el botón "Registrar Nueva Embarcación"
- **FR-034:** El sistema DEBE mostrar el estado operativo actual de cada embarcación con su nombre dentro de una etiqueta con un punto de color, con un color propio para cada estado
- **FR-035:** El sistema DEBE ordenar el listado por fecha de finalización del registro, de la más reciente a la más antigua

**Paso 1 - Datos Básicos**

- **FR-005:** El sistema DEBE mostrar el Paso 1 con los campos: nombre de la embarcación, matrícula legal, tipo de embarcación y capacidad máxima (pasajeros), y un indicador de pasos "1 Datos Básicos — 2 Datos Complementarios"
- **FR-006:** El sistema DEBE validar que la matrícula tenga el formato CP-NN-NNNN-X, donde CP es la sigla fija de Capitanía de Puerto, NN el número de capitanía (00-99), NNNN el consecutivo (0000-9999) y X una letra mayúscula
- **FR-007:** El sistema DEBE ofrecer como tipos de embarcación: Velero, Lancha, Yate, Catamarán
- **FR-008:** El sistema DEBE validar que la capacidad sea un entero mayor a 0 y no exceda el máximo del tipo: Lancha ≤12, Velero ≤15, Catamarán ≤30, Yate ≤40
- **FR-009:** El sistema DEBE validar que los cuatro campos del Paso 1 estén completos antes de permitir "Siguiente"
- **FR-010:** El sistema DEBE, al pulsar "Siguiente" con datos válidos, guardar la embarcación en estado Borrador asociada al propietario autenticado, registrar la fecha y hora de creación y mostrar el Paso 2 con "Datos Básicos" marcado con ✓
- **FR-011:** El sistema DEBE rechazar una matrícula ya existente, incluso en solicitudes simultáneas (restricción de unicidad a nivel de base de datos), con un mensaje específico de matrícula ya registrada
- **FR-012:** El sistema NO DEBE exigir que el nombre de la embarcación sea único

**Paso 2 - Datos Complementarios**

- **FR-013:** El sistema DEBE mostrar un mapa interactivo, sin barra de búsqueda, donde el propietario coloca una etiqueta para definir el puerto de atraque
- **FR-014:** El sistema DEBE obtener y guardar el nombre del puerto, la latitud y la longitud del punto donde se colocó la etiqueta, y mostrarlos en la etiqueta con el estado "Seleccionado"
- **FR-015:** El sistema DEBE permitir reubicar la etiqueta, conservando siempre una única ubicación
- **FR-016:** El sistema DEBE permitir ingresar la tarifa base por hora en COP, sin mínimo ni máximo, y validar que sea un valor numérico mayor a 0
- **FR-017:** El sistema DEBE asociar Capitán y Combustible como servicios base a toda embarcación, mostrarlos como elementos no editables y no permitir removerlos
- **FR-018:** El sistema DEBE permitir seleccionar de forma opcional servicios adicionales, sin costo extra para el arrendatario: Chalecos salvavidas, Nevera con hielo, Equipo de sonido, Equipo de pesca, Equipo de buceo
- **FR-019:** El sistema DEBE permitir adjuntar una fotografía JPG o PNG de hasta 10 MB
- **FR-020:** El sistema DEBE validar que puerto, tarifa y foto estén completos antes de permitir "Guardar Registro"
- **FR-021:** El sistema DEBE, al pulsar "Guardar Registro" con datos válidos, guardar puerto, tarifa, servicios y foto en una sola operación y cambiar el estado de Borrador a Disponible
- **FR-022:** El sistema DEBE mostrar tras el guardado el modal "¡Embarcación registrada!" con el botón "Aceptar", que devuelve al listado
- **FR-023:** El sistema DEBE mostrar, al pulsar "Cancelar" en el Paso 2, las opciones "Continuar registro" y "Salir"; "Salir" vuelve al listado conservando el Borrador
- **FR-024:** El sistema NO DEBE ofrecer botón "Atrás" en el Paso 2
- **FR-025:** El comportamiento de "Cancelar" en el Paso 1 DEBE ser el definido en la especificación "Cancelar Registro"

**Borrador**

- **FR-026:** El sistema DEBE mostrar el modal "Borrador detectado" al pulsar "Registrar Nueva Embarcación" cuando exista un Borrador, con el nombre del Borrador y las opciones "Continuar registro pendiente", "Empezar un registro nuevo" y cerrar (X)
- **FR-027:** El sistema DEBE llevar al Paso 2 al elegir "Continuar registro pendiente"
- **FR-028:** El sistema DEBE mostrar el modal "¿Iniciar nuevo registro?" al elegir "Empezar un registro nuevo"; "Sí, eliminar y empezar" elimina el Borrador de forma permanente, libera su matrícula y muestra el Paso 1 vacío; "Regresar" vuelve al modal "Borrador detectado"
- **FR-029:** El sistema DEBE eliminar automáticamente todo Borrador que supere 10 días desde su creación y liberar su matrícula
- **FR-030:** El sistema DEBE cambiar una embarcación de Borrador a Disponible únicamente cuando estén completos datos básicos, puerto de atraque, tarifa y foto

**Errores de envío**

- **FR-031:** El sistema NO DEBE guardar datos parciales si falla el envío de un paso (por ejemplo, pérdida de conexión); DEBE mostrar un error y conservar lo ingresado en el formulario para reintentar

### Key Entities

- **Embarcación:** Atributos: nombre, matrícula legal (única), tipo, capacidad máxima de pasajeros, tarifa base por hora (COP), fotografía, fecha de creación, fecha de finalización del registro, estado. Pertenece a un único propietario. Estados en esta funcionalidad: Borrador → Disponible.
- **Puerto de Atraque:** Ubicación de la embarcación, obtenida al colocar una etiqueta en el mapa. Atributos: nombre del puerto, latitud, longitud.
- **Servicio:** Inclusión sin costo adicional para el arrendatario. Base (Capitán, Combustible): asignados automáticamente y no removibles. Adicionales (Chalecos salvavidas, Nevera con hielo, Equipo de sonido, Equipo de pesca, Equipo de buceo): seleccionados de forma opcional por el propietario.
- **Propietario:** Usuario autenticado que registra la embarcación (su cuenta se define en otra especificación). Puede registrar múltiples embarcaciones, pero tener un solo Borrador a la vez.

## Success Criteria (mandatory)

### Measurable Outcomes

- **SC-001:** El propietario puede completar el registro de una embarcación (Paso 1 y Paso 2) en menos de 3 minutos
- **SC-002:** El 100% de las matrículas registradas cumplen el formato CP-NN-NNNN-X y son únicas en el sistema
- **SC-003:** Una embarcación solo pasa de Borrador a Disponible cuando tiene datos básicos, puerto de atraque, tarifa y foto completos
