package br.com.saborey.garcom.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** Em que tela o app está */
sealed class Tela {
    object Carregando : Tela()
    data class Cadastro(val msg: String = "") : Tela()
    data class Login(val msg: String = "") : Tela()
    object Mesas : Tela()
    data class Conta(val comandaId: Long) : Tela()
    data class Cardapio(val mesa: String, val comandaId: Long?) : Tela()
}

/** Item escolhido no cardápio, ainda não enviado para a cozinha */
data class ItemCarrinho(
    val id: String, val o: Int, val f: Int, val x: List<String>, var q: Int, val obs: String,
    val unit: Long, val nome: String, val opcao: String?, val sabor: String?
)

class Estado(val activity: MainActivity) {
    val guarda = Guarda(activity)
    val api = Api(guarda)

    var tela by mutableStateOf<Tela>(Tela.Carregando)
    var aviso by mutableStateOf<String?>(null)          // mensagem rápida no rodapé
    var erroAviso by mutableStateOf(false)
    var usuario by mutableStateOf<JSONObject?>(null)
    var cfg by mutableStateOf(JSONObject())
    var produtos by mutableStateOf<List<JSONObject>>(emptyList())
    var categorias by mutableStateOf<List<String>>(emptyList())
    val carrinho = mutableStateListOf<ItemCarrinho>()
    var nomeAparelho by mutableStateOf<String?>(null)

    fun avisar(msg: String, erro: Boolean = false) { aviso = msg; erroAviso = erro }

    fun pode(perm: String): Boolean {
        val u = usuario ?: return false
        if (u.optString("papel") == "admin") return true
        return u.optJSONArray("permissoes").textos().contains(perm)
    }

    /** Abre o app: sem aparelho cadastrado vai para o cadastro; com sessão válida vai para as mesas */
    suspend fun iniciar() {
        if (guarda.aparelho.isEmpty()) { tela = Tela.Cadastro(); return }
        try {
            val r = api.obj("/api/aparelho/eu", JSONObject().put("aparelho", guarda.aparelho))
            val a = r.optJSONObject("aparelho")
            if (a == null) { guarda.aparelho = ""; tela = Tela.Cadastro("Este aparelho não está mais cadastrado. Peça um código novo ao administrador."); return }
            nomeAparelho = a.optString("nome")
        } catch (e: ErroApi) { tela = Tela.Login(e.message ?: ""); return }
        if (guarda.sessao.isEmpty()) { tela = Tela.Login(); return }
        try { carregar(); tela = Tela.Mesas } catch (e: ErroApi) { guarda.sessao = ""; tela = Tela.Login(if (e.status == 401) "" else e.message ?: "") }
    }

    suspend fun cadastrar(codigo: String) {
        val r = api.obj("/api/aparelho/registrar", JSONObject().put("codigo", codigo))
        guarda.aparelho = r.optString("token")
        nomeAparelho = r.optString("nome")
        tela = Tela.Login("Aparelho cadastrado: ${r.optString("nome")}. Agora digite seu PIN.")
    }

    suspend fun entrar(pin: String) {
        try {
            val r = api.obj("/api/login", JSONObject().put("pin", pin).put("tela", "garcom").put("aparelho", guarda.aparelho))
            guarda.sessao = r.optString("token")
            usuario = r.optJSONObject("usuario")
            carregar()
            if (!pode("venda_mesa")) { guarda.sessao = ""; tela = Tela.Login("Seu usuário não pode vender nas mesas."); return }
            tela = Tela.Mesas
        } catch (e: ErroApi) {
            if (e.precisaAparelho) { guarda.aparelho = ""; tela = Tela.Cadastro(e.message ?: "") } else throw e
        }
    }

    suspend fun carregar() {
        val eu = api.obj("/api/sis/eu")
        usuario = eu.optJSONObject("usuario")
        cfg = api.obj("/api/sis/config")
        val p = api.obj("/api/sis/produtos")
        val ativos = p.optJSONArray("produtos")?.objs()?.filter { it.optBoolean("ativo", true) && it.optInt("ativo", 1) != 0 } ?: emptyList()
        produtos = ativos
        categorias = p.optJSONArray("categorias").textos().filter { c -> ativos.any { it.optString("categoria") == c } }
    }

    suspend fun sair() {
        try { api.post("/api/sair") } catch (_: Exception) {}
        guarda.sessao = ""; usuario = null; tela = Tela.Login()
    }

    /** Erro 401 em qualquer tela: volta para o PIN */
    fun tratar(e: ErroApi) {
        if (e.status == 401) { guarda.sessao = ""; tela = Tela.Login("Sua sessão expirou. Entre de novo."); return }
        if (e.precisaAparelho) { guarda.aparelho = ""; tela = Tela.Cadastro(e.message ?: ""); return }
        avisar(e.message ?: "Erro.", true)
    }

