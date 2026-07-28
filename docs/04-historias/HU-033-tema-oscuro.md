---
id: HU-033
titulo: Aplicar tema oscuro en toda la app
epica: EP-005
prioridad: Must
complejidad: S
estado: lista
---

# Aplicar tema oscuro en toda la app

**Construcción**: `openspec_change: plataforma-base-navegacion-tema-offline` (rama `feature/ep-005-plataforma-base-navegacion-tema-offline`).

## Historia

Como **trader de forex**,
quiero **que toda la app use un tema oscuro por defecto**,
para **facilitar el estudio de cada entrada sin fatiga visual**.

## Contexto

Historia de infraestructura visual base. No depende de ninguna otra historia; el tema debe aplicarse consistentemente en todas las pantallas ya construidas o por construir.

## Criterios de aceptación

### Escenario 1 — Happy path: la app abre en modo oscuro
- **Dado que** el usuario abre la app
- **Cuando** cualquier pantalla se renderiza
- **Entonces** el sistema aplica una paleta oscura (fondo oscuro, texto claro, acentos de color para ganada/perdida)

### Escenario 2 — Edge: sin modo claro alternativo, independiente del tema del sistema
- **Dado que** el dispositivo del usuario está configurado en modo claro a nivel de sistema operativo
- **Cuando** el usuario abre la app
- **Entonces** el sistema mantiene el tema oscuro de todas formas (no hay modo claro en v1, fuera de alcance según el PRD)

### Escenario 3 — Edge: consistencia de acentos de color en todas las pantallas
- **Dado que** el usuario navega entre Inicio, Historial y Etiquetas
- **Cuando** observa elementos con codificación de color (verde/rojo para ganada/perdida)
- **Entonces** el sistema mantiene los mismos acentos de color de forma consistente en las tres pantallas

## Notas técnicas (opcional)

- Modo claro explícitamente fuera de alcance en v1 (PRD, Non-goals).

## Checklist INVEST

- [x] **I**ndependent — sí: no depende de ninguna otra historia; es infraestructura visual base.
- [x] **N**egotiable — la paleta exacta de colores es negociable.
- [x] **V**aluable — sí: facilita el estudio de cada entrada, mencionado explícitamente en el propósito del producto.
- [x] **E**stimable — alcance acotado, tema único sin alternancia.
- [x] **S**mall — cabe claramente en 2-4 días.
- [x] **T**estable — 3 escenarios en Given/When/Then con resultados observables (paleta oscura aplicada, sin modo claro, consistencia de acentos entre pantallas).
