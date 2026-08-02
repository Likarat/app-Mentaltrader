## Context

`CatalogItem` (entidad Room, `type: CatalogType` = `ASSET`/`EMOTION`/`ERROR`) y su siembra inicial ya existen desde EP-001 (`CatalogSeeder`). La pestaña "Etiquetas" es hoy un placeholder de EP-005 (`feature/etiquetas/EtiquetasScreen.kt`, solo texto). `Operation` referencia catálogos por `Long` plano (`assetId`, `emotionBeforeId`, `emotionAfterId`, `errorId`) **sin `@ForeignKey` declarado** — decisión ya tomada en EP-001, no se revisita aquí. Esta épica construye la administración real de esos catálogos y una vista de solo lectura del espacio en disco de imágenes.

## Goals / Non-Goals

**Goals:**
- CRUD de `CatalogItem` (agregar/editar/eliminar) con unicidad case-insensitive/trim **por `type`**, compartido por los tres catálogos vía una única pantalla parametrizada.
- Protección de semillas (`Ninguno`/ERROR siempre; `XAUUSD`/ASSET solo si es el único) + recreación automática si el catálogo queda vacío.
- Confirmación con conteo real de uso antes de eliminar un elemento referenciado por alguna `Operation`.
- Reutilizar la misma lógica de creación/unicidad desde el formulario de registro (HU-013), sin duplicarla.
- Cálculo de solo lectura del espacio en disco usado por `OperationImage` (archivo procesado + miniatura), recalculado al abrir la pestaña.

**Non-Goals:**
- No se agrega ningún `@ForeignKey`/cascada a `Operation` — el "valor histórico conservado" de HU-012 ya está garantizado por su ausencia (eliminar un `CatalogItem` no toca filas de `Operation`).
- No se resuelve todavía cómo el Historial (EP-003, aún placeholder) muestra el *nombre* de un `CatalogItem` ya eliminado en una operación antigua — eso es responsabilidad de EP-003 cuando construya la lectura real del historial; esta épica solo garantiza que el dato crudo (`id`) sobrevive intacto.
- HU-014-Escenario 3 (recálculo tras eliminar una operación con imágenes) no se ejercita vía UI real de eliminar-operación — esa UI es HU-021 (EP-003, no construida). Se prueba manipulando directamente Room/archivos en el test, mismo patrón ya usado en `DataResetServiceTest`.

## Decisions

1. **Una sola pantalla `EtiquetasScreen` parametrizada por `CatalogType`, no 3 pantallas.** `TabRow` con 3 pestañas (Activos/Emociones/Errores), cada una renderizando el mismo composable `CatalogListSection(type, items, onAdd, onEdit, onDelete)`. Evita triplicar UI y lógica de estado; consistente con cómo `CatalogItem` ya es una sola entidad parametrizada por `type`.
   - *Alternativa descartada*: 3 pantallas/ViewModels independientes — más código, misma lógica de negocio triplicada, sin beneficio real (las 3 comparten el 100% de las reglas).

2. **Lógica de unicidad/creación centralizada en un método compartido, invocado desde dos puntos de entrada.** `CatalogItemDao` expone `existsByTypeAndNameIgnoreCase(type, name): Boolean` (o `findByTypeAndNameIgnoreCase`); un único método de "crear con validación" (en `EtiquetasViewModel` para HU-010, reutilizado tal cual — misma función, no reimplementada — desde `OperationFormViewModel` para HU-013 inline). Concretamente: se extrae una función pura/DAO-backed `CatalogRepository.addItem(type, name): Result<CatalogItem>` (nuevo, pequeño; vive en `core.data` junto a los DAOs, sin capa de repositorio general de por medio — evita sobre-ingeniería) que ambos ViewModels llaman.
   - *Alternativa descartada*: duplicar la validación en `OperationFormViewModel` — es exactamente el anti-patrón que HU-013 dice explícitamente que no debe pasar ("no redefine la regla de unicidad, solo la reutiliza").

