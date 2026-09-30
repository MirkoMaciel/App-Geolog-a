package com.example.appgeologia.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.appgeologia.data.CatalogoRocas
import com.example.appgeologia.ui.recorridos.DetalleRecorridoScreen
import com.example.appgeologia.ui.recorridos.EditarHorizonteScreen
import com.example.appgeologia.ui.recorridos.EditarParadaScreen
import com.example.appgeologia.ui.recorridos.ListaRecorridosScreen
import com.example.appgeologia.ui.recorridos.RecorridosViewModel
import kotlinx.serialization.Serializable

// ----- Rutas (cada pantalla de la app) -----
@Serializable object RutaRecorridos
@Serializable data class RutaRecorrido(val recorridoId: Long)
@Serializable data class RutaParada(val recorridoId: Long, val paradaId: Long)
@Serializable data class RutaHorizonte(val recorridoId: Long, val paradaId: Long, val horizonteId: Long)
@Serializable object RutaRocas
@Serializable data class RutaRoca(val rocaId: Int)

@Composable
fun AppGeologia() {
    val navController = rememberNavController()
    // Un solo ViewModel compartido por todas las pantallas.
    val viewModel: RecorridosViewModel = viewModel()
    val recorridos by viewModel.recorridos.collectAsStateWithLifecycle()

    val barraInferior: @Composable () -> Unit = { BarraInferior(navController) }

    NavHost(navController = navController, startDestination = RutaRecorridos) {

        composable<RutaRecorridos> {
            ListaRecorridosScreen(
                recorridos = recorridos,
                onAbrirRecorrido = { navController.navigate(RutaRecorrido(it)) },
                onCrearRecorrido = { nombre, tipo, proyecto, descripcion ->
                    val id = viewModel.crearRecorrido(nombre, tipo, proyecto, descripcion)
                    navController.navigate(RutaRecorrido(id))
                },
                barraInferior = barraInferior
            )
        }

        composable<RutaRecorrido> { entrada ->
            val ruta = entrada.toRoute<RutaRecorrido>()
            val recorrido = recorridos.find { it.id == ruta.recorridoId }
            if (recorrido == null) {
                VolverSiNoExiste(navController)
            } else {
                DetalleRecorridoScreen(
                    recorrido = recorrido,
                    viewModel = viewModel,
                    onAbrirParada = { navController.navigate(RutaParada(recorrido.id, it)) },
                    onVolver = { navController.popBackStack() }
                )
            }
        }

        composable<RutaParada> { entrada ->
            val ruta = entrada.toRoute<RutaParada>()
            val recorrido = recorridos.find { it.id == ruta.recorridoId }
            val parada = recorrido?.paradas?.find { it.id == ruta.paradaId }
            if (recorrido == null || parada == null) {
                VolverSiNoExiste(navController)
            } else {
                EditarParadaScreen(
                    recorrido = recorrido,
                    paradaGuardada = parada,
                    viewModel = viewModel,
                    onEditarHorizonte = { navController.navigate(RutaHorizonte(recorrido.id, parada.id, it)) },
                    onVolver = { navController.popBackStack() }
                )
            }
        }

        composable<RutaHorizonte> { entrada ->
            val ruta = entrada.toRoute<RutaHorizonte>()
            val parada = viewModel.buscarParada(ruta.recorridoId, ruta.paradaId)
            val horizonte = parada?.horizontes?.find { it.id == ruta.horizonteId }
            if (parada == null || horizonte == null) {
                VolverSiNoExiste(navController)
            } else {
                EditarHorizonteScreen(
                    horizonteGuardado = horizonte,
                    numeroParada = parada.numero,
                    onGuardar = { viewModel.guardarHorizonte(ruta.recorridoId, ruta.paradaId, it) },
                    onVolver = { navController.popBackStack() }
                )
            }
        }

        composable<RutaRocas> {
            ListaRocasScreen(
                onRocaClick = { navController.navigate(RutaRoca(it.id)) },
                barraInferior = barraInferior
            )
        }

        composable<RutaRoca> { entrada ->
            val roca = CatalogoRocas.buscarPorId(entrada.toRoute<RutaRoca>().rocaId)
            if (roca == null) {
                VolverSiNoExiste(navController)
            } else {
                DetalleRocaScreen(roca = roca, onVolver = { navController.popBackStack() })
            }
        }
    }
}

/** Si lo que se quería abrir fue eliminado, vuelve a la pantalla anterior. */
@Composable
private fun VolverSiNoExiste(navController: NavHostController) {
    LaunchedEffect(Unit) { navController.popBackStack() }
}

@Composable
private fun BarraInferior(navController: NavHostController) {
    val destino = navController.currentBackStackEntryAsState().value?.destination

    fun irA(ruta: Any) {
        navController.navigate(ruta) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavigationBar {
        NavigationBarItem(
            selected = destino?.hasRoute<RutaRecorridos>() == true,
            onClick = { irA(RutaRecorridos) },
            icon = { Icon(Icons.Filled.Place, contentDescription = null) },
            label = { Text("Recorridos") }
        )
        NavigationBarItem(
            selected = destino?.hasRoute<RutaRocas>() == true,
            onClick = { irA(RutaRocas) },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            label = { Text("Rocas") }
        )
    }
}
