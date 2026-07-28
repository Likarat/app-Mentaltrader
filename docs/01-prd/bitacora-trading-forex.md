# PRD — Bitácora de Trading (Forex)

> **Estado**: draft (one-pager)
> **Versión**: 0.1
> **Última actualización**: 2026-07-27
> **Sponsor**: Miguel Torres (proyecto personal, single-user)

> Modo **One-Pager**: componentes 1, 2, 3, 5 y 12 completos (5 se rellenó para cerrar un gap de trazabilidad con las gráficas de Inicio, ver `docs/03-backlog/epicas.md` EP-004). Los componentes 4, 6-11 quedan con placeholder `<!-- TODO -->` para expandir con `/trycore:prd` (modo completo) cuando el proyecto lo requiera. Fuente principal: `Especificacion_Bitacora_Trading.md` (especificación funcional/técnica ya consolidada).

---

## 1. Introducción y objetivos

**Problema**: un trader de forex no cuenta con una forma rápida y sencilla de registrar sus operaciones (contexto, emociones, errores, resultado) ni de visualizar ese historial de forma que le permita detectar patrones — buenos y malos — que mejoren su desempeño de forma continua. Las alternativas genéricas (notas, hojas de cálculo) son lentas de llenar en el momento y difíciles de estudiar después.

**Contexto**: proyecto personal, de uso individual (sin backend ni cuentas), pensado para un solo usuario — el propio dueño del producto. No existe una versión previa de la app; hoy el registro (si ocurre) es manual y disperso. Se prioriza una app Android nativa, 100% local/offline-first, con tema oscuro por defecto para facilitar el estudio de cada entrada.

**Objetivos** (medibles):
- Permitir registrar una operación completa en **~30 segundos** de interacción para un usuario ya familiarizado con el formulario — meta aspiracional de referencia (no un umbral bloqueante ni verificado con tests instrumentados), ya que el único usuario del producto es quien lo desarrolla.
- Mostrar el historial agrupado por mes (y por día dentro del mes) con scroll fluido soportando **3,000–5,000 operaciones** sin degradar la experiencia.
- Exponer en la pantalla de Inicio un ranking de las **emociones y errores más frecuentes** por periodo, para facilitar la detección de patrones recurrentes.
- Operar **100% offline**, sin pérdida de datos entre sesiones ni dependencia de conexión a internet.
- Mantener el consumo de almacenamiento bajo control: imágenes adjuntas comprimidas a **~200–400KB** cada una (máx. 2 por operación).

## 2. Stakeholders

| Rol | Nombre / equipo | Interés / responsabilidad |
|---|---|---|
| Sponsor | Miguel Torres | Financia y define el alcance; único interesado del proyecto |
| Product Owner | Miguel Torres | Decide prioridades y acepta los criterios de "hecho" |
| Equipo técnico | Miguel Torres (+ asistencia de agentes de IA) | Desarrollo Android nativo (Kotlin + Jetpack Compose) |
| Usuario primario | Miguel Torres (trader de forex) | Registra operaciones y estudia su historial a diario |
| Usuario secundario | N/A | Producto de uso estrictamente individual en v1 |
| Operaciones / soporte | N/A | Sin soporte externo; mantenimiento propio |

## 3. Historias de Usuarios (de alto nivel)

> Detalle por historia (con AC en Given/When/Then) se escribirá en `docs/04-historias/` vía `/trycore:historia` + `/trycore:ac`.

- Como trader de forex, necesito registrar una operación en menos de 30 segundos para no interrumpir mi rutina de trading.
- Como trader de forex, necesito ver mi historial agrupado por mes para estudiar mi evolución sin perderme en el detalle día a día.
- Como trader de forex, necesito ver qué emociones y errores se repiten más seguido para identificar patrones que dañan mi rentabilidad.
- Como trader de forex, necesito adjuntar capturas del gráfico a cada operación para revisar visualmente el contexto de la entrada al estudiarla después.
- Como trader de forex, necesito filtrar y buscar en mi historial (por activo, resultado, emoción, error o palabra clave) para encontrar rápido las operaciones que quiero analizar.

## 4. Componentes principales y sitemap

<!-- TODO: expandir en modo completo. Referencia disponible en Especificacion_Bitacora_Trading.md §2 y §6 (stack, estructura de paquetes) y §5 (diseño visual por pantalla). Sitemap de alto nivel ya insinuado: navegación inferior con 3 destinos — Inicio, Historial, Etiquetas. -->

## 5. Características y funcionalidades

Agrupadas por capability (alineadas 1:1 con las épicas de `docs/03-backlog/epicas.md`):

### Capability 1: Registro rápido de operaciones
- Formulario de "Nueva operación" con 16 campos, en una sola columna y orden fijo de layout.
- Valores por defecto: fecha/hora actual, Activo = XAUUSD si el catálogo está vacío.
- Validaciones de rango (Calidad 0.0-10.0, Riesgo % 0-100, Resultado en R con signo) y de obligatoriedad, con fecha/hora futura bloqueada.
- Adjuntar hasta 2 imágenes (cámara o galería), redimensionadas y comprimidas (~200-400KB), con miniatura generada una sola vez al guardar.
- Botón flotante "+" en Inicio e Historial para acceso directo al formulario.
- Confirmación al salir del formulario con cambios sin guardar.

### Capability 2: Gestión de catálogos (Etiquetas)
- Administración de tres catálogos (Activos, Emociones, Errores) sobre la misma entidad `CatalogItem`: agregar, editar, eliminar.
- Semillas no eliminables (`Ninguno` para Errores; `XAUUSD` para Activos, salvo que exista otro activo).
- Sin duplicados (comparación case-insensitive); al eliminar un elemento en uso, se pide confirmación mostrando en cuántas operaciones se usa y se conserva el valor histórico.
- Agregar una etiqueta nueva sin salir del formulario de operación ("+ Agregar nueva").
- Indicador de espacio en disco usado por imágenes.

