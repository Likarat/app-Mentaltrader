## Why

Este es el primer slice de construcción del proyecto. No existe todavía ningún esqueleto navegable de la app — solo el scaffold vacío que genera Android Studio (una `MainActivity` default). Antes de construir cualquier feature de negocio (registro de operaciones, historial, métricas), necesitamos el "esqueleto que camina": la navegación de 3 pestañas, el tema oscuro por defecto, el bloqueo de orientación vertical y la garantía de persistencia 100% local/offline sobre la que todo lo demás se apoya. Sin esto, ninguna épica de negocio (EP-001 a EP-004) tiene dónde vivir.

## What Changes

- Agregar navegación inferior con exactamente 3 destinos (Inicio / Historial / Etiquetas), apertura siempre en Inicio, con el ícono de la pestaña activa resaltado.
- Aplicar un tema oscuro por defecto en toda la app (fondo oscuro, texto claro, acentos de color), sin modo claro alternativo (fuera de alcance en v1).
- Fijar la orientación de pantalla en vertical en toda la app (la única excepción — el visor de imagen a pantalla completa — pertenece a una épica futura, EP-003/HU-019, y no se construye en este slice).
- Confirmar que la app opera 100% offline (sin ninguna llamada de red) y que la persistencia local (Room) sobrevive a reinicios completos del proceso, sin pérdida de datos.

## Capabilities

### New Capabilities
- `app-shell-navigation`: barra de navegación inferior con 3 pestañas fijas (Inicio, Historial, Etiquetas), apertura por defecto en Inicio, resaltado visual del destino activo.
- `dark-theme`: tema visual oscuro aplicado de forma consistente en toda la app, sin alternancia a modo claro.
- `screen-orientation-lock`: orientación fija en vertical en todas las pantallas del shell (Inicio/Historial/Etiquetas), con contrato explícito de excepción reservado para una pantalla futura fuera de este slice.
- `offline-local-persistence`: garantía de funcionamiento sin conexión a internet y de persistencia local (Room) sin pérdida de datos entre sesiones/reinicios.

### Modified Capabilities
(ninguna — no existen specs previas en `openspec/specs/`, es el primer change del proyecto)

## Impact

- **Código nuevo**: estructura de navegación (Navigation Compose) con 3 destinos placeholder (pantallas Inicio/Historial/Etiquetas aún vacías de lógica de negocio, se completan en épicas futuras); tema Compose (`ui/theme/`) con paleta oscura; configuración de orientación (manifest/actividad); capa mínima de persistencia Room (base de datos vacía, sin entidades de negocio todavía — esas llegan con EP-001) para poder verificar la garantía de persistencia/offline de HU-035.
- **Dependencias**: Jetpack Compose Navigation, Room (ya en el stack decidido, ver PRD/spec §2). Ninguna dependencia nueva fuera del stack ya declarado en `.claude/config/stack-allowlist.json`.
- **Historias de usuario cubiertas**: ver bloque `## Trazabilidad` abajo.
- **Sin impacto en APIs externas**: la app no tiene backend (v1 100% local).

## Trazabilidad

- **Épica**: EP-005 — Plataforma base: navegación, tema y almacenamiento offline (`docs/03-backlog/epicas.md`)
- **Historias de usuario cubiertas** (`docs/04-historias/`):
  - HU-032 — Navegar entre Inicio, Historial y Etiquetas
  - HU-033 — Aplicar tema oscuro en toda la app
  - HU-034 — Mantener orientación vertical en el resto de la app
  - HU-035 — Operar 100% offline sin pérdida de datos entre sesiones
