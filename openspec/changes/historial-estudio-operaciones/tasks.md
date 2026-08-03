## 1. Sub-slice EP-003-a: listado agrupado + fluidez a escala (HU-015, HU-016)

- [x] 1.1 `OperationDao`: agregar query paginada `pagingSourceOrderedByDateDesc(): PagingSource<Int, Operation>` (sin filtros todavía, esos llegan en EP-003-e)
- [x] 1.2 Dependencia `androidx.room:room-paging` + `androidx.paging:paging-compose` + `androidx.paging:paging-runtime` (versiones reales verificadas contra `dl.google.com` maven-metadata: 2.8.4/3.5.0/3.5.0) agregadas a `libs.versions.toml`/`build.gradle.kts`. Bloqueado momentáneamente por un SSL handshake al resolver las dependencias nuevas (el JDK 21 auto-provisto de Gradle no confía en el certificado del proxy/AV de esta máquina, mismo patrón que el fix ya documentado de `git http.sslbackend=schannel`); resuelto con `systemProp.javax.net.ssl.trustStoreType=Windows-ROOT` en `~/.gradle/gradle.properties` (config de máquina, no del repo)
- [x] 1.3 `HistorialListItem` (sealed interface: `GroupHeader(month: YearMonth?, day: LocalDate)` + `OperationRow`) + `HistorialSeparators.between(before, after)`, función PURA (sin Room/Paging) que decide el separador -- evita depender de `androidx.paging:paging-testing` (no está en `stack-allowlist.json`) para poder testear la lógica de agrupación en JVM
- [x] 1.4 `HistorialViewModel` nuevo: `Pager(PagingConfig(pageSize = 30)) { operationDao.pagingSourceOrderedByDateDesc() }.flow.insertSeparators{...}.map{...}.cachedIn(viewModelScope)` → `Flow<PagingData<HistorialListItem>>`. Gana también `catalogItemDao` (para resolver el nombre del Activo en la tarjeta) e `imagesOf(operationId)`/`assetNameOf(assetId)`
- [x] 1.5 `HistorialScreen`: reemplazada la `LazyColumn` de verificación por `LazyPagingItems` (`collectAsLazyPagingItems`) renderizando `GroupHeader`/`OperationRow`, con la tarjeta compacta (miniatura o ícono genérico, Activo+Hora arriba, Resultado+R coloreado abajo)
- [x] 1.6 Ícono genérico (`Icons.Filled.Image`) para operaciones sin imagen, sin espacio vacío
- [x] 1.7 Tests unitarios: `HistorialSeparatorsTest` (5 tests: primera operación trae mes+día, mismo día sin separador, cambio de día mismo mes, cambio de mes, sin siguiente) -- `testDebugUnitTest` en verde. La integración completa Pager+insertSeparators+Room se deja para el instrumentado de 1.8 (requiere Room real; `PagingData`/`insertSeparators` en JVM puro sin `paging-testing` no es representativo)
- [ ] 1.8 Test instrumentado con dataset sintético grande (3,000-5,000 operaciones sembradas vía `operationDao.insert` en loop, sin UI) verificando que `pagingSourceOrderedByDateDesc` no carga todo a memoria (tamaño de página real) y que el scroll llega hasta el final sin excepciones -- diferido a la pasada final de instrumentados (sin dispositivo conectado en esta sesión)
- [ ] 1.9 `journey_smoke` EP-003-a: abrir Historial con operaciones reales de distintos meses, ver el agrupamiento y la tarjeta compacta reales -- diferido a la pasada final de instrumentados, misma razón que 1.8

## 2. Sub-slice EP-003-b: detalle completo de una operación (HU-018)

