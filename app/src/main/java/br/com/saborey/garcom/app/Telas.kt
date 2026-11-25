@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.com.saborey.garcom.app

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

// ---------- cores (mesmas do sistema) ----------
val Fundo = Color(0xFF16120F)
val Painel = Color(0xFF1F1A16)
val Painel2 = Color(0xFF29221D)
val Tinta = Color(0xFFF6ECDF)
val Apagado = Color(0xFFB7A693)
val Fraco = Color(0xFF7D6E60)
val Destaque = Color(0xFFF0A830)
val TintaDestaque = Color(0xFF1E1408)
val Ok = Color(0xFF5CC98A)
val Ruim = Color(0xFFFF6B5E)
val Roxo = Color(0xFFA98BFF)

private val esquema = darkColorScheme(
    primary = Destaque, onPrimary = TintaDestaque, background = Fundo, onBackground = Tinta,
    surface = Painel, onSurface = Tinta, surfaceVariant = Painel2, onSurfaceVariant = Apagado, error = Ruim,
)

@Composable
fun AppGarcom(e: Estado) {
    MaterialTheme(colorScheme = esquema) {
        Surface(Modifier.fillMaxSize(), color = Fundo) {
            LaunchedEffect(Unit) { e.iniciar() }
            Box(Modifier.fillMaxSize().systemBarsPadding()) {
                when (val t = e.tela) {
                    is Tela.Carregando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    is Tela.Cadastro -> TelaCodigo(e, t.msg)
                    is Tela.Login -> TelaPin(e, t.msg)
                    is Tela.Mesas -> TelaMesas(e)
                    is Tela.Conta -> TelaConta(e, t.comandaId)
                    is Tela.Cardapio -> TelaCardapio(e, t.mesa, t.comandaId)
                }
                Aviso(e, Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
fun Aviso(e: Estado, mod: Modifier) {
    val msg = e.aviso ?: return
    LaunchedEffect(msg) { delay(if (e.erroAviso) 4500 else 2200); if (e.aviso == msg) e.aviso = null }
    Box(mod.padding(16.dp).padding(bottom = 70.dp).background(if (e.erroAviso) Ruim else Tinta, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(msg, color = if (e.erroAviso) Color.White else Fundo, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

// ---------- peças ----------
@Composable
fun Topo(titulo: String, sub: String? = null, voltar: (() -> Unit)? = null, acoes: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().background(Painel).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (voltar != null) { OutlinedButton(onClick = voltar, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(44.dp)) { Text("←", fontSize = 20.sp) }; Spacer(Modifier.width(8.dp)) }
        Column(Modifier.weight(1f)) {
            Text(titulo, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub != null) Text(sub, fontSize = 13.sp, color = Apagado, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        acoes()
    }
}

@Composable
fun Botao(texto: String, mod: Modifier = Modifier, cor: Color = Painel2, tinta: Color = Tinta, ativo: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = ativo, modifier = mod.heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = cor, contentColor = tinta, disabledContainerColor = cor.copy(alpha = .4f), disabledContentColor = tinta.copy(alpha = .5f))) {
        Text(texto, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun Etiqueta(texto: String, cor: Color) {
    Text(texto, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cor,
        modifier = Modifier.border(1.dp, cor.copy(alpha = .6f), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp))
}

@Composable
fun Folha(titulo: String, fechar: () -> Unit, conteudo: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = fechar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxWidth(.94f).heightIn(max = 640.dp).background(Painel, RoundedCornerShape(18.dp)).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(titulo, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            conteudo()
        }
    }
}

@Composable
fun Teclado(valor: String, max: Int, ok: String, aoOk: () -> Unit, mudar: (String) -> Unit) {
    val teclas = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", ok)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        teclas.chunked(3).forEach { linha ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                linha.forEach { t ->
                    val ehOk = t == ok
                    Botao(t, Modifier.weight(1f).height(64.dp), cor = if (ehOk) Destaque else Painel2, tinta = if (ehOk) TintaDestaque else Tinta) {
                        when (t) { "⌫" -> mudar(valor.dropLast(1)); ok -> aoOk(); else -> if (valor.length < max) mudar(valor + t) }
                    }
                }
            }
        }
    }
}

// ---------- cadastro do aparelho ----------
@Composable
fun TelaCodigo(e: Estado, msg: String) {
    var cod by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf("") }
    var fale by remember { mutableStateOf(false) }
    val esc = rememberCoroutineScope()
    val enviar: () -> Unit = { esc.launch { try { e.cadastrar(cod) } catch (x: ErroApi) { cod = ""; erro = x.message ?: "" } } }
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
        Text("Saborey", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Destaque)
        Text("Cadastrar este aparelho", fontSize = 18.sp)
        Text(msg.ifEmpty { "Peça ao administrador o código em Usuários → Aparelhos e digite aqui." }, color = Apagado, textAlign = TextAlign.Center)
        Text(cod.padEnd(6, '·'), fontSize = 34.sp, letterSpacing = 8.sp)
        Text(erro, color = Ruim, textAlign = TextAlign.Center)
        Teclado(cod, 6, "OK", { if (cod.length == 6) enviar() }) { cod = it; erro = ""; if (it.length == 6) { cod = it; enviar() } }
        TextButton(onClick = { fale = true }) { Text("Fale conosco", color = Apagado) }
    }
    if (fale) FaleConosco(e) { fale = false }
}

// ---------- PIN ----------
@Composable
fun TelaPin(e: Estado, msg: String) {
    var pin by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf(msg) }
    var ocupado by remember { mutableStateOf(false) }
    var fale by remember { mutableStateOf(false) }
    val esc = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
        Text("Saborey", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Destaque)
        Text("Garçom · digite seu PIN", color = Apagado)
        e.nomeAparelho?.let { Text("📱 $it", color = Fraco, fontSize = 13.sp) }
        Text("•".repeat(pin.length), fontSize = 34.sp, letterSpacing = 8.sp, modifier = Modifier.heightIn(min = 44.dp))
        Text(erro, color = if (erro.startsWith("Aparelho cadastrado")) Ok else Ruim, textAlign = TextAlign.Center)
        Teclado(pin, 8, "Entrar", {
            if (ocupado || pin.isEmpty()) return@Teclado
            ocupado = true
            esc.launch { try { e.entrar(pin) } catch (x: ErroApi) { pin = ""; erro = x.message ?: "" } finally { ocupado = false } }
        }) { pin = it; erro = "" }
        Row { TextButton(onClick = { e.tela = Tela.Cadastro() }) { Text("Cadastrar este aparelho", color = Apagado) }; TextButton(onClick = { fale = true }) { Text("Fale conosco", color = Apagado) } }
    }
    if (fale) FaleConosco(e) { fale = false }
}

// ---------- Fale conosco (exigência da Stone) ----------
@Composable
fun FaleConosco(e: Estado, fechar: () -> Unit) {
    var c by remember { mutableStateOf<JSONObject?>(null) }
    LaunchedEffect(Unit) { c = try { e.api.obj("/api/suporte") } catch (_: Exception) { JSONObject().put("nome", "Saborey") } }
    val ctx = e.activity
    fun abrir(u: String) { try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))) } catch (_: Exception) { e.avisar("Não deu para abrir.", true) } }
    fun fone(n: String): String { val d = n.filter { it.isDigit() }; val x = if (d.startsWith("55") && d.length > 11) d.drop(2) else d; return if (x.length >= 10) "(${x.take(2)}) ${x.substring(2, x.length - 4)}-${x.takeLast(4)}" else d }
    Folha("Fale conosco", fechar) {
        val s = c
        if (s == null) { CircularProgressIndicator() } else {
            Text("Precisa de ajuda com o app ou com um pagamento? Fale com ${s.optString("nome", "Saborey")}.", color = Apagado)
            val wa = s.optString("whatsapp").filter { it.isDigit() }
            if (wa.isNotEmpty()) Botao("WhatsApp · ${fone(wa)}", Modifier.fillMaxWidth(), Destaque, TintaDestaque) { abrir("https://wa.me/" + (if (wa.startsWith("55")) wa else "55$wa")) }
            val tel = s.optString("telefone").filter { it.isDigit() }
            if (tel.isNotEmpty()) Botao("Ligar · ${fone(tel)}", Modifier.fillMaxWidth()) { abrir("tel:+" + (if (tel.startsWith("55")) tel else "55$tel")) }
            val mail = s.optString("email")
            if (mail.isNotEmpty()) Botao("E-mail · $mail", Modifier.fillMaxWidth()) { abrir("mailto:$mail") }
            if (s.optString("horario").isNotEmpty()) Text("Atendimento: ${s.optString("horario")}", color = Apagado)
            if (s.optString("obs").isNotEmpty()) Text(s.optString("obs"), color = Apagado)
            Text("Saborey Garçom · versão ${BuildConfig.VERSION_NAME}", color = Fraco, fontSize = 12.sp)
        }
        TextButton(onClick = fechar, modifier = Modifier.fillMaxWidth()) { Text("Fechar") }
    }
}

