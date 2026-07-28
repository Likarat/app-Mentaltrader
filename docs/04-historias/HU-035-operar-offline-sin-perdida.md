---
id: HU-035
titulo: Operar 100% offline sin pérdida de datos entre sesiones
epica: EP-005
prioridad: Must
complejidad: S
estado: lista
---

# Operar 100% offline sin pérdida de datos entre sesiones

**Construcción**: `openspec_change: plataforma-base-navegacion-tema-offline` (rama `feature/ep-005-plataforma-base-navegacion-tema-offline`). Escenarios 2 y 3 (persistencia Room) diferidos a la construcción de EP-001 — ver `openspec/changes/plataforma-base-navegacion-tema-offline/design.md`.

## Historia

Como **trader de forex**,
quiero **que la app funcione completamente sin conexión a internet y sin perder datos entre sesiones**,
para **poder usarla en cualquier momento y confiar en que mi historial persiste**.

## Contexto

Última historia prevista de EP-005; con ella la épica y el backlog completo (35 historias) quedan cubiertos. Es en gran parte una historia de verificación de una propiedad transversal que las demás historias (HU-001 en adelante) ya construyen sobre Room — confirma que ninguna funcionalidad depende de red y que la persistencia sobrevive a reinicios completos de la app.

## Criterios de aceptación

### Escenario 1 — Happy path: uso completo sin conexión a internet
- **Dado que** el dispositivo no tiene conexión a internet
- **Cuando** el usuario crea, edita, elimina o consulta operaciones
- **Entonces** el sistema funciona con normalidad, sin ningún error relacionado a falta de red

### Escenario 2 — Happy path: persistencia entre sesiones
- **Dado que** el usuario cierra completamente la app
- **Cuando** vuelve a abrirla
- **Entonces** el sistema muestra todas las operaciones previamente registradas, sin pérdida de datos

### Escenario 3 — Edge: imágenes adjuntas se cargan desde disco local, sin ninguna solicitud de red
- **Dado que** el usuario tiene operaciones con imágenes adjuntas (HU-006/HU-007)
- **Cuando** navega por el listado o abre el detalle de una operación con imagen
- **Entonces** el sistema carga las imágenes exclusivamente desde `filesDir` local, sin realizar ninguna solicitud de red (ni siquiera para las miniaturas)

## Notas técnicas (opcional)

- Sin llamadas de red en ningún punto de la app (v1 sin backend, spec §1/§9).
- La garantía de bajo consumo de recursos con historiales grandes (miles de operaciones) es responsabilidad de HU-016 (Historial) y de las notas técnicas de HU-026/HU-027 (Inicio); este escenario cubre específicamente la carga de imágenes offline, que ninguna otra historia prueba.

## Checklist INVEST

- [x] **I**ndependent — sí: no depende de ninguna otra épica; verifica una propiedad transversal ya construida sobre Room por las demás historias.
- [x] **N**egotiable — el mecanismo técnico exacto (Room, verificación de ausencia de red) es negociable.
- [x] **V**aluable — sí: sostiene directamente el objetivo #4 del PRD (100% offline, sin pérdida de datos) y la confianza del usuario en la app.
- [x] **E**stimable — alcance acotado, principalmente de verificación.
- [x] **S**mall — cabe claramente en 2-4 días.
- [x] **T**estable — 3 escenarios en Given/When/Then con resultados observables (funcionamiento sin red, datos preservados tras reinicio, imágenes cargadas solo desde disco local).
