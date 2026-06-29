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
import kotlinx.coroutines.launch
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
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Development", "Utilities", "Fun & Toys")
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
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
            (selectedCategory == "All" || pkg.category == selectedCategory) &&
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
                    Text(pkg.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = (-0.2).sp)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(distros) { distro ->
                DistroCard(
                    distro = distro,
                    onInstallClick = { viewModel.installDistro(distro.id) },
                    onBootClick = { viewModel.launchDistro(distro.id) }
                )
            }
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
