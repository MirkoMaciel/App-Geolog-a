package com.example.appgeologia

import com.example.appgeologia.data.recorrido.ItemParada
import com.example.appgeologia.data.recorrido.RecorridoRepository
import com.example.appgeologia.data.recorrido.TipoSalida
import com.example.appgeologia.data.recorrido.tieneDatosSuelo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        val itemId = r1.agregarItem(id, paradaId)
        val h = r1.buscarParada(id, paradaId)!!.items.first()
        r1.guardarItem(id, paradaId, h.copy(nombre = "Bt1", reaccionHcl = "FEf"))

        val r2 = repo(archivo)
        val item = r2.buscarParada(id, paradaId)!!.items.single()
        assertEquals(itemId, item.id)
        assertEquals("Bt1", item.nombre)
        assertEquals("FEf", item.reaccionHcl)
        assertEquals(120.0, r2.buscarParada(id, paradaId)!!.altitud!!, 0.0)
    }

    @Test
    fun guardarParadaNoPisaLosItems() {
        val r = repo()
        val id = r.crearRecorrido("Salida", TipoSalida.SUELOS, "", "")
        val paradaId = r.crearParada(id)
        val vieja = r.buscarParada(id, paradaId)!! // copia sin items

        r.agregarItem(id, paradaId)
        r.guardarParada(id, vieja.copy(sitio = vieja.sitio.copy(calicata = "C-12")))

        val parada = r.buscarParada(id, paradaId)!!
        assertEquals("C-12", parada.sitio.calicata)
        assertEquals(1, parada.items.size)
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
        val h1 = r.agregarItem(id, p)
        val h2 = r.agregarItem(id, p)
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

    @Test
    fun cargaDatosGuardadosConLaVersionAnterior() {
        // En la primera versión los items se llamaban "horizontes" y no tenían título ni fotos.
        val archivo = File(carpeta.root, "viejo.json").apply {
            writeText(
                """[{"id":1,"nombre":"Salida vieja","fechaInicio":0,"paradas":[
                   {"id":2,"numero":1,"fecha":0,"horizontes":[{"id":3,"nombre":"A","textura":"Franco"}]}]}]"""
            )
        }
        val item = repo(archivo).buscarParada(1, 2)!!.items.single()
        assertEquals("A", item.nombre)
        assertEquals("Franco", item.textura)
        assertTrue(item.fotos.isEmpty())
        assertTrue(item.tieneDatosSuelo())
    }

    @Test
    fun itemSoloConTituloYFotosNoTieneDatosDeSuelo() {
        val item = ItemParada(id = 1, titulo = "Afloramiento", descripcion = "Basalto", fotos = listOf("a.jpg"), fecha = 5)
        assertFalse(item.tieneDatosSuelo())
        assertTrue(item.copy(reaccionHcl = "SEf").tieneDatosSuelo())
    }

    @Test
    fun fotosEnUsoIncluyeTodasLasParadas() {
        val r = repo()
        val id = r.crearRecorrido("Salida", TipoSalida.SUELOS, "", "")
        val p1 = r.crearParada(id)
        val p2 = r.crearParada(id)
        val i1 = r.agregarItem(id, p1)
        val i2 = r.agregarItem(id, p2)
        r.guardarItem(id, p1, ItemParada(id = i1, fotos = listOf("a.jpg", "b.jpg")))
        r.guardarItem(id, p2, ItemParada(id = i2, fotos = listOf("c.jpg")))
        assertEquals(setOf("a.jpg", "b.jpg", "c.jpg"), r.fotosEnUso())

        r.eliminarItem(id, p1, i1)
        assertEquals(setOf("c.jpg"), r.fotosEnUso())
    }

    @Test
    fun laFechaDeModificacionSoloCambiaSiCambiaElContenido() {
        var ahora = 1000L
        val r = RecorridoRepository(File(carpeta.root, "m.json"), CoroutineScope(Dispatchers.Unconfined), reloj = { ahora })
        val id = r.crearRecorrido("Salida", TipoSalida.SUELOS, "", "")
        val p = r.crearParada(id)
        val inicial = r.buscarParada(id, p)!!

        // Guardar sin cambios (como pasa al abrir y cerrar una pantalla) no la cambia.
        ahora = 5000L
        r.guardarParada(id, inicial)
        assertEquals(inicial.modificado, r.buscarParada(id, p)!!.modificado)

        // Un cambio real actualiza la fecha de la parada y de la salida.
        ahora = 9000L
        r.guardarParada(id, inicial.copy(observaciones = "nueva"))
        assertEquals(9000L, r.buscarParada(id, p)!!.modificado)
        assertEquals(9000L, r.buscarRecorrido(id)!!.modificado)

        // Cambiar un horizonte también cuenta como cambio de la parada.
        ahora = 12000L
        val h = r.agregarItem(id, p)
        assertEquals(12000L, r.buscarParada(id, p)!!.modificado)
        ahora = 15000L
        r.guardarItem(id, p, r.buscarParada(id, p)!!.items.first { it.id == h }.copy(textura = "Franco"))
        assertEquals(15000L, r.buscarParada(id, p)!!.modificado)
    }
}
