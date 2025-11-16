package com.example.proyecto.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.proyecto.R
import com.example.proyecto.api.ApiService
import com.example.proyecto.api.ApiClient
import com.example.proyecto.data.SessionData // Para obtener el token JWT guardado
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.proyecto.api.FcmTokenRequest

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFCMService"
    private val CHANNEL_ID = "reuniones_channel"
    private val apiService: ApiService = ApiClient.apiService

    /**
     * Llamado cuando el token de registro del dispositivo se genera o actualiza.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Token de FCM actualizado: $token")
        // Reintentamos enviar el token al servidor si el usuario ya está logueado
        val authToken = SessionData
            .token
        if (authToken != null) {
            sendRegistrationToServer(authToken, token)
        }
    }

    /**
     * Llamado cuando se recibe un mensaje de FCM.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Mensaje recibido de: ${remoteMessage.from}")

        // Los mensajes enviados desde Django tienen el campo 'notification' y 'data'.
        remoteMessage.notification?.let {
            showNotification(it.title, it.body, remoteMessage.data)
        }

        // Manejar la lógica de datos (payload)
        if (remoteMessage.data["tipo"] == "nueva_reunion") {
            val reunionId = remoteMessage.data["reunion_id"]
            Log.i(TAG, "Notificación de nueva reunión ID: $reunionId. Actualizando datos en segundo plano.")
            // Aquí iría la lógica para refrescar el ViewModel si la app está activa.
        }
    }

    /**
     * Función para enviar el token actualizado al backend.
     */
    private fun sendRegistrationToServer(authToken: String, fcmToken: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val body = mapOf(
                    "fcm_token" to fcmToken
                )

                // Igual que antes: DRF Token -> "Token <clave>"
                val authHeader = "Token $authToken"

                apiService.registrarFCMToken(authHeader, body)
                Log.i(TAG, "Token FCM actualizado al servidor.")
            } catch (e: Exception) {
                Log.e(TAG, "Fallo al enviar el token FCM: ${e.message}", e)
            }
        }
    }

    /**
     * Muestra la notificación en el área de notificaciones de Android.
     */
    private fun showNotification(title: String?, body: String?, data: Map<String, String>) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal de notificación (obligatorio en Android 8.0/Oreo o superior)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notificaciones de Reuniones",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Reemplaza con un ícono de tu app
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Usamos un ID único basado en la reunión para evitar sobrescribir notificaciones
        val notificationId = data["reunion_id"]?.hashCode() ?: System.currentTimeMillis().toInt()

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}