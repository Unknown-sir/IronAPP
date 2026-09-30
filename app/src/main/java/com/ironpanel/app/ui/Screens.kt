package com.ironpanel.app.ui

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
                .background(heroBrush(MaterialTheme.colorScheme.background == CoalBlack))
        ) {
            EmberBlobs()
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 6 })
                        .togetherWith(fadeOut(tween(160)))
                },
                label = "tabs"
            ) { t ->
                when (t) {
                    0 -> HomeTab(vm, onScanQr, onShowConfigs = { tab = 1 })
                    1 -> ConfigsTab(vm)
                    else -> SettingsTab(vm)
                }
            }
        }
    }
}

/** Slow-drifting ember glows behind everything. */
@Composable
fun EmberBlobs() {
    val drift = rememberInfiniteTransition(label = "drift")
    val dx by drift.animateFloat(
        initialValue = -30f, targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dx"
    )
    Canvas(Modifier.fillMaxSize()) {
        drawCircle(
            Brush.radialGradient(
                listOf(BloodRed.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(size.width * 0.85f + dx, size.height * 0.06f),
                radius = size.minDimension * 0.55f
            ),
            radius = size.minDimension * 0.55f,
            center = Offset(size.width * 0.85f + dx, size.height * 0.06f)
        )
        drawCircle(
            Brush.radialGradient(
                listOf(DeepRed.copy(alpha = 0.28f), Color.Transparent),
                center = Offset(size.width * 0.08f - dx, size.height * 0.38f),
                radius = size.minDimension * 0.62f
            ),
            radius = size.minDimension * 0.62f,
            center = Offset(size.width * 0.08f - dx, size.height * 0.38f)
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
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { 60 },
        label = "onboard"
    ) {
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🛡️", fontSize = 40.sp)
                Text(
                    "IronAPP",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.add_sub),
                    style = MaterialTheme.typography.titleMedium,
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
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
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
}

@Composable
fun ConnectedHome(vm: AppViewModel, snap: AppSnapshot, baseUrl: String, onShowConfigs: () -> Unit) {
    val protocol by vm.protocol.collectAsState()
    val vpnState by VpnManager.state.collectAsState()
    val speeds by TrafficMonitor.speeds.collectAsState()
    val history by TrafficMonitor.history.collectAsState()
    val context = LocalContext.current
    val connected = vpnState is VpnManager.State.Connected
    val preparing = vpnState is VpnManager.State.Preparing

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { 40 },
        label = "home"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(emberBrush()),
                    Alignment.Center
                ) {
                    Text(
                        snap.user.username.firstOrNull()?.uppercase() ?: "U",
                        color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp
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
            Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), Alignment.Center) {
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

            // live speeds + waveform
            if (connected) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    SpeedChip("↓", TrafficMonitor.format(speeds.downBps))
                    SpeedChip("↑", TrafficMonitor.format(speeds.upBps))
                }
                SpeedGraph(history)
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
            UsageEmberCard(snap)

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
    }
}

@Composable
fun StatusPill(connected: Boolean) {
    val pulse = rememberInfiniteTransition(label = "dot")
    val alpha by pulse.animateFloat(
        initialValue = 1f, targetValue = if (connected) 0.35f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900), repeatMode = RepeatMode.Reverse
        ),
        label = "dot"
    )
    val bg = if (connected) GoodGreen.copy(alpha = 0.16f)
    else BloodRed.copy(alpha = 0.12f)
    val fg = if (connected) GoodGreen else BloodRed
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(Modifier.size(8.dp)) { drawCircle(fg.copy(alpha = alpha)) }
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
        initialValue = 0.96f, targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val spin by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(1600)),
        label = "spin"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (connected) 1.04f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "press"
    )
    Box(
        Modifier
            .size(184.dp)
            .scale(pressScale),
        Alignment.Center
    ) {
        if (connected) {
            Canvas(Modifier.size((184 * pulse).dp)) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(BloodRed.copy(alpha = 0.4f), Color.Transparent)
                    )
                )
            }
        }
        Canvas(Modifier.size(152.dp)) {
            if (busy) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(BloodRed, Color.Transparent, BloodRed)),
                    startAngle = spin, sweepAngle = 300f, useCenter = false,
                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                )
            } else {
                drawArc(
                    brush = if (connected) Brush.sweepGradient(
                        listOf(BloodRed, EmberOrange, BloodRed)
                    ) else Brush.sweepGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            BloodRed.copy(alpha = 0.65f),
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    ),
                    startAngle = -90f, sweepAngle = 360f, useCenter = false,
                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
        Box(
            Modifier
                .size(116.dp)
                .clip(CircleShape)
                .background(
                    if (connected) Brush.linearGradient(listOf(BloodRed, DeepRed))
                    else Brush.linearGradient(
                        listOf(Color(0xFF141419), Color(0xFF08080B))
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            Alignment.Center
        ) {
            if (busy) {
                CircularProgressIndicator(
                    Modifier.size(40.dp),
                    color = BloodRed,
                    strokeWidth = 4.dp
                )
            } else {
                Icon(
                    Icons.Filled.PowerSettingsNew, null,
                    Modifier.size(54.dp),
                    tint = if (connected) Color.White else BloodRed
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
            .background(BloodRed.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(dir, fontWeight = FontWeight.Black, color = BloodRed)
        Spacer(Modifier.width(6.dp))
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}

/** Live download waveform drawn from the last 40 samples. */
@Composable
fun SpeedGraph(history: List<Long>) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        )
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            if (history.size < 2) return@Canvas
            val max = (history.maxOrNull() ?: 1L).coerceAtLeast(1L).toFloat()
            val stepX = size.width / (history.size - 1)
            val path = Path()
            history.forEachIndexed { i, v ->
                val x = i * stepX
                val y = size.height - (v.toFloat() / max) * size.height * 0.92f - size.height * 0.04f
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val fill = Path().apply {
                addPath(path)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                fill,
                Brush.verticalGradient(listOf(BloodRed.copy(alpha = 0.35f), Color.Transparent))
            )
            drawPath(path, BloodRed, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
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
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "card"
    )
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) BloodRed.copy(alpha = 0.16f)
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ),
        border = if (selected) androidx.compose.foundation.BorderStroke(
            1.5.dp, BloodRed
        ) else CardDefaults.outlinedCardBorder(),
        modifier = Modifier.scale(scale)
    ) {
        Column(
            Modifier.width(108.dp).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(emberBrush()),
                Alignment.Center
            ) {
                Text(
                    name.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White, fontWeight = FontWeight.Black
                )
            }
            Text(name, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                stringResource(if (engine) R.string.one_tap else R.string.view_only),
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) BloodRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun UsageEmberCard(snap: AppSnapshot) {
    val usage = snap.user.usage
    val target = if (usage.totalBytes > 0) {
        val rem = usage.remainingBytes ?: usage.totalBytes
        (1f - rem.toFloat() / usage.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val fraction by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "usage"
    )
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.remaining_volume), fontWeight = FontWeight.SemiBold)
                Text(
                    if (usage.unlimitedTraffic) stringResource(R.string.unlimited)
                    else usage.remainingHuman.ifBlank { usage.totalHuman },
                    fontWeight = FontWeight.Black, color = BloodRed
                )
            }
            Canvas(Modifier.fillMaxWidth().height(12.dp)) {
                drawRoundRect(
                    Color(0xFF2A2A32),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
                drawRoundRect(
                    Brush.horizontalGradient(listOf(DeepRed, BloodRed, EmberOrange)),
                    size = Size(size.width * fraction, size.height),
                    cornerRadius = CornerRadius(6.dp.toPx())
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
    val customs by vm.customConfigs.collectAsState(initial = emptyList())
    val context = LocalContext.current
    var showAdd by remember { mutableStateOf(false) }
    if (showAdd) AddCustomDialog(vm, onClose = { showAdd = false })
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.my_configs),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
            )
            AssistChip(onClick = { showAdd = true }, label = { Text("+ ${stringResource(R.string.add_config)}") })
        }
        Text(
            stringResource(R.string.my_configs_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (customs.isEmpty()) {
            Text(
                stringResource(R.string.no_custom_yet),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        customs.forEach { custom ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            custom.name, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f), maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            custom.kind,
                            style = MaterialTheme.typography.labelSmall,
                            color = BloodRed
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = {
                                val activity = context as? Activity ?: return@AssistChip
                                try {
                                    val node = AppViewModel.customNode(custom.kind, custom.payload)
                                    VpnManager.connectNode(
                                        activity, "${custom.kind} · ${custom.name}", node
                                    )
                                } catch (e: Exception) {
                                    Share.copy(context, "Config error", e.message ?: "bad config")
                                }
                            },
                            label = { Text(stringResource(R.string.connect)) }
                        )
                        AssistChip(
                            onClick = { vm.deleteCustomConfig(custom.id) },
                            label = { Text(stringResource(R.string.delete)) }
                        )
                    }
                }
            }
        }
        val snap = (load as? LoadState.Ready)?.snapshot
        if (snap == null) {
            return@Column
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

@Composable
fun AddCustomDialog(vm: AppViewModel, onClose: () -> Unit) {
    var kind by remember { mutableStateOf("xray") }
    var name by remember { mutableStateOf("") }
    var payload by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.add_config)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("xray", "wireguard", "openvpn").forEach { k ->
                        FilterChip(
                            selected = kind == k,
                            onClick = { kind = k },
                            label = { Text(k) }
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.config_name)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = payload,
                    onValueChange = { payload = it; error = "" },
                    label = { Text(stringResource(R.string.config_payload_hint)) },
                    modifier = Modifier.fillMaxWidth().height(140.dp)
                )
                if (error.isNotEmpty()) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                vm.addCustomConfig(name, kind, payload) { error = it }
                if (error.isEmpty()) onClose()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text(stringResource(R.string.cancel)) }
        }
    )
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
                Text("IronAPP 1.3.0", fontWeight = FontWeight.Black, color = BloodRed)
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
