package com.example.appgeologia

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.appgeologia.archivos.AlmacenExportaciones
import com.example.appgeologia.archivos.AlmacenFotos
import com.example.appgeologia.data.recorrido.ColoresMunsell
import com.example.appgeologia.data.recorrido.DatosSitio
import com.example.appgeologia.data.recorrido.ItemParada
import com.example.appgeologia.data.recorrido.OpcionesSuelo
import com.example.appgeologia.data.recorrido.Parada
import com.example.appgeologia.data.recorrido.Recorrido
import com.example.appgeologia.data.recorrido.RecorridoRepository
import com.example.appgeologia.data.recorrido.TipoSalida
import com.example.appgeologia.data.recorrido.TrianguloTextural
import com.example.appgeologia.data.recorrido.tieneDatosSuelo
import com.example.appgeologia.pdf.PlanillaPdf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.ceil

/**
 * Prueba integral de una salida de campo completa, usando las mismas piezas que la app:
 * repositorio, fotos, colores Munsell, triángulo textural, generador de PDF y
 * exportaciones (vigente, reemplazo, copia y "al día").
 *
 * No toca los datos reales: la salida se guarda en un archivo aparte, las exportaciones en
 * una carpeta temporal y las fotos de prueba se borran al terminar.
 *
 * Resultado (PDFs, vistas PNG de cada página e informe) en
 * /sdcard/Android/data/com.example.appgeologia/files/TestPDF/
 */