3. **Protección/recreación de semillas vía una función determinista, no vía trigger de base de datos.** Al eliminar, tras el `DELETE`, se consulta `COUNT(*) WHERE type = :type`; si es `0`, se re-inserta la semilla correspondiente (reutilizando `CatalogSeeder`, que ya sabe qué semilla corresponde a cada `type`). La regla "XAUUSD eliminable solo si es el único" se evalúa **antes** de eliminar: si `type == ASSET && name == "XAUUSD" && count(ASSET) == 1`, se bloquea con mensaje explícito, sin llegar a la recreación (la recreación cubre el caso en que se borra *otro* elemento de Activos, no XAUUSD, y XAUUSD ya no está — o el caso simétrico de vaciar completamente sin pasar por el guard porque el usuario borró en otro orden).
   - *Alternativa descartada*: `Room.Callback`/trigger SQL — ya se determinó en EP-001 (memoria del entorno) que la siembra vía `RoomDatabase.Callback` no es confiable tras migraciones; se mantiene el patrón ya probado de `CatalogSeeder.ensureSeeded()` invocado explícitamente desde código Kotlin.

4. **Conteo de uso: un solo método de agregación en `OperationDao`, sin importar el `type` del `CatalogItem`.** `countUsageOfCatalogItem(id: Long): Int` → `SELECT COUNT(*) FROM operation WHERE assetId = :id OR emotionBeforeId = :id OR emotionAfterId = :id OR errorId = :id`. Como el `id` de `CatalogItem` es una PK autogenerada única en toda la tabla (compartida entre los 3 `type`), un `id` de un `CatalogItem` de tipo `EMOTION` nunca puede coincidir con un `assetId` real (esa columna solo recibe ids que en algún momento fueron guardados como `type=ASSET`) — la consulta amplia es segura y evita 3 métodos casi idénticos.

5. **Espacio en disco: suma real de `File(path).length()` sobre las rutas de `OperationImage` (procesada + miniatura), no una estimación por conteo.** "Aproximado" (spec) se refiere a la *presentación* (redondeo a KB/MB), no al cálculo: se suma el tamaño real en bytes de los archivos que existen hoy en `filesDir` (si un archivo ya no existe — borrado externamente — se ignora sin error, tratando esa entrada como 0 en vez de crashear). Cálculo bajo `Dispatchers.IO`, solo al abrir la pestaña Etiquetas (no observamos cambios en tiempo real desde otras pantallas — coincide con la nota técnica de la HU).
   - *Alternativa descartada*: estimar por conteo × tamaño promedio — menos preciso sin ganar nada en simplicidad real (el cálculo real es una suma trivial sobre archivos ya en disco).

6. **Sub-slices**: se construyen en el orden ya decidido en el DoR — `EP-002-a` (HU-010+HU-011, la base CRUD) → `EP-002-b` (HU-013, reutiliza `CatalogRepository.addItem` de `a`) → `EP-002-c` (HU-012, extiende el delete de `a` con el guard de conteo) → `EP-002-d` (HU-014, independiente, sin relación con el CRUD).

## Risks / Trade-offs

- **[Riesgo] La consulta de conteo de uso (`OperationDao.countUsageOfCatalogItem`) no distingue si el `id` corresponde al `type` que se está intentando borrar.** → Mitigación: dado que `CatalogItem.id` es una PK autogenerada única global, no hay colisión real posible entre tipos; documentado en la Decisión 4, sin necesidad de filtrar por `type` en la query.
- **[Riesgo] Recrear la semilla automáticamente tras vaciar un catálogo puede sorprender al usuario** (borró todo y "reapareció" un elemento). → Mitigación: ya es el comportamiento explícitamente pedido por HU-011-Escenario 4; se hace visible de inmediato en el propio listado (sin demora ni background job), no es un efecto oculto.
- **[Riesgo] El cálculo de espacio en disco recorre el filesystem de forma síncrona al abrir la pestaña.** → Mitigación: bajo `Dispatchers.IO`, y el volumen esperado (imágenes de un solo usuario, app personal) es bajo; si se detectara lentitud real en verificación, se revisita (no se optimiza especulativamente ahora).

## Open Questions

Ninguna abierta: las decisiones técnicas de las 5 HU ya estaban acotadas por sus propias "Notas técnicas"; este diseño solo las conecta.
