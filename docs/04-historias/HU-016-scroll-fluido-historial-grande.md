---
id: HU-016
titulo: Garantizar scroll fluido con historiales grandes (3,000-5,000 operaciones)
epica: EP-003
prioridad: Should
complejidad: S
estado: lista
---

# Garantizar scroll fluido con historiales grandes (3,000-5,000 operaciones)

## Historia

Como **trader de forex**,
quiero **que el Historial siga navegándose con fluidez aunque tenga miles de operaciones registradas**,
para **poder revisar mi historial completo a largo plazo sin que la app se vuelva lenta o inutilizable**.

## Contexto

Segundo slice vertical de "historial agrupado" (ver HU-015 para el primero). Depende de HU-015 para la consulta agrupada base — esta historia integra la carga incremental (Paging 3) sobre esa consulta y verifica la fluidez a escala con un dataset grande de prueba.

## Criterios de aceptación

### Escenario 1 — Happy path: scroll fluido con historial grande
- **Dado que** el usuario tiene más de 3,000 operaciones registradas
- **Cuando** navega por el listado de Historial
- **Entonces** el sistema carga las operaciones de forma incremental a medida que el usuario hace scroll, sin cargar todo el histórico a memoria y sin caídas de fluidez perceptibles

### Escenario 2 — Edge: carga inicial rápida al abrir Historial con dataset grande
- **Dado que** el usuario tiene más de 3,000 operaciones registradas
- **Cuando** abre la pestaña Historial por primera vez en la sesión
- **Entonces** el sistema muestra las primeras operaciones sin esperar a cargar el histórico completo

### Escenario 3 — Edge: historial en el límite superior del rango soportado (5,000 operaciones)
- **Dado que** el usuario tiene 5,000 operaciones registradas (límite superior del rango soportado por la spec)
- **Cuando** navega por todo el listado hasta el final
- **Entonces** el sistema mantiene la misma fluidez que con datasets menores, sin errores ni caídas perceptibles

## Notas técnicas (opcional)

- Implementación vía Paging 3 (Jetpack) sobre la consulta agrupada de HU-015.
- Verificación con dataset sintético (3,000-5,000 registros de prueba) antes de dar por cumplido el KPI #3 del PRD.

## Checklist INVEST

- [x] **I**ndependent — sí: depende de HU-015 (misma épica, la consulta base debe existir), sin acoplarse a ninguna otra épica.
- [x] **N**egotiable — el mecanismo exacto (Paging 3, tamaño de página, prefetch) queda en Notas técnicas, no prescrito en los AC.
- [x] **V**aluable — sí: sostiene directamente el KPI #3 del PRD (3,000-5,000 operaciones sin degradar el scroll).
- [x] **E**stimable — alcance acotado a integración de paginación + verificación de escala.
- [x] **S**mall — alcance reducido respecto a la historia original; cabe razonablemente en 2-4 días.
- [x] **T**estable — 3 escenarios en Given/When/Then con resultados observables (fluidez, carga inicial rápida, comportamiento en el límite superior del rango).
