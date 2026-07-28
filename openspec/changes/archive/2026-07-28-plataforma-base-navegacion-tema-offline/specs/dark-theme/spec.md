## ADDED Requirements

### Requirement: Tema visual oscuro por defecto
El sistema SHALL aplicar una paleta de tema oscuro (fondo oscuro, texto claro, acentos de color) en todas las pantallas de la app, de forma inmutable — sin modo claro alternativo en v1, independientemente de la configuración de tema del sistema operativo del dispositivo.

#### Scenario: La app abre en modo oscuro
- **WHEN** cualquier pantalla de la app se renderiza
- **THEN** el sistema aplica la paleta oscura (fondo oscuro, texto claro, acentos de color)

#### Scenario: El tema no varía según la configuración del sistema operativo
- **WHEN** el dispositivo está configurado en modo claro a nivel de sistema operativo
- **THEN** la app mantiene el tema oscuro de todas formas

#### Scenario: Consistencia de acentos de color entre pantallas
- **WHEN** el usuario navega entre Inicio, Historial y Etiquetas
- **THEN** los acentos de color se mantienen consistentes en las 3 pantallas
