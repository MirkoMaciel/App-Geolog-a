package com.example.appgeologia.data.recorrido

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Guarda los recorridos en un archivo JSON dentro del almacenamiento privado de la app,
 * así los datos quedan en el teléfono y todo funciona sin conexión en el campo.
 *
 * Los cambios se aplican primero en memoria ([recorridos]) y luego se escriben al archivo
 * en segundo plano.
 */
class RecorridoRepository(
    private val archivo: File,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val reloj: () -> Long = System::currentTimeMillis
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }
    private val mutexEscritura = Mutex()
    private var ultimoId = 0L

    private val _recorridos = MutableStateFlow(leerArchivo())
    val recorridos: StateFlow<List<Recorrido>> = _recorridos.asStateFlow()

    fun buscarRecorrido(id: Long): Recorrido? = _recorridos.value.find { it.id == id }

    fun buscarParada(recorridoId: Long, paradaId: Long): Parada? =
        buscarRecorrido(recorridoId)?.paradas?.find { it.id == paradaId }

    // ----- Recorridos -----

    fun crearRecorrido(nombre: String, tipo: TipoSalida, proyecto: String, descripcion: String): Long {
        val recorrido = Recorrido(
            id = nuevoId(),
            nombre = nombre.trim(),
            tipoSalida = tipo,
            proyecto = proyecto.trim(),
            descripcion = descripcion.trim(),
            fechaInicio = reloj()
        )
        modificar { it + recorrido }
        return recorrido.id
    }

    fun eliminarRecorrido(id: Long) = modificar { lista -> lista.filterNot { it.id == id } }

    fun cambiarEstado(id: Long, finalizado: Boolean) =
        modificarRecorrido(id) { it.copy(finalizado = finalizado) }

    // ----- Paradas -----

    fun crearParada(
        recorridoId: Long,
        latitud: Double? = null,
        longitud: Double? = null,
        altitud: Double? = null,
        precisionMetros: Float? = null
    ): Long {
        val id = nuevoId()
        modificarRecorrido(recorridoId) { recorrido ->
            val parada = Parada(
                id = id,
                numero = (recorrido.paradas.maxOfOrNull { it.numero } ?: 0) + 1,
                fecha = reloj(),
                latitud = latitud,
                longitud = longitud,
                altitud = altitud,
                precisionMetros = precisionMetros,
                // El proyecto del recorrido se copia como valor inicial de la planilla.
                sitio = DatosSitio(proyecto = recorrido.proyecto)
            )
            recorrido.copy(paradas = recorrido.paradas + parada)
        }
        return id
    }

    /**
     * Guarda los datos de la parada (ubicación, sitio, observaciones).
     * Los horizontes NO se toman de [parada]: se editan con sus propias funciones para
     * que dos pantallas abiertas a la vez no se pisen los cambios.
     */
    fun guardarParada(recorridoId: Long, parada: Parada) =
        modificarParada(recorridoId, parada.id) { actual ->
            parada.copy(horizontes = actual.horizontes)
        }

    fun eliminarParada(recorridoId: Long, paradaId: Long) =
        modificarRecorrido(recorridoId) { r -> r.copy(paradas = r.paradas.filterNot { it.id == paradaId }) }

    // ----- Horizontes -----

    fun agregarHorizonte(recorridoId: Long, paradaId: Long): Long {
        val id = nuevoId()
        modificarParada(recorridoId, paradaId) { p ->
            p.copy(horizontes = p.horizontes + Horizonte(id = id))
        }
        return id
    }

    fun guardarHorizonte(recorridoId: Long, paradaId: Long, horizonte: Horizonte) =
        modificarParada(recorridoId, paradaId) { p ->
            p.copy(horizontes = p.horizontes.map { if (it.id == horizonte.id) horizonte else it })
        }

    fun eliminarHorizonte(recorridoId: Long, paradaId: Long, horizonteId: Long) =
        modificarParada(recorridoId, paradaId) { p ->
            p.copy(horizontes = p.horizontes.filterNot { it.id == horizonteId })
        }

    // ----- Internos -----

    private fun modificarRecorrido(id: Long, cambio: (Recorrido) -> Recorrido) =
        modificar { lista -> lista.map { if (it.id == id) cambio(it) else it } }

    private fun modificarParada(recorridoId: Long, paradaId: Long, cambio: (Parada) -> Parada) =
        modificarRecorrido(recorridoId) { r ->
            r.copy(paradas = r.paradas.map { if (it.id == paradaId) cambio(it) else it })
        }

    private fun modificar(cambio: (List<Recorrido>) -> List<Recorrido>) {
        _recorridos.update(cambio)
        scope.launch { escribirArchivo() }
    }

    /** Ids únicos y crecientes aunque se creen varios elementos en el mismo milisegundo. */
    @Synchronized
    private fun nuevoId(): Long {
        ultimoId = maxOf(reloj(), ultimoId + 1)
        return ultimoId
    }

    private fun leerArchivo(): List<Recorrido> {
        if (!archivo.exists()) return emptyList()
        return try {
            json.decodeFromString<List<Recorrido>>(archivo.readText())
        } catch (e: Exception) {
            // Si el archivo está dañado se guarda una copia para no perder los datos.
            archivo.copyTo(File(archivo.parentFile, "${archivo.name}.danado"), overwrite = true)
            emptyList()
        }
    }

    /** Escribe en un archivo temporal y lo renombra, para no dejar el JSON a medio escribir. */
    private suspend fun escribirArchivo() = mutexEscritura.withLock {
        val contenido = json.encodeToString(_recorridos.value)
        val temporal = File(archivo.parentFile, "${archivo.name}.tmp")
        temporal.writeText(contenido)
        if (!temporal.renameTo(archivo)) {
            archivo.delete()
            temporal.renameTo(archivo)
        }
    }
}
