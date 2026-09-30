package com.ironpanel.app.vpn.box

import com.ironpanel.libbox.NetworkInterfaceIterator
import com.ironpanel.libbox.StringIterator

/** gomobile StringIterator backed by a Kotlin list. */
class StringArray(source: Iterator<String>) : StringIterator {
    private val items = source.asSequence().toList()
    private var index = 0
    override fun len(): Int = items.size
    override fun hasNext(): Boolean = index < items.size
    override fun next(): String = if (index < items.size) items[index++] else ""
}

/** gomobile NetworkInterfaceIterator backed by a Kotlin list. */
class InterfaceArray(
    private val items: List<com.ironpanel.libbox.NetworkInterface>,
) : NetworkInterfaceIterator {
    private var index = 0
    override fun hasNext(): Boolean = index < items.size
    override fun next(): com.ironpanel.libbox.NetworkInterface = items[index++]
}
