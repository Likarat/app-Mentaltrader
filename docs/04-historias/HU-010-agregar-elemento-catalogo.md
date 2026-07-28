---
id: HU-010
titulo: Agregar elemento a un catálogo (Activos, Emociones o Errores)
epica: EP-002
prioridad: Must
complejidad: S
estado: lista
---

# Agregar elemento a un catálogo (Activos, Emociones o Errores)

## Historia

Como **trader de forex**,
quiero **agregar un nuevo elemento a los catálogos de Activos, Emociones o Errores desde la pestaña Etiquetas**,
para **tener disponibles las opciones que reflejan mi forma real de operar al registrar una operación**.

## Contexto

Primer slice vertical de la administración de catálogos (antes era una única historia con agregar/editar/eliminar, dividida tras fallar el criterio INVEST **S** en `/trycore:invest`: 3 catálogos × 3 operaciones con reglas de negocio distintas excedía 2-4 días). Este slice cubre la operación más usada del día a día: agregar. **HU-011** añade sobre esta base editar y eliminar (con protección de semillas).

No depende de EP-001 para entregar valor: las semillas se crean por migración/inicialización de base de datos, no por esta historia — la administración de catálogos funciona igual con o sin que el formulario de registro esté implementado.

## Criterios de aceptación

### Escenario 1 — Happy path: agregar un nuevo elemento a un catálogo
- **Dado que** el usuario está en la administración de un catálogo (ej. Activos)
- **Cuando** agrega un nuevo elemento (ej. "EURUSD")
- **Entonces** el sistema lo guarda y lo muestra disponible inmediatamente en los formularios de registro

### Escenario 2 — Error: intento de agregar un elemento duplicado
- **Dado que** el catálogo ya contiene un elemento con un nombre determinado
- **Cuando** el usuario intenta agregar otro elemento con el mismo nombre (ignorando mayúsculas/minúsculas y espacios al inicio/final)
- **Entonces** el sistema impide guardarlo y muestra el mensaje "Este valor ya existe en el catálogo"

### Escenario 3 — Edge: el mismo nombre es válido en catálogos de tipo distinto
- **Dado que** el catálogo de Errores ya tiene un elemento llamado "Noticias"
- **Cuando** el usuario agrega un elemento llamado "Noticias" al catálogo de Activos
- **Entonces** el sistema lo permite, ya que la regla de unicidad aplica solo dentro del mismo `type` de catálogo, no entre catálogos distintos

## Notas técnicas (opcional)

- Los tres catálogos comparten la entidad `CatalogItem`, diferenciados por `type` (`ASSET` / `EMOTION` / `ERROR`) — misma pantalla genérica parametrizada, no 3 implementaciones distintas.
- Unicidad: comparación case-insensitive, ignorando espacios al inicio/final. Mensaje de error: "Este valor ya existe en el catálogo".

## Checklist INVEST

- [x] **I**ndependent — sí: no depende de EP-001 (las semillas son de inicialización de datos); no depende de HU-011 (editar/eliminar es una operación distinta sobre los mismos datos).
- [x] **N**egotiable — el "qué" (agregar con validación de duplicados) está claro; el "cómo" (widget exacto de input) queda abierto.
- [x] **V**aluable — sí: mantener catálogos relevantes es condición para que el registro (EP-001) refleje la realidad del usuario.
- [x] **E**stimable — alcance acotado a una sola operación con una regla de negocio (unicidad).
- [x] **S**mall — alcance reducido respecto a la historia original; cabe razonablemente en 2-4 días.
- [x] **T**estable — 3 escenarios en Given/When/Then con resultados observables (elemento disponible en formularios, mensaje de error de duplicado, unicidad acotada por tipo de catálogo).
