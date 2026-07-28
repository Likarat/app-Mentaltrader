# Bitácora de Trading (Forex) — Especificación Unificada

> Este documento consolida `Doc Inicial.md` (especificación funcional) y `Doc Inicial 2.md` (arquitectura, modelo de datos y reglas técnicas) en una sola fuente de verdad, eliminando duplicados y contradicciones.
>
> **Decisiones tomadas para resolver conflictos entre ambos documentos originales:**
> 1. El Historial se agrupa **solo por Mes** (no por semana). "Última semana" existe únicamente como **filtro** rápido, no como nivel de agrupación.
> 2. **minSdk = 27** (Android 8.1 Oreo), priorizando compatibilidad amplia sobre usar la versión del dispositivo actual. Ajustado desde la decisión original (minSdk 21) porque el wizard de Android Studio ya no ofrece API 21/22 (mínimo disponible: API 23); se eligió 27 como piso razonable en vez de saltar directo al máximo disponible.

---

## 1. Propósito y contexto

Proveer una aplicación Android nativa que permita a un trader de forex registrar sus operaciones de manera rápida y sencilla, y visualizar/analizar su historial agrupado por mes, con el objetivo de identificar patrones (buenos y malos) que le permitan mejorar de forma continua. Tema oscuro por defecto para facilitar el estudio de cada entrada.

- Uso personal (single-user, sin backend ni cuentas en v1).
- Prioridades: rapidez de registro, facilidad de visualización/estudio y detección de patrones.
- Plataforma: Android nativo. Distribución en v1 vía **APK manual** (sin Play Store por ahora; evaluado para v2).
- Almacenamiento: 100% local en el dispositivo (offline-first), incluyendo imágenes.
- Idioma: español (única versión en v1). Nombres técnicos de clases/tablas/rutas en inglés.
- No hay mockups previos; la UI se ajusta de forma iterativa durante el desarrollo. Ya definido: en el formulario, Fecha y Hora van en la misma fila, y "Descripción entrada" es el último campo (multilínea, crece con el contenido).
- Nombre e ícono de la app: pendientes, se resuelven al final del desarrollo.

---

## 2. Decisiones técnicas y stack

| Aspecto | Decisión |
|---|---|
| Lenguaje | Kotlin |
| UI | Jetpack Compose, tema oscuro por defecto |
| minSdk | **27** (Android 8.1, para compatibilidad amplia; ver nota de decisión arriba) |
| Arquitectura | MVVM (ViewModel + Repository), StateFlow para estado de UI |
| Base de datos local | Room (sobre SQLite) |
| Imágenes | Almacenadas en `filesDir` (archivo, no BLOB), con referencia/ruta en Room; redimensionadas (~1600–2000px ancho) y comprimidas (JPEG ~85%) |
| Carga de imágenes en UI | Coil |
| Navegación | Navigation Compose, barra inferior (Inicio / Historial / Etiquetas) |
| Preferencias simples persistentes | DataStore Preferences |
| Gráficas | Librería ligera compatible con Compose (a confirmar en diseño técnico), evitando soluciones pesadas |
| Inyección de dependencias | Manual en v1, con contrato de interfaces entre capas (Hilt queda como posible mejora futura) |
| Distribución | APK manual en v1; Play Store evaluado para v2 |
| Orientación | Bloqueada en vertical (portrait), excepto vista de imagen a pantalla completa |
| Listas grandes | Paging 3 (Jetpack) para el Historial |

---

## 3. Modelo de datos

### 3.1 Entidades

#### `Operation`
| Campo | Tipo | Notas |
|---|---|---|
| `id` | `Long` (PK, autogenerado) | |
| `dateTime` | `Long` (timestamp UTC) | Fecha + hora combinadas; se usa para ordenar y filtrar |
| `assetId` | `Long` (FK → `CatalogItem`, type = `ASSET`) | |
| `direction` | `Direction` (enum) | `BUY` / `SELL` |
| `quality` | `Float` | 0.0–10.0, 1 decimal |
| `emotionBeforeId` | `Long` (FK → `CatalogItem`, type = `EMOTION`) | |
| `emotionBeforeReason` | `String?` | |
| `emotionAfterId` | `Long` (FK → `CatalogItem`, type = `EMOTION`) | |
| `emotionAfterReason` | `String?` | |
| `errorId` | `Long` (FK → `CatalogItem`, type = `ERROR`) | |
| `errorReason` | `String?` | |
| `result` | `ResultType` (enum) | `WIN` / `LOSS` / `BREAK_EVEN` |
| `riskPercentage` | `Float?` | 0–100 |
| `resultInR` | `Float?` | Decimal con signo, sin límite |
| `plannedRatio` | `String?` | Ej. "1:2" |
| `entryDescription` | `String` | Texto multilínea, sin límite de longitud en UI ni modelo |
| `createdAt` / `updatedAt` | `Long` | Auditoría local |

