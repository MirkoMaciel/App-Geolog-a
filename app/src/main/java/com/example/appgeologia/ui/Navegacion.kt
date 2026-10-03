package com.example.appgeologia.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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
import com.example.appgeologia.archivos.AlmacenExportaciones
import com.example.appgeologia.ui.exportaciones.ExportacionesScreen
import com.example.appgeologia.ui.inicio.InicioScreen
import com.example.appgeologia.ui.recorridos.DetalleRecorridoScreen
import com.example.appgeologia.ui.recorridos.EditarItemScreen
import com.example.appgeologia.ui.recorridos.EditarParadaScreen
import com.example.appgeologia.ui.recorridos.ListaRecorridosScreen
import com.example.appgeologia.ui.recorridos.RecorridosViewModel
import kotlinx.serialization.Serializable

// ----- Rutas (cada pantalla de la app) -----
@Serializable object RutaInicio
@Serializable object RutaRecorridos
@Serializable object RutaExportaciones
@Serializable data class RutaRecorrido(val recorridoId: Long)
@Serializable data class RutaParada(val recorridoId: Long, val paradaId: Long)
@Serializable data class RutaItem(val recorridoId: Long, val paradaId: Long, val itemId: Long)

@Composable
fun AppGeologia() {
    val navController = rememberNavController()
    // Un solo ViewModel compartido por todas las pantallas.
    val viewModel: RecorridosViewModel = viewModel()
    val recorridos by viewModel.recorridos.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val exportaciones = remember { AlmacenExportaciones(context) }

    val barraInferior: @Composable () -> Unit = { BarraInferior(navController) }

    NavHost(navController = navController, startDestination = RutaInicio) {

        composable<RutaInicio> {
            InicioScreen(
                recorridos = recorridos,
                cantidadExportaciones = remember { exportaciones.listar().size },
                onCrearRecorrido = { nombre, tipo, proyecto, descripcion ->
                    val id = viewModel.crearRecorrido(nombre, tipo, proyecto, descripcion)
                    navController.navigate(RutaRecorrido(id))
                },
                onAbrirRecorrido = { navController.navigate(RutaRecorrido(it)) },
                onIrARecorridos = { navController.irAPestana(RutaRecorridos) },
                onIrAExportaciones = { navController.irAPestana(RutaExportaciones) },
                barraInferior = barraInferior
            )
        }

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

        composable<RutaExportaciones> {
            ExportacionesScreen(barraInferior = barraInferior)
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
                    onEditarItem = { navController.navigate(RutaItem(recorrido.id, parada.id, it)) },
                    onVolver = { navController.popBackStack() }
                )
            }
        }

        composable<RutaItem> { entrada ->
            val ruta = entrada.toRoute<RutaItem>()
            val parada = viewModel.buscarParada(ruta.recorridoId, ruta.paradaId)
            val item = parada?.items?.find { it.id == ruta.itemId }
            if (parada == null || item == null) {
                VolverSiNoExiste(navController)
            } else {
                EditarItemScreen(
                    itemGuardado = item,
                    numeroParada = parada.numero,
                    almacenFotos = viewModel.fotos,
                    onGuardar = { viewModel.guardarItem(ruta.recorridoId, ruta.paradaId, it) },
                    onVolver = { navController.popBackStack() }
                )
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

    NavigationBar {
        NavigationBarItem(
            selected = destino?.hasRoute<RutaInicio>() == true,
            onClick = { navController.irAPestana(RutaInicio) },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("Inicio") }
        )
        NavigationBarItem(
            selected = destino?.hasRoute<RutaRecorridos>() == true,
            onClick = { navController.irAPestana(RutaRecorridos) },
            icon = { Icon(Icons.Filled.Place, contentDescription = null) },
            label = { Text("Recorridos") }
        )
        NavigationBarItem(
            selected = destino?.hasRoute<RutaExportaciones>() == true,
            onClick = { navController.irAPestana(RutaExportaciones) },
            icon = { Icon(Icons.Filled.Share, contentDescription = null) },
            label = { Text("Mis exportaciones") }
        )
    }
}

/** Cambia de pestaña del menú inferior conservando el estado de cada una. */
private fun NavHostController.irAPestana(ruta: Any) = navigate(ruta) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
