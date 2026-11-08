package com.yhcc.wallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat

private val Ink = Color(0xFF090A0D)
private val Panel = Color(0xFF15171C)
private val Panel2 = Color(0xFF1D2027)
private val Gold = Color(0xFFD6B36A)
private val GoldLight = Color(0xFFF0D99B)
private val Muted = Color(0xFF969BA6)
private val Green = Color(0xFF61C69A)
private val Red = Color(0xFFE27D7D)

data class Asset(val symbol: String, val name: String, var amount: Double, var price: Double) {
    val value: Double get() = amount * price
}
data class ActivityItem(val title: String, val detail: String, val amount: String, val incoming: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WalletApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletApp() {
    val assets = remember {
        mutableStateListOf(
            Asset("BTC", "Bitcoin", 0.3500, 70000.0),
            Asset("ETH", "Ethereum", 1.2000, 3000.0),
            Asset("USDT", "Tether", 350.80, 1.0)
        )
    }
    val activities = remember {
        mutableStateListOf(
            ActivityItem("收到 BTC", "今天 · 09:42", "+0.025 BTC", true),
            ActivityItem("转出 USDT", "昨天 · 18:16", "-120 USDT", false),
            ActivityItem("收到 ETH", "11月05日 · 14:20", "+0.15 ETH", true)
        )
    }
    var tab by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Asset?>(null) }
    var amountText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var symbolText by remember { mutableStateOf("") }
    var nameText by remember { mutableStateOf("") }
    var transferAddress by remember { mutableStateOf("") }
    var transferAmount by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }

    val total = assets.sumOf { it.value }
    val money = remember { DecimalFormat("#,##0.00") }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Ink, surface = Panel, primary = Gold,
            onBackground = Color.White, onSurface = Color.White,
            secondary = GoldLight
        )
    ) {
        Scaffold(
            containerColor = Ink,
            topBar = {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(38.dp).background(Gold, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Text("Y", color = Ink, fontWeight = FontWeight.Black, fontSize = 23.sp)
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("YHCC WALLET", color = GoldLight, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Text("PERSONAL ASSET DISPLAY", color = Muted, fontSize = 9.sp, letterSpacing = 1.2.sp)
                    }
                    IconButton(onClick = { notice = "当前为本地资产展示；不会发起链上交易。" }) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = "通知", tint = Gold)
                    }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = Panel, contentColor = Gold) {
                    val labels = listOf("资产", "行情", "记录", "我的")
                    val icons = listOf(Icons.Default.AccountBalanceWallet, Icons.Default.ShowChart, Icons.Default.ReceiptLong, Icons.Default.Person)
                    labels.forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { Icon(icons[index], contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Gold, selectedTextColor = Gold,
                                indicatorColor = Color(0xFF39301E),
                                unselectedIconColor = Muted, unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        ) { padding ->
            when (tab) {
                0 -> HomeScreen(
                    Modifier.padding(padding), total, money, assets,
                    onAdd = { selected = null; symbolText = ""; nameText = ""; amountText = ""; priceText = ""; dialog = "asset" },
                    onEdit = { a -> selected = a; symbolText = a.symbol; nameText = a.name; amountText = a.amount.toString(); priceText = a.price.toString(); dialog = "asset" },
                    onSend = { dialog = "transfer" },
                    onReceive = { dialog = "receive" }
                )
                1 -> MarketScreen(Modifier.padding(padding), assets)
                2 -> ActivityScreen(Modifier.padding(padding), activities)
                else -> ProfileScreen(Modifier.padding(padding), onNotice = { notice = it })
            }
        }

        if (dialog == "asset") {
            AlertDialog(
                onDismissRequest = { dialog = "" },
                containerColor = Panel2,
                title = { Text(if (selected == null) "添加资产" else "编辑资产", color = GoldLight) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Field("币种代码", symbolText) { symbolText = it }
                        Field("币种名称", nameText) { nameText = it }
                        Field("持有数量", amountText, true) { amountText = it }
                        Field("单价（USD）", priceText, true) { priceText = it }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val a = amountText.toDoubleOrNull()
                        val p = priceText.toDoubleOrNull()
                        if (symbolText.isNotBlank() && nameText.isNotBlank() && a != null && p != null && a >= 0 && p >= 0) {
                            if (selected == null) assets.add(Asset(symbolText.uppercase(), nameText, a, p))
                            else { selected!!.symbol.let { old -> val i = assets.indexOfFirst { it === selected }; if (i >= 0) assets[i] = Asset(symbolText.uppercase(), nameText, a, p) } }
                            dialog = ""
                        } else notice = "请填写有效的资产信息。"
                    }) { Text("保存", color = Gold) }
                },
                dismissButton = {
                    Row {
                        if (selected != null) TextButton(onClick = { assets.remove(selected); dialog = "" }) { Text("删除", color = Red) }
                        TextButton(onClick = { dialog = "" }) { Text("取消", color = Muted) }
                    }
                }
            )
        }
        if (dialog == "transfer") {
            AlertDialog(
                onDismissRequest = { dialog = "" }, containerColor = Panel2,
                title = { Text("转账", color = GoldLight) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("填写转账信息以预览操作。此页面不会连接网络或广播交易。", color = Muted, fontSize = 12.sp)
                        Field("收款地址", transferAddress) { transferAddress = it }
                        Field("转账数量", transferAmount, true) { transferAmount = it }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (transferAddress.isNotBlank() && (transferAmount.toDoubleOrNull() ?: 0.0) > 0) {
                            notice = "已完成本地转账信息预览；未发送资产。"
                            dialog = ""
                        } else notice = "请填写有效地址和数量。"
                    }) { Text("确认", color = Gold) }
                },
                dismissButton = { TextButton(onClick = { dialog = "" }) { Text("取消", color = Muted) } }
            )
        }
        if (dialog == "receive") {
            AlertDialog(
                onDismissRequest = { dialog = "" }, containerColor = Panel2,
                title = { Text("收款", color = GoldLight) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(170.dp).background(Color.White, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Text("YHCC\nWALLET", color = Ink, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("展示地址：未配置", color = Muted, fontSize = 12.sp)
                    }
                },
                confirmButton = { TextButton(onClick = { dialog = "" }) { Text("完成", color = Gold) } }
            )
        }
        if (notice.isNotBlank()) {
            AlertDialog(
                onDismissRequest = { notice = "" }, containerColor = Panel2,
                text = { Text(notice, color = Color.White) },
                confirmButton = { TextButton(onClick = { notice = "" }) { Text("知道了", color = Gold) } }
            )
        }
    }
}

