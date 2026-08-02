## ADDED Requirements

### Requirement: Vista de detalle completo de una operación
El sistema SHALL abrir, al tocar una tarjeta del listado, la vista de detalle completo de esa operación, mostrando primero la imagen (si existe) y luego los campos organizados en secciones con encabezados claros, terminando con la Descripción de entrada al final.

#### Scenario: Abrir detalle desde la tarjeta
- **WHEN** el usuario ve una tarjeta en el listado y la toca
- **THEN** el sistema abre la vista de detalle completo de esa operación

#### Scenario: Secciones legibles con imagen primero
- **WHEN** la vista de detalle de una operación con imagen carga
- **THEN** el sistema muestra primero la imagen, seguida de los campos organizados en secciones con encabezados claros, terminando con la Descripción de entrada al final

#### Scenario: Operación sin imagen adjunta
- **WHEN** una operación no tiene ninguna imagen adjunta y el usuario abre su vista de detalle
- **THEN** el sistema omite la sección de imagen sin dejar espacio vacío ni layout roto, mostrando el resto de las secciones con normalidad

### Requirement: Editar una operación existente
El sistema SHALL permitir editar una operación existente desde su vista de detalle, reutilizando el mismo formulario y las mismas reglas de validación de registro, prellenado con los datos actuales.

#### Scenario: Abrir el formulario prellenado
- **WHEN** el usuario está en la vista de detalle de una operación y presiona "Editar"
- **THEN** el sistema abre el formulario prellenado con los datos actuales de esa operación

#### Scenario: Guardar los cambios editados
- **WHEN** el usuario modificó uno o más campos del formulario de edición y presiona "Guardar"
- **THEN** el sistema actualiza la operación con los nuevos valores, reflejándolos en el listado y en la vista de detalle

#### Scenario: Validación existente aplica también al editar
- **WHEN** el usuario, editando una operación, ingresa un valor fuera de rango (misma regla de validación que en creación) e intenta guardar
- **THEN** el sistema aplica la misma validación que en creación y bloquea el guardado

### Requirement: Eliminar una operación con opción de deshacer
El sistema SHALL permitir eliminar una operación desde su vista de detalle, difiriendo el borrado físico (registro e imágenes) hasta que expire una ventana de "Deshacer", durante la cual la operación puede restaurarse exactamente como estaba.

#### Scenario: Eliminar una operación muestra el deshacer
- **WHEN** el usuario está en la vista de detalle de una operación, presiona "Eliminar" y confirma
- **THEN** el sistema retira la operación del listado y muestra un snackbar temporal con la opción "Deshacer"

#### Scenario: Deshacer la eliminación dentro del tiempo disponible
- **WHEN** el snackbar de "Deshacer" está visible tras eliminar una operación y el usuario lo toca dentro del tiempo disponible
- **THEN** el sistema restaura la operación (y sus imágenes) exactamente como estaba, mostrándola de nuevo en el listado

#### Scenario: Eliminación definitiva tras expirar el tiempo de deshacer
- **WHEN** el usuario eliminó una operación, no tocó "Deshacer" dentro del tiempo disponible, y el snackbar desaparece
- **THEN** el sistema elimina permanentemente la operación y sus imágenes asociadas (archivos en `filesDir`)
