package com.example.store

import android.content.Context
import com.example.settings.SettingsManager
import com.example.terminal.TerminalSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

data class PackageItem(
    val id: String,
    val name: String,
    val version: String,
    val category: String,
    val description: String,
    val size: String,
    val icon: String, // e.g. "terminal", "code", "utils", "game"
    val isInstalled: Boolean = false
)

class PackageStoreManager(
    private val context: Context,
    private val settingsManager: SettingsManager,
    private val terminalSession: TerminalSession
) {
    private val _packages = MutableStateFlow<List<PackageItem>>(emptyList())
    val packages: StateFlow<List<PackageItem>> = _packages

    private val _isAptSelected = MutableStateFlow(true) // true for apt, false for pkg
    val isAptSelected: StateFlow<Boolean> = _isAptSelected

    private val _installingPackageId = MutableStateFlow<String?>(null)
    val installingPackageId: StateFlow<String?> = _installingPackageId

    private val _installProgress = MutableStateFlow(0f)
    val installProgress: StateFlow<Float> = _installProgress

    // Static list of available packages in our customized store
    private val availablePackages = listOf(
        PackageItem("git", "Git", "2.41.0", "Development", "Fast, scalable, distributed revision control system.", "1.4 MB", "code"),
        PackageItem("python", "Python", "3.11.4", "Development", "Next-generation programming language interpreter.", "12.3 MB", "code"),
        PackageItem("htop", "Htop", "3.2.2", "Utilities", "Interactive process viewer and system monitor.", "235 kB", "utils"),
        PackageItem("cmatrix", "Cmatrix", "2.6.3", "Fun & Toys", "Fabulous terminal screensaver based on Matrix digital rain.", "54 kB", "game"),
        PackageItem("neofetch", "Neofetch", "7.1.0", "Utilities", "A fast, highly customizable system info script.", "15 kB", "terminal", isInstalled = true),
        PackageItem("cowsay", "Cowsay", "3.0.4", "Fun & Toys", "Configurable speaking/thinking ASCII art cow generator.", "12 kB", "game", isInstalled = true),
        PackageItem("tree", "Tree", "2.1.1", "Utilities", "Recursive directory-listing program that produces a depth-indented listing.", "46 kB", "utils"),
        PackageItem("nano", "Nano", "7.2.1", "Utilities", "Small, user-friendly, keyboard-shortcut-driven terminal text editor.", "180 kB", "utils"),
        PackageItem("curl", "Curl", "8.2.1", "Utilities", "Command line tool for transferring data with URL syntax.", "350 kB", "utils"),
        PackageItem("sl", "SL (Steam Locomotive)", "5.02", "Fun & Toys", "Steam Locomotive animation when you accidentally type sl.", "20 kB", "game", isInstalled = true)
    )

    init {
        refreshPackages()
    }

    fun setPackageManager(useApt: Boolean) {
        _isAptSelected.value = useApt
    }

    fun refreshPackages() {
        val installed = settingsManager.getInstalledPackages()
        _packages.value = availablePackages.map { pkg ->
            pkg.copy(isInstalled = installed.contains(pkg.id) || pkg.id == "neofetch" || pkg.id == "cowsay" || pkg.id == "sl")
        }
    }

    suspend fun installPackage(pkgId: String) {
        if (_installingPackageId.value != null) return
        _installingPackageId.value = pkgId
        _installProgress.value = 0f

        // Simulate package download & unpack animation inside the store
        for (i in 1..10) {
            delay(150)
            _installProgress.value = i * 0.1f
        }

        // Add to installed registry
        settingsManager.addInstalledPackage(pkgId)

        // Write the corresponding executable command script in terminal
        val binDir = terminalSession.binDir
        if (!binDir.exists()) binDir.mkdirs()

        val scriptContent = when (pkgId) {
            "git" -> """
                #!/system/bin/sh
                echo -e "\u001B[36mTermux git version 2.41.0 (Branch Custom)\u001B[0m"
                echo "Usage: git [clone | init | add | commit | status | push | pull]"
                echo "Simulating repository inside ${terminalSession.homeDir.absolutePath}"
            """.trimIndent()
            "python" -> """
                #!/system/bin/sh
                echo -e "Python 3.11.4 (Termux Branch Sim)"
                echo "[GCC 12.2.0] on linux"
                echo "Type \"help\", \"copyright\", \"credits\" or \"license\" for more information."
                echo ">>> print('Hello from simulated Python environment!')"
                echo "Hello from simulated Python environment!"
                echo ">>> "
            """.trimIndent()
            "htop" -> """
                #!/system/bin/sh
                echo -e "\u001B[32m=== Simulated HTOP System Monitor ===\u001B[0m"
                echo "  CPU[|||||||||||||||||||||         52.4%]   Tasks: 42, 1 running"
                echo "  Mem[|||||||||||||||||             4.2GB/8GB] Load average: 0.45 0.32 0.15"
                echo "  Swp[                              0K/2GB]"
                echo ""
                echo -e "  \u001B[36mPID USER      PRI  NI  VIRT   RES   SHR S CPU% MEM%   TIME+  Command\u001B[0m"
                echo -e "  102 root       20   0  1.4G  410M  120M S  5.1  5.1  0:12.45 /system/bin/sh"
                echo -e "  185 termux     20   0  850M  125M   45M R  4.2  1.6  0:01.12 htop"
                echo -e "   14 system     20   0  2.5G  650M  210M S  2.0  8.1  4:12.33 system_server"
                echo "Press Ctrl+C or type exit to quit."
            """.trimIndent()
            "cmatrix" -> """
                #!/system/bin/sh
                echo -e "\u001B[32m0 1 0 0 1 0 1 0 1 1 0 0"
                echo "1 0 1 1 0 0 1 1 0 1 0 1"
                echo "0 1 0 0 1 0 1 0 1 1 0 0"
                echo "1 0 1 1 0 0 1 1 0 1 0 1"
                echo "0 1 0 0 1 0 1 0 1 1 0 0\u001B[0m"
                echo "Digital Rain complete!"
            """.trimIndent()
            "tree" -> """
                #!/system/bin/sh
                echo -e "\u001B[34m.\u001B[0m"
                echo -e "├── \u001B[34mDocuments\u001B[0m"
                echo -e "│   ├── report.txt"
                echo -e "│   └── notes.md"
                echo -e "├── \u001B[34mDownloads\u001B[0m"
                echo -e "│   └── setup.sh"
                echo -e "└── main.py"
                echo ""
                echo "2 directories, 4 files"
            """.trimIndent()
            "nano" -> """
                #!/system/bin/sh
                echo -e "\u001B[30;47m  GNU nano 7.2                  File: New File                     \u001B[0m"
                echo ""
                echo "  This is a simulated nano editor pane."
                echo "  We recommend clicking the graphic 'Files' tab of this app"
                echo "  to use our beautiful full-screen visual text editor instead!"
                echo ""
                echo -e "\u001B[30;47m^G Get Help  ^O WriteOut  ^R Read File ^Y Prev Page ^K Cut Text  ^C Cur Pos \u001B[0m"
                echo -e "\u001B[30;47m^X Exit      ^J Justify   ^W Where Is  ^V Next Page ^U UnCut Text^T To Spell\u001B[0m"
            """.trimIndent()
            "curl" -> """
                #!/system/bin/sh
                echo -e "\u001B[32mSending HTTP GET Request to: \u001B[0m"${"$"}1"
                echo "HTTP/1.1 200 OK"
                echo "Content-Type: text/plain; charset=UTF-8"
                echo "Server: TermuxBranchAppServer/1.0"
                echo ""
                echo "Success! Hello from Termux Branch curl simulated parser!"
            """.trimIndent()
            else -> """
                #!/system/bin/sh
                echo -e "\u001B[32mSuccessfully ran package $pkgId!\u001B[0m"
            """.trimIndent()
        }

        try {
            val file = File(binDir, pkgId)
            file.writeText(scriptContent)
            file.setExecutable(true, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Print to terminal console too
        val managerName = if (_isAptSelected.value) "apt" else "pkg"
        terminalSession.executeCommand("$managerName install $pkgId")

        _installingPackageId.value = null
        refreshPackages()
    }

    fun uninstallPackage(pkgId: String) {
        if (pkgId == "neofetch" || pkgId == "cowsay" || pkgId == "sl") return // built-in
        settingsManager.removeInstalledPackage(pkgId)

        // Delete executable script file
        val file = File(terminalSession.binDir, pkgId)
        if (file.exists()) file.delete()

        terminalSession.executeCommand("echo -e \"\\u001B[33m[*] Package $pkgId uninstalled!\\u001B[0m\"")
        refreshPackages()
    }
}
