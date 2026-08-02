## Why

El registro de operaciones (EP-001) depende de tres catálogos (Activos, Emociones, Errores) que hoy solo existen sembrados por migración, sin ninguna forma de que el usuario los mantenga. Sin poder agregar/editar/eliminar elementos, los catálogos no reflejan la forma real de operar del usuario y el ranking de emociones/errores (EP-004, objetivo #3 del PRD) queda expuesto a datos duplicados o mal etiquetados. Esta épica cierra esa administración (pestaña "Etiquetas") y agrega visibilidad del espacio en disco usado por imágenes (objetivo #5).

## What Changes

- Agregar un elemento nuevo a un catálogo (Activos, Emociones o Errores) desde la pestaña Etiquetas, con validación de unicidad case-insensitive/trim **por tipo de catálogo** (HU-010).
- Editar el nombre de un elemento existente y eliminarlo, con protección de los elementos semilla (`Ninguno` en Errores, `XAUUSD` en Activos si es el único) y recreación automática de la semilla si el catálogo queda vacío (HU-011).
- Al eliminar un elemento **en uso** por operaciones existentes, mostrar una advertencia con el conteo de operaciones afectadas antes de confirmar; el valor histórico de esas operaciones se conserva sin cambios (HU-012).
- Permitir crear un elemento de catálogo nuevo **sin salir del formulario de registro** ("+ Agregar nueva"), reutilizando la misma validación de unicidad de HU-010, con selección automática del elemento creado y sin perder el resto de los datos ya ingresados (HU-013).
- Mostrar en la pestaña Etiquetas el espacio en disco total aproximado usado por las imágenes adjuntas a las operaciones (HU-014).

## Capabilities

### New Capabilities
- `catalog-management`: CRUD de elementos de `CatalogItem` (agregar/editar/eliminar) con unicidad por tipo de catálogo, protección/recreación de semillas, y confirmación de eliminación cuando el elemento está en uso (con conteo real de operaciones afectadas, preservando el valor histórico).
- `image-storage-usage`: cálculo y presentación de solo lectura del espacio en disco aproximado usado por las imágenes adjuntas (`OperationImage`), recalculado cada vez que se abre la pestaña Etiquetas.

### Modified Capabilities
- `operation-entry-form`: se agrega la posibilidad de crear un nuevo elemento de catálogo inline desde el propio formulario ("+ Agregar nueva" en los selectores de Activo/Emoción/Error), sin abandonar el formulario ni perder los datos ya ingresados.

## Impact

- **Código nuevo**: pantalla real de Etiquetas (`feature/etiquetas/`, hoy placeholder de EP-005) con su ViewModel; ampliación de `CatalogItemDao` (unicidad por `type`, conteo de uso por `CatalogItem.id` vía `Operation`, recreación de semillas); consulta de agregación sobre `OperationImage` para el espacio en disco.
- **Código modificado**: `feature/registro/OperationFormScreen.kt`/`OperationFormViewModel.kt` (HU-013, entrada "+ Agregar nueva" en los tres selectores de catálogo).
- **Datos**: sin migración de esquema nueva — `CatalogItem` y `Operation` ya existen (EP-001); no hay `@ForeignKey` declarado entre `Operation.assetId/emotionBeforeId/emotionAfterId/errorId` y `CatalogItem.id`, así que eliminar un `CatalogItem` no toca las filas de `Operation` a nivel de base de datos — el "valor histórico conservado" de HU-012 depende de esa ausencia de FK, no de un mecanismo nuevo (se documenta en `design.md`).
- **Dependencias**: ninguna nueva (mismo stack ya permitido en `stack-allowlist.json`).
- **Sin backend / sin red**: igual que EP-001/EP-005, 100% offline.

## Trazabilidad

- Épica: EP-002 — Gestión de catálogos (Etiquetas)
- Historias: HU-010, HU-011, HU-012, HU-013, HU-014
