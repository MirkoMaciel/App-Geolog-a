package com.example.appgeologia.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.appgeologia.data.recorrido.DatosSitio
import com.example.appgeologia.data.recorrido.GrupoOpciones
import com.example.appgeologia.archivos.AlmacenExportaciones
import com.example.appgeologia.archivos.AlmacenFotos
import com.example.appgeologia.data.recorrido.ItemParada
import com.example.appgeologia.data.recorrido.tieneDatosSuelo
import com.example.appgeologia.data.recorrido.Opcion
import com.example.appgeologia.data.recorrido.OpcionesSuelo
import com.example.appgeologia.data.recorrido.Parada
import com.example.appgeologia.data.recorrido.Recorrido
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dibuja la planilla de descripción de suelos en un PDF, copiando el formato de la
 * planilla en papel: un encabezado con los datos del sitio y una tabla con una columna
 * por item con datos de suelo (4 por página). En los campos con opciones fijas se
 * imprimen todas las opciones y se marca con un círculo la elegida, como se haría a mano.
 *
 * Después de la planilla va una ficha por cada item con su título, descripción y fotos.
 * Los PDF quedan guardados en "Mis exportaciones".
 */
class PlanillaPdf(private val context: Context) {

    // Tamaño A4 en puntos (1/72 de pulgada).
    private val anchoPagina = 595
    private val altoPagina = 842
    private val margen = 28f
    private val anchoUtil = anchoPagina - 2 * margen

    // Tabla de horizontes
    private val anchoGrupo = 18f
    private val anchoEtiqueta = 72f
    private val horizontesPorPagina = 4
    private val anchoHorizonte = (anchoUtil - anchoGrupo - anchoEtiqueta) / horizontesPorPagina
    private val anchoForma = 34f
    private val altoFila = 16f
    private val altoFilaDoble = 22f

    // Fichas de items
    private val altoFoto = 200f
    private val separacionFotos = 10f

    private val fotos = AlmacenFotos(context)
    private val exportaciones = AlmacenExportaciones(context)

    private val colorMarca = Color.rgb(192, 57, 43)