    // ---------- cardápio ----------
    fun nomeProd(p: JSONObject): String {
        val c = p.optString("categoria")
        return (if (c == "Lanches no Prato") "Prato " else if (c == "Pizzas") "Pizza " else "") + semTags(p.optString("nome"))
    }
    fun opcoes(p: JSONObject): List<Pair<String, Long>> =
        p.optJSONArray("opcoes")?.objs()?.map { it.optString("n") to it.optLong("p") } ?: emptyList()
    fun sabores(p: JSONObject): List<String> = p.optJSONArray("sabores").textos()
    fun precoTexto(p: JSONObject): String {
        val o = opcoes(p)
        return if (o.isNotEmpty()) brl(o.minOf { it.second }) + "+" else brl(p.optLong("preco"))
    }
    fun maxSabores(tamanho: String): Int = cfg.optJSONObject("pizzaSabores")?.optInt(tamanho, 1) ?: 1

    /** preço da pizza meio a meio = média dos sabores no mesmo tamanho (igual ao sistema) */
    fun precoItem(p: JSONObject, o: Int, x: List<String>): Long {
        val ops = opcoes(p)
        if (ops.isEmpty()) return p.optLong("preco")
        if (x.isEmpty()) return ops[o].second
        val tam = ops[o].first
        val todos = listOf(p) + x.mapNotNull { id -> produtos.find { it.optString("id") == id } }
        val precos = todos.map { z -> opcoes(z).find { it.first == tam }?.second ?: 0L }
        return Math.round(precos.sum().toDouble() / precos.size)
    }

    fun adicionar(p: JSONObject, o: Int, f: Int, q: Int, obs: String, x: List<String>) {
        val unit = precoItem(p, o, x)
        val nome = if (x.isNotEmpty()) {
            val frac = if (x.size == 1) "1/2" else "1/3"
            "Pizza " + (listOf(p) + x.mapNotNull { id -> produtos.find { it.optString("id") == id } }).joinToString(" + ") { "$frac ${semTags(it.optString("nome"))}" }
        } else nomeProd(p)
        val ops = opcoes(p); val sab = sabores(p)
        val igual = carrinho.indexOfFirst { it.id == p.optString("id") && it.o == o && it.f == f && it.obs == obs && it.x == x }
        if (igual >= 0) { val it = carrinho[igual]; carrinho[igual] = it.copy(q = it.q + q) }
        else carrinho.add(ItemCarrinho(p.optString("id"), o, f, x, q, obs, unit, nome, ops.getOrNull(o)?.first, sab.getOrNull(f)))
        avisar("+$q $nome")
    }

    suspend fun enviarCarrinho(mesa: String, comandaId: Long?): Long {
        val itens = JSONArray()
        carrinho.forEach { i ->
            itens.put(JSONObject().put("id", i.id).put("o", i.o).put("f", i.f).put("x", JSONArray(i.x)).put("q", i.q).put("obs", i.obs))
        }
        val corpo = JSONObject().put("tipo", "mesa").put("mesa", mesa).put("itens", itens)
        if (comandaId != null) corpo.put("comanda_id", comandaId)
        val p = api.obj("/api/sis/pedidos", corpo)
        carrinho.clear()
        val imp = p.optJSONObject("impressao")
        var falhou = false
        imp?.keys()?.forEach { k -> if (imp.optJSONObject(k)?.optString("status") == "erro") falhou = true }
        avisar("Pedido ${p.optInt("numero")} enviado para a cozinha" + if (falhou) " (impressora falhou: avise o caixa)." else ".", falhou)
        return p.optLong("comanda_id")
    }

    // ---------- pagamentos (o que foi aprovado fica guardado até o sistema confirmar) ----------
    suspend fun lancarPagamento(pg: JSONObject): JSONObject {
        val r = api.obj("/api/sis/garcom/comandas/${pg.optLong("comanda")}/pagar", JSONObject()
            .put("forma", pg.optString("forma")).put("valor", pg.optLong("valor")).put("recebido", pg.optLong("recebido"))
            .put("ref", pg.optString("ref")).put("transacao", pg.optJSONObject("transacao") ?: JSONObject()))
        guarda.tirarPendente(pg.optString("ref"))
        return r
    }

    suspend fun lancarPendentes(): String? {
        val l = guarda.pendentes.objs()
        var ok = 0
        for (pg in l) {
            try { lancarPagamento(pg); ok++ } catch (e: ErroApi) {
                return "Mesa ${pg.optString("mesa")} · ${FORMAS[pg.optString("forma")]} ${brl(pg.optLong("valor"))}: ${e.message}. O cartão já foi cobrado: mostre ao caixa."
            }
        }
        if (ok > 0) avisar("$ok pagamento(s) lançado(s).")
        return null
    }
}
