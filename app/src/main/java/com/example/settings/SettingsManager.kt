package com.example.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import org.json.JSONArray

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("termux_branch_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_THEME = "terminal_theme"
        const val KEY_FONT = "terminal_font"
        const val KEY_FONT_SIZE = "terminal_font_size"
        const val KEY_SHORTCUTS = "terminal_shortcuts"
        const val KEY_WALLPAPER = "terminal_wallpaper"
        const val KEY_WALLPAPER_OPACITY = "terminal_wallpaper_opacity"
        const val KEY_INSTALLED_PACKAGES = "installed_packages"
        const val KEY_INSTALLED_DISTROS = "installed_distros"
    }

    // Themes definition
    enum class TerminalTheme(
        val displayName: String,
        val background: Color,
        val foreground: Color,
        val cursorColor: Color,
        val ansiColors: List<Color>
    ) {
        MATRIX_GREEN(
            "Matrix Green",
            Color(0xFF000500),
            Color(0xFF00FF00),
            Color(0xFF00FF00),
            listOf(
                Color(0xFF000000), Color(0xFFFF5555), Color(0xFF00FF00), Color(0xFFFFFF55),
                Color(0xFF5555FF), Color(0xFFFF55FF), Color(0xFF55FFFF), Color(0xFFFFFFFF)
            )
        ),
        RETRO_AMBER(
            "Retro Amber",
            Color(0xFF0D0800),
            Color(0xFFFFB000),
            Color(0xFFFFB000),
            listOf(
                Color(0xFF000000), Color(0xFFFF5555), Color(0xFFFFB000), Color(0xFFFFA000),
                Color(0xFFCC7A00), Color(0xFFFF9900), Color(0xFFFFC266), Color(0xFFFFFFFF)
            )
        ),
        CYBERPUNK(
            "Cyberpunk Neon",
            Color(0xFF140D26),
            Color(0xFF00FFFF),
            Color(0xFFFF007F),
            listOf(
                Color(0xFF140D26), Color(0xFFFF007F), Color(0xFF00FF66), Color(0xFFFFFF00),
                Color(0xFF00FFFF), Color(0xFFBD00FF), Color(0xFF00FFFF), Color(0xFFFFFFFF)
            )
        ),
        MONOKAI(
            "Monokai Pro",
            Color(0xFF2D2A2E),
            Color(0xFFFCFCFA),
            Color(0xFFF9357C),
            listOf(
                Color(0xFF2D2A2E), Color(0xFFF9357C), Color(0xFFA6E22E), Color(0xFFE6DB74),
                Color(0xFF66D9EF), Color(0xFFAE81FF), Color(0xFFA1EFE4), Color(0xFFFCFCFA)
            )
        ),
        CLASSIC_DARK(
            "Classic Dark",
            Color(0xFF121212),
            Color(0xFFE0E0E0),
            Color(0xFFFFFFFF),
            listOf(
                Color(0xFF121212), Color(0xFFCF6679), Color(0xFF03DAC6), Color(0xFFFDD835),
                Color(0xFFBB86FC), Color(0xFFCF6679), Color(0xFF03DAC6), Color(0xFFFFFFFF)
            )
        ),
        DEEP_BLUE(
            "Ocean Blue",
            Color(0xFF000F26),
            Color(0xFF80E5FF),
            Color(0xFF80E5FF),
            listOf(
                Color(0xFF000F26), Color(0xFFFF6666), Color(0xFF80FF80), Color(0xFFFFFF80),
                Color(0xFF80E5FF), Color(0xFFFF80FF), Color(0xFF80FFFF), Color(0xFFFFFFFF)
            )
        )
    }

    var theme: TerminalTheme
        get() {
            val name = prefs.getString(KEY_THEME, TerminalTheme.MATRIX_GREEN.name)
            return try {
                TerminalTheme.valueOf(name ?: TerminalTheme.MATRIX_GREEN.name)
            } catch (e: Exception) {
                TerminalTheme.MATRIX_GREEN
            }
        }
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    var fontSize: Float
        get() = prefs.getFloat(KEY_FONT_SIZE, 14f)
        set(value) = prefs.edit().putFloat(KEY_FONT_SIZE, value).apply()

    var fontFamily: String
        get() = prefs.getString(KEY_FONT, "Monospace") ?: "Monospace"
        set(value) = prefs.edit().putString(KEY_FONT, value).apply()

    var wallpaperPath: String?
        get() = prefs.getString(KEY_WALLPAPER, null)
        set(value) = prefs.edit().putString(KEY_WALLPAPER, value).apply()

    var wallpaperOpacity: Float
        get() = prefs.getFloat(KEY_WALLPAPER_OPACITY, 0.25f)
        set(value) = prefs.edit().putFloat(KEY_WALLPAPER_OPACITY, value).apply()

    // Default built-in shortcuts
    private val defaultShortcuts = listOf(
        "ls -la",
        "neofetch",
        "help",
        "cowsay 'Welcome to Termux!'",
        "sl",
        "proot-distro list"
    )

    fun getShortcuts(): List<String> {
        val raw = prefs.getString(KEY_SHORTCUTS, null) ?: return defaultShortcuts
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (e: Exception) {
            defaultShortcuts
        }
    }

    fun saveShortcuts(shortcuts: List<String>) {
        val arr = JSONArray(shortcuts)
        prefs.edit().putString(KEY_SHORTCUTS, arr.toString()).apply()
    }

    fun addShortcut(shortcut: String) {
        val current = getShortcuts().toMutableList()
        if (!current.contains(shortcut) && shortcut.isNotBlank()) {
            current.add(shortcut)
            saveShortcuts(current)
        }
    }

    fun removeShortcut(shortcut: String) {
        val current = getShortcuts().toMutableList()
        current.remove(shortcut)
        saveShortcuts(current)
    }

    fun getInstalledPackages(): Set<String> {
        return prefs.getStringSet(KEY_INSTALLED_PACKAGES, setOf("help")) ?: setOf("help")
    }

    fun addInstalledPackage(pkgId: String) {
        val current = getInstalledPackages().toMutableSet()
        current.add(pkgId)
        prefs.edit().putStringSet(KEY_INSTALLED_PACKAGES, current).apply()
    }

    fun removeInstalledPackage(pkgId: String) {
        val current = getInstalledPackages().toMutableSet()
        current.remove(pkgId)
        prefs.edit().putStringSet(KEY_INSTALLED_PACKAGES, current).apply()
    }

    fun getInstalledDistros(): Set<String> {
        return prefs.getStringSet(KEY_INSTALLED_DISTROS, emptySet()) ?: emptySet()
    }

    fun addInstalledDistro(distroId: String) {
        val current = getInstalledDistros().toMutableSet()
        current.add(distroId)
        prefs.edit().putStringSet(KEY_INSTALLED_DISTROS, current).apply()
    }

    // --- Desktop & VNC persistence ---
    fun isDesktopInstalled(distroId: String): Boolean {
        return prefs.getBoolean("desktop_installed_$distroId", false)
    }

    fun setDesktopInstalled(distroId: String, installed: Boolean) {
        prefs.edit().putBoolean("desktop_installed_$distroId", installed).apply()
    }

    fun isVncRunning(distroId: String): Boolean {
        return prefs.getBoolean("vnc_running_$distroId", false)
    }

    fun setVncRunning(distroId: String, running: Boolean) {
        prefs.edit().putBoolean("vnc_running_$distroId", running).apply()
    }

    // --- Custom Software Sources persistence ---
    fun getCustomSourcesJson(): String {
        return prefs.getString("custom_software_sources", "[]") ?: "[]"
    }

    fun saveCustomSourcesJson(json: String) {
        prefs.edit().putString("custom_software_sources", json).apply()
    }
}
