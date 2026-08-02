## ADDED Requirements

### Requirement: Listado agrupado por mes y día
El sistema SHALL mostrar el historial de operaciones agrupado por mes y, dentro de cada mes, por día, con el mes más reciente arriba y expandido por defecto, y el día/operación más reciente primero dentro de cada grupo.

#### Scenario: Agrupación por mes con el más reciente expandido
- **WHEN** existen operaciones registradas en distintos meses y el usuario abre Historial
- **THEN** el sistema muestra secciones por mes, con el mes más reciente arriba y expandido por defecto

#### Scenario: Agrupación por día dentro de un mes
- **WHEN** un mes tiene operaciones registradas en distintos días y el usuario visualiza ese grupo
- **THEN** el sistema muestra un encabezado por día, listando debajo las operaciones de ese día, con la más reciente primero

### Requirement: Tarjeta compacta de operación
El sistema SHALL mostrar cada operación en el listado como una tarjeta compacta con miniatura de imagen (o ícono genérico si no hay), Activo + Hora en la línea superior, y Resultado + Resultado en R coloreados (verde/rojo/gris) en la línea inferior.

#### Scenario: Contenido y color de la tarjeta
- **WHEN** se renderiza la tarjeta de una operación en el listado
- **THEN** el sistema muestra miniatura (o ícono genérico si no hay imagen), Activo + Hora arriba, y Resultado + Resultado en R coloreados abajo

### Requirement: Carga incremental sin degradar la fluidez a escala
El sistema SHALL cargar las operaciones de forma incremental (paginada) a medida que el usuario hace scroll, sin cargar el histórico completo a memoria, manteniendo la misma fluidez con historiales de hasta 5,000 operaciones.

#### Scenario: Scroll fluido con historial grande
- **WHEN** el usuario tiene más de 3,000 operaciones registradas y navega por el listado de Historial
- **THEN** el sistema carga las operaciones de forma incremental a medida que hace scroll, sin cargar todo el histórico a memoria y sin caídas de fluidez perceptibles

#### Scenario: Carga inicial rápida con dataset grande
- **WHEN** el usuario tiene más de 3,000 operaciones registradas y abre la pestaña Historial por primera vez en la sesión
- **THEN** el sistema muestra las primeras operaciones sin esperar a cargar el histórico completo

#### Scenario: Límite superior del rango soportado (5,000 operaciones)
- **WHEN** el usuario tiene 5,000 operaciones registradas y navega por todo el listado hasta el final
- **THEN** el sistema mantiene la misma fluidez que con datasets menores, sin errores ni caídas perceptibles

### Requirement: Resumen mensual con colapsar/expandir
El sistema SHALL mostrar, por cada mes, un resumen rápido (número de operaciones, % ganadas, R acumulado) y SHALL permitir colapsar o expandir el grupo de ese mes.

#### Scenario: Ver el resumen de un mes
- **WHEN** el usuario visualiza el encabezado de un grupo de mes
- **THEN** el sistema muestra el número de operaciones, el % ganadas y el R acumulado de ese mes

#### Scenario: Colapsar y expandir un grupo de mes
- **WHEN** el usuario toca el encabezado de un grupo de mes expandido (o colapsado)
- **THEN** el sistema colapsa (o expande) ese grupo, ocultando (o mostrando) sus operaciones sin afectar otros meses

### Requirement: Estados vacíos diferenciados
El sistema SHALL distinguir entre "nunca hubo operaciones registradas" (con acceso directo para crear la primera) y "hay operaciones pero el filtro/búsqueda activa no arroja resultados" (sin ese acceso directo).

#### Scenario: Primera vez sin operaciones registradas
- **WHEN** el usuario no ha registrado ninguna operación aún y abre Historial
- **THEN** el sistema muestra un mensaje amigable invitando a crear la primera operación, con un acceso directo al formulario de "Nueva operación"

#### Scenario: Sin resultados para el filtro activo
- **WHEN** el usuario tiene operaciones registradas pero el filtro o búsqueda activa no coincide con ninguna, y el listado se renderiza
- **THEN** el sistema muestra un mensaje indicando que no hay resultados para ese filtro, distinto del mensaje de "primera vez", sin ofrecer el acceso directo de creación

#### Scenario: Acceder al formulario desde el estado vacío inicial
- **WHEN** el sistema muestra el mensaje de "primera vez sin operaciones" y el usuario toca el acceso directo
- **THEN** el sistema abre el formulario de "Nueva operación"
