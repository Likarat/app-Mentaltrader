---
id: HU-007
titulo: Comprimir, redimensionar y generar miniatura cacheada al guardar
epica: EP-001
prioridad: Must
complejidad: M
estado: lista
---

# Comprimir, redimensionar y generar miniatura cacheada al guardar

## Historia

Como **trader de forex**,
quiero **que las imágenes adjuntas se redimensionen y compriman automáticamente, generando una miniatura una sola vez al guardar**,
para **mantener la app liviana en almacenamiento y con un listado fluido, sin tener que gestionar yo el tamaño de las fotos**.

## Contexto

Segundo slice vertical de "adjuntar imagen" (ver HU-006 para el primero). Depende de que exista una imagen adjunta sin procesar (HU-006), y agrega sobre esa base el procesamiento que sostiene el objetivo #5 del PRD (imágenes comprimidas ~200-400KB) y la fluidez del listado de EP-003 (miniatura cacheada, no recalculada en cada render).

## Criterios de aceptación

### Escenario 1 — Happy path: redimensionar y comprimir al adjuntar
- **Dado que** el usuario adjuntó una imagen sin procesar (HU-006) desde cámara o galería
- **Cuando** el sistema procesa la imagen antes de guardarla
- **Entonces** la almacena redimensionada a ~1600-2000px de ancho y comprimida en JPEG ~85% (~200-400KB)

### Escenario 2 — Edge: la miniatura se genera una sola vez al guardar
- **Dado que** el usuario adjuntó una imagen y guardó la operación
- **Cuando** el listado principal se re-renderiza múltiples veces (ej. al hacer scroll)
- **Entonces** el sistema reutiliza la miniatura ya generada al guardar, sin recalcularla en cada render

### Escenario 3 — Error: imagen de origen corrupta o ilegible
- **Dado que** el usuario selecciona un archivo de imagen corrupto o en un formato no soportado
- **Cuando** el sistema intenta procesarlo (redimensionar/comprimir)
- **Entonces** el sistema informa que la imagen no pudo procesarse y no la adjunta, permitiendo al usuario intentar con otra

## Notas técnicas (opcional)

- Redimensionado ~1600-2000px de ancho, compresión JPEG ~85%, objetivo ~200-400KB por imagen.
- Miniaturas usadas en el listado (EP-003) se generan una sola vez al guardar la operación, no en cada render — requisito de performance (spec §8).
- Carga de imágenes en UI vía Coil.

## Checklist INVEST

- [x] **I**ndependent — sí: depende secuencialmente de HU-006 (misma épica), no de ninguna historia de otra épica.
- [x] **N**egotiable — la librería/mecanismo exacto de compresión es negociable; los parámetros de tamaño/calidad ya están fijados por la spec.
- [x] **V**aluable — sí: sostiene el objetivo #5 del PRD (almacenamiento bajo control) y la fluidez del Historial (EP-003).
- [x] **E**stimable — alcance acotado a un pipeline de procesamiento con parámetros ya definidos.
- [x] **S**mall — alcance reducido respecto a la historia original; cabe razonablemente en 2-4 días.
- [x] **T**estable — 3 escenarios en Given/When/Then con resultados observables (tamaño/peso del archivo, no recálculo de miniatura, manejo de imagen corrupta).
