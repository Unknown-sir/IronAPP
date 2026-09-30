package com.ironpanel.app.ui

import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ironpanel.app.R
import com.ironpanel.app.data.AppSnapshot
import com.ironpanel.app.vpn.MtprotoConnector
import com.ironpanel.app.vpn.Share
import com.ironpanel.app.vpn.TrafficMonitor
import com.ironpanel.app.vpn.VpnManager

// ============================== shell ==============================

@Composable
fun AppShell(vm: AppViewModel, onScanQr: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Home, null) },
                    label = { Text(stringResource(R.string.home)) }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Add, null) },
                    label = { Text(stringResource(R.string.configs)) }
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Settings, null) },
                    label = { Text(stringResource(R.string.settings)) }
                )
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(heroBrush(MaterialTheme.colorScheme.background == SpaceBlack))
        ) {
            AuroraBlobs()
            when (tab) {
                0 -> HomeTab(vm, onScanQr, onShowConfigs = { tab = 1 })
                1 -> ConfigsTab(vm)
                else -> SettingsTab(vm)
            }
        }
    }
}

/** Soft aurora blobs behind everything (2026 VPN-app look). */
@Composable
fun AuroraBlobs() {
    Canvas(Modifier.fillMaxSize()) {
        drawCircle(
            Brush.radialGradient(
                listOf(AuroraViolet.copy(alpha = 0.25f), Color.Transparent),
                center = Offset(size.width * 0.85f, size.height * 0.08f),
                radius = size.minDimension * 0.55f
            ),
            radius = size.minDimension * 0.55f,
            center = Offset(size.width * 0.85f, size.height * 0.08f)
        )
        drawCircle(
            Brush.radialGradient(
                listOf(AuroraCyan.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(size.width * 0.1f, size.height * 0.32f),
                radius = size.minDimension * 0.6f
            ),
            radius = size.minDimension * 0.6f,
            center = Offset(size.width * 0.1f, size.height * 0.32f)
        )
    }
}

// ============================== home ==============================

@Composable
fun HomeTab(vm: AppViewModel, onScanQr: () -> Unit, onShowConfigs: () -> Unit) {
    val load by vm.load.collectAsState()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when (val s = load) {
            is LoadState.Empty, is LoadState.Failed -> {
                if (s is LoadState.Failed) {
                    StatusBanner(s.message, isError = true)
                }
                OnboardingCard(vm, onScanQr)
            }
            is LoadState.Loading -> {
                Box(Modifier.fillMaxWidth().padding(top = 80.dp), Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is LoadState.Ready -> ConnectedHome(vm, s.snapshot, s.baseUrl, onShowConfigs)
        }
    }
}

@Composable
fun OnboardingCard(vm: AppViewModel, onScanQr: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("⚡", fontSize = 40.sp)
            Text(
                stringResource(R.string.add_sub),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.onboarding_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it; error = "" },
                label = { Text(stringResource(R.string.sub_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { vm.import(text) { error = it } },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) { Text(stringResource(R.string.paste)) }
                OutlinedButton(onClick = onScanQr, shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Filled.QrCodeScanner, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.scan_qr))
                }
            }
        }
    }
}

