package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.filemanager.FileItem
import com.example.proot.DistroItem
import com.example.settings.SettingsManager
import com.example.store.PackageItem
import com.example.terminal.AnsiParser
import com.example.vnc.VncViewContainer
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.shadow
import java.io.File

import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val mainViewModel: MainViewModel = viewModel()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppScreen(viewModel = mainViewModel)
                }
            }
        }
    }
}

@Composable
fun AppHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color(0xFF1D1B20))) {
                        append("Termux")
                    }
                    withStyle(style = SpanStyle(color = Color(0xFF6750A4))) {
                        append("Hub")
                    }
                },
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-1).sp
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFFEADDFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = "User Profile",
                tint = Color(0xFF21005D),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val theme by viewModel.terminalTheme.collectAsStateWithLifecycle()
    
    val wallpaperPath by viewModel.wallpaperPath.collectAsStateWithLifecycle()
    val wallpaperOpacity by viewModel.wallpaperOpacity.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (wallpaperPath != null) {
            AsyncImage(
                model = wallpaperPath,
                contentDescription = "Background Wallpaper",
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(wallpaperOpacity),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                    tonalElevation = 8.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    val tabs = listOf(
                        Triple(MainViewModel.AppTab.TERMINAL, "终端", Icons.Default.Terminal),
                        Triple(MainViewModel.AppTab.STORE, "商店", Icons.Default.ShoppingBag),
                        Triple(MainViewModel.AppTab.FILES, "文件", Icons.Default.Folder),
                        Triple(MainViewModel.AppTab.DISTROS, "容器", Icons.Default.Layers),
                        Triple(MainViewModel.AppTab.SETTINGS, "设置", Icons.Default.Settings)
                    )

                    tabs.forEach { (tab, label, icon) ->
                        val selected = activeTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { viewModel.setActiveTab(tab) },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                AppHeader()
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (activeTab) {
                        MainViewModel.AppTab.TERMINAL -> TerminalTab(viewModel)
                        MainViewModel.AppTab.STORE -> StoreTab(viewModel)
                        MainViewModel.AppTab.FILES -> FilesTab(viewModel)
                        MainViewModel.AppTab.DISTROS -> DistrosTab(viewModel)
                        MainViewModel.AppTab.SETTINGS -> SettingsTab(viewModel)
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. TERMINAL TAB
// ==========================================
@Composable
fun TerminalTab(viewModel: MainViewModel) {
    val localContext = LocalContext.current
    val theme by viewModel.terminalTheme.collectAsStateWithLifecycle()
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val fontFamily by viewModel.fontFamily.collectAsStateWithLifecycle()
    val shortcuts by viewModel.shortcuts.collectAsStateWithLifecycle()

    val output by viewModel.terminalSession.output.collectAsStateWithLifecycle()
    val prompt by viewModel.terminalSession.currentPrompt.collectAsStateWithLifecycle()
    val activeDistro = viewModel.terminalSession.activeDistro

    var commandInput by remember { mutableStateOf("") }
    val commandHistory = remember { mutableStateListOf<String>() }
    var historyIndex by remember { mutableStateOf(-1) }

    val scrollState = rememberScrollState()

    LaunchedEffect(output) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        if (activeDistro != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xE0E95420)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = "Distro Active",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "正在运行 Linux $activeDistro 容器环境",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { viewModel.exitDistro() },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("退出容器", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.72f))
                .border(1.dp, theme.foreground.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                SelectionContainer {
                    Text(
                        text = AnsiParser.parse(output, theme),
                        fontFamily = when (fontFamily) {
                            "Monospace" -> FontFamily.Monospace
                            "Serif" -> FontFamily.Serif
                            "SansSerif" -> FontFamily.SansSerif
                            else -> FontFamily.Monospace
                        },
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.35f).sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(shortcuts) { cmd ->
                AssistChip(
                    onClick = { viewModel.executeShortcut(cmd) },
                    label = { Text(cmd, fontSize = 11.sp, color = theme.foreground) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = theme.foreground.copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = theme.foreground.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val keys = listOf("Ctrl", "Tab", "Esc", "Fn")
            keys.forEach { key ->
                Button(
                    onClick = {
                        if (key == "Tab") {
                            viewModel.executeShortcut("\t")
                        } else {
                            Toast.makeText(localContext, "模拟按下: $key", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.foreground.copy(alpha = 0.15f),
                        contentColor = theme.foreground
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(key, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = {
                    if (commandHistory.isNotEmpty()) {
                        if (historyIndex == -1) {
                            historyIndex = commandHistory.size - 1
                        } else if (historyIndex > 0) {
                            historyIndex--
                        }
                        commandInput = commandHistory[historyIndex]
                    }
                },
                modifier = Modifier
                    .size(32.dp)
                    .background(theme.foreground.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "History Up", tint = theme.foreground, modifier = Modifier.size(16.dp))
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = {
                    if (commandHistory.isNotEmpty() && historyIndex != -1) {
                        if (historyIndex < commandHistory.size - 1) {
                            historyIndex++
                            commandInput = commandHistory[historyIndex]
                        } else {
                            historyIndex = -1
                            commandInput = ""
                        }
                    }
                },
                modifier = Modifier
                    .size(32.dp)
                    .background(theme.foreground.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "History Down", tint = theme.foreground, modifier = Modifier.size(16.dp))
            }

            Spacer(modifier = Modifier.width(4.dp))

            Button(
                onClick = { viewModel.terminalSession.sendCtrlC() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = 0.25f),
                    contentColor = Color.Red
                ),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("Ctrl+C", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                .border(1.dp, theme.foreground.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = prompt,
                color = theme.foreground,
                fontSize = fontSize.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            TextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = theme.foreground,
                    unfocusedTextColor = theme.foreground,
                    cursorColor = theme.cursorColor
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize.sp
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = {
                        val cmd = commandInput
                        if (cmd.isNotBlank()) {
                            viewModel.executeShortcut(cmd)
                            if (commandHistory.isEmpty() || commandHistory.last() != cmd) {
                                commandHistory.add(cmd)
                            }
                            commandInput = ""
                            historyIndex = -1
                        }
                    }
                ),
                placeholder = {
                    Text(
                        "输入终端指令...",
                        fontSize = (fontSize - 2).sp,
                        fontFamily = FontFamily.Monospace,
                        color = theme.foreground.copy(alpha = 0.45f)
                    )
                }
            )
            if (commandInput.isNotEmpty()) {
                IconButton(onClick = { commandInput = "" }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear Input", tint = theme.foreground.copy(alpha = 0.6f))
                }
            }
        }
    }
}

// ==========================================
// 2. PACKAGE STORE TAB
// ==========================================
@Composable
fun StoreTab(viewModel: MainViewModel) {
    val isAptSelected by viewModel.packageStoreManager.isAptSelected.collectAsStateWithLifecycle()
    val packages by viewModel.packageStoreManager.packages.collectAsStateWithLifecycle()
    val installingId by viewModel.packageStoreManager.installingPackageId.collectAsStateWithLifecycle()
    val progress by viewModel.packageStoreManager.installProgress.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("全部") }

    val categories = listOf("全部", "开发环境", "系统工具", "休闲娱乐", "安全评估", "多媒体")
    val coroutineScope = rememberCoroutineScope()
    var showManageSourcesDialog by remember { mutableStateOf(false) }

    if (showManageSourcesDialog) {
        ManageSourcesDialog(
            viewModel = viewModel,
            onDismiss = { showManageSourcesDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.ShoppingBag,
                    contentDescription = "Store",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "软件包商店",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "浏览、一键安装和部署 Linux CLI 软件包",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        fontSize = 11.sp
                    )
                }
            }

            OutlinedButton(
                onClick = { showManageSourcesDialog = true },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Manage Sources", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("软件源管理", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            Button(
                onClick = { viewModel.packageStoreManager.setPackageManager(true) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAptSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (isAptSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("APT 包管理器", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Button(
                onClick = { viewModel.packageStoreManager.setPackageManager(false) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isAptSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (!isAptSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("PKG 包管理器", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("搜索软件包 (e.g. python, htop)...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val filteredPackages = packages.filter { pkg ->
            (selectedCategory == "全部" || pkg.category == selectedCategory) &&
                    (pkg.name.contains(searchQuery, ignoreCase = true) || pkg.id.contains(searchQuery, ignoreCase = true))
        }

        if (filteredPackages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = "Empty", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("未找到匹配的软件包", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredPackages) { pkg ->
                    PackageCard(
                        pkg = pkg,
                        installingId = installingId,
                        progress = progress,
                        onInstall = {
                            coroutineScope.launch {
                                viewModel.packageStoreManager.installPackage(pkg.id)
                            }
                        },
                        onUninstall = {
                            viewModel.packageStoreManager.uninstallPackage(pkg.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PackageCard(
    pkg: PackageItem,
    installingId: String?,
    progress: Float,
    onInstall: () -> Unit,
    onUninstall: () -> Unit
) {
    val isInstallingThis = installingId == pkg.id

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val iconVector = when (pkg.icon) {
                    "code" -> Icons.Default.Code
                    "utils" -> Icons.Default.Handyman
                    "game" -> Icons.Default.VideogameAsset
                    "security" -> Icons.Default.Lock
                    "media" -> Icons.Default.Language
                    else -> Icons.Default.Terminal
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(iconVector, contentDescription = pkg.name, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(pkg.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = (-0.2).sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = pkg.sourceName,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Version: ${pkg.version}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("•", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(pkg.size, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (pkg.isInstalled) {
                    TextButton(
                        onClick = onUninstall,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("卸载", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                } else if (isInstallingThis) {
                    CircularProgressIndicator(
                        progress = progress,
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Button(
                        onClick = onInstall,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("安装", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = pkg.description,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (isInstallingThis) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = progress,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${(progress * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// 3. FILE MANAGER TAB
// ==========================================
@Composable
fun FilesTab(viewModel: MainViewModel) {
    val localContext = LocalContext.current
    val currentDir by viewModel.fileManager.currentDirectory.collectAsStateWithLifecycle()
    val files by viewModel.fileManager.filesList.collectAsStateWithLifecycle()
    val editingFile by viewModel.fileManager.editingFile.collectAsStateWithLifecycle()
    val editorContent by viewModel.fileManager.editorContent.collectAsStateWithLifecycle()

    var showAddFolderDialog by remember { mutableStateOf(false) }
    var showAddFileDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }
    var fileNameInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { viewModel.fileManager.navigateUp() },
                enabled = currentDir.absolutePath != viewModel.terminalSession.termuxRootDir.absolutePath,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Up", tint = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "文件管理器",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = currentDir.absolutePath.substringAfter("termux/"),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = { showAddFolderDialog = true }) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "Create Folder", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { showAddFileDialog = true }) {
                Icon(Icons.Default.NoteAdd, contentDescription = "Create File", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (files.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Empty", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("当前目录为空", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(files) { item ->
                    FileRowItem(
                        item = item,
                        onRowClick = {
                            if (item.isDirectory) {
                                viewModel.fileManager.navigateTo(File(item.path))
                            } else {
                                viewModel.fileManager.openFileForEditing(item)
                            }
                        },
                        onDelete = {
                            viewModel.fileManager.deleteItem(item)
                        }
                    )
                }
            }
        }
    }

    if (editingFile != null) {
        Dialog(onDismissRequest = { viewModel.fileManager.closeEditor() }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(14.dp)
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = "Editor", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = editingFile?.name ?: "编辑文件",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.fileManager.closeEditor() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(
                            onClick = {
                                if (viewModel.fileManager.saveEditingFile()) {
                                    Toast.makeText(localContext, "文件已保存", Toast.LENGTH_SHORT).show()
                                    viewModel.fileManager.closeEditor()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    TextField(
                        value = editorContent,
                        onValueChange = { viewModel.fileManager.updateEditorContent(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        ),
                        placeholder = { Text("在此输入文件内容...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)) }
                    )
                }
            }
        }
    }

    if (showAddFolderDialog) {
        AlertDialog(
            onDismissRequest = { showAddFolderDialog = false },
            title = { Text("新建文件夹", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, letterSpacing = (-0.5).sp) },
            text = {
                OutlinedTextField(
                    value = folderNameInput,
                    onValueChange = { folderNameInput = it },
                    label = { Text("文件夹名称", fontWeight = FontWeight.Bold) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (viewModel.fileManager.createFolder(folderNameInput)) {
                            folderNameInput = ""
                            showAddFolderDialog = false
                        } else {
                            Toast.makeText(localContext, "创建失败，文件夹已存在或名称不合法", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("创建", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFolderDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)) {
                    Text("取消", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showAddFileDialog) {
        AlertDialog(
            onDismissRequest = { showAddFileDialog = false },
            title = { Text("新建文件", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, letterSpacing = (-0.5).sp) },
            text = {
                OutlinedTextField(
                    value = fileNameInput,
                    onValueChange = { fileNameInput = it },
                    label = { Text("文件名称 (e.g. script.sh, run.py)", fontWeight = FontWeight.Bold) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (viewModel.fileManager.createFile(fileNameInput)) {
                            fileNameInput = ""
                            showAddFileDialog = false
                        } else {
                            Toast.makeText(localContext, "创建失败，文件已存在或名称不合法", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("创建", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFileDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)) {
                    Text("取消", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun FileRowItem(
    item: FileItem,
    onRowClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRowClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            val iconVector = if (item.isDirectory) {
                Icons.Default.Folder
            } else {
                when (item.extension) {
                    "py" -> Icons.Default.Code
                    "sh", "bash" -> Icons.Default.Terminal
                    "txt", "md" -> Icons.Default.Description
                    else -> Icons.Default.InsertDriveFile
                }
            }

            val iconColor = if (item.isDirectory) {
                Color(0xFF1E88E5)
            } else {
                MaterialTheme.colorScheme.primary
            }

            Icon(
                iconVector,
                contentDescription = item.name,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row {
                    Text(item.size, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(item.lastModified, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ==========================================
// 4. DISTROS TAB
// ==========================================
@Composable
fun DistrosTab(viewModel: MainViewModel) {
    val distros by viewModel.prootDistroManager.distros.collectAsStateWithLifecycle()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Layers,
                    contentDescription = "Distros",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Linux 容器 (proot-distro)",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "一键部署和登录独立、安全沙盒的各种 Linux 系统",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        item {
            DesktopAndVncSection(viewModel = viewModel)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "可部署的 Linux 操作系统镜像",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        items(distros) { distro ->
            DistroCard(
                distro = distro,
                onInstallClick = { viewModel.installDistro(distro.id) },
                onBootClick = { viewModel.launchDistro(distro.id) }
            )
        }
    }
}

@Composable
fun DistroCard(
    distro: DistroItem,
    onInstallClick: () -> Unit,
    onBootClick: () -> Unit
) {
    val cardColor = Color(android.graphics.Color.parseColor(distro.colorHex))

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(cardColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = distro.logoText,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(distro.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = (-0.2).sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("RootFS 大小: ${distro.size}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("•", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (distro.isInstalled) "已部署" else "未部署",
                            color = if (distro.isInstalled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (distro.isInstalling) {
                    CircularProgressIndicator(
                        progress = distro.installProgress,
                        color = cardColor,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (distro.isInstalled) {
                    Button(
                        onClick = onBootClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cardColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("登录/启动", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onInstallClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("一键部署", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                distro.description,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                fontSize = 12.sp
            )

            if (distro.isInstalling) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = distro.installProgress,
                        color = cardColor,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${(distro.installProgress * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// 5. SETTINGS TAB
// ==========================================
@Composable
fun SettingsTab(viewModel: MainViewModel) {
    val localContext = LocalContext.current
    val theme by viewModel.terminalTheme.collectAsStateWithLifecycle()
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val fontFamily by viewModel.fontFamily.collectAsStateWithLifecycle()
    val shortcuts by viewModel.shortcuts.collectAsStateWithLifecycle()

    val wallpaperPath by viewModel.wallpaperPath.collectAsStateWithLifecycle()
    val wallpaperOpacity by viewModel.wallpaperOpacity.collectAsStateWithLifecycle()

    var customShortcutInput by remember { mutableStateOf("") }
    var wallpaperInput by remember { mutableStateOf(wallpaperPath ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "个性化设置",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "配置终端配色方案、字体、背景图与快捷面板",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "1. 选择终端配色方案",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            letterSpacing = (-0.2).sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            items(SettingsManager.TerminalTheme.values()) { scheme ->
                val selected = theme == scheme
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = scheme.background,
                        contentColor = scheme.foreground
                    ),
                    border = BorderStroke(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) scheme.foreground else scheme.foreground.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .clickable { viewModel.setTerminalTheme(scheme) }
                        .fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Column {
                            Text(
                                scheme.displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                scheme.ansiColors.take(4).forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(color)
                                    )
                                }
                            }
                        }
                        if (selected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Active",
                                tint = scheme.foreground,
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "2. 自定义终端背景图",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            letterSpacing = (-0.2).sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = wallpaperInput,
                    onValueChange = { wallpaperInput = it },
                    placeholder = { Text("输入背景图路径或网络 URL", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (wallpaperInput.isNotBlank()) {
                                viewModel.setWallpaperPath(wallpaperInput)
                                Toast.makeText(localContext, "背景图设置成功！", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(localContext, "请输入有效的背景路径", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("保存背景", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            wallpaperInput = ""
                            viewModel.setWallpaperPath(null)
                            Toast.makeText(localContext, "背景已重置", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text("重置默认", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (wallpaperPath != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("背景透明度: ${(wallpaperOpacity * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = wallpaperOpacity,
                        onValueChange = { viewModel.setWallpaperOpacity(it) },
                        valueRange = 0.05f..0.95f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "3. 终端字体 & 字号配置",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            letterSpacing = (-0.2).sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("字体字号: ${fontSize.toInt()} sp", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "预览效果: $ [ok]",
                        fontFamily = when (fontFamily) {
                            "Monospace" -> FontFamily.Monospace
                            "Serif" -> FontFamily.Serif
                            "SansSerif" -> FontFamily.SansSerif
                            else -> FontFamily.Monospace
                        },
                        fontSize = fontSize.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = fontSize,
                    onValueChange = { viewModel.setFontSize(it) },
                    valueRange = 10f..24f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("字体类型:", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Monospace", "SansSerif", "Serif").forEach { font ->
                        val selected = fontFamily == font
                        ElevatedFilterChip(
                            selected = selected,
                            onClick = { viewModel.setFontFamily(font) },
                            label = { Text(font, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "4. 管理终端快捷命令面板",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            letterSpacing = (-0.2).sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = customShortcutInput,
                        onValueChange = { customShortcutInput = it },
                        placeholder = { Text("输入常用命令 e.g. python -h", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f), fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (customShortcutInput.isNotBlank()) {
                                viewModel.addShortcut(customShortcutInput)
                                customShortcutInput = ""
                                Toast.makeText(localContext, "快捷指令已添加", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("添加", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("当前所有快捷键:", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    shortcuts.forEach { cmd ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Terminal, contentDescription = "Cmd", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                cmd,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.removeShortcut(cmd) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// 6. GRAPHICAL DESKTOP & VNC REMOTE SERVICES
// ==========================================================
@Composable
fun DesktopAndVncSection(viewModel: MainViewModel) {
    val distros by viewModel.prootDistroManager.distros.collectAsStateWithLifecycle()
    val installedDistros = distros.filter { it.isInstalled }

    if (installedDistros.isEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Layers,
                    contentDescription = "No GUI",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "未检测到已部署容器",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "请在下方部署任一 Linux 容器 (例如 Ubuntu) 即可开启一键图形化桌面和内置 VNC 启动功能。",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
        return
    }

    var selectedDistroId by remember { mutableStateOf("") }
    if (selectedDistroId.isEmpty() && installedDistros.isNotEmpty()) {
        selectedDistroId = installedDistros.first().id
    }
    val selectedDistro = installedDistros.find { it.id == selectedDistroId } ?: installedDistros.firstOrNull()

    if (selectedDistro == null) return

    val isDesktopInstalled = viewModel.settingsManager.isDesktopInstalled(selectedDistro.id)
    val isDesktopInstalling by viewModel.isDesktopInstalling.collectAsStateWithLifecycle()
    val desktopProgress by viewModel.desktopInstallProgress.collectAsStateWithLifecycle()
    val desktopLogs by viewModel.desktopInstallLogs.collectAsStateWithLifecycle()
    val vncRunningMap by viewModel.isVncRunning.collectAsStateWithLifecycle()
    val isVncRunning = vncRunningMap[selectedDistro.id] ?: viewModel.settingsManager.isVncRunning(selectedDistro.id)

    var showVncDesktopDialog by remember { mutableStateOf(false) }

    if (showVncDesktopDialog) {
        VncDesktopSimDialog(
            distroName = selectedDistro.name,
            viewModel = viewModel,
            onDismiss = { showVncDesktopDialog = false }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Layers,
                    contentDescription = "Desktop GUI",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "极速图形化桌面与 VNC 服务",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    letterSpacing = (-0.2).sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (installedDistros.size > 1) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "选择容器: ",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(installedDistros) { d ->
                            FilterChip(
                                selected = selectedDistroId == d.id,
                                onClick = { selectedDistroId = d.id },
                                label = { Text(d.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (isDesktopInstalling) {
                Text(
                    text = "正在部署图形桌面 (XFCE4 & VNC)...",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = desktopProgress,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(desktopProgress * 100).toInt()}%",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    val scrollState = rememberScrollState()
                    LaunchedEffect(desktopLogs) {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                    Text(
                        text = desktopLogs,
                        color = Color(0xFF00FF00),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        modifier = Modifier.verticalScroll(scrollState)
                    )
                }
            } else if (!isDesktopInstalled) {
                Text(
                    text = "状态: 未安装 XFCE 经典桌面环境",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "自动拉取、解包并一键配置轻量、流畅的 XFCE4 视窗管理器和内置 TigerVNC 远程服务器。无须用户输入任何命令行或手动配置，系统已全自动深度适配。",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.installDesktop(selectedDistro.id) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Build, contentDescription = "Install GUI")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("一键部署图形化桌面 (免配置)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "远程服务状态: ",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isVncRunning) "● VNC 服务已运行" else "○ VNC 服务已停止",
                        color = if (isVncRunning) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (isVncRunning) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "系统内置直连参数：",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text("• 远程地址: 127.0.0.1:5901 (Display: :1)", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                            Text("• 内置密码: 123456 (免配置直连)", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                        }
                    }
                } else {
                    Text(
                        text = "图形桌面就绪。一键开启后即可通过本地 5901 端口直连，或点击下方【打开内置 VNC 桌面】沉浸式体验窗口系统。",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (!isVncRunning) {
                            viewModel.toggleVnc(selectedDistro.id)
                        }
                        showVncDesktopDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF388E3C),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Language, contentDescription = "One-Click VNC")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isVncRunning) "一键连接 XFCE VNC 桌面" else "一键启动并连接 VNC 桌面",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                if (isVncRunning) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.toggleVnc(selectedDistro.id) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFD32F2F)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop VNC", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("关闭 VNC 远程服务", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun VncDesktopSimDialog(
    distroName: String,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = 真实 VNC, 1 = 离线模拟桌面
    
    var isTerminalOpen by remember { mutableStateOf(false) }
    var isBrowserOpen by remember { mutableStateOf(false) }
    var isFilesOpen by remember { mutableStateOf(false) }
    var isStoreOpen by remember { mutableStateOf(false) }
    var isStartMenuOpen by remember { mutableStateOf(false) }

    var timeText by remember { mutableStateOf("12:00:00") }
    LaunchedEffect(Unit) {
        while (true) {
            val cal = java.util.Calendar.getInstance()
            val h = String.format("%02d", cal.get(java.util.Calendar.HOUR_OF_DAY))
            val m = String.format("%02d", cal.get(java.util.Calendar.MINUTE))
            val s = String.format("%02d", cal.get(java.util.Calendar.SECOND))
            timeText = "$h:$m:$s"
            delay(1000)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0C091A))
        ) {
            // Mode Select Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141124))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .background(Color(0xFF1F1B35), RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val activeColor = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White)
                    val idleColor = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.LightGray)
                    
                    Button(
                        onClick = { selectedTab = 0 },
                        colors = if (selectedTab == 0) activeColor else idleColor,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = "Real VNC", modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("连接真实 VNC", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = { selectedTab = 1 },
                        colors = if (selectedTab == 1) activeColor else idleColor,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Monitor, contentDescription = "Sim", modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("体验模拟桌面", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                IconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                }
            }
            
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (selectedTab == 0) {
                    VncViewContainer(onClose = onDismiss)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF0F081D), Color(0xFF1B0E33), Color(0xFF05030B))
                                )
                            )
                    ) {
                        // Wallpaper Grid Pattern overlay for tech aesthetic
                        Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 40.dp.toPx()
                for (x in 0..size.width.toInt() step step.toInt()) {
                    drawLine(Color(0x0AFFFFFF), start = androidx.compose.ui.geometry.Offset(x.toFloat(), 0f), end = androidx.compose.ui.geometry.Offset(x.toFloat(), size.height), strokeWidth = 1f)
                }
                for (y in 0..size.height.toInt() step step.toInt()) {
                    drawLine(Color(0x0AFFFFFF), start = androidx.compose.ui.geometry.Offset(0f, y.toFloat()), end = androidx.compose.ui.geometry.Offset(size.width, y.toFloat()), strokeWidth = 1f)
                }
            }

            // Desktop Icons Workspace area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp, start = 16.dp, end = 16.dp, bottom = 72.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Icon for terminal
                DesktopIcon(
                    name = "MATE 终端",
                    icon = Icons.Default.Terminal,
                    onClick = {
                        isTerminalOpen = true
                        isStartMenuOpen = false
                    }
                )

                // Icon for files
                DesktopIcon(
                    name = "Thunar 文件管理器",
                    icon = Icons.Default.Folder,
                    onClick = {
                        isFilesOpen = true
                        isStartMenuOpen = false
                    }
                )

                // Icon for Browser
                DesktopIcon(
                    name = "Chromium 浏览器",
                    icon = Icons.Default.Language,
                    onClick = {
                        isBrowserOpen = true
                        isStartMenuOpen = false
                    }
                )

                // Icon for App Store
                DesktopIcon(
                    name = "应用软件商店",
                    icon = Icons.Default.ShoppingBag,
                    onClick = {
                        isStoreOpen = true
                        isStartMenuOpen = false
                    }
                )
            }

            // FLOATING WINDOWS OVERLAYS
            if (isTerminalOpen) {
                DesktopWindowSim(
                    title = "root@localhost:~ (bash)",
                    onClose = { isTerminalOpen = false }
                ) {
                    var terminalHistory = remember {
                        mutableStateListOf(
                            "\u001B[32mWelcome to XFCE Terminal Emulator\u001B[0m",
                            "root@termux-container:~# "
                        )
                    }
                    var cmdInput by remember { mutableStateOf("") }
                    val listState = rememberLazyListState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F0F0F))
                            .padding(8.dp)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(terminalHistory) { line ->
                                SelectionContainer {
                                    Text(
                                        text = AnsiParser.parse(line, viewModel.settingsManager.theme),
                                        color = Color(0xFFDCDCDC),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Quick action buttons for commands
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp)
                        ) {
                            val cmds = listOf("neofetch", "cmatrix", "cowsay", "sl", "clear")
                            cmds.forEach { c ->
                                Button(
                                    onClick = {
                                        if (c == "clear") {
                                            terminalHistory.clear()
                                            terminalHistory.add("root@termux-container:~# ")
                                        } else {
                                            terminalHistory.add("root@termux-container:~# $c")
                                            val response = when (c) {
                                                "neofetch" -> """
                                                    [31m            .-/+oossssoo+/-.[0m
                                                    [31m        `:+ssssssssssssssssss+:`[0m      [36mroot@termux-hub[0m
                                                    [31m      -+ssssssssssssssssssyyssss+-[0m    ---------------
                                                    [31m    .ossssssssssssssssssdMMMNysssso.[0m  [33mOS[0m: Ubuntu 22.04.2 LTS
                                                    [31m   /ssssssssssshdmmNNmmyNMMMMhssssss/[0m [33mHost[0m: Proot Virtual Container
                                                    [31m  +ssssssssshmydMMMMMMMNddddyssssssss+[0m [33mKernel[0m: Android-Linux 5.10.110
                                                    [31m /ssssssshssyNMMMyhhyshhhysssssssssss/[0m [33mUptime[0m: 1 hour, 12 mins
                                                    [31m.ssssssshshdMMMMMyyyssssssssssssssssss.[0m [33mShell[0m: bash 5.1.16
                                                    [31m.ssssssshshdMMMMMyyyssssssssssssssssss.[0m [33mResolution[0m: 1280x720 (VNC :1)
                                                    [31m /ssssssshssyNMMMyhhyshhhysssssssssss/[0m [33mDE[0m: XFCE 4.16
                                                    [31m  +ssssssssshmydMMMMMMMNddddyssssssss+[0m [33mWM[0m: Xfwm4
                                                    [31m   /ssssssssssshdmmNNmmyNMMMMhssssss/[0m [33mTerminal[0m: xfce4-terminal
                                                    [31m    .ossssssssssssssssssdMMMNysssso.[0m  [33mCPU[0m: ARMv8 Neon (8 Cores)
                                                    [31m      -+ssssssssssssssssssyyssss+-[0m    [33mMemory[0m: 3.8 GiB / 7.6 GiB
                                                    [31m        `:+ssssssssssssssssss+:`[0m
                                                    [31m            .-/+oossssoo+/-.[0m
                                                """.trimIndent()
                                                "cmatrix" -> """
                                                    [32m0 1 0 1 0 0 1 1 0 1 1 0 1 0 1 0 0 1 0
                                                    1 0 1 0 1 1 0 1 1 0 1 0 0 1 1 0 1 0 1
                                                    0 1 1 0 1 0 1 1 0 0 1 1 0 1 0 1 0 1 0
                                                    Matrix digital rain scrolling...
                                                    [32m[+] Simulation finished successfully.[0m
                                                """.trimIndent()
                                                "cowsay" -> """
                                                     _______________________
                                                    < Hello from XFCE4 VNC! >
                                                     -----------------------
                                                            \   ^__^
                                                             \  (oo)\_______
                                                                (__)\       )\/\
                                                                    ||----w |
                                                                    ||     ||
                                                """.trimIndent()
                                                "sl" -> """
                                                    [33m      ____||____
                                                    _||_  |  []  |  _||_
                                                   (____) |______| (____)
                                                   /o o \          / o o\
                                                   ======================
                                                   Choo Choo! Steam Locomotive sailed over.[0m
                                                """.trimIndent()
                                                else -> "bash: command not found: $c"
                                            }
                                            terminalHistory.add(response)
                                            terminalHistory.add("root@termux-container:~# ")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262626), contentColor = Color.White),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text(c, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }

            if (isFilesOpen) {
                DesktopWindowSim(
                    title = "Thunar 文件浏览器",
                    onClose = { isFilesOpen = false }
                ) {
                    var currentPath by remember { mutableStateOf("/home/root") }
                    var fileContentDialog by remember { mutableStateOf<String?>(null) }

                    if (fileContentDialog != null) {
                        AlertDialog(
                            onDismissRequest = { fileContentDialog = null },
                            title = { Text("文件内容查看", fontWeight = FontWeight.Bold) },
                            text = {
                                Text(
                                    fileContentDialog ?: "",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.05f))
                                        .padding(8.dp)
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = { fileContentDialog = null }) {
                                    Text("确定")
                                }
                            }
                        )
                    }

                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left navigation panel
                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("快捷位置", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                            ThunarPlaceItem("根目录 /", Icons.Default.Layers) { currentPath = "/" }
                            ThunarPlaceItem("主目录 ~", Icons.Default.Home) { currentPath = "/home/root" }
                            ThunarPlaceItem("文档夹", Icons.Default.Folder) { currentPath = "/home/root/Documents" }
                            ThunarPlaceItem("下载夹", Icons.Default.Folder) { currentPath = "/home/root/Downloads" }
                        }

                        // Right file panel
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(Color.White)
                                .padding(8.dp)
                        ) {
                            Text(
                                "目录: $currentPath",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            val files = when (currentPath) {
                                "/" -> listOf(
                                    Pair("bin", "文件夹"),
                                    Pair("etc", "文件夹"),
                                    Pair("home", "文件夹"),
                                    Pair("usr", "文件夹"),
                                    Pair("var", "文件夹")
                                )
                                "/home/root" -> listOf(
                                    Pair("Documents", "文件夹"),
                                    Pair("Downloads", "文件夹"),
                                    Pair("README.txt", "240 字节 (文本文件)"),
                                    Pair("main.py", "1.2 kB (Python代码)")
                                )
                                "/home/root/Documents" -> listOf(
                                    Pair("config.json", "512 字节 (JSON)"),
                                    Pair("notes.md", "4.2 kB (Markdown)")
                                )
                                "/home/root/Downloads" -> listOf(
                                    Pair("setup.sh", "850 字节 (Shell脚本)")
                                )
                                else -> emptyList()
                            }

                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(files) { (name, desc) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                fileContentDialog = when (name) {
                                                    "README.txt" -> "========================\nWelcome to XFCE4 Desktop!\n========================\nThis is a virtual desktop sandbox configured automatically inside this app.\nEnjoy fully offline-isolated sandboxed container controls!"
                                                    "main.py" -> "import os\nimport sys\n\ndef main():\n    print('Hello, secure VNC user!')\n    print('Python version:', sys.version)\n\nif __name__ == '__main__':\n    main()"
                                                    "config.json" -> "{\n  \"vnc_server\": \"tigervnc\",\n  \"display\": \":1\",\n  \"port\": 5901,\n  \"desktop_environment\": \"xfce4\",\n  \"auto_config\": true\n}"
                                                    "notes.md" -> "# XFCE Container Notes\n- Zero setup required\n- Built-in resolution optimized: 1280x720\n- High fidelity visual simulator embedded."
                                                    "setup.sh" -> "#!/bin/bash\necho 'Configuring VNC setup...'\necho 'Writing default startup logs...'\necho 'Done!'"
                                                    else -> "这是一个目录: $name"
                                                }
                                            }
                                            .padding(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (desc == "文件夹") Icons.Default.Folder else Icons.Default.Layers,
                                            contentDescription = "File Icon",
                                            tint = if (desc == "文件夹") Color(0xFFFFC107) else Color(0xFF9C27B0),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(name, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(desc, color = Color.Gray, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isBrowserOpen) {
                DesktopWindowSim(
                    title = "Chromium 极速网页浏览器",
                    onClose = { isBrowserOpen = false }
                ) {
                    var webAddress by remember { mutableStateOf("https://www.google.com") }
                    var searchWord by remember { mutableStateOf("") }
                    var hasSearched by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF5F5F5))
                    ) {
                        // Navigation bar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(6.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Gray, modifier = Modifier.size(20.dp).clickable { hasSearched = false })
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = webAddress,
                                onValueChange = { webAddress = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp),
                                textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFEFEFEF),
                                    unfocusedContainerColor = Color(0xFFEFEFEF),
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                singleLine = true
                            )
                        }

                        Divider(color = Color.LightGray)

                        // Web view body simulation
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (!hasSearched) {
                                Text(
                                    text = "Google",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                OutlinedTextField(
                                    value = searchWord,
                                    onValueChange = { searchWord = it },
                                    placeholder = { Text("搜索关于 Linux、容器、VNC 的奥秘...", fontSize = 11.sp) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .padding(horizontal = 8.dp),
                                    textStyle = TextStyle(fontSize = 12.sp),
                                    shape = RoundedCornerShape(22.dp),
                                    singleLine = true,
                                    keyboardActions = KeyboardActions(onSearch = { hasSearched = true }),
                                    trailingIcon = {
                                        IconButton(onClick = { hasSearched = true }) {
                                            Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                )
                            } else {
                                Text(
                                    text = "关于 '${searchWord}' 的安全搜索结果:",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                )

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("1. 如何在一键 VNC 桌面中部署开发环境？", color = Color(0xFF1A0DAB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("打开内置 Terminal 终端，输入 'apt update && apt install python3' 即可一键完成。本应用已内置中文字体和快捷 DPI 调节，画面极度丝滑。", color = Color.DarkGray, fontSize = 10.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("2. VNC 与 XFCE 经典桌面的优越性", color = Color(0xFF1A0DAB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("XFCE 是一套极其轻量且美观的 Linux 桌面。配合 VNC 服务，可在几乎不消耗手机电量的情况下提供极其丰富的图形视窗交互体验。", color = Color.DarkGray, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isStoreOpen) {
                DesktopWindowSim(
                    title = "应用商店管理器",
                    onClose = { isStoreOpen = false }
                ) {
                    val packages by viewModel.packageStoreManager.packages.collectAsStateWithLifecycle()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFECEFF1))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            Text("图形化内置包状态一览：", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.DarkGray, modifier = Modifier.padding(4.dp))
                        }
                        items(packages) { pkg ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = "Pkg",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pkg.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        Text("来源: ${pkg.sourceName} | 大小: ${pkg.size}", fontSize = 9.sp, color = Color.Gray)
                                    }
                                    Text(
                                        text = if (pkg.isInstalled) "已部署" else "未安装",
                                        color = if (pkg.isInstalled) Color(0xFF4CAF50) else Color.Gray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // WHISKER START MENU OVERLAY
            if (isStartMenuOpen) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 56.dp, start = 8.dp)
                        .width(220.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFEADDFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = "User", tint = Color(0xFF21005D), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("root (超级管理员)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("ubuntu @ desktop", color = Color.Gray, fontSize = 9.sp)
                            }
                        }

                        Divider(color = Color(0x22FFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                        WhiskerMenuItem("MATE 控制台终端", Icons.Default.Terminal) {
                            isTerminalOpen = true
                            isStartMenuOpen = false
                        }
                        WhiskerMenuItem("Thunar 文件管理器", Icons.Default.Folder) {
                            isFilesOpen = true
                            isStartMenuOpen = false
                        }
                        WhiskerMenuItem("Chromium 网络浏览器", Icons.Default.Language) {
                            isBrowserOpen = true
                            isStartMenuOpen = false
                        }
                        WhiskerMenuItem("软件包中心商店", Icons.Default.ShoppingBag) {
                            isStoreOpen = true
                            isStartMenuOpen = false
                        }

                        Divider(color = Color(0x22FFFFFF), modifier = Modifier.padding(vertical = 6.dp))

                        WhiskerMenuItem("断开 VNC 桌面连接", Icons.Default.Close, iconColor = Color.Red) {
                            isStartMenuOpen = false
                            onDismiss()
                        }
                    }
                }
            }

            // BOTTOM TASK BAR (XFCE Panel)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(Color(0xE61C1B22))
                    .border(BorderStroke(1.dp, Color(0x1FFFFFFF)))
                    .padding(horizontal = 8.dp)
            ) {
                // Application menu button
                IconButton(
                    onClick = { isStartMenuOpen = !isStartMenuOpen },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isStartMenuOpen) Color(0x33FFFFFF) else Color.Transparent)
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Apps", tint = Color.White, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Short launch icons on panel
                PanelLaunchIcon(Icons.Default.Terminal, isTerminalOpen) { isTerminalOpen = !isTerminalOpen }
                PanelLaunchIcon(Icons.Default.Folder, isFilesOpen) { isFilesOpen = !isFilesOpen }
                PanelLaunchIcon(Icons.Default.Language, isBrowserOpen) { isBrowserOpen = !isBrowserOpen }
                PanelLaunchIcon(Icons.Default.ShoppingBag, isStoreOpen) { isStoreOpen = !isStoreOpen }

                Spacer(modifier = Modifier.weight(1f))

                // Status Clock Panel
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0x1AFFFFFF), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Secure", tint = Color(0xFF4CAF50), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = timeText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Disconnect power button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFD32F2F))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Disconnect VNC", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
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
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x33FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = name, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            lineHeight = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.shadow(1.dp)
        )
    }
}

@Composable
fun PanelLaunchIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) Color(0x22FFFFFF) else Color.Transparent)
    ) {
        Icon(icon, contentDescription = "Launch Icon", tint = if (isActive) MaterialTheme.colorScheme.primary else Color.White, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun DesktopWindowSim(
    title: String,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.LightGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Window Header Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE0E0E0))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Win", tint = Color.DarkGray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(14.dp))
                }
            }

            Divider(color = Color.LightGray)

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun WhiskerMenuItem(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp)
    ) {
        Icon(icon, contentDescription = name, tint = iconColor, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ThunarPlaceItem(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Icon(icon, contentDescription = name, tint = Color.Gray, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(name, color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ManageSourcesDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val customRepos by viewModel.packageStoreManager.customRepos.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var customRepoName by remember { mutableStateOf("") }
    var customRepoUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "管理软件源",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                letterSpacing = (-0.5).sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "在此一键添加推荐的第三方官方或自定义源，添加后可在【包商店】中直接过滤和安装对应包。",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    fontSize = 11.sp
                )

                // Current Repos
                Text("当前软件源列表:", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = "System", tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("官方默认源 (Termux Official Hub)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        customRepos.forEach { repo ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Layers, contentDescription = "Custom Repo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(repo.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(repo.url, fontSize = 9.sp, color = Color.Gray)
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.packageStoreManager.removeCustomRepo(repo.url)
                                        Toast.makeText(context, "成功卸载源: ${repo.name}", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Recommended Repos
                Text("一键添加推荐第三方源:", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                viewModel.packageStoreManager.recommendedRepos.forEach { rec ->
                    val isAlreadyAdded = customRepos.any { it.url == rec.url }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rec.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("提供: ${rec.packages.joinToString { it.name }}", fontSize = 9.sp, color = Color.Gray)
                        }
                        Button(
                            onClick = {
                                if (viewModel.packageStoreManager.addCustomRepo(rec)) {
                                    Toast.makeText(context, "成功加载源: ${rec.name}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "该源已存在", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isAlreadyAdded,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isAlreadyAdded) Color.Gray else MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (isAlreadyAdded) "已加载" else "一键添加", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Custom repo inputs
                Text("手动添加自定义软件源:", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = customRepoName,
                    onValueChange = { customRepoName = it },
                    label = { Text("源名称 (e.g. 极客工具源)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = customRepoUrl,
                    onValueChange = { customRepoUrl = it },
                    label = { Text("源链接 (e.g. https://my-custom.org)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (customRepoName.isNotBlank() && customRepoUrl.isNotBlank()) {
                            val customRepo = com.example.store.CustomRepo(
                                name = customRepoName,
                                url = customRepoUrl,
                                packages = listOf(
                                    com.example.store.PackageItem("wget", "Wget Utility", "1.21.4", "系统工具", "一个在命令行下下载网络文件的超级工具。", "420 kB", "utils", sourceName = customRepoName),
                                    com.example.store.PackageItem("neovim", "Neovim CLI", "0.9.1", "开发环境", "一种旨在提高可扩展性和可用性的 Vim 衍生编辑器。", "2.8 MB", "code", sourceName = customRepoName)
                                )
                            )
                            if (viewModel.packageStoreManager.addCustomRepo(customRepo)) {
                                Toast.makeText(context, "成功加载自定义源: $customRepoName", Toast.LENGTH_SHORT).show()
                                customRepoName = ""
                                customRepoUrl = ""
                            } else {
                                Toast.makeText(context, "源链接已存在！", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "请填入完整参数！", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("添加自定义源", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

