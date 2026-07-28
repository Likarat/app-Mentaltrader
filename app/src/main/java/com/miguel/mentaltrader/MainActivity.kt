package com.miguel.mentaltrader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@Composable
fun MentaltraderApp(navController: NavHostController = rememberNavController()) {
    Scaffold(
        bottomBar = { AppBottomNavigationBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destino.Inicio.route,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding)
        ) {
            composable(Destino.Inicio.route) { InicioScreen() }
            composable(Destino.Historial.route) { HistorialScreen() }
            composable(Destino.Etiquetas.route) { EtiquetasScreen() }
        }
    }
}

@Composable
private fun AppBottomNavigationBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        Destino.items.forEach { destino ->
            NavigationBarItem(
                selected = currentRoute == destino.route,
                onClick = {
                    navController.navigate(destino.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destino.icon, contentDescription = destino.label) },
                label = { Text(destino.label) }
            )
        }
    }
}