@Composable
fun HomeScreen(
    modifier: Modifier, total: Double, money: DecimalFormat, assets: List<Asset>,
    onAdd: () -> Unit, onEdit: (Asset) -> Unit, onSend: () -> Unit, onReceive: () -> Unit
) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 22.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(22.dp)).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("总资产估值", color = Muted, fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("USD", color = Gold, fontSize = 10.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text("$${money.format(total)}", color = GoldLight, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("资产概览", color = Muted, fontSize = 11.sp)
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton("转账", Icons.Default.ArrowUpward, onSend, Modifier.weight(1f))
                    ActionButton("收款", Icons.Default.ArrowDownward, onReceive, Modifier.weight(1f))
                    ActionButton("添加资产", Icons.Default.Add, onAdd, Modifier.weight(1f))
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("我的资产", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("编辑资产可修改数量与单价", color = Muted, fontSize = 10.sp)
            }
        }
        items(assets) { asset ->
            Row(
                Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp))
                    .clickable { onEdit(asset) }.padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(42.dp).background(Color(0xFF30291C), CircleShape), contentAlignment = Alignment.Center) {
                    Text(asset.symbol.take(1), color = Gold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(asset.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(asset.symbol, color = Muted, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("$${money.format(asset.value)}", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("${fmt(asset.amount)} ${asset.symbol}", color = Muted, fontSize = 11.sp)
                }
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = "编辑", tint = Muted)
            }
        }
        item { Text("提示：资产数量、单价和记录由你在应用内自行维护。", color = Muted, fontSize = 11.sp) }
    }
}

@Composable
fun ActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.background(Color(0xFF282316), RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = Gold)
        Spacer(Modifier.height(5.dp))
        Text(label, color = GoldLight, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
fun MarketScreen(modifier: Modifier, assets: List<Asset>) {
    Column(modifier.fillMaxSize().padding(18.dp)) {
        Text("行情", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("币种参考单价由资产设置维护", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        assets.forEach {
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(it.symbol, color = Gold, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
                Text(it.name, color = Color.White, modifier = Modifier.weight(1f))
                Text("$${DecimalFormat("#,##0.00").format(it.price)}", color = Color.White)
            }
            HorizontalDivider(color = Panel2)
        }
    }
}

@Composable
fun ActivityScreen(modifier: Modifier, activities: List<ActivityItem>) {
    Column(modifier.fillMaxSize().padding(18.dp)) {
        Text("交易记录", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("本机记录", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        activities.forEach {
            Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (it.incoming) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, contentDescription = null, tint = if (it.incoming) Green else Red)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(it.title, color = Color.White, fontWeight = FontWeight.Medium)
                    Text(it.detail, color = Muted, fontSize = 11.sp)
                }
                Text(it.amount, color = if (it.incoming) Green else Red, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
fun ProfileScreen(modifier: Modifier, onNotice: (String) -> Unit) {
    Column(modifier.fillMaxSize().padding(18.dp)) {
        Text("我的", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        listOf("应用名称" to "YHCC WALLET", "版本" to "1.0.0", "显示设置" to "黑金主题", "数据管理" to "本机保存").forEach {
            Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(it.first, color = Color.White, modifier = Modifier.weight(1f))
                Text(it.second, color = Gold, fontSize = 12.sp)
            }
            Spacer(Modifier.height(9.dp))
        }
        TextButton(onClick = { onNotice("YHCC WALLET：个人资产展示工具。当前版本不连接区块链、不保管私钥，也不会发送真实交易。") }) {
            Text("关于 YHCC WALLET", color = Gold)
        }
    }
}

@Composable
fun Field(label: String, value: String, numeric: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        singleLine = true, modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Gold, focusedLabelColor = Gold,
            unfocusedBorderColor = Muted, focusedTextColor = Color.White,
            unfocusedTextColor = Color.White, cursorColor = Gold
        )
    )
}

fun fmt(value: Double): String = DecimalFormat("#,##0.########").format(value)