- [x] 2.1 `HistorialDetalleViewModel` (`operationDao.getById(id)` + `operationImageDao.getByOperationId(id)` + nombres de catálogo resueltos vía `CatalogItemDao.getById`) + `HistorialDetalleScreen` (Composable real)
- [x] 2.2 Secciones ordenadas igual que el formulario de registro: imagen (si existe) primero, luego "Datos generales"/"Emociones y errores"/"Resultado", "Descripción entrada" al final
- [x] 2.3 Se omite la sección de imagen sin dejar espacio vacío si `state.images` está vacío (`if (state.images.isNotEmpty())`)
- [x] 2.4 Navegación real: `HistorialScreen` gana `onOperationClick`, `OperationCard` es `clickable`, `MainActivity.kt` agrega la ruta parametrizada `RUTA_DETALLE_OPERACION` (`detalle_operacion/{operationId}`, `NavType.LongType`) y navega ahí con el id real de la operación tocada
- [x] 2.5 Tests unitarios: `HistorialDetalleViewModelTest` (4 tests: carga operación+catálogo resuelto, carga imágenes, sin imágenes queda vacío, operación inexistente no crashea) -- `testDebugUnitTest` en verde
- [ ] 2.6 Test instrumentado: tocar una tarjeta real del listado abre el detalle real con los datos de esa operación (Room real, sin fakes) -- diferido a la pasada final de instrumentados (sin dispositivo conectado en esta sesión)
- [ ] 2.7 `journey_smoke` EP-003-b: desde Historial, tocar una operación real y ver su detalle completo -- diferido, misma razón que 2.6

## 3. Sub-slice EP-003-c: editar + eliminar con deshacer (HU-020, HU-021)

