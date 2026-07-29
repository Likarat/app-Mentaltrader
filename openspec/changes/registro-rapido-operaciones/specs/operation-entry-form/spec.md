## ADDED Requirements

### Requirement: Registrar operación con campos identificadores mínimos
El sistema SHALL permitir crear una operación con Fecha, Hora, Activo, Dirección y Resultado, más los campos opcionales Riesgo (%) y Resultado en R.

#### Scenario: Guardar con los campos mínimos completos
- **WHEN** el usuario completa Fecha, Hora, Activo, Dirección y Resultado con valores válidos y presiona "Guardar"
- **THEN** el sistema almacena la operación localmente y la muestra inmediatamente en el listado, ordenada por fecha (más reciente primero)

#### Scenario: Impedir el guardado si falta un campo mínimo
- **WHEN** el usuario deja sin completar al menos uno de los 5 campos mínimos y presiona "Guardar"
- **THEN** el sistema impide el guardado y resalta visualmente el/los campo(s) faltante(s)

#### Scenario: Seleccionar el activo semilla en un catálogo recién inicializado
- **WHEN** el catálogo de Activos no tiene ningún elemento agregado manualmente (solo la semilla "XAUUSD") y el usuario lo selecciona y guarda
- **THEN** el sistema guarda la operación con `assetId` apuntando al elemento semilla "XAUUSD"

#### Scenario: Guardar con Riesgo (%) y Resultado en R completados
- **WHEN** el usuario completa los 5 campos mínimos más Riesgo (%) y Resultado en R, y guarda
- **THEN** el sistema almacena ambos valores opcionales junto con el resto de la operación

#### Scenario: Guardar dejando Riesgo (%) y Resultado en R vacíos
- **WHEN** el usuario completa solo los 5 campos mínimos, deja vacíos Riesgo (%) y Resultado en R, y guarda
- **THEN** el sistema guarda la operación con normalidad, dejando ambos campos como nulos

### Requirement: Completar operación con campos de catálogo obligatorios
El sistema SHALL permitir completar Calidad, Emoción antes, Emoción después y Error (todos obligatorios), más el motivo en texto libre de cada uno (opcional).

#### Scenario: Completar Calidad, Emoción antes/después y Error
- **WHEN** el usuario, con los campos mínimos ya completos, completa Calidad, Emoción antes, Emoción después y Error con valores válidos de catálogo y guarda
- **THEN** el sistema almacena los cuatro campos junto con el resto de la operación

#### Scenario: Impedir el guardado si falta uno de los campos de catálogo
- **WHEN** el usuario deja sin completar al menos uno de Calidad, Emoción antes, Emoción después o Error y presiona "Guardar"
- **THEN** el sistema impide el guardado y resalta visualmente el/los campo(s) faltante(s)

#### Scenario: Catálogo de Emociones recién inicializado, solo con valores semilla
- **WHEN** el catálogo de Emociones no tiene ningún valor agregado manualmente (solo los valores semilla) y el usuario selecciona un valor semilla como Emoción antes y otro como Emoción después, y guarda
- **THEN** el sistema guarda la operación con `emotionBeforeId`/`emotionAfterId` apuntando a los elementos semilla seleccionados

#### Scenario: Catálogo de Error con un único valor semilla disponible
- **WHEN** el catálogo de Errores solo tiene disponible "Ninguno" y el usuario lo selecciona y guarda
- **THEN** el sistema guarda la operación con `errorId` apuntando al elemento semilla "Ninguno"

#### Scenario: Completar los motivos en texto libre de cada selección
- **WHEN** el usuario completa opcionalmente `emotionBeforeReason`, `emotionAfterReason` y `errorReason` y guarda
- **THEN** el sistema almacena los tres motivos como texto libre

### Requirement: Agregar descripción de entrada a la operación
El sistema SHALL requerir un campo de texto libre multilínea ("Descripción entrada"), sin límite de longitud, como último campo del formulario.

#### Scenario: Guardar con descripción completa
- **WHEN** el usuario, con el resto de campos obligatorios completos, escribe texto en "Descripción entrada" y guarda
- **THEN** el sistema almacena el texto completo en `entryDescription` junto con el resto de la operación

#### Scenario: Impedir el guardado con descripción vacía
- **WHEN** el usuario deja vacío "Descripción entrada" y presiona "Guardar"
- **THEN** el sistema impide el guardado y resalta visualmente el campo como obligatorio

