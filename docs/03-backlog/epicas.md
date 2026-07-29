# Épicas — Bitácora de Trading (Forex)

> Fuente: `docs/01-prd/bitacora-trading-forex.md` (PRD, modo one-pager) + `Especificacion_Bitacora_Trading.md` (detalle funcional §4, usado originalmente para derivar capabilities; el PRD §5 ya se completó después, ver nota superior del PRD).
>
> Cada épica se traza a ≥ 1 objetivo del PRD (sección 1). Cada objetivo del PRD queda cubierto por ≥ 1 épica.

## Mapa de trazabilidad

| Épica | Objetivo(s) del PRD que cubre |
|---|---|
| EP-001 — Registro rápido de operaciones | Obj. 1 (registro ≤30s), Obj. 5 (imágenes comprimidas) |
| EP-002 — Gestión de catálogos (Etiquetas) | Obj. 3 (detección de patrones), Obj. 5 (espacio en disco) |
| EP-003 — Historial y estudio de operaciones | Obj. 2 (historial agrupado, scroll fluido), Obj. 3 (detección de patrones) |
| EP-004 — Métricas y detección de patrones (Inicio) | Obj. 3 (detección de patrones) |
| EP-005 — Plataforma base: navegación, tema y almacenamiento offline | Obj. 4 (100% offline, sin pérdida de datos), Obj. 5 (almacenamiento eficiente) |

**Objetivos del PRD (referencia)**:
1. Registrar una operación completa en ≤30 segundos de interacción.
2. Historial agrupado por mes, scroll fluido con 3,000–5,000 operaciones.
3. Ranking de emociones/errores más frecuentes → detección de patrones.
4. Operar 100% offline, sin pérdida de datos entre sesiones.
5. Consumo de almacenamiento bajo control (imágenes comprimidas ~200–400KB).

---

## EP-001 — Registro rápido de operaciones

**Construcción**: `openspec_change: registro-rapido-operaciones` (rama `feature/ep-001-registro-rapido-operaciones`).

**Resumen**: cubre el formulario de "Nueva operación" completo — sus 16 campos, el orden de layout, los controles especializados por tipo de dato, los valores por defecto (fecha/hora actual, XAUUSD), las validaciones de rango/obligatoriedad, y el adjuntado de hasta 2 imágenes comprimidas. Es el punto de entrada de todo el dato que luego se estudia en Historial e Inicio.

**Justificación**: es la épica que sostiene directamente el objetivo #1 del PRD (registro en ≤30s) — la prioridad #1 declarada en la Introducción del PRD ("rapidez de registro"). También contribuye al objetivo #5 (imágenes comprimidas) al definir dónde y cómo se adjuntan.

**Capabilities incluidas**:
- Registro de operación (formulario con los 16 campos, en el orden fijo de layout de una columna).
- Reglas de validación (rangos de Calidad, Riesgo %, Resultado en R; fecha/hora no futura; catálogos sin duplicados).
- Registro rápido con valores por defecto (fecha/hora actual, XAUUSD si el catálogo de Activos está vacío).
- Adjuntar imagen a la operación (cámara o galería, máx. 2, redimensionadas y comprimidas ~200-400KB, miniatura generada una sola vez).
- Acceso rápido para nueva operación (botón flotante "+" en Inicio e Historial).
- Confirmación al salir del formulario con cambios pendientes.

**Historias previstas** (alta vista, se detallan en `docs/04-historias/`):
- HU-001 — Registrar operación con campos identificadores mínimos (Fecha, Hora, Activo, Dirección, Resultado)
- HU-002 — Completar operación con campos de catálogo obligatorios (Calidad, Emoción antes/después, Error)
- HU-003 — Agregar descripción de entrada a la operación (texto libre multilínea)
- HU-004 — Validar rangos numéricos y de fecha/hora al guardar (Calidad, Riesgo %, Resultado en R, fecha/hora no futura)
- HU-005 — Prellenar fecha/hora y activo por defecto al abrir "Nueva operación"
- HU-006 — Adjuntar imagen sin procesar (cámara o galería, máx. 2, incluye flujo de permisos)
- HU-007 — Comprimir, redimensionar y generar miniatura cacheada al guardar
- HU-008 — Crear operación desde el botón flotante "+"
- HU-009 — Confirmar antes de salir del formulario con cambios sin guardar

> Nota: HU-001, HU-002 y HU-003 son la partición vertical de lo que originalmente era una única historia "Crear una operación completa" — dividida tras fallar los criterios INVEST **I** (dependía implícitamente de EP-002 vía el catálogo de Emociones, sin semilla obligatoria) y **S** (10 campos heterogéneos + persistencia excedía 2-4 días). Ver `docs/04-historias/HU-001-registrar-campos-minimos.md` §Contexto.
>
> Nota: HU-006 y HU-007 son la partición vertical de lo que originalmente era una única historia "Adjuntar hasta 2 imágenes" — dividida tras fallar el criterio INVEST **S** (cámara/galería + permisos + resize + compresión + miniatura excedía 2-4 días). Ver `docs/04-historias/HU-006-adjuntar-imagen-sin-procesar.md` §Contexto.