#### `CatalogItem`
Estructura única para los tres catálogos (Activos, Emociones, Errores), diferenciados por `type`.

| Campo | Tipo | Notas |
|---|---|---|
| `id` | `Long` (PK, autogenerado) | |
| `type` | `CatalogType` (enum) | `ASSET` / `EMOTION` / `ERROR` |
| `name` | `String` | Único dentro de cada `type` (comparación case-insensitive, ignorando espacios al inicio/final) |
| `isDefault` | `Boolean` | Elemento semilla / no eliminable |
| `createdAt` / `updatedAt` | `Long` | |

- Validación de referencias en UI/ViewModels: `emotionBeforeId`/`emotionAfterId` solo pueden apuntar a `type = EMOTION`; `errorId` solo a `type = ERROR`; `assetId` solo a `type = ASSET`. Así, aunque técnicamente comparten tabla, una emoción nunca puede usarse como error ni como activo.
- `ERROR`: elemento semilla `Ninguno` con `isDefault = true` (obligatorio, no eliminable; si el catálogo quedara vacío, se recrea automáticamente).
- `ASSET`: si el catálogo queda vacío se crea automáticamente `XAUUSD`; `XAUUSD` solo es eliminable si existe al menos otro activo.
- `EMOTION`: puede quedar vacío; la app debe permitir añadir nuevas emociones libremente (no requiere semilla obligatoria, aunque se puede sembrar con valores iniciales de forma opcional).

#### `OperationImage`
| Campo | Tipo | Notas |
|---|---|---|
| `id` | `Long` (PK, autogenerado) | |
| `operationId` | `Long` (FK → `Operation`) | |
| `filePath` | `String` | Ruta relativa dentro de `filesDir` |
| `position` | `Int` | Orden dentro de la operación (máx. 2 imágenes) |
| `createdAt` | `Long` | |

Al eliminar la operación se borran también los archivos de imagen asociados.

### 3.2 Enums
- `Direction`: `BUY`, `SELL`
- `ResultType`: `WIN`, `LOSS`, `BREAK_EVEN`
- `CatalogType`: `ASSET`, `EMOTION`, `ERROR`

### 3.3 Mapeo de campos del formulario al modelo

| Campo del formulario | Atributo en el modelo |
|---|---|
| Activo | `assetId` (CatalogType.ASSET) |
| Emoción antes | `emotionBeforeId` (CatalogType.EMOTION) |
| Emoción después | `emotionAfterId` (CatalogType.EMOTION) |
| Error | `errorId` (CatalogType.ERROR) |
| Motivo emoción antes | `emotionBeforeReason` |
| Motivo emoción después | `emotionAfterReason` |
| Motivo/descripción del error | `errorReason` |
| Ratio planeado | `plannedRatio` |
| Descripción entrada | `entryDescription` |
| Fecha/Hora | `dateTime` |
| Calidad | `quality` |
| Riesgo (%) | `riskPercentage` |
| Resultado en R | `resultInR` |

---

## 4. Especificación funcional (Requirements & Scenarios)

### Requirement: Registro de operación
El sistema SHALL permitir crear una nueva entrada de operación con los siguientes campos:

| Campo | Tipo | Obligatorio | Notas |
|---|---|---|---|
| Fecha | Fecha (dd/mm/aaaa) | Sí | Por defecto, fecha actual. Misma fila que Hora |
| Hora | Hora (hh:mm AM/PM) | Sí | Por defecto, hora actual. Misma fila que Fecha |
| Activo | Selección de catálogo administrable | Sí | Valor por defecto si el catálogo está vacío: XAUUSD |
| Dirección | Selección única: Compra / Venta | Sí | |
| Calidad | Decimal 0.0–10.0, 1 decimal | Sí | Autoevaluación de la ejecución |
| Emoción antes | Etiqueta de catálogo fijo administrable | Sí | Ej. nervios, confianza, ansiedad |
| Motivo emoción antes | Texto libre | No | |
| Emoción después | Etiqueta de catálogo fijo administrable | Sí | Ej. felicidad, frustración, alivio |
| Motivo emoción después | Texto libre | No | |
| Error | Etiqueta de catálogo fijo administrable | Sí | Catálogo pre-creado con "Ninguno" disponible desde el inicio; el campo siempre SHALL tener un valor seleccionado |
| Motivo/descripción del error | Texto libre | No | |
| Resultado | Selección única: Ganada / Perdida / Break Even | Sí | |
| Riesgo (%) | Decimal, 0–100 | No | % de la cuenta arriesgado |
| Resultado en R | Decimal con signo, sin límite | No | Ej. +2, -1, +0.5 — se muestra como "+2R" |
| Ratio planeado | Texto corto (ej. "1:2") | No | |
| Descripción entrada | Texto libre multilínea, crece con el contenido | Sí | Último campo del formulario |
| Imagen adjunta | Máximo 2 imágenes | No | Captura del gráfico de la operación |

