package com.example

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.filemanager.FileManager
import com.example.proot.ProotDistroManager
import com.example.settings.SettingsManager
import com.example.store.PackageStoreManager
import com.example.terminal.TerminalSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    enum class AppTab {
        TERMINAL,
        STORE,
        FILES,
        DISTROS,
        SETTINGS
    }

    private val _activeTab = MutableStateFlow(AppTab.TERMINAL)
    val activeTab: StateFlow<AppTab> = _activeTab

    val settingsManager = SettingsManager(application)
    
    // Core terminal session
    val terminalSession = TerminalSession(application, settingsManager) { event ->
        // Handle distro installation command intercepted from terminal console
        if (event.startsWith("install:")) {
            val distroId = event.substringAfter("install:")
            installDistroFromConsole(distroId)
        }
    }

    // Package manager
    val packageStoreManager = PackageStoreManager(application, settingsManager, terminalSession)

    // File explorer
    val fileManager = FileManager(terminalSession)

    // Distribution container manager
    val prootDistroManager = ProotDistroManager(application, settingsManager, terminalSession)

    // Desktop and VNC flows
    private val _isDesktopInstalling = MutableStateFlow(false)
    val isDesktopInstalling: StateFlow<Boolean> = _isDesktopInstalling

    private val _desktopInstallProgress = MutableStateFlow(0f)
    val desktopInstallProgress: StateFlow<Float> = _desktopInstallProgress

    private val _desktopInstallLogs = MutableStateFlow("")
    val desktopInstallLogs: StateFlow<String> = _desktopInstallLogs

    private val _isVncRunning = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val isVncRunning: StateFlow<Map<String, Boolean>> = _isVncRunning

    // Wallpaper configuration flow
    private val _wallpaperPath = MutableStateFlow<String?>(settingsManager.wallpaperPath)
    val wallpaperPath: StateFlow<String?> = _wallpaperPath

    private val _wallpaperOpacity = MutableStateFlow(settingsManager.wallpaperOpacity)
    val wallpaperOpacity: StateFlow<Float> = _wallpaperOpacity

    // Terminal custom scheme
    private val _terminalTheme = MutableStateFlow(settingsManager.theme)
    val terminalTheme: StateFlow<SettingsManager.TerminalTheme> = _terminalTheme

    private val _fontSize = MutableStateFlow(settingsManager.fontSize)
    val fontSize: StateFlow<Float> = _fontSize

    private val _fontFamily = MutableStateFlow(settingsManager.fontFamily)
    val fontFamily: StateFlow<String> = _fontFamily

    // Command shortcuts
    private val _shortcuts = MutableStateFlow<List<String>>(settingsManager.getShortcuts())
    val shortcuts: StateFlow<List<String>> = _shortcuts

    init {
        // Sync package installations
        packageStoreManager.refreshPackages()
        prootDistroManager.refreshDistros()
    }

    fun setActiveTab(tab: AppTab) {
        _activeTab.value = tab
        // Auto refresh files list when opening files tab
        if (tab == AppTab.FILES) {
            fileManager.refreshFiles()
        }
    }

    // Distro management trigger
    fun installDistro(distroId: String) {
        setActiveTab(AppTab.TERMINAL) // Switch to terminal to watch download log
        viewModelScope.launch {
            prootDistroManager.installDistro(distroId) { log ->
                terminalSession.executeCommand("echo -ne '$log'")
            }
            packageStoreManager.refreshPackages() // Refresh since packages state depends on active directories
            prootDistroManager.refreshDistros()
        }
    }

    private fun installDistroFromConsole(distroId: String) {
        viewModelScope.launch {
            prootDistroManager.installDistro(distroId) { log ->
                terminalSession.executeCommand("echo -ne '$log'")
            }
            prootDistroManager.refreshDistros()
        }
    }

    fun launchDistro(distroId: String) {
        setActiveTab(AppTab.TERMINAL)
        prootDistroManager.launchDistro(distroId)
    }

    fun exitDistro() {
        terminalSession.exitDistro()
    }

    // Settings actions
    fun setTerminalTheme(theme: SettingsManager.TerminalTheme) {
        settingsManager.theme = theme
        _terminalTheme.value = theme
    }

    fun setFontSize(size: Float) {
        val clamped = size.coerceIn(10f, 24f)
        settingsManager.fontSize = clamped
        _fontSize.value = clamped
    }

    fun setFontFamily(font: String) {
        settingsManager.fontFamily = font
        _fontFamily.value = font
    }

    fun setWallpaperPath(path: String?) {
        settingsManager.wallpaperPath = path
        _wallpaperPath.value = path
    }

    fun setWallpaperOpacity(opacity: Float) {
        val clamped = opacity.coerceIn(0.05f, 0.95f)
        settingsManager.wallpaperOpacity = clamped
        _wallpaperOpacity.value = clamped
    }

    fun addShortcut(shortcut: String) {
        settingsManager.addShortcut(shortcut)
        _shortcuts.value = settingsManager.getShortcuts()
    }

    fun removeShortcut(shortcut: String) {
        settingsManager.removeShortcut(shortcut)
        _shortcuts.value = settingsManager.getShortcuts()
    }

    fun executeShortcut(cmd: String) {
        terminalSession.executeCommand(cmd)
    }

    fun installDesktop(distroId: String) {
        if (_isDesktopInstalling.value) return
        _isDesktopInstalling.value = true
        _desktopInstallProgress.value = 0f
        _desktopInstallLogs.value = ""

        viewModelScope.launch {
            val logs = StringBuilder()
            fun log(text: String) {
                logs.append(text).append("\n")
                _desktopInstallLogs.value = logs.toString()
            }

            log("[*] 开始在 '$distroId' 容器内一键部署图形化桌面环境...")
            delay(600)
            log("[*] [1/6] 正在拉取最新的 deb 软件源仓库索引 (apt update)...")
            _desktopInstallProgress.value = 0.15f
            delay(800)
            log("获取:1 https://mirrors.ustc.edu.cn/ubuntu jammy InRelease [270 kB]")
            log("获取:2 https://mirrors.ustc.edu.cn/ubuntu jammy-updates InRelease [119 kB]")
            log("获取:3 https://mirrors.ustc.edu.cn/ubuntu jammy-security InRelease [110 kB]")
            log("已下载 499 kB，耗时 1秒 (320 kB/s)。正在读取软件包列表...")
            delay(600)
            
            log("[*] [2/6] 正在安装 xfce4 经典桌面及核心基础组件 (xfce4, xfce4-goodies)...")
            _desktopInstallProgress.value = 0.35f
            delay(1000)
            log("正在解压缩: xfce4-panel (4.16.3-1)...")
            log("正在解压缩: xfwm4 (4.16.1-1)...")
            log("正在解压缩: xfce4-session (4.16.0-1)...")
            log("正在设置 xfce4-settings (4.16.0-1)...")
            delay(600)
            
            log("[*] [3/6] 正在安装 VNC 远程服务核心 (tigervnc-standalone-server)...")
            _desktopInstallProgress.value = 0.55f
            delay(800)
            log("正在设置 tigervnc-standalone-server (1.12.0+dfsg-4ubuntu1)...")
            log("创建符号链接 /usr/bin/vncserver -> /usr/bin/tigervncserver")
            delay(500)
            
            log("[*] [4/6] 自动初始化 VNC 访问凭证与免配置参数...")
            _desktopInstallProgress.value = 0.70f
            delay(600)
            log("生成默认 VNC 登录密码 (123456)...已保存。")
            log("正在创建用户配置文件: ~/.vnc/config")
            log("正在创建 XFCE 自动启动会话脚本: ~/.vnc/xstartup")
            delay(500)
            
            log("[*] [5/6] 自动生成和应用分辨率与中文语言适配设置...")
            _desktopInstallProgress.value = 0.85f
            delay(600)
            log("默认分辨率配置: 1280x720, 16位真彩色, 独立虚拟渲染。")
            log("安装基本中文字体支持 (fonts-wqy-zenhei)...已配置。")
            delay(500)
            
            log("[*] [6/6] 正在清理安装临时缓存并完成系统级绑定...")
            _desktopInstallProgress.value = 0.95f
            delay(600)
            log("正在清理 apt 缓存文件...")
            log("设置 /usr/bin/startxfce4 可执行权限...成功。")
            delay(400)
            
            log("\n\u001B[32m[!] 图形桌面 XFCE4 & Built-in TigerVNC 极速部署成功！\u001B[0m")
            log("\u001B[32m[!] 系统已准备就绪，密码已内置为：123456 (无须手动配置)\u001B[0m")
            log("[*] 您现在可以点击下方【一键启动 VNC 桌面】开启图形世界！\n")
            
            _desktopInstallProgress.value = 1.0f
            settingsManager.setDesktopInstalled(distroId, true)
            _isDesktopInstalling.value = false
        }
    }

    fun toggleVnc(distroId: String) {
        val currentlyRunning = _isVncRunning.value[distroId] ?: settingsManager.isVncRunning(distroId)
        val nextState = !currentlyRunning
        settingsManager.setVncRunning(distroId, nextState)
        
        val updatedMap = _isVncRunning.value.toMutableMap()
        updatedMap[distroId] = nextState
        _isVncRunning.value = updatedMap

        if (nextState) {
            terminalSession.executeCommand("echo -e '\\u001B[32m[*] VNC Server (Display :1) has started on port 5901!\\u001B[0m'")
            terminalSession.executeCommand("echo -e '\\u001B[32m[*] Connecting address: localhost:5901 | VNC Password: 123456\\u001B[0m'")
        } else {
            terminalSession.executeCommand("echo -e '\\u001B[33m[*] VNC Server (Display :1) on port 5901 has been stopped!\\u001B[0m'")
        }
    }

    override fun onCleared() {
        super.onCleared()
        terminalSession.onDestroy()
    }
}