**Métrica de éxito de la épica**: un usuario ya familiarizado con el formulario percibe el registro de una operación como rápido (~30 segundos de referencia, meta aspiracional no bloqueante — KPI #1 del PRD, ajustado post-`/trycore:revisar` para no exigir verificación instrumentada en un producto de un solo usuario).

---

## EP-002 — Gestión de catálogos (Etiquetas)

**Resumen**: cubre la pestaña "Etiquetas" — administración de los tres catálogos (Activos, Emociones, Errores) compartiendo la misma entidad `CatalogItem`, incluyendo semillas (`Ninguno` obligatoria para Errores, `XAUUSD` obligatoria para Activos, y un set inicial **opcional** para Emociones — ver nota abajo), la posibilidad de agregar una etiqueta nueva sin salir del formulario de registro, y la visualización del espacio en disco usado por imágenes.

> **Decisión de producto (post `/trycore:invest` sobre la historia original de EP-001)**: el catálogo `EMOTION`, que la especificación original dejaba sin semilla obligatoria, ahora se siembra con un set inicial de valores por defecto (Confianza, Nervios, Ansiedad, Euforia, Frustración, Alivio, Miedo, Calma — editable desde Etiquetas) para que el registro de operaciones (EP-001, HU-002) sea completable desde el primer uso sin depender de esta épica. Detalle en `docs/04-historias/HU-002-completar-campos-catalogo.md` §Notas técnicas.

**Justificación**: catálogos limpios y bien mantenidos son condición necesaria para que el ranking de emociones/errores (EP-004) sea útil — datos duplicados o mal etiquetados rompen la detección de patrones (objetivo #3). También expone el espacio en disco usado por imágenes, relevante para el objetivo #5.

**Capabilities incluidas**:
- Gestión de catálogos (Activos, Emociones, Errores): agregar, editar, eliminar.
- Reglas de unicidad (sin duplicados, comparación case-insensitive) y semillas no eliminables (`Ninguno`, `XAUUSD` con excepción).
- Confirmación al eliminar un elemento de catálogo en uso (con conteo de operaciones afectadas), conservando el valor histórico.
- Agregar etiqueta nueva sin salir del formulario de operación ("+ Agregar nueva").
- Visualización del espacio en disco usado por imágenes.

**Historias previstas**:
- HU-010 — Agregar elemento a un catálogo (Activos, Emociones o Errores)
- HU-011 — Editar y eliminar elemento de un catálogo (con protección de semillas)
- HU-012 — Confirmar eliminación de un elemento de catálogo en uso
- HU-013 — Agregar una nueva etiqueta sin salir del formulario de operación
- HU-014 — Ver espacio en disco usado por imágenes

> Nota: HU-010 y HU-011 son la partición vertical de lo que originalmente era una única historia "Administrar catálogo de Activos, Emociones y Errores" — dividida tras fallar el criterio INVEST **S** (3 catálogos × 3 operaciones con reglas de negocio distintas excedía 2-4 días). Ver `docs/04-historias/HU-010-administrar-catalogos.md` §Contexto.

**Métrica de éxito de la épica**: 0 duplicados permitidos en catálogos; elementos semilla (`Ninguno`, `XAUUSD`) siempre presentes cuando el catálogo correspondiente queda vacío.

---

## EP-003 — Historial y estudio de operaciones

**Resumen**: cubre la pestaña "Historial" — listado agrupado por mes y día (mes más reciente expandido por defecto), tarjeta compacta por operación, vista de detalle completo (editar/eliminar con deshacer), y filtros/búsqueda combinados con lógica AND, persistidos entre sesiones.

**Justificación**: sostiene directamente el objetivo #2 del PRD (historial agrupado por mes, scroll fluido con 3,000–5,000 operaciones vía Paging 3) y contribuye al objetivo #3, ya que filtrar/buscar por emoción, error o palabra clave es una de las formas en que el usuario detecta patrones manualmente antes de llegar al ranking automático de Inicio.

**Capabilities incluidas**:
- Listado de Historial agrupado por mes → día, orden más reciente primero, paginado (Paging 3).
- Resumen rápido por mes (n° operaciones, % ganadas, R acumulado) y colapsar/expandir grupo.
- Tarjeta de operación en el listado (miniatura, activo + hora, resultado + R coloreado).
- Vista de detalle para estudio (secciones legibles, imagen primero, editar, eliminar con deshacer vía snackbar).
- Visor de imagen a pantalla completa (zoom, navegación entre imágenes, rotación excepcional a horizontal).
- Filtros y búsqueda combinados (fecha/periodo, activo, resultado, error, emoción antes/después, palabra clave) con lógica AND.
- Persistencia del último filtro relevante (DataStore Preferences).
- Estados vacíos en Historial (sin operaciones o sin resultados para el filtro).

**Historias previstas**:
- HU-015 — Ver historial agrupado por mes y día con tarjeta compacta
- HU-016 — Garantizar scroll fluido con historiales grandes (3,000-5,000 operaciones)
- HU-017 — Ver resumen rápido por mes (operaciones, % ganadas, R acumulado) y colapsar/expandir
- HU-018 — Abrir detalle completo de una operación desde la tarjeta
- HU-019 — Ver imagen en pantalla completa con zoom y navegación entre imágenes
- HU-020 — Editar una operación existente
- HU-021 — Eliminar una operación con opción de deshacer
- HU-022 — Filtrar operaciones por etiqueta y por fecha/periodo (AND)
- HU-023 — Buscar por palabra clave, combinada con los filtros de etiqueta
- HU-024 — Persistir el último filtro de Historial entre sesiones
- HU-025 — Ver estado vacío en Historial sin operaciones registradas

> Nota: HU-019 se agregó tras detectar un gap de cobertura — la spec define un Requirement completo de visor de imagen (zoom, navegación, rotación) que no tenía historia propia. Ver `docs/04-historias/HU-019-visor-imagen-pantalla-completa.md` §Contexto.

> Nota: HU-015 y HU-016 son la partición vertical de lo que originalmente era una única historia "Ver historial agrupado por mes y día" — dividida tras fallar el criterio INVEST **S** (mezclaba la funcionalidad de agrupación con la garantía de fluidez a escala, dos ciclos de verificación distintos). Ver `docs/04-historias/HU-015-historial-agrupado-mes-dia.md` §Contexto.

> Nota: HU-022 y HU-023 son la partición vertical de lo que originalmente era una única historia "Filtrar y buscar operaciones combinando criterios" — dividida proactivamente por el mismo patrón de tamaño (6 tipos de filtro de etiqueta + fecha + búsqueda de texto + combinación AND excedía el umbral ya observado en HU-001/006/010/015). Ver `docs/04-historias/HU-022-filtrar-etiqueta-fecha.md` §Contexto.

**Métrica de éxito de la épica**: scroll fluido sin caídas de fluidez perceptibles con 3,000–5,000 operaciones cargadas de forma paginada (KPI #3 del PRD).

---

## EP-004 — Métricas y detección de patrones (Inicio)

**Resumen**: cubre la pestaña "Inicio" (única pantalla de métricas, sin pestaña separada) — tarjetas de resumen numérico, gráfica de R acumulado, gráfica de distribución de resultados, y el ranking de emociones/errores más frecuentes por periodo, con selector de periodo persistido entre sesiones.

**Justificación**: es la épica que materializa de forma directa el objetivo #3 del PRD — exponer, sin esfuerzo manual, qué emociones y errores se repiten más y así permitir la mejora continua declarada en el propósito del producto.

**Capabilities incluidas**:
- Pestaña de Inicio (Métricas): selector de periodo, tarjetas de resumen (total operaciones, % ganadas/perdidas/BE, calidad promedio, R total, R promedio).
- Gráfica de línea de R acumulado y gráfica de barras de distribución de resultados.
- Ranking de emociones y errores más frecuentes en el periodo seleccionado.
- Filtro de periodo predefinido y personalizado, persistido entre sesiones (DataStore Preferences).
- Codificación por color de valores positivos/negativos (verde/rojo) consistente con el resto de la app.
- Estado vacío en Inicio cuando no hay operaciones en el periodo.

**Historias previstas**:
- HU-026 — Ver resumen numérico y gráficas de R acumulado / distribución de resultados
- HU-027 — Ver ranking de emociones y errores más frecuentes por periodo
- HU-028 — Filtrar métricas por periodo predefinido
- HU-029 — Filtrar métricas por rango personalizado
- HU-030 — Persistir el filtro de periodo de Inicio entre sesiones
- HU-031 — Ver estado vacío en Inicio sin operaciones en el periodo

**Métrica de éxito de la épica**: el ranking de emociones/errores se recalcula correctamente para cualquier periodo seleccionado (predefinido o personalizado), sin intervención manual del usuario.

---

## EP-005 — Plataforma base: navegación, tema y almacenamiento offline

**Construcción**: `openspec_change: plataforma-base-navegacion-tema-offline` (rama `feature/ep-005-plataforma-base-navegacion-tema-offline`) — primer slice construido del proyecto.

**Resumen**: cubre las capacidades transversales que sostienen a las demás épicas — navegación inferior de 3 pestañas, tema oscuro por defecto, orientación de pantalla, permisos de dispositivo bajo demanda, y almacenamiento 100% local/offline sin pérdida de datos.

**Justificación**: sostiene directamente el objetivo #4 del PRD (100% offline, sin pérdida de datos) y contribuye al objetivo #5 (eficiencia de almacenamiento) al definir cómo se resuelven las consultas de agrupamiento/métricas a nivel de base de datos sin cargar todo a memoria. Es una épica de infraestructura/plataforma, no de feature visible aislada, pero condiciona el resto del producto.

**Capabilities incluidas**:
- Navegación principal por pestañas (Inicio / Historial / Etiquetas), apertura siempre en Inicio.
- Tema visual oscuro por defecto en toda la app.
- Orientación de pantalla fija en vertical en el resto de la app (la excepción del visor de imagen a pantalla completa es responsabilidad de **HU-019**, EP-003 — no revalidar la rotación permitida ahí, solo la regla general de "vertical" aquí).
- ~~Permisos de dispositivo (Cámara, Almacenamiento) solicitados solo al adjuntar imagen~~ — cubierta funcionalmente por **HU-006** (EP-001, ver Escenario 3 de sus AC); no requiere historia propia en esta épica. Ver nota de solapamiento resuelto en "Reporte de cobertura".
- Almacenamiento local persistente y eficiente (Room/SQLite), consultas de agrupamiento y métricas resueltas en base de datos, sin conexión a internet ni cuenta de usuario.

**Historias previstas**:
- HU-032 — Navegar entre Inicio, Historial y Etiquetas
- HU-033 — Aplicar tema oscuro en toda la app
- HU-034 — Mantener orientación vertical en el resto de la app (la excepción del visor de imagen vive en HU-019)
- HU-035 — Operar 100% offline sin pérdida de datos entre sesiones

**Métrica de éxito de la épica**: 0 incidentes de pérdida de datos entre sesiones; 100% de los flujos (crear, editar, eliminar, consultar) funcionan sin conexión a internet (KPI #4 del PRD).

---

## Reporte de cobertura

- **Objetivos huérfanos** (sin épica que los cubra): ninguno. Los 5 objetivos del PRD tienen ≥1 épica asignada (ver mapa de trazabilidad).
- **Épicas sin objetivo claro**: ninguna. Las 5 épicas trazan a ≥1 objetivo cada una.
- **Nota de solapamiento intencional**: el objetivo #3 (detección de patrones) y el objetivo #5 (almacenamiento eficiente) son cubiertos por más de una épica de forma deliberada — son objetivos transversales que varias capabilities distintas sostienen en conjunto, no una duplicación de alcance entre épicas.
- **Solapamiento resuelto (post `/trycore:invest` sobre HU-006)**: la historia originalmente prevista en EP-005 para "solicitar permisos de cámara/almacenamiento solo al adjuntar imagen" se retiró del backlog. El agente `invest-validator` determinó que no tenía alcance propio: pedir el permiso y manejar su rechazo es intrínseco al acto de adjuntar una imagen, y ese flujo completo (incluida la degradación graceful si se rechaza) ya vive en **HU-006** (EP-001). No recrear esta historia en EP-005 en futuras pasadas de `/trycore:historia` o `/trycore:epicas`.
- **Gap de cobertura resuelto**: el visor de imagen a pantalla completa (zoom, navegación entre imágenes, rotación) no tenía historia propia pese a estar completamente especificado en la spec y referenciado por EP-005. Se agregó **HU-019** en EP-003.
- **Backlog completo**: las 35 historias previstas (HU-001 a HU-035) tienen archivo propio en `docs/04-historias/`, con AC en Given/When/Then y `estado: lista` (pasaron las 6 letras INVEST). 5 historias originales se dividieron en 10 tras fallar el criterio **S** durante `/trycore:invest` (ver notas en cada épica): HU-001→HU-001/002/003, HU-006→HU-006/007, HU-010→HU-010/011, HU-015→HU-015/016, HU-022→HU-022/023. Próximo paso natural: `/trycore:backlog` para consolidar, o `/trycore:mapa` para el User Story Map.
- **Post-`/trycore:revisar` (2026-07-27)**: se detectó y corrigió un gap de trazabilidad — Riesgo (%), Resultado en R y los 3 motivos en texto libre (`emotionBeforeReason`/`emotionAfterReason`/`errorReason`) eran consumidos por AC de HU-004/015/017/023/026 sin que ninguna historia los creara. Se extendieron HU-001 (Riesgo %, Resultado en R) y HU-002 (los 3 motivos) para cubrirlos. También se subieron HU-006/HU-007 a Must (única implementación del objetivo #5 del PRD) — ver `docs/05-priorizacion/moscow-2026-07-27.md`. Detalle completo en `docs/.reviews/20260727-2138-global.md`.
