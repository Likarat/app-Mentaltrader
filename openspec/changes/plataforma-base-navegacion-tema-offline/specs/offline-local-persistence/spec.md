## ADDED Requirements

### Requirement: Operación 100% offline sin llamadas de red
El sistema SHALL funcionar completamente sin conexión a internet: ninguna dependencia ni permiso de red SHALL estar presente en el proyecto, y ninguna funcionalidad SHALL requerir conectividad.

#### Scenario: Sin dependencia HTTP ni permiso de red en el proyecto
- **WHEN** se inspecciona el manifiesto de dependencias (`build.gradle.kts`) y el `AndroidManifest.xml`
- **THEN** no existe ninguna dependencia de cliente HTTP ni el permiso `android.permission.INTERNET`

### Requirement: Persistencia local sobrevive a reinicios completos
El sistema SHALL usar una base de datos local (Room) que se inicializa sin error y cuyo estado sobrevive a un reinicio completo del proceso de la app, sin pérdida de datos.

#### Scenario: Room se inicializa sin error al arrancar la app
- **WHEN** la app arranca por primera vez
- **THEN** la base de datos Room se inicializa sin errores

#### Scenario: Persistencia entre reinicios completos del proceso
- **WHEN** el usuario cierra completamente el proceso de la app y la vuelve a abrir
- **THEN** el sistema levanta la misma instancia de base de datos local sin pérdida de datos ni reinicialización

#### Scenario: Verificación completa diferida a datos de negocio reales
- **WHEN** existan entidades de negocio reales (`Operation`, `CatalogItem`, `OperationImage` — EP-001) sobre esta base de datos
- **THEN** la garantía de "sin pérdida de datos entre sesiones" SHALL re-verificarse con esos datos reales, no solo con la base de datos vacía de este slice
