package com.example.vnc

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class VncConnectionState {
    val bitmapState = MutableStateFlow<Bitmap?>(null)
    val statusState = MutableStateFlow("Disconnected")
    val isConnectedState = MutableStateFlow(false)
}

class VncClient(
    private val host: String,
    private val port: Int,
    private val password: String = "",
    private val state: VncConnectionState
) {
    private var socket: Socket? = null
    private var dis: DataInputStream? = null
    private var dos: DataOutputStream? = null
    private var running = false
    private var width = 0
    private var height = 0
    private var bitmap: Bitmap? = null
    private var pixels: IntArray? = null

    suspend fun connectAndRun() = withContext(Dispatchers.IO) {
        try {
            state.isConnectedState.value = false
            state.statusState.value = "正在连接到 $host:$port..."
            Log.d("VNC", "Connecting to $host:$port...")
            
            val s = Socket(host, port)
            socket = s
            s.tcpNoDelay = true
            s.soTimeout = 15000 // 15s timeout
            
            dis = DataInputStream(s.getInputStream())
            dos = DataOutputStream(s.getOutputStream())
            running = true

            // 1. Handshake
            val versionBytes = ByteArray(12)
            dis!!.readFully(versionBytes)
            val versionStr = String(versionBytes)
            Log.d("VNC", "Server version: ${versionStr.trim()}")
            
            // Send back same version
            dos!!.write(versionBytes)
            dos!!.flush()

            // 2. Security Handshake
            var chosenType = 1
            if (versionStr.contains("003.003")) {
                // RFB 3.3 sends a single 4-byte security type
                val secType = dis!!.readInt()
                if (secType == 0) {
                    val reasonLen = dis!!.readInt()
                    val reasonBytes = ByteArray(reasonLen)
                    dis!!.readFully(reasonBytes)
                    throw Exception("连接失败: ${String(reasonBytes)}")
                }
                chosenType = secType
                dos!!.writeInt(chosenType)
                dos!!.flush()
            } else {
                // RFB 3.7 or 3.8
                val numSecTypes = dis!!.readUnsignedByte()
                if (numSecTypes == 0) {
                    val reasonLen = dis!!.readInt()
                    val reasonBytes = ByteArray(reasonLen)
                    dis!!.readFully(reasonBytes)
                    throw Exception("连接失败: ${String(reasonBytes)}")
                }
                val secTypes = ByteArray(numSecTypes)
                dis!!.readFully(secTypes)
                
                // Choose security type: None (1) or VNC Auth (2)
                chosenType = if (secTypes.contains(1)) {
                    1
                } else if (secTypes.contains(2)) {
                    2
                } else {
                    secTypes[0].toInt() and 0xFF
                }
                
                dos!!.writeByte(chosenType)
                dos!!.flush()
            }

            if (chosenType == 2) {
                // VNC Authentication
                val challenge = ByteArray(16)
                dis!!.readFully(challenge)
                
                val response = encryptVncPassword(password, challenge)
                dos!!.write(response)
                dos!!.flush()

                val securityResult = dis!!.readInt()
                if (securityResult != 0) {
                    if (securityResult == 1) {
                        try {
                            val reasonLen = dis!!.readInt()
                            val reasonBytes = ByteArray(reasonLen)
                            dis!!.readFully(reasonBytes)
                            throw Exception("认证失败: ${String(reasonBytes)}")
                        } catch (e: Exception) {
                            throw Exception("密码错误，认证失败")
                        }
                    }
                    throw Exception("安全握手失败 (代码: $securityResult)")
                }
            } else if (chosenType == 1) {
                // RFB 3.8 sends SecurityResult for None too
                if (!versionStr.contains("003.003")) {
                    val securityResult = dis!!.readInt()
                    if (securityResult != 0) {
                        throw Exception("安全认证失败")
                    }
                }
            } else {
                throw Exception("不支持的安全认证类型: $chosenType")
            }

            // 3. ClientInit
            dos!!.writeByte(1) // Shared-session flag
            dos!!.flush()

            // 4. ServerInit
            width = dis!!.readUnsignedShort()
            height = dis!!.readUnsignedShort()
            
            // PixelFormat (16 bytes)
            val bpp = dis!!.readUnsignedByte() // bits-per-pixel
            val depth = dis!!.readUnsignedByte()
            val bigEndian = dis!!.readUnsignedByte()
            val trueColor = dis!!.readUnsignedByte()
            val redMax = dis!!.readUnsignedShort()
            val greenMax = dis!!.readUnsignedShort()
            val blueMax = dis!!.readUnsignedShort()
            val redShift = dis!!.readUnsignedByte()
            val greenShift = dis!!.readUnsignedByte()
            val blueShift = dis!!.readUnsignedByte()
            dis!!.skipBytes(3) // padding
            
            val nameLen = dis!!.readInt()
            val nameBytes = ByteArray(nameLen)
            dis!!.readFully(nameBytes)
            val serverName = String(nameBytes)
            
            Log.d("VNC", "Desktop initialized: $serverName (${width}x${height}), $bpp bpp")
            state.statusState.value = "已连接到 $serverName ($width×$height)"
            state.isConnectedState.value = true

            // Set socket timeout to 0 (infinite) for session data loop
            s.soTimeout = 0

            // Create Bitmap for rendering
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap = bmp
            pixels = IntArray(width * height) { Color.BLACK }
            state.bitmapState.value = bmp

            // Request first full frame update
            requestFrameUpdate(0, 0, width, height, incremental = 0)

            // Main message loop
            while (running) {
                val msgType = dis!!.readUnsignedByte()
                when (msgType) {
                    0 -> { // FramebufferUpdate
                        dis!!.readByte() // padding
                        val numRects = dis!!.readUnsignedShort()
                        for (r in 0 until numRects) {
                            val rx = dis!!.readUnsignedShort()
                            val ry = dis!!.readUnsignedShort()
                            val rw = dis!!.readUnsignedShort()
                            val rh = dis!!.readUnsignedShort()
                            val encoding = dis!!.readInt()

                            if (encoding == 0) { // Raw encoding
                                val bytesPerPixel = bpp / 8
                                val buf = ByteArray(rw * rh * bytesPerPixel)
                                dis!!.readFully(buf)
                                drawRawRect(rx, ry, rw, rh, buf, bpp, redShift, greenShift, blueShift, redMax, greenMax, blueMax)
                            } else {
                                // For unsupported encoding types, we skip bytes safely or log them
                                Log.w("VNC", "收到不支持的编码类型: $encoding")
                                // If it's desktop size pseudo encoding, we handle size change
                                if (encoding == -223) { // DesktopSize pseudo-encoding
                                    width = rw
                                    height = rh
                                    Log.d("VNC", "Desktop size changed dynamically to: ${width}x${height}")
                                    val newBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                    bitmap = newBmp
                                    pixels = IntArray(width * height) { Color.BLACK }
                                    state.bitmapState.value = newBmp
                                }
                            }
                        }
                        // Draw pixels onto bitmap
                        val currentBmp = bitmap
                        val currentPixels = pixels
                        if (currentBmp != null && currentPixels != null) {
                            currentBmp.setPixels(currentPixels, 0, width, 0, 0, width, height)
                            state.bitmapState.value = currentBmp
                        }
                        
                        // Request next incremental update
                        requestFrameUpdate(0, 0, width, height, incremental = 1)
                    }
                    1 -> { // SetColourMapEntries
                        dis!!.readByte() // padding
                        val firstColour = dis!!.readUnsignedShort()
                        val numColours = dis!!.readUnsignedShort()
                        dis!!.skipBytes(numColours * 6) // Skip color entries
                    }
                    2 -> { // Bell
                        // Standard sound ring if supported
                        Log.d("VNC", "收到服务器铃声指令")
                    }
                    3 -> { // ServerCutText
                        dis!!.skipBytes(3) // padding
                        val textLen = dis!!.readInt()
                        val textBytes = ByteArray(textLen)
                        dis!!.readFully(textBytes)
                        Log.d("VNC", "剪贴板更新: ${String(textBytes)}")
                    }
                    else -> {
                        Log.d("VNC", "收到未处理的消息类型: $msgType")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("VNC", "Connection error", e)
            state.statusState.value = "连接断开: ${e.localizedMessage ?: "Unknown"}"
            state.isConnectedState.value = false
        } finally {
            close()
        }
    }

    private fun drawRawRect(
        rx: Int, ry: Int, rw: Int, rh: Int,
        buf: ByteArray, bpp: Int,
        redShift: Int, greenShift: Int, blueShift: Int,
        rMax: Int, gMax: Int, bMax: Int
    ) {
        val pix = pixels ?: return
        val bytesPerPixel = bpp / 8
        var idx = 0
        for (y in ry until (ry + rh)) {
            if (y >= height) break
            for (x in rx until (rx + rw)) {
                if (x >= width) {
                    idx += bytesPerPixel
                    continue
                }
                var r = 0
                var g = 0
                var b = 0
                
                if (bytesPerPixel == 4) {
                    val pixelVal = ((buf[idx + 3].toInt() and 0xFF) shl 24) or
                                   ((buf[idx + 2].toInt() and 0xFF) shl 16) or
                                   ((buf[idx + 1].toInt() and 0xFF) shl 8) or
                                   (buf[idx].toInt() and 0xFF)
                    r = (pixelVal shr redShift) and rMax
                    g = (pixelVal shr greenShift) and gMax
                    b = (pixelVal shr blueShift) and bMax
                    
                    // Scale colors to 255
                    r = if (rMax > 0) (r * 255) / rMax else r
                    g = if (gMax > 0) (g * 255) / gMax else g
                    b = if (bMax > 0) (b * 255) / bMax else b
                } else if (bytesPerPixel == 2) {
                    val pixelVal = ((buf[idx + 1].toInt() and 0xFF) shl 8) or (buf[idx].toInt() and 0xFF)
                    r = (pixelVal shr redShift) and rMax
                    g = (pixelVal shr greenShift) and gMax
                    b = (pixelVal shr blueShift) and bMax
                    
                    r = if (rMax > 0) (r * 255) / rMax else r
                    g = if (gMax > 0) (g * 255) / gMax else g
                    b = if (bMax > 0) (b * 255) / bMax else b
                } else if (bytesPerPixel == 3) {
                    // 24 bit colors usually raw R, G, B
                    r = buf[idx].toInt() and 0xFF
                    g = buf[idx + 1].toInt() and 0xFF
                    b = buf[idx + 2].toInt() and 0xFF
                }
                
                pix[y * width + x] = Color.rgb(r, g, b)
                idx += bytesPerPixel
            }
        }
    }

    fun requestFrameUpdate(x: Int, y: Int, w: Int, h: Int, incremental: Int) {
        try {
            dos?.apply {
                writeByte(3) // FramebufferUpdateRequest
                writeByte(incremental)
                writeShort(x)
                writeShort(y)
                writeShort(w)
                writeShort(h)
                flush()
            }
        } catch (e: Exception) {
            Log.e("VNC", "Failed to send update request", e)
        }
    }

    fun sendPointerEvent(buttonMask: Int, x: Int, y: Int) {
        try {
            dos?.apply {
                writeByte(5) // PointerEvent
                writeByte(buttonMask)
                writeShort(x)
                writeShort(y)
                flush()
            }
        } catch (e: Exception) {
            Log.e("VNC", "Failed to send pointer event", e)
        }
    }

    fun sendKeyEvent(downFlag: Int, key: Int) {
        try {
            dos?.apply {
                writeByte(4) // KeyEvent
                writeByte(downFlag)
                writeShort(0) // padding
                writeInt(key)
                flush()
            }
        } catch (e: Exception) {
            Log.e("VNC", "Failed to send key event", e)
        }
    }

    fun close() {
        running = false
        try { socket?.close() } catch (e: Exception) {}
        socket = null
        dis = null
        dos = null
        state.isConnectedState.value = false
    }

    private fun encryptVncPassword(password: String, challenge: ByteArray): ByteArray {
        val key = ByteArray(8)
        val pBytes = password.toByteArray()
        for (i in 0 until 8) {
            if (i < pBytes.size) {
                val b = pBytes[i].toInt()
                // Reverse bits of each byte as required by RFB VNC standard DES
                var rev = 0
                for (j in 0 until 8) {
                    if ((b and (1 shl j)) != 0) {
                        rev = rev or (1 shl (7 - j))
                    }
                }
                key[i] = rev.toByte()
            } else {
                key[i] = 0
            }
        }
        val keySpec = SecretKeySpec(key, "DES")
        val cipher = Cipher.getInstance("DES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        return cipher.doFinal(challenge)
    }
}