// ---------- mesas ----------
@Composable
fun TelaMesas(e: Estado) {
    var mesas by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var escolher by remember { mutableStateOf<JSONObject?>(null) }
    var fale by remember { mutableStateOf(false) }
    var sair by remember { mutableStateOf(false) }
    var problemaPend by remember { mutableStateOf<String?>(null) }
    var nPend by remember { mutableStateOf(e.guarda.pendentes.length()) }
    val esc = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        while (true) {   // atualiza sozinho a cada 5 segundos
            try { mesas = (e.api.get("/api/sis/mesas") as? org.json.JSONArray)?.objs() ?: mesas } catch (x: ErroApi) { if (x.status == 401) { e.tratar(x); break } }
            nPend = e.guarda.pendentes.length()
            delay(5000)
        }
    }
    BackHandler { sair = true }
    val ocupadas = mesas.count { !it.optBoolean("livre") }
    Column(Modifier.fillMaxSize()) {
        Topo("Mesas", "${e.usuario?.optString("nome") ?: ""} · $ocupadas ocupada${if (ocupadas == 1) "" else "s"}") {
            TextButton(onClick = { fale = true }) { Text("Ajuda", color = Apagado) }
            TextButton(onClick = { sair = true }) { Text("Sair", color = Apagado) }
        }
        if (nPend > 0) {
            Column(Modifier.fillMaxWidth().padding(12.dp).background(Color(0xFF3A1814), RoundedCornerShape(12.dp)).border(1.dp, Ruim.copy(alpha = .5f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                Text("$nPend pagamento(s) aprovado(s) na maquininha ainda não lançado(s) no sistema.", fontWeight = FontWeight.Bold)
                problemaPend?.let { Text(it, color = Apagado, fontSize = 13.sp) }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Botao("Lançar agora", cor = Destaque, tinta = TintaDestaque) { esc.launch { problemaPend = e.lancarPendentes(); nPend = e.guarda.pendentes.length() } }
                    if (problemaPend != null) Botao("Já resolvido no caixa") { e.guarda.pendentes = org.json.JSONArray(); nPend = 0; problemaPend = null }
                }
            }
        }
        LazyVerticalGrid(GridCells.Adaptive(96.dp), Modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(mesas, key = { it.optString("mesa") }) { m ->
                val livre = m.optBoolean("livre")
                val conf = m.optInt("a_conferir") > 0
                val pronto = m.optInt("prontos") > 0
                val (bg, borda) = when {
                    livre -> Color.Transparent to Fraco
                    conf -> Color(0xFF2A1F3D) to Roxo
                    pronto -> Color(0xFF183024) to Ok
                    else -> Color(0xFF2F2418) to Destaque.copy(alpha = .6f)
                }
                Column(Modifier.heightIn(min = 92.dp).background(bg, RoundedCornerShape(14.dp)).border(1.dp, borda, RoundedCornerShape(14.dp))
                    .clickable {
                        if (livre) { e.carrinho.clear(); e.tela = Tela.Cardapio(m.optString("mesa"), null); return@clickable }
                        val contas = m.optJSONArray("contas")?.objs() ?: emptyList()
                        if (contas.size <= 1) e.tela = Tela.Conta(contas.firstOrNull()?.optLong("comanda_id") ?: m.optLong("comanda_id")) else escolher = m
                    }.padding(10.dp)) {
                    Text(m.optString("mesa"), fontSize = 26.sp, fontWeight = FontWeight.Black, color = if (livre) Apagado else Tinta)
                    if (livre) Text("livre", fontSize = 12.sp, color = Apagado) else {
                        Text(brl(m.optLong("total")), fontWeight = FontWeight.Bold)
                        Text(when {
                            conf -> "💵 levar ${brl(m.optLong("a_conferir_valor"))} ao caixa"
                            pronto -> "✅ pronto p/ levar"
                            m.optInt("na_cozinha") > 0 -> "🔥 ${m.optInt("na_cozinha")} na cozinha"
                            (m.optJSONArray("contas")?.length() ?: 0) > 1 -> "${m.optJSONArray("contas")?.length()} contas"
                            else -> "aberta " + m.optString("aberta_em").drop(11).take(5)
                        }, fontSize = 12.sp, color = Apagado)
                    }
                }
            }
        }
    }
    escolher?.let { m ->
        Folha("Mesa ${m.optString("mesa")} · qual conta?", { escolher = null }) {
            m.optJSONArray("contas")?.objs()?.forEach { c ->
                Botao("${c.optString("nome")}  ·  ${brl(c.optLong("total"))}", Modifier.fillMaxWidth()) { escolher = null; e.tela = Tela.Conta(c.optLong("comanda_id")) }
            }
            TextButton(onClick = { escolher = null }, modifier = Modifier.fillMaxWidth()) { Text("Voltar") }
        }
    }
    if (fale) FaleConosco(e) { fale = false }
    if (sair) AlertDialog(onDismissRequest = { sair = false }, title = { Text("Sair do app?") },
        confirmButton = { TextButton(onClick = { sair = false; esc.launch { e.sair() } }) { Text("Sair") } },
        dismissButton = { TextButton(onClick = { sair = false }) { Text("Voltar") } })
}

