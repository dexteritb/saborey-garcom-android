package br.com.saborey.garcom.app

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

/**
 * App do garçom da Saborey para a maquininha.
 *
 * A tela é o próprio app do garçom (página web do sistema, por HTTPS). O que este app faz a mais:
 * entrega para a página o objeto window.SaboreyPOSNative, que cobra no app de pagamento da Stone
 * pelo Deeplink (payment-app://pay) e devolve o resultado para a página.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var web: WebView
    private val principal = Handler(Looper.getMainLooper())

    // cobrança em andamento (só uma por vez)
    private var pendente: String? = null
    private var chegouResposta = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this).apply {
            setBackgroundColor(Color.parseColor("#16120F"))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true          // guarda o login e o cadastro do aparelho
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.userAgentString = settings.userAgentString + " SaboreyGarcom/" + BuildConfig.VERSION_NAME
            webChromeClient = WebChromeClient()
            webViewClient = Cliente()
            addJavascriptInterface(Ponte(), "SaboreyPOSNative")
        }
        setContentView(web)
        if (savedInstanceState != null) web.restoreState(savedInstanceState) else web.loadUrl(BuildConfig.URL_APP)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    // ------------------------------------------------------------------
    // Ponte página <-> app (a página chama window.SaboreyPOSNative.cobrar(json))
    // ------------------------------------------------------------------
    inner class Ponte {
        @JavascriptInterface
        fun versao(): String = BuildConfig.VERSION_NAME

        @JavascriptInterface
        fun cobrar(json: String) {
            principal.post { iniciarCobranca(json) }
        }
    }

    private fun iniciarCobranca(json: String) {
        val d = try { JSONObject(json) } catch (e: Exception) { JSONObject() }
        val id = d.optString("id")
        if (pendente != null) { responder(id, falha("Já tem uma cobrança em andamento.")); return }
        val valor = d.optLong("valor", 0)
        if (valor <= 0) { responder(id, falha("Valor inválido.")); return }
        val tipo = when (d.optString("forma")) {
            "credito" -> "CREDIT"
            "debito" -> "DEBIT"
            "pix" -> "PIX"
            "vale" -> "VOUCHER"
            else -> null
        }
        val parcelas = d.optInt("parcelas", 1)
        val uri = Uri.Builder()
            .scheme("payment-app")
            .authority("pay")
            .appendQueryParameter("return_scheme", BuildConfig.ESQUEMA_RETORNO)
            .appendQueryParameter("amount", valor.toString())        // centavos
            .appendQueryParameter("editable_amount", "0")             // o garçom não muda o valor na maquininha
            .apply {
                if (tipo != null) appendQueryParameter("transaction_type", tipo)
                if (tipo == "CREDIT" && parcelas >= 2) {
                    appendQueryParameter("installment_type", "MERCHANT")
                    appendQueryParameter("installment_count", parcelas.toString())
                } else if (tipo == "CREDIT" || tipo == "DEBIT") {
                    appendQueryParameter("installment_type", "NONE")
                }
            }
            .build()
        pendente = id
        chegouResposta = false
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            pendente = null
            responder(id, falha("O app de pagamento da Stone não está neste aparelho."))
        }
    }

    // resposta do app de pagamento: <esquema>://pay-response?code=0&authorization_code=...&itk=...
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val dados = intent.data ?: return
        if (dados.host != "pay-response") return
        chegouResposta = true
        val id = pendente ?: return
        pendente = null
        val r = JSONObject()
        try {
            val code = dados.getQueryParameter("code")
            Log.i("SaboreyPOS", "Resposta da Stone: code=$code")
            if (code == "0") {
                r.put("aprovado", true)
                r.put("autorizacao", dados.getQueryParameter("authorization_code") ?: "")
                r.put("nsu", dados.getQueryParameter("itk") ?: "")           // Initiator Transaction Key
                r.put("atk", dados.getQueryParameter("atk") ?: "")           // Authorizer Transaction Key
                r.put("bandeira", dados.getQueryParameter("brand") ?: "")
                r.put("parcelas", (dados.getQueryParameter("installment_count") ?: "1").toIntOrNull() ?: 1)
                r.put("tipo", dados.getQueryParameter("type") ?: "")
                r.put("autorizada_em", dados.getQueryParameter("authorization_date_time") ?: "")
            } else {
                r.put("aprovado", false)
                r.put("cancelado", code == "2")                              // 2 = o operador encerrou o pagamento
                r.put("motivo", dados.getQueryParameter("message") ?: "Pagamento não aprovado (código $code).")
            }
        } catch (e: Exception) {
            r.put("aprovado", false)
            r.put("motivo", "Erro ao ler a resposta da maquininha: ${e.message}")
        }
        responder(id, r)
    }

    // voltou para o app sem resposta (ex.: apertou voltar no app de pagamento): considera cancelado
    override fun onResume() {
        super.onResume()
        val id = pendente ?: return
        principal.postDelayed({
            if (pendente == id && !chegouResposta) {
                pendente = null
                responder(id, JSONObject().put("aprovado", false).put("cancelado", true).put("motivo", "Pagamento não concluído."))
            }
        }, 1500)
    }

    private fun falha(msg: String) = JSONObject().put("aprovado", false).put("motivo", msg)

    private fun responder(id: String, r: JSONObject) {
        val js = "window.__saboreyPosResposta && window.__saboreyPosResposta(${JSONObject.quote(id)}, $r)"
        web.evaluateJavascript(js, null)
    }

    // ------------------------------------------------------------------
    // Navegação: só o endereço do app; sem internet mostra uma tela para tentar de novo
    // ------------------------------------------------------------------
    inner class Cliente : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean {
            val u = req.url
            val base = Uri.parse(BuildConfig.URL_APP)
            if (u.scheme == "https" && u.host == base.host) return false
            // WhatsApp, telefone, e-mail (Fale conosco): abre no app certo
            return try { startActivity(Intent(Intent.ACTION_VIEW, u)); true } catch (e: Exception) { true }
        }

        override fun onReceivedError(view: WebView, req: WebResourceRequest, erro: WebResourceError) {
            if (!req.isForMainFrame) return
            view.loadDataWithBaseURL(null, telaSemInternet(erro.description?.toString() ?: ""), "text/html", "utf-8", null)
        }
    }

    private fun telaSemInternet(detalhe: String) = """
        <html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
        <body style="background:#16120F;color:#f6ecdf;font-family:sans-serif;text-align:center;padding:40px 20px">
        <h2>Sem conexão com o sistema</h2>
        <p style="color:#b7a693">Confira a internet da maquininha e tente de novo.</p>
        <p style="color:#7d6e60;font-size:12px">${detalhe.replace("<", "")}</p>
        <button onclick="location.href='${BuildConfig.URL_APP}'" style="font-size:18px;padding:14px 28px;border-radius:12px;border:0;background:#f0a830;color:#1e1408;font-weight:bold">Tentar de novo</button>
        </body></html>
    """.trimIndent()

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (web.canGoBack()) { web.goBack(); return }
        AlertDialog.Builder(this)
            .setMessage("Sair do app do garçom?")
            .setPositiveButton("Sair") { _, _ -> finish() }
            .setNegativeButton("Voltar", null)
            .show()
    }

    override fun onDestroy() {
        web.removeJavascriptInterface("SaboreyPOSNative")
        web.destroy()
        super.onDestroy()
    }
}
