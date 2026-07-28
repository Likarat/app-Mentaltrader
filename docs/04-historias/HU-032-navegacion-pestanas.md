---
id: HU-032
titulo: Navegar entre Inicio, Historial y Etiquetas
epica: EP-005
prioridad: Must
complejidad: S
estado: lista
---

# Navegar entre Inicio, Historial y Etiquetas

**Construcción**: `openspec_change: plataforma-base-navegacion-tema-offline` (rama `feature/ep-005-plataforma-base-navegacion-tema-offline`).

## Historia

Como **trader de forex**,
quiero **navegar entre las pestañas Inicio, Historial y Etiquetas desde una barra inferior**,
para **acceder rápido a cualquier parte de la app**.

## Contexto

Historia de infraestructura base (walking skeleton) de la que dependen secuencialmente otras historias de otras épicas (ej. HU-008 en EP-001, que asume que Inicio y Historial ya existen como destinos navegables). Es la primera historia recomendable a construir del proyecto.

## Criterios de aceptación

### Escenario 1 — Happy path: la app abre en Inicio
- **Dado que** el usuario abre la app
- **Cuando** termina de cargar
- **Entonces** el sistema muestra la pestaña de Inicio

### Escenario 2 — Happy path: cambiar entre pestañas
- **Dado que** el usuario está en cualquier pestaña
- **Cuando** toca otro destino en la barra inferior
- **Entonces** el sistema cambia a esa pestaña de forma inmediata

### Escenario 3 — Edge: resaltado del ícono activo
- **Dado que** el usuario está en una pestaña determinada
- **Cuando** la barra inferior se renderiza
- **Entonces** el ícono de la pestaña activa se muestra resaltado (color de acento) frente a los inactivos (color neutro/atenuado)

### Escenario 4 — Edge: exactamente 3 destinos, sin una cuarta pestaña de "Métricas"
- **Dado que** la barra de navegación inferior se renderiza
- **Cuando** el usuario la observa
- **Entonces** el sistema muestra exactamente 3 destinos (Inicio, Historial, Etiquetas), sin una pestaña separada de "Métricas" (esa pantalla vive dentro de Inicio, ver EP-004)

## Notas técnicas (opcional)

- Navigation Compose, barra inferior con 3 íconos fijos.

## Checklist INVEST

- [x] **I**ndependent — sí: no depende de ninguna otra historia; es infraestructura base que otras historias asumen construida.
- [x] **N**egotiable — el diseño exacto de los íconos/colores es negociable.
- [x] **V**aluable — sí: es la forma en que el usuario accede a cualquier parte visible de la app.
- [x] **E**stimable — alcance acotado, patrón estándar de navegación por pestañas.
- [x] **S**mall — cabe claramente en 2-4 días.
- [x] **T**estable — 4 escenarios en Given/When/Then con resultados observables (apertura en Inicio, cambio inmediato, resaltado activo, exactamente 3 destinos).
