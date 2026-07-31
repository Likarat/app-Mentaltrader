## Purpose

Garantía de operación 100% offline y de persistencia local (Room) sin pérdida de datos. En este primer slice (EP-005) solo se implementa y verifica la ausencia de red; la persistencia real queda declarada pero diferida hasta que EP-001 aporte entidades de dominio reales.
## Requirements
### Requirement: Operación 100% offline sin llamadas de red
El sistema SHALL funcionar completamente sin conexión a internet: ninguna dependencia ni permiso de red SHALL estar presente en el proyecto, y ninguna funcionalidad SHALL requerir conectividad.

#### Scenario: Sin dependencia HTTP ni permiso de red en el proyecto
- **WHEN** se inspecciona el manifiesto de dependencias (`build.gradle.kts`) y el `AndroidManifest.xml` (en tiempo de ejecución, vía `PackageManager`)
- **THEN** no existe ninguna dependencia de cliente HTTP ni el permiso `android.permission.INTERNET`

### Requirement: Persistencia local sobrevive a reinicios completos
El sistema SHALL usar una base de datos local (Room) con las entidades reales `Operation` y `CatalogItem` (spec §3.1), que se inicializa sin error (con las semillas de catálogo correspondientes) y cuyo estado sobrevive a un reinicio completo del proceso de la app, sin pérdida de datos.

#### Scenario: Room se inicializa sin error al arrancar la app
- **WHEN** la app arranca por primera vez
- **THEN** la base de datos Room se inicializa sin errores, con las semillas de catálogo (`XAUUSD`, `Ninguno`, set de Emociones) ya sembradas

#### Scenario: Persistencia entre reinicios completos del proceso
- **WHEN** el usuario registra al menos una operación, cierra completamente el proceso de la app y la vuelve a abrir
- **THEN** el sistema muestra la operación previamente registrada, sin pérdida de datos ni reinicialización de la base de datos

