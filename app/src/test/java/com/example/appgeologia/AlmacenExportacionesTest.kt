package com.example.appgeologia

import com.example.appgeologia.archivos.AlmacenExportaciones
import com.example.appgeologia.archivos.DatosExportacion
import com.example.appgeologia.data.recorrido.Parada
import com.example.appgeologia.data.recorrido.Recorrido
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AlmacenExportacionesTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private val parada2 = Parada(id = 20, numero = 2, fecha = 0)
    private val salida = Recorrido(id = 1, nombre = "Cerro Chenque", fechaInicio = 0, paradas = listOf(Parada(id = 10, numero = 1, fecha = 0), parada2))

    private fun almacen() = AlmacenExportaciones(carpeta.newFolder("exportaciones"))

    private fun exportarParada(a: AlmacenExportaciones, texto: String, copia: Boolean = false): File {
        val (datos, nombre) = AlmacenExportaciones.datosParada(salida, parada2)
        return a.exportar(datos, nombre, copia) { it.writeText(texto) }
    }

    @Test
    fun reexportarReemplazaElMismoArchivo() {
        val a = almacen()
        val primero = exportarParada(a, "versión 1")
        val segundo = exportarParada(a, "versión 2")

        assertEquals(primero.canonicalPath, segundo.canonicalPath)
        assertEquals("Parada_2_Cerro_Chenque.pdf", segundo.name)
        assertEquals("versión 2", segundo.readText())
        assertEquals(1, a.listar().size)
        assertEquals(segundo.canonicalPath, a.vigente(1, 20)!!.archivo.canonicalPath)
    }

    @Test
    fun laCopiaNoReemplazaAlVigente() {
        val a = almacen()
        val vigente = exportarParada(a, "original")
        val copia = exportarParada(a, "copia", copia = true)

        assertEquals(2, a.listar().size)
        assertTrue(copia.name.contains("_copia_"))
        assertEquals(vigente.canonicalPath, a.vigente(1, 20)!!.archivo.canonicalPath)
        assertEquals("original", vigente.readText())
    }

    @Test
    fun elPdfDeLaSalidaYElDeUnaParadaSonDistintos() {
        val a = almacen()
        exportarParada(a, "parada")
        val (datos, nombre) = AlmacenExportaciones.datosSalida(salida)
        a.exportar(datos, nombre, copia = false) { it.writeText("salida") }

        assertEquals(2, a.listar().size)
        assertEquals("salida", a.vigente(1, null)!!.archivo.readText())
        assertEquals("parada", a.vigente(1, 20)!!.archivo.readText())
    }

    @Test
    fun siFallaLaGeneracionElAnteriorQuedaIntacto() {
        val a = almacen()
        val vigente = exportarParada(a, "bueno")
        val (datos, nombre) = AlmacenExportaciones.datosParada(salida, parada2)
        runCatching { a.exportar(datos, nombre, copia = false) { error("falló el PDF") } }

        assertEquals("bueno", vigente.readText())
        assertNotNull(a.vigente(1, 20))
    }

    @Test
    fun laLimpiezaDejaSoloElMasRecienteDeCadaParadaYSalida() {
        val dir = carpeta.newFolder("exportaciones")
        // Exportaciones de la versión anterior: un archivo nuevo con fecha por cada vez.
        fun viejo(nombre: String, edad: Long) = File(dir, nombre).apply {
            writeText(nombre)
            setLastModified(1_000_000_000_000 - edad)
        }
        viejo("Parada_2_Cerro_Chenque_2026-11-01_100000.pdf", 3000)
        viejo("Parada_2_Cerro_Chenque_2026-11-02_100000.pdf", 2000)
        val masNueva = viejo("Parada_2_Cerro_Chenque_2026-11-03_100000.pdf", 1000)
        viejo("Salida_Cerro_Chenque_2026-11-01_100000.pdf", 3000)
        viejo("Salida_Cerro_Chenque_2026-11-02_100000.pdf", 1000)
        val desconocido = viejo("algo_que_no_es_de_la_app.pdf", 500)

        val a = AlmacenExportaciones(dir)
        a.limpiarExportacionesAnteriores(listOf(salida))

        val pdfs = a.listar()
        assertEquals(3, pdfs.size)                          // parada 2, salida completa y el desconocido
        assertTrue(desconocido.exists())                    // lo que no se reconoce no se toca
        val vigenteParada = a.vigente(1, 20)!!
        assertEquals("Parada_2_Cerro_Chenque.pdf", vigenteParada.archivo.name)
        assertEquals(masNueva.name, vigenteParada.archivo.readText())   // se quedó con el más reciente
        assertNotNull(a.vigente(1, null))
        assertFalse(File(dir, masNueva.name).exists())      // se movió a la carpeta de la salida
    }

    @Test
    fun laLimpiezaConservaSinDuplicadosLosDeSalidasBorradas() {
        val dir = carpeta.newFolder("exportaciones")
        File(dir, "indice.json").writeText(
            """{"Parada_1_Vieja_2026-11-01_100000.pdf":{"recorridoId":99,"nombreSalida":"Vieja","contenido":"Parada 1"},
                "Parada_1_Vieja_2026-11-02_100000.pdf":{"recorridoId":99,"nombreSalida":"Vieja","contenido":"Parada 1"}}"""
        )
        File(dir, "Parada_1_Vieja_2026-11-01_100000.pdf").apply { writeText("a"); setLastModified(1_000_000_000_000) }
        File(dir, "Parada_1_Vieja_2026-11-02_100000.pdf").apply { writeText("b"); setLastModified(1_000_000_100_000) }

        val a = AlmacenExportaciones(dir)
        a.limpiarExportacionesAnteriores(listOf(salida))   // la salida 99 ya no existe

        val pdfs = a.listarConDatos()
        assertEquals(1, pdfs.size)
        assertEquals("b", pdfs.single().archivo.readText())
        assertEquals(99L, pdfs.single().datos!!.recorridoId)
    }

    @Test
    fun sinPdfViejosLaLimpiezaNoHaceNada() {
        val a = almacen()
        exportarParada(a, "x")
        a.limpiarExportacionesAnteriores(listOf(salida))
        assertEquals(1, a.listar().size)
        assertNull(a.vigente(1, 10))
    }

    @Test
    fun deduceLaSalidaDelNombreDeArchivosViejos() {
        val datos: DatosExportacion? = AlmacenExportaciones.deducirDelNombre(
            File("Parada_2_Cerro_Chenque_2026-11-03_100000.pdf"), listOf(salida)
        )
        assertEquals(1L, datos!!.recorridoId)
        assertEquals("Parada 2", datos.contenido)
    }
}