No hay límite de longitud máxima aplicado en la UI ni en el modelo para los campos de texto libre; se recomienda un límite "razonable" en la UX por performance, pero no es obligatorio.

#### Scenario: Crear una operación completa
- **GIVEN** el usuario está en "Nueva operación"
- **WHEN** completa todos los campos obligatorios y presiona "Guardar"
- **THEN** el sistema SHALL almacenar la operación localmente
- **AND** SHALL mostrarla inmediatamente en el listado principal, en la posición correspondiente por fecha (más reciente primero)

#### Scenario: Intento de guardar sin campos obligatorios
- **GIVEN** el usuario está creando una nueva operación
- **WHEN** intenta guardar sin completar un campo obligatorio (ej. Activo o Resultado)
- **THEN** el sistema SHALL impedir el guardado
- **AND** SHALL resaltar el/los campo(s) faltante(s)

#### Scenario: Registro rápido con valores por defecto
- **GIVEN** el usuario abre "Nueva operación"
- **WHEN** la pantalla carga
- **THEN** el sistema SHALL prellenar Fecha y Hora con el valor actual, en la misma fila
- **AND** SHALL permitir editarlos manualmente
- **AND** si el catálogo de Activos está vacío, SHALL preseleccionar XAUUSD

#### Scenario: Registrar el resultado en R
- **GIVEN** el usuario está completando el formulario
- **WHEN** ingresa un valor en "Resultado en R" (ej. -1, +2, +0.5)
- **THEN** el sistema SHALL almacenarlo como decimal con signo
- **AND** SHALL mostrarlo con el sufijo "R" (ej. "+2R")

#### Scenario: Catálogo de Error con valor por defecto "Ninguno"
- **GIVEN** es la primera vez que se abre la app
- **WHEN** el catálogo de Errores se inicializa
- **THEN** el sistema SHALL crear automáticamente el elemento "Ninguno"
- **AND** el campo Error SHALL requerir siempre un valor seleccionado (no puede quedar vacío)

---

### Requirement: Reglas de validación

| Campo | Regla |
|---|---|
| Fecha y Hora | No se permiten fechas ni horas futuras. Mensaje de error: "No se permiten fechas/horas futuras" |
| Calidad | Solo entre 0.0 y 10.0, máximo 1 decimal |
| Riesgo (%) | Solo entre 0 y 100 |
| Resultado en R | Cualquier valor decimal con signo, sin límite superior o inferior; se muestra siempre con sufijo "R" |
| Catálogos | No se permiten duplicados (comparación case-insensitive, ignorando espacios al inicio/final). Mensaje de error: "Este valor ya existe en el catálogo" |
| Texto libre | Sin longitud máxima obligatoria |

#### Scenario: Validación de rangos numéricos
- **GIVEN** el usuario está completando el formulario
- **WHEN** ingresa un valor en Calidad, Riesgo (%) o Resultado en R
- **THEN** el sistema SHALL aplicar las reglas de la tabla anterior para cada campo

#### Scenario: Intento de guardar con fecha/hora futura
- **GIVEN** el usuario ingresa una fecha/hora posterior al momento actual
- **WHEN** intenta guardar
- **THEN** el sistema SHALL mostrar el error "No se permiten fechas/horas futuras" en rojo e impedir el guardado

---

### Requirement: Gestión de catálogos (Activos, Emociones, Errores)
El sistema SHALL permitir administrar tres catálogos editables desde la propia app: Activos, Emociones y Errores. Cada catálogo SHALL soportar agregar, editar y eliminar elementos, respetando las reglas de la sección 3.1 (`CatalogItem`).

#### Scenario: Catálogo de Activos vacío por defecto
- **GIVEN** es la primera vez que se abre la app
- **WHEN** el catálogo de Activos no tiene elementos
- **THEN** el sistema SHALL incluir XAUUSD como valor por defecto disponible

#### Scenario: Agregar un nuevo elemento a un catálogo
- **GIVEN** el usuario está en la administración de un catálogo
- **WHEN** agrega un nuevo elemento (ej. un nuevo activo "EURUSD")
- **THEN** el sistema SHALL guardarlo y mostrarlo disponible inmediatamente en los formularios de registro

#### Scenario: Eliminar un elemento en uso
- **GIVEN** un elemento de catálogo ya fue usado en operaciones existentes
- **WHEN** el usuario lo elimina del catálogo
- **THEN** el sistema SHALL conservar el valor histórico en las operaciones ya registradas
- **AND** SHALL dejar de mostrarlo como opción para nuevas operaciones

#### Scenario: Confirmación al eliminar un elemento en uso
- **GIVEN** un elemento del catálogo fue usado en una o más operaciones
- **WHEN** el usuario intenta eliminarlo desde la pestaña de Etiquetas
- **THEN** el sistema SHALL mostrar una advertencia indicando en cuántas operaciones está en uso
- **AND** SHALL permitir confirmar o cancelar la eliminación

