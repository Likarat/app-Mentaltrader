## ADDED Requirements

### Requirement: Agregar etiqueta nueva sin salir del formulario
El sistema SHALL permitir crear un nuevo elemento de catálogo (Activo, Emoción o Error) directamente desde el formulario de operación mediante "+ Agregar nueva", sin salir de él, reutilizando la misma validación de unicidad del catálogo, seleccionando automáticamente el elemento creado y conservando el resto de los datos ya ingresados.

#### Scenario: Mostrar el campo inline al tocar "+ Agregar nueva"
- **WHEN** el usuario, seleccionando una etiqueta en el formulario de operación, toca "+ Agregar nueva"
- **THEN** el sistema muestra un campo pequeño para escribir el nombre del nuevo elemento, sin salir del formulario

#### Scenario: Guardar y seleccionar automáticamente sin salir del formulario
- **WHEN** el usuario escribe un nombre válido en el campo inline y confirma la creación
- **THEN** el sistema guarda el nuevo elemento en el catálogo correspondiente, lo selecciona automáticamente en el campo del formulario, y el resto de los datos ya ingresados permanecen intactos

#### Scenario: Nombre duplicado desde el flujo inline
- **WHEN** el usuario intenta confirmar la creación de un elemento cuyo nombre ya existe en el catálogo correspondiente y el sistema rechaza la creación
- **THEN** el campo inline permanece abierto y con foco, se muestra el mensaje "Este valor ya existe en el catálogo" junto al campo, sin navegar fuera del formulario, y el resto de los datos permanece intacto

#### Scenario: Cancelar el campo inline sin crear ningún elemento
- **WHEN** el usuario cierra el campo inline sin escribir nada o cancela la acción
- **THEN** el sistema cierra el campo y el selector de etiqueta vuelve a su estado anterior, sin crear ningún elemento nuevo
