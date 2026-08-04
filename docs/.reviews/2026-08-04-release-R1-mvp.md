# Release Gate — R1-mvp (2026-08-04)

Corrida completa del outer loop (`releasing-a-version`) sobre el diff acumulado `3d37773..main`
(EP-001 a EP-005, primera vez que EP-002/EP-003/EP-004 pasan por este gate — la corrida anterior,
2026-07-31, solo cubría EP-001+EP-005 y falló en `coherence` sin dejar rastro auditable del detalle,
ver hallazgo B8 más abajo).

## Veredicto por gate

| Gate | Resultado | Reviewer |
|---|---|---|
| `security` | ✅ PASA | security-reviewer |
| `smell` | ✅ PASA | simple-design-reviewer |
| `ux` | ❌ FALLA | ux-krug-reviewer |
| `coherence` | ❌ FALLA | coherence-three-way |
| `stack_arch` | ✅ PASA | stack-guardian |
| `integration` | ⏸️ Diferido (no se corrió — no cambia el status=failed ya determinado por ux/coherence) | — |

**Status de la release: `failed`.**

---

## `security` — PASA

Sin hallazgos CRÍTICO/ALTO. Room parametrizado en todas las queries, sin path traversal en imágenes
(nombres siempre `UUID.randomUUID()`), FileProvider `exported=false` acotado, sin permiso `INTERNET`,
sin secretos/keys hardcodeadas, sin logging que exponga datos sensibles, sin mecanismo de exportar/
compartir datos fuera del sandbox.

Nota no bloqueante: `app/build.gradle.kts` deja el build `release` sin R8/minify (bajo impacto, no hay
secretos server-side). Nota no bloqueante fuera de alcance de seguridad: `AppDatabase.kt` usa
`fallbackToDestructiveMigration(dropAllTables=true)` con un comentario propio "revisar antes del
primer release real" — esta es esa release; riesgo de pérdida de datos ante un futuro cambio de
esquema, no de seguridad.

## `smell` — PASA

Sin hallazgos bloqueantes. Código limpio para su tamaño (49 archivos, ~7.4k líneas), lógica de
dominio separada en objetos puros testeables, sin TODOs abandonados. Recomendaciones (no bloqueantes):
- Duplicación literal en `MainActivity.kt` entre las rutas "Nueva operación"/"Editar operación".
- `MainActivity.kt` (`MentaltraderApp`, ~315 líneas) acumula demasiadas responsabilidades.
- Mecánica de filtro persistido (DataStore) casi idéntica entre `Inicio` e `Historial` — extraer un
  `PersistedFilterRepository<T>` genérico.
- 3 implementaciones casi idénticas de `ExposedDropdownMenuBox` (Historial x2 + Registro).
- `HistorialFilterMatcher` (Kotlin puro) y `OperationDao.pagingSourceFiltered` (SQL) deben mantenerse
  sincronizados a mano, sin test de equivalencia automatizado.
- `OperationDao.kt` (241 líneas) mezcla CRUD de 4 épicas distintas.
- Código muerto: `HistorialScreen.kt` conserva un parámetro `viewModel: HistorialViewModel? = null`
  con rama de placeholder que ningún call-site real usa.

## `ux` — FALLA

**Bloqueante:** el resultado de la operación se muestra sin traducir (`WIN`/`LOSS`/`BREAK_EVEN`) en
3 lugares de alta visibilidad — `HistorialScreen.kt` (tarjeta del listado, dropdown de filtro) y
`HistorialDetalleScreen.kt` (detalle) — mientras el formulario de registro sí lo traduce a
"Ganada"/"Perdida"/"Break Even" (`OperationFormScreen.kt:238-246`, lógica inline no reutilizada).
Fix: extraer una función de mapeo única (ej. `ResultType.displayName()`) y reutilizarla en los 4
call-sites.

Recomendado (no bloqueante): el FAB puede tapar el último ítem del scroll en Historial/Inicio (falta
padding inferior extra); sin feedback de "guardando"/protección de doble-tap en "Guardar" (sí existe
en "Borrar todo", inconsistente); posible deriva semántica entre los selectores de periodo de
Historial ("última semana" = ventana rodante) e Inicio ("semana" = semana de calendario), mismo
patrón visual pero comportamiento distinto; doble narración de accesibilidad en la barra inferior
(ícono + label ya fusionados).

Varios bugs reales reportados por el usuario ya están corregidos y verificados en código (predictive-
back, botón "atrás" visible en Ajustes/Nueva operación, título del TopAppBar duplicando el tab activo,
banner de errores de validación fijo, teclado tapando campos, coma decimal, formato de "ratio
planeado", estados vacíos compartidos bien resueltos, confirmación de borrado de catálogo con conteo
de uso, snackbar de deshacer).

## `coherence` — FALLA

Auditoría completa de las 35 HU / 123 AC de las 5 épicas. Matriz resumen:

