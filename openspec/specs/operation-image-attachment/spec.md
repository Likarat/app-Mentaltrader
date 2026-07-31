# operation-image-attachment Specification

## Purpose
TBD - created by archiving change registro-rapido-operaciones. Update Purpose after archive.
## Requirements
### Requirement: Adjuntar imagen sin procesar (cámara o galería, máx. 2)
El sistema SHALL permitir adjuntar hasta 2 imágenes por operación desde cámara o galería, solicitando el permiso correspondiente solo al intentar adjuntar, y SHALL permitir continuar registrando la operación sin imagen si el permiso es rechazado.

#### Scenario: Adjuntar una imagen desde galería o cámara
- **WHEN** el usuario, con menos de 2 imágenes adjuntas, selecciona "Adjuntar imagen" y toma una foto o elige una de la galería
- **THEN** el sistema guarda la referencia de la imagen a la operación y muestra una miniatura sin procesar en el formulario

#### Scenario: Impedir una tercera imagen
- **WHEN** la operación ya tiene 2 imágenes adjuntas y el usuario intenta adjuntar una adicional
- **THEN** el sistema impide adjuntar una tercera imagen

#### Scenario: Permiso de cámara o almacenamiento rechazado
- **WHEN** el usuario, al intentar adjuntar una imagen por primera vez, rechaza el permiso solicitado
- **THEN** el sistema informa que la función no está disponible sin el permiso y permite continuar registrando la operación sin imagen

### Requirement: Comprimir, redimensionar y generar miniatura cacheada al guardar
El sistema SHALL redimensionar (~1600-2000px de ancho) y comprimir (JPEG ~85%, ~200-400KB) cada imagen adjunta, y SHALL generar su miniatura una sola vez al guardar, sin recalcularla en cada render.

#### Scenario: Redimensionar y comprimir al adjuntar
- **WHEN** el usuario adjuntó una imagen sin procesar y el sistema la procesa antes de guardarla
- **THEN** la almacena redimensionada a ~1600-2000px de ancho y comprimida en JPEG ~85% (~200-400KB)

#### Scenario: La miniatura se genera una sola vez al guardar
- **WHEN** el usuario adjuntó una imagen, guardó la operación, y el listado se re-renderiza múltiples veces
- **THEN** el sistema reutiliza la miniatura ya generada al guardar, sin recalcularla en cada render

#### Scenario: Imagen de origen corrupta o ilegible
- **WHEN** el usuario selecciona un archivo de imagen corrupto o en un formato no soportado y el sistema intenta procesarlo
- **THEN** el sistema informa que la imagen no pudo procesarse y no la adjunta, permitiendo intentar con otra

