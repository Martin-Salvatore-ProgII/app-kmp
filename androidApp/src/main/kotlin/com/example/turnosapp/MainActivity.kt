package com.example.turnosapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Punto de entrada de Android. Muestra la interfaz común, que vive en el módulo composeApp,
 * con el contenedor de dependencias que creó TurnosApplication.
 */
class MainActivity : ComponentActivity() {

    // Si el usuario lo rechaza, la app abre igual y cada pedido falla como "No se pudo conectar".
    private val requestLocalNetwork =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestLocalNetworkIfNeeded()
        val container = (application as TurnosApplication).container
        setContent {
            App(container)
        }
    }

    /**
     * Desde Android 17, una app necesita este permiso para conectarse a direcciones de la red local,
     * como la de la máquina vista desde el emulador (10.0.2.2), donde corren los backends (ADR-0078).
     * Sin él, Android descarta la conexión en silencio. En versiones anteriores el permiso no existe.
     */
    private fun requestLocalNetworkIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) return
        if (checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED) {
            requestLocalNetwork.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        }
    }
}
