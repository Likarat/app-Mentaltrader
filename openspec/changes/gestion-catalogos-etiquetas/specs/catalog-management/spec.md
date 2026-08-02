## ADDED Requirements

### Requirement: Agregar elemento a un catálogo
El sistema SHALL permitir agregar un nuevo elemento a los catálogos de Activos, Emociones o Errores desde la pestaña Etiquetas, con unicidad case-insensitive (ignorando espacios al inicio/final) evaluada **solo dentro del mismo tipo de catálogo**.

#### Scenario: Agregar un nuevo elemento a un catálogo
- **WHEN** el usuario, en la administración de un catálogo (ej. Activos), agrega un nuevo elemento (ej. "EURUSD")
- **THEN** el sistema lo guarda y lo muestra disponible inmediatamente en los formularios de registro

#### Scenario: Impedir agregar un elemento duplicado
- **WHEN** el usuario intenta agregar un elemento con el mismo nombre que uno ya existente en ese catálogo (ignorando mayúsculas/minúsculas y espacios al inicio/final)
- **THEN** el sistema impide guardarlo y muestra el mensaje "Este valor ya existe en el catálogo"

#### Scenario: El mismo nombre es válido en catálogos de tipo distinto
- **WHEN** el catálogo de Errores ya tiene un elemento llamado "Noticias" y el usuario agrega un elemento llamado "Noticias" al catálogo de Activos
- **THEN** el sistema lo permite, ya que la unicidad aplica solo dentro del mismo tipo de catálogo

### Requirement: Editar y eliminar elemento de un catálogo con protección de semillas
El sistema SHALL permitir editar el nombre de un elemento existente y eliminarlo, SHALL impedir eliminar un elemento semilla cuando su condición de protección aplica (`Ninguno` en Errores siempre; `XAUUSD` en Activos solo si es el único activo existente), y SHALL recrear automáticamente la semilla correspondiente si un catálogo queda sin ningún elemento.

#### Scenario: Editar un elemento existente
- **WHEN** el usuario modifica el nombre de un elemento existente y confirma
- **THEN** el sistema actualiza el elemento, reflejando el nuevo nombre en las próximas selecciones del formulario

#### Scenario: Eliminar un elemento sin uso en ninguna operación
- **WHEN** el usuario elimina un elemento del catálogo que no ha sido usado en ninguna operación registrada
- **THEN** el sistema lo elimina de inmediato, sin pedir confirmación adicional

#### Scenario: Impedir eliminar un elemento semilla no eliminable
- **WHEN** el usuario intenta eliminar el elemento semilla "Ninguno" del catálogo de Errores, o "XAUUSD" del catálogo de Activos siendo este el único activo existente, y confirma la eliminación
- **THEN** el sistema impide la eliminación, indicando que ese elemento no puede eliminarse en esta condición

#### Scenario: Recreación automática de semilla al vaciar el catálogo
- **WHEN** el usuario elimina el último elemento restante de un catálogo, dejándolo sin ningún elemento
- **THEN** el sistema recrea automáticamente el elemento semilla correspondiente a ese catálogo

### Requirement: Confirmar eliminación de un elemento de catálogo en uso
El sistema SHALL mostrar, antes de eliminar un elemento de catálogo que ya está en uso por al menos una operación existente, una advertencia con el conteo de operaciones afectadas, permitiendo confirmar o cancelar; al confirmarse, SHALL retirar el elemento del catálogo sin modificar el valor histórico de las operaciones que ya lo usaban.

#### Scenario: Advertencia con conteo al eliminar un elemento en uso
- **WHEN** el usuario intenta eliminar, desde la pestaña de Etiquetas, un elemento de catálogo que ya fue usado en una o más operaciones existentes
- **THEN** el sistema muestra una advertencia indicando en cuántas operaciones está en uso, permitiendo confirmar o cancelar la eliminación

#### Scenario: Cancelar la eliminación de un elemento en uso
- **WHEN** la advertencia de eliminación está visible para un elemento en uso y el usuario elige cancelar
- **THEN** el sistema cierra la advertencia y el elemento permanece sin cambios en el catálogo

#### Scenario: Confirmar la eliminación conservando el valor histórico
- **WHEN** la advertencia de eliminación está visible para un elemento en uso y el usuario confirma la eliminación
- **THEN** el sistema retira el elemento del catálogo (deja de ofrecerse en nuevos formularios) y las operaciones que ya lo usaban conservan su valor histórico sin cambios
