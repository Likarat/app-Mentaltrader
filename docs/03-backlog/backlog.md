# Backlog — Bitácora de Trading (Forex)

> El orden de las filas **es** la priorización. Priorizado con MoSCoW el 2026-07-27 (ver `docs/05-priorizacion/moscow-2026-07-27.md`); dentro de cada categoría, el orden respeta las dependencias documentadas en "Notas". Actualizado el mismo día tras `/trycore:revisar`: HU-006/HU-007 subieron de Should/Could a Must (ver nota de priorización) y se corrigieron referencias de ID y conteos.

**Última actualización**: 2026-07-27
**Framework activo**: MoSCoW

## Resumen

- Total historias: 35
- En estado `lista`: 35
- En estado `en-curso`: 0
- En estado `hecha`: 0
- Por prioridad (MoSCoW): Must 16 (46%), Should 11 (31%), Could 8 (23%), Won't 0
- Por complejidad: S 26, M 9, L 0

## Tabla priorizada

| # | ID | Título | Épica | Prioridad | Complejidad | Estado | AC | Notas |
|---|----|--------|-------|-----------|-------------|--------|----|-------|
| 1 | HU-032 | Navegar entre Inicio, Historial y Etiquetas | EP-005 | Must | S | lista | 4 | infraestructura base, sin dependencias |
| 2 | HU-033 | Aplicar tema oscuro en toda la app | EP-005 | Must | S | lista | 3 | infraestructura base, sin dependencias |
| 3 | HU-001 | Registrar operación con campos identificadores mínimos | EP-001 | Must | M | lista | 5 | incluye Riesgo %/Resultado en R (post-revisión) |
| 4 | HU-002 | Completar operación con campos de catálogo obligatorios | EP-001 | Must | M | lista | 5 | depende de HU-001; incluye motivos en texto libre (post-revisión) |
| 5 | HU-003 | Agregar descripción de entrada a la operación | EP-001 | Must | S | lista | 3 | depende de HU-001/002 |
| 6 | HU-004 | Validar rangos numéricos y de fecha/hora al guardar | EP-001 | Must | S | lista | 4 | depende de HU-001/002 |
| 7 | HU-006 | Adjuntar imagen sin procesar (cámara o galería, máx. 2) | EP-001 | Must | M | lista | 3 | depende de HU-001; subida de Should a Must (cierra objetivo #5 del PRD) |
| 8 | HU-007 | Comprimir, redimensionar y generar miniatura cacheada al guardar | EP-001 | Must | M | lista | 3 | depende de HU-006; subida de Could a Must (única implementación del objetivo #5 del PRD) |
| 9 | HU-010 | Agregar elemento a un catálogo (Activos, Emociones o Errores) | EP-002 | Must | S | lista | 2 | |
| 10 | HU-015 | Ver historial agrupado por mes y día con tarjeta compacta | EP-003 | Must | M | lista | 3 | depende de HU-001 |
| 11 | HU-018 | Abrir detalle completo de una operación desde la tarjeta | EP-003 | Must | S | lista | 3 | depende de HU-015 |
| 12 | HU-020 | Editar una operación existente | EP-003 | Must | S | lista | 3 | depende de HU-018 y EP-001 (HU-001 a HU-004) |
| 13 | HU-021 | Eliminar una operación con opción de deshacer | EP-003 | Must | M | lista | 3 | depende de HU-018 |
| 14 | HU-026 | Ver resumen numérico y gráficas de R acumulado / distribución de resultados | EP-004 | Must | M | lista | 4 | depende de EP-001 (datos, incl. Resultado en R de HU-001) |
| 15 | HU-027 | Ver ranking de emociones y errores más frecuentes por periodo | EP-004 | Must | S | lista | 4 | depende de EP-001/EP-002 (datos) |
| 16 | HU-035 | Operar 100% offline sin pérdida de datos entre sesiones | EP-005 | Must | S | lista | 3 | verificación transversal sobre EP-001 a EP-004 |
| 17 | HU-005 | Prellenar fecha/hora y activo por defecto al abrir "Nueva operación" | EP-001 | Should | S | lista | 5 | depende de HU-001 |
| 18 | HU-008 | Crear operación desde el botón flotante "+" | EP-001 | Should | S | lista | 4 | depende de HU-001 y HU-032 (navegación) |
| 19 | HU-009 | Confirmar antes de salir del formulario con cambios sin guardar | EP-001 | Should | S | lista | 5 | depende de HU-001 |
| 20 | HU-011 | Editar y eliminar elemento de un catálogo (con protección de semillas) | EP-002 | Should | S | lista | 3 | depende de HU-010 |
| 21 | HU-013 | Agregar una nueva etiqueta sin salir del formulario de operación | EP-002 | Should | S | lista | 4 | reutiliza lógica de HU-010 |
| 22 | HU-016 | Garantizar scroll fluido con historiales grandes (3,000-5,000 operaciones) | EP-003 | Should | S | lista | 3 | depende de HU-015 |
| 23 | HU-017 | Ver resumen rápido por mes y colapsar/expandir el grupo | EP-003 | Should | S | lista | 4 | depende de HU-015 |
| 24 | HU-022 | Filtrar operaciones por etiqueta y por fecha/periodo (AND) | EP-003 | Should | M | lista | 4 | depende de HU-015 |
| 25 | HU-025 | Ver estado vacío en Historial sin operaciones registradas | EP-003 | Should | S | lista | 3 | depende de HU-015 y HU-008 |
| 26 | HU-028 | Filtrar métricas por periodo predefinido | EP-004 | Should | S | lista | 3 | depende de HU-026/027 |
| 27 | HU-031 | Ver estado vacío en Inicio sin operaciones en el periodo | EP-004 | Should | S | lista | 3 | depende de HU-025/026 |
| 28 | HU-012 | Confirmar eliminación de un elemento de catálogo en uso | EP-002 | Could | S | lista | 3 | depende de HU-011 y datos de HU-001 |
| 29 | HU-014 | Ver espacio en disco usado por imágenes | EP-002 | Could | S | lista | 3 | depende de datos de HU-006/007 |
| 30 | HU-019 | Ver imagen en pantalla completa con zoom y navegación entre imágenes | EP-003 | Could | M | lista | 4 | depende de HU-018 y HU-006/007 |
| 31 | HU-023 | Buscar por palabra clave, combinada con los filtros de etiqueta | EP-003 | Could | S | lista | 3 | depende de HU-022 |
| 32 | HU-024 | Persistir el último filtro de Historial entre sesiones | EP-003 | Could | S | lista | 3 | depende de HU-022/023 |
| 33 | HU-029 | Filtrar métricas por rango personalizado | EP-004 | Could | S | lista | 3 | depende de HU-028 |
| 34 | HU-030 | Persistir el filtro de periodo de Inicio entre sesiones | EP-004 | Could | S | lista | 3 | depende de HU-028/029 |
| 35 | HU-034 | Mantener orientación vertical en el resto de la app | EP-005 | Could | S | lista | 2 | depende de HU-019 (excepción); declarar orden HU-019 → HU-034 |

## Trazabilidad rápida (épica → historias)

- **EP-001 — Registro rápido de operaciones**: HU-001, HU-002, HU-003, HU-004, HU-005, HU-006, HU-007, HU-008, HU-009
- **EP-002 — Gestión de catálogos (Etiquetas)**: HU-010, HU-011, HU-012, HU-013, HU-014
- **EP-003 — Historial y estudio de operaciones**: HU-015, HU-016, HU-017, HU-018, HU-019, HU-020, HU-021, HU-022, HU-023, HU-024, HU-025
- **EP-004 — Métricas y detección de patrones (Inicio)**: HU-026, HU-027, HU-028, HU-029, HU-030, HU-031
- **EP-005 — Plataforma base: navegación, tema y almacenamiento offline**: HU-032, HU-033, HU-034, HU-035

## Items pendientes de definir

- Ninguno. Las 35 historias previstas en `docs/03-backlog/epicas.md` tienen archivo propio en `docs/04-historias/`, con AC y `estado: lista`.

## Notas de consistencia detectadas al construir este backlog

- **HU-014** tenía `estado: draft` en su frontmatter pese a tener el checklist INVEST completo (6/6) y AC completos — se corrigió a `lista` antes de generar la tabla original (inconsistencia de una sola historia, ya resuelta).
- Priorización aplicada con MoSCoW el 2026-07-27, reemplazando la asignación optimista original (89% Must → 40% Must). Ver razonamiento completo caso por caso en `docs/05-priorizacion/moscow-2026-07-27.md`.

## Cambios post-`/trycore:revisar` (2026-07-27, misma fecha)

Tras la auditoría global (`docs/.reviews/20260727-2138-global.md`), se aplicaron estas correcciones:

1. **HU-006/HU-007 subidas de Should/Could a Must** — la auditoría de trazabilidad detectó que HU-007 era la única implementación del objetivo #5 del PRD (imágenes ~200-400KB); dejarla en Could significaba no entregarlo en el MVP.
2. **HU-001 y HU-002 extendidas** con los 5 campos que 8 AC de otras historias (HU-004, HU-015, HU-017, HU-023, HU-026) ya asumían existentes sin que ninguna historia los creara: Riesgo (%) y Resultado en R (en HU-001); `emotionBeforeReason`, `emotionAfterReason`, `errorReason` (en HU-002). AC count de ambas subió de 3/4 a 5.
3. **Conteo de complejidad corregido**: era "S 24, M 11" (incorrecto), el conteo real de las 35 filas es **S 26, M 9**.
4. **Objetivo #1 del PRD (≤30s) reclasificado como meta aspiracional, no bloqueante** — al ser un producto de un solo usuario, no amerita una historia de verificación instrumentada dedicada; ver `docs/01-prd/bitacora-trading-forex.md` §12.
5. Referencia de ID corregida en HU-008 (dependía de "HU-028" por error de transcripción; la dependencia real es HU-032).
