## 1. Sub-slice EP-001-a: registro completo de campos + Room real (HU-001, HU-002, HU-003)

- [ ] 1.1 Crear enums `Direction`, `ResultType`, `CatalogType` en `core.model`
- [ ] 1.2 Crear entidades Room `Operation` y `CatalogItem` en `core.data` (spec §3.1, 16 campos de `Operation`)
- [ ] 1.3 Crear `OperationDao` y `CatalogItemDao` (insert, getAll ordenado por fecha desc, getById)
- [ ] 1.4 Crear `AppDatabase` real (`@Database(entities = [Operation::class, CatalogItem::class], version = 1)`) con `RoomDatabase.Callback.onCreate` que siembra XAUUSD (ASSET), Ninguno (ERROR) y las 8 emociones semilla
- [ ] 1.5 Recrear `MentaltraderApplication` instanciando `AppDatabase` y exponiendo los DAOs (inyección manual, sin Hilt)
- [ ] 1.6 Crear `OperationFormViewModel` con `StateFlow<OperationFormState>` (data class con los 16 campos + errores + flag `isDirty`)
- [ ] 1.7 Construir la UI del formulario: Fecha+Hora (misma fila), Activo, Dirección, Calidad, Emoción antes+motivo, Emoción después+motivo, Error+motivo, Resultado, Descripción entrada (multilínea, expansible, al final)
- [ ] 1.8 Cablear "Guardar": validar obligatorios, persistir vía Repository/DAO, navegar de vuelta
- [ ] 1.9 Listado mínimo de verificación (placeholder de EP-003): mostrar las operaciones guardadas ordenadas por fecha, solo para poder verificar visualmente el guardado (no es la feature de Historial completa)
- [ ] 1.10 Tests unitarios: `OperationFormViewModel` (validación de obligatorios, mapeo de estado)
- [ ] 1.11 Tests instrumentados: guardar operación completa, guardar sin campo obligatorio, semilla XAUUSD, semilla Emociones, semilla Error, motivos en texto libre, descripción vacía/larga
- [ ] 1.12 `journey_smoke` de este sub-slice: crear una operación de punta a punta y verla en el listado mínimo

## 2. Sub-slice EP-001-b: validaciones de rango/fecha + valores por defecto (HU-004, HU-005)

- [ ] 2.1 Agregar campos opcionales `riskPercentage`/`resultInR` a la UI del formulario (ya en el modelo desde 1.2)
- [ ] 2.2 Validación de rango en el ViewModel: Calidad 0.0-10.0, Riesgo 0-100, fecha/hora no futura, R sin límite con sufijo "R"
- [ ] 2.3 Prellenado: Fecha/Hora actual al abrir, Activo=XAUUSD si el catálogo está vacío, editable manualmente
- [ ] 2.4 Tests unitarios: reglas de validación (límites, fecha futura)
- [ ] 2.5 Tests instrumentados: guardar dentro de rango, fecha futura bloqueada, Calidad/Riesgo fuera de rango, R negativo grande aceptado, prellenado correcto, edición manual conservada
- [ ] 2.6 `journey_smoke`: abrir formulario ya prellenado, intentar guardar con un valor fuera de rango, corregir y guardar

## 3. Sub-slice EP-001-c: adjuntar y procesar imágenes (HU-006, HU-007)

- [ ] 3.1 Crear entidad Room `OperationImage` (spec §3.1) y su DAO
- [ ] 3.2 UI: botón "Adjuntar imagen" (cámara/galería vía `ActivityResultContracts`), límite de 2, miniatura sin procesar
- [ ] 3.3 Solicitud de permiso de Cámara/Almacenamiento solo al adjuntar, con degradación graceful si se rechaza
- [ ] 3.4 Procesamiento en `Dispatchers.Default`: redimensionar (~1600-2000px ancho) + comprimir JPEG ~85% + guardar en `filesDir` + generar miniatura cacheada una sola vez
- [ ] 3.5 Manejo de imagen corrupta/formato no soportado (no adjuntar, informar)
- [ ] 3.6 Carga de miniaturas en UI vía Coil
- [ ] 3.7 Tests unitarios: cálculo de redimensionado por relación de aspecto
- [ ] 3.8 Tests instrumentados: adjuntar desde galería, límite de 2, permiso rechazado, miniatura no se recalcula en cada render
- [ ] 3.9 `journey_smoke`: adjuntar una imagen real, guardar la operación, verla en el listado mínimo con su miniatura

## 4. Sub-slice EP-001-d: entrada rápida (FAB) y salvaguarda de salida (HU-008, HU-009)

- [ ] 4.1 Agregar FAB al `Scaffold` raíz de `MainActivity`, visible solo en Inicio/Historial, navega al formulario
- [ ] 4.2 `BackHandler` + intercept de navegación: comparar estado actual del formulario contra su estado inicial (`isDirty`)
- [ ] 4.3 Diálogo de confirmación (`AlertDialog`) al detectar salida con cambios pendientes; cancelar/confirmar
- [ ] 4.4 Tests instrumentados: FAB en Inicio/Historial, ausencia en Etiquetas, FAB no tapa contenido en scroll, diálogo al navegar/atrás con cambios, cancelar conserva datos, confirmar descarta, sin cambios sale sin fricción
- [ ] 4.5 `journey_smoke` final de todo EP-001: FAB → formulario completo (con imagen) → intentar salir sin guardar → cancelar → guardar → ver en listado

## 5. Cierre del change completo

- [ ] 5.1 Ejecutar el journey-smoke integral de las 4 sub-slices en un solo recorrido
- [ ] 5.2 Revisar `wiring_checklist` completo (44 items) sin ítems `failing` no justificados
- [ ] 5.3 `openspec validate registro-rapido-operaciones --strict`
