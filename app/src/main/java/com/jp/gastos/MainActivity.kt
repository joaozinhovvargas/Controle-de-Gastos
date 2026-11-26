package com.jp.gastos

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Gasto(val valor: Double, val data: String)
data class Aba(val nome: String, val gastos: List<Gasto>)

private const val PREFS = "gastos"
private const val KEY = "abas"

fun salvar(ctx: Context, abas: List<Aba>) {
    val arr = JSONArray()
    abas.forEach { a ->
        val g = JSONArray()
        a.gastos.forEach { g.put(JSONObject().put("valor", it.valor).put("data", it.data)) }
        arr.put(JSONObject().put("nome", a.nome).put("gastos", g))
    }
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putString(KEY, arr.toString()).apply()
}

fun carregar(ctx: Context): List<Aba> {
    val s = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY, null) ?: return emptyList()
    return try {
        val arr = JSONArray(s)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val g = o.getJSONArray("gastos")
            Aba(
                o.getString("nome"),
                (0 until g.length()).map { j ->
                    val x = g.getJSONObject(j)
                    Gasto(x.getDouble("valor"), x.getString("data"))
                }
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}

fun dinheiro(v: Double): String =
    String.format(Locale("pt", "BR"), "R$ %,.2f", v)

fun hoje(): String =
    SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date())

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) { App() }
            }
        }
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    var abas by remember { mutableStateOf(carregar(ctx)) }
    var sel by remember { mutableStateOf(0) }
    var dialogAba by remember { mutableStateOf(false) }
    var dialogGasto by remember { mutableStateOf(false) }
    var dialogExcluir by remember { mutableStateOf(false) }

    fun atualizar(novas: List<Aba>) {
        abas = novas
        salvar(ctx, novas)
    }

    val idx = if (abas.isEmpty()) 0 else sel.coerceIn(0, abas.lastIndex)

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Text(
            "Controle de Gastos",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        if (abas.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Nenhuma aba ainda")
                    Button(
                        onClick = { dialogAba = true },
                        modifier = Modifier.padding(top = 12.dp)
                    ) { Text("Criar primeira aba") }
                }
            }
        } else {
            ScrollableTabRow(selectedTabIndex = idx, edgePadding = 8.dp) {
                abas.forEachIndexed { i, a ->
                    Tab(
                        selected = i == idx,
                        onClick = { sel = i },
                        text = { Text(a.nome) }
                    )
                }
                Tab(
                    selected = false,
                    onClick = { dialogAba = true },
                    text = { Text("+ Nova") }
                )
            }

            val aba = abas[idx]

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text("VALOR", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("DATA", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("", Modifier.weight(0.4f))
            }

            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(aba.gastos) { i, g ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(dinheiro(g.valor), Modifier.weight(1f))
                        Text(g.data, Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                val novos = aba.gastos.toMutableList().also { it.removeAt(i) }
                                atualizar(abas.toMutableList().also {
                                    it[idx] = aba.copy(gastos = novos)
                                })
                            },
                            modifier = Modifier.weight(0.4f)
                        ) { Text("X") }
                    }
                }
            }

            Text(
                "Total: " + dinheiro(aba.gastos.sumOf { it.valor }),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { dialogGasto = true },
                    modifier = Modifier.weight(1f)
                ) { Text("Novo gasto") }
                OutlinedButton(
                    onClick = { dialogExcluir = true },
                    modifier = Modifier.weight(1f)
                ) { Text("Excluir aba") }
            }
        }
    }

    if (dialogAba) {
        var nome by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { dialogAba = false },
            title = { Text("Nova aba") },
            text = {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome (ex: Gasolina)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (nome.isNotBlank()) {
                        atualizar(abas + Aba(nome.trim(), emptyList()))
                        sel = abas.size
                        dialogAba = false
                    }
                }) { Text("Criar") }
            },
            dismissButton = {
                TextButton(onClick = { dialogAba = false }) { Text("Cancelar") }
            }
        )
    }

    if (dialogGasto && abas.isNotEmpty()) {
        var valor by remember { mutableStateOf("") }
        var data by remember { mutableStateOf(hoje()) }
        AlertDialog(
            onDismissRequest = { dialogGasto = false },
            title = { Text("Novo gasto em " + abas[idx].nome) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = valor,
                        onValueChange = { valor = it },
                        label = { Text("Valor (ex: 30,00)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = data,
                        onValueChange = { data = it },
                        label = { Text("Data") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = valor.replace(',', '.').toDoubleOrNull()
                    if (v != null && data.isNotBlank()) {
                        val aba = abas[idx]
                        atualizar(abas.toMutableList().also {
                            it[idx] = aba.copy(gastos = aba.gastos + Gasto(v, data.trim()))
                        })
                        dialogGasto = false
                    }
                }) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { dialogGasto = false }) { Text("Cancelar") }
            }
        )
    }

    if (dialogExcluir && abas.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { dialogExcluir = false },
            title = { Text("Excluir aba?") },
            text = { Text("A aba \"" + abas[idx].nome + "\" e todos os gastos dela serão apagados.") },
            confirmButton = {
                TextButton(onClick = {
                    atualizar(abas.toMutableList().also { it.removeAt(idx) })
                    sel = 0
                    dialogExcluir = false
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { dialogExcluir = false }) { Text("Cancelar") }
            }
        )
    }
}
