---
id: HU-025
titulo: Ver estado vacío en Historial sin operaciones registradas
epica: EP-003
prioridad: Should
complejidad: S
estado: lista
---

# Ver estado vacío en Historial sin operaciones registradas

## Historia

Como **trader de forex**,
quiero **ver un mensaje amigable con acceso directo para crear mi primera operación cuando el Historial está vacío**,
para **saber qué hacer a continuación en vez de ver una pantalla en blanco**.

## Contexto

Última historia prevista de EP-003; con ella la épica queda completa. Se apoya en HU-015 (el listado ya debe existir) y en HU-008 (acceso rápido vía botón flotante, reutilizado como el acceso directo de este mensaje). Cubre dos casos distintos: nunca hubo operaciones registradas, vs. hay operaciones pero el filtro activo (HU-022/HU-023) no arroja resultados — mensajes distintos para cada caso.

## Criterios de aceptación

### Escenario 1 — Happy path: primera vez sin operaciones registradas
- **Dado que** el usuario no ha registrado ninguna operación aún
- **Cuando** abre Historial
- **Entonces** el sistema muestra un mensaje amigable invitando a crear la primera operación, con un acceso directo al formulario

### Escenario 2 — Edge: sin resultados para el filtro activo
- **Dado que** el usuario tiene operaciones registradas, pero el filtro o búsqueda activa (HU-022/HU-023) no coincide con ninguna
- **Cuando** el listado se renderiza
- **Entonces** el sistema muestra un mensaje indicando que no hay resultados para ese filtro, distinto del mensaje de "primera vez", sin ofrecer el acceso directo de creación (las operaciones sí existen, solo están filtradas)

### Escenario 3 — Edge: acceder al formulario desde el estado vacío inicial
- **Dado que** el sistema muestra el mensaje de "primera vez sin operaciones"
- **Cuando** el usuario toca el acceso directo del mensaje
- **Entonces** el sistema abre el formulario de "Nueva operación" (mismo destino que HU-008)

## Notas técnicas (opcional)

- Distinguir explícitamente "nunca hubo operaciones" de "hay operaciones pero el filtro no arroja resultados" (dos mensajes distintos, spec Requirement "Estados vacíos").

## Checklist INVEST

- [x] **I**ndependent — sí: depende de HU-015 (mismo epic) y reutiliza el destino de HU-008 (no duplica su lógica, solo navega al mismo lugar).
- [x] **N**egotiable — el texto exacto del mensaje es negociable.
- [x] **V**aluable — sí: orienta al usuario nuevo sobre qué hacer, evita una pantalla en blanco confusa.
- [x] **E**stimable — alcance acotado a dos mensajes condicionales + un acceso directo reutilizado.
- [x] **S**mall — cabe claramente en 2-4 días.
- [x] **T**estable — 3 escenarios en Given/When/Then con resultados observables (mensaje de primera vez, mensaje de filtro sin resultados, navegación al formulario).
