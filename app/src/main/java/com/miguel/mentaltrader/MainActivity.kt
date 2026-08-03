package com.miguel.mentaltrader

import android.os.Build
import android.os.Bundle
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.miguel.mentaltrader.core.navigation.Destino
import com.miguel.mentaltrader.feature.ajustes.AjustesScreen
import com.miguel.mentaltrader.feature.ajustes.AjustesViewModel
import com.miguel.mentaltrader.feature.etiquetas.EtiquetasScreen
import com.miguel.mentaltrader.feature.historial.HistorialDetalleScreen
import com.miguel.mentaltrader.feature.historial.HistorialDetalleViewModel
import com.miguel.mentaltrader.feature.etiquetas.EtiquetasViewModel
import com.miguel.mentaltrader.feature.historial.HistorialScreen
import com.miguel.mentaltrader.feature.historial.HistorialViewModel
import com.miguel.mentaltrader.feature.historial.HistorialVisorImagenScreen
import com.miguel.mentaltrader.feature.historial.HistorialVisorImagenViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.miguel.mentaltrader.feature.inicio.InicioScreen
import com.miguel.mentaltrader.feature.inicio.InicioViewModel
import com.miguel.mentaltrader.feature.registro.OperationFormScreen
import com.miguel.mentaltrader.feature.registro.OperationFormViewModel
import com.miguel.mentaltrader.ui.theme.MentaltraderTheme
import com.miguel.mentaltrader.ui.theme.PrimaryActionGradient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MentaltraderTheme {
                MentaltraderApp()
            }
        }
    }
}

const val RUTA_NUEVA_OPERACION = "nueva_operacion"

// HU-018: ruta parametrizada del detalle de una operación, abierta desde una tarjeta real del
// listado de Historial (EP-003-a).
private const val RUTA_DETALLE_OPERACION_BASE = "detalle_operacion"
private const val ARG_OPERATION_ID = "operationId"
const val RUTA_DETALLE_OPERACION = "$RUTA_DETALLE_OPERACION_BASE/{$ARG_OPERATION_ID}"
fun rutaDetalleOperacion(operationId: Long) = "$RUTA_DETALLE_OPERACION_BASE/$operationId"

// HU-020: ruta parametrizada del formulario en modo edición, abierta desde el botón "Editar" de
// la vista de detalle (EP-003-c). Reutiliza OperationFormScreen/OperationFormViewModel (design.md
// decisión #3), no un formulario nuevo.
private const val RUTA_EDITAR_OPERACION_BASE = "editar_operacion"
const val RUTA_EDITAR_OPERACION = "$RUTA_EDITAR_OPERACION_BASE/{$ARG_OPERATION_ID}"
fun rutaEditarOperacion(operationId: Long) = "$RUTA_EDITAR_OPERACION_BASE/$operationId"

// HU-019: ruta parametrizada del visor de imagen a pantalla completa, abierta al tocar la imagen
// en la vista de detalle (EP-003-b/HU-018). Única ruta de la app donde se permite rotar a
// horizontal (design.md decisión #7) -- por eso MainActivity oculta topBar/bottomBar para ella
// (ver `isVisorImagenRoute` más abajo), en vez de reutilizar el Scaffold de las demás pantallas.
private const val RUTA_VISOR_IMAGEN_BASE = "visor_imagen"
private const val ARG_IMAGE_INDEX = "imageIndex"
const val RUTA_VISOR_IMAGEN = "$RUTA_VISOR_IMAGEN_BASE/{$ARG_OPERATION_ID}/{$ARG_IMAGE_INDEX}"
fun rutaVisorImagen(operationId: Long, imageIndex: Int) = "$RUTA_VISOR_IMAGEN_BASE/$operationId/$imageIndex"

const val RUTA_AJUSTES = "ajustes"

/**
 * Bug real encontrado por `FormExitGuardTest` (HU-009-AC2, 2026-07-29): `NavHost` de
 * navigation-compose 2.9.x registra su propio callback de "atrás" predictivo con prioridad
 * `PRIORITY_DEFAULT`. El `BackHandler` estándar de Compose (`androidx.activity.compose`) TAMBIÉN
 * registra con `PRIORITY_DEFAULT` a través del `OnBackPressedDispatcher` de compatibilidad — en
 * ese caso, el orden de registro decide quién gana, y se verificó empíricamente (logging
 * temporal en dispositivo real) que el callback interno de NavHost termina ganando SIEMPRE,
 * sin importar dónde se declare nuestro `BackHandler` (ni a nivel de `MentaltraderApp`, ni
 * anidado dentro del contenido del propio destino). El diálogo de confirmación nunca se
 * disparaba por el gesto/botón "atrás" real del sistema (sí funcionaba, correctamente, al tocar
 * otra pestaña, que no pasa por este dispatcher).
 *
 * Fix real: en API 33+ (`OnBackInvokedCallback` nativo), se registra DIRECTAMENTE contra el
 * `onBackInvokedDispatcher` de la Activity con `PRIORITY_OVERLAY`, que por contrato de la
 * plataforma tiene prioridad sobre cualquier callback `PRIORITY_DEFAULT` (como el de NavHost),
 * sin depender del orden de registro. En API < 33 se usa el `BackHandler` de Compose de siempre
 * (predictive back no existe ahí, por lo que este conflicto de prioridad no aplica).
 */