    private val lapizLinea = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.6f
        color = Color.BLACK
        isAntiAlias = true
    }
    private val lapizLineaGruesa = Paint(lapizLinea).apply { strokeWidth = 1.2f }
    private val lapizMarca = Paint(lapizLinea).apply {
        strokeWidth = 0.9f
        color = colorMarca
    }
    private val texto = TextPaint().apply {
        color = Color.BLACK
        textSize = 8f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    private val textoNegrita = TextPaint(texto).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val textoGris = TextPaint(texto).apply {
        color = Color.GRAY
        textSize = 6.5f
    }

    /** Una fila de la tabla de horizontes. */
    private class Fila(
        val grupo: String?,
        val etiqueta: String,
        val valor: (ItemParada) -> String,
        val opciones: GrupoOpciones? = null,
        val alto: Float
    )

    private val filas: List<Fila> = listOf(
        Fila(null, "Horizontes", { it.nombre.ifBlank { it.titulo } }, alto = altoFila),
        Fila(null, "Profundidad (cm)", { it.profundidad }, alto = altoFila),
        Fila("Límite", "Definición", { it.limiteDefinicion }, OpcionesSuelo.limiteDefinicion, altoFila),
        Fila("Límite", "Topografía", { it.limiteTopografia }, OpcionesSuelo.limiteTopografia, altoFila),
        Fila("Color", "Seco", { it.colorSeco }, alto = altoFila),
        Fila("Color", "Húmedo", { it.colorHumedo }, alto = altoFila),
        Fila(null, "Textura al tacto", { it.textura }, alto = altoFila),
        Fila("Consistencia", "Resist.", { it.resistencia }, alto = altoFila),
        Fila("Consistencia", "Adhes.", { it.adhesividad }, OpcionesSuelo.adhesividad, altoFilaDoble),
        Fila("Consistencia", "Plast.", { it.plasticidad }, OpcionesSuelo.plasticidad, altoFilaDoble),
        Fila("Estructura", "Tipo", { it.estructuraTipo }, alto = altoFila),
        Fila("Estructura", "Grado", { it.estructuraGrado }, OpcionesSuelo.estructuraGrado, altoFila),
        Fila("Estructura", "Tamaño", { it.estructuraTamano }, alto = altoFila),
        Fila(null, "Reacción HCl", { it.reaccionHcl }, OpcionesSuelo.reaccionHcl, altoFila),
        Fila(GRUPO_CONCENTRACIONES, "Tipo", { it.concentracionTipo }, OpcionesSuelo.concentracionTipo, altoFila),
        Fila(GRUPO_CONCENTRACIONES, "Tamaño", { it.concentracionTamano }, alto = altoFila),
        Fila(GRUPO_CONCENTRACIONES, "Color", { it.concentracionColor }, alto = altoFila),
        Fila(GRUPO_CONCENTRACIONES, "Cantidad", { it.concentracionCantidad }, alto = altoFila),
        Fila(null, "Rasgos Redox", { it.rasgosRedox }, alto = altoFila),
        Fila(null, "Moteados", { it.moteados }, alto = altoFila),
        Fila(null, "Barnices", { it.barnices }, alto = altoFila),
        Fila(null, "Humedad", { it.humedad }, OpcionesSuelo.humedad, altoFila),
        Fila("Raíces", "Cantidad", { it.raicesCantidad }, OpcionesSuelo.raicesCantidad, altoFila),
        Fila("Raíces", "Tamaño", { it.raicesTamano }, alto = altoFila),
        Fila("Raíces", "Ubic.", { it.raicesUbicacion }, alto = altoFila),
        Fila("Fragmentos Gruesos", "Tipo", { it.fragmentosTipo }, alto = altoFila),
        Fila("Fragmentos Gruesos", "% Vol.", { it.fragmentosVolumen }, alto = altoFila),
        Fila("Fragmentos Gruesos", "Redond.", { it.fragmentosRedondez }, alto = altoFila),
        Fila("Fragmentos Gruesos", "Tamaño", { it.fragmentosTamano }, alto = altoFila)
    )

    /** Lleva la cuenta de páginas del documento. */
    private inner class Documento {
        val pdf = PdfDocument()
        private var actual: PdfDocument.Page? = null
        private var numero = 0

        /** Canvas de la página en curso. */
        val canvas: Canvas? get() = actual?.canvas

        fun nuevaPagina(): Canvas {
            terminarPagina()
            numero++
            val pagina = pdf.startPage(PdfDocument.PageInfo.Builder(anchoPagina, altoPagina, numero).create())
            actual = pagina
            return pagina.canvas
        }

        private fun terminarPagina() {
            actual?.let { pagina ->
                dibujarPie(pagina.canvas, numero)
                pdf.finishPage(pagina)
            }
            actual = null
        }

        fun guardar(archivo: File): File {
            terminarPagina()
            archivo.parentFile?.mkdirs()
            archivo.outputStream().use { pdf.writeTo(it) }
            pdf.close()
            return archivo
        }
    }

    // ------------------------------------------------------------------ API pública

    /** Genera el PDF de una sola parada. */
    fun generarParada(recorrido: Recorrido, parada: Parada): File {
        val documento = Documento()
        dibujarParada(documento, recorrido, parada)
        return documento.guardar(exportaciones.nuevoArchivo("Parada_${parada.numero}_${recorrido.nombre}"))
    }

    /** Genera un PDF con una carátula del recorrido y la planilla de cada parada. */
    fun generarRecorrido(recorrido: Recorrido): File {
        val documento = Documento()
        dibujarCaratula(documento.nuevaPagina(), recorrido)
        recorrido.paradas.sortedBy { it.numero }.forEach { dibujarParada(documento, recorrido, it) }
        return documento.guardar(exportaciones.nuevoArchivo("Recorrido_${recorrido.nombre}"))
    }

    // ------------------------------------------------------------------ Parada

    private fun dibujarParada(documento: Documento, recorrido: Recorrido, parada: Parada) {
        // En la tabla van los items que tienen datos de suelo, 4 por página. Siempre se
        // dibuja al menos una página, aunque no haya ninguno (sirve como planilla en blanco).
        val columnas = parada.items.filter { it.tieneDatosSuelo() }
        val bloques = columnas.chunked(horizontesPorPagina).ifEmpty { listOf(emptyList()) }

        var y = 0f
        bloques.forEachIndexed { i, bloque ->
            val canvas = documento.nuevaPagina()
            y = dibujarTitulo(
                canvas, recorrido, parada,
                if (i == 0) "PLANILLA DE DESCRIPCIÓN DE SUELOS" else "PLANILLA DE DESCRIPCIÓN DE SUELOS (continuación)"
            )
            if (i == 0) y = dibujarEncabezado(canvas, y + 6f, parada) + 12f
            y = dibujarTablaHorizontes(canvas, y, bloque)
        }

        if (parada.observaciones.isNotBlank()) {
            val espacioRestante = altoPagina - margen - 20f - y
            val canvas = documento.canvas
            if (canvas == null || espacioRestante < 70f) {
                val nueva = documento.nuevaPagina()
                y = dibujarTitulo(nueva, recorrido, parada, "PLANILLA DE DESCRIPCIÓN DE SUELOS (continuación)")
                dibujarObservaciones(nueva, y + 10f, parada.observaciones)
            } else {
                dibujarObservaciones(canvas, y + 10f, parada.observaciones)
            }
        }

        if (parada.items.isNotEmpty()) dibujarFichasItems(documento, recorrido, parada)
    }

    // ------------------------------------------------------------------ Fichas de items

    /**
     * Una ficha por item: título, hora, descripción y sus fotos en dos columnas.
     * Las fichas se encadenan y pasan a otra página cuando no entran.
     */
    private fun dibujarFichasItems(documento: Documento, recorrido: Recorrido, parada: Parada) {
        val limite = altoPagina - margen - 20f
        val tituloPagina = "ITEMS DE LA PARADA N° ${parada.numero}"
        var canvas = documento.nuevaPagina()
        var y = dibujarTitulo(canvas, recorrido, parada, tituloPagina) + 10f

        fun paginaNueva() {
            canvas = documento.nuevaPagina()
            y = dibujarTitulo(canvas, recorrido, parada, "$tituloPagina (continuación)") + 10f
        }

        parada.items.forEachIndexed { i, item ->
            val altoEncabezado = 20f
            val descripcion = item.descripcion.takeIf { it.isNotBlank() }?.let { layoutTexto(it, 9f, anchoUtil - 8f) }
            val altoDescripcion = descripcion?.height?.plus(8f) ?: 0f
            val altoMinimo = altoEncabezado + altoDescripcion + if (item.fotos.isNotEmpty()) altoFoto else 0f
            if (y + minOf(altoMinimo, limite - margen - 40f) > limite) paginaNueva()

            // Encabezado de la ficha
            val fondo = Paint().apply { color = Color.rgb(240, 230, 222); style = Paint.Style.FILL }
            canvas.drawRect(margen, y, margen + anchoUtil, y + altoEncabezado, fondo)
            canvas.drawRect(margen, y, margen + anchoUtil, y + altoEncabezado, lapizLinea)
            textoNegrita.textSize = 10f
            canvas.drawText("${i + 1}. ${item.titulo.ifBlank { "Item sin título" }}", margen + 5f, y + 14f, textoNegrita)
            texto.textSize = 8f
            val detalle = listOfNotNull(
                item.fecha.takeIf { it > 0 }?.let { horaCorta(it) },
                item.nombre.takeIf { item.tieneDatosSuelo() }?.let { nombre ->
                    "Columna \"${nombre.ifBlank { item.titulo.ifBlank { "s/n" } }}\" de la planilla"
                }
            ).joinToString("  ·  ")
            canvas.drawText(detalle, margen + anchoUtil - 5f - texto.measureText(detalle), y + 14f, texto)
            y += altoEncabezado + 4f

            // Descripción
            if (descripcion != null) {
                canvas.save()
                canvas.clipRect(margen, y, margen + anchoUtil, limite)
                canvas.translate(margen + 4f, y)
                descripcion.draw(canvas)
                canvas.restore()
                y += altoDescripcion
            }

            // Fotos en dos columnas
            val anchoCelda = (anchoUtil - separacionFotos) / 2
            item.fotos.chunked(2).forEach { fila ->
                if (y + altoFoto > limite) paginaNueva()
                fila.forEachIndexed { col, nombre ->
                    val izquierda = margen + col * (anchoCelda + separacionFotos)
                    dibujarFoto(canvas, nombre, RectF(izquierda, y, izquierda + anchoCelda, y + altoFoto))
                }
                y += altoFoto + separacionFotos
            }
            y += 14f
        }
    }

    /** Dibuja la foto dentro de [rect] sin deformarla, centrada y con un borde fino. */
    private fun dibujarFoto(canvas: Canvas, nombre: String, rect: RectF) {
        val bitmap = fotos.cargarReducida(nombre, 1200)
        if (bitmap == null) {
            canvas.drawRect(rect, lapizLinea)
            textoAjustado(canvas, rect, "Foto no disponible", tamMax = 8f, centrado = true)
            return
        }
        val escala = minOf(rect.width() / bitmap.width, rect.height() / bitmap.height)
        val ancho = bitmap.width * escala
        val alto = bitmap.height * escala
        val destino = RectF(
            rect.centerX() - ancho / 2, rect.centerY() - alto / 2,
            rect.centerX() + ancho / 2, rect.centerY() + alto / 2
        )
        canvas.drawBitmap(bitmap, null, destino, Paint(Paint.FILTER_BITMAP_FLAG))
        canvas.drawRect(destino, lapizLinea)
        bitmap.recycle()
    }

    private fun layoutTexto(contenido: String, tam: Float, ancho: Float): StaticLayout {
        val pincel = TextPaint(texto).apply { textSize = tam }
        return StaticLayout.Builder.obtain(contenido, 0, contenido.length, pincel, ancho.toInt())
            .setIncludePad(false)
            .build()
    }

    private fun dibujarTitulo(canvas: Canvas, recorrido: Recorrido, parada: Parada, titulo: String): Float {
        val y = margen + 10f
        textoNegrita.textSize = 11f
        canvas.drawText(titulo, margen, y, textoNegrita)
        texto.textSize = 8f
        val derecha = "Recorrido: ${recorrido.nombre}  ·  Parada N° ${parada.numero}"
        canvas.drawText(derecha, anchoPagina - margen - texto.measureText(derecha), y, texto)
        textoNegrita.textSize = 8f
        return y + 4f
    }

    // ------------------------------------------------------------------ Encabezado

    private fun dibujarEncabezado(canvas: Canvas, arriba: Float, parada: Parada): Float {
        val s: DatosSitio = parada.sitio
        val x0 = margen
        val x1 = margen + 255f
        val x2 = margen + 393f
        val fin = margen + anchoUtil
        val alto = 15f

        var y = arriba
        val lineasDivisorias = mutableListOf<Float>()

        fun fila(altoFila: Float = alto, contenido: (top: Float, bottom: Float) -> Unit) {
            contenido(y, y + altoFila)
            y += altoFila
            lineasDivisorias += y
        }

        fila { t, b ->
            campo(canvas, "Calicata:", s.calicata, x0, x1, t, b)
            campo(canvas, "Sitio de Ref.:", s.sitioReferencia, x1, x2, t, b)
            campo(canvas, "Fecha:", fechaCorta(parada.fecha), x2, fin, t, b)
        }
        fila { t, b -> campo(canvas, "Proyecto:", s.proyecto, x0, fin, t, b) }
        fila { t, b ->
            campo(canvas, "Coord. Geográficas.:", "", x0, x0 + 90f, t, b)
            campo(canvas, "Lat.:", coordenada(parada.latitud), x0 + 90f, x1, t, b)
            campo(canvas, "Long.:", coordenada(parada.longitud), x1, x2, t, b)
            campo(canvas, "Altitud:", parada.altitud?.let { "%.0f m".format(Locale.US, it) } ?: "", x2, fin, t, b)
        }
        fila { t, b ->
            campo(canvas, "Geomorfología:", s.geomorfologia, x0, x2, t, b)
            campo(canvas, "Posic. Geomórfica:", s.posicionGeomorfica, x2, fin, t, b)
        }
        fila { t, b ->
            campo(canvas, "Relieve:", s.relieve, x0, x2, t, b)
            campo(canvas, "Exposición:", s.exposicion, x2, fin, t, b)
        }
        fila { t, b ->
            campo(canvas, "Mat. Originario / Sustrato:", s.materialOriginario, x0, x2, t, b)
            campo(canvas, "Pendiente:", s.pendiente, x2, fin, t, b)
        }
        fila(alto * 2) { t, b ->
            campo(canvas, "Vegetación:", s.vegetacion, x0, x2, t, b)
            campo(canvas, "Cobertura vegetal:", s.coberturaVegetal, x2, fin, t, t + alto)
        }
        val separacion = y
        fila { t, b ->
            campo(canvas, "Clase drenaje:", s.claseDrenaje, x0, x2, t, b)
            campo(canvas, "Anegamiento:", s.anegamiento, x2, fin, t, b)
        }
        fila { t, b ->
            campo(canvas, "Erosión:", s.erosion, x0, x2, t, b)
            campo(canvas, "Sales/Álcalis:", s.salesAlcalis, x2, fin, t, b)
        }
        fila { t, b ->
            campo(canvas, "Uso de la tierra:", s.usoTierra, x0, x2, t, b)
            campo(canvas, "Pedregosidad:", s.pedregosidad, x2, fin, t, b)
        }
        fila { t, b ->
            campo(canvas, "Clasificación:", s.clasificacion, x0, x2, t, b)
            campo(canvas, "Temperatura:", s.temperatura, x2, fin, t, b)
        }

        canvas.drawRect(x0, arriba, fin, y, lapizLinea)
        lineasDivisorias.dropLast(1).forEach { canvas.drawLine(x0, it, fin, it, lapizLinea) }
        canvas.drawLine(x0, separacion, fin, separacion, lapizLineaGruesa)
        return y
    }

    /** Dibuja "Etiqueta: valor" dentro de [izquierda]..[derecha]. */
    private fun campo(
        canvas: Canvas, etiqueta: String, valor: String,
        izquierda: Float, derecha: Float, arriba: Float, abajo: Float
    ) {
        textoNegrita.textSize = 8f
        val base = arriba + 11f
        canvas.drawText(etiqueta, izquierda + 3f, base, textoNegrita)
        val inicioValor = izquierda + 3f + textoNegrita.measureText(etiqueta) + 4f
        if (valor.isNotBlank()) {
            textoAjustado(canvas, RectF(inicioValor, arriba + 1f, derecha - 2f, abajo - 1f), valor, tamMax = 8f)
        }
    }

    // ------------------------------------------------------------------ Tabla de horizontes

    private fun dibujarTablaHorizontes(canvas: Canvas, arriba: Float, horizontes: List<ItemParada>): Float {
        val xEtiqueta = margen + anchoGrupo
        val xDatos = xEtiqueta + anchoEtiqueta

        // Posición vertical de cada fila
        val tops = mutableListOf<Float>()
        var y = arriba
        filas.forEach { tops += y; y += it.alto }
        val abajo = y

        filas.forEachIndexed { i, fila ->
            val t = tops[i]
            val b = t + fila.alto

            // Etiqueta (si no tiene grupo ocupa también la columna del grupo)
            val izquierdaEtiqueta = if (fila.grupo == null) margen else xEtiqueta
            val rectEtiqueta = RectF(izquierdaEtiqueta, t, xDatos, b)
            canvas.drawRect(rectEtiqueta, lapizLinea)
            textoAjustado(canvas, rectEtiqueta, fila.etiqueta, tamMax = 8f, negrita = true)

            // Una celda por horizonte
            for (col in 0 until horizontesPorPagina) {
                val izquierda = xDatos + col * anchoHorizonte
                val derecha = izquierda + anchoHorizonte -
                    if (fila.grupo == GRUPO_CONCENTRACIONES) anchoForma else 0f
                val rect = RectF(izquierda, t, derecha, b)
                canvas.drawRect(rect, lapizLinea)

                val horizonte = horizontes.getOrNull(col)
                val valor = horizonte?.let(fila.valor).orEmpty()
                val opciones = fila.opciones
                if (opciones != null && (valor.isEmpty() || opciones.todas.any { it.codigo == valor })) {
                    dibujarOpciones(canvas, rect, opciones, valor)
                } else {
                    textoAjustado(canvas, rect, valor, tamMax = 8f, centrado = true)
                }
            }
        }

        // Columna de grupos con el texto vertical (Límite, Color, Consistencia, ...)
        var i = 0
        while (i < filas.size) {
            val grupo = filas[i].grupo
            var j = i
            while (j + 1 < filas.size && filas[j + 1].grupo == grupo) j++
            if (grupo != null) {
                val t = tops[i]
                val b = tops[j] + filas[j].alto
                canvas.drawRect(margen, t, xEtiqueta, b, lapizLinea)
                textoVertical(canvas, grupo, margen + anchoGrupo / 2, (t + b) / 2, b - t - 4f)

                // Sub-columna "Forma" de las concentraciones, que abarca sus 4 filas
                if (grupo == GRUPO_CONCENTRACIONES) {
                    for (col in 0 until horizontesPorPagina) {
                        val derecha = xDatos + (col + 1) * anchoHorizonte
                        val rect = RectF(derecha - anchoForma, t, derecha, b)
                        canvas.drawRect(rect, lapizLinea)
                        texto.textSize = 7f
                        canvas.drawText("Forma", rect.left + 3f, t + 9f, texto)
                        val forma = horizontes.getOrNull(col)?.concentracionForma.orEmpty()
                        textoAjustado(
                            canvas, RectF(rect.left, t + 11f, rect.right, b), forma,
                            tamMax = 7f, centrado = true
                        )
                    }
                }
            }
            i = j + 1
        }

        canvas.drawRect(margen, arriba, margen + anchoUtil, abajo, lapizLineaGruesa)
        return abajo
    }

    /**
     * Imprime las opciones ("MA – A – C – G – D") y encierra en un círculo la seleccionada,
     * que además va en negrita.
     */
    private fun dibujarOpciones(canvas: Canvas, rect: RectF, grupo: GrupoOpciones, seleccion: String) {
        val separador = " – "
        val anchoDisponible = rect.width() - 6f
        // Espacio extra a cada lado de la opción elegida para que el círculo no tape los guiones.
        val holgura = 2.5f
        var tam = 7.5f

        fun anchoOpcion(op: Opcion): Float =
            if (op.codigo == seleccion) textoNegrita.measureText(op.codigo) + 2 * holgura
            else texto.measureText(op.codigo)

        fun anchoLinea(linea: List<Opcion>): Float {
            texto.textSize = tam
            textoNegrita.textSize = tam
            return linea.sumOf { anchoOpcion(it).toDouble() }.toFloat() +
                texto.measureText(separador) * (linea.size - 1)
        }

        while (tam > 5f && grupo.lineas.any { anchoLinea(it) > anchoDisponible }) tam -= 0.25f
        texto.textSize = tam
        textoNegrita.textSize = tam

        val altoLinea = tam + 2.5f
        val altoTotal = altoLinea * grupo.lineas.size
        var base = rect.top + (rect.height() - altoTotal) / 2 + tam

        grupo.lineas.forEach { linea ->
            var x = rect.left + (rect.width() - anchoLinea(linea)) / 2
            linea.forEachIndexed { k, opcion ->
                val elegida = opcion.codigo == seleccion
                val pincel = if (elegida) textoNegrita else texto
                val ancho = anchoOpcion(opcion)
                val xTexto = if (elegida) x + holgura else x
                canvas.drawText(opcion.codigo, xTexto, base, pincel)
                if (elegida) {
                    canvas.drawOval(RectF(x + 0.5f, base - tam - 0.5f, x + ancho - 0.5f, base + 2.5f), lapizMarca)
                }
                x += ancho
                if (k < linea.lastIndex) {
                    canvas.drawText(separador, x, base, texto)
                    x += texto.measureText(separador)
                }
            }
            base += altoLinea
        }
    }

    // ------------------------------------------------------------------ Otras secciones

    private fun dibujarObservaciones(canvas: Canvas, arriba: Float, observaciones: String) {
        val abajo = altoPagina - margen - 20f
        val rect = RectF(margen, arriba, margen + anchoUtil, abajo)
        canvas.drawRect(rect, lapizLinea)
        textoNegrita.textSize = 8f
        canvas.drawText("Observaciones:", margen + 3f, arriba + 11f, textoNegrita)
        textoAjustado(
            canvas, RectF(rect.left + 2f, arriba + 14f, rect.right - 2f, abajo - 2f),
            observaciones, tamMax = 8f, alinearArriba = true
        )
    }

    private fun dibujarCaratula(canvas: Canvas, recorrido: Recorrido) {
        var y = margen + 20f
        textoNegrita.textSize = 16f
        canvas.drawText("Recorrido: ${recorrido.nombre}", margen, y, textoNegrita)
        y += 22f

        texto.textSize = 10f
        val datos = listOf(
            "Tipo de salida: ${recorrido.tipoSalida.nombre}",
            "Proyecto: ${recorrido.proyecto.ifBlank { "—" }}",
            "Fecha de inicio: ${fechaCorta(recorrido.fechaInicio)}",
            "Estado: ${if (recorrido.finalizado) "Finalizado" else "En curso"}",
            "Cantidad de paradas: ${recorrido.paradas.size}"
        )
        datos.forEach { canvas.drawText(it, margen, y, texto); y += 14f }
        if (recorrido.descripcion.isNotBlank()) {
            y += 4f
            val alto = textoAjustado(
                canvas, RectF(margen, y, margen + anchoUtil, y + 80f),
                recorrido.descripcion, tamMax = 10f, alinearArriba = true
            )
            y += alto + 8f
        }

        // Tabla resumen de paradas
        y += 10f
        val columnas = listOf("N°" to 30f, "Fecha" to 70f, "Calicata" to 110f, "Latitud" to 80f,
            "Longitud" to 80f, "Altitud" to 60f, "Items" to 109f)
        val alto = 16f
        fun filaTabla(valores: List<String>, negrita: Boolean) {
            var x = margen
            columnas.forEachIndexed { k, (_, ancho) ->
                val rect = RectF(x, y, x + ancho, y + alto)
                canvas.drawRect(rect, lapizLinea)
                textoAjustado(canvas, rect, valores[k], tamMax = 8f, negrita = negrita, centrado = true)
                x += ancho
            }
            y += alto
        }
        filaTabla(columnas.map { it.first }, negrita = true)
        recorrido.paradas.sortedBy { it.numero }.forEach { p ->
            if (y + alto > altoPagina - margen - 20f) return
            filaTabla(
                listOf(
                    p.numero.toString(), fechaCorta(p.fecha), p.sitio.calicata,
                    coordenada(p.latitud), coordenada(p.longitud),
                    p.altitud?.let { "%.0f m".format(Locale.US, it) } ?: "",
                    p.items.size.toString()
                ),
                negrita = false
            )
        }
    }

    private fun dibujarPie(canvas: Canvas, numero: Int) {
        val texto = "App Geología · generado el ${fechaCorta(System.currentTimeMillis())} · Página $numero"
        canvas.drawText(
            texto,
            (anchoPagina - textoGris.measureText(texto)) / 2,
            altoPagina - margen + 8f,
            textoGris
        )
    }

    // ------------------------------------------------------------------ Utilidades de texto

    /**
     * Escribe [contenido] dentro de [rect] con saltos de línea automáticos, achicando la
     * letra hasta que entre. Devuelve el alto usado.
     */
    private fun textoAjustado(
        canvas: Canvas,
        rect: RectF,
        contenido: String,
        tamMax: Float,
        tamMin: Float = 5f,
        negrita: Boolean = false,
        centrado: Boolean = false,
        alinearArriba: Boolean = false
    ): Float {
        if (contenido.isBlank()) return 0f
        val pincel = TextPaint(if (negrita) textoNegrita else texto)
        val ancho = (rect.width() - 4f).toInt().coerceAtLeast(1)
        val alineacion = if (centrado) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL

        var tam = tamMax
        var layout: StaticLayout
        while (true) {
            pincel.textSize = tam
            layout = StaticLayout.Builder.obtain(contenido, 0, contenido.length, pincel, ancho)
                .setAlignment(alineacion)
                .setIncludePad(false)
                .build()
            if (layout.height <= rect.height() - 2f || tam <= tamMin) break
            tam -= 0.5f
        }

        canvas.save()
        canvas.clipRect(rect)
        val dy = if (alinearArriba) 1f else ((rect.height() - layout.height) / 2).coerceAtLeast(1f)
        canvas.translate(rect.left + 2f, rect.top + dy)
        layout.draw(canvas)
        canvas.restore()
        return layout.height.toFloat()
    }

    /**
     * Texto rotado 90° para la columna de grupos. Si no entra en una línea y tiene
     * espacios lo parte en dos (como "Fragmentos / Gruesos" en la planilla).
     */
    private fun textoVertical(canvas: Canvas, contenido: String, cx: Float, cy: Float, largoMax: Float) {
        val pincel = TextPaint(textoNegrita)
        var tam = 7.5f
        pincel.textSize = tam
        val lineas = if (pincel.measureText(contenido) > largoMax && contenido.contains(' ')) {
            tam = 7f
            contenido.split(' ', limit = 2)
        } else {
            listOf(contenido)
        }
        pincel.textSize = tam
        while (lineas.any { pincel.measureText(it) > largoMax } && tam > 5f) {
            tam -= 0.25f
            pincel.textSize = tam
        }

        canvas.save()
        canvas.rotate(-90f, cx, cy)
        val altoLinea = tam + 1f
        var base = cy - altoLinea * (lineas.size - 1) / 2 + tam / 3
        lineas.forEach { linea ->
            canvas.drawText(linea, cx - pincel.measureText(linea) / 2, base, pincel)
            base += altoLinea
        }
        canvas.restore()
    }

    private fun horaCorta(millis: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

    private fun fechaCorta(millis: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))

    private fun coordenada(valor: Double?): String =
        valor?.let { "%.6f".format(Locale.US, it) } ?: ""

    private companion object {
        const val GRUPO_CONCENTRACIONES = "Concentraciones"
    }
}
