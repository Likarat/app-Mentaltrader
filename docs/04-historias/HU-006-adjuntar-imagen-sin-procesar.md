---
id: HU-006
titulo: Adjuntar imagen sin procesar (cámara o galería, máx. 2)
epica: EP-001
prioridad: Must
complejidad: M
estado: lista
---

# Adjuntar imagen sin procesar (cámara o galería, máx. 2)

## Historia

Como **trader de forex**,
quiero **adjuntar hasta 2 imágenes a una operación, tomándolas con la cámara o eligiéndolas de la galería**,
para **poder revisar visualmente el contexto del gráfico al estudiar la operación después**.

## Contexto

Primer slice vertical de "adjuntar imagen" (antes era una única historia con todo el pipeline, dividida tras fallar el criterio INVEST **S** en `/trycore:invest`: cámara/galería + permisos + resize + compresión + guardado + miniatura era demasiado para una sola historia). Este slice cubre selección, límite de 2 imágenes y el flujo de permisos con degradación graceful si se rechazan — ya es un incremento de valor completo por sí solo (el trader puede ver su gráfico adjunto, aunque todavía sin optimizar espacio en disco).

**HU-007** añade sobre esta base el redimensionado, la compresión y la generación de miniatura cacheada.

**Nota de solapamiento resuelta**: EP-005 preveía una historia separada para "solicitar permisos de cámara/almacenamiento al adjuntar imagen" (sin ID propio asignado todavía en el momento en que se detectó el solapamiento). Se determinó que esa historia no tenía alcance propio — pedir el permiso y manejar su rechazo es parte intrínseca de esta historia (no se puede adjuntar sin el permiso) — por lo que se retiró de EP-005 y su comportamiento vive completo aquí (ver Escenario 3). Ver `docs/03-backlog/epicas.md` §Reporte de cobertura para el detalle de esta resolución.

## Criterios de aceptación

### Escenario 1 — Happy path: adjuntar una imagen desde galería o cámara
- **Dado que** el usuario está creando o editando una operación y aún no tiene 2 imágenes adjuntas
- **Cuando** selecciona "Adjuntar imagen" y toma una foto o elige una de la galería
- **Entonces** el sistema guarda la referencia de la imagen a la operación y muestra una miniatura sin procesar en el formulario

### Escenario 2 — Edge: impedir una tercera imagen
- **Dado que** la operación ya tiene 2 imágenes adjuntas
- **Cuando** el usuario intenta adjuntar una imagen adicional
- **Entonces** el sistema impide adjuntar una tercera imagen a la misma operación

### Escenario 3 — Error: permiso de Cámara o Almacenamiento rechazado
- **Dado que** el usuario intenta adjuntar una imagen por primera vez
- **Cuando** rechaza el permiso de Cámara o Almacenamiento solicitado por el sistema
- **Entonces** el sistema informa que la función no está disponible sin el permiso, y permite continuar registrando la operación con normalidad, sin imagen

## Notas técnicas (opcional)

- Alcance intencionalmente sin procesamiento de imagen: el redimensionado, la compresión JPEG y la generación de miniatura cacheada quedan en HU-007.
- Persistencia de la referencia vía `OperationImage` (Room), `filePath` apuntando a `filesDir`, `position` 0 o 1.

## Checklist INVEST

- [x] **I**ndependent — sí: no depende de ninguna historia de EP-005; el flujo de permisos vive completo aquí. Solo depende secuencialmente de HU-001 (misma épica).
- [x] **N**egotiable — el mecanismo exacto de selección (picker nativo, UI de permisos) es negociable.
- [x] **V**aluable — sí: el trader ya puede ver su gráfico adjunto, aunque sin optimizar espacio.
- [x] **E**stimable — alcance acotado a selección + límite + permisos, sin ambigüedad.
- [x] **S**mall — alcance reducido respecto a la historia original; cabe razonablemente en 2-4 días.
- [x] **T**estable — 3 escenarios en Given/When/Then con resultados observables (miniatura visible, bloqueo de 3ra imagen, mensaje + continuidad sin imagen).
