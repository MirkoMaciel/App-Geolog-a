package com.example.appgeologia

import com.example.appgeologia.data.CatalogoRocas
import com.example.appgeologia.data.TipoRoca
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas locales del catálogo de rocas (se ejecutan en la PC, sin emulador).
 */
class ExampleUnitTest {

    @Test
    fun losIdsDeLasRocasSonUnicos() {
        val ids = CatalogoRocas.rocas.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun filtrarSinTipoDevuelveTodas() {
        assertEquals(CatalogoRocas.rocas, CatalogoRocas.filtrar(null))
    }

    @Test
    fun filtrarPorTipoDevuelveSoloEseTipo() {
        val igneas = CatalogoRocas.filtrar(TipoRoca.IGNEA)
        assertTrue(igneas.isNotEmpty())
        assertTrue(igneas.all { it.tipo == TipoRoca.IGNEA })
    }

    @Test
    fun buscarIdInexistenteDevuelveNull() {
        assertNull(CatalogoRocas.buscarPorId(-1))
    }
}
