---
id: HU-021
titulo: Eliminar una operación con opción de deshacer
epica: EP-003
prioridad: Must
complejidad: M
estado: lista
---

# Eliminar una operación con opción de deshacer

## Historia

Como **trader de forex**,
quiero **eliminar una operación desde su vista de detalle, con la posibilidad de deshacer la acción durante un breve período**,
para **poder corregir un borrado accidental sin perder el registro permanentemente**.

## Contexto

Se apoya en HU-018 (vista de detalle, botón "Eliminar"). Al confirmarse definitivamente la eliminación (pasado el tiempo de deshacer), se eliminan también los archivos de imagen asociados (`OperationImage`, spec §3.1). Mientras la ventana de "Deshacer" esté activa, la operación no debe eliminarse físicamente todavía (soft-delete temporal), para poder restaurarla sin pérdida.

## Criterios de aceptación

### Escenario 1 — Happy path: eliminar una operación
- **Dado que** el usuario está en la vista de detalle de una operación
- **Cuando** presiona "Eliminar" y confirma
- **Entonces** el sistema retira la operación del listado y muestra un snackbar temporal con la opción "Deshacer"

### Escenario 2 — Edge: deshacer la eliminación dentro del tiempo disponible
- **Dado que** el snackbar de "Deshacer" está visible tras eliminar una operación
- **Cuando** el usuario toca "Deshacer" dentro del tiempo disponible
- **Entonces** el sistema restaura la operación (y sus imágenes) exactamente como estaba, mostrándola de nuevo en el listado

### Escenario 3 — Edge: eliminación definitiva tras expirar el tiempo de deshacer
- **Dado que** el usuario eliminó una operación y no tocó "Deshacer" dentro del tiempo disponible
- **Cuando** el snackbar desaparece
- **Entonces** el sistema elimina permanentemente la operación y sus imágenes asociadas (archivos en `filesDir`)

## Notas técnicas (opcional)

- Patrón soft-delete temporal: la eliminación física (registro + archivos de imagen) se difiere hasta que expire la ventana de "Deshacer".
- Al eliminar definitivamente, se borran también los archivos de imagen asociados (spec §3.1).

## Checklist INVEST

- [x] **I**ndependent — sí: depende de HU-018 (mismo epic, se dispara desde el detalle), sin acoplarse a otra épica.
- [x] **N**egotiable — la duración exacta de la ventana de deshacer es negociable.
- [x] **V**aluable — sí: protege contra el borrado accidental de un registro de estudio valioso.
- [x] **E**stimable — patrón conocido (soft-delete + snackbar de deshacer), sin ambigüedad de alcance.
- [x] **S**mall — cabe razonablemente en 2-4 días.
- [x] **T**estable — 3 escenarios en Given/When/Then con resultados observables (snackbar mostrado, operación restaurada, eliminación definitiva con archivos borrados).
