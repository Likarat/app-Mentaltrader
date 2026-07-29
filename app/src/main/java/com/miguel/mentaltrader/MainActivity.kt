package com.miguel.mentaltrader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.miguel.mentaltrader.feature.etiquetas.EtiquetasScreen
import com.miguel.mentaltrader.feature.historial.HistorialScreen
import com.miguel.mentaltrader.feature.metricas.InicioScreen
import com.miguel.mentaltrader.feature.registro.OperationFormScreen
import com.miguel.mentaltrader.feature.registro.OperationFormViewModel
import com.miguel.mentaltrader.ui.theme.MentaltraderTheme

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

@Composable
fun MentaltraderApp(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showFab = currentRoute == Destino.Inicio.route || currentRoute == Destino.Historial.route
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

    Scaffold(
        bottomBar = {
            AppBottomNavigationBar(navController) { destino ->
                requestExit {
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
                FloatingActionButton(onClick = { navController.navigate(RUTA_NUEVA_OPERACION) }) {
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
