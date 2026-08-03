## 1. Sub-slice EP-002-a: agregar/editar/eliminar con protección de semillas (HU-010, HU-011)

- [x] 1.1 `CatalogItemDao`: agregados `update(item)`, `delete(item)` y `countByTypeAndNameIgnoreCaseExcludingId(type, name, excludeId)` (unicidad + exclusión del propio id al editar, en un solo método en vez de `existsByType...`/`deleteById` separados)
- [x] 1.2 `CatalogRepository` nuevo (`core.data`): `addItem(type, name): CatalogAddResult` (sellado `Added`/`Duplicate` en vez de `Result<T>`, más explícito para la UI) — punto único reutilizado también por HU-013
- [x] 1.3 `CatalogRepository.deleteItem(item): CatalogDeleteResult` — bloquea "Ninguno" (ERROR) siempre y "XAUUSD" (ASSET) solo si `countByType(ASSET)<=1`; si no, elimina y dispara `CatalogSeeder.ensureSeeded` si el catálogo queda en 0
- [x] 1.4 `CatalogRepository.updateItem(item, newName): CatalogUpdateResult` — misma validación de unicidad que `addItem`, excluyendo el propio id
- [x] 1.5 `EtiquetasViewModel` nuevo: `StateFlow` por tipo de catálogo (`assets`/`emotions`/`errors`), acciones agregar/editar/eliminar, mapeo de errores a mensaje visible (`addError`/`editError`/`blockedDeleteMessage`)
- [x] 1.6 `EtiquetasScreen`: reemplazado el placeholder por `SecondaryTabRow` (Activos/Emociones/Errores) + lista única parametrizada por la pestaña activa (sin duplicar composable por tipo, ya que solo se renderiza una lista a la vez) + diálogo de edición + diálogo de bloqueo de eliminación
- [x] 1.7 Tests unitarios: `CatalogRepositoryTest` (12, unicidad case-insensitive/trim por `type`, no cruza tipos, protección de semillas ambos casos, recreación automática al vaciar) + `EtiquetasViewModelTest` (9, estado de UI). Se extrajo `testutil.FakeCatalogItemDao` compartido (regla de tres: ya se duplicaba en `OperationFormViewModelTest`) — se corrigió además un bug real del fake (no era reactivo: `getByType` creaba un `Flow` nuevo por llamada en vez de derivar de un `MutableStateFlow` único, rompiendo `stateIn(Eagerly)` en el ViewModel)
- [x] 1.8 Tests instrumentados: agregar un elemento y verlo disponible en el formulario de registro; editar; eliminar sin uso; bloqueo de eliminación de semilla con mensaje visible en pantalla — `EtiquetasCatalogManagementTest` (4 tests, dispositivo real Motorola Edge 50 Fusion, `connectedDebugAndroidTest` 38/38 PASSED). De paso se corrigió `AppNavigationTest.tocarEtiquetasMuestraLaPantallaDeEtiquetas`, que seguía asertando el placeholder de EP-005 ("Pantalla de Etiquetas (placeholder)") ya reemplazado por esta épica — quedó desincronizado desde 1.6 y lo expuso esta corrida real
- [x] 1.9 `journey_smoke` EP-002-a: abrir Etiquetas, agregar un elemento a Activos, verlo en el selector del formulario de registro — verificado con capturas reales (`adb exec-out screencap`) y taps por coordenadas reales (`adb shell uiautomator dump`) en el mismo dispositivo: se agregó "EURJPY-smoke" en Activos y apareció de inmediato en el selector "Activo" de "Nueva operación", junto a "XAUUSD"

## 2. Sub-slice EP-002-b: agregar etiqueta inline desde el formulario (HU-013)

- [x] 2.1 `OperationFormScreen`: agregar opción "+ Agregar nueva" en los 3 selectores de catálogo (Activo, Emoción antes/después, Error)
- [x] 2.2 `OperationFormViewModel`: estado del campo inline (abierto/cerrado, texto, error) + acción de confirmar que invoca `CatalogRepository.addItem` (mismo método de 1.2, sin reimplementar la validación)
- [x] 2.3 Al confirmar con éxito: seleccionar automáticamente el elemento creado en el campo correspondiente, sin tocar el resto del estado del formulario
- [x] 2.4 Al confirmar con nombre duplicado: mantener el campo inline abierto y con foco, mostrar "Este valor ya existe en el catálogo" junto al campo
- [x] 2.5 Tests unitarios: creación inline exitosa + selección automática, duplicado mantiene el campo abierto con el error, cancelar no crea nada, resto del formulario permanece intacto en los 3 casos (`OperationFormViewModelTest`, 7 tests nuevos, `testDebugUnitTest` fresco en verde sin regresiones)
- [ ] 2.6 Tests instrumentados: flujo completo tocando "+ Agregar nueva" sobre el `OperationFormScreen` real, verificando el nodo renderizado (no solo el StateFlow) — mismo estándar que `OperationFormRenderedUiTest` de EP-001. `OperationFormCatalogInlineAddTest` escrito y compila (`compileDebugAndroidTestKotlin` en verde), pero **sin ejecutar todavía**: no hay dispositivo/emulador conectado en esta sesión (`adb devices` vacío) — pendiente de correr `connectedDebugAndroidTest` en cuanto haya uno
- [ ] 2.7 `journey_smoke` EP-002-b: en "Nueva operación", crear un Activo nuevo inline y completar el resto del formulario sin salir — pendiente de dispositivo real, misma razón que 2.6

