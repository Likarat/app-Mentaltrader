## ADDED Requirements

### Requirement: Orientación fija en vertical en el shell de la app
El sistema SHALL mantener la orientación de pantalla fija en vertical en Inicio, Historial y Etiquetas, rechazando cualquier intento de forzar horizontal en esas pantallas. La única excepción prevista (visor de imagen a pantalla completa) pertenece a una capability futura (EP-003/HU-019, no construida en este slice) y no está cubierta aquí.

#### Scenario: Orientación vertical en el uso general
- **WHEN** el usuario rota el dispositivo mientras navega por Inicio, Historial o Etiquetas
- **THEN** el sistema mantiene la orientación vertical

#### Scenario: Rechazo de forzado horizontal
- **WHEN** el sistema operativo o un gesto intenta forzar la orientación horizontal en cualquier pantalla del shell
- **THEN** el sistema rechaza el cambio y permanece en vertical

#### Scenario: Contrato de excepción reservado para una capability futura
- **WHEN** una capability futura (visor de imagen, EP-003/HU-019) introduzca una pantalla que permita rotación horizontal
- **THEN** el resto del shell (Inicio/Historial/Etiquetas) SHALL seguir fijo en vertical al volver a él, sin que esta capability necesite cambios
