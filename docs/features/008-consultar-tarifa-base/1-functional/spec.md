# Feature Specification: Consultar Tarifa Base
Created: 2026-09-08

## User Scenarios & Testing *(mandatory)*

### User Story 1 - El módulo de liquidación consulta la tarifa base de una embarcación (Priority: P1)

El módulo de liquidación necesita consultar de forma automática y rápida la tarifa base configurada para una embarcación totalmente registrada y activa con el fin de realizar los cálculos económicos y procesar los pagos o cobros correspondientes.

**Why this priority**: Es un dato financiero crítico para el funcionamiento de las transacciones de liquidación y pagos dentro de la plataforma.

**Independent Test**: Puede probarse simulando una consulta desde el módulo de liquidación hacia una embarcación existente y disponible, verificando que el sistema devuelva exactamente el valor numérico de la tarifa base registrado.

**Acceptance Scenarios**:

1. **Scenario**: Consulta exitosa de la tarifa base por parte del módulo de liquidación
   - **Given** una embarcación cuenta con su registro completo y una tarifa base definida
   - **When** el módulo de liquidación solicita la tarifa base de dicha embarcación
   - **Then** el sistema retorna el valor numérico exacto de la tarifa asociada

---

### Edge Cases

- ¿Qué pasa si el módulo de liquidación intenta consultar la tarifa de una embarcación que no existe? El sistema rechaza la consulta indicando que la embarcación no está activa ni disponible para operaciones.
- ¿Qué pasa si hay fallas de comunicación en la red al momento de consultar la tarifa? El módulo de liquidación implementa un tiempo de espera (*timeout*) y reintenta la solicitud para evitar errores en el procesamiento de los cálculos.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE proveer un mecanismo interno seguro para que el Módulo de Liquidación pueda consultar la tarifa base de una embarcación activa.
- **FR-002**: El sistema DEBE retornar el valor numérico exacto y positivo de la tarifa base asociada a la embarcación solicitada.

### Key Entities

- **Embarcación**: Objeto que almacena el atributo de tarifa base consultado por el sistema de liquidación, aplicable únicamente a embarcaciones con registro finalizado.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las consultas de tarifa base solicitadas por el módulo de liquidación sobre embarcaciones activas se responden en menos de 1 segundo.
- **SC-002**: Se garantiza que el valor de la tarifa base devuelto coincide exactamente con el configurado por el propietario en el módulo de registro.