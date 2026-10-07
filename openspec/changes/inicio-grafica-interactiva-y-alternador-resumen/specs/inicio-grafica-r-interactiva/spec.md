## ADDED Requirements

### Requirement: Exploración táctil de la gráfica de R acumulado
El sistema SHALL permitir al usuario tocar y arrastrar el dedo sobre la gráfica de R acumulado de
Inicio (cuando tiene al menos 2 puntos de datos) y SHALL mostrar en tiempo real un indicador visual
(línea vertical + tooltip) sobre el punto de datos más cercano a la posición horizontal del dedo,
con su fecha y su valor de R acumulado formateado con signo (mismo formato que
`InicioFormatting.signedR`).

#### Scenario: Arrastre muestra fecha y R del punto más cercano
- **WHEN** el usuario presiona sobre la gráfica (con al menos 2 puntos visibles) y arrastra el dedo
  horizontalmente
- **THEN** el sistema muestra un indicador visual sobre el punto más cercano a la posición del
  dedo, con su fecha y su R acumulado con signo

#### Scenario: El indicador sigue el arrastre de punto en punto
- **WHEN** el usuario, con el indicador ya visible sobre un punto, mueve el dedo a otra posición
  horizontal dentro de la gráfica
- **THEN** el indicador se actualiza al nuevo punto más cercano sin saltos ni parpadeos
  perceptibles, mostrando la fecha y el R de ese nuevo punto

#### Scenario: Soltar el dedo fija el último punto tocado
- **WHEN** el usuario levanta el dedo de la pantalla mientras el indicador está visible sobre un
  punto
- **THEN** el indicador permanece visible fijo en ese último punto tocado, hasta que el usuario
  toque en cualquier lugar DENTRO de la gráfica (sin arrastrar) o inicie un nuevo arrastre

#### Scenario: Arrastre fuera de los límites horizontales de la gráfica
- **WHEN** el dedo, durante el arrastre, sale del área horizontal donde hay puntos dibujados (antes
  del primero o después del último)
- **THEN** el indicador se mantiene fijo (clamped) en el primer o último punto de la serie según
  corresponda, sin mostrar datos fuera de rango ni desaparecer

#### Scenario: Serie con exactamente 2 puntos
- **WHEN** el usuario arrastra el dedo sobre una gráfica con exactamente 2 puntos (mínimo para
  dibujarse)
- **THEN** el indicador alterna correctamente entre esos 2 puntos según la posición del dedo, sin
  errores
