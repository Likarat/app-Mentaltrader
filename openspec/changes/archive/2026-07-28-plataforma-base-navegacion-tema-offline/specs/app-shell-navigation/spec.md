## ADDED Requirements

### Requirement: Navegación principal por pestañas
El sistema SHALL ofrecer una barra de navegación inferior con exactamente 3 destinos: Inicio, Historial y Etiquetas. La app SHALL abrir siempre en Inicio. El ícono del destino activo SHALL mostrarse resaltado (color de acento) frente a los inactivos (color neutro/atenuado). El botón "+" flotante de creación rápida de operación (cuando exista, ver épicas futuras) NO forma parte de esta barra ni SHALL aparecer en la pestaña Etiquetas.

#### Scenario: Apertura de la app en Inicio
- **WHEN** el usuario abre la app y termina de cargar
- **THEN** el sistema muestra la pestaña de Inicio

#### Scenario: Cambio inmediato entre pestañas
- **WHEN** el usuario toca un destino distinto en la barra inferior
- **THEN** el sistema cambia a esa pestaña de forma inmediata

#### Scenario: Resaltado visual del destino activo
- **WHEN** la barra de navegación se renderiza en cualquier pestaña
- **THEN** el ícono del destino activo se muestra con color de acento y los demás en color neutro/atenuado

#### Scenario: Exactamente 3 destinos, sin una cuarta pestaña de Métricas
- **WHEN** la barra de navegación inferior se renderiza
- **THEN** el sistema muestra exactamente 3 destinos (Inicio, Historial, Etiquetas), sin ninguna pestaña adicional
