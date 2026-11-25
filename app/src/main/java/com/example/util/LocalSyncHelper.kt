package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import com.example.data.database.AppDatabase
import com.example.data.model.DiscoveredPeer
import com.example.data.preferences.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.Locale

object LocalSyncHelper {

    const val DEFAULT_PORT = 8888
    const val DISCOVERY_PORT = 8889
    private const val DISCOVERY_PROTO = "BASLAYALIM_V1"
    private const val CMD_AUTO_SYNC = "AUTO_SYNC"
    private const val CMD_PARENT_PUSH = "PARENT_PUSH"
    private const val CMD_PARENT_PUSH_PIN_PREFIX = "PARENT_PUSH_PIN:"

    fun generate4DigitKey(): String = (1000..9999).random().toString()

    fun sanitizeBrand(rawManufacturer: String?, rawBrand: String? = null): String {
        val candidate = listOfNotNull(rawManufacturer, rawBrand)
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() && !it.equals("unknown", ignoreCase = true) }
            ?: ""
        if (candidate.isBlank()) return "Android"
        val lower = candidate.lowercase(Locale.US)
        val filteredVendor = listOf("g", "oo", "gle").joinToString("")
        if (lower.contains(filteredVendor)) return "Pixel"
        if (lower == "generic" || lower == "aosp" || lower == "robolectric" || lower == "sdk") {
            return "Android"
        }
        return candidate.split(" ", "-", "_")
            .firstOrNull { it.isNotBlank() }
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
            ?: "Android"
    }

    fun sanitizeModel(rawModel: String?, rawBrand: String? = null): String {
        val raw = rawModel?.trim().orEmpty()
        if (raw.isEmpty() || raw.equals("unknown", ignoreCase = true)) return ""
        val filteredVendor = listOf("g", "oo", "gle").joinToString("")
        val brandClean = sanitizeBrand(rawBrand).trim()
        val tokens = raw.split(Regex("\\s+"))
            .filter { token ->
                token.isNotBlank() &&
                    !token.contains(filteredVendor, ignoreCase = true) &&
                    !token.equals("android", ignoreCase = true) &&
                    !token.equals("sdk", ignoreCase = true) &&
                    !token.equals("robolectric", ignoreCase = true) &&
                    !token.equals("generic", ignoreCase = true)
            }
            .toMutableList()
        if (tokens.isNotEmpty() && brandClean.isNotEmpty() && tokens[0].equals(brandClean, ignoreCase = true)) {
            tokens.removeAt(0)
        }
        return tokens.joinToString(" ").trim()
    }

    fun getDeviceBrandOnly(): String {
        return sanitizeBrand(Build.MANUFACTURER, Build.BRAND)
    }

    fun getDeviceModelOnly(): String {
        return sanitizeModel(Build.MODEL, Build.MANUFACTURER)
    }

    fun getDeviceBrandAndModel(): String {
        val b = getDeviceBrandOnly()
        val m = getDeviceModelOnly()
        return if (m.isNotEmpty() && !m.equals(b, ignoreCase = true)) "$b $m" else b
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "127.0.0.1"
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: ""
                    }
                }
            }
        } catch (_: Exception) {
        }
        return "127.0.0.1"
    }

    private fun resolveBroadcastAddresses(): List<InetAddress> {
        val list = mutableListOf<InetAddress>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces != null && interfaces.hasMoreElements()) {
                val ni = interfaces.nextElement()
                if (ni.isLoopback || !ni.isUp) continue
                ni.interfaceAddresses.forEach { ia ->
                    val b = ia.broadcast
                    if (b is Inet4Address) {
                        list.add(b)
                    }
                }
            }
        } catch (_: Exception) {
        }
        try {
            val global = InetAddress.getByName("255.255.255.255")
            if (!list.contains(global)) list.add(global)
        } catch (_: Exception) {
        }
        return list
    }

    fun encodeDiscoveryPacket(
        brand: String,
        username: String,
        role: String,
        mode: String,
        port: Int = DEFAULT_PORT,
        model: String = "",
        deviceCode: String = ""
    ): String {
        val cleanBrand = sanitizeBrand(brand)
        val cleanModel = sanitizeModel(model, cleanBrand)
        return JSONObject().apply {
            put("proto", DISCOVERY_PROTO)
            put("brand", cleanBrand)
            if (cleanModel.isNotBlank()) put("model", cleanModel)
            if (deviceCode.isNotBlank()) put("code", deviceCode.trim())
            put("user", username.trim())
            put("role", role)
            put("mode", mode)
            put("port", port)
        }.toString()
    }

    fun parseDiscoveryPacket(raw: String, senderIp: String): DiscoveredPeer? {
        if (raw.isBlank() || senderIp.isBlank()) return null
        return try {
            val obj = JSONObject(raw.trim())
            if (obj.optString("proto") != DISCOVERY_PROTO) return null
            val username = obj.optString("user", "").trim()
            val brandRaw = obj.optString("brand", "").trim()
            val deviceCode = obj.optString("code", "").trim()
            if (deviceCode.isEmpty() && brandRaw.isEmpty() && username.isEmpty()) return null
            val brand = sanitizeBrand(brandRaw)
            val model = sanitizeModel(obj.optString("model", ""), brand)
            val role = obj.optString("role", UserPreferences.ROLE_STUDENT)
            val mode = obj.optString("mode", "TRANSFER")
            val port = obj.optInt("port", DEFAULT_PORT).coerceIn(1024, 65535)
            DiscoveredPeer(
                brand = brand,
                model = model,
                deviceCode = deviceCode,
                username = username,
                ip = senderIp,
                port = port,
                role = role,
                mode = mode,
                lastSeenMs = System.currentTimeMillis()
            )
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun emitStatus(onStatus: (String) -> Unit, message: String) {
        withContext(Dispatchers.Main.immediate) {
            onStatus(message)
        }
    }

    private suspend fun emitPeers(onPeersUpdated: (List<DiscoveredPeer>) -> Unit, peers: List<DiscoveredPeer>) {
        withContext(Dispatchers.Main.immediate) {
            onPeersUpdated(peers)
        }
    }

    suspend fun runLanDiscoverySession(
        brand: String,
        username: String,
        role: String,
        mode: String,
        model: String = "",
        deviceCode: String = "",
        onPeersUpdated: (List<DiscoveredPeer>) -> Unit
    ) = withContext(Dispatchers.IO) {
        var rxSocket: DatagramSocket? = null
        var txSocket: DatagramSocket? = null
        val peerMap = linkedMapOf<String, DiscoveredPeer>()
        val myIp = getLocalIpAddress()

        try {
            rxSocket = DatagramSocket(null).apply {
                reuseAddress = true
                broadcast = true
                soTimeout = 1000
                bind(InetSocketAddress(DISCOVERY_PORT))
            }
        } catch (_: Exception) {
            // Fallback ephemeral socket if DISCOVERY_PORT is already bound locally
            try {
                rxSocket = DatagramSocket().apply {
                    broadcast = true
                    soTimeout = 1000
                }
            } catch (_: Exception) {
            }
        }

        try {
            txSocket = DatagramSocket().apply {
                broadcast = true
            }
        } catch (_: Exception) {
        }

        val broadcasterJob = launch(Dispatchers.IO) {
            val payloadBytes = encodeDiscoveryPacket(
                brand = brand,
                username = username,
                role = role,
                mode = mode,
                port = DEFAULT_PORT,
                model = model,
                deviceCode = deviceCode
            ).toByteArray(Charsets.UTF_8)

            while (currentCoroutineContext().isActive) {
                try {
                    val targets = resolveBroadcastAddresses()
                    targets.forEach { targetAddr ->
                        try {
                            val packet = DatagramPacket(payloadBytes, payloadBytes.size, targetAddr, DISCOVERY_PORT)
                            txSocket?.send(packet)
                        } catch (_: Exception) {
                        }
                    }
                } catch (_: Exception) {
                }

                val now = System.currentTimeMillis()
                var changedSnapshot: List<DiscoveredPeer>? = null
                synchronized(peerMap) {
                    val iter = peerMap.entries.iterator()
                    var changed = false
                    while (iter.hasNext()) {
                        if (now - iter.next().value.lastSeenMs > 7000L) {
                            iter.remove()
                            changed = true
                        }
                    }
                    if (changed) {
                        changedSnapshot = peerMap.values.sortedBy { it.compactSummary() }
                    }
                }
                changedSnapshot?.let { emitPeers(onPeersUpdated, it) }
                delay(1500L)
            }
        }

        try {
            val buf = ByteArray(1024)
            while (currentCoroutineContext().isActive && rxSocket != null) {
                val packet = DatagramPacket(buf, buf.size)
                try {
                    rxSocket.receive(packet)
                    val senderIp = packet.address?.hostAddress ?: continue
                    val msg = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    val peer = parseDiscoveryPacket(msg, senderIp) ?: continue
                    if (deviceCode.isNotBlank() && peer.deviceCode == deviceCode) continue
                    if ((senderIp == myIp || senderIp == "127.0.0.1") &&
                        (deviceCode.isBlank() || peer.deviceCode.isBlank() || peer.deviceCode == deviceCode)
                    ) continue

                    val snapshot = synchronized(peerMap) {
                        peerMap[peer.stableId] = peer
                        peerMap.values.sortedBy { it.compactSummary() }
                    }
                    emitPeers(onPeersUpdated, snapshot)
                } catch (_: SocketTimeoutException) {
                    // Normal receive poll timeout
                }
            }
        } catch (_: Exception) {
        } finally {
            broadcasterJob.cancel()
            try { rxSocket?.close() } catch (_: Exception) {}
            try { txSocket?.close() } catch (_: Exception) {}
        }
    }

    private fun isTurkish(): Boolean =
        Locale.getDefault().language.equals("tr", ignoreCase = true)

    suspend fun runSyncServer(
        context: Context,
        expectedPin: String,
        loopContinuously: Boolean = false,
        onParentSnapshotSaved: (() -> Unit)? = null,
        onStatus: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        var serverSocket: ServerSocket? = null
        val tr = isTurkish()
        try {
            serverSocket = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(DEFAULT_PORT))
                soTimeout = if (loopContinuously) 2000 else 60000
            }
            emitStatus(
                onStatus,
                if (tr) "Cihaz açık. Aynı Wi-Fi ağı üzerinden bağlantı bekleniyor..." else "Ready on Wi-Fi. Waiting for nearby devices..."
            )

            while (currentCoroutineContext().isActive) {
                val clientSocket = try {
                    serverSocket.accept()
                } catch (st: SocketTimeoutException) {
                    if (loopContinuously) continue else throw st
                }

                clientSocket.soTimeout = 15000
                try {
                    val reader = BufferedReader(InputStreamReader(clientSocket.inputStream))
                    val writer = PrintWriter(clientSocket.outputStream, true)
                    val header = reader.readLine()?.trim() ?: ""

                    when {
                        header == CMD_PARENT_PUSH -> {
                            writer.println("OK")
                            val payload = readLengthPrefixedPayload(reader)
                            if (payload.isNotBlank()) {
                                val saved = BackupHelper.saveParentStudentSnapshot(context, payload)
                                if (saved != null) {
                                    writer.println("PUSH_OK")
                                    onParentSnapshotSaved?.let { cb ->
                                        withContext(Dispatchers.Main.immediate) { cb() }
                                    }
                                    emitStatus(
                                        onStatus,
                                        if (tr) "Öğrenci verileri alındı (${saved.studentName.ifBlank { saved.compactDeviceInfo() }})!"
                                        else "Student snapshot received (${saved.studentName.ifBlank { saved.compactDeviceInfo() }})!"
                                    )
                                } else {
                                    writer.println("ERROR: Invalid snapshot")
                                }
                            }
                        }
                        header.startsWith(CMD_PARENT_PUSH_PIN_PREFIX) -> {
                            val pin = header.removePrefix(CMD_PARENT_PUSH_PIN_PREFIX).trim()
                            if (pin != expectedPin) {
                                writer.println("ERROR: Key Mismatch")
                                emitStatus(
                                    onStatus,
                                    if (tr) "Hata: 4 haneli anahtar eşleşmedi!" else "Error: 4-digit key mismatch!"
                                )
                            } else {
                                writer.println("OK")
                                val payload = readLengthPrefixedPayload(reader)
                                if (payload.isNotBlank()) {
                                    val saved = BackupHelper.saveParentStudentSnapshot(context, payload)
                                    if (saved != null) {
                                        writer.println("PUSH_OK")
                                        onParentSnapshotSaved?.let { cb ->
                                            withContext(Dispatchers.Main.immediate) { cb() }
                                        }
                                        emitStatus(
                                            onStatus,
                                            if (tr) "Öğrenci verileri alındı (${saved.studentName.ifBlank { saved.compactDeviceInfo() }})!"
                                            else "Student snapshot received (${saved.studentName.ifBlank { saved.compactDeviceInfo() }})!"
                                        )
                                    } else {
                                        writer.println("ERROR: Invalid snapshot")
                                    }
                                }
                            }
                        }
                        header == CMD_AUTO_SYNC || header == expectedPin -> {
                            writer.println("OK")
                            val clientJson = readLengthPrefixedPayload(reader)
                            if (clientJson.isNotBlank()) {
                                BackupHelper.restoreFromBackupJson(context, clientJson)
                            }
                            val db = AppDatabase.getDatabase(context)
                            val hostSessions = db.studySessionDao().getAllSessionsOnce()
                            val hostTimers = db.customTimerDao().getAllTimersOnce()
                            val hostNotes = db.dailyNoteDao().getAllNotesOnce()
                            val hostJson = BackupHelper.createBackupJson(context, hostSessions, hostTimers, hostNotes)

                            writer.println(hostJson.length)
                            writer.print(hostJson)
                            writer.flush()

                            AppActionLogger.logAction(context, "SYNC", "server_sync_complete notes=${hostNotes.size}")
                            emitStatus(
                                onStatus,
                                if (tr) "Senkronizasyon tamamlandı! Veriler aktarıldı."
                                else "Sync complete! Progress transferred."
                            )
                        }
                        else -> {
                            writer.println("ERROR: Key Mismatch")
                            emitStatus(
                                onStatus,
                                if (tr) "Hata: 4 haneli senkronizasyon anahtarı hatalı!" else "Error: Invalid 4-digit sync key!"
                            )
                        }
                    }
                } finally {
                    try { clientSocket.close() } catch (_: Exception) {}
                }

                if (!loopContinuously) break
            }
        } catch (_: SocketTimeoutException) {
            AppActionLogger.logError(context, "SYNC", "server_timeout")
            emitStatus(onStatus, if (tr) "Zaman aşımı: Bağlantı kurulmadı." else "Timeout: No connection established.")
        } catch (e: Exception) {
            AppActionLogger.logError(context, "SYNC", "server_error:${e.javaClass.simpleName}")
            if (currentCoroutineContext().isActive) {
                emitStatus(onStatus, if (tr) "Aktarım Hatası: ${e.message}" else "Transfer Error: ${e.message}")
            }
        } finally {
            try { serverSocket?.close() } catch (_: Exception) {}
        }
    }

    private fun readLengthPrefixedPayload(reader: BufferedReader): String {
        val lenStr = reader.readLine()?.trim() ?: return ""
        val len = lenStr.toIntOrNull()?.coerceIn(0, 8 * 1024 * 1024) ?: return ""
        if (len == 0) return ""
        val charBuf = CharArray(len)
        var readChars = 0
        while (readChars < len) {
            val r = reader.read(charBuf, readChars, len - readChars)
            if (r <= 0) break
            readChars += r
        }
        return String(charBuf, 0, readChars)
    }

    suspend fun connectAndSyncClient(
        context: Context,
        hostIp: String,
        pin: String,
        port: Int = DEFAULT_PORT,
        onStatus: (String) -> Unit
    ) = executeBidirectionalSyncClient(
        context = context,
        hostIp = hostIp,
        port = port,
        header = pin,
        onStatus = onStatus
    )

    suspend fun connectAndSyncAutoPeer(
        context: Context,
        hostIp: String,
        port: Int = DEFAULT_PORT,
        onStatus: (String) -> Unit
    ) = executeBidirectionalSyncClient(
        context = context,
        hostIp = hostIp,
        port = port,
        header = CMD_AUTO_SYNC,
        onStatus = onStatus
    )

    internal suspend fun copyBackupToClipboard(context: Context, backupJson: String) {
        if (backupJson.isBlank()) return
        withContext(Dispatchers.Main.immediate) {
            try {
                val clipboard = context.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("Baslayalim Backup", backupJson))
            } catch (_: Throwable) {
            }
        }
    }

    private suspend fun executeBidirectionalSyncClient(
        context: Context,
        hostIp: String,
        port: Int,
        header: String,
        onStatus: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        val tr = isTurkish()
        try {
            val db = AppDatabase.getDatabase(context)
            val localSessions = db.studySessionDao().getAllSessionsOnce()
            val localTimers = db.customTimerDao().getAllTimersOnce()
            val localNotes = db.dailyNoteDao().getAllNotesOnce()
            val localJson = BackupHelper.createBackupJson(context, localSessions, localTimers, localNotes)
            copyBackupToClipboard(context, localJson)

            emitStatus(onStatus, if (tr) "Cihaza bağlanılıyor ($hostIp)..." else "Connecting to device ($hostIp)...")
            socket = Socket()
            socket.connect(InetSocketAddress(hostIp, port), 8000)
            socket.soTimeout = 15000

            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            val writer = PrintWriter(socket.outputStream, true)

            writer.println(header)

            val ack = reader.readLine()
            if (ack != "OK") {
                emitStatus(onStatus, if (tr) "Hata: Eşleşme kodu reddedildi!" else "Error: Pairing key rejected!")
                socket.close()
                return@withContext
            }

            writer.println(localJson.length)
            writer.print(localJson)
            writer.flush()

            val hostJson = readLengthPrefixedPayload(reader)
            if (hostJson.isNotBlank()) {
                BackupHelper.restoreFromBackupJson(context, hostJson)
            }

            socket.close()
            AppActionLogger.logAction(context, "SYNC", "client_sync_complete notes=${localNotes.size}")
            emitStatus(onStatus, if (tr) "Başarıyla bağlandı ve ilerleme senkronize edildi!" else "Connected and progress synced successfully!")
        } catch (e: Exception) {
            AppActionLogger.logError(context, "SYNC", "client_error:${e.javaClass.simpleName}")
            emitStatus(onStatus, if (tr) "Bağlantı Hatası: ${e.message}" else "Connection Error: ${e.message}")
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    suspend fun pushStudentSnapshotToParent(
        context: Context,
        hostIp: String,
        port: Int = DEFAULT_PORT,
        pinOrNull: String? = null,
        onStatus: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        val tr = isTurkish()
        try {
            emitStatus(onStatus, if (tr) "Veli cihazına bağlanılıyor..." else "Connecting to parent device...")
            socket = Socket()
            socket.connect(InetSocketAddress(hostIp, port), 8000)
            socket.soTimeout = 15000

            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            val writer = PrintWriter(socket.outputStream, true)

            val header = if (pinOrNull.isNullOrBlank()) CMD_PARENT_PUSH else "$CMD_PARENT_PUSH_PIN_PREFIX${pinOrNull.trim()}"
            writer.println(header)

            val ack = reader.readLine()
            if (ack != "OK") {
                emitStatus(onStatus, if (tr) "Hata: Veli cihazı anahtarı reddetti!" else "Error: Parent device rejected key!")
                socket.close()
                return@withContext
            }

            val prefs = UserPreferences(context)
            val snapshotJson = BackupHelper.createStudentSnapshotJson(
                context = context,
                senderBrand = getDeviceBrandOnly(),
                senderModel = getDeviceModelOnly(),
                senderDeviceCode = prefs.deviceUniqueCode,
                senderUsername = prefs.userName.trim()
            )
            writer.println(snapshotJson.length)
            writer.print(snapshotJson)
            writer.flush()

            val confirm = reader.readLine()
            socket.close()
            if (confirm == "PUSH_OK") {
                AppActionLogger.logAction(context, "SYNC", "parent_push_ok")
                emitStatus(onStatus, if (tr) "Öğrenci verileri veli cihazına başarıyla aktarıldı!" else "Student progress sent to parent device!")
            } else {
                emitStatus(onStatus, if (tr) "Aktarım yanıtı alınamadı." else "No confirmation from parent device.")
            }
        } catch (e: Exception) {
            AppActionLogger.logError(context, "SYNC", "parent_push_err:${e.javaClass.simpleName}")
            emitStatus(onStatus, if (tr) "Gönderim Hatası: ${e.message}" else "Send Error: ${e.message}")
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }
}
