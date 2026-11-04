package com.example.appgeologia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.appgeologia.ui.AppGeologia
import com.example.appgeologia.ui.theme.AppGeologiaTheme

/**
 * Punto de entrada de la app: Android ejecuta esta Activity al tocar el ícono.
 * Toda la interfaz está en Compose; la navegación entre pantallas empieza en [AppGeologia].
 *
 * Para estudiar el código, ver docs/Guia_del_codigo.pdf. Orden de lectura sugerido:
 *  1. data/recorrido/Modelos.kt              → cómo son los datos
 *  2. data/recorrido/RecorridoRepository.kt  → cómo se guardan y modifican
 *  3. ui/recorridos/RecorridosViewModel.kt   → puente entre pantallas y datos
 *  4. ui/Navegacion.kt                       → pantallas y cómo se pasa de una a otra
 *  5. ui/inicio/InicioScreen.kt y el resto de ui/ → las pantallas
 *  6. ubicacion/, archivos/ y pdf/           → GPS, fotos, exportaciones y PDF
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppGeologiaTheme {
                AppGeologia()
            }
        }
    }
}