## 3. Sub-slice EP-002-c: confirmar eliminación de un elemento en uso (HU-012)

- [x] 3.1 `OperationDao`: agregar `countUsageOfCatalogItem(id: Long): Int` (`OR` sobre `assetId`/`emotionBeforeId`/`emotionAfterId`/`errorId`)
- [x] 3.2 (implementado distinto al texto original, alcance negociable per INVEST de HU-012) `CatalogRepository` gana `usageCountOf(item): Int` de solo lectura; `deleteItem` NO cambió su contrato (sigue eliminando directo salvo protección de semilla) porque el diálogo de confirmación de HU-011 ya es genérico para CUALQUIER eliminación (`pendingDeleteItem`, ya construido y testeado en EP-002-a) -- reintroducir un resultado `RequiresConfirmation` habría duplicado ese gate. `EtiquetasViewModel.onRequestDelete` consulta `usageCountOf` en paralelo a abrir el diálogo existente, sin bloquear ni retrasar su apertura.
- [x] 3.3 `EtiquetasViewModel`/`EtiquetasScreen`: el diálogo de confirmación ya existente (HU-011) ahora muestra el conteo real ("... está en uso en N operación(es) ...") cuando `pendingDeleteUsageCount > 0`, con Cancelar/Confirmar sin cambios
- [x] 3.4 Tests unitarios: `CatalogRepositoryTest` (conteo correcto contra las 4 columnas de `Operation`, 0 sin uso, eliminar en uso no toca la `Operation` que lo referencia) + `EtiquetasViewModelTest` (conteo disponible sin eliminar, cancelar limpia el conteo, confirmar elimina y conserva la operación) -- `testDebugUnitTest --rerun` fresco en verde, sin regresiones
- [ ] 3.5 Test instrumentado: sembrar una `Operation` real que use un `CatalogItem`, intentar eliminarlo, verificar el conteo mostrado en pantalla, confirmar y verificar que la `Operation` sembrada conserva su `id` de catálogo intacto en Room -- diferido a la pasada final de instrumentados (decisión del usuario 2026-08-02, sin dispositivo conectado en esta sesión)
- [ ] 3.6 `journey_smoke` EP-002-c: registrar una operación usando un Activo, ir a Etiquetas, intentar eliminar ese Activo y ver la advertencia con conteo 1 -- diferido, misma razón que 3.5

## 4. Sub-slice EP-002-d: espacio en disco usado por imágenes (HU-014)

- [x] 4.1 `DiskSpaceCalculator` nuevo (`core.image`, función pura): lista todas las rutas (`filePath` + su miniatura vía `ImageProcessor.thumbnailPathFor`) y suma `File(path).length()` de las que existan, ignorando silenciosamente las que no
- [x] 4.2 `EtiquetasViewModel`: gana `operationImageDao`/`filesDir`/`ioDispatcher` (inyectable para tests, default `Dispatchers.IO`), carga el total (bytes) una sola vez en `init` al entrar a la pestaña; `EtiquetasUiState.formatDiskSpaceKbMb` formatea a KB (sin decimales)/MB (1 decimal), `Locale.US` fijo
- [x] 4.3 `EtiquetasScreen`: muestra "Espacio usado por imágenes: ..." arriba de las pestañas (0 KB por defecto sin imágenes, sin error)
- [x] 4.4 Tests unitarios: `DiskSpaceCalculatorTest` (suma correcta sobre archivos reales de prueba vía `TemporaryFolder`, caso cero, archivo faltante no crashea) + `EtiquetasViewModelTest` (expone el total con imágenes reales, 0 sin ninguna, formatDiskSpaceKbMb) -- `testDebugUnitTest --rerun` fresco en verde, sin regresiones
- [ ] 4.5 Test instrumentado: guardar una operación con imagen real (reutilizando el pipeline de `ImageProcessor` de EP-001), abrir Etiquetas y verificar que el total mostrado es > 0 y coherente con el tamaño real del archivo en disco -- diferido a la pasada final de instrumentados (decisión del usuario 2026-08-02, sin dispositivo conectado en esta sesión)
- [x] 4.6 Test de recálculo tras eliminar (Escenario 3), a nivel JVM: `EtiquetasViewModelTest.'al reabrir Etiquetas tras eliminar una operacion con imagenes el espacio recalculado baja'` borra el archivo real + la fila de `OperationImage` y una segunda apertura (nueva instancia del ViewModel) refleja la baja -- falta la variante instrumentada real, diferida junto con 4.5
- [ ] 4.7 `journey_smoke` EP-002-d: registrar una operación con imagen, abrir Etiquetas, ver el espacio > 0 -- diferido, misma razón que 4.5

## 5. Cierre del change completo

- [ ] 5.1 Suite completa (`testDebugUnitTest` + `connectedDebugAndroidTest`) en verde con evidencia real de ejecución, sin regresiones sobre EP-001/EP-005
- [ ] 5.2 `wiring_checklist` completo sin ítems `failing` no justificados
- [ ] 5.3 `openspec validate gestion-catalogos-etiquetas --strict`
