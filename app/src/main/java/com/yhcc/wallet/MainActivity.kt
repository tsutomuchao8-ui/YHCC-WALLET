package com.yhcc.wallet
import androidx.compose.foundation.Canvas

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import java.text.DecimalFormat

private val Bg = Color(0xFF080A0E)
private val Panel = Color(0xFF141820)
private val Panel2 = Color(0xFF1D2430)
private val Gold = Color(0xFFD7B56D)
private val GoldBright = Color(0xFFF2DCA5)
private val Muted = Color(0xFF929BA9)
private val Up = Color(0xFF36C995)
private val Down = Color(0xFFEF6D75)

data class Coin(val id: String, val symbol: String, val name: String, val price: Double, val change: Double, val spark: List<Double>)
data class Holding(val symbol: String, val name: String, val amount: Double, val price: Double) {
    val value get() = amount * price
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { YhccLaunchGate() }
    }
}

@Composable
private fun YhccLaunchGate() {
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(3000)
        showSplash = false
    }
    if (showSplash) YhccSplash() else YhccApp()
}

@Composable
private fun YhccSplash() {
    val transition = rememberInfiniteTransition(label = "yhcc-splash")
    val rotation by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing)), label = "ring-rotation"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.94f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "logo-pulse"
    )
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val alpha by animateFloatAsState(if (entered) 1f else 0f, tween(900), label = "splash-fade")
    Box(
        Modifier.fillMaxSize().background(Bg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(292.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation }.border(1.dp, Gold.copy(alpha = 0.75f), CircleShape))
                Box(Modifier.size(270.dp).graphicsLayer { rotationZ = -rotation * 0.55f }.border(1.dp, GoldBright.copy(alpha = 0.28f), CircleShape))
                Image(
                    painter = painterResource(id = R.drawable.yhcc_wallet_logo),
                    contentDescription = "YHCC WALLET 3.0",
                    modifier = Modifier.size(248.dp).graphicsLayer { scaleX = pulse; scaleY = pulse; this.alpha = alpha },
                )
            }
            Spacer(Modifier.height(28.dp))
            Text("YHCC WALLET", color = GoldBright, fontSize = 23.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            Spacer(Modifier.height(7.dp))
            Text("3.0  ·  DIGITAL ASSET TERMINAL", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(28.dp))
            LinearProgressIndicator(
                modifier = Modifier.width(116.dp).height(2.dp),
                color = Gold, trackColor = Panel2
            )
            Spacer(Modifier.height(12.dp))
            Text("INITIALIZING INTERFACE", color = Muted, fontSize = 9.sp, letterSpacing = 1.6.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YhccApp() {
    var tab by remember { mutableIntStateOf(0) }
    var showEdit by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf(false) }
    var showReceive by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var scannedAddress by remember { mutableStateOf("") }
    var transferTo by remember { mutableStateOf("") }
    var transferAmount by remember { mutableStateOf("") }
    var editSymbol by remember { mutableStateOf("BTC") }
    var editName by remember { mutableStateOf("Bitcoin") }
    var editAmount by remember { mutableStateOf("0.35") }
    var editPrice by remember { mutableStateOf("0") }

    val holdings = remember {
        mutableStateListOf(
            Holding("BTC", "Bitcoin", 0.35, 0.0),
            Holding("ETH", "Ethereum", 1.2, 0.0),
            Holding("USDT", "Tether", 350.8, 1.0)
        )
    }
    var coins by remember { mutableStateOf<List<Coin>>(emptyList()) }
    var marketError by remember { mutableStateOf("") }
    var lastUpdated by remember { mutableStateOf("正在连接公开行情源…") }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                val result = withContext(Dispatchers.IO) {
                    val raw = URL("https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=bitcoin,ethereum,solana&price_change_percentage=24h&sparkline=true").readText()
                    val arr = JSONArray(raw)
                    (0 until arr.length()).map { i ->
                        val o = arr.getJSONObject(i)
                        val sp = o.optJSONObject("sparkline_in_7d")?.optJSONArray("price")
                        val values = if (sp == null) emptyList() else (0 until sp.length()).map { sp.optDouble(it) }.filter { it.isFinite() }
                        Coin(
                            o.optString("id"), o.optString("symbol").uppercase(), o.optString("name"),
                            o.optDouble("current_price"), o.optDouble("price_change_percentage_24h_in_currency"),
                            values.takeLast(48)
                        )
                    }
                }
                coins = result
                marketError = ""
                lastUpdated = "公开行情 · ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}"
                holdings.indices.forEach { idx ->
                    val c = result.firstOrNull { it.symbol == holdings[idx].symbol }
                    if (c != null) holdings[idx] = holdings[idx].copy(price = c.price)
                }
            } catch (_: Exception) {
                marketError = "行情暂时不可用，请稍后刷新"
                lastUpdated = "行情连接失败"
            }
            delay(60_000)
        }
    }

    val total = holdings.sumOf { it.value }
    val fmt = DecimalFormat("#,##0.00")
    val scanLauncher = androidx.activity.compose.rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { scannedAddress = it; transferTo = it }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Bg, surface = Panel, primary = Gold, secondary = GoldBright,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Scaffold(
            containerColor = Bg,
            topBar = {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(38.dp).background(Gold, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                        Text("Y", color = Bg, fontWeight = FontWeight.Black, fontSize = 23.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("YHCC WALLET", color = GoldBright, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                        Text("MARKET & ASSET TERMINAL", color = Muted, fontSize = 9.sp, letterSpacing = 1.2.sp)
                    }
                    IconButton(onClick = { notice = "行情来自公开市场数据接口，可能延迟；本应用不执行真实交易或链上转账。" }) {
                        Icon(Icons.Default.Info, contentDescription = "说明", tint = Gold)
                    }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = Panel, contentColor = Gold) {
                    val labels = listOf("总览", "行情", "交易", "资产")
                    val icons = listOf(Icons.Default.Dashboard, Icons.Default.ShowChart, Icons.Default.SwapHoriz, Icons.Default.AccountBalanceWallet)
                    labels.forEachIndexed { i, label ->
                        NavigationBarItem(
                            selected = tab == i, onClick = { tab = i },
                            icon = { Icon(icons[i], contentDescription = label) },
                            label = { Text(label, fontSize = 10.sp) },
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
                0 -> OverviewScreen(
                    Modifier.padding(padding), total, holdings, coins, lastUpdated, fmt,
                    onSend = { showTransfer = true }, onReceive = { showReceive = true },
                    onEdit = { showEdit = true }
                )
                1 -> MarketScreen(Modifier.padding(padding), coins, marketError, lastUpdated)
                2 -> TradeScreen(
                    Modifier.padding(padding), coins,
                    onScan = { scanLauncher.launch(ScanOptions().setPrompt("扫描二维码").setBeepEnabled(false).setOrientationLocked(false)) },
                    onDraft = { to, amount ->
                        transferTo = to; transferAmount = amount
                        notice = "订单草稿已保存在当前页面流程中；没有提交到交易所，也没有发生资产变动。"
                    }
                )
                else -> AssetsScreen(Modifier.padding(padding), holdings, fmt, onEdit = { showEdit = true }, onReceive = { showReceive = true })
            }
        }

        if (showEdit) {
            EditHoldingDialog(
                holdings, editSymbol, editName, editAmount, editPrice,
                onSymbol = { editSymbol = it }, onName = { editName = it },
                onAmount = { editAmount = it }, onPrice = { editPrice = it },
                onSave = {
                    val amount = editAmount.toDoubleOrNull()
                    val price = editPrice.toDoubleOrNull()
                    if (amount != null && price != null && amount >= 0 && price >= 0 && editSymbol.isNotBlank()) {
                        val i = holdings.indexOfFirst { it.symbol == editSymbol.uppercase() }
                        val h = Holding(editSymbol.uppercase(), editName, amount, price)
                        if (i >= 0) holdings[i] = h else holdings.add(h)
                        showEdit = false
                    } else notice = "请填写有效的数量和价格"
                },
                onDismiss = { showEdit = false }
            )
        }

        if (showTransfer) {
            AlertDialog(
                onDismissRequest = { showTransfer = false }, containerColor = Panel2,
                title = { Text("转账信息", color = GoldBright) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("填写收款信息并生成本地草稿。此版本不会签名、广播或发送链上交易。", color = Muted, fontSize = 12.sp)
                        OutlinedTextField(transferTo, { transferTo = it }, label = { Text("收款地址") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(transferAmount, { transferAmount = it }, label = { Text("数量") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        TextButton(onClick = { scanLauncher.launch(ScanOptions().setPrompt("扫描收款二维码").setBeepEnabled(false)) }) {
                            Icon(Icons.Default.QrCodeScanner, null, tint = Gold); Spacer(Modifier.width(6.dp)); Text("扫描二维码", color = Gold)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (transferTo.isNotBlank() && (transferAmount.toDoubleOrNull() ?: 0.0) > 0) {
                            notice = "已生成本地转账草稿，未发送资产。"
                            showTransfer = false
                        } else notice = "请填写有效的地址和数量"
                    }) { Text("生成草稿", color = Gold) }
                },
                dismissButton = { TextButton(onClick = { showTransfer = false }) { Text("取消", color = Muted) } }
            )
        }

        if (showReceive) {
            ReceiveDialog(address, { address = it }, { showReceive = false })
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
private fun OverviewScreen(
    modifier: Modifier, total: Double, holdings: List<Holding>, coins: List<Coin>,
    updated: String, fmt: DecimalFormat, onSend: () -> Unit, onReceive: () -> Unit, onEdit: () -> Unit
) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(20.dp)).padding(18.dp)) {
                Text("资产估值 · USD", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(5.dp))
                Text("$${fmt.format(total)}", color = GoldBright, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("余额为本机可编辑的资产记录，不代表链上余额", color = Muted, fontSize = 10.sp)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickAction("转账草稿", Icons.Default.ArrowUpward, onSend, Modifier.weight(1f))
                    QuickAction("收款地址", Icons.Default.ArrowDownward, onReceive, Modifier.weight(1f))
                    QuickAction("编辑余额", Icons.Default.Edit, onEdit, Modifier.weight(1f))
                }
            }
        }
        item { SectionTitle("市场概览", updated) }
        items(coins.take(3)) { CoinRow(it) }
        item { SectionTitle("我的资产", "点击编辑可调整本机记录") }
        items(holdings) { h ->
            Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                CoinBadge(h.symbol)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(h.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(h.symbol, color = Muted, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("$${fmt.format(h.value)}", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("${DecimalFormat("#,##0.########").format(h.amount)} ${h.symbol}", color = Muted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun MarketScreen(modifier: Modifier, coins: List<Coin>, error: String, updated: String) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).padding(16.dp)) {
                Text("市场行情", color = GoldBright, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("公开市场参考数据 · 约每 60 秒刷新", color = Muted, fontSize = 11.sp)
                Text(updated, color = Muted, fontSize = 10.sp)
                if (error.isNotBlank()) Text(error, color = Down, fontSize = 11.sp)
            }
        }
        items(coins) { coin ->
            Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoinBadge(coin.symbol)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${coin.symbol}/USD", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(coin.name, color = Muted, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("$${DecimalFormat("#,##0.00").format(coin.price)}", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("${if (coin.change >= 0) "+" else ""}${DecimalFormat("0.00").format(coin.change)}%", color = if (coin.change >= 0) Up else Down, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Sparkline(coin.spark, if (coin.change >= 0) Up else Down, Modifier.fillMaxWidth().height(55.dp))
                Text("7 日趋势缩略图 · 非交易所盘口", color = Muted, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun TradeScreen(modifier: Modifier, coins: List<Coin>, onScan: () -> Unit, onDraft: (String, String) -> Unit) {
    var pair by remember { mutableStateOf("BTC") }
    var side by remember { mutableStateOf("买入") }
    var amount by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    val selected = coins.firstOrNull { it.symbol == pair }
    LazyColumn(modifier.fillMaxSize().padding(16.dp), contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).padding(16.dp)) {
                Text("交易终端", color = GoldBright, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("行情观察与委托信息预览", color = Muted, fontSize = 11.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("BTC", "ETH", "SOL").forEach { s ->
                        FilterChip(selected = pair == s, onClick = { pair = s }, label = { Text(s) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(selected?.let { "$${DecimalFormat("#,##0.00").format(it.price)}  ·  ${it.change}%" } ?: "行情加载中", color = GoldBright, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("订单信息", color = Color.White, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("买入", "卖出").forEach { s ->
                        FilterChip(selected = side == s, onClick = { side = s }, label = { Text(s) })
                    }
                }
                OutlinedTextField(amount, { amount = it }, label = { Text("数量 ($pair)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(address, { address = it }, label = { Text("备注 / 对手方地址（可选）") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("订单类型", color = Muted, modifier = Modifier.weight(1f))
                    Text("限价 / 市价界面预览", color = Gold, fontSize = 11.sp)
                }
                Button(
                    onClick = { onDraft(address, amount) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Bg)
                ) { Text("生成委托预览", fontWeight = FontWeight.Bold) }
                Text("不会提交订单、撮合成交或扣减资产。", color = Muted, fontSize = 10.sp)
            }
        }
        item {
            OutlinedButton(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.QrCodeScanner, null); Spacer(Modifier.width(8.dp)); Text("扫描二维码导入文本")
            }
        }
    }
}

@Composable
private fun AssetsScreen(modifier: Modifier, holdings: List<Holding>, fmt: DecimalFormat, onEdit: () -> Unit, onReceive: () -> Unit) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("资产管理", color = GoldBright, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("余额与估值由本机记录维护；不等于区块链钱包余额。", color = Muted, fontSize = 11.sp)
        holdings.forEach { h ->
            Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                CoinBadge(h.symbol)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { Text(h.name, color = Color.White); Text("${h.amount} ${h.symbol}", color = Muted, fontSize = 11.sp) }
                Text("$${fmt.format(h.value)}", color = GoldBright, fontWeight = FontWeight.Bold)
            }
        }
        Button(onClick = onEdit, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Bg), modifier = Modifier.fillMaxWidth()) { Text("编辑本机余额") }
        OutlinedButton(onClick = onReceive, modifier = Modifier.fillMaxWidth()) { Text("管理收款地址") }
    }
}

@Composable
private fun ReceiveDialog(address: String, onAddress: (String) -> Unit, onClose: () -> Unit) {
    val qr = remember(address) { if (address.isBlank()) null else makeQr(address) }
    AlertDialog(
        onDismissRequest = onClose, containerColor = Panel2,
        title = { Text("收款地址", color = GoldBright) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("请粘贴你自己持有的公开收款地址。应用不会替你生成区块链地址。", color = Muted, fontSize = 11.sp)
                OutlinedTextField(address, onAddress, label = { Text("公开收款地址") }, modifier = Modifier.fillMaxWidth())
                if (qr != null) {
                    Image(qr.asImageBitmap(), contentDescription = "收款地址二维码", modifier = Modifier.size(180.dp).background(Color.White, RoundedCornerShape(8.dp)).padding(8.dp))
                    Text("二维码仅编码你输入的文本；请核对网络和地址。", color = Muted, fontSize = 10.sp)
                } else {
                    Box(Modifier.size(160.dp).background(Color.White, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.QrCode2, null, tint = Bg, modifier = Modifier.size(90.dp))
                    }
                    Text("输入地址后生成二维码", color = Muted, fontSize = 10.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("完成", color = Gold) } }
    )
}

@Composable
private fun EditHoldingDialog(
    holdings: List<Holding>, symbol: String, name: String, amount: String, price: String,
    onSymbol: (String) -> Unit, onName: (String) -> Unit, onAmount: (String) -> Unit, onPrice: (String) -> Unit,
    onSave: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Panel2,
        title = { Text("编辑本机资产", color = GoldBright) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(symbol, onSymbol, label = { Text("币种代码") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(name, onName, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(amount, onAmount, label = { Text("数量") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(price, onPrice, label = { Text("估值单价 USD") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("保存", color = Gold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Muted) } }
    )
}

@Composable
private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier) {
    Column(modifier.background(Color(0xFF2B2519), RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Gold)
        Spacer(Modifier.height(4.dp))
        Text(label, color = GoldBright, fontSize = 10.sp)
    }
}

@Composable
private fun CoinBadge(symbol: String) {
    Box(Modifier.size(38.dp).background(Color(0xFF332B1D), CircleShape), contentAlignment = Alignment.Center) {
        Text(symbol.take(1), color = Gold, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CoinRow(coin: Coin) {
    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
        CoinBadge(coin.symbol)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) { Text(coin.symbol, color = Color.White, fontWeight = FontWeight.Bold); Text(coin.name, color = Muted, fontSize = 10.sp) }
        Column(horizontalAlignment = Alignment.End) {
            Text("$${DecimalFormat("#,##0.00").format(coin.price)}", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("${if (coin.change >= 0) "+" else ""}${DecimalFormat("0.00").format(coin.change)}%", color = if (coin.change >= 0) Up else Down, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SectionTitle(title: String, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(detail, color = Muted, fontSize = 9.sp)
    }
}

@Composable
private fun Sparkline(values: List<Double>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val min = values.minOrNull() ?: return@Canvas
        val max = values.maxOrNull() ?: return@Canvas
        val span = (max - min).takeIf { it > 0 } ?: 1.0
        val path = androidx.compose.ui.graphics.Path()
        values.forEachIndexed { i, v ->
            val x = size.width * i / (values.size - 1)
            val y = size.height - ((v - min) / span * (size.height * 0.85f)).toFloat() - size.height * 0.075f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx()))
    }
}

private fun makeQr(value: String): Bitmap? = try {
    val matrix = QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, 420, 420)
    Bitmap.createBitmap(420, 420, Bitmap.Config.ARGB_8888).also { bmp ->
        for (x in 0 until 420) for (y in 0 until 420) {
            bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
} catch (_: Exception) { null }