#### Scenario: Agregar etiqueta sin salir del formulario
- **GIVEN** el usuario está seleccionando una etiqueta (ej. Activo) en el formulario de operación
- **WHEN** no encuentra la opción deseada y toca "+ Agregar nueva"
- **THEN** el sistema SHALL mostrar un campo pequeño para escribir el nombre
- **AND** SHALL guardar el nuevo elemento en el catálogo correspondiente y seleccionarlo automáticamente, sin salir del formulario

---

### Requirement: Adjuntar imagen a la operación
El sistema SHALL permitir adjuntar hasta 2 imágenes por operación (cámara o galería), almacenándolas comprimidas en `filesDir` (por referencia, no como BLOB) para mantener la app liviana. Con la compresión definida (ancho máx. ~1600–2000px, JPEG ~85%), cada imagen debería pesar aproximadamente 200–400KB. Las miniaturas usadas en el listado SHALL generarse una sola vez al guardar la operación (no recalcularse en cada render).

#### Scenario: Adjuntar imagen desde galería o cámara
- **GIVEN** el usuario está creando o editando una operación
- **WHEN** selecciona "Adjuntar imagen" y toma una foto o elige una de la galería
- **THEN** el sistema SHALL redimensionar y comprimir la imagen antes de guardarla
- **AND** SHALL mostrar una miniatura en el formulario
- **AND** SHALL impedir adjuntar una tercera imagen a la misma operación

#### Scenario: Ver imagen adjunta en detalle con zoom
- **GIVEN** una operación tiene una o más imágenes adjuntas
- **WHEN** el usuario abre el detalle y toca la imagen
- **THEN** el sistema SHALL mostrarla en tamaño completo, con fondo negro/oscuro alrededor
- **AND** SHALL permitir hacer zoom (pellizcar/ampliar)

#### Scenario: Navegar entre múltiples imágenes de una operación
- **GIVEN** una operación tiene 2 imágenes adjuntas
- **WHEN** el usuario abre la vista de imagen a pantalla completa
- **THEN** el sistema SHALL mostrar primero la primera imagen
- **AND** SHALL mostrar un indicador de puntos en la parte inferior
- **AND** SHALL permitir avanzar deslizando o tocando una flecha a la derecha

#### Scenario: Visualizar espacio usado por imágenes
- **GIVEN** el usuario tiene operaciones con imágenes adjuntas
- **WHEN** abre la pestaña de Etiquetas
- **THEN** el sistema SHALL mostrar el espacio total aproximado en disco ocupado por todas las imágenes

---

### Requirement: Listado de Historial agrupado por mes

> Decisión: la agrupación es **solo por mes**. No existe nivel de "semana" en la agrupación visual; "Última semana" es un filtro, no un grupo.

El sistema SHALL mostrar el historial de operaciones agrupado por mes, y dentro de cada mes por día, ordenado siempre con la operación más reciente primero.

#### Scenario: Ver operaciones agrupadas por mes
- **GIVEN** existen operaciones registradas en distintos meses
- **WHEN** el usuario abre Historial
- **THEN** el sistema SHALL mostrar secciones por mes (ej. "Diciembre 2026" o separador simple tipo `--- Diciembre 2026 ---`), con el mes más reciente arriba, **expandido por defecto**
- **AND** dentro de cada mes SHALL mostrar un encabezado por día, listando debajo las operaciones de ese día

#### Scenario: Resumen rápido por mes
- **GIVEN** el usuario ve un grupo de mes
- **WHEN** el grupo se renderiza
- **THEN** el sistema SHALL mostrar un resumen visible: número de operaciones, % de ganadas y R acumulado del grupo

#### Scenario: Colapsar y expandir un mes
- **GIVEN** el usuario está viendo el listado agrupado
- **WHEN** toca el encabezado de un mes
- **THEN** el sistema SHALL alternar entre expandir y colapsar ese grupo (esta opción existe, pero no es protagonista visualmente)

#### Scenario: Scroll fluido con historial grande
- **GIVEN** el usuario tiene más de 3,000 operaciones registradas
- **WHEN** navega por el listado de Historial
- **THEN** el sistema SHALL cargar las operaciones de forma paginada (Paging 3), evitando caídas de fluidez perceptibles

---

### Requirement: Tarjeta de operación en el listado
El sistema SHALL representar cada operación como una tarjeta compacta tipo fila, mostrando de izquierda a derecha: miniatura de imagen (o ícono genérico si no hay), y a la derecha en dos líneas:
- Línea superior: Activo + Hora
- Línea inferior: Resultado (texto de color: verde=ganada, rojo=perdida, gris=break even) + Resultado en R coloreado (verde si positivo, rojo si negativo)

#### Scenario: Abrir detalle desde la tarjeta
- **GIVEN** el usuario ve una tarjeta en el listado
- **WHEN** toca la tarjeta
- **THEN** el sistema SHALL abrir la vista de detalle completo de esa operación