- [x] 3.1 `OperationDao.update(operation: Operation)` (Room `@Update`)
- [x] 3.2 `OperationDao`: `deleteById(id: Long)` + `OperationImageDao.deleteByOperationId(operationId: Long)` + reutiliza `OperationImageDao.getByOperationId`/borrado de archivos (mismo patrón de limpieza que `DataResetService`) para el borrado físico definitivo
- [x] 3.3 `OperationFormViewModel`: parámetro opcional `editingOperationId: Long? = null`; si no es null, `init` carga la operación existente vía `getById` real y prellena el estado con `stateFrom(existing)` (en vez del prellenado de HU-005); `save()` llama `update(...)` en vez de `insert(...)` en modo edición, preservando `createdAt` original
- [x] 3.4 Botón "Editar" en `HistorialDetalleScreen` navega a la ruta parametrizada real `RUTA_EDITAR_OPERACION` (`editar_operacion/{operationId}`, `MainActivity.kt`) que monta `OperationFormScreen`/`OperationFormViewModel` con `editingOperationId` seteado (mismo guard de "salir sin guardar" de HU-009 ya extendido a esta ruta)
- [x] 3.5 `HistorialViewModel`: `pendingDeleteIds: StateFlow<Set<Long>>` + `onRequestDelete(id)` (agrega el id, lanza `Job` con `delay(UNDO_WINDOW_MS)` que ejecuta el borrado físico real -- fila + imágenes + archivos -- si no se canceló) + `onUndoDelete(id)` (cancela el `Job`, quita el id) + `deleteRequested: SharedFlow<Long>` (evento para el snackbar) — el `PagingData` se filtra excluyendo `pendingDeleteIds` (design.md decisión #4); `HistorialScreen` además oculta la fila de inmediato a nivel de UI como red de seguridad optimista mientras no haya verificación instrumentada real de la reactividad del filtro de Paging
- [x] 3.6 Snackbar "Deshacer" real: `HistorialViewModel` hoisteado a nivel de `MentaltraderApp` (sobrevive navegar al detalle y volver); botón "Eliminar" en `HistorialDetalleScreen` pide confirmación (`AlertDialog`), dispara `onRequestDelete` + vuelve al listado, donde `MainActivity` muestra el `Snackbar` real con acción "Deshacer" (`SnackbarDuration.Short`, por debajo de `UNDO_WINDOW_MS` a propósito)
- [x] 3.7 Tests unitarios: `OperationFormViewModelTest` (3 tests nuevos: prellenado real en modo edición, `update` en vez de `insert` preservando `createdAt`, validación de HU-004 aplica igual al editar) + `HistorialViewModelTest` nuevo (4 tests: `pendingDeleteIds` se agrega de inmediato sin tocar Room, deshacer dentro del tiempo cancela el borrado y la operación sigue intacta, expirar la ventana borra definitivamente fila+imágenes+archivos, pedir eliminar el mismo id dos veces no duplica el borrado) -- `StandardTestDispatcher` pasado explícitamente a `runTest` para controlar el tiempo virtual de `delay(UNDO_WINDOW_MS)`. `testDebugUnitTest` (48 tests en toda la suite) + `assembleDebug` + `compileDebugAndroidTestKotlin`: BUILD SUCCESSFUL, sin regresiones
- [ ] 3.8 Test instrumentado: editar una operación real desde el detalle y ver el cambio reflejado en el listado + detalle; eliminar una real, deshacer y verificar que sigue intacta; eliminar una real, esperar el timeout y verificar que la fila y sus archivos de imagen ya no existen -- diferido a la pasada final de instrumentados (sin dispositivo conectado en esta sesión, mismo criterio que EP-003-a/b)
- [ ] 3.9 `journey_smoke` EP-003-c: editar una operación real y ver el cambio reflejado; eliminar una y deshacer justo a tiempo -- diferido, misma razón que 3.8

## 4. Sub-slice EP-003-d: resumen mensual + colapsar/expandir (HU-017)

- [ ] 4.1 `OperationDao`: query de agregación por mes (`GROUP BY strftime('%Y-%m', ...)`: conteo, % ganadas, R acumulado) — `Flow<List<MonthSummary>>`, sin paginar
- [ ] 4.2 `HistorialViewModel`: expone el resumen mensual + `collapsedMonths: Set<YearMonth>` (estado de UI puro) con `onToggleMonth(month)`
- [ ] 4.3 `HistorialScreen`: encabezado de mes muestra el resumen real y colapsa/expande sus operaciones al tocarlo, sin afectar otros meses
- [ ] 4.4 Tests unitarios: cálculo de agregación correcto (conteo, % ganadas, R acumulado) contra datos de prueba; colapsar un mes no afecta el estado de otro
- [ ] 4.5 Test instrumentado: tocar el encabezado de un mes real colapsa/expande sus operaciones en pantalla
- [ ] 4.6 `journey_smoke` EP-003-d: ver el resumen real de un mes y colapsar/expandir su grupo

## 5. Sub-slice EP-003-e: filtros por etiqueta/fecha + búsqueda (HU-022, HU-023)

- [ ] 5.1 `OperationDao`: extender la query paginada de 1.1 con parámetros opcionales nulos (`(:assetId IS NULL OR assetId = :assetId) AND ...`) para Activo/Resultado/Error/Emoción antes/Emoción después/rango de fecha/texto libre (`LIKE` sobre `entryDescription`/`emotionBeforeReason`/`emotionAfterReason`/`errorReason`)
- [ ] 5.2 `HistorialFilterState` (Activo/Resultado/Error/EmocionAntes/EmocionDespues como ids nulos, periodo predefinido o rango personalizado, texto de búsqueda) + `HistorialViewModel` reconstruye el `Pager` cuando cambia el filtro
- [ ] 5.3 UI de filtros: selectores por etiqueta (reutilizando `CatalogItemDao.getByType` de EP-002 para las opciones), selector de fecha/periodo predefinido + rango personalizado, campo de búsqueda de texto, botón "Limpiar filtros"
- [ ] 5.4 Combinación AND real entre todos los filtros + la búsqueda de texto (ver Escenario 2 de HU-023: Activo="XAUUSD" + texto="ruptura")
- [ ] 5.5 Tests unitarios: cada filtro individual, combinación AND de 2+ filtros, búsqueda de texto sola y combinada con un filtro de etiqueta, limpiar filtros vuelve al listado completo, búsqueda sin coincidencias no rompe (lista vacía sin excepción)
- [ ] 5.6 Test instrumentado con dataset real: aplicar un filtro de Activo real y verificar que el listado renderizado solo muestra esas operaciones; combinar con búsqueda de texto real
- [ ] 5.7 `journey_smoke` EP-003-e: filtrar por Activo real, agregar un texto de búsqueda, ver el resultado combinado, limpiar filtros

## 6. Sub-slice EP-003-f: estados vacíos diferenciados (HU-025)

- [ ] 6.1 `HistorialViewModel`/`HistorialScreen`: distinguir "sin ninguna operación registrada nunca" (query de conteo total = 0) de "hay operaciones pero el filtro activo no arroja resultados" (conteo total > 0, `PagingData` vacío)
- [ ] 6.2 Mensaje de "primera vez" con acceso directo al mismo destino de HU-008 (FAB "Nueva operación")
- [ ] 6.3 Mensaje de "sin resultados para el filtro" sin ese acceso directo
- [ ] 6.4 Tests unitarios: los dos estados se distinguen correctamente según el escenario (0 operaciones totales vs. filtro sin coincidencias)
- [ ] 6.5 Test instrumentado: verificar en pantalla real ambos mensajes (BD vacía real vs. filtro real sin coincidencias) y que el acceso directo del primero navega al formulario real
- [ ] 6.6 `journey_smoke` EP-003-f: ver el estado vacío inicial real (BD limpia) y navegar desde su acceso directo

## 7. Sub-slice EP-003-g: visor de imagen a pantalla completa (HU-019)

- [ ] 7.1 `HistorialVisorImagenScreen` nuevo (ruta con `operationId` + índice de imagen inicial), `HorizontalPager` sobre las imágenes reales de esa operación (`operationImageDao.getByOperationId`)
- [ ] 7.2 Zoom vía `Modifier.pointerInput { detectTransformGestures { ... } }` (sin dependencia nueva) por imagen
- [ ] 7.3 Indicador de puntos (página actual) + flecha para avanzar, además del gesto de deslizar
- [ ] 7.4 Excepción de orientación: `DisposableEffect` que fija `activity.requestedOrientation = SCREEN_ORIENTATION_SENSOR` al entrar y `SCREEN_ORIENTATION_PORTRAIT` al salir (mismo patrón de acceso directo a `ComponentActivity` que `HighPriorityBackHandler`)
- [ ] 7.5 Navegación real: tocar la imagen en `HistorialDetalleScreen` (EP-003-b) abre este visor con esa operación/imagen
- [ ] 7.6 Tests unitarios: cálculo de matriz/escala de zoom (función pura, sin Compose real) si se extrae como tal; de lo contrario, cubrir solo con instrumentado (zoom depende de gestos reales)
- [ ] 7.7 Test instrumentado: abrir el visor real desde el detalle, verificar fondo oscuro + imagen a pantalla completa, navegar entre 2 imágenes reales de la misma operación, y verificar que rotar el `Activity` (vía `requestedOrientation` real) es permitido solo en esta pantalla
- [ ] 7.8 `journey_smoke` EP-003-g: abrir una imagen real a pantalla completa, hacer zoom, navegar a la segunda imagen si existe
- [ ] 7.9 Cierre retroactivo de `INT-hu034-retro-cierre` en `history[]` de EP-005 (HU-034-AC2, diferido esperando esta historia) — verificar en el instrumentado de 7.7 que, al salir del visor, la orientación vuelve a vertical

## 8. Sub-slice EP-003-h: persistir el último filtro entre sesiones (HU-024)

- [ ] 8.1 `HistorialFilterRepository` nuevo (`core/data`, DataStore Preferences) con las claves ya fijadas por la spec: `lastPeriodFilter`, `lastCustomRangeStart`/`lastCustomRangeEnd`, `lastSearchText`, `lastSelectedAssets`, `lastSelectedResults`, `lastSelectedErrors`, `lastSelectedEmotionBefore`, `lastSelectedEmotionAfter` (listas como CSV de ids, sin dependencia de serialización nueva)
- [ ] 8.2 `HistorialViewModel`: al abrir, restaura `HistorialFilterState` desde `HistorialFilterRepository`; al cambiar el filtro, persiste el nuevo estado
- [ ] 8.3 Tests unitarios: `HistorialFilterRepositoryTest` con DataStore real de prueba (`PreferenceDataStoreFactory.create` sobre un archivo temporal, mismo patrón que `TemporaryFolder` ya usado en `DiskSpaceCalculatorTest`) — guardar y leer cada clave, estado por defecto sin filtros previos
- [ ] 8.4 Test instrumentado: aplicar un filtro real, forzar el cierre completo del proceso (no solo background) y reabrir, verificar que el filtro sigue restaurado desde disco
- [ ] 8.5 `journey_smoke` EP-003-h: aplicar un filtro real, reabrir la app, confirmar que sigue activo

## 9. Cierre del change completo

- [ ] 9.1 Suite completa (`testDebugUnitTest` + `connectedDebugAndroidTest`) en verde con evidencia real de ejecución, sin regresiones sobre EP-001/EP-002/EP-005
- [ ] 9.2 `wiring_checklist` completo sin ítems `failing` no justificados
- [ ] 9.3 `openspec validate historial-estudio-operaciones --strict`
- [ ] 9.4 Back-reference: agregar `openspec_change: historial-estudio-operaciones` en `docs/03-backlog/epicas.md#ep-003` y en el frontmatter/nota de cada una de las 11 HU
