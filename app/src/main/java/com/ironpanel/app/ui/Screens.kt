package com.ironpanel.app.ui

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ironpanel.app.R
import com.ironpanel.app.data.AppSnapshot
import com.ironpanel.app.vpn.VpnManager

// ---------- Shell ----------

@Composable
fun AppShell(vm: AppViewModel, onScanQr: () -> Unit) {
    val load by vm.load.collectAsState()
    var tab by remember { mutableStateOf(0) }
    Scaffold(
        bottomBar = {
            BottomAppBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Home, null) }, label = { Text(stringResource(R.string.home)) }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Add, null) }, label = { Text(stringResource(R.string.configs)) }
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Settings, null) }, label = { Text(stringResource(R.string.settings)) }
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> HomeTab(vm, onScanQr)
                1 -> ConfigsTab(vm)
                else -> SettingsTab(vm)
            }
            @Suppress("UNUSED_EXPRESSION")
            load
        }
    }
}

// ---------- Home ----------

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeTab(vm: AppViewModel, onScanQr: () -> Unit) {
    val load by vm.load.collectAsState()
    val protocol by vm.protocol.collectAsState()
    val vpnState by VpnManager.state.collectAsState()
    val context = LocalContext.current

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (val s = load) {
            is LoadState.Empty, is LoadState.Failed -> {
                if (s is LoadState.Failed) {
                    Card { Text(s.message, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error) }
                }
                AddSubCard(vm, onScanQr)
            }
            is LoadState.Loading -> {
                Box(Modifier.fillMaxWidth(), Alignment.Center) { CircularProgressIndicator() }
            }
            is LoadState.Ready -> {
                val snap = s.snapshot
                UsageCard(snap)
                // Protocol picker — only what the panel enabled for THIS user.
                Text(stringResource(R.string.configs), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    snap.user.protocols.forEach { p ->
                        FilterChip(
                            selected = protocol == p,
                            onClick = { vm.selectProtocol(p) },
                            label = { Text(p) }
                        )
                    }
                }
                if (snap.user.protocols.isEmpty()) {
                    Text(stringResource(R.string.expired_blocked), color = MaterialTheme.colorScheme.error)
                }
                // Gate message straight from the panel (volume / expiry).
                if (!snap.user.accessOk) {
                    Card {
                        Text(
                            snap.user.accessReason.ifBlank { stringResource(R.string.expired_blocked) },
                            Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                ConnectRow(vm, snap, protocol, vpnState)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.refresh))
                    }
                    TextButton(onClick = {
                        VpnManager.disconnect(context)
                        vm.forget()
                    }) { Text("× ${snap.user.username}") }
                }
            }
        }
    }
}

@Composable
fun AddSubCard(vm: AppViewModel, onScanQr: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.add_sub), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = text, onValueChange = { text = it; error = "" },
                label = { Text(stringResource(R.string.sub_hint)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.import(text) { error = it } }) { Text(stringResource(R.string.paste)) }
                OutlinedButton(onClick = onScanQr) { Text(stringResource(R.string.scan_qr)) }
            }
        }
    }
}

