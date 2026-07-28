---
id: HU-011
titulo: Editar y eliminar elemento de un catálogo (con protección de semillas)
epica: EP-002
prioridad: Should
complejidad: S
estado: lista
---

# Editar y eliminar elemento de un catálogo (con protección de semillas)

## Historia

Como **trader de forex**,
quiero **editar el nombre de un elemento existente o eliminarlo de los catálogos de Activos, Emociones o Errores**,
para **corregir o depurar mis catálogos sin perder los elementos semilla que la app necesita para funcionar**.

## Contexto

Segundo slice vertical de la administración de catálogos (ver HU-010 para el primero). Es independiente de HU-010: puede probarse sobre los elementos ya sembrados por migración (`Ninguno`, `XAUUSD`, set inicial de Emociones) sin que exista todavía la UI de "agregar". La eliminación de un elemento **en uso** por operaciones existentes (con confirmación y conteo) se cubre en **HU-012**, no aquí — esta historia asume elementos sin uso o elementos semilla.

## Criterios de aceptación

### Escenario 1 — Happy path: editar un elemento existente
- **Dado que** el usuario está en la administración de un catálogo y selecciona un elemento existente para editar
- **Cuando** modifica su nombre y confirma
- **Entonces** el sistema actualiza el elemento, reflejando el nuevo nombre en las próximas selecciones del formulario

### Escenario 2 — Edge: eliminar un elemento sin uso en ninguna operación
- **Dado que** un elemento del catálogo no ha sido usado en ninguna operación registrada
- **Cuando** el usuario lo elimina
- **Entonces** el sistema lo elimina de inmediato, sin pedir confirmación adicional

### Escenario 3 — Edge: intento de eliminar un elemento semilla no eliminable
- **Dado que** el usuario intenta eliminar el elemento semilla "Ninguno" del catálogo de Errores, o "XAUUSD" del catálogo de Activos siendo este el único activo existente
- **Cuando** confirma la acción de eliminar
- **Entonces** el sistema impide la eliminación, indicando que ese elemento no puede eliminarse en esta condición

### Escenario 4 — Edge: recreación automática de semilla al vaciar el catálogo
- **Dado que** el usuario eliminó todos los elementos de un catálogo hasta dejarlo sin ninguno (ej. borró todos los activos agregados manualmente, incluyendo la última semilla "XAUUSD" tras haber tenido otro activo)
- **Cuando** el catálogo queda sin ningún elemento
- **Entonces** el sistema recrea automáticamente el elemento semilla correspondiente a ese catálogo

## Notas técnicas (opcional)

- Semillas: `Ninguno` (`ERROR`, no eliminable) y `XAUUSD` (`ASSET`, eliminable solo si existe otro activo) se recrean automáticamente si el catálogo queda vacío.
- Edición no debe permitir renombrar a un valor duplicado (misma regla de unicidad de HU-010).

## Checklist INVEST

- [x] **I**ndependent — sí: no depende de HU-010 (puede probarse sobre elementos ya sembrados por migración); no depende de HU-012 (esa cubre solo el caso de eliminar un elemento *en uso*).
- [x] **N**egotiable — el "qué" (editar/eliminar con protección de semillas) está claro; el "cómo" (mensaje exacto, gesto de eliminar) queda abierto.
- [x] **V**aluable — sí: permite corregir errores de tipeo o depurar catálogos sin arriesgar la integridad de las semillas que la app necesita.
- [x] **E**stimable — alcance acotado a dos operaciones con reglas de negocio ya fijadas (protección de semillas).
- [x] **S**mall — alcance reducido respecto a la historia original; cabe razonablemente en 2-4 días.
- [x] **T**estable — 4 escenarios en Given/When/Then con resultados observables (nombre actualizado, eliminación inmediata, bloqueo de eliminación de semilla, recreación automática de semilla al vaciar el catálogo).