### Capability 3: Historial y estudio de operaciones
- Listado agrupado por mes → día, orden más reciente primero, carga paginada (Paging 3) para 3,000-5,000 operaciones sin degradar el scroll.
- Resumen rápido por mes (n° operaciones, % ganadas, R acumulado) y colapsar/expandir grupo.
- Tarjeta compacta por operación (miniatura, activo + hora, resultado + R coloreado).
- Vista de detalle para estudio (imagen primero, secciones legibles), con editar y eliminar (con deshacer vía snackbar).
- Filtros y búsqueda combinados con lógica AND (fecha/periodo, activo, resultado, error, emoción antes/después, palabra clave), con el último filtro persistido entre sesiones.
- Estado vacío cuando no hay operaciones o no hay resultados para el filtro.

### Capability 4: Métricas y detección de patrones (Inicio)
- Selector de periodo (Día/Semana/Mes/Últimos 3 meses/Personalizado), persistido entre sesiones.
- Tarjetas de resumen numérico: total de operaciones, % ganadas/perdidas/break even, calidad promedio, R total acumulado, R promedio por operación.
- **Gráfica de línea de R acumulado** en el periodo seleccionado.
- **Gráfica de barras de distribución de resultados** (% Ganadas/Perdidas/Break Even).
- Ranking de las emociones y errores más frecuentes en el periodo.
- Estado vacío cuando no hay operaciones en el periodo seleccionado.

### Capability 5: Plataforma base (navegación, tema, almacenamiento offline)
- Navegación inferior de 3 pestañas (Inicio / Historial / Etiquetas), apertura siempre en Inicio.
- Tema oscuro por defecto en toda la app.
- Orientación fija en vertical, excepto en el visor de imagen a pantalla completa (con zoom).
- Permisos de Cámara/Almacenamiento solicitados solo al adjuntar imagen; el resto de la app funciona sin ellos.
- Almacenamiento 100% local (Room/SQLite), sin conexión a internet ni cuenta de usuario; consultas de agrupamiento y métricas resueltas en base de datos, no en memoria.

## 6. Diseño y experiencia del usuario

<!-- TODO: expandir en modo completo. Definiciones ya tomadas en Especificacion_Bitacora_Trading.md: tema oscuro por defecto, español (v1), una sola columna en formularios, orientación vertical fija (excepto visor de imagen). -->

## 7. Requisitos técnicos

<!-- TODO: expandir en modo completo. Stack ya decidido en Especificacion_Bitacora_Trading.md §2: Kotlin + Jetpack Compose, MVVM, Room, Coil, Navigation Compose, DataStore Preferences, Paging 3, minSdk 27, sin backend. -->

## 8. Planificación del proyecto

<!-- TODO: expandir en modo completo. Ver Especificacion_Bitacora_Trading.md §11 "Próximos pasos sugeridos" como insumo inicial para fases/hitos. -->

## 9. Criterios de aceptación (nivel producto)

<!-- TODO: expandir en modo completo. Existe ya un set extenso de Scenarios G/W/T a nivel funcional en Especificacion_Bitacora_Trading.md §4, que servirán de insumo directo para las Historias de Usuario (docs/04-historias/). -->

## 10. Apéndices y recursos adicionales

- Documento fuente: `Especificacion_Bitacora_Trading.md` (especificación unificada funcional + técnica + modelo de datos), en la raíz del proyecto.

## 11. Non-goals (fuera de alcance)

Explícitamente NO se incluye en esta iteración (v1) — ver detalle en `Especificacion_Bitacora_Trading.md` §10:
- Sincronización en la nube / multi-dispositivo.
- Múltiples usuarios o autenticación.
- Integración automática con brokers/MT4/MT5.
- Modo claro (tema alternativo); solo tema oscuro en v1.
- Publicación en Google Play Store (evaluado para v2; v1 se distribuye vía APK manual).
- Exportar/importar backups.
- Agrupación por semana en el Historial (solo existe como filtro, no como nivel de agrupación).
- Tests de UI/instrumentados end-to-end (solo unit tests en v1).

## 12. Métricas de éxito (KPIs)

| KPI | Línea base | Meta | Cuándo se mide |
|---|---|---|---|
| Tiempo de registro de una operación | N/A — no existe app ni bitácora digital hoy | **Meta aspiracional, no bloqueante**: ~30 segundos de interacción como referencia de "rápido", usuario ya familiarizado con el formulario. Al ser un producto de uso estrictamente personal (el único usuario es quien lo desarrolla), no amerita un AC de timing instrumentado — el propio usuario juzga si el flujo se siente ágil, sin ser una camisa de fuerza ni tampoco una excusa para un flujo lento/ineficiente | Percepción subjetiva del propio usuario durante el uso diario tras v1, sin prueba formal instrumentada |
| Frecuencia de uso semanal | 0 (sin bitácora digital hoy) | ≥3 registros/semana durante el primer mes de uso post-lanzamiento | Revisión manual al mes de haber empezado a usar la app |
| Operaciones soportadas sin degradar el scroll del Historial | N/A — sin implementación previa | 3,000–5,000 operaciones sin caídas de fluidez perceptibles | Prueba de carga con dataset sintético antes de dar v1 por "hecho" |
| Pérdida de datos entre sesiones | N/A — sin implementación previa | 0 incidentes de pérdida de datos | Continuo, durante el uso diario tras el lanzamiento de v1 |

---

<!-- Este PRD es de uso personal/individual; no se anexa Anexo A "para agentes" salvo que el usuario re-invoque /trycore:prd en modo para-agentes. -->
