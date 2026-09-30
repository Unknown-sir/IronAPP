package com.ironpanel.app.vpn

import android.content.Context
import android.net.TrafficStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class Speeds(val downBps: Long = 0, val upBps: Long = 0)

/** Live up/down speeds from per-UID TrafficStats deltas (no core API needed). */
object TrafficMonitor {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private val _speeds = MutableStateFlow(Speeds())
    val speeds: StateFlow<Speeds> = _speeds

    /** Rolling history of download Bps (last 40s) for the speed graph. */
    private val _history = MutableStateFlow<List<Long>>(emptyList())
    val history: StateFlow<List<Long>> = _history

    fun start(context: Context) {
        stop()
        job = scope.launch {
            val uid = context.applicationInfo.uid
            var lastRx = TrafficStats.getUidRxBytes(uid).coerceAtLeast(0)
            var lastTx = TrafficStats.getUidTxBytes(uid).coerceAtLeast(0)
            var lastT = System.currentTimeMillis()
            while (true) {
                delay(1000)
                val now = System.currentTimeMillis()
                val dt = ((now - lastT).coerceAtLeast(1)).toDouble() / 1000.0
                val rx = TrafficStats.getUidRxBytes(uid).coerceAtLeast(0)
                val tx = TrafficStats.getUidTxBytes(uid).coerceAtLeast(0)
                _speeds.value = Speeds(
                    downBps = ((rx - lastRx).coerceAtLeast(0) / dt).toLong(),
                    upBps = ((tx - lastTx).coerceAtLeast(0) / dt).toLong(),
                )
                _history.value = (_history.value + _speeds.value.downBps).takeLast(40)
                lastRx = rx
                lastTx = tx
                lastT = now
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _speeds.value = Speeds()
        _history.value = emptyList()
    }

    fun format(bps: Long): String {
        val bs = bps * 8
        return when {
            bs < 1_000 -> "$bs b/s"
            bs < 1_000_000 -> String.format("%.1f Kb/s", bs / 1_000.0)
            bs < 1_000_000_000 -> String.format("%.1f Mb/s", bs / 1_000_000.0)
            else -> String.format("%.2f Gb/s", bs / 1_000_000_000.0)
        }
    }
}
