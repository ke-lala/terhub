package com.example.vnc

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun VncViewContainer(
    onClose: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val state = remember { VncConnectionState() }
    
    var host by remember { mutableStateOf("127.0.0.1") }
    var portText by remember { mutableStateOf("5901") }
    var password by remember { mutableStateOf("") }
    
    val isConnected by state.isConnectedState.collectAsStateWithLifecycle()
    val statusMsg by state.statusState.collectAsStateWithLifecycle()
    val bitmap by state.bitmapState.collectAsStateWithLifecycle()
    
    var clientInstance by remember { mutableStateOf<VncClient?>(null) }
    
    // Mouse Control Settings
    var mouseMode by remember { mutableStateOf(0) } // 0 = Direct Touch, 1 = Virtual Touchpad
    var virtualCursorOffset by remember { mutableStateOf(Offset(200f, 200f)) }
    
    DisposableEffect(Unit) {
        onDispose {
            clientInstance?.close()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF14121E))
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1A30))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Real VNC Viewer",
                    tint = Color(0xFF818CF8),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "VNC 远程桌面连接",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = statusMsg,
                        color = if (isConnected) Color(0xFF34D399) else Color.LightGray.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isConnected) {
                    IconButton(
                        onClick = {
                            clientInstance?.close()
                            clientInstance = null
                        },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFFF87171))
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = "Disconnect")
                    }
                }
                
                IconButton(
                    onClick = onClose,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close View")
                }
            }
        }

        if (!isConnected) {
            // CONNECTION CONFIG PANEL & GUIDE
            VncConnectionConfigPanel(
                host = host,
                onHostChange = { host = it },
                portText = portText,
                onPortChange = { portText = it },
                password = password,
                onPasswordChange = { password = it },
                statusMsg = statusMsg,
                onConnect = {
                    coroutineScope.launch {
                        val port = portText.toIntOrNull() ?: 5901
                        clientInstance?.close()
                        val client = VncClient(host, port, password, state)
                        clientInstance = client
                        client.connectAndRun()
                    }
                }
            )
        } else {
            // REAL ACTIVE DESKTOP VIEW
            val currentBitmap = bitmap
            var canvasSize by remember { mutableStateOf(Size(0f, 0f)) }
            
            val vncWidth = currentBitmap?.width ?: 1
            val vncHeight = currentBitmap?.height ?: 1
            
            var fitWidth = 1f
            var fitHeight = 1f
            var offsetX = 0f
            var offsetY = 0f
            
            if (canvasSize.width > 0 && canvasSize.height > 0) {
                val canvasAspect = canvasSize.width / canvasSize.height
                val vncAspect = vncWidth.toFloat() / vncHeight.toFloat()
                
                if (canvasAspect > vncAspect) {
                    fitHeight = canvasSize.height
                    fitWidth = fitHeight * vncAspect
                    offsetX = (canvasSize.width - fitWidth) / 2f
                    offsetY = 0f
                } else {
                    fitWidth = canvasSize.width
                    fitHeight = fitWidth / vncAspect
                    offsetX = 0f
                    offsetY = (canvasSize.height - fitHeight) / 2f
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
            ) {
                if (currentBitmap != null) {
                    // Touch controller
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { size ->
                                canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                            }
                            .pointerInput(mouseMode, canvasSize) {
                                if (mouseMode == 0) {
                                    // 0 = Direct Touch
                                    detectTapGestures(
                                        onTap = { tapOffset ->
                                            val vx = ((tapOffset.x - offsetX) / fitWidth) * vncWidth
                                            val vy = ((tapOffset.y - offsetY) / fitHeight) * vncHeight
                                            if (vx in 0f..vncWidth.toFloat() && vy in 0f..vncHeight.toFloat()) {
                                                // Send Left click press + release
                                                clientInstance?.sendPointerEvent(1, vx.toInt(), vy.toInt())
                                                coroutineScope.launch {
                                                    kotlinx.coroutines.delay(50)
                                                    clientInstance?.sendPointerEvent(0, vx.toInt(), vy.toInt())
                                                }
                                            }
                                        },
                                        onDoubleTap = { tapOffset ->
                                            val vx = ((tapOffset.x - offsetX) / fitWidth) * vncWidth
                                            val vy = ((tapOffset.y - offsetY) / fitHeight) * vncHeight
                                            if (vx in 0f..vncWidth.toFloat() && vy in 0f..vncHeight.toFloat()) {
                                                coroutineScope.launch {
                                                    clientInstance?.sendPointerEvent(1, vx.toInt(), vy.toInt())
                                                    kotlinx.coroutines.delay(30)
                                                    clientInstance?.sendPointerEvent(0, vx.toInt(), vy.toInt())
                                                    kotlinx.coroutines.delay(30)
                                                    clientInstance?.sendPointerEvent(1, vx.toInt(), vy.toInt())
                                                    kotlinx.coroutines.delay(30)
                                                    clientInstance?.sendPointerEvent(0, vx.toInt(), vy.toInt())
                                                }
                                            }
                                        },
                                        onLongPress = { tapOffset ->
                                            val vx = ((tapOffset.x - offsetX) / fitWidth) * vncWidth
                                            val vy = ((tapOffset.y - offsetY) / fitHeight) * vncHeight
                                            if (vx in 0f..vncWidth.toFloat() && vy in 0f..vncHeight.toFloat()) {
                                                // Send Right click press + release
                                                clientInstance?.sendPointerEvent(4, vx.toInt(), vy.toInt())
                                                coroutineScope.launch {
                                                    kotlinx.coroutines.delay(50)
                                                    clientInstance?.sendPointerEvent(0, vx.toInt(), vy.toInt())
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                            .pointerInput(mouseMode, canvasSize) {
                                if (mouseMode == 0) {
                                    detectDragGestures(
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val vx = ((change.position.x - offsetX) / fitWidth) * vncWidth
                                            val vy = ((change.position.y - offsetY) / fitHeight) * vncHeight
                                            if (vx in 0f..vncWidth.toFloat() && vy in 0f..vncHeight.toFloat()) {
                                                // Dragging sends left-mouse-down pointer events to draw/drag elements
                                                clientInstance?.sendPointerEvent(1, vx.toInt(), vy.toInt())
                                            }
                                        },
                                        onDragEnd = {
                                            clientInstance?.sendPointerEvent(0, 0, 0)
                                        }
                                    )
                                } else {
                                    // 1 = Virtual Touchpad mode (precision relative cursor movement)
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val nextX = (virtualCursorOffset.x + dragAmount.x).coerceIn(0f, canvasSize.width)
                                        val nextY = (virtualCursorOffset.y + dragAmount.y).coerceIn(0f, canvasSize.height)
                                        virtualCursorOffset = Offset(nextX, nextY)
                                        
                                        // Translate cursor position to VNC bounds
                                        val vx = ((nextX - offsetX) / fitWidth) * vncWidth
                                        val vy = ((nextY - offsetY) / fitHeight) * vncHeight
                                        clientInstance?.sendPointerEvent(0, vx.toInt().coerceIn(0, vncWidth), vy.toInt().coerceIn(0, vncHeight))
                                    }
                                }
                            }
                    ) {
                        // Render Framebuffer Bitmap
                        Image(
                            bitmap = currentBitmap.asImageBitmap(),
                            contentDescription = "Active VNC Display",
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Draw precise visual touchpad cursor pointer
                        if (mouseMode == 1) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = Color.White,
                                    radius = 8f,
                                    center = virtualCursorOffset
                                )
                                drawCircle(
                                    color = Color.Black,
                                    radius = 8f,
                                    center = virtualCursorOffset,
                                    style = Stroke(width = 2f)
                                )
                                drawLine(
                                    color = Color.Red,
                                    start = Offset(virtualCursorOffset.x - 12f, virtualCursorOffset.y),
                                    end = Offset(virtualCursorOffset.x + 12f, virtualCursorOffset.y),
                                    strokeWidth = 2f
                                )
                                drawLine(
                                    color = Color.Red,
                                    start = Offset(virtualCursorOffset.x, virtualCursorOffset.y - 12f),
                                    end = Offset(virtualCursorOffset.x, virtualCursorOffset.y + 12f),
                                    strokeWidth = 2f
                                )
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF818CF8))
                    }
                }
            }

            // CONTROLS OVERLAY BAR (Footer)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1A30))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Mouse Buttons Control
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = {
                            val vx = ((virtualCursorOffset.x - offsetX) / fitWidth) * vncWidth
                            val vy = ((virtualCursorOffset.y - offsetY) / fitHeight) * vncHeight
                            clientInstance?.sendPointerEvent(1, vx.toInt(), vy.toInt())
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(50)
                                clientInstance?.sendPointerEvent(0, vx.toInt(), vy.toInt())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("左键", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = {
                            val vx = ((virtualCursorOffset.x - offsetX) / fitWidth) * vncWidth
                            val vy = ((virtualCursorOffset.y - offsetY) / fitHeight) * vncHeight
                            clientInstance?.sendPointerEvent(4, vx.toInt(), vy.toInt())
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(50)
                                clientInstance?.sendPointerEvent(0, vx.toInt(), vy.toInt())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B7280)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("右键", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Middle Navigation Control Mode Switcher
                Row(
                    modifier = Modifier
                        .background(Color(0xFF111827), RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val activeColor = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White)
                    val idleColor = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Gray)
                    
                    Button(
                        onClick = { mouseMode = 0 },
                        colors = if (mouseMode == 0) activeColor else idleColor,
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("直触模式", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { mouseMode = 1 },
                        colors = if (mouseMode == 1) activeColor else idleColor,
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("触控板", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Keyboard / Input actions
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            // Send Escape key (key code 0xff1b)
                            clientInstance?.sendKeyEvent(1, 0xff1b)
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(30)
                                clientInstance?.sendKeyEvent(0, 0xff1b)
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White, containerColor = Color(0xFF2E2A47))
                    ) {
                        Text("Esc", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    IconButton(
                        onClick = {
                            // Send Return key (key code 0xff0d)
                            clientInstance?.sendKeyEvent(1, 0xff0d)
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(30)
                                clientInstance?.sendKeyEvent(0, 0xff0d)
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White, containerColor = Color(0xFF2E2A47))
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Enter Key")
                    }
                }
            }
        }
    }
}

@Composable
fun VncConnectionConfigPanel(
    host: String,
    onHostChange: (String) -> Unit,
    portText: String,
    onPortChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    statusMsg: String,
    onConnect: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // QUICK CONNECT CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B33)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "配置 VNC 连接参数",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = host,
                    onValueChange = onHostChange,
                    label = { Text("主机地址 (Host)") },
                    textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF818CF8),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                        focusedLabelColor = Color(0xFF818CF8),
                        unfocusedLabelColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = portText,
                        onValueChange = onPortChange,
                        label = { Text("端口 (Port)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF818CF8),
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                            focusedLabelColor = Color(0xFF818CF8),
                            unfocusedLabelColor = Color.Gray
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text("VNC 密码") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF818CF8),
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                            focusedLabelColor = Color(0xFF818CF8),
                            unfocusedLabelColor = Color.Gray
                        ),
                        modifier = Modifier.weight(1.5f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Quick Fill buttons
                    Button(
                        onClick = {
                            onHostChange("127.0.0.1")
                            onPortChange("5901")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2A47), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("本地 5901", fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            onHostChange("10.0.2.2") // Android emulator loopback host
                            onPortChange("5901")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2A47), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("宿主机 5901", fontSize = 11.sp)
                    }
                }

                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Power, contentDescription = "Connect")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("立即连接 VNC 远程服务", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                if (statusMsg.isNotEmpty() && statusMsg != "Disconnected") {
                    Text(
                        text = "状态: $statusMsg",
                        color = Color(0xFFFBBF24),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // PHYSICAL LINUX SETUP GUIDE CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141224)),
            border = BorderStroke(1.dp, Color(0x33818CF8)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MenuBook, contentDescription = "Guide", tint = Color(0xFF818CF8), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "一键配置真实 XFCE4 & VNC 服务指南",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "如果您正在宿主机、Termux 或其他外部服务器上运行，请在终端复制并运行以下命令，即可一键下载并配置好一个原生的、真正的 XFCE4 桌面环境及 TigerVNC 服务器：",
                    color = Color.LightGray.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                val commandScript = """
                    # 1. 更新环境并安装 XFCE4 与 TigerVNC
                    pkg update -y && pkg install xfce4 xfce4-goodies tigervnc-standalone-server -y
                    
                    # 2. 创建 VNC 初始化启动脚本并配置
                    mkdir -p ~/.vnc
                    echo -e "#!/bin/sh\nexport DISPLAY=:1\nstartxfce4 &" > ~/.vnc/xstartup
                    chmod +x ~/.vnc/xstartup
                    
                    # 3. 启动并开启非 localhost 监听模式
                    vncserver -localhost no :1
                """.trimIndent()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = commandScript,
                            color = Color(0xFF34D399),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(commandScript))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF312E81)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .align(Alignment.End)
                                .height(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("复制代码", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text(
                    text = "运行成功后，直接回到本页面点击“立即连接 VNC”，即可一键飞入流畅、原生的物理 XFCE 经典轻量桌面世界！",
                    color = Color(0xFFFBBF24),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun DesktopIcon(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}

@Composable
fun ThunarPlaceItem(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = name, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(name, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DesktopWindowSim(
    title: String,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .background(Color(0xFF1F1F1F), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Window Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2D2D2D))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFFFBD2E), RoundedCornerShape(5.dp)))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFFFF5F56), RoundedCornerShape(5.dp))
                            .clickable(onClick = onClose)
                    )
                }
            }
            
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
        }
    }
}
