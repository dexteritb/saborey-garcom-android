package br.com.saborey.motoboy

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat

/**
 * Tela única: o app do motoboy do sistema (/motoboy) dentro do aplicativo.
 * A página chama window.SaboreyMotoboy.rastrear(...) quando o motoboy tem (ou deixa de ter)
 * entrega em rota; aí o serviço [Localizacao] manda o GPS mesmo com a tela apagada.
 */
class MainActivity : Activity() {
    private lateinit var web: WebView
    private val host = Uri.parse(BuildConfig.SERVIDOR).host
    private var visivel = false
    private var pedidoGeo: Pair<String, GeolocationPermissions.Callback>? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this).apply { setBackgroundColor(Color.parseColor("#16120F")) }
        setContentView(web)
        with(web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            setGeolocationEnabled(true)
            mediaPlaybackRequiresUserGesture = false // som do aviso de entrega nova
            setSupportMultipleWindows(false)         // target=_blank cai no shouldOverrideUrlLoading
            userAgentString = "$userAgentString SaboreyMotoboyApp/${BuildConfig.VERSION_NAME}"
        }
        web.addJavascriptInterface(Ponte(), "SaboreyMotoboy")
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean {
                val u = req.url
                if ((u.scheme == "https") && u.host == host) return false
                abrirFora(u); return true
            }
            override fun onReceivedError(view: WebView, req: WebResourceRequest, err: WebResourceError) {
                if (!req.isForMainFrame) return
                view.loadDataWithBaseURL(null, SEM_INTERNET, "text/html", "utf-8", null)
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(origin: String, cb: GeolocationPermissions.Callback) {
                if (temGps()) cb.invoke(origin, true, false)
                else { pedidoGeo = origin to cb; pedirPermissoes() }
            }
        }
        pedirPermissoes()
        if (savedInstanceState == null) web.loadUrl(BuildConfig.SERVIDOR + "/motoboy") else web.restoreState(savedInstanceState)
    }

    private fun abrirFora(u: Uri) {
        val i = if (u.scheme == "tel") Intent(Intent.ACTION_DIAL, u) else Intent(Intent.ACTION_VIEW, u)
        try { startActivity(i) } catch (_: Exception) {}
    }

    private fun temGps() = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun pedirPermissoes() {
        val faltam = mutableListOf<String>()
        if (!temGps()) faltam += listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            faltam += Manifest.permission.POST_NOTIFICATIONS
        if (faltam.isNotEmpty() && Build.VERSION.SDK_INT >= 23) requestPermissions(faltam.toTypedArray(), 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        pedidoGeo?.let { (o, cb) -> cb.invoke(o, temGps(), false) }
        pedidoGeo = null
        web.evaluateJavascript("window.dispatchEvent && window.dispatchEvent(new Event('saborey-app'))", null)
    }

    override fun onResume() { super.onResume(); visivel = true; web.onResume() }
    override fun onPause() { visivel = false; super.onPause() } // não pausa o WebView: os avisos de entrega continuam tocando
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); web.saveState(outState) }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { if (web.canGoBack()) web.goBack() else moveTaskToBack(true) }

    /** O que a página do motoboy pode pedir para o aplicativo. */
    inner class Ponte {
        /** Liga/desliga o GPS em segundo plano. Chamado pela página a cada atualização. */
        @JavascriptInterface
        fun rastrear(ligar: Boolean, sessao: String?, base: String?) {
            runOnUiThread {
                if (!ligar) { if (Localizacao.ativo) stopService(Intent(this@MainActivity, Localizacao::class.java)); return@runOnUiThread }
                if (sessao.isNullOrBlank() || !temGps()) return@runOnUiThread
                // Android só deixa ligar serviço de localização com o app na frente; depois ele segue sozinho
                if (!visivel && !Localizacao.ativo) return@runOnUiThread
                val b = if (base != null && Uri.parse(base).host == host) base else BuildConfig.SERVIDOR
                try {
                    ContextCompat.startForegroundService(this@MainActivity,
                        Intent(this@MainActivity, Localizacao::class.java).putExtra("sessao", sessao).putExtra("base", b))
                } catch (_: Exception) {}
            }
        }
        /** "ativo", "sem_permissao" ou "parado": a página mostra o aviso certo. */
        @JavascriptInterface
        fun estado(): String = when { !temGps() -> "sem_permissao"; Localizacao.ativo -> "ativo"; else -> "parado" }
        @JavascriptInterface
        fun pedirPermissao() { runOnUiThread { pedirPermissoes() } }
        @JavascriptInterface
        fun versao(): String = BuildConfig.VERSION_NAME
    }

    companion object {
        private val SEM_INTERNET = """<html><body style="background:#16120F;color:#f6ecdf;font:18px sans-serif;display:flex;flex-direction:column;align-items:center;justify-content:center;height:90vh;text-align:center;gap:16px">
            <div style="font-size:48px">📶</div><div>Sem conexão com a Saborey.</div>
            <button onclick="location.href='""" + BuildConfig.SERVIDOR + """/motoboy'" style="font-size:18px;padding:14px 24px;border-radius:12px;border:0;background:#f0a830;color:#1e1408;font-weight:bold">Tentar de novo</button></body></html>"""
    }
}
