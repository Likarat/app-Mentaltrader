---
id: HU-001
titulo: Registrar operación con campos identificadores mínimos
epica: EP-001
prioridad: Must
complejidad: M
estado: lista
---

# Registrar operación con campos identificadores mínimos

**Construcción**: `openspec_change: registro-rapido-operaciones` (rama `feature/ep-001-registro-rapido-operaciones`).

## Historia

Como **trader de forex**,
quiero **registrar una operación con sus campos identificadores mínimos (Fecha, Hora, Activo, Dirección y Resultado), más los campos opcionales de contexto del resultado (Riesgo % y Resultado en R)**,
para **dejar una constancia mínima y verificable de que la operación ocurrió, disponible de inmediato en el listado, con el contexto de riesgo/resultado que necesito para mis métricas**.

## Contexto

Primer slice vertical de la creación de operaciones (antes era una única historia monolítica "Crear una operación completa", dividida tras fallar los criterios INVEST **I** y **S** en `/trycore:invest`). Esta historia por sí sola ya entrega valor: una operación mínima, persistida y visible en el listado.

**HU-002** añade sobre esta base los campos de catálogo obligatorios (Calidad, Emoción antes, Emoción después, Error). **HU-003** añade la Descripción entrada. Ninguna de las tres depende del catálogo de Emociones (que no tiene semilla obligatoria) ni de ninguna historia de EP-002, porque el único catálogo que toca este slice es Activo, que sí tiene semilla obligatoria (`XAUUSD`, ver spec §3.1) — por eso esta historia es independiente y entregable de punta a punta sin EP-002.

**Fix post-`/trycore:revisar` (auditoría de trazabilidad, 2026-07-27)**: se detectó que `Riesgo (%)` y `Resultado en R` eran consumidos por los AC de HU-004 (validación de rango), HU-015/HU-017 (visualización en tarjeta/resumen) y HU-026 (métricas de R acumulado) sin que ninguna historia los creara realmente en el formulario. Se decidió que esta historia (no una historia nueva) sea la dueña de crear estos dos campos opcionales, ya que conceptualmente acompañan a "Resultado" (mismo bloque de datos de cierre de la operación). HU-004 sigue siendo la única dueña de sus reglas de rango/formato; esta historia solo los hace guardables.

## Criterios de aceptación

### Escenario 1 — Happy path: guardar una operación con los 5 campos mínimos
- **Dado que** el usuario está en el formulario de "Nueva operación" y completó Fecha, Hora, Activo, Dirección y Resultado con valores válidos
- **Cuando** presiona "Guardar"
- **Entonces** el sistema almacena la operación localmente y la muestra inmediatamente en el listado principal, en la posición correspondiente por fecha (más reciente primero)

### Escenario 2 — Error: intento de guardar sin completar uno de los campos mínimos
- **Dado que** el usuario dejó sin completar al menos uno de los 5 campos mínimos (ej. Activo o Resultado)
- **Cuando** presiona "Guardar"
- **Entonces** el sistema impide el guardado y resalta visualmente el/los campo(s) faltante(s), sin cerrar el formulario

### Escenario 3 — Edge: guardar seleccionando el activo semilla en un catálogo de Activos recién inicializado
- **Dado que** el catálogo de Activos no tiene ningún elemento agregado manualmente (solo la semilla "XAUUSD" auto-creada)
- **Cuando** el usuario selecciona "XAUUSD" como Activo y completa el resto de campos mínimos antes de presionar "Guardar"
- **Entonces** el sistema guarda la operación con normalidad, con `assetId` apuntando al elemento semilla "XAUUSD"

### Escenario 4 — Edge: guardar con los campos opcionales de Riesgo (%) y Resultado en R completados
- **Dado que** el usuario completó los 5 campos mínimos y además ingresó un valor en Riesgo (%) y en Resultado en R
- **Cuando** presiona "Guardar"
- **Entonces** el sistema almacena ambos valores junto con el resto de la operación, dejando su validación de rango a cargo de HU-004

### Escenario 5 — Edge: guardar dejando Riesgo (%) y Resultado en R vacíos
- **Dado que** el usuario completó los 5 campos mínimos pero dejó vacíos Riesgo (%) y Resultado en R (ambos opcionales)
- **Cuando** presiona "Guardar"
- **Entonces** el sistema guarda la operación con normalidad, dejando ambos campos como nulos

## Notas técnicas (opcional)

- Alcance: 5 campos identificadores mínimos + 2 campos opcionales de contexto de resultado (Riesgo %, Resultado en R) — 7 de los 10 campos obligatorios/opcionales cubiertos por EP-001 fuera de HU-002/HU-003. Calidad, Emoción antes/después, Error se cubren en HU-002; Descripción entrada en HU-003; los 3 campos de motivo en texto libre (`emotionBeforeReason`, `emotionAfterReason`, `errorReason`) se cubren en HU-002 (van junto a sus respectivas selecciones de catálogo).
- Persistencia vía Room; el guardado debe ubicar la operación en el listado principal ordenado por fecha (más reciente primero) — dependencia de lectura hacia EP-003, no de escritura.
- Resultado en R: signo definido por dropdown "+/-", nunca escrito como texto; teclado numérico restringido a dígitos y separador decimal (ver spec, Requirement "Formulario de registro").

## Checklist INVEST

- [x] **I**ndependent — sí: no depende de EP-002 ni de ninguna otra historia de EP-001; el único catálogo que toca (Activo) tiene semilla obligatoria.
- [x] **N**egotiable — el "cómo" (controles exactos de UI) queda abierto a discusión con el equipo.
- [x] **V**aluable — sí: una operación mínima ya es útil (fecha/hora/activo/dirección/resultado permiten un primer conteo de ganadas/perdidas); Riesgo %/R habilitan las métricas de EP-004.
- [x] **E**stimable — alcance acotado a 7 campos con tipos de control bien conocidos (fecha, hora, 2 selects, enum, 2 decimales); el equipo puede estimar con confianza.
- [x] **S**mall — alcance ampliado levemente (2 campos opcionales más) pero sigue cabiendo razonablemente en 2-4 días; ambos campos son simples inputs decimales sin lógica de negocio propia (la validación vive en HU-004).
- [x] **T**estable — 5 escenarios en Given/When/Then con resultados observables (persistencia, resaltado de campos, `assetId` concreto, campos opcionales completados/vacíos).