// ---------- conta da mesa ----------
fun minutosDesde(dataHora: String, agoraServidor: Long): Long {
    return try {
        val f = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        val t = f.parse(dataHora)?.time ?: return 0
        maxOf(0, (agoraServidor - t) / 60000)
    } catch (_: Exception) { 0 }
}

@Composable
fun TelaConta(e: Estado, comandaId: Long) {
    var c by remember { mutableStateOf<JSONObject?>(null) }
    var agora by remember { mutableStateOf(System.currentTimeMillis()) }
    var cobrar by remember { mutableStateOf(false) }
    var fechar by remember { mutableStateOf<JSONObject?>(null) }
    var levar by remember { mutableStateOf<JSONObject?>(null) }
    var mais by remember { mutableStateOf(false) }
    var novaConta by remember { mutableStateOf(false) }
    var recarregar by remember { mutableStateOf(0) }
    val esc = rememberCoroutineScope()
    val ocupado by rememberUpdatedState(cobrar || fechar != null || levar != null || mais || novaConta)
    LaunchedEffect(comandaId, recarregar) {
        while (true) {
            if (!ocupado || c == null) {
                try {
                    val r = e.api.obj("/api/sis/comandas/$comandaId")
                    if (r.optString("status") != "aberta") { e.avisar("A mesa ${r.optString("mesa")} já foi fechada."); e.tela = Tela.Mesas; break }
                    c = r; agora = System.currentTimeMillis()
                } catch (x: ErroApi) { e.tratar(x); if (x.status == 401 || x.status == 404) { if (x.status == 404) e.tela = Tela.Mesas; break } }
            }
            delay(5000)
        }
    }
    BackHandler { e.tela = Tela.Mesas }
    val k = c
    if (k == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    val podeCobrar = e.pode("cobrar_mesa")
    val aConferir = k.optJSONArray("a_conferir")?.objs() ?: emptyList()
    val temItens = (k.optJSONArray("itens")?.objs() ?: emptyList()).any { !it.optBoolean("cancelado") && it.optInt("cancelado") == 0 }
    val restante = k.optLong("restante")
    val multi = (k.optJSONArray("contas")?.objs() ?: emptyList()).count { it.optBoolean("aberta") || it.optInt("aberta") == 1 } > 1
    // diferença entre o relógio da maquininha e o do servidor não importa aqui: usa só o "agora" local relativo à última leitura
    Column(Modifier.fillMaxSize()) {
        Topo("Mesa ${k.optString("mesa")}" + if (multi) " · ${k.optString("nome_conta")}" else "",
            "aberta " + k.optString("aberta_em").drop(11).take(5) + (if (k.optString("cliente").isNotEmpty()) " · " + k.optString("cliente") else ""), { e.tela = Tela.Mesas }) {
            OutlinedButton(onClick = { mais = true }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(44.dp)) { Text("⋯") }
        }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
            if (aConferir.isNotEmpty()) item {
                Column(Modifier.fillMaxWidth().background(Color(0xFF2A1F3D), RoundedCornerShape(12.dp)).border(1.dp, Roxo, RoundedCornerShape(12.dp)).padding(12.dp)) {
                    Text("💵 Leve ao caixa: ${brl(aConferir.sumOf { it.optLong("valor") })}", fontWeight = FontWeight.Bold)
                    Text(aConferir.joinToString(" · ") { "${FORMAS[it.optString("forma")] ?: it.optString("forma")} ${brl(it.optLong("valor"))}" + if (it.optString("forma") == "dinheiro") " (dinheiro)" else " (comprovante)" } +
                        ". A mesa só fecha depois que o caixa confirmar.", color = Apagado, fontSize = 13.sp)
                }
            }
            val peds = (k.optJSONArray("pedidos")?.objs() ?: emptyList()).filter { (it.optJSONArray("itens")?.length() ?: 0) > 0 }
            if (peds.isEmpty()) item { Text("Nada lançado ainda.", color = Apagado, modifier = Modifier.fillMaxWidth().padding(30.dp), textAlign = TextAlign.Center) }
            items(peds, key = { it.optLong("id") }) { p ->
                val st = p.optString("status")
                Column(Modifier.fillMaxWidth().background(Painel, RoundedCornerShape(14.dp)).border(1.dp, Painel2, RoundedCornerShape(14.dp)).padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Pedido ${p.optInt("numero")}", fontWeight = FontWeight.Bold)
                        Text(p.optString("criado_em").drop(11).take(5) + (if (p.optString("usuario").isNotEmpty()) " · " + p.optString("usuario") else ""), color = Fraco, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (st != "cancelado") {
                            val min = minutosDesde(p.optString("criado_em"), agora + 0)
                            val naCozinha = st == "novo" || st == "preparo"
                            Etiqueta("⏱ ${if (min < 60) "$min min" else "${min / 60}h${String.format("%02d", min % 60)}"}", if (naCozinha && min >= 40) Ruim else if (naCozinha && min >= 20) Destaque else Apagado)
                        }
                        Etiqueta(STATUS[st] ?: st, when (st) { "pronto" -> Ok; "cancelado" -> Ruim; "entregue" -> Apagado; else -> Destaque })
                    }
                    if (st == "pronto") Botao("Entreguei", Modifier.padding(top = 6.dp), Ok, Color(0xFF06200F)) {
                        esc.launch { try { e.api.post("/api/sis/pedidos/${p.optLong("id")}/status", JSONObject().put("status", "entregue")); e.avisar("Marcado como entregue."); recarregar++ } catch (x: ErroApi) { e.tratar(x) } }
                    }
                    p.optJSONArray("itens")?.objs()?.forEach { i ->
                        val canc = i.optBoolean("cancelado") || i.optInt("cancelado") == 1 || st == "cancelado"
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                            Text("${i.optInt("q")}×", fontWeight = FontWeight.Black, modifier = Modifier.width(32.dp))
                            Column(Modifier.weight(1f)) {
                                Text(i.optString("nome") + (if (i.optString("opcao").isNotEmpty() && i.optString("opcao") != "null") " (${i.optString("opcao")})" else "") +
                                    (if (i.optString("sabor").isNotEmpty() && i.optString("sabor") != "null") " · ${i.optString("sabor")}" else ""),
                                    textDecoration = if (canc) TextDecoration.LineThrough else null, color = if (canc) Fraco else Tinta)
                                if (i.optString("obs").isNotEmpty() && i.optString("obs") != "null") Text(i.optString("obs"), color = Apagado, fontSize = 13.sp)
                            }
                            Text(brl(i.optLong("total")), color = if (canc) Fraco else Tinta)
                        }
                    }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().background(Painel, RoundedCornerShape(14.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinhaValor("Consumo", brl(k.optLong("subtotal")))
                    if (k.optLong("servico") > 0) LinhaValor("Serviço ${k.optString("servico_pct").replace(".", ",")}%", brl(k.optLong("servico")))
                    if (k.optLong("desconto") > 0) LinhaValor("Desconto", "− " + brl(k.optLong("desconto")))
                    Row { Text("Total", fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f)); Text(brl(k.optLong("total")), fontSize = 22.sp, fontWeight = FontWeight.Black) }
                    val pags = k.optJSONArray("pagamentos")?.objs() ?: emptyList()
                    pags.forEach { g ->
                        val garc = g.optString("origem") == "garcom"
                        LinhaValor("Pago · ${FORMAS[g.optString("forma")] ?: g.optString("forma")}" + if (garc) (if (g.optString("conferido_em").let { it.isNotEmpty() && it != "null" }) " · caixa ok" else " · aguardando caixa") else "", "− " + brl(g.optLong("valor")))
                    }
                    if (pags.isNotEmpty()) Row { Text(if (restante > 0) "Falta pagar" else "Tudo pago", fontWeight = FontWeight.Bold, color = if (restante > 0) Destaque else Ok, modifier = Modifier.weight(1f)); Text(brl(restante), fontWeight = FontWeight.Bold, color = if (restante > 0) Destaque else Ok) }
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(Painel).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Botao("+ Itens", Modifier.weight(1f)) { e.carrinho.clear(); e.tela = Tela.Cardapio(k.optString("mesa"), comandaId) }
            if (podeCobrar && restante > 0) Botao("Cobrar ${brl(restante)}", Modifier.weight(1.4f), Destaque, TintaDestaque) { cobrar = true }
            if (podeCobrar && restante <= 0 && temItens && aConferir.isEmpty()) Botao("Fechar mesa", Modifier.weight(1.4f), Ok, Color(0xFF06200F)) { fechar = k }
            if (!podeCobrar && temItens) Botao("Imprimir conta", Modifier.weight(1.4f), Destaque, TintaDestaque) {
                esc.launch { try { e.api.post("/api/sis/comandas/$comandaId/conta"); e.avisar("Conta impressa no caixa.") } catch (x: ErroApi) { e.tratar(x) } }
            }
        }
    }
    if (cobrar) TelaCobrar(e, k, { cobrar = false }) { cm ->
        cobrar = false; c = cm
        if (cm.optLong("restante") <= 0) { if ((cm.optJSONArray("a_conferir")?.length() ?: 0) > 0) levar = cm else fechar = cm }
    }
    levar?.let { cm ->
        val lv = cm.optJSONArray("a_conferir")?.objs() ?: emptyList()
        Folha("✅ Conta paga!", { levar = null; e.tela = Tela.Mesas }) {
            Column(Modifier.fillMaxWidth().background(Color(0xFF2A1F3D), RoundedCornerShape(12.dp)).border(1.dp, Roxo, RoundedCornerShape(12.dp)).padding(12.dp)) {
                Text("💵 Leve ao caixa: ${brl(lv.sumOf { it.optLong("valor") })}", fontWeight = FontWeight.Bold)
                Text(lv.joinToString(" · ") { "${FORMAS[it.optString("forma")]} ${brl(it.optLong("valor"))}" + if (it.optString("forma") == "dinheiro") " em dinheiro" else " (comprovante)" }, color = Apagado, fontSize = 13.sp)
            }
            Text("A mesa fica roxa até o caixa confirmar e fechar.", color = Apagado)
            Botao("Entendi", Modifier.fillMaxWidth(), Destaque, TintaDestaque) { levar = null; e.tela = Tela.Mesas }
        }
    }
    fechar?.let { cm -> TelaFechar(e, cm, { fechar = null; recarregar++ }) { fechar = null; e.tela = Tela.Mesas } }
    if (mais) Folha("Mesa ${k.optString("mesa")}", { mais = false }) {
        if (temItens) Botao("🧾 Imprimir a conta no caixa", Modifier.fillMaxWidth()) {
            mais = false; esc.launch { try { e.api.post("/api/sis/comandas/$comandaId/conta"); e.avisar("Conta impressa no caixa.") } catch (x: ErroApi) { e.tratar(x) } }
        }
        Botao("➕ Abrir outra conta nesta mesa", Modifier.fillMaxWidth()) { mais = false; novaConta = true }
        TextButton(onClick = { mais = false }, modifier = Modifier.fillMaxWidth()) { Text("Voltar") }
    }
    if (novaConta) {
        var nome by remember { mutableStateOf("") }
        Folha("Nova conta na mesa ${k.optString("mesa")}", { novaConta = false }) {
            OutlinedTextField(nome, { nome = it.take(30) }, label = { Text("Nome (ex: João, Família 2)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Botao("Voltar", Modifier.weight(1f)) { novaConta = false }
                Botao("Abrir conta", Modifier.weight(1f), Destaque, TintaDestaque) {
                    esc.launch {
                        try { val r = e.api.obj("/api/sis/mesas/${k.optString("mesa")}/contas", JSONObject().put("nome", nome.trim())); novaConta = false; e.carrinho.clear(); e.tela = Tela.Cardapio(k.optString("mesa"), r.optLong("id")) }
                        catch (x: ErroApi) { e.tratar(x) }
                    }
                }
            }
        }
    }
}

@Composable
fun LinhaValor(rot: String, valor: String) { Row { Text(rot, color = Apagado, modifier = Modifier.weight(1f)); Text(valor) } }

// ---------- cobrar ----------
@Composable
fun TelaCobrar(e: Estado, c: JSONObject, fechar: () -> Unit, pago: (JSONObject) -> Unit) {
    val restante = c.optLong("restante")
    var caixa by remember { mutableStateOf<JSONObject?>(null) }
    var valorTxt by remember { mutableStateOf(emReais(restante)) }
    var recebidoTxt by remember { mutableStateOf("") }
    var forma by remember { mutableStateOf<String?>(null) }
    var processando by remember { mutableStateOf(false) }
    var problema by remember { mutableStateOf<String?>(null) }
    val esc = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        try {
            val cx = e.api.obj("/api/sis/garcom/caixa")
            if (!cx.optBoolean("aberto")) { e.avisar("O caixa ${cx.optString("nome")} está fechado. Peça para abrir antes de cobrar.", true); fechar() } else caixa = cx
        } catch (x: ErroApi) { e.tratar(x); fechar() }
    }
    val valor = centavos(valorTxt)
    val din = forma == "dinheiro"
    val recebido = if (din) maxOf(centavos(recebidoTxt), valor) else 0L
    Folha("Cobrar mesa ${c.optString("mesa")}", { if (!processando) fechar() }) {
        if (caixa == null) { CircularProgressIndicator(); return@Folha }
        if (problema != null) {
            Text("⚠ Cobrado, mas não lançado", fontWeight = FontWeight.Bold, color = Ruim)
            Text(problema!!, color = Apagado)
            Text("O pagamento ficou guardado neste aparelho. Na tela de mesas aparece o botão para lançar de novo.", color = Apagado)
            Botao("Entendi", Modifier.fillMaxWidth(), Destaque, TintaDestaque) { fechar() }
            return@Folha
        }
        Text("Falta pagar ${brl(restante)}", color = Apagado, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        OutlinedTextField(valorTxt, { valorTxt = it }, label = { Text("Valor a cobrar") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), textStyle = LocalTextStyle.current.copy(fontSize = 26.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
        Text("Dividir a conta", color = Apagado, fontSize = 13.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(2, 3, 4, 5).forEach { n -> OutlinedButton(onClick = { valorTxt = emReais((restante + n - 1) / n) }) { Text("÷ $n") } }
            OutlinedButton(onClick = { valorTxt = emReais(restante) }) { Text("Tudo") }
        }
        Text("Forma", color = Apagado, fontSize = 13.sp)
        FORMAS.entries.chunked(2).forEach { linha ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                linha.forEach { (k, n) ->
                    val sel = forma == k
                    Botao(n, Modifier.weight(1f), if (sel) Tinta else Painel2, if (sel) Fundo else Tinta) { forma = k }
                }
                if (linha.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (din) {
            OutlinedTextField(recebidoTxt, { recebidoTxt = it }, label = { Text("Recebido do cliente (para o troco)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            if (recebido > valor) Text("Troco: ${brl(recebido - valor)}", fontWeight = FontWeight.Bold)
        }
        val okAtivo = forma != null && valor > 0 && valor <= restante && !processando
        val rotulo = when {
            processando -> "Aguarde…"
            forma == null -> "Escolha a forma"
            valor > restante -> "Valor maior que o que falta"
            valor <= 0 -> "Informe o valor"
            din -> "Recebi ${brl(valor)} em dinheiro"
            else -> "Cobrar ${brl(valor)} no ${FORMAS[forma]}"
        }
        Botao(rotulo, Modifier.fillMaxWidth(), Destaque, TintaDestaque, okAtivo) {
            processando = true
            esc.launch {
                val f = forma!!
                val ref = novaRef()
                val r = if (f == "dinheiro") JSONObject().put("aprovado", true).put("modo", "dinheiro")
                        else e.activity.cobrarNaStone(valor, f, 1).put("modo", "stone")
                if (!r.optBoolean("aprovado")) {
                    processando = false
                    if (!r.optBoolean("cancelado")) e.avisar(r.optString("motivo", "Pagamento não aprovado."), true) else e.avisar("Pagamento cancelado.")
                    return@launch
                }
                val pg = JSONObject().put("ref", ref).put("comanda", c.optLong("id")).put("mesa", c.optString("mesa")).put("forma", f).put("valor", valor)
                    .put("recebido", if (f == "dinheiro") recebido else 0L)
                    .put("transacao", JSONObject().put("modo", r.optString("modo")).put("autorizacao", r.optString("autorizacao")).put("nsu", r.optString("nsu"))
                        .put("bandeira", r.optString("bandeira")).put("parcelas", r.optInt("parcelas", 1)).put("terminal", r.optString("atk")))
                e.guarda.guardarPendente(pg)
                try {
                    val res = e.lancarPagamento(pg)
                    val cm = res.optJSONObject("comanda") ?: JSONObject()
                    val troco = if (f == "dinheiro" && recebido > valor) " Troco ${brl(recebido - valor)}." else ""
                    if (cm.optLong("restante") > 0) e.avisar("${brl(valor)} recebido.$troco Falta ${brl(cm.optLong("restante"))}.") else if (troco.isNotEmpty()) e.avisar("Devolva o troco:$troco")
                    pago(cm)
                } catch (x: ErroApi) {
                    problema = "${FORMAS[f]} · ${brl(valor)} · autorização ${r.optString("autorizacao").ifEmpty { "-" }}\n${x.message}"
                } finally { processando = false }
            }
        }
        Text("Vai para o caixa ${caixa!!.optString("nome")}.", color = Fraco, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

// ---------- fechar a mesa ----------
@Composable
fun TelaFechar(e: Estado, c: JSONObject, cancelar: () -> Unit, fechou: () -> Unit) {
    var nfce by remember { mutableStateOf(false) }
    var nota by remember { mutableStateOf(true) }
    var cpf by remember { mutableStateOf("") }
    var ocupado by remember { mutableStateOf(false) }
    val esc = rememberCoroutineScope()
    LaunchedEffect(Unit) { nfce = try { e.api.obj("/api/sis/garcom/caixa").optBoolean("nfce") } catch (_: Exception) { false } }
    Folha("Fechar mesa ${c.optString("mesa")}", cancelar) {
        Text("Total ${brl(c.optLong("total"))} recebido. Fechando, a mesa fica livre.", color = Apagado)
        if (nfce) {
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(nota, { nota = it }); Text("Emitir nota fiscal (NFC-e)") }
            OutlinedTextField(cpf, { cpf = it.filter { ch -> ch.isDigit() }.take(14) }, label = { Text("CPF na nota (opcional)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Botao("Agora não", Modifier.weight(1f)) { cancelar() }
            Botao("Fechar mesa", Modifier.weight(1f), Ok, Color(0xFF06200F), !ocupado) {
                ocupado = true
                esc.launch {
                    try {
                        val corpo = if (nfce) JSONObject().put("nota", nota).put("cpf", cpf) else JSONObject()
                        e.api.post("/api/sis/garcom/comandas/${c.optLong("id")}/fechar", corpo)
                        e.avisar("Mesa ${c.optString("mesa")} fechada."); fechou()
                    } catch (x: ErroApi) { e.tratar(x) } finally { ocupado = false }
                }
            }
        }
    }
}

// ---------- cardápio ----------
@Composable
fun TelaCardapio(e: Estado, mesa: String, comandaId: Long?) {
    var cat by remember { mutableStateOf(e.categorias.firstOrNull() ?: "") }
    var busca by remember { mutableStateOf("") }
    var escolhendo by remember { mutableStateOf<JSONObject?>(null) }
    var verCarrinho by remember { mutableStateOf(false) }
    var descartar by remember { mutableStateOf(false) }
    val voltar: () -> Unit = { if (e.carrinho.isNotEmpty()) { descartar = true } else { e.tela = if (comandaId != null) Tela.Conta(comandaId) else Tela.Mesas } }
    BackHandler { voltar() }
    val b = busca.trim().lowercase()
    val lista = e.produtos.filter { p -> if (b.isNotEmpty()) (semTags(p.optString("nome")) + " " + p.optString("descricao") + " " + p.optString("categoria")).lowercase().contains(b) else p.optString("categoria") == cat }
    Column(Modifier.fillMaxSize()) {
        Topo("Mesa $mesa", if (comandaId != null) "lançar mais itens" else "abrir mesa", voltar)
        Column(Modifier.background(Painel).padding(horizontal = 12.dp)) {
            OutlinedTextField(busca, { busca = it }, placeholder = { Text("Buscar produto") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                e.categorias.forEach { c ->
                    val sel = b.isEmpty() && c == cat
                    FilterChip(selected = sel, onClick = { cat = c; busca = "" }, label = { Text(c) })
                }
            }
        }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
            if (lista.isEmpty()) item { Text("Nada encontrado.", color = Apagado, modifier = Modifier.fillMaxWidth().padding(30.dp), textAlign = TextAlign.Center) }
            items(lista, key = { it.optString("id") }) { p ->
                val sem = (p.optBoolean("estoque_controla") || p.optInt("estoque_controla") == 1) && p.optDouble("estoque", 1.0) <= 0
                val q = e.carrinho.filter { it.id == p.optString("id") }.sumOf { it.q }
                Row(Modifier.fillMaxWidth().background(Painel, RoundedCornerShape(14.dp)).clickable(enabled = !sem) {
                    if (e.opcoes(p).isEmpty() && e.sabores(p).isEmpty()) e.adicionar(p, -1, -1, 1, "", emptyList()) else escolhendo = p
                }.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(e.nomeProd(p), fontWeight = FontWeight.SemiBold, color = if (sem) Fraco else Tinta)
                        if (b.isNotEmpty()) Text(p.optString("categoria"), color = Fraco, fontSize = 13.sp)
                    }
                    if (sem) Etiqueta("Esgotado", Ruim) else Text(e.precoTexto(p), color = Destaque, fontWeight = FontWeight.Bold)
                    if (q > 0) Box(Modifier.size(28.dp).background(Destaque, RoundedCornerShape(50)), contentAlignment = Alignment.Center) { Text("$q", color = TintaDestaque, fontWeight = FontWeight.Black, fontSize = 14.sp) }
                }
            }
        }
        val n = e.carrinho.sumOf { it.q }
        val tot = e.carrinho.sumOf { it.unit * it.q }
        Row(Modifier.fillMaxWidth().background(Painel).padding(10.dp)) {
            Botao(if (n > 0) "Ver pedido · $n ${if (n == 1) "item" else "itens"} · ${brl(tot)}" else "Nada escolhido", Modifier.fillMaxWidth(), Destaque, TintaDestaque, n > 0) { verCarrinho = true }
        }
    }
    escolhendo?.let { p -> Opcoes(e, p, { escolhendo = null }) }
    if (verCarrinho) Carrinho(e, mesa, comandaId) { verCarrinho = false }
    if (descartar) AlertDialog(onDismissRequest = { descartar = false }, title = { Text("Descartar os itens escolhidos?") }, text = { Text("Eles ainda não foram para a cozinha.") },
        confirmButton = { TextButton(onClick = { descartar = false; e.carrinho.clear(); e.tela = if (comandaId != null) Tela.Conta(comandaId) else Tela.Mesas }) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = { descartar = false }) { Text("Voltar") } })
}

@Composable
fun Opcoes(e: Estado, p: JSONObject, fechar: () -> Unit) {
    val ops = e.opcoes(p); val sab = e.sabores(p)
    val pizza = p.optString("categoria") == "Pizzas" && ops.isNotEmpty()
    var o by remember { mutableStateOf(if (ops.isEmpty()) -1 else if (pizza && ops.size > 2) 2 else 0) }
    var f by remember { mutableStateOf(if (sab.isEmpty()) -1 else 0) }
    var q by remember { mutableStateOf(1) }
    var obs by remember { mutableStateOf("") }
    val x = remember { mutableStateListOf<String>() }
    var escolherSabor by remember { mutableStateOf(false) }
    val tam = ops.getOrNull(o)?.first ?: ""
    val max = if (pizza) e.maxSabores(tam) else 1
    while (x.size > max - 1 && x.isNotEmpty()) x.removeAt(x.size - 1)
    Folha(e.nomeProd(p), fechar) {
        if (p.optString("descricao").isNotEmpty() && p.optString("descricao") != "null") Text(semTags(p.optString("descricao")), color = Apagado, fontSize = 14.sp)
        if (ops.isNotEmpty()) {
            Text("Tamanho / tipo", color = Apagado, fontSize = 13.sp)
            ops.forEachIndexed { i, (n, pr) -> val sel = i == o; Botao("$n · ${brl(pr)}", Modifier.fillMaxWidth(), if (sel) Tinta else Painel2, if (sel) Fundo else Tinta) { o = i } }
        }
        if (sab.isNotEmpty()) {
            Text("Sabor", color = Apagado, fontSize = 13.sp)
            sab.forEachIndexed { i, n -> val sel = i == f; Botao(n, Modifier.fillMaxWidth(), if (sel) Tinta else Painel2, if (sel) Fundo else Tinta) { f = i } }
        }
        if (pizza && max > 1) {
            Text("Sabores da $tam (até $max)", color = Apagado, fontSize = 13.sp)
            Text("1. ${semTags(p.optString("nome"))}", fontWeight = FontWeight.Bold)
            x.forEachIndexed { i, id ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 2}. " + semTags(e.produtos.find { it.optString("id") == id }?.optString("nome") ?: ""), modifier = Modifier.weight(1f))
                    TextButton(onClick = { x.removeAt(i) }) { Text("Tirar", color = Ruim) }
                }
            }
            if (x.size + 1 < max) OutlinedButton(onClick = { escolherSabor = true }, modifier = Modifier.fillMaxWidth()) { Text("+ ${x.size + 2}º sabor (meio a meio)") }
        }
        OutlinedTextField(obs, { obs = it.take(120) }, label = { Text("Observação (ex: sem cebola)") }, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { if (q > 1) q-- }, modifier = Modifier.size(52.dp), contentPadding = PaddingValues(0.dp)) { Text("−", fontSize = 22.sp) }
            Text("$q", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
            OutlinedButton(onClick = { if (q < 99) q++ }, modifier = Modifier.size(52.dp), contentPadding = PaddingValues(0.dp)) { Text("+", fontSize = 22.sp) }
            Botao("Adicionar ${brl(e.precoItem(p, maxOf(o, 0), x.toList()) * q)}", Modifier.weight(1f), Destaque, TintaDestaque) {
                e.adicionar(p, o, f, q, obs.trim(), x.toList()); fechar()
            }
        }
    }
    if (escolherSabor) {
        val outros = e.produtos.filter { z -> z.optString("categoria") == "Pizzas" && z.optString("id") != p.optString("id") && !x.contains(z.optString("id")) && e.opcoes(z).any { it.first == tam } }
        Folha("Escolha o ${x.size + 2}º sabor", { escolherSabor = false }) {
            outros.forEach { z -> Botao(semTags(z.optString("nome")), Modifier.fillMaxWidth()) { x.add(z.optString("id")); escolherSabor = false } }
            TextButton(onClick = { escolherSabor = false }, modifier = Modifier.fillMaxWidth()) { Text("Voltar") }
        }
    }
}

@Composable
fun Carrinho(e: Estado, mesa: String, comandaId: Long?, fechar: () -> Unit) {
    var enviando by remember { mutableStateOf(false) }
    val esc = rememberCoroutineScope()
    if (e.carrinho.isEmpty()) { LaunchedEffect(Unit) { fechar() }; return }
    Folha("Pedido da mesa $mesa", { if (!enviando) fechar() }) {
        e.carrinho.toList().forEachIndexed { i, it ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(it.nome + (if (it.opcao != null) " (${it.opcao})" else "") + (if (it.sabor != null) " · ${it.sabor}" else ""))
                    Text((if (it.obs.isNotEmpty()) it.obs + " · " else "") + brl(it.unit * it.q), color = Apagado, fontSize = 13.sp)
                }
                OutlinedButton(onClick = { if (it.q <= 1) e.carrinho.removeAt(i) else e.carrinho[i] = it.copy(q = it.q - 1) }, modifier = Modifier.size(40.dp), contentPadding = PaddingValues(0.dp)) { Text("−") }
                Text("${it.q}", fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                OutlinedButton(onClick = { e.carrinho[i] = it.copy(q = it.q + 1) }, modifier = Modifier.size(40.dp), contentPadding = PaddingValues(0.dp)) { Text("+") }
            }
        }
        Row { Text("Total", fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f)); Text(brl(e.carrinho.sumOf { it.unit * it.q }), fontSize = 20.sp, fontWeight = FontWeight.Black) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Botao("+ Mais itens", Modifier.weight(1f)) { fechar() }
            Botao(if (enviando) "Enviando…" else "Enviar p/ cozinha", Modifier.weight(1f), Destaque, TintaDestaque, !enviando) {
                enviando = true
                esc.launch {
                    try { val id = e.enviarCarrinho(mesa, comandaId); fechar(); e.tela = Tela.Conta(id) }
                    catch (x: ErroApi) { e.tratar(x) } finally { enviando = false }
                }
            }
        }
    }
}