@RunWith(AndroidJUnit4::class)
class RecorridoCompletoTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val fotos = AlmacenFotos(context)
    private val fotosDePrueba = mutableListOf<String>()
    private val informe = StringBuilder()

    /** Valores de un horizonte de prueba (todas las casillas de la planilla). */
    private data class Plantilla(
        val nombre: String, val profundidad: String, val limiteDef: String, val limiteTopo: String,
        val seco: String, val humedo: String, val arena: Int, val limo: Int, val arcilla: Int,
        val resistencia: String, val adhesividad: String, val plasticidad: String,
        val estrTipo: String, val estrGrado: String, val estrTamano: String, val hcl: String,
        val concTipo: String, val concTamano: String, val concColor: String, val concCantidad: String, val forma: String,
        val redox: String, val moteados: String, val barnices: String, val humedad: String,
        val raicesCantidad: String, val raicesTamano: String, val raicesUbicacion: String,
        val fragTipo: String, val fragVolumen: String, val fragRedondez: String, val fragTamano: String,
        val descripcion: String
    )

    // Entre todas las plantillas se usan TODAS las opciones fijas de la planilla.
    private val hA = Plantilla(
        "A", "0-12", "C", "Pl", "10YR 5/3", "10YR 3/3", 62, 24, 14,
        "Blando", "Lig adhesivo", "Lig plástico", "Granular", "Déb", "Fino", "NoEf",
        "M", "Fino", "7.5YR 5/6", "Pocas", "Irregular", "No", "No", "No", "Seco",
        "Comunes", "Finas", "Entre agregados", "Gravas basálticas", "5", "Subredondeado", "Grava fina",
        "Horizonte superficial pardo, con raíces finas de la estepa."
    )
    private val hBt = Plantilla(
        "Bt", "12-30", "G", "Ond", "7.5YR 5/4", "7.5YR 4/4", 40, 30, 30,
        "Duro", "Mod adhesivo", "Mod plástico", "Bloques subangulares", "Mod", "Medio", "SEf",
        "N", "Medio", "10YR 8/2", "Comunes", "Esférica", "Escasos rasgos de reducción", "Pocos, finos", "Arcilla, comunes", "Fres.",
        "Pocas", "Muy finas", "Dentro de agregados", "Rodados", "10", "Redondeado", "Grava media",
        "Horizonte de acumulación de arcilla, con barnices visibles en las caras de los agregados."
    )
    private val hBk = Plantilla(
        "Bk", "30-55", "D", "Irr", "10YR 7/3", "10YR 5/4", 45, 35, 20,
        "Muy duro", "No adhesivo", "No plástico", "Bloques angulares", "Fte", "Grueso", "FEf",
        "C", "Grueso", "10YR 8/1", "Abund.", "Hilos", "No", "No", "No", "Seco",
        "Pocas", "Finas", "En grietas", "Rodados y tosca", "25", "Subangular", "Grava gruesa",
        "Acumulación de carbonatos en hilos blancos; fuerte reacción al HCl."
    )
    private val hCkm = Plantilla(
        "Ckm", "55-80", "MA", "Que", "10YR 8/2", "10YR 7/3", 70, 20, 10,
        "Muy duro", "No adhesivo", "No plástico", "Masiva", "SE", "Muy grueso", "VEf",
        "Cr", "Muy fino", "10YR 8/1", "Abund.", "Laminar", "No", "No", "Carbonatos", "Seco",
        "Pocas", "Gruesas", "En grietas", "Tosca", "40", "Angular", "Bloques",
        "Costra calcárea (tosca) laminar y cementada."
    )
    private val h2C = Plantilla(
        "2C", "80-110", "A", "Pl", "7.5YR 6/4", "7.5YR 4/6", 85, 10, 5,
        "Suelto", "No adhesivo", "No plástico", "Grano suelto", "SE", "Muy fino", "MSEf",
        "D", "Fino", "7.5YR 5/8", "Pocas", "Cilíndrica", "Moteados de óxidos", "Comunes, medios", "No", "Húm.",
        "Pocas", "Muy gruesas", "Entre agregados", "Gravas", "15", "Redondeado", "Grava fina",
        "Material arenoso de otro ciclo de depositación (discontinuidad litológica)."
    )
    private val hBw = Plantilla(
        "Bw", "12-35", "C", "Ond", "10YR 6/4", "10YR 4/4", 20, 60, 20,
        "Lig. duro", "Lig adhesivo", "Lig plástico", "Prismática", "Mod", "Grueso", "SEf",
        "M", "Medio", "10YR 5/8", "Comunes", "Dendrítica", "Rasgos redox débiles", "Pocos", "Arcilla, pocos", "Fres.",
        "Comunes", "Medias", "Entre agregados", "Gravas finas", "3", "Subredondeado", "Grava fina",
        "Horizonte cámbico de limos eólicos (loess)."
    )
    private val hBtk = Plantilla(
        "Btk", "35-60", "G", "Irr", "7.5YR 4/3", "7.5YR 3/2", 10, 40, 50,
        "Duro", "Muy adhesivo", "Muy plástico", "Columnar", "Fte", "Muy grueso", "FEf",
        "N", "Grueso", "10YR 8/2", "Abund.", "Reticulada", "Rasgos de reducción", "Abundantes", "Arcilla, abundantes", "Sat.",
        "Abund.", "Finas", "Dentro de agregados", "Rodados", "8", "Redondeado", "Grava media",
        "Horizonte arcilloso saturado en un bajo; nódulos de carbonato y estructura columnar."
    )

    private fun sitio(n: Int) = DatosSitio(
        calicata = "C-0$n", sitioReferencia = "Cerro Chenque, ladera ${listOf("sur", "este", "norte")[n - 1]}",
        proyecto = "Cerro Chenque", geomorfologia = "Pedimento de flanco", posicionGeomorfica = "Ladera media",
        relieve = "Normal", exposicion = listOf("S", "NE", "N")[n - 1], materialOriginario = "Sedimentos eólicos sobre Fm. Patagonia",
        pendiente = "${2 + n * 2} %", vegetacion = "Estepa arbustiva de quilembai y colapiche, con coirón",
        coberturaVegetal = "${30 + n * 5} %", claseDrenaje = "Bien drenado", anegamiento = "No",
        erosion = "Leve eólica", salesAlcalis = "No", usoTierra = "Ganadería extensiva ovina",
        pedregosidad = "Moderada (10 %)", clasificacion = "Haplocalcid típico", temperatura = "${14 + n} °C"
    )

    @Test
    fun salidaCompletaConTodasLasFunciones() {
        val salidaDir = File(context.getExternalFilesDir(null), "TestPDF").apply { deleteRecursively(); mkdirs() }
        val vistas = File(salidaDir, "vistas").apply { mkdirs() }
        val almacen = AlmacenExportaciones(File(context.cacheDir, "test_exportaciones").apply { deleteRecursively() })
        val repo = RecorridoRepository(
            File(context.cacheDir, "test_recorridos.json").apply { delete() },
            CoroutineScope(Dispatchers.Unconfined)
        )
        try {
            // ---------------------------------------------------------------- 1. Salida
            val id = repo.crearRecorrido(
                "Prueba integral Cerro Chenque", TipoSalida.SUELOS, "Cerro Chenque",
                "Salida de prueba que usa todas las funciones de la app para verificar la exportación de PDF."
            )
            linea("Salida: Prueba integral Cerro Chenque (tipo Suelos, proyecto Cerro Chenque)")

            // ---------------------------------------------------------------- 2. Paradas
            val paradas = listOf(
                Triple(Triple(-45.858312, -67.498105, 180.0), listOf(hA, hBt, hBk), true),
                Triple(Triple(-45.856604, -67.493021, 230.0), listOf(hA, hBw, hBtk, hCkm), false),
                Triple(Triple(-45.854917, -67.488944, 268.0), listOf(hA, hBt, hBk, hCkm, h2C), false)
            )
            paradas.forEachIndexed { i, (pos, plantillas, conAfloramiento) ->
                val n = i + 1
                val paradaId = repo.crearParada(id, pos.first, pos.second, pos.third, 4.5f)
                val p = repo.buscarParada(id, paradaId)!!
                repo.guardarParada(id, p.copy(sitio = sitio(n), observaciones = "Perfil $n descripto en la pared norte de la calicata C-0$n."))

                plantillas.forEachIndexed { j, pl ->
                    val itemId = repo.agregarItem(id, paradaId)
                    val item = repo.buscarParada(id, paradaId)!!.items.first { it.id == itemId }
                    repo.guardarItem(id, paradaId, horizonte(item, pl, n, j + 1))
                }
                if (conAfloramiento) {
                    // Item solo con título, descripción y fotos: va a las fichas, no a la tabla.
                    val itemId = repo.agregarItem(id, paradaId)
                    repo.guardarItem(
                        id, paradaId,
                        ItemParada(
                            id = itemId, titulo = "Afloramiento de tosca en el corte del camino",
                            descripcion = "Banco de tosca de 40 cm expuesto a 20 m de la calicata. Se fotografió de cerca y de lejos.",
                            fotos = (1..3).map { foto("p${n}_aflora_$it", "Afloramiento $it", 0xE3DCCB) },
                            fecha = System.currentTimeMillis()
                        )
                    )
                }
                linea("Parada $n: ${plantillas.size} horizontes con planilla (${plantillas.joinToString { it.nombre }})" +
                    if (conAfloramiento) " + 1 observación solo con fotos" else "")
            }

            // ---------------------------------------------------------------- 3. Verificaciones de datos
            val salida = repo.buscarRecorrido(id)!!
            assertEquals(3, salida.paradas.size)
            salida.paradas.flatMap { it.items }.filter { it.tieneDatosSuelo() }.forEach { h ->
                assertNotNull("Color seco ${h.colorSeco}", ColoresMunsell.buscar(h.colorSeco))
                assertNotNull("Color húmedo ${h.colorHumedo}", ColoresMunsell.buscar(h.colorHumedo))
                assertEquals(h.textura, TrianguloTextural.clasificar(h.texturaArena!!, h.texturaLimo!!, h.texturaArcilla!!)!!.nombre)
                assertTrue(OpcionesSuelo.formasConcentracion.any { it.codigo == h.concentracionForma })
            }
            val formasUsadas = salida.paradas.flatMap { it.items }.map { it.concentracionForma }.filter { it.isNotEmpty() }.toSet()
            assertEquals("Se usan las 7 formas", 7, formasUsadas.size)
            linea("Datos: colores Munsell válidos, texturas coherentes con el triángulo, las 7 formas usadas: OK")

            // ---------------------------------------------------------------- 4. Exportar cada parada y la salida
            linea("")
            linea("PDF generados:")
            salida.paradas.forEach { p ->
                val archivo = exportarParada(almacen, salida, p)
                val columnas = p.items.count { it.tieneDatosSuelo() }
                val hojasTabla = maxOf(1, ceil(columnas / 4.0).toInt())
                val paginas = renderizar(archivo, vistas, "parada_${p.numero}")
                // Hojas de la tabla + al menos una hoja de fichas de horizontes con fotos.
                assertTrue("Parada ${p.numero}: $paginas páginas", paginas >= hojasTabla + 1)
                linea(" - ${archivo.name}: $paginas páginas ($columnas columnas en la tabla = $hojasTabla hoja(s) de planilla)")
            }
            val pdfSalida = exportarSalida(almacen, salida)
            val paginasSalida = renderizar(pdfSalida, vistas, "salida")
            linea(" - ${pdfSalida.name}: $paginasSalida páginas (carátula + las 3 paradas)")
            assertEquals(4, almacen.listar().size)

            // ---------------------------------------------------------------- 5. Exportar sin cambios → "al día"
            val p1 = repo.buscarRecorrido(id)!!.paradas.first { it.numero == 1 }
            val vigente1 = almacen.vigente(id, p1.id)!!
            assertTrue("Sin cambios el PDF debe estar al día", almacen.estaAlDia(vigente1, p1.modificado))
            assertTrue(almacen.estaAlDia(almacen.vigente(id, null)!!, repo.buscarRecorrido(id)!!.modificado))
            linea("")
            linea("Exportar de nuevo sin cambios: el PDF está al día y no se genera otro: OK")

            // ---------------------------------------------------------------- 6. Modificar → desactualizado → reemplazar
            Thread.sleep(1100) // para que la modificación sea claramente posterior al PDF
            val p2 = repo.buscarRecorrido(id)!!.paradas.first { it.numero == 2 }
            val anterior2 = almacen.vigente(id, p2.id)!!
            repo.guardarParada(id, p2.copy(observaciones = p2.observaciones + " Se agregó una muestra para laboratorio."))
            val p2Modificada = repo.buscarRecorrido(id)!!.paradas.first { it.numero == 2 }
            assertFalse("Después de modificar debe quedar desactualizado", almacen.estaAlDia(anterior2, p2Modificada.modificado))
            assertFalse(almacen.estaAlDia(almacen.vigente(id, null)!!, repo.buscarRecorrido(id)!!.modificado))
            val reemplazo = exportarParada(almacen, repo.buscarRecorrido(id)!!, p2Modificada)
            assertEquals(anterior2.archivo.canonicalPath, reemplazo.canonicalPath)
            assertTrue(almacen.estaAlDia(almacen.vigente(id, p2.id)!!, p2Modificada.modificado))
            assertEquals("Reemplazar no agrega archivos", 4, almacen.listar().size)
            linea("Modificar la parada 2: queda desactualizada; Reemplazar mantiene un solo PDF: OK")

            // ---------------------------------------------------------------- 7. Guardar como copia
            val (datos1, nombre1) = AlmacenExportaciones.datosParada(salida, p1)
            val copia = almacen.exportar(datos1, nombre1, copia = true) {
                PlanillaPdf(context).generarParada(repo.buscarRecorrido(id)!!, p1, it)
            }
            assertTrue(copia.name.contains("_copia_"))
            assertEquals(vigente1.archivo.canonicalPath, almacen.vigente(id, p1.id)!!.archivo.canonicalPath)
            assertEquals(5, almacen.listar().size)
            linea("Guardar como copia: se crea ${copia.name} y el vigente no cambia: OK")

            // ---------------------------------------------------------------- 8. Mis exportaciones
            val conDatos = almacen.listarConDatos()
            assertTrue(conDatos.all { it.datos?.recorridoId == id })
            assertEquals(1, conDatos.count { it.datos!!.copia })
            linea("Mis exportaciones: los 5 PDF quedan asociados a la salida (4 vigentes + 1 copia): OK")

            // Copia final de los PDF a la carpeta de resultados
            almacen.listar().forEach { it.copyTo(File(salidaDir, it.name), overwrite = true) }
            linea("")
            linea("RESULTADO: todas las verificaciones pasaron.")
        } finally {
            File(salidaDir, "informe.txt").writeText(informe.toString())
            fotos.eliminar(fotosDePrueba)
        }
    }

    // ------------------------------------------------------------------------ ayudantes

    private fun horizonte(base: ItemParada, pl: Plantilla, parada: Int, n: Int): ItemParada {
        val clase = TrianguloTextural.clasificar(pl.arena, pl.limo, pl.arcilla)!!
        return base.copy(
            titulo = "Horizonte ${pl.nombre}", descripcion = pl.descripcion,
            fotos = listOf(
                foto("p${parada}_h${n}_seco", "${pl.nombre} · seco ${pl.seco}", ColoresMunsell.buscar(pl.seco)!!.rgb),
                foto("p${parada}_h${n}_humedo", "${pl.nombre} · húmedo ${pl.humedo}", ColoresMunsell.buscar(pl.humedo)!!.rgb)
            ),
            nombre = pl.nombre, profundidad = pl.profundidad,
            limiteDefinicion = pl.limiteDef, limiteTopografia = pl.limiteTopo,
            colorSeco = pl.seco, colorHumedo = pl.humedo,
            textura = clase.nombre, texturaArena = pl.arena, texturaLimo = pl.limo, texturaArcilla = pl.arcilla,
            resistencia = pl.resistencia, adhesividad = pl.adhesividad, plasticidad = pl.plasticidad,
            estructuraTipo = pl.estrTipo, estructuraGrado = pl.estrGrado, estructuraTamano = pl.estrTamano,
            reaccionHcl = pl.hcl,
            concentracionTipo = pl.concTipo, concentracionTamano = pl.concTamano, concentracionColor = pl.concColor,
            concentracionCantidad = pl.concCantidad, concentracionForma = pl.forma,
            rasgosRedox = pl.redox, moteados = pl.moteados, barnices = pl.barnices, humedad = pl.humedad,
            raicesCantidad = pl.raicesCantidad, raicesTamano = pl.raicesTamano, raicesUbicacion = pl.raicesUbicacion,
            fragmentosTipo = pl.fragTipo, fragmentosVolumen = pl.fragVolumen,
            fragmentosRedondez = pl.fragRedondez, fragmentosTamano = pl.fragTamano
        )
    }

    /** Foto de prueba del color del suelo, con una leyenda; se guarda donde la app guarda las fotos. */
    private fun foto(nombre: String, leyenda: String, rgb: Int): String {
        val bitmap = Bitmap.createBitmap(1200, 900, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF))
        val grano = Paint().apply { color = Color.argb(60, 0, 0, 0) }
        val azar = java.util.Random(nombre.hashCode().toLong())
        repeat(3000) { canvas.drawCircle(azar.nextFloat() * 1200, azar.nextFloat() * 900, 2f + azar.nextFloat() * 4, grano) }
        canvas.drawRect(0f, 760f, 1200f, 900f, Paint().apply { color = Color.argb(170, 0, 0, 0) })
        canvas.drawText(leyenda, 40f, 850f, Paint().apply { color = Color.WHITE; textSize = 64f; isAntiAlias = true })
        val archivo = "test_$nombre.jpg"
        fotos.archivo(archivo).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        fotosDePrueba += archivo
        return archivo
    }

    private fun exportarParada(almacen: AlmacenExportaciones, recorrido: Recorrido, parada: Parada): File {
        val (datos, nombre) = AlmacenExportaciones.datosParada(recorrido, parada)
        return almacen.exportar(datos, nombre, copia = false) { PlanillaPdf(context).generarParada(recorrido, parada, it) }
    }

    private fun exportarSalida(almacen: AlmacenExportaciones, recorrido: Recorrido): File {
        val (datos, nombre) = AlmacenExportaciones.datosSalida(recorrido)
        return almacen.exportar(datos, nombre, copia = false) { PlanillaPdf(context).generarRecorrido(recorrido, it) }
    }

    /** Guarda cada página como PNG para revisarla a ojo y devuelve la cantidad de páginas. */
    private fun renderizar(pdf: File, carpeta: File, prefijo: String): Int =
        ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { pagina ->
                        val bitmap = Bitmap.createBitmap(pagina.width * 2, pagina.height * 2, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        pagina.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        File(carpeta, "${prefijo}_pag${i + 1}.png").outputStream().use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }
                    }
                }
                renderer.pageCount
            }
        }

    private fun linea(texto: String) {
        informe.appendLine(texto)
    }
}
