package br.com.saborey.garcom.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

/**
 * App do garçom da Saborey para a maquininha Stone — telas nativas (sem WebView, regra PCI da Stone).
 * Conversa com o sistema da Saborey (HTTPS) e cobra no app de pagamento da Stone pelo Deeplink:
 *   payment-app://pay?return_scheme=saboreygarcom&amount=<centavos>&transaction_type=...
 *   resposta: saboreygarcom://pay-response?code=0&authorization_code=...&itk=...&brand=...
 */
class MainActivity : ComponentActivity() {

    private var espera: CompletableDeferred<JSONObject>? = null
    private var voltouSemResposta = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val estado = Estado(this)
        setContent { AppGarcom(estado) }
    }

    /** Cobra no app de pagamento da Stone e espera a resposta. Resultado: aprovado, autorizacao, nsu, bandeira... */
    suspend fun cobrarNaStone(valor: Long, forma: String, parcelas: Int): JSONObject {
        if (espera != null) return falha("Já tem uma cobrança em andamento.")
        val tipo = when (forma) { "credito" -> "CREDIT"; "debito" -> "DEBIT"; "pix" -> "PIX"; "vale" -> "VOUCHER"; else -> null }
        val uri = Uri.Builder()
            .scheme("payment-app")
            .authority("pay")
            .appendQueryParameter("return_scheme", BuildConfig.ESQUEMA_RETORNO)
            .appendQueryParameter("amount", valor.toString())     // centavos
            .appendQueryParameter("editable_amount", "0")          // o valor não pode ser mudado na maquininha
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
        val d = CompletableDeferred<JSONObject>()
        espera = d
        voltouSemResposta = false
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            espera = null
            return falha("O app de pagamento da Stone não está neste aparelho.")
        }
        // espera a resposta (no máximo 10 minutos)
        val r = withTimeoutOrNull(10 * 60 * 1000L) { d.await() }
        espera = null
        return r ?: falha("A maquininha não respondeu.").put("cancelado", true)
    }

    // resposta do app de pagamento
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val dados = intent.data ?: return
        if (dados.host != "pay-response") return
        val d = espera ?: return
        val r = JSONObject()
        try {
            val code = dados.getQueryParameter("code")
            Log.i("SaboreyPOS", "Resposta da Stone: code=$code")
            if (code == "0") {
                r.put("aprovado", true)
                r.put("autorizacao", dados.getQueryParameter("authorization_code") ?: "")
                r.put("nsu", dados.getQueryParameter("itk") ?: "")          // Initiator Transaction Key
                r.put("atk", dados.getQueryParameter("atk") ?: "")          // Authorizer Transaction Key
                r.put("bandeira", dados.getQueryParameter("brand") ?: "")
                r.put("parcelas", (dados.getQueryParameter("installment_count") ?: "1").toIntOrNull() ?: 1)
            } else {
                r.put("aprovado", false)
                r.put("cancelado", code == "2")                             // 2 = o operador encerrou o pagamento
                r.put("motivo", dados.getQueryParameter("message") ?: "Pagamento não aprovado (código $code).")
            }
        } catch (e: Exception) {
            r.put("aprovado", false).put("motivo", "Erro ao ler a resposta da maquininha.")
        }
        d.complete(r)
    }

    // voltou para o app sem resposta (apertou voltar no app de pagamento): considera cancelado
    override fun onResume() {
        super.onResume()
        val d = espera ?: return
        window.decorView.postDelayed({
            if (espera === d && !d.isCompleted) d.complete(falha("Pagamento não concluído.").put("cancelado", true))
        }, 1500)
    }

    private fun falha(msg: String) = JSONObject().put("aprovado", false).put("motivo", msg)
}

/** pequeno atraso para não martelar o servidor */
suspend fun pausa(ms: Long) = delay(ms)
