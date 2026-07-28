# INVEST Audit (segunda pasada, escala completa) — 35 historias

**Alcance**: `docs/04-historias/HU-001-*.md` a `HU-035-*.md`

Las 35 historias tienen AC en G/W/T con happy/error/edge y razonamiento explícito; ninguna debe revertirse de `lista` a `draft` por fallar abiertamente un criterio individual. La comparación cruzada del conjunto completo (algo que el análisis historia-por-historia no hace) encontró 5 discrepancias.

## Hallazgos

### 1. 🔴 HU-008 — referencia a ID equivocado en la dependencia declarada (I)

El checklist INVEST de HU-008 dice que depende de "HU-028/EP-005" para la navegación por pestañas y pide declarar el orden "HU-028 → HU-008". **HU-028 es "Filtrar métricas por periodo predefinido" (EP-004)**, sin relación con navegación. La historia real de navegación es **HU-032** ("Navegar entre Inicio, Historial y Etiquetas", EP-005).

**Riesgo**: si se usa esta nota para priorizar, se exigiría una dependencia inexistente (HU-028) mientras la real (HU-032) queda sin declarar.

**Fix**: reemplazar las dos apariciones de "HU-028" por "HU-032" en el checklist de HU-008.

### 2. 🟡 HU-011 — falta escenario para auto-recreación de semillas (T)

Las Notas técnicas afirman que las semillas se "recrean automáticamente si el catálogo queda vacío", pero ningún escenario de AC prueba ese caso (solo el bloqueo de eliminar la semilla protegida directamente).

**Fix**: agregar un 4º escenario Edge: catálogo queda vacío tras eliminaciones sucesivas → el sistema recrea automáticamente la semilla correspondiente.

### 3. 🟡 HU-026 — sigue siendo demasiado grande pese al slicing aplicado al resto del backlog (S)

Combina agregación de 6 métricas + gráfica de línea + gráfica de barras, con librería de gráficas todavía no elegida. Es la única historia M de EP-004 que no pasó por el mismo tratamiento de división que HU-001/006-007/015/022 (todas divididas por combinar mecanismos heterogéneos).

**Fix**: dividir en HU-026a (tarjetas de resumen, con manejo seguro de cero operaciones) y HU-026b (gráficas de línea + barras, donde se resuelve la elección de librería).

### 4. 🟡 HU-035 (Escenario 3) — duplica cobertura de test ya establecida por HU-016 (T/I)

El Escenario 3 de HU-035 ("carga progresiva sin picos de CPU/memoria con cientos de operaciones") es esencialmente el mismo comportamiento que HU-016 ya prueba de forma más exigente (3,000-5,000 operaciones) para Historial, y que las Notas técnicas de HU-026/027 ya garantizan para Inicio. Nunca se compararon entre sí por estar en épicas distintas (EP-003 vs EP-005).

**Fix**: recortar el Escenario 3 de HU-035 para cubrir algo no probado en otro lado (ej. carga de imágenes desde disco sin red), o eliminarlo y dejar una nota "ver HU-016/HU-026 para la garantía de escala".

### 5. 🟢 HU-034 — dependencia cruzada de épica (EP-005→EP-003) no declarada con el mismo rigor que HU-008 (I)

El Escenario 2 de HU-034 (retorno a vertical tras salir del visor de imagen) solo es verificable si HU-019 (EP-003) ya existe. Se menciona en Contexto pero no se traduce en una nota de orden de priorización en el checklist, a diferencia de cómo se manejó el caso análogo en HU-008.

**Fix**: agregar en el checklist I de HU-034: "declarar el orden HU-019 → HU-034 al priorizar".

## Conclusión

Ninguna historia requiere revertirse a `draft`. Las 5 correcciones son puntuales (una referencia de ID incorrecta, dos gaps de test, un solapamiento de AC entre épicas, una nota de orden faltante) y no comprometen la validez general del backlog.
