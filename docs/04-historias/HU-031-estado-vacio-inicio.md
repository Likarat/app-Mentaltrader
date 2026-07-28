---
id: HU-031
titulo: Ver estado vacío en Inicio sin operaciones en el periodo
epica: EP-004
prioridad: Should
complejidad: S
estado: lista
---

# Ver estado vacío en Inicio sin operaciones en el periodo

## Historia

Como **trader de forex**,
quiero **ver un mensaje amigable cuando no hay operaciones en el periodo seleccionado en Inicio**,
para **entender que no hay datos que mostrar, en vez de ver un panel vacío o roto**.

## Contexto

Última historia prevista de EP-004; con ella la épica queda completa. Se apoya en HU-026/HU-027 (métricas/ranking) y HU-028/HU-029 (selector de periodo). El caso de "primera vez, nunca registró ninguna operación" reutiliza el mismo mensaje y acceso directo ya definidos en **HU-025** (la spec lo declara explícitamente compartido entre Inicio y Historial) — no se redefine aquí, solo se reutiliza en esta pantalla. El caso propio de esta historia es el periodo seleccionado sin datos, habiendo operaciones en otros periodos.

## Criterios de aceptación

### Escenario 1 — Happy path: primera vez sin operaciones registradas (reutiliza HU-025)
- **Dado que** el usuario no ha registrado ninguna operación aún
- **Cuando** abre Inicio
- **Entonces** el sistema muestra el mismo mensaje amigable y acceso directo al formulario ya definidos en HU-025, sin redefinir su contenido

### Escenario 2 — Edge: sin datos para el periodo seleccionado
- **Dado que** el usuario tiene operaciones registradas, pero ninguna cae dentro del periodo actualmente seleccionado
- **Cuando** la pantalla de Inicio se renderiza
- **Entonces** el sistema muestra un estado vacío indicando que no hay datos para ese periodo, sin ofrecer el acceso directo de creación (las operaciones sí existen, solo no en este rango)

### Escenario 3 — Edge: recuperación fluida al cambiar a un periodo con datos
- **Dado que** el sistema muestra el estado vacío de "sin datos para este periodo"
- **Cuando** el usuario selecciona un periodo distinto que sí tiene operaciones
- **Entonces** el sistema muestra el panel de métricas normalmente, sin rastros del estado vacío anterior

## Notas técnicas (opcional)

- El Escenario 1 no duplica la lógica de HU-025; ambas pantallas deben invocar el mismo componente/mensaje compartido (spec: "El sistema SHALL mostrar un estado vacío amigable en Inicio y en Historial").

## Checklist INVEST

- [x] **I**ndependent — sí: depende de HU-026/027/028/029 (mismo epic) y reutiliza el mensaje de HU-025 (EP-003) sin duplicar su lógica, solo invocándolo.
- [x] **N**egotiable — el texto exacto del mensaje de "sin datos para este periodo" es negociable.
- [x] **V**aluable — sí: evita un panel confuso o roto cuando no hay datos que mostrar.
- [x] **E**stimable — alcance acotado a un mensaje condicional + reutilización de un componente existente.
- [x] **S**mall — cabe claramente en 2-4 días.
- [x] **T**estable — 3 escenarios en Given/When/Then con resultados observables (mensaje de primera vez, mensaje de periodo sin datos, recuperación al cambiar de periodo).