#### Scenario: Texto largo con múltiples saltos de línea, sin truncar
- **WHEN** el usuario escribe un texto extenso con múltiples párrafos y guarda
- **THEN** el sistema almacena el texto completo sin truncarlo, y el campo se muestra expandido según su contenido

### Requirement: Prellenar valores por defecto al abrir el formulario
El sistema SHALL prellenar Fecha y Hora con el valor actual (misma fila) y Activo con "XAUUSD" si el catálogo está vacío, permitiendo edición manual.

#### Scenario: Prellenado de Fecha y Hora al abrir el formulario
- **WHEN** el usuario abre "Nueva operación"
- **THEN** el sistema muestra Fecha y Hora prellenadas con el valor actual del dispositivo, ambas en la misma fila

#### Scenario: Preselección de Activo cuando el catálogo está vacío
- **WHEN** el catálogo de Activos no tiene ningún elemento agregado y el usuario abre "Nueva operación"
- **THEN** el sistema preselecciona "XAUUSD" como Activo

#### Scenario: Sin forzar valor por defecto cuando el catálogo ya tiene elementos
- **WHEN** el catálogo de Activos ya tiene al menos un elemento agregado por el usuario y este abre "Nueva operación"
- **THEN** el sistema no fuerza ningún valor por defecto de Activo

#### Scenario: Conservar la edición manual de un valor prellenado
- **WHEN** el usuario modifica manualmente Fecha u Hora antes de guardar
- **THEN** el sistema conserva el valor editado, sin revertirlo al valor por defecto

#### Scenario: Reloj del dispositivo desconfigurado
- **WHEN** el reloj del dispositivo está desconfigurado y el usuario abre "Nueva operación"
- **THEN** el sistema prellena Fecha y Hora igualmente con el valor (incorrecto) del dispositivo, sin validarlo en este punto

### Requirement: Acceso rápido para nueva operación
El sistema SHALL mostrar un botón flotante "+" en Inicio y en Historial que abra el formulario de "Nueva operación", sin aparecer en Etiquetas ni tapar contenido al hacer scroll.

#### Scenario: Crear operación desde el FAB en Inicio
- **WHEN** el usuario, en la pestaña Inicio, toca el botón flotante "+"
- **THEN** el sistema abre el formulario de "Nueva operación"

#### Scenario: Crear operación desde el FAB en Historial
- **WHEN** el usuario, en la pestaña Historial, toca el botón flotante "+"
- **THEN** el sistema abre el formulario de "Nueva operación"

#### Scenario: Ausencia del FAB en la pestaña Etiquetas
- **WHEN** el usuario visualiza la pestaña Etiquetas
- **THEN** el sistema no muestra ningún botón flotante "+"

#### Scenario: El FAB no tapa contenido al hacer scroll
- **WHEN** el usuario desplaza la pantalla en Inicio o Historial con contenido suficiente
- **THEN** el botón flotante permanece visible sin bloquear la interacción con el contenido subyacente

### Requirement: Confirmar antes de salir del formulario con cambios sin guardar
El sistema SHALL mostrar un diálogo de confirmación al intentar salir del formulario (navegación o "atrás") si hay cambios sin guardar, y SHALL permitir salir sin fricción si no hay cambios.

#### Scenario: Mostrar diálogo al navegar a otra pestaña con cambios pendientes
- **WHEN** el usuario, tras modificar al menos un campo sin guardar, intenta navegar a otra pestaña
- **THEN** el sistema muestra un diálogo de confirmación indicando que los cambios se perderán

#### Scenario: Mostrar diálogo al presionar "atrás" con cambios pendientes
- **WHEN** el usuario, tras modificar al menos un campo sin guardar, presiona el botón/gesto "atrás"
- **THEN** el sistema muestra el mismo diálogo de confirmación

#### Scenario: Cancelar la salida y permanecer en el formulario
- **WHEN** el diálogo de confirmación está visible y el usuario elige "Cancelar"
- **THEN** el sistema cierra el diálogo y permanece en el formulario con los datos ingresados intactos

#### Scenario: Confirmar la salida y perder los cambios
- **WHEN** el diálogo de confirmación está visible y el usuario confirma la salida
- **THEN** el sistema abandona el formulario sin guardar, descartando los cambios

#### Scenario: Salir sin fricción cuando no hay cambios pendientes
- **WHEN** el usuario no modificó ningún campo y navega a otra pestaña o presiona "atrás"
- **THEN** el sistema sale del formulario de inmediato, sin mostrar ningún diálogo
