---
id: HU-015
titulo: Ver historial agrupado por mes y día con tarjeta compacta
epica: EP-003
prioridad: Must
complejidad: M
estado: lista
---

# Ver historial agrupado por mes y día con tarjeta compacta

## Historia

Como **trader de forex**,
quiero **ver mi historial de operaciones agrupado por mes y, dentro de cada mes, por día**,
para **estudiar mi evolución sin perderme en el detalle operación por operación**.

## Contexto

Primer slice vertical de "historial agrupado" (antes incluía también la garantía de fluidez a escala, dividida tras fallar el criterio INVEST **S**: mezclaba "hacer el historial correcto" con "garantizar que no se degrade con 3,000-5,000 operaciones", dos ciclos de verificación distintos). Este slice cubre la funcionalidad completa y observable para volúmenes normales de datos. **HU-016** añade sobre esta base la garantía de escala (Paging 3 / fluidez con datasets grandes). El resumen numérico por mes y el colapsar/expandir grupo se cubren en **HU-017**.

Depende de que existan operaciones registradas (HU-001, EP-001) para tener datos que agrupar.

## Criterios de aceptación

### Escenario 1 — Happy path: agrupación por mes, mes más reciente expandido
- **Dado que** existen operaciones registradas en distintos meses
- **Cuando** el usuario abre Historial
- **Entonces** el sistema muestra secciones por mes, con el mes más reciente arriba y expandido por defecto

### Escenario 2 — Happy path: agrupación por día dentro de cada mes
- **Dado que** un mes tiene operaciones registradas en distintos días
- **Cuando** el usuario visualiza ese grupo de mes
- **Entonces** el sistema muestra un encabezado por día, listando debajo las operaciones de ese día, con la más reciente primero

### Escenario 3 — Edge: tarjeta compacta de operación
- **Dado que** una operación aparece en el listado
- **Cuando** se renderiza su tarjeta
- **Entonces** el sistema muestra miniatura de imagen (o ícono genérico si no hay), Activo + Hora en la línea superior, y Resultado + Resultado en R coloreados (verde/rojo/gris) en la línea inferior

## Notas técnicas (opcional)

- Agrupamiento resuelto a nivel de base de datos (Room), no cargando todas las operaciones a memoria (spec §8/§9).
- La garantía de fluidez con datasets grandes (Paging 3) es responsabilidad de HU-016, no de esta historia — una consulta agrupada simple es suficiente para validar este alcance.

## Checklist INVEST

- [x] **I**ndependent — sí: depende de que existan operaciones (EP-001) como dato de prueba, no de alcance; no depende de HU-016 (esa es una mejora de escala sobre esta base, no un prerequisito) ni de HU-017 (resumen/colapso es capa adicional).
- [x] **N**egotiable — el diseño exacto de la tarjeta y del separador de mes es negociable.
- [x] **V**aluable — sí: sostiene directamente el objetivo #2 del PRD (historial agrupado).
- [x] **E**stimable — alcance acotado a agrupación + tarjeta, con parámetros ya fijados por la spec.
- [x] **S**mall — alcance reducido respecto a la historia original; cabe razonablemente en 2-4 días.
- [x] **T**estable — 3 escenarios en Given/When/Then con resultados observables (secciones por mes/día, contenido exacto de la tarjeta).
