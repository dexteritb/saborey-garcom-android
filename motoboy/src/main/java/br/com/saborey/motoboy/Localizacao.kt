package br.com.saborey.motoboy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Manda o GPS do motoboy para a Saborey a cada ~5 s enquanto ele tem entrega em rota.
 * Funciona com a tela apagada e com o Google Maps aberto (serviço em primeiro plano, com notificação fixa).
 * Para sozinho quando o sistema responde que não há mais entrega em rota, ou quando a sessão cai.
 */
class Localizacao : Service() {
    private val gps by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val envio = Executors.newSingleThreadExecutor()
    @Volatile private var sessao = ""
    @Volatile private var base = BuildConfig.SERVIDOR
    @Volatile private var ultimoEnvio = 0L
    private var ligado = false

    private val retorno = object : LocationCallback() {
        override fun onLocationResult(r: LocationResult) { r.lastLocation?.let { mandar(it) } }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra("sessao")?.takeIf { it.isNotBlank() }?.let { sessao = it }
        intent?.getStringExtra("base")?.takeIf { it.isNotBlank() }?.let { base = it.trimEnd('/') }
        if (sessao.isBlank()) { stopSelf(); return START_NOT_STICKY }
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(1, notificacao(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            else startForeground(1, notificacao())
        } catch (_: Exception) { stopSelf(); return START_NOT_STICKY } // sem permissão de localização
        if (!ligado) {
            try {
                val pedido = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
                    .setMinUpdateIntervalMillis(4_000L)
                    .setWaitForAccurateLocation(false)
                    .build()
                gps.requestLocationUpdates(pedido, retorno, Looper.getMainLooper())
                ligado = true; ativo = true
            } catch (_: SecurityException) { stopSelf() }
        }
        return START_NOT_STICKY
    }

    private fun mandar(l: Location) {
        val agora = System.currentTimeMillis()
        if (agora - ultimoEnvio < 4_000L) return
        ultimoEnvio = agora
        val s = sessao; val b = base
        val corpo = "{\"lat\":${l.latitude},\"lng\":${l.longitude},\"precisao\":${if (l.hasAccuracy()) l.accuracy.toInt() else 0}" +
            (if (l.hasSpeed()) ",\"velocidade\":${l.speed}" else "") + ",\"app\":\"android\"}"
        envio.execute {
            try {
                val c = URL("$b/api/sis/motoboy/pos").openConnection() as HttpURLConnection
                c.requestMethod = "POST"; c.connectTimeout = 10_000; c.readTimeout = 10_000; c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.setRequestProperty("x-sessao", s)
                c.outputStream.use { it.write(corpo.toByteArray()) }
                val cod = c.responseCode
                val resp = (if (cod < 400) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
                c.disconnect()
                // sessão caiu, ou não tem mais entrega em rota: desliga o GPS
                if (cod == 401 || cod == 403 || (cod < 400 && resp.contains("\"ativo\":false"))) parar()
            } catch (_: Exception) { /* sem internet agora: tenta no próximo ponto */ }
        }
    }

    private fun parar() { android.os.Handler(Looper.getMainLooper()).post { stopSelf() } }

    private fun notificacao(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CANAL) == null)
            nm.createNotificationChannel(NotificationChannel(CANAL, "Entrega em rota", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Aparece enquanto a Saborey e o cliente acompanham sua entrega no mapa"
            })
        val abrir = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CANAL) else @Suppress("DEPRECATION") Notification.Builder(this)
        return b.setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Entrega em rota 🛵")
            .setContentText("A Saborey e o cliente estão vendo você no mapa")
            .setOngoing(true)
            .setContentIntent(abrir)
            .build()
    }

    override fun onDestroy() {
        try { gps.removeLocationUpdates(retorno) } catch (_: Exception) {}
        ligado = false; ativo = false
        envio.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val CANAL = "rastreio"
        @Volatile var ativo = false
    }
}
