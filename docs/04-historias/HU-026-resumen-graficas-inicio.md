---
id: HU-026
titulo: Ver resumen numérico y gráficas de R acumulado / distribución de resultados
epica: EP-004
prioridad: Must
complejidad: M
estado: lista
---

# Ver resumen numérico y gráficas de R acumulado / distribución de resultados

## Historia

Como **trader de forex**,
quiero **ver en Inicio un resumen numérico de mi desempeño y gráficas de R acumulado y distribución de resultados**,
para **tener un panorama rápido de cómo me está yendo, sin tener que revisar operación por operación**.

## Contexto

Historia núcleo de EP-004. Depende de que existan operaciones registradas (EP-001) para tener datos que agregar. Cubre las tarjetas de resumen numérico, la gráfica de línea de R acumulado y la gráfica de barras de distribución de resultados — las 3 comparten la misma fuente de datos (operaciones del periodo seleccionado), solo difieren en visualización, por lo que se mantienen juntas como un mismo slice vertical ("ver mi desempeño de un vistazo"). El selector de periodo en sí (predefinido/personalizado) y su persistencia se cubren en **HU-028/HU-029/HU-030**; esta historia asume que ya existe algún periodo seleccionado (por defecto el más reciente disponible).

## Criterios de aceptación

### Escenario 1 — Happy path: tarjetas de resumen numérico
- **Dado que** existen operaciones registradas dentro del periodo seleccionado
- **Cuando** el usuario abre Inicio
- **Entonces** el sistema muestra tarjetas con: total de operaciones, % ganadas/perdidas/break even, calidad promedio, R total acumulado y R promedio por operación

### Escenario 2 — Happy path: gráfica de línea de R acumulado
- **Dado que** existen operaciones registradas dentro del periodo seleccionado
- **Cuando** la pantalla de Inicio se renderiza
- **Entonces** el sistema muestra una gráfica de línea del R acumulado a lo largo del periodo

### Escenario 3 — Happy path: gráfica de barras de distribución de resultados
- **Dado que** existen operaciones registradas dentro del periodo seleccionado
- **Cuando** la pantalla de Inicio se renderiza
- **Entonces** el sistema muestra una gráfica de barras con el % de operaciones Ganadas, Perdidas y Break Even

### Escenario 4 — Edge: cálculo seguro con cero operaciones en el periodo
- **Dado que** no existen operaciones dentro del periodo seleccionado
- **Cuando** el sistema calcula los porcentajes y promedios
- **Entonces** evita divisiones por cero, mostrando valores neutros (0 o "—") sin errores ni caídas

## Notas técnicas (opcional)

- Cálculos resueltos a nivel de base de datos (Room), no cargando todas las operaciones a memoria (spec §8/§9).
- El Escenario 4 cubre el cálculo seguro; el mensaje de estado vacío visible al usuario es responsabilidad de **HU-031**, no de esta historia.

## Checklist INVEST

- [x] **I**ndependent — sí: depende de EP-001 solo como dato de prueba; asume un periodo ya seleccionado sin depender del selector de HU-028/029/030 para su propio cálculo.
- [x] **N**egotiable — la librería de gráficas y el diseño exacto de las tarjetas son negociables (spec: "librería ligera compatible con Compose, a confirmar en diseño técnico").
- [x] **V**aluable — sí: sostiene directamente el propósito central del producto (detectar patrones, mejora continua).
- [x] **E**stimable — 3 visualizaciones sobre la misma fuente de datos agregada, con métricas ya fijadas por la spec.
- [x] **S**mall — cohesivo (una sola fuente de datos, 3 formas de visualizarla), cabe razonablemente en 2-4 días.
- [x] **T**estable — 4 escenarios en Given/When/Then con resultados observables (tarjetas, gráfica de línea, gráfica de barras, cálculo seguro con cero datos).
