## 1. Sub-slice EP-002-a: agregar/editar/eliminar con protección de semillas (HU-010, HU-011)

- [ ] 1.1 `CatalogItemDao`: agregar `existsByTypeAndNameIgnoreCase(type, name): Boolean`, `countByType(type): Int`, `deleteById(id)`, `update(item)` (los que falten sobre lo ya existente de EP-001)
- [ ] 1.2 `CatalogRepository` nuevo (`core.data`): `addItem(type, name): Result<CatalogItem>` (valida unicidad antes de insertar) — punto único reutilizado también por HU-013
- [ ] 1.3 `CatalogRepository`: `deleteItem(item): Result<Unit>` — bloquea si es semilla protegida (`Ninguno`/ERROR siempre; `XAUUSD`/ASSET solo si `countByType(ASSET)==1`), si no, elimina y dispara recreación de semilla si `countByType(type)` queda en 0 tras el delete (reutiliza `CatalogSeeder`)
- [ ] 1.4 `CatalogRepository`: `updateItem(item, newName): Result<Unit>` — misma validación de unicidad que `addItem`
- [ ] 1.5 `EtiquetasViewModel` nuevo: `StateFlow` por tipo de catálogo (lista de `CatalogItem`), acciones agregar/editar/eliminar, mapeo de errores a mensaje visible
- [ ] 1.6 `EtiquetasScreen`: reemplazar el placeholder por `TabRow` (Activos/Emociones/Errores) + `CatalogListSection` compartido (lista + FAB/botón "Agregar" + diálogo de edición + confirmación simple de eliminar-sin-uso)
- [ ] 1.7 Tests unitarios: unicidad case-insensitive/trim por `type`, unicidad no cruza tipos, protección de semillas (ambos casos), recreación automática al vaciar
- [ ] 1.8 Tests instrumentados: agregar un elemento y verlo disponible en el formulario de registro; editar; eliminar sin uso; bloqueo de eliminación de semilla con mensaje visible en pantalla
- [ ] 1.9 `journey_smoke` EP-002-a: abrir Etiquetas, agregar un elemento a Activos, verlo en el selector del formulario de registro

## 2. Sub-slice EP-002-b: agregar etiqueta inline desde el formulario (HU-013)

- [ ] 2.1 `OperationFormScreen`: agregar opción "+ Agregar nueva" en los 3 selectores de catálogo (Activo, Emoción antes/después, Error)
- [ ] 2.2 `OperationFormViewModel`: estado del campo inline (abierto/cerrado, texto, error) + acción de confirmar que invoca `CatalogRepository.addItem` (mismo método de 1.2, sin reimplementar la validación)
- [ ] 2.3 Al confirmar con éxito: seleccionar automáticamente el elemento creado en el campo correspondiente, sin tocar el resto del estado del formulario
- [ ] 2.4 Al confirmar con nombre duplicado: mantener el campo inline abierto y con foco, mostrar "Este valor ya existe en el catálogo" junto al campo
- [ ] 2.5 Tests unitarios: creación inline exitosa + selección automática, duplicado mantiene el campo abierto con el error, cancelar no crea nada, resto del formulario permanece intacto en los 3 casos
- [ ] 2.6 Tests instrumentados: flujo completo tocando "+ Agregar nueva" sobre el `OperationFormScreen` real, verificando el nodo renderizado (no solo el StateFlow) — mismo estándar que `OperationFormRenderedUiTest` de EP-001
- [ ] 2.7 `journey_smoke` EP-002-b: en "Nueva operación", crear un Activo nuevo inline y completar el resto del formulario sin salir

## 3. Sub-slice EP-002-c: confirmar eliminación de un elemento en uso (HU-012)

- [ ] 3.1 `OperationDao`: agregar `countUsageOfCatalogItem(id: Long): Int` (`OR` sobre `assetId`/`emotionBeforeId`/`emotionAfterId`/`errorId`)
- [ ] 3.2 `CatalogRepository.deleteItem`: antes de eliminar, si `countUsageOfCatalogItem(id) > 0`, devolver un resultado que exige confirmación explícita con el conteo (no eliminar todavía)
- [ ] 3.3 `EtiquetasViewModel`/`EtiquetasScreen`: diálogo de advertencia con el conteo real, Cancelar/Confirmar
- [ ] 3.4 Tests unitarios: conteo correcto contra las 4 columnas de `Operation`, cancelar no modifica nada, confirmar elimina el `CatalogItem` sin tocar las filas de `Operation` que lo referencian
- [ ] 3.5 Test instrumentado: sembrar una `Operation` real que use un `CatalogItem`, intentar eliminarlo, verificar el conteo mostrado en pantalla, confirmar y verificar que la `Operation` sembrada conserva su `id` de catálogo intacto en Room
- [ ] 3.6 `journey_smoke` EP-002-c: registrar una operación usando un Activo, ir a Etiquetas, intentar eliminar ese Activo y ver la advertencia con conteo 1

## 4. Sub-slice EP-002-d: espacio en disco usado por imágenes (HU-014)

- [ ] 4.1 `OperationImageDao` o `ImageProcessor`: método de agregación que liste todas las rutas (`filePath` + su miniatura vía `thumbnailPathFor`) y sume `File(path).length()` de las que existan, ignorando silenciosamente las que no
- [ ] 4.2 `EtiquetasViewModel`: cargar el total (bytes) bajo `Dispatchers.IO` al entrar a la pestaña, formatear a KB/MB para presentación
- [ ] 4.3 `EtiquetasScreen`: mostrar el total (o "0" sin imágenes, sin error)
- [ ] 4.4 Tests unitarios: suma correcta sobre archivos reales de prueba, caso cero, archivo faltante no crashea (se cuenta como 0)
- [ ] 4.5 Test instrumentado: guardar una operación con imagen real (reutilizando el pipeline de `ImageProcessor` de EP-001), abrir Etiquetas y verificar que el total mostrado es > 0 y coherente con el tamaño real del archivo en disco
- [ ] 4.6 Test de recálculo tras eliminar (Escenario 3): sin UI de eliminar-operación todavía (HU-021/EP-003 no construida) — borrar directamente la fila de `Operation`/`OperationImage` y el archivo real en el test (mismo patrón que `DataResetServiceTest`), reabrir Etiquetas y verificar que el total bajó
- [ ] 4.7 `journey_smoke` EP-002-d: registrar una operación con imagen, abrir Etiquetas, ver el espacio > 0

## 5. Cierre del change completo

- [ ] 5.1 Suite completa (`testDebugUnitTest` + `connectedDebugAndroidTest`) en verde con evidencia real de ejecución, sin regresiones sobre EP-001/EP-005
- [ ] 5.2 `wiring_checklist` completo sin ítems `failing` no justificados
- [ ] 5.3 `openspec validate gestion-catalogos-etiquetas --strict`