---

### Requirement: Vista de detalle para estudio de la operación
El sistema SHALL mostrar primero la imagen (si existe, en la parte superior), seguida de los campos organizados en secciones con encabezados claros (ej. "Datos generales", "Emociones", "Resultado"), terminando con la Descripción entrada al final — replicando el mismo orden de lectura del formulario.

#### Scenario: Ver detalle completo
- **GIVEN** el usuario abre una operación desde el listado
- **WHEN** la vista de detalle carga
- **THEN** el sistema SHALL mostrar todos los campos registrados, organizados en secciones legibles

#### Scenario: Editar una operación existente
- **GIVEN** el usuario está en la vista de detalle
- **WHEN** presiona "Editar"
- **THEN** el sistema SHALL abrir el formulario prellenado con los datos actuales y permitir guardar los cambios

#### Scenario: Eliminar una operación
- **GIVEN** el usuario está en la vista de detalle
- **WHEN** presiona "Eliminar" y confirma
- **THEN** el sistema SHALL eliminar permanentemente la operación y sus imágenes asociadas

#### Scenario: Eliminar y deshacer
- **GIVEN** el usuario elimina una operación desde el detalle
- **WHEN** la eliminación se confirma
- **THEN** el sistema SHALL mostrar un snackbar temporal con la opción "Deshacer"
- **AND** SHALL restaurar la operación si el usuario toca "Deshacer" dentro del tiempo disponible

---

### Requirement: Filtros y búsqueda combinados
El sistema SHALL permitir filtrar y buscar dentro del Historial, combinando filtros con lógica **AND** (una operación solo aparece si cumple todos los filtros activos simultáneamente).

**Filtros disponibles**: Fecha/periodo (Última semana, Último mes, Este mes, Últimos 3 meses, Personalizado), Activo, Resultado, Error, Emoción antes, Emoción después, y búsqueda por palabra clave.

- La búsqueda por palabra clave consulta `entryDescription`, `emotionBeforeReason`, `emotionAfterReason` y `errorReason`, y también puede coincidir parcialmente con nombres de catálogo seleccionados.
- "Última semana" filtra los últimos 7 días (es un filtro, no afecta la agrupación visual, que sigue siendo solo por mes).
- El último filtro relevante se persiste con **DataStore Preferences** y se restaura al reabrir Historial/la app. Claves guardadas: `lastPeriodFilter` (`LAST_WEEK`, `THIS_MONTH`, `CUSTOM`, `ALL_TIME`, etc.), `lastCustomRangeStart`/`lastCustomRangeEnd`, `lastSearchText`, `lastSelectedAssets`, `lastSelectedResults`, `lastSelectedErrors`, `lastSelectedEmotionBefore`, `lastSelectedEmotionAfter`.

#### Scenario: Filtrar por etiqueta
- **GIVEN** el usuario está en Historial
- **WHEN** aplica un filtro por Activo, Emoción, Error o Resultado
- **THEN** el sistema SHALL mostrar solo las operaciones que coincidan, manteniendo el agrupamiento por mes

#### Scenario: Filtrar por fecha
- **GIVEN** el usuario está en Historial
- **WHEN** selecciona una fecha específica o un rango
- **THEN** el sistema SHALL mostrar solo las operaciones dentro de ese rango

#### Scenario: Buscar por palabra clave
- **GIVEN** el usuario está en Historial
- **WHEN** ingresa una palabra clave en el buscador
- **THEN** el sistema SHALL mostrar las operaciones cuyos campos de texto (ver arriba) contengan esa palabra

#### Scenario: Combinar filtro de etiqueta y palabra clave
- **GIVEN** el usuario aplicó el filtro Activo = "XAUUSD"
- **WHEN** adicionalmente escribe "ruptura" en el buscador
- **THEN** el sistema SHALL mostrar solo operaciones de XAUUSD cuyos campos de texto contengan "ruptura"

#### Scenario: Limpiar filtros
- **GIVEN** el usuario tiene uno o más filtros activos
- **WHEN** toca "Limpiar filtros"
- **THEN** el sistema SHALL remover todos los filtros y mostrar el listado completo agrupado por mes

#### Scenario: Persistencia del filtro entre sesiones
- **GIVEN** el usuario aplicó filtros en Historial
- **WHEN** cierra la app y la reabre
- **THEN** el sistema SHALL restaurar el último estado de filtros guardado

---

### Requirement: Pestaña de Inicio (Métricas)
El sistema SHALL proveer una pestaña de Inicio con métricas/estadísticas y filtro por periodo, orientada a detectar patrones recurrentes. **No existe una pestaña separada de "Métricas"**; Inicio y Métricas son la misma pantalla.

