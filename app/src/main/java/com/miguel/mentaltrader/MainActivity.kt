package com.miguel.mentaltrader

import android.os.Bundle
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
import com.miguel.mentaltrader.feature.historial.HistorialScreen
import com.miguel.mentaltrader.feature.metricas.InicioScreen
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
const val RUTA_AJUSTES = "ajustes"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MentaltraderApp(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showFab = currentRoute == Destino.Inicio.route || currentRoute == Destino.Historial.route
    val showSettingsAction = Destino.items.any { it.route == currentRoute }
    val application = LocalContext.current.applicationContext as MentaltraderApplication

    // HU-009: el formulario "sucio" (isDirty) intercepta tanto la navegación por pestañas como
    // el botón/gesto "atrás" del sistema con un diálogo de confirmación.
    var formDirty by remember { mutableStateOf(false) }
    var pendingExitAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun requestExit(action: () -> Unit) {
        if (currentRoute == RUTA_NUEVA_OPERACION && formDirty) {
            pendingExitAction = action
        } else {
            action()
        }
    }

    BackHandler(enabled = currentRoute == RUTA_NUEVA_OPERACION) {
        requestExit { navController.popBackStack() }
    }

    // Título del TopAppBar: NO repetir la etiqueta del tab activo (ya se muestra en la barra
    // inferior) — bug real encontrado en device: onNodeWithText("Inicio") pasó a matchear 2
    // nodos (título + tab) y rompió AppNavigationTest. Solo las pantallas fuera de las 3
    // pestañas fijas (Ajustes, Nueva operación) necesitan su propio título.
    val topBarTitle = when (currentRoute) {
        RUTA_NUEVA_OPERACION -> "Nueva operación"
        RUTA_AJUSTES -> "Ajustes"
        else -> "Mentaltrader"
    }

    // Bugs reales reportados por el usuario (2026-07-29): en Ajustes y en Nueva operación no
    // había ninguna forma VISIBLE de volver (solo el gesto/botón "atrás" del sistema, poco
    // descubrible) — se sentía "trabado". Se agrega una flecha "atrás" explícita en el
    // TopAppBar para ambas pantallas, usando el mismo requestExit (respeta el diálogo de
    // confirmación de HU-009 cuando corresponde).
    val showBackAction = currentRoute == RUTA_NUEVA_OPERACION || currentRoute == RUTA_AJUSTES

    Scaffold(
        topBar = {
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
        },
        bottomBar = {
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
                    if (currentRoute == RUTA_NUEVA_OPERACION || currentRoute == RUTA_AJUSTES) {
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
            composable(Destino.Inicio.route) { InicioScreen() }
            composable(Destino.Historial.route) {
                HistorialScreen(
                    operationDao = application.database.operationDao(),
                    operationImageDao = application.database.operationImageDao()
                )
            }
            composable(Destino.Etiquetas.route) { EtiquetasScreen() }
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