@Composable
fun ConnectedHome(vm: AppViewModel, snap: AppSnapshot, baseUrl: String, onShowConfigs: () -> Unit) {
    val protocol by vm.protocol.collectAsState()
    val vpnState by VpnManager.state.collectAsState()
    val speeds by TrafficMonitor.speeds.collectAsState()
    val context = LocalContext.current
    val connected = vpnState is VpnManager.State.Connected
    val preparing = vpnState is VpnManager.State.Preparing

    // header
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(auroraBrush()),
            Alignment.Center
        ) {
            Text(
                snap.user.username.firstOrNull()?.uppercase() ?: "U",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                snap.user.username,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                baseUrl.removePrefix("http://").removePrefix("https://"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        StatusPill(connected = connected)
        IconButton(onClick = { vm.refresh() }) { Icon(Icons.Filled.Refresh, null) }
    }

    // hero power button
    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), Alignment.Center) {
        PowerButton(
            connected = connected,
            busy = preparing,
            onClick = {
                val activity = context as? Activity ?: return@PowerButton
                when {
                    connected -> VpnManager.disconnect(context)
                    protocol == "telegram_proxy" -> MtprotoConnector.open(context, snap)
                    VpnManager.isEngineProtocol(protocol) ->
                        VpnManager.connect(activity, snap, protocol)
                    else -> onShowConfigs()
                }
            }
        )
    }
    Text(
        when {
            connected -> (vpnState as VpnManager.State.Connected).label
            preparing -> stringResource(R.string.connecting)
            !snap.user.accessOk -> snap.user.accessReason.ifBlank { stringResource(R.string.expired_blocked) }
            else -> stringResource(R.string.tap_to_connect, protocol)
        },
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
        color = if (!snap.user.accessOk && !connected) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurfaceVariant
    )

    // live speeds
    if (connected) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            SpeedChip("↓", TrafficMonitor.format(speeds.downBps))
            SpeedChip("↑", TrafficMonitor.format(speeds.upBps))
        }
    }

    // error / blocked banner
    when (val st = vpnState) {
        is VpnManager.State.Blocked ->
            StatusBanner(st.reasonFa.ifBlank { st.reasonEn }, isError = true)
        is VpnManager.State.Error -> StatusBanner(st.message, isError = true)
        else -> Unit
    }
    if (!snap.user.accessOk && !connected) {
        OutlinedButton(onClick = { vm.refresh() }, Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.refresh))
        }
    }

    // protocols carousel
    Text(stringResource(R.string.configs), style = MaterialTheme.typography.titleMedium)
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(end = 4.dp)
    ) {
        itemsIndexed(snap.user.protocols) { _, p ->
            ProtocolCard(
                name = p,
                selected = protocol == p,
                engine = VpnManager.isEngineProtocol(p) || p == "telegram_proxy",
                onClick = { vm.selectProtocol(p) }
            )
        }
    }

    // usage + expiry glass card
    UsageGlassCard(snap)

    // view-only credential hint for legacy protocols
    if (!VpnManager.isEngineProtocol(protocol) && protocol != "telegram_proxy" &&
        snap.user.protocols.contains(protocol)
    ) {
        CredentialHintCard(snap, protocol)
    }

    // footer
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
                    onClick = {
                VpnManager.disconnect(context)
                vm.forget()
            },
            modifier = Modifier.weight(1f)
        ) { Text(stringResource(R.string.switch_account)) }
        OutlinedButton(onClick = { vm.refresh() }, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.refresh))
        }
    }
}

@Composable
fun StatusPill(connected: Boolean) {
    val bg = if (connected) GoodGreen.copy(alpha = 0.16f)
    else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (connected) GoodGreen else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(Modifier.size(8.dp)) { drawCircle(fg) }
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(if (connected) R.string.protected_ else R.string.not_protected),
            style = MaterialTheme.typography.labelMedium, color = fg
        )
    }
}