Orden de la pantalla, de arriba hacia abajo:
1. Selector de periodo (Día/Semana/Mes/Últimos 3 meses/Personalizado), fijo en la parte superior
2. Tarjetas de resumen numérico: total de operaciones, % ganadas/perdidas/break even, calidad promedio, R total acumulado, R promedio por operación
3. Gráfica de línea de R acumulado
4. Gráfica de barras de distribución de resultados (% Ganadas/Perdidas/Break Even)
5. Ranking de las emociones y errores más frecuentes en el periodo
6. Botón flotante "+" en la esquina inferior derecha, sin tapar el contenido al hacer scroll

El sistema SHALL recordar el último filtro de periodo seleccionado (persistido con DataStore) y aplicarlo automáticamente la próxima vez que se abra la app.

#### Scenario: Filtrar métricas por periodo predefinido
- **GIVEN** el usuario está en Inicio
- **WHEN** selecciona un periodo (Día, Semana, Mes, Últimos 3 meses)
- **THEN** el sistema SHALL recalcular todas las métricas usando solo las operaciones dentro de ese periodo

#### Scenario: Filtrar métricas por rango personalizado
- **GIVEN** el usuario está en Inicio
- **WHEN** selecciona "Personalizado"
- **THEN** el sistema SHALL mostrar un calendario para elegir fecha de inicio y fin
- **AND** SHALL recalcular las métricas limitadas a ese rango una vez confirmado

#### Scenario: Persistencia del filtro de periodo
- **GIVEN** el usuario seleccionó el filtro "Semana" en Inicio
- **WHEN** cierra la app y la reabre más tarde
- **THEN** el sistema SHALL mostrar Inicio con el filtro "Semana" ya aplicado

#### Scenario: Sin operaciones en el periodo
- **GIVEN** no existen operaciones dentro del periodo seleccionado
- **WHEN** la pantalla se renderiza
- **THEN** el sistema SHALL mostrar un estado vacío indicando que no hay datos para ese periodo

---

### Requirement: Estados vacíos
El sistema SHALL mostrar un estado vacío amigable en Inicio y en Historial cuando no existan operaciones registradas o no haya datos para el filtro/periodo seleccionado.

#### Scenario: Primera vez sin operaciones registradas
- **GIVEN** el usuario no ha registrado ninguna operación aún
- **WHEN** abre Inicio o Historial
- **THEN** el sistema SHALL mostrar un mensaje amigable invitando a crear la primera operación, con un acceso directo al formulario

---

### Requirement: Codificación por color de valores positivos/negativos
El sistema SHALL usar color de forma consistente en toda la app: verde para valores/resultados positivos, rojo para negativos (tarjetas, detalle y métricas).

#### Scenario: Color en métricas agregadas
- **GIVEN** una métrica agregada tiene signo (ej. R total acumulado)
- **WHEN** se muestra en Inicio
- **THEN** el sistema SHALL mostrarla en verde si es positiva y en rojo si es negativa

---

### Requirement: Tema visual oscuro
El sistema SHALL utilizar un tema oscuro por defecto en toda la aplicación.

#### Scenario: Aplicación abre en modo oscuro
- **GIVEN** el usuario abre la app
- **WHEN** cualquier pantalla se renderiza
- **THEN** el sistema SHALL aplicar una paleta oscura (fondo oscuro, texto claro, acentos de color para ganada/perdida)

---

### Requirement: Orientación de pantalla
El sistema SHALL fijar la orientación en vertical, permitiendo rotación a horizontal únicamente en la vista de imagen a pantalla completa.

#### Scenario: Uso general en vertical
- **GIVEN** el usuario navega por Inicio, Historial o Etiquetas
- **WHEN** rota el celular
- **THEN** el sistema SHALL mantener la orientación vertical

#### Scenario: Rotación permitida al ver una imagen
- **GIVEN** el usuario está en la vista de imagen a pantalla completa
- **WHEN** rota el celular a horizontal
- **THEN** el sistema SHALL permitir la rotación

---

### Requirement: Permisos del dispositivo
El sistema SHALL solicitar permisos de Cámara y Almacenamiento/Fotos únicamente al intentar adjuntar una imagen, permitiendo el uso completo de la app sin ellos (excepto adjuntar imágenes).

#### Scenario: Rechazo de permisos
- **GIVEN** el usuario rechaza el permiso de Cámara o Almacenamiento
- **WHEN** intenta adjuntar una imagen
- **THEN** el sistema SHALL informar que la función no está disponible sin el permiso
- **AND** SHALL permitir continuar registrando la operación sin imagen con normalidad

---

### Requirement: Almacenamiento local persistente y eficiente
El sistema SHALL almacenar operaciones, catálogos e imágenes localmente, sin conexión a internet ni cuenta de usuario, priorizando bajo consumo de recursos. Las consultas de agrupamiento (por mes) y de métricas SHALL resolverse a nivel de base de datos (Room), sin cargar todos los registros a memoria.

#### Scenario: Uso sin conexión a internet
- **GIVEN** el dispositivo no tiene conexión a internet
- **WHEN** el usuario crea, edita, elimina o consulta operaciones
- **THEN** el sistema SHALL funcionar con normalidad