@Composable
private fun HighPriorityBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val activity = LocalContext.current as? ComponentActivity
    // Nit detectado por wiring-adversarial-verifier: el DisposableEffect de abajo solo se
    // vuelve a ejecutar cuando cambian `enabled`/`activity` (sus keys), NO cuando cambia el
    // lambda `onBack` en sí — y `onBack` sí cambia entre recomposiciones (nueva clausura
    // capturando `currentRoute`/`onBack` frescos cada vez, ver el call-site más abajo). Sin
    // `rememberUpdatedState`, el callback ya registrado en el dispatcher nativo seguiría
    // invocando la clausura VIEJA (con `currentRoute` desactualizado) hasta la próxima vez que
    // cambiaran `enabled`/`activity`. `rememberUpdatedState` asegura que el callback siempre
    // invoque la versión más reciente de `onBack`, sin necesidad de re-registrar el callback en
    // cada recomposición.
    val currentOnBack by rememberUpdatedState(onBack)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && activity != null) {
        DisposableEffect(enabled, activity) {
            val callback = OnBackInvokedCallback { currentOnBack() }
            if (enabled) {
                activity.onBackInvokedDispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                    callback
                )
            }
            onDispose {
                if (enabled) activity.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(callback)
            }
        }
    } else {
        BackHandler(enabled = enabled, onBack = onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MentaltraderApp(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showFab = currentRoute == Destino.Inicio.route || currentRoute == Destino.Historial.route
    val showSettingsAction = Destino.items.any { it.route == currentRoute }
    val application = LocalContext.current.applicationContext as MentaltraderApplication

    // HU-020: la edición reutiliza OperationFormScreen/OperationFormViewModel -- el mismo
    // guard de "salir sin guardar" (HU-009) y la misma barra superior con flecha "atrás" que ya
    // aplican a "Nueva operación" deben aplicar también a esta ruta parametrizada.
    val isFormRoute = currentRoute == RUTA_NUEVA_OPERACION || currentRoute?.startsWith(RUTA_EDITAR_OPERACION_BASE) == true

    // HU-019 Escenario 1/4: el visor de imagen es pantalla completa de verdad (fondo negro, sin
    // barra superior/inferior) -- también es la única ruta donde se permite rotar a horizontal,
    // y una bottomBar visible en landscape no tendría sentido en ese layout. Su propio botón
    // "Cerrar" (HistorialVisorImagenScreen) reemplaza a la flecha "atrás" del TopAppBar aquí.
    val isVisorImagenRoute = currentRoute?.startsWith(RUTA_VISOR_IMAGEN_BASE) == true

    // HU-021: HistorialViewModel se hoistea al nivel de la app (no al de la ruta "Historial" del
    // NavHost) porque el estado de "deshacer" (pendingDeleteIds + el snackbar) debe sobrevivir a
    // navegar al detalle de una operación y volver -- es la misma instancia que usan tanto el
    // listado como el botón "Eliminar" del detalle.
    val historialViewModel: HistorialViewModel = viewModel(
        factory = HistorialViewModel.Factory(
            application.database.operationDao(),
            application.database.operationImageDao(),
            application.database.catalogItemDao(),
            application.historialFilterRepository,
            application
        )
    )
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(historialViewModel) {
        historialViewModel.deleteRequested.collect { operationId ->
            // HU-021 Escenario 1/2: duration Short (~4s) queda deliberadamente por debajo de
            // UNDO_WINDOW_MS (5s) para que "Deshacer" siempre sea válido mientras el snackbar
            // esté visible (ver HistorialViewModel.onUndoDelete).
            val result = snackbarHostState.showSnackbar(
                message = "Operación eliminada",
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                historialViewModel.onUndoDelete(operationId)
            }
        }
    }

    // HU-009: el formulario "sucio" (isDirty) intercepta tanto la navegación por pestañas como
    // el botón/gesto "atrás" del sistema con un diálogo de confirmación.
    var formDirty by remember { mutableStateOf(false) }
    var pendingExitAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun requestExit(action: () -> Unit) {
        if (isFormRoute && formDirty) {
            pendingExitAction = action
        } else {
            action()
        }
    }

    // Título del TopAppBar: NO repetir la etiqueta del tab activo (ya se muestra en la barra
    // inferior) — bug real encontrado en device: onNodeWithText("Inicio") pasó a matchear 2
    // nodos (título + tab) y rompió AppNavigationTest. Solo las pantallas fuera de las 3
    // pestañas fijas (Ajustes, Nueva operación) necesitan su propio título.
    val topBarTitle = when {
        currentRoute == RUTA_NUEVA_OPERACION -> "Nueva operación"
        currentRoute?.startsWith(RUTA_EDITAR_OPERACION_BASE) == true -> "Editar operación"
        currentRoute == RUTA_AJUSTES -> "Ajustes"
        else -> "Mentaltrader"
    }

    // Bugs reales reportados por el usuario (2026-07-29): en Ajustes y en Nueva operación no
    // había ninguna forma VISIBLE de volver (solo el gesto/botón "atrás" del sistema, poco
    // descubrible) — se sentía "trabado". Se agrega una flecha "atrás" explícita en el
    // TopAppBar para ambas pantallas, usando el mismo requestExit (respeta el diálogo de
    // confirmación de HU-009 cuando corresponde).
    val showBackAction = isFormRoute || currentRoute == RUTA_AJUSTES

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!isVisorImagenRoute) {
                TopAppBar(
                    title = { Text(topBarTitle) },
                    navigationIcon = {
                        if (showBackAction) {
                            IconButton(onClick = { requestExit { navController.popBackStack() } }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                            }
                        }
                    },
                    actions = {
                        if (showSettingsAction) {
                            IconButton(onClick = {
                                navController.navigate(RUTA_AJUSTES) { launchSingleTop = true }
                            }) {
                                Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!isVisorImagenRoute) {
                AppBottomNavigationBar(navController) { destino ->
                    requestExit {
                        // Bug real reportado por el usuario: Nueva operación y Ajustes se pushean
                        // ENCIMA de la pestaña activa (no son pestañas en sí). Si se navega a otra
                        // pestaña con popUpTo(start){saveState=true} mientras alguna de las dos está
                        // arriba de la pila, Navigation-Compose la guarda como si fuera parte del
                        // estado restaurable de la pestaña de origen, y la vuelve a mostrar más tarde
                        // al re-visitar esa pestaña ("la operación/el ajuste seguía ahí"). Por eso
                        // primero se la saca de la pila con un popBackStack limpio (sin saveState),
                        // y RECIÉN DESPUÉS se hace el cambio de pestaña estándar.
                        if (isFormRoute || currentRoute == RUTA_AJUSTES) {
                            navController.popBackStack()
                        }
                        navController.navigate(destino.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = { navController.navigate(RUTA_NUEVA_OPERACION) },
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    modifier = androidx.compose.ui.Modifier.background(PrimaryActionGradient, CircleShape)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Nueva operación")
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destino.Inicio.route,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding)
        ) {
            composable(Destino.Inicio.route) {
                val inicioViewModel: InicioViewModel = viewModel(
                    factory = InicioViewModel.Factory(application.database.operationDao())
                )
                InicioScreen(viewModel = inicioViewModel)
            }
            composable(Destino.Historial.route) {
                HistorialScreen(
                    viewModel = historialViewModel,
                    onOperationClick = { operationId ->
                        navController.navigate(rutaDetalleOperacion(operationId))
                    },
                    // HU-025 Escenario 3: mismo destino real que el FAB "Nueva operación" (HU-008,
                    // EP-001 ya archivada), sin duplicar esa navegación (INT-estado-vacio-fab).
                    onNewOperationClick = { navController.navigate(RUTA_NUEVA_OPERACION) }
                )
            }
            composable(
                RUTA_DETALLE_OPERACION,
                arguments = listOf(navArgument("operationId") { type = NavType.LongType })
            ) { backStackEntry ->
                val operationId = backStackEntry.arguments?.getLong("operationId") ?: 0L
                val detalleViewModel: HistorialDetalleViewModel = viewModel(
                    key = "detalle-$operationId",
                    factory = HistorialDetalleViewModel.Factory(
                        operationId,
                        application.database.operationDao(),
                        application.database.operationImageDao(),
                        application.database.catalogItemDao()
                    )
                )
                HistorialDetalleScreen(
                    viewModel = detalleViewModel,
                    // HU-020: navega al mismo formulario de registro, en modo edición.
                    onEdit = { navController.navigate(rutaEditarOperacion(operationId)) },
                    // HU-021 Escenario 1: soft-delete en memoria (HistorialViewModel, hoisteado a
                    // nivel de app) + vuelve al listado, donde se muestra el snackbar real.
                    onDelete = {
                        historialViewModel.onRequestDelete(operationId)
                        navController.popBackStack()
                    },
                    // HU-019 Escenario 1: abre el visor a pantalla completa con esta operación
                    // real y el índice de la imagen tocada.
                    onOpenImage = { imageIndex ->
                        navController.navigate(rutaVisorImagen(operationId, imageIndex))
                    }
                )
            }
            composable(
                RUTA_VISOR_IMAGEN,
                arguments = listOf(
                    navArgument(ARG_OPERATION_ID) { type = NavType.LongType },
                    navArgument(ARG_IMAGE_INDEX) { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val operationId = backStackEntry.arguments?.getLong(ARG_OPERATION_ID) ?: 0L
                val imageIndex = backStackEntry.arguments?.getInt(ARG_IMAGE_INDEX) ?: 0
                val visorViewModel: HistorialVisorImagenViewModel = viewModel(
                    key = "visor-$operationId",
                    factory = HistorialVisorImagenViewModel.Factory(
                        operationId,
                        application.database.operationImageDao()
                    )
                )
                HistorialVisorImagenScreen(
                    viewModel = visorViewModel,
                    initialImageIndex = imageIndex,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Destino.Etiquetas.route) {
                val etiquetasViewModel: EtiquetasViewModel = viewModel(
                    factory = EtiquetasViewModel.Factory(
                        application.database.catalogItemDao(),
                        application.catalogRepository,
                        application.database.operationImageDao(),
                        application.filesDir
                    )
                )
                EtiquetasScreen(viewModel = etiquetasViewModel)
            }
            composable(RUTA_NUEVA_OPERACION) {
                val formViewModel: OperationFormViewModel = viewModel(
                    factory = OperationFormViewModel.Factory(
                        application.database.operationDao(),
                        application.database.catalogItemDao(),
                        application.database.operationImageDao(),
                        application.imageProcessor
                    )
                )
                val dirty by formViewModel.isDirty.collectAsState()
                LaunchedEffect(dirty) { formDirty = dirty }
                DisposableEffect(Unit) { onDispose { formDirty = false } }

                // HU-009-AC2: ver KDoc de HighPriorityBackHandler (arriba) para el detalle del
                // bug real de precedencia de callbacks frente al predictive-back interno de
                // NavHost que esto soluciona.
                HighPriorityBackHandler(enabled = true) {
                    requestExit { navController.popBackStack() }
                }

                OperationFormScreen(
                    viewModel = formViewModel,
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(
                RUTA_EDITAR_OPERACION,
                arguments = listOf(navArgument("operationId") { type = NavType.LongType })
            ) { backStackEntry ->
                val operationId = backStackEntry.arguments?.getLong("operationId") ?: 0L
                val formViewModel: OperationFormViewModel = viewModel(
                    key = "editar-$operationId",
                    factory = OperationFormViewModel.Factory(
                        application.database.operationDao(),
                        application.database.catalogItemDao(),
                        application.database.operationImageDao(),
                        application.imageProcessor,
                        editingOperationId = operationId
                    )
                )
                val dirty by formViewModel.isDirty.collectAsState()
                LaunchedEffect(dirty) { formDirty = dirty }
                DisposableEffect(Unit) { onDispose { formDirty = false } }

                HighPriorityBackHandler(enabled = true) {
                    requestExit { navController.popBackStack() }
                }

                OperationFormScreen(
                    viewModel = formViewModel,
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(RUTA_AJUSTES) {
                val ajustesViewModel: AjustesViewModel = viewModel(
                    factory = AjustesViewModel.Factory(application.dataResetService)
                )
                AjustesScreen(viewModel = ajustesViewModel)
            }
        }
    }

    if (pendingExitAction != null) {
        AlertDialog(
            onDismissRequest = { pendingExitAction = null },
            title = { Text("¿Salir sin guardar?") },
            text = { Text("Tenés cambios sin guardar en esta operación. Si salís ahora, se perderán.") },
            confirmButton = {
                TextButton(onClick = {
                    val action = pendingExitAction
                    pendingExitAction = null
                    formDirty = false
                    action?.invoke()
                }) { Text("Salir sin guardar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingExitAction = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun AppBottomNavigationBar(navController: NavHostController, onNavigate: (Destino) -> Unit) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        Destino.items.forEach { destino ->
            NavigationBarItem(
                selected = currentRoute == destino.route,
                onClick = { onNavigate(destino) },
                icon = { Icon(destino.icon, contentDescription = destino.label) },
                label = { Text(destino.label) }
            )
        }
    }
}
