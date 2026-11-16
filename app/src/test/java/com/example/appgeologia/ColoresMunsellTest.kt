package com.example.appgeologia

import com.example.appgeologia.data.recorrido.ColoresMunsell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ColoresMunsellTest {

    private fun cantidad(matiz: String) = ColoresMunsell.tabla(matiz).sumOf { fila -> fila.chips.count { it != null } }

    @Test
    fun lasTablasTienenLosMismosColoresQueElLibro() {
        // 10YR: 6+6+6+6+5+5+2 colores; 7.5YR: 5+6+6+6+5+4+3 colores (según las láminas).
        assertEquals(36, cantidad("10YR"))
        assertEquals(35, cantidad("7.5YR"))
        assertEquals(listOf("8", "7", "6", "5", "4", "3", "2"), ColoresMunsell.tabla("10YR").map { it.luminosidad })
        assertEquals(listOf("8", "7", "6", "5", "4", "3", "2.5"), ColoresMunsell.tabla("7.5YR").map { it.luminosidad })
    }

    @Test
    fun huecosEnLosMismosLugaresQueElLibro() {
        // 7.5YR 8/8 no existe en el libro; 10YR 8/8 sí.
        assertNull(ColoresMunsell.buscar("7.5YR 8/8"))
        assertNotNull(ColoresMunsell.buscar("10YR 8/8"))
        assertNull(ColoresMunsell.buscar("10YR 2/3"))
    }

    @Test
    fun entiendeLaNotacionEscritaDeDistintasFormas() {
        assertEquals("10YR 5/3", ColoresMunsell.buscar("10YR 5/3")?.notacion)
        assertEquals("10YR 5/3", ColoresMunsell.buscar(" 10yr5/3 ")?.notacion)
        assertEquals("7.5YR 4/4", ColoresMunsell.buscar("7,5YR 4/4")?.notacion)
        assertEquals("7.5YR 2.5/2", ColoresMunsell.buscar("7.5YR 2,5/2")?.notacion)
    }

    @Test
    fun textoQueNoEsDeLaTablaDevuelveNull() {
        assertNull(ColoresMunsell.buscar(""))
        assertNull(ColoresMunsell.buscar("marrón oscuro"))
        assertNull(ColoresMunsell.buscar("2.5Y 6/2"))
    }

    @Test
    fun cadaColorTieneSuNotacion() {
        val chip = ColoresMunsell.buscar("10YR 6/4")!!
        assertEquals("10YR", chip.matiz)
        assertEquals("6", chip.luminosidad)
        assertEquals(4, chip.croma)
        assertEquals(0xAB8F68, chip.rgb)
    }
}