#### Scenario: Persistencia entre sesiones
- **GIVEN** el usuario cierra completamente la app
- **WHEN** vuelve a abrirla
- **THEN** el sistema SHALL mostrar todas las operaciones previamente registradas sin pérdida de datos

#### Scenario: Bajo consumo de recursos con historial grande
- **GIVEN** el usuario tiene cientos de operaciones con imágenes
- **WHEN** navega por el listado o Inicio
- **THEN** el sistema SHALL cargar los datos progresivamente (paginación/lazy loading), evitando picos de CPU/memoria

---

### Requirement: Navegación principal por pestañas
El sistema SHALL ofrecer una barra de navegación inferior con **exactamente 3 destinos** (no 4):

| Ícono | Pestaña | Contenido |
|---|---|---|
| 🏠 | Inicio | Pantalla de Métricas (mismo contenido; "Inicio" es su nombre en la navegación) |
| 📋 | Historial | Listado agrupado por Mes → Día, con filtros y búsqueda |
| ⚙️ | Etiquetas | Gestión de catálogos (Activos, Emociones, Errores) |

El ícono de la pestaña activa SHALL resaltarse (color de acento) frente a los inactivos (color neutro/atenuado). La app SHALL abrir siempre en Inicio.

#### Scenario: Apertura de la app
- **GIVEN** el usuario abre la app
- **WHEN** termina de cargar
- **THEN** el sistema SHALL mostrar la pestaña de Inicio

#### Scenario: Cambiar entre pestañas
- **GIVEN** el usuario está en cualquier pestaña
- **WHEN** toca otro destino en la barra inferior
- **THEN** el sistema SHALL cambiar a esa pestaña de forma inmediata

---

### Requirement: Acceso rápido para nueva operación
El sistema SHALL mostrar un botón de acción flotante ("+") en Inicio e Historial que abra directamente el formulario de nueva operación. La pestaña de Etiquetas **no** SHALL tener este botón (el agregar ocurre dentro de cada sección de catálogo).

#### Scenario: Crear operación desde el botón flotante
- **GIVEN** el usuario está en Inicio o en Historial
- **WHEN** toca el botón flotante "+"
- **THEN** el sistema SHALL abrir el formulario de "Nueva operación"

---

### Requirement: Formulario de registro — layout y controles de entrada
El sistema SHALL presentar el formulario en una sola columna, con controles especializados por tipo de dato, en este orden:

1. Fecha + Hora (misma fila)
2. Activo
3. Dirección
4. Calidad
5. Emoción antes + Motivo
6. Emoción después + Motivo
7. Error + Motivo
8. Resultado
9. Riesgo (%)
10. Resultado en R
11. Ratio planeado
12. Imagen adjunta
13. Descripción entrada (al final, campo multilínea expansible)

#### Scenario: Campos de porcentaje con sufijo fijo
- **GIVEN** el usuario está en "Riesgo (%)"
- **WHEN** escribe un valor
- **THEN** el sistema SHALL mostrar el símbolo "%" fijo al final del campo
- **AND** SHALL abrir el teclado numérico automáticamente al enfocar el campo

#### Scenario: Composición del valor de Resultado en R
- **GIVEN** el usuario selecciona "+" o "-" en un dropdown de dos opciones y escribe la magnitud numérica
- **WHEN** guarda la operación
- **THEN** el sistema SHALL almacenar el valor como decimal con signo (ej. +2, -1)
- **AND** SHALL mostrarlo en toda la UI (tarjetas, detalle, métricas) con el sufijo "R" fijo y no editable (ej. "+2R")
- **AND** el teclado numérico del campo SHALL restringirse a dígitos y separador decimal; el signo se define exclusivamente por el dropdown, nunca escrito como texto

#### Scenario: Entrada de fecha con máscara y selector visual
- **GIVEN** el usuario está en el campo "Fecha"
- **WHEN** interactúa con el campo
- **THEN** el sistema SHALL permitir escribir directamente en formato dd/MM/aaaa
- **AND** SHALL ofrecer un ícono de calendario para abrir un selector visual como alternativa

#### Scenario: Entrada de hora con máscara y selector visual
- **GIVEN** el usuario está en el campo "Hora"
- **WHEN** interactúa con el campo
- **THEN** el sistema SHALL permitir escribir directamente en formato HH:mm
- **AND** SHALL ofrecer un ícono de reloj para abrir un selector visual como alternativa

#### Scenario: Teclado contextual automático
- **GIVEN** el usuario toca cualquier campo del formulario
- **WHEN** el campo obtiene el foco
- **THEN** el sistema SHALL abrir automáticamente el teclado correspondiente (numérico o de texto)

#### Scenario: Texto de ayuda visible bajo cada campo
- **GIVEN** el usuario está llenando el formulario
- **WHEN** cualquier campo se muestra
- **THEN** el sistema SHALL mostrar un texto de ayuda pequeño debajo del campo, siempre visible

