## ADDED Requirements

### Requirement: Visor de imagen a pantalla completa con zoom y navegación
El sistema SHALL permitir ver, desde la vista de detalle, cualquier imagen adjunta de una operación en pantalla completa, con zoom por gesto de pellizcar/ampliar y navegación entre las imágenes de esa misma operación cuando haya más de una.

#### Scenario: Abrir la imagen en pantalla completa
- **WHEN** una operación tiene una o más imágenes adjuntas, el usuario está en su vista de detalle y toca la imagen
- **THEN** el sistema la muestra en tamaño completo, con fondo negro/oscuro alrededor

#### Scenario: Hacer zoom sobre la imagen
- **WHEN** el usuario está en la vista de imagen a pantalla completa y hace el gesto de pellizcar/ampliar
- **THEN** el sistema aplica el zoom correspondiente sobre la imagen

#### Scenario: Navegar entre imágenes de la misma operación
- **WHEN** una operación tiene 2 imágenes adjuntas y el usuario abre la vista de imagen a pantalla completa
- **THEN** el sistema muestra primero la primera imagen con un indicador de puntos en la parte inferior, y permite avanzar deslizando o tocando una flecha a la derecha

### Requirement: Excepción de orientación solo en el visor
El sistema SHALL permitir la rotación a horizontal únicamente en la vista de imagen a pantalla completa, manteniendo el resto de la app fija en vertical.

#### Scenario: Rotación permitida solo en esta vista
- **WHEN** el usuario está en la vista de imagen a pantalla completa y rota el dispositivo a horizontal
- **THEN** el sistema permite la rotación, a diferencia del resto de la app (que permanece fija en vertical)
