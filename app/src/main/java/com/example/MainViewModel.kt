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

    override fun onCleared() {
        super.onCleared()
        terminalSession.onDestroy()
    }
}
