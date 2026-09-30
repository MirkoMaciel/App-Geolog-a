package com.example.appgeologia.ui.recorridos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.appgeologia.data.recorrido.Horizonte
import com.example.appgeologia.data.recorrido.Parada
import com.example.appgeologia.data.recorrido.Recorrido
import com.example.appgeologia.data.recorrido.RecorridoRepository
import com.example.appgeologia.data.recorrido.TipoSalida
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Punto de acceso de las pantallas a los recorridos. Sobrevive a la rotación de pantalla
 * y se comparte entre todas las pantallas de la app.
 */
class RecorridosViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = RecorridoRepository(File(app.filesDir, "recorridos.json"))

    val recorridos: StateFlow<List<Recorrido>> = repo.recorridos

    fun buscarParada(recorridoId: Long, paradaId: Long): Parada? = repo.buscarParada(recorridoId, paradaId)

    fun crearRecorrido(nombre: String, tipo: TipoSalida, proyecto: String, descripcion: String): Long =
        repo.crearRecorrido(nombre, tipo, proyecto, descripcion)

    fun eliminarRecorrido(id: Long) = repo.eliminarRecorrido(id)

    fun cambiarEstado(id: Long, finalizado: Boolean) = repo.cambiarEstado(id, finalizado)

    fun crearParada(
        recorridoId: Long,
        latitud: Double? = null,
        longitud: Double? = null,
        altitud: Double? = null,
        precisionMetros: Float? = null
    ): Long = repo.crearParada(recorridoId, latitud, longitud, altitud, precisionMetros)

    fun guardarParada(recorridoId: Long, parada: Parada) = repo.guardarParada(recorridoId, parada)

    fun eliminarParada(recorridoId: Long, paradaId: Long) = repo.eliminarParada(recorridoId, paradaId)

    fun agregarHorizonte(recorridoId: Long, paradaId: Long): Long = repo.agregarHorizonte(recorridoId, paradaId)

    fun guardarHorizonte(recorridoId: Long, paradaId: Long, horizonte: Horizonte) =
        repo.guardarHorizonte(recorridoId, paradaId, horizonte)

    fun eliminarHorizonte(recorridoId: Long, paradaId: Long, horizonteId: Long) =
        repo.eliminarHorizonte(recorridoId, paradaId, horizonteId)
}
