## Purpose

Garantía de operación 100% offline y de persistencia local (Room) sin pérdida de datos. En este primer slice (EP-005) solo se implementa y verifica la ausencia de red; la persistencia real queda declarada pero diferida hasta que EP-001 aporte entidades de dominio reales.

## Requirements

### Requirement: Operación 100% offline sin llamadas de red
El sistema SHALL funcionar completamente sin conexión a internet: ninguna dependencia ni permiso de red SHALL estar presente en el proyecto, y ninguna funcionalidad SHALL requerir conectividad.

#### Scenario: Sin dependencia HTTP ni permiso de red en el proyecto
- **WHEN** se inspecciona el manifiesto de dependencias (`build.gradle.kts`) y el `AndroidManifest.xml` (en tiempo de ejecución, vía `PackageManager`)
- **THEN** no existe ninguna dependencia de cliente HTTP ni el permiso `android.permission.INTERNET`

### Requirement: Persistencia local sobrevive a reinicios completos
El sistema SHALL usar una base de datos local (Room) que se inicializa sin error y cuyo estado sobrevive a un reinicio completo del proceso de la app, sin pérdida de datos.

> **Nota de alcance (EP-005)**: Room exige al menos una entidad (`@Database` no admite `entities = []`). Las entidades reales (`Operation`, `CatalogItem`, `OperationImage`) pertenecen a EP-001. Esta Requirement queda declarada pero no implementada en EP-005 — la implementación real se entrega en el change de EP-001, que deberá incluir un delta `MODIFIED Requirements` aquí marcando estos escenarios como satisfechos.

#### Scenario: Room se inicializa sin error al arrancar la app
- **WHEN** la app arranca por primera vez (con al menos una entidad real ya definida, ver nota de alcance)
- **THEN** la base de datos Room se inicializa sin errores

#### Scenario: Persistencia entre reinicios completos del proceso
- **WHEN** el usuario cierra completamente el proceso de la app y la vuelve a abrir (con datos reales ya persistidos)
- **THEN** el sistema levanta la misma instancia de base de datos local sin pérdida de datos ni reinicialización