#### Scenario: Sin corte de palabras en campos largos
- **GIVEN** el usuario está en un campo de texto libre (ej. "Motivo emoción antes")
- **WHEN** el texto de ayuda o la etiqueta es larga
- **THEN** el sistema SHALL mostrarla en una sola columna de ancho completo, sin fragmentar palabras

#### Scenario: Confirmación al salir con cambios pendientes
- **GIVEN** el usuario ha modificado al menos un campo del formulario
- **WHEN** intenta navegar a otra pestaña o presiona "atrás" sin guardar
- **THEN** el sistema SHALL mostrar un diálogo de confirmación indicando que los cambios se perderán
- **AND** SHALL permitir cancelar (quedarse) o confirmar (salir sin guardar)

---

## 5. Diseño visual por pantalla (resumen)

- **Barra de navegación inferior**: 3 íconos fijos (🏠 📋 ⚙️), con la pestaña activa resaltada por color de acento.
- **Etiquetas**: 3 secciones diferenciadas (Activos, Emociones, Errores), cada una con su lista, un botón "+ Agregar" al final, y acciones de editar/eliminar visibles al tocar o deslizar cada elemento (lápiz / basura). Incluye el indicador de espacio en disco usado por imágenes. Sin botón flotante "+".
- **Visor de imagen a pantalla completa**: imagen ocupando el máximo espacio posible, fondo negro/oscuro, indicador de puntos si hay más de una imagen, zoom por gesto de pellizco.

---

## 6. Estructura de paquetes sugerida

- `feature.registro`
- `feature.historial`
- `feature.etiquetas`
- `feature.metricas`
- `core.data`
- `core.model`
- `core.ui`
- `core.navigation`

---

## 7. Testing

### Alcance para v1
- **Unit tests (obligatorios)**:
  - **ViewModels**: transformación de estado de UI, cálculo de métricas (R acumulado, R promedio, % ganadas/perdidas/BE, ranking de etiquetas), validaciones de formulario (rangos de Calidad, Riesgo %, Resultado en R).
  - **Repositories**: mapeo entre entidades de Room y modelos de dominio, lógica de combinación de filtros (AND).
  - **DAO (Room)**: con base de datos en memoria, validando que las consultas de agrupamiento (por mes) y de filtros combinados devuelven los resultados esperados.
- **Fuera de alcance en v1**: tests de UI/instrumentados end-to-end (Compose UI tests); se evalúan como mejora futura.

### Requirement: Cobertura de pruebas mínima
El sistema SHALL incluir pruebas unitarias para el cálculo de métricas y validación de formularios en los ViewModels, y para el filtrado combinado en los Repositories, aprovechando el diseño por contratos (interfaces) para usar fakes/mocks sin depender de la base de datos real.

---

## 8. Performance y límites

- El Historial SHALL soportar entre **3,000 y 5,000 operaciones** sin degradar el scroll, usando carga paginada (Paging 3).
- Las miniaturas de imagen SHALL generarse una sola vez al guardar la operación, no en cada render.
- Las consultas de agrupamiento (por mes) y de métricas SHALL resolverse a nivel de base de datos, no en memoria del cliente.
- Cada imagen comprimida debería pesar aproximadamente 200–400KB.

---

## 9. Non-Functional Requirements

- **Rendimiento de registro**: crear una nueva operación no SHALL requerir más de ~30 segundos de interacción para un usuario que ya conoce el formulario.
- **Eficiencia**: la app SHALL evitar procesos en segundo plano innecesarios, minimizar uso de batería/CPU, y comprimir imágenes para no consumir almacenamiento ni memoria en exceso.
- **Plataforma**: Android nativo (Kotlin + Jetpack Compose), instalación en v1 vía APK manual.
- **Base de datos local**: Room (sobre SQLite).
- **Backups**: fuera de alcance en v1 (posible mejora futura: exportar/importar datos, útil ante cambio de dispositivo).

---

## 10. Out of Scope (v1)

- Sincronización en la nube / multi-dispositivo.
- Múltiples usuarios o autenticación.
- Integración automática con brokers/MT4/MT5.
- Modo claro (tema alternativo).
- Publicación en Google Play Store (evaluado para v2).
- Exportar/importar backups.
- Agrupación por semana en el Historial (solo existe como filtro).
- Tests de UI/instrumentados end-to-end.

---

## 11. Próximos pasos sugeridos

1. Definir las entidades de Room concretas con su anotación `@Entity`.
2. Diseñar los DAOs para: operaciones con filtros combinados, catálogo por tipo, imágenes de operación.
3. Decidir si la búsqueda de texto usa `LIKE` o `FTS` en SQLite.
4. Definir los nombres precisos de las rutas de navegación Compose.
5. Aprobar el conjunto exacto de valores iniciales de catálogo (semilla).
