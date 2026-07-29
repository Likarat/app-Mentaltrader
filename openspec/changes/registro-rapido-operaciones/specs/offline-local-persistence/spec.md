## MODIFIED Requirements

### Requirement: Persistencia local sobrevive a reinicios completos
El sistema SHALL usar una base de datos local (Room) con las entidades reales `Operation` y `CatalogItem` (spec §3.1), que se inicializa sin error (con las semillas de catálogo correspondientes) y cuyo estado sobrevive a un reinicio completo del proceso de la app, sin pérdida de datos.

#### Scenario: Room se inicializa sin error al arrancar la app
- **WHEN** la app arranca por primera vez
- **THEN** la base de datos Room se inicializa sin errores, con las semillas de catálogo (`XAUUSD`, `Ninguno`, set de Emociones) ya sembradas

#### Scenario: Persistencia entre reinicios completos del proceso
- **WHEN** el usuario registra al menos una operación, cierra completamente el proceso de la app y la vuelve a abrir
- **THEN** el sistema muestra la operación previamente registrada, sin pérdida de datos ni reinicialización de la base de datos