@Composable
fun UsageCard(snap: AppSnapshot) {
    val usage = snap.user.usage
    val fraction = if (usage.totalBytes > 0 && (usage.remainingBytes ?: 0) >= 0) {
        val rem = usage.remainingBytes ?: 0
        (1f - rem.toFloat() / usage.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val timeText = when {
        snap.user.unlimitedTime -> "∞"
        snap.user.remainingDays != null -> "${snap.user.remainingDays}d"
        else -> snap.user.expiresAt ?: "—"
    }
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            UsageRing(fraction)
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(snap.user.username, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${stringResource(R.string.remaining_volume)}: " +
                        if (usage.unlimitedTraffic) stringResource(R.string.unlimited)
                        else usage.remainingHuman.ifBlank { usage.totalHuman },
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "${stringResource(R.string.remaining_time)}: " +
                        if (snap.user.unlimitedTime) stringResource(R.string.unlimited) else timeText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun UsageRing(fraction: Float) {
    val color = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(Modifier.size(84.dp), Alignment.Center) {
        Canvas(Modifier.size(84.dp)) {
            drawArc(track, 0f, 360f, false, style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
            drawArc(color, -90f, 360f * fraction, false, style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
        }
        Text("${(fraction * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ConnectRow(vm: AppViewModel, snap: AppSnapshot, protocol: String, vpnState: VpnManager.State) {
    val context = LocalContext.current
    val activity = context as? Activity
    when (vpnState) {
        is VpnManager.State.Connected -> {
            Text("● ${vpnState.label}", color = MaterialTheme.colorScheme.secondary)
            Button(onClick = { VpnManager.disconnect(context) }, Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.disconnect))
            }
        }
        is VpnManager.State.Blocked -> {
            Text(vpnState.reasonFa.ifBlank { vpnState.reasonEn }, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { vm.refresh() }, Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.refresh))
            }
        }
        is VpnManager.State.Error -> {
            Text(vpnState.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            Button(
                onClick = { activity?.let { VpnManager.connect(it, snap, protocol) } },
                Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.connect)) }
        }
        VpnManager.State.Preparing -> {
            Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.connecting))
            }
        }
        VpnManager.State.Idle -> {
            val canConnect = snap.user.accessOk && snap.user.connectable &&
                protocol in snap.user.protocols
            Button(
                onClick = { activity?.let { VpnManager.connect(it, snap, protocol) } },
                enabled = canConnect, modifier = Modifier.fillMaxWidth()
            ) { Text("${stringResource(R.string.connect)} · $protocol") }
            if (!canConnect && snap.user.accessOk) {
                Text(stringResource(R.string.expired_blocked), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ---------- Configs ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConfigsTab(vm: AppViewModel) {
    val load by vm.load.collectAsState()
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val snap = (load as? LoadState.Ready)?.snapshot
        if (snap == null) {
            Text(stringResource(R.string.add_sub))
            return
        }
        Text("Xray (${snap.user.xrayLinks.size})", style = MaterialTheme.typography.titleMedium)
        snap.user.xrayLinks.forEachIndexed { i, link ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        com.ironpanel.app.data.XrayLinkParser.labelFor(link, i),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = {
                            com.ironpanel.app.vpn.ExternalApps.copyToClipboard(context, "Copied", link)
                        }, label = { Text(stringResource(R.string.copy_link)) })
                        AssistChip(onClick = {
                            val activity = context as? Activity
                            activity?.let {
                                VpnManager.connect(it, snap, "xray")
                            }
                        }, label = { Text(stringResource(R.string.open_in_app)) })
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Files (${snap.user.configs.size})", style = MaterialTheme.typography.titleMedium)
        snap.user.configs.keys.sorted().forEach { name ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, style = MaterialTheme.typography.bodyMedium)
                    AssistChip(onClick = {
                        com.ironpanel.app.vpn.ExternalApps.copyToClipboard(
                            context, "Copied", snap.user.configs[name].orEmpty()
                        )
                    }, label = { Text(stringResource(R.string.copy_link)) })
                }
            }
        }
    }
}

// ---------- Settings ----------

@Composable
fun SettingsTab(vm: AppViewModel) {
    val theme by vm.theme.collectAsState(initial = "system")
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("system" to R.string.theme_system, "light" to R.string.theme_light, "dark" to R.string.theme_dark)
                .forEach { (mode, label) ->
                    FilterChip(
                        selected = theme == mode,
                        onClick = { vm.saveTheme(mode) },
                        label = { Text(stringResource(label)) }
                    )
                }
        }
        Text(stringResource(R.string.about), style = MaterialTheme.typography.bodyMedium)
        Text("IronAPP 1.0.0 · panel ≥ 2.0.11 · Apache-2.0", style = MaterialTheme.typography.bodySmall)
    }
}
