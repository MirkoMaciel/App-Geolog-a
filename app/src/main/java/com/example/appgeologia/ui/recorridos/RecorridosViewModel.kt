package com.example.appgeologia.ui.recorridos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appgeologia.archivos.AlmacenFotos
import com.example.appgeologia.data.recorrido.ItemParada
import com.example.appgeologia.data.recorrido.Parada
import com.example.appgeologia.data.recorrido.Recorrido
import com.example.appgeologia.data.recorrido.RecorridoRepository
import com.example.appgeologia.data.recorrido.TipoSalida
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Punto de acceso de las pantallas a los recorridos. Sobrevive a la rotación de pantalla
 * y se comparte entre todas las pantallas de la app.
 */
class RecorridosViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = RecorridoRepository(File(app.filesDir, "recorridos.json"))
    val fotos = AlmacenFotos(app)

    val recorridos: StateFlow<List<Recorrido>> = repo.recorridos

    init {
        // Fotos que quedaron sueltas (por ejemplo, si la app se cerró en medio de una edición).
        viewModelScope.launch(Dispatchers.IO) { fotos.eliminarNoUsadas(repo.fotosEnUso()) }
    }

    fun buscarParada(recorridoId: Long, paradaId: Long): Parada? = repo.buscarParada(recorridoId, paradaId)

    fun crearRecorrido(nombre: String, tipo: TipoSalida, proyecto: String, descripcion: String): Long =
        repo.crearRecorrido(nombre, tipo, proyecto, descripcion)

    fun eliminarRecorrido(id: Long) {
        val fotosRecorrido = repo.buscarRecorrido(id)?.paradas.orEmpty().flatMap { p -> p.items.flatMap { it.fotos } }
        repo.eliminarRecorrido(id)
        borrarFotos(fotosRecorrido)
    }

    fun cambiarEstado(id: Long, finalizado: Boolean) = repo.cambiarEstado(id, finalizado)

    fun crearParada(
        recorridoId: Long,
        latitud: Double? = null,
        longitud: Double? = null,
        altitud: Double? = null,
        precisionMetros: Float? = null
    ): Long = repo.crearParada(recorridoId, latitud, longitud, altitud, precisionMetros)

    fun guardarParada(recorridoId: Long, parada: Parada) = repo.guardarParada(recorridoId, parada)

    fun eliminarParada(recorridoId: Long, paradaId: Long) {
        val fotosParada = repo.buscarParada(recorridoId, paradaId)?.items.orEmpty().flatMap { it.fotos }
        repo.eliminarParada(recorridoId, paradaId)
        borrarFotos(fotosParada)
    }

    fun agregarItem(recorridoId: Long, paradaId: Long): Long = repo.agregarItem(recorridoId, paradaId)

    fun guardarItem(recorridoId: Long, paradaId: Long, item: ItemParada) =
        repo.guardarItem(recorridoId, paradaId, item)

    fun eliminarItem(recorridoId: Long, paradaId: Long, itemId: Long) {
        val fotosItem = repo.buscarParada(recorridoId, paradaId)?.items?.find { it.id == itemId }?.fotos.orEmpty()
        repo.eliminarItem(recorridoId, paradaId, itemId)
        borrarFotos(fotosItem)
    }

    private fun borrarFotos(nombres: List<String>) {
        if (nombres.isNotEmpty()) viewModelScope.launch(Dispatchers.IO) { fotos.eliminar(nombres) }
    }
}
