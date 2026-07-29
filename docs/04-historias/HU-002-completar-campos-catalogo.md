---
id: HU-002
titulo: Completar operación con campos de catálogo obligatorios
epica: EP-001
prioridad: Must
complejidad: M
estado: lista
---

# Completar operación con campos de catálogo obligatorios

**Construcción**: `openspec_change: registro-rapido-operaciones` (rama `feature/ep-001-registro-rapido-operaciones`).

## Historia

Como **trader de forex**,
quiero **completar mi operación con los campos de catálogo obligatorios (Calidad, Emoción antes, Emoción después y Error), más el motivo en texto libre de cada uno (opcional)**,
para **enriquecer el registro con el contexto de ejecución y el estado emocional necesario para detectar patrones más adelante**.

## Contexto

Segundo slice vertical de la creación de operaciones (ver HU-001 para el primero). Depende de que la operación base ya exista/sea guardable (HU-001), pero no depende de que el usuario haya administrado manualmente ningún catálogo (EP-002): como parte del fix de independencia detectado en `/trycore:invest` sobre la historia original, se decidió sembrar el catálogo de Emociones con un conjunto inicial opcional de valores por defecto (la spec §3.1 ya contemplaba esta opción), de modo que "Emoción antes" y "Emoción después" siempre tengan al menos un valor seleccionable desde el primer uso — igual que "Error" ya lo tenía garantizado con la semilla obligatoria "Ninguno".

**Fix post-`/trycore:revisar` (auditoría de trazabilidad, 2026-07-27)**: se detectó que `emotionBeforeReason`, `emotionAfterReason` y `errorReason` (los motivos en texto libre de cada selección) eran consumidos por el AC de búsqueda de HU-023 sin que ninguna historia los creara. Se decidió que esta historia sea la dueña de crearlos, ya que acompañan naturalmente a sus respectivas selecciones de catálogo (misma sección del formulario).

**Semilla inicial propuesta para `EMOTION`** (editable después desde Etiquetas, EP-002): Confianza, Nervios, Ansiedad, Euforia, Frustración, Alivio, Miedo, Calma.

## Criterios de aceptación

### Escenario 1 — Happy path: completar Calidad, Emoción antes/después y Error sobre una operación existente
- **Dado que** el usuario está completando el formulario y ya tiene los campos mínimos de HU-001 llenos
- **Cuando** completa Calidad, Emoción antes, Emoción después y Error con valores válidos de catálogo, y presiona "Guardar"
- **Entonces** el sistema almacena los cuatro campos junto con el resto de la operación

### Escenario 2 — Error: intento de guardar sin completar uno de estos cuatro campos
- **Dado que** el usuario dejó sin completar al menos uno de los campos Calidad, Emoción antes, Emoción después o Error
- **Cuando** presiona "Guardar"
- **Entonces** el sistema impide el guardado y resalta visualmente el/los campo(s) faltante(s), sin cerrar el formulario

### Escenario 3 — Edge: catálogo de Emociones recién inicializado, solo con los valores semilla disponibles
- **Dado que** el catálogo de Emociones no tiene ningún valor agregado manualmente por el usuario (solo los valores semilla iniciales)
- **Cuando** el usuario selecciona un valor semilla como "Emoción antes" y otro como "Emoción después", completa el resto de campos y presiona "Guardar"
- **Entonces** el sistema guarda la operación con normalidad, con `emotionBeforeId`/`emotionAfterId` apuntando a los elementos semilla seleccionados

### Escenario 4 — Edge: catálogo de Error con un único valor semilla disponible
- **Dado que** el catálogo de Errores solo tiene disponible el elemento semilla "Ninguno" (catálogo recién inicializado, sin elementos adicionales agregados)
- **Cuando** el usuario selecciona "Ninguno" como Error y completa el resto de campos obligatorios antes de presionar "Guardar"
- **Entonces** el sistema guarda la operación con normalidad, con `errorId` apuntando al elemento semilla "Ninguno"

### Escenario 5 — Edge: completar los motivos en texto libre de cada selección
- **Dado que** el usuario seleccionó Emoción antes, Emoción después y Error
- **Cuando** completa opcionalmente el motivo de cada uno (`emotionBeforeReason`, `emotionAfterReason`, `errorReason`) y presiona "Guardar"
- **Entonces** el sistema almacena los tres motivos como texto libre, disponibles luego para la búsqueda por palabra clave (HU-023)

## Notas técnicas (opcional)

- Requiere que la migración/inicialización de base de datos siembre `CatalogItem(type = EMOTION)` con el set inicial listado arriba (`isDefault = true`, editable/eliminable a diferencia de la semilla de `ERROR`, ya que `EMOTION` no exige un elemento no-eliminable per spec §3.1).
- Esta siembra es un cambio de decisión de producto respecto al documento fuente original (que dejaba `EMOTION` sin semilla obligatoria "opcional"); queda registrada aquí y debe reflejarse también en `docs/03-backlog/epicas.md` (EP-002).
- `emotionBeforeReason`, `emotionAfterReason` y `errorReason` son campos de texto libre opcionales, sin límite de longitud obligatorio (spec, Requirement "Registro de operación").

## Checklist INVEST

- [x] **I**ndependent — sí: no depende de ninguna historia de EP-002 gracias a la siembra por defecto del catálogo de Emociones; solo depende de HU-001 (misma épica, secuenciación normal, no acoplamiento cruzado de épicas).
- [x] **N**egotiable — el set exacto de valores semilla de Emoción es negociable con el equipo/usuario.
- [x] **V**aluable — sí: habilita el contexto emocional que sostiene el ranking de patrones de EP-004 y la búsqueda de texto de HU-023.
- [x] **E**stimable — alcance acotado a 4 campos de catálogo + 3 campos de texto libre asociados, con tipos de control conocidos (selects + text fields).
- [x] **S**mall — alcance ampliado levemente (3 campos de texto opcionales más) pero sigue cabiendo razonablemente en 2-4 días; son inputs de texto libre sin lógica de negocio propia.
- [x] **T**estable — 5 escenarios en Given/When/Then con resultados observables, incluyendo el edge de catálogo de Emociones solo-semilla y el registro de los 3 motivos en texto libre.
