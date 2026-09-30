package com.example.appgeologia

import com.example.appgeologia.data.recorrido.RecorridoRepository
import com.example.appgeologia.data.recorrido.TipoSalida
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RecorridoRepositoryTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    // Unconfined: las escrituras al archivo ocurren en el momento, sin esperar.
    private fun repo(archivo: File = File(carpeta.root, "recorridos.json")) =
        RecorridoRepository(archivo, CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun crearRecorridoYParadasNumeradas() {
        val r = repo()
        val id = r.crearRecorrido("Salida Valle", TipoSalida.SUELOS, "Proyecto X", "")
        r.crearParada(id, -45.0, -67.0)
        r.crearParada(id)

        val paradas = r.buscarRecorrido(id)!!.paradas
        assertEquals(listOf(1, 2), paradas.map { it.numero })
        assertEquals(-45.0, paradas[0].latitud!!, 0.0)
        assertNull(paradas[1].latitud)
        // El proyecto del recorrido se copia a la planilla de la parada.
        assertEquals("Proyecto X", paradas[0].sitio.proyecto)
    }

    @Test
    fun losDatosSobrevivenAReabrirLaApp() {
        val archivo = File(carpeta.root, "datos.json")
        val r1 = repo(archivo)
        val id = r1.crearRecorrido("Salida", TipoSalida.SUELOS, "", "")
        val paradaId = r1.crearParada(id, -45.5, -67.5, 120.0)
        val horizonteId = r1.agregarHorizonte(id, paradaId)
        val h = r1.buscarParada(id, paradaId)!!.horizontes.first()
        r1.guardarHorizonte(id, paradaId, h.copy(nombre = "Bt1", reaccionHcl = "FEf"))

        val r2 = repo(archivo)
        val horizonte = r2.buscarParada(id, paradaId)!!.horizontes.single()
        assertEquals(horizonteId, horizonte.id)
        assertEquals("Bt1", horizonte.nombre)
        assertEquals("FEf", horizonte.reaccionHcl)
        assertEquals(120.0, r2.buscarParada(id, paradaId)!!.altitud!!, 0.0)
    }

    @Test
    fun guardarParadaNoPisaLosHorizontes() {
        val r = repo()
        val id = r.crearRecorrido("Salida", TipoSalida.SUELOS, "", "")
        val paradaId = r.crearParada(id)
        val vieja = r.buscarParada(id, paradaId)!! // copia sin horizontes

        r.agregarHorizonte(id, paradaId)
        r.guardarParada(id, vieja.copy(sitio = vieja.sitio.copy(calicata = "C-12")))

        val parada = r.buscarParada(id, paradaId)!!
        assertEquals("C-12", parada.sitio.calicata)
        assertEquals(1, parada.horizontes.size)
    }

    @Test
    fun idsUnicosAunqueSeCreenEnElMismoMilisegundo() {
        val r = RecorridoRepository(
            File(carpeta.root, "r.json"),
            CoroutineScope(Dispatchers.Unconfined),
            reloj = { 1000L }
        )
        val id = r.crearRecorrido("Salida", TipoSalida.SUELOS, "", "")
        val p = r.crearParada(id)
        val h1 = r.agregarHorizonte(id, p)
        val h2 = r.agregarHorizonte(id, p)
        assertEquals(4, setOf(id, p, h1, h2).size)
        assertNotEquals(h1, h2)
    }

    @Test
    fun eliminarParadaYRecorrido() {
        val r = repo()
        val id = r.crearRecorrido("Salida", TipoSalida.SUELOS, "", "")
        val p = r.crearParada(id)
        r.eliminarParada(id, p)
        assertTrue(r.buscarRecorrido(id)!!.paradas.isEmpty())
        r.eliminarRecorrido(id)
        assertTrue(r.recorridos.value.isEmpty())
    }

    @Test
    fun archivoDanadoNoRompeLaApp() {
        val archivo = File(carpeta.root, "roto.json").apply { writeText("{esto no es json") }
        val r = repo(archivo)
        assertTrue(r.recorridos.value.isEmpty())
        assertTrue(File(carpeta.root, "roto.json.danado").exists())
    }
}
