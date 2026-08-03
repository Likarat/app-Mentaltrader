package com.miguel.mentaltrader.feature.historial

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.miguel.mentaltrader.core.data.OperationImage
import kotlinx.coroutines.launch
import java.io.File

/**
 * HU-019: visor de imagen a pantalla completa, abierto desde `HistorialDetalleScreen` al tocar la
 * imagen de una operación (HU-018). Escenario 1: fondo negro + imagen a tamaño completo (usa
 * [OperationImage.filePath], la imagen completa ya redimensionada por `ImageProcessor` al
 * guardarla -- NO la miniatura cacheada que usan la tarjeta del listado y el detalle, para no
 * perder calidad, tal como pide la historia). Escenario 2: zoom vía pellizcar/ampliar. Escenario
 * 3: 2+ imágenes reales de la misma operación muestran un indicador de puntos y permiten avanzar
 * deslizando o con una flecha. Escenario 4 (design.md decisión #7): única pantalla de la app donde
 * se permite rotar a horizontal -- ver el `DisposableEffect` de abajo, mismo patrón de acceso
 * directo a la `ComponentActivity` que `MainActivity.HighPriorityBackHandler`.
 */
@Composable
fun HistorialVisorImagenScreen(
    viewModel: HistorialVisorImagenViewModel,
    initialImageIndex: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val images by viewModel.images.collectAsState()
    val context = LocalContext.current

    // HU-019 Escenario 4: excepción de orientación -- se fija al entrar y se restaura SIEMPRE al
    // salir (incluso si se sale por el gesto/botón "atrás" del sistema, que no pasa por `onBack`
    // sino que directamente saca este Composable de composición).
    val activity = context as? ComponentActivity
    DisposableEffect(activity) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (images.isEmpty()) {
            // Edge real no explícito en el AC (en producción el visor solo se abre desde una
            // imagen ya visible en el detalle, HU-018 Escenario 3): la última imagen de la
            // operación se borró mientras el visor estaba abierto (p. ej. HU-021 en otra sesión).
            Text(
                "Imagen no disponible",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            val initialPage = HistorialVisorImagenNavigation
                .resolveInitialPage(images.size, initialImageIndex)
                .coerceAtLeast(0)
            val pagerState = rememberPagerState(initialPage = initialPage) { images.size }
            val scope = rememberCoroutineScope()

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                ZoomableOperationImage(
                    imageFile = File(context.filesDir, images[page].filePath),
                    contentDescription = "Imagen ${page + 1} de ${images.size}"
                )
            }

            if (images.size > 1) {
                PageIndicatorDots(
                    pageCount = images.size,
                    currentPage = pagerState.currentPage,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
                )
                if (HistorialVisorImagenNavigation.hasNextPage(pagerState.currentPage, images.size)) {
                    IconButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Siguiente imagen",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Pantalla completa deliberada (MainActivity oculta la barra superior/inferior para esta
        // ruta): sin la flecha "atrás" del TopAppBar, este botón es la única forma visible de
        // salir (además del gesto/botón "atrás" del sistema, que también funciona por defecto).
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
        }
    }
}

/**
 * HU-019 Escenario 2: aplica zoom real sobre la imagen con el gesto de pellizcar/ampliar
 * (`detectTransformGestures`, Compose Foundation puro -- sin dependencia nueva, design.md decisión
 * #7). El paneo (`pan`) solo se acumula mientras hay zoom aplicado (`scale > 1f`); al volver a
 * `scale == 1f` el desplazamiento se resetea, para no dejar la imagen descentrada la próxima vez
 * que se vuelve a hacer zoom.
 */
@Composable
private fun ZoomableOperationImage(imageFile: File, contentDescription: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    AsyncImage(
        model = imageFile,
        contentDescription = contentDescription,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(imageFile) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                    scale = newScale
                    offset = if (newScale <= MIN_ZOOM) Offset.Zero else offset + pan
                }
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            )
    )
}

/** HU-019 Escenario 3: indicador de puntos, la página actual resaltada. */
@Composable
private fun PageIndicatorDots(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(8.dp)
                    .background(
                        color = if (index == currentPage) Color.White else Color.White.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
            )
        }
    }
}

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 5f