| Épica | HU | AC | AC→escenario | AC→test ejecutado | Change |
|---|---|---|---|---|---|
| EP-001 | 9 | 37 | 37/37 | 37/37 | archivado |
| EP-002 | 5 | 17 | 17/17 | 17/17 | sin archivar |
| EP-003 | 11 | 36 | 34/36 | 28/36 | sin archivar |
| EP-004 | 6 | 20 | 20/20 | 11/20 | sin archivar |
| EP-005 | 4 | 13 | 13/13 | 8/13 | archivado |
| **Total** | **35** | **123** | **121/123** | **101/123** | 3 de 5 sin archivar |

**Bloqueantes (orden de gravedad):**

- **B1** — HU-027 (ranking de emociones/errores, objetivo #3 del PRD) y HU-016 (scroll fluido
  3.000-5.000 operaciones, KPI #3 del PRD) sin un solo test ejecutado. El fake de test
  (`FakeOperationDao.kt`) lanza `UnsupportedOperationException` para toda query agregada/paginada de
  Room ("requiere Room real, no ejercitado contra este fake"), y **no existe ningún `androidTest` de
  EP-003 ni de EP-004** en todo el proyecto.
- **B2** — 22 de 123 AC sin test ejecutado; 11 de ellos usan "garantía estructural por lectura del
  código" como evidencia en `wiring_checklist`, lo que contradice la regla dura de `CLAUDE.md`
  ("la verificación es ejecutada, no por inspección").
- **B3** — Rotura de cadena AC→escenario en HU-017-AC3/AC4 (el Requirement del change solo tiene 2
  escenarios para 4 AC); los tests sí existen, falta agregar los 2 escenarios G/W/T al spec.
- **B4** — Ajustes → "Borrar todo" es una feature de cara al usuario (ícono permanente, diálogo de
  confirmación) sin HU/AC propios — el propio `proposal.md` de EP-001 fijó el criterio que dispara
  esta promoción ("si se considera feature de producto, debe tener su propia HU"). Decisión de
  producto pendiente, no del modelo.
- **B5** — 3 AC diferidos de EP-005 (HU-034-AC2, HU-035-AC2, HU-035-AC3) nunca retro-cerrados pese a
  que sus dependencias ya existen, con EP-005 archivada en `dod=true`.
- **B6** — La evidencia de "PASSED en dispositivo real" de HU-032-AC1/HU-001-AC1/HU-008-AC1/
  HU-009-AC1..5 (épicas EP-001/EP-005, ya archivadas) queda desmentida por el propio commit `49cb4dc`
  ("corrige 4 tests instrumentados NUNCA ANTES ejecutados en hardware real"). Requiere reconciliar
  la evidencia registrada.
- **B7** — 3 changes sin archivar (EP-002/003/004); falta el back-ref `openspec_change` en
  `epicas.md` para EP-003/EP-004 y en 22 de 35 HU (HU-010 a HU-031).
- **B8 (proceso)** — El fallo de `coherence` del 2026-07-31 no dejó rastro auditable (solo un booleano
  en `build-state.json`, sin detalle). Este mismo archivo es la corrección de ese hallazgo de proceso.

No bloqueantes: rama muerta en `HistorialScreen.kt` (placeholder), KDoc obsoleto en el mismo archivo,
un escenario de spec de EP-004 ("Codificación por color") sin AC ni test correspondiente.

Nota del propio reviewer: "la coherencia documental de este proyecto es inusualmente buena... lo que
falla es el tercer vértice del triángulo: la implementación verificada." EP-003 y EP-004 siguen en
`phase: green` con `dod: false` en su propio estado — no son épicas cerradas.

## `stack_arch` — PASA

Todas las dependencias declaradas matchean `stack-allowlist.json`; cero ocurrencias de
`retrofit`/`okhttp`/`hilt`/`dagger`/librerías de gráficos de terceros. Decisión de EP-004 (gráficas
con Compose Canvas puro) confirmada en código real. MVVM/StateFlow consistente en las 4 features.

Nota no bloqueante: `Operation` se accede directo vía `OperationDao` sin un `OperationRepository`
intermedio (a diferencia de `Catalog`, que sí pasa por `CatalogRepository`) — consistente en todo el
proyecto, no es una desviación puntual, pero vale documentarlo explícitamente como decisión asumida.

## `integration` — Diferido

No se corrió esta pasada: el status ya queda en `failed` por `ux`+`coherence`, y la única forma
disponible de correr integración con deps reales en esta sesión es el teléfono personal del usuario
(mismo riesgo ya documentado en el inner loop de EP-004). Se retoma cuando los bloqueantes de arriba
estén resueltos.

---

## Próximo paso

Corregir B1-B7 (B8 ya corregido con este mismo archivo) como un slice normal (probablemente el mayor
esfuerzo real del proyecto hasta la fecha: escribir la primera suite de `androidTest` con Room real
para EP-003/EP-004, y decidir el destino de "Borrar todo"), más el bloqueante de `ux` (traducción de
`ResultType`), y volver a correr este Release Gate.
