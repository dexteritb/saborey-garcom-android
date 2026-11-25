package br.com.saborey.garcom.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

/** Erro vindo do servidor (mensagem já em português) */
class ErroApi(msg: String, val status: Int = 0, val precisaAparelho: Boolean = false) : Exception(msg)

/** O que fica guardado no aparelho: chave do aparelho cadastrado, sessão e pagamentos ainda não lançados */
class Guarda(ctx: Context) {
    private val sp = ctx.getSharedPreferences("saborey", Context.MODE_PRIVATE)
    var aparelho: String
        get() = sp.getString("aparelho", "") ?: ""
        set(v) = sp.edit().putString("aparelho", v).apply()
    var sessao: String
        get() = sp.getString("sessao", "") ?: ""
        set(v) = sp.edit().putString("sessao", v).apply()
    var pendentes: JSONArray
        get() = try { JSONArray(sp.getString("pendentes", "[]")) } catch (e: Exception) { JSONArray() }
        set(v) = sp.edit().putString("pendentes", v.toString()).apply()

    fun guardarPendente(p: JSONObject) { val l = pendentes; l.put(p); pendentes = l }
    fun tirarPendente(ref: String) {
        val l = pendentes; val n = JSONArray()
        for (i in 0 until l.length()) { val x = l.getJSONObject(i); if (x.optString("ref") != ref) n.put(x) }
        pendentes = n
    }
}

/** Chamadas ao servidor da Saborey (as mesmas que o app do garçom da web usa) */
class Api(private val guarda: Guarda) {
    private val base = BuildConfig.SERVIDOR

    suspend fun get(caminho: String): Any = chamar("GET", caminho, null)
    suspend fun post(caminho: String, corpo: JSONObject = JSONObject()): Any = chamar("POST", caminho, corpo)
    suspend fun obj(caminho: String, corpo: JSONObject? = null): JSONObject =
        (if (corpo == null) get(caminho) else post(caminho, corpo)) as? JSONObject ?: JSONObject()

    private suspend fun chamar(metodo: String, caminho: String, corpo: JSONObject?): Any = withContext(Dispatchers.IO) {
        val c = (URL(base + caminho).openConnection() as HttpURLConnection)
        try {
            c.requestMethod = metodo
            c.connectTimeout = 10000
            c.readTimeout = 30000
            c.setRequestProperty("Content-Type", "application/json")
            c.setRequestProperty("Accept", "application/json")
            c.setRequestProperty("x-sessao", guarda.sessao)
            c.setRequestProperty("User-Agent", "SaboreyGarcom/" + BuildConfig.VERSION_NAME + " (Android)")
            if (corpo != null) {
                c.doOutput = true
                c.outputStream.use { it.write(corpo.toString().toByteArray(Charsets.UTF_8)) }
            }
            val st = c.responseCode
            val txt = (if (st >= 400) c.errorStream else c.inputStream)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            val t = txt.trim()
            val dado: Any = when {
                t.startsWith("[") -> JSONArray(t)
                t.startsWith("{") -> JSONObject(t)
                t == "null" || t.isEmpty() -> JSONObject.NULL
                else -> JSONObject()
            }
            if (st >= 400) {
                val o = dado as? JSONObject
                throw ErroApi(o?.optString("erro")?.takeIf { it.isNotEmpty() } ?: "Erro $st no servidor.", st, o?.optBoolean("precisaAparelho") == true)
            }
            dado
        } catch (e: ErroApi) {
            throw e
        } catch (e: Exception) {
            throw ErroApi("Sem conexão com o sistema. Confira a internet.", 0)
        } finally {
            c.disconnect()
        }
    }
}

// ---------- ajudantes ----------
private val moeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
fun brl(centavos: Long): String = moeda.format(centavos / 100.0)
fun brl(centavos: Int): String = brl(centavos.toLong())
fun centavos(txt: String): Long {
    val s = txt.replace(Regex("[^0-9,.]"), "")
    if (s.isEmpty()) return 0
    val n = if (s.contains(",")) s.replace(".", "").replace(",", ".") else s
    return Math.round((n.toDoubleOrNull() ?: 0.0) * 100)
}
fun emReais(c: Long): String = String.format(Locale("pt", "BR"), "%.2f", c / 100.0)
fun semTags(s: String): String = s.replace(Regex("<[^>]+>"), "")
fun JSONArray.objs(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
fun JSONArray?.textos(): List<String> = if (this == null) emptyList() else (0 until length()).map { optString(it) }
fun novaRef(): String = "a" + java.lang.Long.toString(System.currentTimeMillis(), 36) + java.util.UUID.randomUUID().toString().replace("-", "").take(8)

val FORMAS = linkedMapOf("credito" to "Crédito", "debito" to "Débito", "pix" to "Pix", "vale" to "Vale-refeição", "dinheiro" to "Dinheiro")
val STATUS = mapOf("novo" to "Na cozinha", "preparo" to "Preparando", "pronto" to "Pronto p/ levar", "entregue" to "Entregue", "cancelado" to "Cancelado")
