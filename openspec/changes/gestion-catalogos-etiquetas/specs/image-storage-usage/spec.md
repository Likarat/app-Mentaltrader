## ADDED Requirements

### Requirement: Ver espacio en disco usado por imágenes
El sistema SHALL mostrar, al abrir la pestaña Etiquetas, el espacio total aproximado en disco ocupado por todas las imágenes adjuntas a operaciones, recalculado cada vez que se abre esa pestaña.

#### Scenario: Mostrar el espacio total usado
- **WHEN** el usuario, teniendo una o más operaciones con imágenes adjuntas, abre la pestaña de Etiquetas
- **THEN** el sistema muestra el espacio total aproximado en disco ocupado por todas las imágenes

#### Scenario: Ninguna imagen adjuntada todavía
- **WHEN** el usuario abre la pestaña de Etiquetas y ninguna operación tiene imágenes adjuntas
- **THEN** el sistema muestra el espacio usado como 0 (o equivalente), sin mostrar ningún error

#### Scenario: El cálculo se actualiza tras eliminar una operación con imágenes
- **WHEN** el usuario eliminó una operación que tenía imágenes adjuntas (y sus archivos fueron borrados) y vuelve a abrir la pestaña de Etiquetas
- **THEN** el espacio total mostrado refleja la reducción, sin contar los archivos ya eliminados
