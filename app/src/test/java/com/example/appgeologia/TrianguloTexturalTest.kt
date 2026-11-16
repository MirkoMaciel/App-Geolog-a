package com.example.appgeologia

import com.example.appgeologia.data.recorrido.TrianguloTextural
import com.example.appgeologia.data.recorrido.TrianguloTextural.Clase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrianguloTexturalTest {

    private fun clase(arena: Int, limo: Int, arcilla: Int) = TrianguloTextural.clasificar(arena, limo, arcilla)

    @Test
    fun ejemploDelLibro() {
        // "Si tenemos un 35% de arena, 25% de arcilla y 40% de limo, la clase textural será franca (Fig. 22)."
        assertEquals(Clase.FRANCO, clase(35, 40, 25))
    }

    @Test
    fun unPuntoDentroDeCadaUnaDeLasDoceClases() {
        assertEquals(Clase.ARENOSO, clase(92, 5, 3))
        assertEquals(Clase.ARENO_FRANCO, clase(82, 12, 6))
        assertEquals(Clase.FRANCO_ARENOSO, clase(65, 25, 10))
        assertEquals(Clase.FRANCO, clase(40, 40, 20))
        assertEquals(Clase.FRANCO_LIMOSO, clase(20, 65, 15))
        assertEquals(Clase.LIMOSO, clase(5, 88, 7))
        assertEquals(Clase.FRANCO_ARCILLO_ARENOSO, clase(60, 13, 27))
        assertEquals(Clase.FRANCO_ARCILLOSO, clase(32, 34, 34))
        assertEquals(Clase.FRANCO_ARCILLO_LIMOSO, clase(10, 56, 34))
        assertEquals(Clase.ARCILLO_ARENOSO, clase(52, 6, 42))
        assertEquals(Clase.ARCILLO_LIMOSO, clase(6, 47, 47))
        assertEquals(Clase.ARCILLOSO, clase(20, 20, 60))
    }

    @Test
    fun losVerticesDelTriangulo() {
        assertEquals(Clase.ARENOSO, clase(100, 0, 0))
        assertEquals(Clase.LIMOSO, clase(0, 100, 0))
        assertEquals(Clase.ARCILLOSO, clase(0, 0, 100))
    }

    @Test
    fun cadaEtiquetaDelDibujoCaeEnSuPropiaClase() {
        // La posición donde se escribe cada nombre en el triángulo tiene que estar dentro de esa clase.
        Clase.entries.forEach { c ->
            val (arena, arcilla) = c.posEtiqueta
            assertEquals(c.name, c, TrianguloTextural.clasificar(arena, 100 - arena - arcilla, arcilla))
        }
    }

    @Test
    fun siNoSumanCienNoHayClase() {
        assertNull(clase(30, 30, 30))
        assertNull(clase(50, 50, 10))
        assertNull(clase(-5, 55, 50))
    }

    @Test
    fun losNombresSonLosDeLasSugerenciasDeTextura() {
        assertEquals("Arcillo arenoso", Clase.ARCILLO_ARENOSO.nombre)
        assertEquals("Franco", Clase.FRANCO.nombre)
    }
}
