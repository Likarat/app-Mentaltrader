---
id: HU-019
titulo: Ver imagen en pantalla completa con zoom y navegación entre imágenes
epica: EP-003
prioridad: Could
complejidad: M
estado: lista
---

# Ver imagen en pantalla completa con zoom y navegación entre imágenes

## Historia

Como **trader de forex**,
quiero **tocar la imagen adjunta en el detalle de una operación para verla en pantalla completa con zoom, y deslizar entre imágenes si hay más de una**,
para **revisar con detalle el gráfico de la operación sin perder calidad ni contexto**.

## Contexto

Historia agregada tras detectar un vacío de cobertura: la spec define un Requirement completo de visor de imagen (zoom, navegación entre imágenes, rotación) y EP-005 ya referenciaba la excepción de orientación asociada, pero no existía una historia propia que construyera el visor en sí. Vive en EP-003 porque es parte del estudio de la operación (junto a HU-018, vista de detalle), no de la plataforma base.

Depende de HU-018 (la vista de detalle, desde donde se toca la imagen para abrir este visor) y de que existan imágenes adjuntas (HU-006/HU-007, EP-001). Esta historia es la responsable de la excepción de orientación (permitir rotación a horizontal solo en esta pantalla) — EP-005 solo fija la regla general de "vertical en el resto de la app".

## Criterios de aceptación

### Escenario 1 — Happy path: abrir la imagen en pantalla completa
- **Dado que** una operación tiene una o más imágenes adjuntas y el usuario está en su vista de detalle
- **Cuando** toca la imagen
- **Entonces** el sistema la muestra en tamaño completo, con fondo negro/oscuro alrededor

### Escenario 2 — Happy path: hacer zoom sobre la imagen
- **Dado que** el usuario está en la vista de imagen a pantalla completa
- **Cuando** hace el gesto de pellizcar/ampliar sobre la imagen
- **Entonces** el sistema aplica el zoom correspondiente sobre la imagen

### Escenario 3 — Edge: navegar entre 2 imágenes de la misma operación
- **Dado que** una operación tiene 2 imágenes adjuntas y el usuario abre la vista de imagen a pantalla completa
- **Cuando** la vista carga
- **Entonces** el sistema muestra primero la primera imagen con un indicador de puntos en la parte inferior, y permite avanzar deslizando o tocando una flecha a la derecha

### Escenario 4 — Edge: rotación permitida solo en esta vista
- **Dado que** el usuario está en la vista de imagen a pantalla completa
- **Cuando** rota el dispositivo a horizontal
- **Entonces** el sistema permite la rotación, a diferencia del resto de la app (que permanece fija en vertical)

## Notas técnicas (opcional)

- Única pantalla de la app donde se permite rotación a horizontal (excepción documentada en EP-005, implementada aquí).

## Checklist INVEST

- [x] **I**ndependent — sí: depende de HU-018 (mismo epic, se abre desde el detalle) y de que existan imágenes (EP-001, dato de prueba); no depende de EP-005.
- [x] **N**egotiable — el mecanismo exacto de zoom/swipe (librería, gestos) es negociable.
- [x] **V**aluable — sí: permite revisar el gráfico con detalle, parte central del propósito de estudio del producto.
- [x] **E**stimable — patrón conocido de visor de imagen (zoom + pager + excepción de orientación), sin ambigüedad de alcance.
- [x] **S**mall — es un único componente cohesivo (visor de imagen), no múltiples reglas de negocio heterogéneas; cabe razonablemente en 2-4 días.
- [x] **T**estable — 4 escenarios en Given/When/Then con resultados observables (imagen a pantalla completa, zoom aplicado, navegación entre imágenes, rotación permitida).
