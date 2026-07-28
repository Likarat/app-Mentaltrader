---
id: HU-034
titulo: Mantener orientación vertical en el resto de la app
epica: EP-005
prioridad: Could
complejidad: S
estado: lista
---

# Mantener orientación vertical en el resto de la app

**Construcción**: `openspec_change: plataforma-base-navegacion-tema-offline` (rama `feature/ep-005-plataforma-base-navegacion-tema-offline`).

## Historia

Como **trader de forex**,
quiero **que la app permanezca en orientación vertical en Inicio, Historial y Etiquetas**,
para **tener una experiencia consistente, sin reflows inesperados al rotar el dispositivo**.

## Contexto

La excepción de esta regla (permitir rotación a horizontal) vive exclusivamente en **HU-019** (EP-003, visor de imagen a pantalla completa) — esta historia no la redefine, solo confirma que el resto de la app permanece vertical, incluso al entrar/salir de esa excepción.

## Criterios de aceptación

### Escenario 1 — Happy path: orientación vertical en el uso general
- **Dado que** el usuario navega por Inicio, Historial o Etiquetas
- **Cuando** rota el celular
- **Entonces** el sistema mantiene la orientación vertical

### Escenario 2 — Edge: retorno a vertical tras salir del visor de imagen
- **Dado que** el usuario estaba en el visor de imagen a pantalla completa (HU-019) en horizontal
- **Cuando** vuelve a la vista de detalle o al listado (fuera del visor)
- **Entonces** el sistema fija nuevamente la orientación vertical, sin quedar atascado en horizontal

### Escenario 3 — Error: intento de forzar horizontal fuera del visor de imagen
- **Dado que** el usuario está en cualquier pantalla que no sea el visor de imagen a pantalla completa
- **Cuando** el sistema operativo o un gesto del usuario intenta forzar la orientación horizontal
- **Entonces** el sistema rechaza el cambio y permanece en vertical, sin excepción

## Notas técnicas (opcional)

- La excepción de rotación en el visor de imagen es responsabilidad exclusiva de HU-019; esta historia solo fija la regla general.

## Checklist INVEST

- [x] **I**ndependent — sí: fija la regla general; no redefine la excepción de HU-019, solo confirma que el resto de la app no se ve afectada por ella. **Nota de orden**: el Escenario 2 solo es verificable si HU-019 ya existe — declarar el orden HU-019 → HU-034 al priorizar (mismo tratamiento que HU-032 → HU-008).
- [x] **N**egotiable — el mecanismo técnico exacto (manifest, `requestedOrientation`) es negociable.
- [x] **V**aluable — sí: evita una experiencia inconsistente o reflows inesperados en el uso diario.
- [x] **E**stimable — alcance mínimo y bien acotado.
- [x] **S**mall — claramente cabe en menos de 2-4 días.
- [x] **T**estable — 3 escenarios en Given/When/Then con resultados observables (orientación mantenida, retorno a vertical tras la excepción, rechazo de forzado horizontal).