@Composable
fun PowerButton(connected: Boolean, busy: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.96f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    Box(
        Modifier.size(176.dp),
        Alignment.Center
    ) {
        val trackIdle = MaterialTheme.colorScheme.surfaceVariant
        val trackGlow = MaterialTheme.colorScheme.primary
        if (connected) {
            Canvas(Modifier.size((176 * pulse).dp)) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(GoodGreen.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
            }
        }
        Canvas(Modifier.size(150.dp)) {
            drawArc(
                brush = if (connected) Brush.sweepGradient(listOf(GoodGreen, AuroraCyan, GoodGreen))
                else Brush.sweepGradient(listOf(trackIdle, trackGlow, trackIdle)),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Box(
            Modifier
                .size(116.dp)
                .clip(CircleShape)
                .background(
                    if (connected) Brush.linearGradient(listOf(GoodGreen, AuroraCyan))
                    else Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .clickable(onClick = onClick),
            Alignment.Center
        ) {
            if (busy) {
                CircularProgressIndicator(
                    Modifier.size(40.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
            } else {
                Icon(
                    Icons.Filled.PowerSettingsNew, null,
                    Modifier.size(52.dp),
                    tint = if (connected) Color.White else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SpeedChip(dir: String, value: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(dir, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun StatusBanner(message: String, isError: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(message, Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ProtocolCard(name: String, selected: Boolean, engine: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ),
        border = if (selected) null else CardDefaults.outlinedCardBorder()
    ) {
        Column(
            Modifier.width(108.dp).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(auroraBrush()),
                Alignment.Center
            ) {
                Text(
                    name.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White, fontWeight = FontWeight.Bold
                )
            }
            Text(name, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                stringResource(if (engine) R.string.one_tap else R.string.view_only),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun UsageGlassCard(snap: AppSnapshot) {
    val usage = snap.user.usage
    val fraction = if (usage.totalBytes > 0) {
        val rem = usage.remainingBytes ?: usage.totalBytes
        (1f - rem.toFloat() / usage.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        )
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.remaining_volume), fontWeight = FontWeight.SemiBold)
                Text(
                    if (usage.unlimitedTraffic) stringResource(R.string.unlimited)
                    else usage.remainingHuman.ifBlank { usage.totalHuman },
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
                )
            }
            Canvas(Modifier.fillMaxWidth().height(10.dp)) {
                drawRoundRect(
                    trackColor,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx())
                )
                drawRoundRect(
                    Brush.horizontalGradient(listOf(AuroraCyan, AuroraViolet)),
                    size = androidx.compose.ui.geometry.Size(size.width * fraction, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx())
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${stringResource(R.string.used)}: ${usage.usedHuman}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${stringResource(R.string.remaining_time)}: " +
                        if (snap.user.unlimitedTime) stringResource(R.string.unlimited)
                        else snap.user.remainingDays?.let { "$it ${stringResource(R.string.days)}" }
                            ?: snap.user.expiresAt ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CredentialHintCard(snap: AppSnapshot, protocol: String) {
    val body = when (protocol) {
        "l2tp" -> snap.user.configs["l2tp.txt"].orEmpty()
        "pptp" -> snap.user.configs["pptp.txt"].orEmpty()
        else -> ""
    }
    val context = LocalContext.current
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.legacy_proto_hint),
                style = MaterialTheme.typography.bodyMedium
            )
            if (body.isNotBlank()) {
                Text(
                    body, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AssistChip(
                    onClick = { Share.copy(context, "Copied", body) },
                    label = { Text(stringResource(R.string.copy_link)) }
                )
            }
        }
    }
}

// ============================== configs ==============================

@Composable
fun ConfigsTab(vm: AppViewModel) {
    val load by vm.load.collectAsState()
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val snap = (load as? LoadState.Ready)?.snapshot
        if (snap == null) {
            Text(stringResource(R.string.add_sub))
            return
        }
        Text(
            "Xray · ${snap.user.xrayLinks.size}",
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
        )
        snap.user.xrayLinks.forEachIndexed { i, link ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        com.ironpanel.app.data.XrayLinkParser.labelFor(link, i),
                        fontWeight = FontWeight.SemiBold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = {
                                val activity = context as? Activity ?: return@AssistChip
                                VpnManager.connect(activity, snap, "xray", i)
                            },
                            label = { Text(stringResource(R.string.connect)) }
                        )
                        AssistChip(
                            onClick = { Share.copy(context, "Copied", link) },
                            label = { Text(stringResource(R.string.copy_link)) }
                        )
                    }
                }
            }
        }
        if (snap.user.configs.containsKey("telegram_proxy.txt")) {
            AssistChip(
                onClick = { MtprotoConnector.open(context, snap) },
                label = { Text(stringResource(R.string.open_telegram)) }
            )
        }
        Text(
            "${stringResource(R.string.files)} · ${snap.user.configs.size}",
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
        )
        snap.user.configs.keys.sorted().forEach { name ->
            Card(shape = RoundedCornerShape(16.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, modifier = Modifier.weight(1f), maxLines = 1)
                    AssistChip(
                        onClick = { Share.copy(context, "Copied", snap.user.configs[name].orEmpty()) },
                        label = { Text(stringResource(R.string.copy_link)) }
                    )
                }
            }
        }
    }
}

// ============================== settings ==============================

@Composable
fun SettingsTab(vm: AppViewModel) {
    val theme by vm.theme.collectAsState(initial = "system")
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "system" to R.string.theme_system,
                "light" to R.string.theme_light,
                "dark" to R.string.theme_dark
            ).forEach { (mode, label) ->
                FilterChip(
                    selected = theme == mode,
                    onClick = { vm.saveTheme(mode) },
                    label = { Text(stringResource(label)) }
                )
            }
        }
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("IronAPP 1.1.0", fontWeight = FontWeight.Bold)
                Text(
                    "panel ≥ 2.0.11 · GPL-3.0-or-later",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.core_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.about),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
