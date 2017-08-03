package org.blokada.app

import org.blokada.framework.IWhen
import org.pcap4j.packet.*
import org.xbill.DNS.*
import java.io.IOException
import java.net.DatagramPacket
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.util.*

class DnsProxy(
        val s: State,
        val proxyEvents: IProxyEvents,
        val adBlocked: (String) -> Unit
) {

    private val NAME = Name("org.blokada.invalid.")
    private val BLOCK_CACHE_RESPONSE = SOARecord(NAME, DClass.IN, 5L, NAME, NAME, 0, 0, 0, 0, 5L);

    var block: Set<String> = setOf()
        @Synchronized get
        @Synchronized set

    var updateFilters = { filters: Set<String> ->
        block = filters
    }

    var listener: IWhen? = null

    init {
        listener = s.filtersCompiled.doWhenSet().then { updateFilters(s.filtersCompiled()) }
        updateFilters(s.filtersCompiled())
    }

    fun stop() {
        s.filtersCompiled.cancel(listener)
    }

    fun handleRequest(packetBytes: ByteArray) {
        val packet = try {
            IpSelector.newPacket(packetBytes, 0, packetBytes.size) as IpPacket
        } catch (e: Exception) {
            return
        }

        if (packet.payload !is UdpPacket) return

        val destination = rewriteDnsAddress(packet)
        val parsedUdp = packet.payload as UdpPacket

        if (parsedUdp.payload == null) {
            // Some apps may use empty UDP packets for something good
            val out = DatagramPacket(ByteArray(0), 0, 0, destination, parsedUdp.header.dstPort.valueAsInt())
            proxyEvents.forward(out, null)
            return
        }

        val rawData = parsedUdp.payload.rawData
        val message = try {
            Message(rawData)
        } catch (e: IOException) {
            return
        }
        if (message.question == null) return

        val questionName = message.question.name.toString(true)
        if (!isBlocked(questionName)) {
            val out = DatagramPacket(rawData, 0, rawData.size, destination, parsedUdp.header.dstPort.valueAsInt())
            proxyEvents.forward(out, packet)
        } else {
            message.header.setFlag(Flags.QR.toInt())
            message.header.rcode = Rcode.NOERROR
            message.addRecord(BLOCK_CACHE_RESPONSE, Section.AUTHORITY)
            handleResponse(packet, message.toWire())
            adBlocked(questionName)
        }
    }

    fun handleResponse(packet: IpPacket, payload: ByteArray) {
        val out = packet.payload as UdpPacket
        val builder = UdpPacket.Builder(out)
                .srcAddr(packet.header.dstAddr)
                .dstAddr(packet.header.srcAddr)
                .srcPort(out.header.dstPort)
                .dstPort(out.header.srcPort)
                .correctChecksumAtBuild(true)
                .correctLengthAtBuild(true)
                .payloadBuilder(UnknownPacket.Builder().rawData(payload))

        val ipOut: IpPacket
        if (packet is IpV4Packet) {
            ipOut = IpV4Packet.Builder(packet)
                    .srcAddr(packet.header.dstAddr as Inet4Address)
                    .dstAddr(packet.header.srcAddr as Inet4Address)
                    .correctChecksumAtBuild(true)
                    .correctLengthAtBuild(true)
                    .payloadBuilder(builder)
                    .build()
        } else {
            ipOut = IpV6Packet.Builder(packet as IpV6Packet)
                    .srcAddr(packet.header.dstAddr as Inet6Address)
                    .dstAddr(packet.header.srcAddr as Inet6Address)
                    .correctLengthAtBuild(true)
                    .payloadBuilder(builder)
                    .build()
        }

        proxyEvents.loopback(ipOut)
    }

    private fun rewriteDnsAddress(packet: IpPacket): InetAddress {
        val servers = s.connection().dnsServers
        val current = packet.header.dstAddr
        return when {
            servers.isEmpty() -> current
            else -> try {
                // Last octet of DNS server IP is equal corresponds to its index
                val index = current.address.last() - 2
                servers[index]
            } catch (e: Exception) {
                current
            }
        }
    }

    @Synchronized fun isBlocked(host: String): Boolean {
        return block.contains(host.toLowerCase(Locale.ENGLISH))
    }

}
