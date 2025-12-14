package com.example.proyecto.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.proyecto.R
import com.example.proyecto.MainActivity
import com.example.proyecto.api.ApiService
import com.example.proyecto.api.ApiClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.proyecto.data.SessionData

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFCMService"
    private val CHANNEL_ID = "reuniones_channel"
    private val apiService: ApiService = ApiClient.apiService

    override fun onNewToken(token: String) {
        Log.d(TAG, "Token de FCM actualizado: $token")

        val authToken = SessionData.token
        if (authToken != null) {
            sendRegistrationToServer(authToken, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Mensaje recibido de: ${remoteMessage.from}")

        val data = remoteMessage.data
        val tipo = data["tipo"]

        // ✅ Si viene sin tipo, igual mostramos notificación genérica (por si acaso)
        val title = remoteMessage.notification?.title ?: "Notificación"
        val body = remoteMessage.notification?.body ?: "Tienes una nueva notificación"

        if (tipo != null) {
            // (Opcional) Guarda último evento para que la app lo procese al abrirse
            guardarUltimoEvento(data)

            // ✅ Notificación “clickeable”: abre MainActivity con extras
            showNotificationClickable(title, body, data)
        } else {
            // Notificación simple
            showNotificationSimple(title, body)
        }

        // ✅ Log para depuración
        when (tipo) {
            "nueva_reunion" -> Log.i(TAG, "📌 Notificación: nueva_reunion id=${data["reunion_id"]}")
            "reunion_iniciada" -> Log.i(TAG, "📌 Notificación: reunion_iniciada id=${data["reunion_id"]}")
            "reunion_finalizada" -> Log.i(TAG, "📌 Notificación: reunion_finalizada id=${data["reunion_id"]}")
            "reunion_cancelada" -> Log.i(TAG, "📌 Notificación: reunion_cancelada id=${data["reunion_id"]}")
            "acta_aprobada" -> Log.i(TAG, "📌 Notificación: acta_aprobada acta=${data["acta_id"]} reunion=${data["reunion_id"]}")
        }
    }

    private fun sendRegistrationToServer(authToken: String, fcmToken: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val body = mapOf("fcm_token" to fcmToken)
                val authHeader = "Token $authToken"
                apiService.registrarFCMToken(authHeader, body)
                Log.i(TAG, "✅ Token FCM actualizado al servidor.")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Fallo al enviar el token FCM: ${e.message}", e)
            }
        }
    }

    // ============================================================
    // ✅ NOTIFICACIÓN SIMPLE (fallback)
    // ============================================================
    private fun showNotificationSimple(title: String?, body: String?) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        crearCanalSiCorresponde(notificationManager)

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title ?: "Notificación")
            .setContentText(body ?: "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body ?: ""))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    // ============================================================
    // ✅ NOTIFICACIÓN CLICKEABLE (abre app con extras)
    // ============================================================
    private fun showNotificationClickable(title: String?, body: String?, data: Map<String, String>) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        crearCanalSiCorresponde(notificationManager)

        // Intent para abrir MainActivity con los extras
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("tipo", data["tipo"])
            data["reunion_id"]?.let { putExtra("reunion_id", it) }
            data["acta_id"]?.let { putExtra("acta_id", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title ?: "Notificación")
            .setContentText(body ?: "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body ?: ""))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) // ✅ clave

        val notificationId = data["reunion_id"]?.hashCode()
            ?: data["acta_id"]?.hashCode()
            ?: System.currentTimeMillis().toInt()

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun crearCanalSiCorresponde(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notificaciones de Reuniones",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    // ============================================================
    // (Opcional recomendado) Guardar evento para procesarlo al abrir app
    // ============================================================
    private fun guardarUltimoEvento(data: Map<String, String>) {
        try {
            val prefs = getSharedPreferences("proyecto_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("last_fcm_tipo", data["tipo"])
                .putString("last_fcm_reunion_id", data["reunion_id"])
                .putString("last_fcm_acta_id", data["acta_id"])
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo guardar evento FCM en prefs: ${e.message}")
        }
    }
}
