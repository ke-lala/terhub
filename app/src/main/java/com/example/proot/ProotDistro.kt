package com.example.proot

import android.content.Context
import com.example.settings.SettingsManager
import com.example.terminal.TerminalSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

data class DistroItem(
    val id: String,
    val name: String,
    val description: String,
    val size: String,
    val colorHex: String,
    val logoText: String, // e.g. "U", "A", "Ar", "D", "K"
    val isInstalled: Boolean = false,
    val isInstalling: Boolean = false,
    val installProgress: Float = 0f
)

class ProotDistroManager(
    private val context: Context,
    private val settingsManager: SettingsManager,
    private val terminalSession: TerminalSession
) {
    private val _distros = MutableStateFlow<List<DistroItem>>(emptyList())
    val distros: StateFlow<List<DistroItem>> = _distros

    private val staticDistros = listOf(
        DistroItem("ubuntu", "Ubuntu Linux", "Popular and stable Debian-based distribution. Great for standard development.", "35.2 MB", "#E95420", "U"),
        DistroItem("alpine", "Alpine Linux", "Ultra-lightweight (5MB) distribution based on musl libc and busybox.", "2.4 MB", "#0D597F", "A"),
        DistroItem("arch", "Arch Linux", "A lightweight and flexible distribution. Features bleeding-edge software.", "42.8 MB", "#1793D1", "Ar"),
        DistroItem("debian", "Debian GNU/Linux", "The universal operating system. Rock solid stability and vast repository.", "28.5 MB", "#D70A53", "D"),
        DistroItem("kali", "Kali Linux", "Advanced penetration testing and security auditing Linux distribution.", "54.1 MB", "#00FFCC", "K")
    )

    init {
        refreshDistros()
    }

    fun refreshDistros() {
        val installed = settingsManager.getInstalledDistros()
        _distros.value = staticDistros.map { distro ->
            distro.copy(isInstalled = installed.contains(distro.id))
        }
    }

    suspend fun installDistro(distroId: String, onLogOutput: (String) -> Unit = {}) {
        _distros.value = _distros.value.map {
            if (it.id == distroId) it.copy(isInstalling = true, installProgress = 0f) else it
        }

        onLogOutput("\r\n\u001B[33m[*] Starting proot-distro installation of '$distroId'...\u001B[0m\r\n")
        delay(300)
        onLogOutput("[*] Resolving rootfs download URL...\r\n")
        delay(300)
        onLogOutput("[*] Fetching archive from cloud registry...\r\n")

        val steps = listOf(
            "Downloading rootfs Tarball...",
            "Verifying SHA256 checksum...",
            "Creating container directories...",
            "Extracting rootfs stage3 (this may take a few seconds)...",
            "Configuring networking dns (/etc/resolv.conf)...",
            "Setting up loopback interface...",
            "Writing OS metadata (/etc/os-release)...",
            "Finalizing container bootstrap..."
        )

        for (i in steps.indices) {
            onLogOutput("\u001B[36m[+] [${i + 1}/${steps.size}] ${steps[i]}\u001B[0m\r\n")
            // Update progress
            _distros.value = _distros.value.map {
                if (it.id == distroId) it.copy(installProgress = (i + 1).toFloat() / steps.size) else it
            }
            delay(400)
        }

        // Write real directory files inside termux/distros/<distroId>/
        bootstrapDistroFilesystem(distroId)

        settingsManager.addInstalledDistro(distroId)
        _distros.value = _distros.value.map {
            if (it.id == distroId) it.copy(isInstalling = false, isInstalled = true, installProgress = 1.0f) else it
        }
        refreshDistros()

        onLogOutput("\u001B[32m[!] proot-distro '$distroId' successfully bootstrapped!\u001B[0m\r\n")
        onLogOutput("[*] Type 'proot-distro login $distroId' or click 'Launch' to boot into the bash prompt.\r\n\r\n")
    }

    private fun bootstrapDistroFilesystem(distroId: String) {
        val distroDir = File(terminalSession.distrosDir, distroId)
        val etcDir = File(distroDir, "etc")
        val homeDir = File(distroDir, "home")
        val usrBinDir = File(distroDir, "usr/bin")

        if (!etcDir.exists()) etcDir.mkdirs()
        if (!homeDir.exists()) homeDir.mkdirs()
        if (!usrBinDir.exists()) usrBinDir.mkdirs()

        // Create an os-release file
        val osReleaseFile = File(etcDir, "os-release")
        val content = when (distroId) {
            "ubuntu" -> """
                NAME="Ubuntu"
                VERSION="22.04.2 LTS (Jammy Jellyfish)"
                ID=ubuntu
                ID_LIKE=debian
                PRETTY_NAME="Ubuntu 22.04.2 LTS"
                VERSION_ID="22.04"
                HOME_URL="https://ubuntu.com/"
                BUG_REPORT_URL="https://bugs.launchpad.net/ubuntu/"
            """.trimIndent()
            "alpine" -> """
                NAME="Alpine Linux"
                ID=alpine
                PRETTY_NAME="Alpine Linux v3.18"
                VERSION_ID="3.18.2"
                HOME_URL="https://alpinelinux.org/"
                BUG_REPORT_URL="https://gitlab.alpinelinux.org/alpine/aports/-/issues"
            """.trimIndent()
            "arch" -> """
                NAME="Arch Linux"
                PRETTY_NAME="Arch Linux"
                ID=arch
                BUILD_ID=rolling
                HOME_URL="https://archlinux.org/"
                DOCUMENTATION_URL="https://wiki.archlinux.org/"
            """.trimIndent()
            "debian" -> """
                NAME="Debian GNU/Linux"
                VERSION_ID="12"
                VERSION="12 (bookworm)"
                ID=debian
                PRETTY_NAME="Debian GNU/Linux 12 (bookworm)"
                HOME_URL="https://www.debian.org/"
                SUPPORT_URL="https://www.debian.org/support"
            """.trimIndent()
            "kali" -> """
                NAME="Kali GNU/Linux"
                ID=kali
                ID_LIKE=debian
                PRETTY_NAME="Kali GNU/Linux Rolling"
                VERSION_ID="2023.2"
                HOME_URL="https://www.kali.org/"
                BUG_REPORT_URL="https://bugs.kali.org/"
            """.trimIndent()
            else -> "NAME=\"Simulated Linux\"\nID=linux"
        }
        osReleaseFile.writeText(content)

        // Write a welcome text file in the distro home folder
        val readmeFile = File(homeDir, "README.txt")
        readmeFile.writeText("""
            ==================================================
             Welcome to your '$distroId' Container!
            ==================================================
            This container environment runs locally in your sandbox.
            
            Operating System: ${distroId.uppercase()}
            Virtual Directory: ${distroDir.absolutePath}
            
            You can use standard command-line tools to interact here.
            Use 'exit' to log out of the container.
            
            Have fun exploring!
        """.trimIndent())
    }

    fun launchDistro(distroId: String) {
        terminalSession.loginToDistro(distroId)
    }
}
