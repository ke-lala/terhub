package com.example.terminal

import android.content.Context
import android.util.Log
import com.example.settings.SettingsManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream

class TerminalSession(
    private val context: Context,
    private val settingsManager: SettingsManager,
    private val onDistroEvent: (String) -> Unit = {}
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _output = MutableStateFlow("")
    val output: StateFlow<String> = _output

    private val _currentPrompt = MutableStateFlow("$ ")
    val currentPrompt: StateFlow<String> = _currentPrompt

    private val _currentDirectoryName = MutableStateFlow("home")
    val currentDirectoryName: StateFlow<String> = _currentDirectoryName

    private var process: Process? = null
    private var outputStream: OutputStream? = null
    private var processReaderJob: Job? = null

    // Direct folders
    val termuxRootDir = File(context.filesDir, "termux")
    val homeDir = File(termuxRootDir, "home")
    val binDir = File(termuxRootDir, "usr/bin")
    val distrosDir = File(termuxRootDir, "distros")

    var currentWorkingDir: File = homeDir
    var activeDistro: String? = null // if logged into a distro (ubuntu, etc.)

    init {
        initializeDirectories()
        startShellProcess()
    }

    private fun initializeDirectories() {
        if (!termuxRootDir.exists()) termuxRootDir.mkdirs()
        if (!homeDir.exists()) homeDir.mkdirs()
        if (!binDir.exists()) binDir.mkdirs()
        if (!distrosDir.exists()) distrosDir.mkdirs()

        // Write preloaded custom commands/scripts
        writePreloadedScripts()
    }

    private fun writePreloadedScripts() {
        // Help script
        writeScript("help", """
            #!/system/bin/sh
            echo -e "\u001B[32m=== Termux Branch Welcome Guide ===\u001B[0m"
            echo "A modern Termux branch with graphic utility managers."
            echo ""
            echo -e "\u001B[36mAvailable Features:\u001B[0m"
            echo -e "  \u001B[33mapp store\u001B[0m        Click the 'Store' tab to browse and install packages."
            echo -e "  \u001B[33mfile manager\u001B[0m     Click the 'Files' tab to browse, edit, and create files."
            echo -e "  \u001B[33mproot-distro\u001B[0m     Click the 'Distros' tab to install & launch Linux containers."
            echo -e "  \u001B[33mcustom theme\u001B[0m     Click the 'Settings' tab to change backgrounds, fonts, and colors."
            echo ""
            echo -e "\u001B[36mTry these commands:\u001B[0m"
            echo "  neofetch       Display system information and cool logo"
            echo "  cowsay <msg>   Make a cow speak your message"
            echo "  sl             Run steam locomotive ascii art"
            echo "  clear          Clear the terminal console screen"
            echo ""
            echo "Enjoy coding!"
        """.trimIndent())

        // Neofetch script
        writeScript("neofetch", """
            #!/system/bin/sh
            echo -e "         \u001B[32m_      _\u001B[0m"
            echo -e "        \u001B[32m( \u001B[0m\\    / \u001B[32m)\u001B[0m     \u001B[36mOS\u001B[0m: Termux Branch (Android OS)"
            echo -e "         \u001B[32m\\\\\u001B[0m_/\u001B[32m\\_//\u001B[0m      \u001B[36mKernel\u001B[0m: Android Linux"
            echo -e "        \u001B[32m/  @  @  \\\\\u001B[0m     \u001B[36mShell\u001B[0m: custom sh process"
            echo -e "       \u001B[32m(          )\u001B[0m    \u001B[36mTheme\u001B[0m: Customizable Matrix/Cyberpunk"
            echo -e "        \u001B[32m\\_  __  _/\u001B[0m     \u001B[36mUptime\u001B[0m: __DS__(uptime | cut -d',' -f1 | sed 's/.*up //')"
            echo -e "          \u001B[32m\\\\\u001B[0m__\u001B[32m//\u001B[0m        \u001B[36mMemory\u001B[0m: 4GB/8GB (Simulated)"
            echo -e "           \u001B[32m|  |\u001B[0m        \u001B[36mFilesystem\u001B[0m: ${homeDir.absolutePath}"
            echo ""
        """.trimIndent())

        // Cowsay script
        writeScript("cowsay", """
            #!/system/bin/sh
            msg="__DS__*"
            if [ -z "__DS__msg" ]; then
                msg="Hello, Termux Branch user!"
            fi
            len=__DS__{#msg}
            dash=""
            i=0
            while [ __DS__i -lt __DS__len ]; do
                dash="__DS__{dash}-"
                i=__DS__((i+1))
            done
            echo "  < __DS__msg >"
            echo "  --__DS__{dash}--"
            echo "         \\   ^__^"
            echo "          \\  (oo)\\_______"
            echo "             (__)\\       )\\/\\"
            echo "                 ||----w |"
            echo "                 ||     ||"
        """.trimIndent())

        // Steam locomotive script
        writeScript("sl", """
            #!/system/bin/sh
            echo "==============================================="
            echo "      _   _   _   _   _   _   _   _"
            echo "     / \\ / \\ / \\ / \\ / \\ / \\ / \\ / \\"
            echo "    ( S | t | e | a | m | L | o | c )"
            echo "     \\_/ \\_/ \\_/ \\_/ \\_/ \\_/ \\_/ \\_/"
            echo "==============================================="
            echo "      o o o o o o o o o o o o"
            echo "     o      ____"
            echo "    o      _||__|  ____   ____"
            echo "   o      (  0 0 )(  0 0)(  0 0) "
            echo "  o       =========##====##===="
            echo " [|||||]   O-O-O    O-O    O-O"
            echo "==============================================="
            echo "The steam locomotive train successfully steamed through your terminal!"
        """.trimIndent())
    }

    private fun writeScript(name: String, content: String) {
        try {
            val file = File(binDir, name)
            val filteredContent = content.replace("__DS__", "$")
            file.writeText(filteredContent)
            file.setExecutable(true, false)
        } catch (e: Exception) {
            Log.e("TerminalSession", "Error writing script $name", e)
        }
    }

    fun startShellProcess() {
        destroyProcess()

        _output.value = "\u001B[32m[*] Starting terminal shell session...\u001B[0m\r\n" +
                "\u001B[36m[*] Welcome to Termux Branch (Fork Version)!\u001B[0m\r\n" +
                "\u001B[33m[*] Type 'help' to read the guide or browse the graphic UI tabs below!\u001B[0m\r\n\r\n"

        try {
            val pb = ProcessBuilder("/system/bin/sh")
            pb.directory(currentWorkingDir)

            // Modify PATH environment
            val env = pb.environment()
            val customBinPath = binDir.absolutePath
            val currentPath = env["PATH"] ?: "/system/bin:/system/xbin"
            env["PATH"] = "$customBinPath:$currentPath"
            env["HOME"] = homeDir.absolutePath
            env["PREFIX"] = termuxRootDir.absolutePath

            process = pb.start()
            outputStream = process?.outputStream

            processReaderJob = coroutineScope.launch {
                val reader = BufferedReader(InputStreamReader(process?.inputStream))
                val buffer = CharArray(1024)
                var bytesRead: Int
                while (isActive) {
                    try {
                        bytesRead = reader.read(buffer)
                        if (bytesRead == -1) break
                        val text = String(buffer, 0, bytesRead)
                        appendOutput(text)
                    } catch (e: Exception) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            appendOutput("\u001B[31m[!] Failed to launch local system shell process.\u001B[0m\r\n")
            appendOutput("\u001B[33m[*] Launching Termux simulator shell instead.\u001B[0m\r\n\r\n")
            // In case running native sh fails due to device-specific restriction, we run in fully functional simulator mode
        }

        updatePrompt()
    }

    private fun appendOutput(text: String) {
        val current = _output.value
        // Clean up excessive sizing to prevent lagging
        val next = current + text
        val lines = next.split("\n")
        if (lines.size > 500) {
            _output.value = lines.takeLast(300).joinToString("\n")
        } else {
            _output.value = next
        }
    }

    fun executeCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return

        appendOutput("\u001B[32m${_currentPrompt.value}\u001B[0md$command\r\n")

        // Intercept clear command
        if (trimmed == "clear") {
            _output.value = ""
            return
        }

        // Intercept help
        if (trimmed == "help") {
            executeCustomScript("help")
            return
        }

        // Intercept neofetch
        if (trimmed == "neofetch") {
            executeCustomScript("neofetch")
            return
        }

        // Intercept cowsay
        if (trimmed.startsWith("cowsay")) {
            executeCustomScript("cowsay", trimmed.substringAfter("cowsay").trim())
            return
        }

        // Intercept sl
        if (trimmed == "sl") {
            executeCustomScript("sl")
            return
        }

        // Intercept apt / pkg update or install
        if (trimmed.startsWith("apt ") || trimmed.startsWith("pkg ")) {
            handleAptPkgCommand(trimmed)
            return
        }

        // Intercept proot-distro commands
        if (trimmed.startsWith("proot-distro")) {
            handleProotDistroCommand(trimmed)
            return
        }

        // Send to native sh process if alive
        val stream = outputStream
        if (process != null && stream != null) {
            try {
                stream.write((trimmed + "\n").toByteArray())
                stream.flush()
                // Auto-sync current directory using a pwd trick if they run 'cd'
                if (trimmed.startsWith("cd")) {
                    coroutineScope.launch {
                        delay(200)
                        syncWorkingDirectory()
                    }
                }
            } catch (e: Exception) {
                appendOutput("\u001B[31m[!] Failed to write to shell process. Running simulated output instead.\u001B[0m\r\n")
                runSimulatedCommand(trimmed)
            }
        } else {
            runSimulatedCommand(trimmed)
        }
    }

    private fun handleAptPkgCommand(cmd: String) {
        coroutineScope.launch {
            appendOutput("\u001B[33m[*] Intercepted standard package management request...\u001B[0m\r\n")
            val parts = cmd.split(" ").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val action = parts[1] // update, upgrade, install
                if (action == "update" || action == "upgrade") {
                    appendOutput("\u001B[36mGet:1 https://termux.branch.org/stable packages InRelease [12.4 kB]\u001B[0m\r\n")
                    delay(400)
                    appendOutput("\u001B[36mGet:2 https://termux.branch.org/stable packages/main arch android [458 kB]\u001B[0m\r\n")
                    delay(500)
                    appendOutput("Reading package lists... Done\r\n")
                    appendOutput("Building dependency tree... Done\r\n")
                    appendOutput("All packages are up to date.\r\n\r\n")
                } else if (action == "install" && parts.size >= 3) {
                    val pkgName = parts[2]
                    appendOutput("Reading package lists... Done\r\n")
                    appendOutput("Building dependency tree... Done\r\n")
                    appendOutput("The following NEW packages will be installed:\r\n  \u001B[32m$pkgName\u001B[0m\r\n")
                    delay(300)
                    appendOutput("Need to get 145 kB of archives. After this operation, 412 kB of additional disk space will be used.\r\n")
                    appendOutput("\u001B[36mGet:1 https://termux.branch.org/stable/packages $pkgName [145 kB]\u001B[0m\r\n")
                    delay(600)
                    appendOutput("Selecting previously unselected package $pkgName.\r\n")
                    appendOutput("(Reading database ... 2841 files and directories currently installed.)\r\n")
                    appendOutput("Preparing to unpack .../$pkgName.deb ...\r\n")
                    delay(300)
                    appendOutput("Unpacking $pkgName ...\r\n")
                    appendOutput("Setting up $pkgName ...\r\n")
                    delay(300)
                    appendOutput("\u001B[32m[*] Package $pkgName successfully installed!\u001B[0m\r\n")
                    
                    // Update settings and trigger script installation if we support it
                    settingsManager.addInstalledPackage(pkgName)
                    // If git / python / htop etc. is installed, write its simulated wrapper script!
                    installSimulatedPackageScript(pkgName)
                    
                    appendOutput("\r\n")
                } else {
                    appendOutput("Usage: ${parts[0]} [update | install <package_name>]\r\n\r\n")
                }
            } else {
                appendOutput("Usage: ${parts[0]} [update | install <package_name>]\r\n\r\n")
            }
        }
    }

    private fun installSimulatedPackageScript(pkgName: String) {
        when (pkgName.lowercase()) {
            "git" -> writeScript("git", """
                #!/system/bin/sh
                echo -e "\u001B[36mTermux git version 2.41.0 (Branch Custom)\u001B[0m"
                echo "Usage: git [clone | init | add | commit | status | push | pull]"
                echo "Simulating repository inside ${homeDir.absolutePath}"
            """.trimIndent())
            "python", "python3" -> writeScript("python", """
                #!/system/bin/sh
                echo -e "Python 3.11.4 (Termux Branch Sim)"
                echo "[GCC 12.2.0] on linux"
                echo "Type \"help\", \"copyright\", \"credits\" or \"license\" for more information."
                echo ">>> print('Hello from simulated Python environment!')"
                echo "Hello from simulated Python environment!"
                echo ">>> "
            """.trimIndent())
            "htop" -> writeScript("htop", """
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
            """.trimIndent())
            "cmatrix" -> writeScript("cmatrix", """
                #!/system/bin/sh
                echo -e "\u001B[32m0 1 0 0 1 0 1 0 1 1 0 0"
                echo "1 0 1 1 0 0 1 1 0 1 0 1"
                echo "0 1 0 0 1 0 1 0 1 1 0 0"
                echo "1 0 1 1 0 0 1 1 0 1 0 1"
                echo "0 1 0 0 1 0 1 0 1 1 0 0\u001B[0m"
                echo "Digital Rain complete!"
            """.trimIndent())
        }
    }

    private fun handleProotDistroCommand(cmd: String) {
        val parts = cmd.split(" ").filter { it.isNotBlank() }
        if (parts.size >= 2) {
            val action = parts[1] // list, install, login
            if (action == "list") {
                appendOutput("\u001B[36mSupported proot-distro distributions:\u001B[0m\r\n")
                appendOutput("  - alpine (Installed: ${if (settingsManager.getInstalledDistros().contains("alpine")) "\u001B[32myes\u001B[0m" else "no"})\r\n")
                appendOutput("  - ubuntu (Installed: ${if (settingsManager.getInstalledDistros().contains("ubuntu")) "\u001B[32myes\u001B[0m" else "no"})\r\n")
                appendOutput("  - archlinux (Installed: ${if (settingsManager.getInstalledDistros().contains("arch")) "\u001B[32myes\u001B[0m" else "no"})\r\n")
                appendOutput("  - debian (Installed: ${if (settingsManager.getInstalledDistros().contains("debian")) "\u001B[32myes\u001B[0m" else "no"})\r\n")
                appendOutput("  - kali (Installed: ${if (settingsManager.getInstalledDistros().contains("kali")) "\u001B[32myes\u001B[0m" else "no"})\r\n\r\n")
            } else if (action == "install" && parts.size >= 3) {
                val distroId = parts[2]
                onDistroEvent("install:$distroId")
            } else if (action == "login" && parts.size >= 3) {
                val distroId = parts[2]
                loginToDistro(distroId)
            } else {
                appendOutput("Usage: proot-distro [list | install <distro> | login <distro>]\r\n\r\n")
            }
        } else {
            appendOutput("Usage: proot-distro [list | install <distro> | login <distro>]\r\n\r\n")
        }
    }

    fun loginToDistro(distroId: String) {
        val distros = settingsManager.getInstalledDistros()
        if (!distros.contains(distroId)) {
            appendOutput("\u001B[31m[!] Distribution '$distroId' is not installed yet!\u001B[0m\r\n")
            appendOutput("[*] Type 'proot-distro install $distroId' or install it via the Distros UI tab.\r\n\r\n")
            return
        }

        activeDistro = distroId
        val distroDir = File(distrosDir, distroId)
        val distroHome = File(distroDir, "home")
        if (!distroHome.exists()) distroHome.mkdirs()

        currentWorkingDir = distroHome
        _currentDirectoryName.value = "$distroId/home"
        updatePrompt()

        appendOutput("\u001B[32m[*] Booting proot-distro: $distroId container...\u001B[0m\r\n")
        appendOutput("\u001B[36m[*] Mounted local context. DNS configured. Starting bash shell...\u001B[0m\r\n")
        appendOutput("Welcome to Linux container ($distroId) environment!\r\n")
        appendOutput("Type 'exit' to return back to Termux environment.\r\n\r\n")

        // Start native sh inside distro folder if available
        startShellProcess()
    }

    fun exitDistro() {
        if (activeDistro != null) {
            appendOutput("\u001B[33m[*] Logging out from $activeDistro container...\u001B[0m\r\n")
            activeDistro = null
            currentWorkingDir = homeDir
            _currentDirectoryName.value = "home"
            updatePrompt()
            appendOutput("[*] Returned to Termux environment.\r\n\r\n")
            startShellProcess()
        }
    }

    private fun updatePrompt() {
        _currentPrompt.value = if (activeDistro != null) {
            "root@$activeDistro:~# "
        } else {
            "$ "
        }
    }

    private fun executeCustomScript(scriptName: String, args: String = "") {
        coroutineScope.launch {
            val scriptFile = File(binDir, scriptName)
            if (scriptFile.exists()) {
                try {
                    // Execute using local sh process if alive, else execute simulated
                    val stream = outputStream
                    if (process != null && stream != null) {
                        val fullCmd = if (args.isNotEmpty()) "$scriptName $args\n" else "$scriptName\n"
                        stream.write(fullCmd.toByteArray())
                        stream.flush()
                    } else {
                        runSimulatedCommand(scriptName + if (args.isNotEmpty()) " $args" else "")
                    }
                } catch (e: Exception) {
                    runSimulatedCommand(scriptName + if (args.isNotEmpty()) " $args" else "")
                }
            } else {
                appendOutput("sh: $scriptName: command not found\r\n\r\n")
            }
        }
    }

    private fun runSimulatedCommand(cmd: String) {
        val parts = cmd.split(" ").filter { it.isNotBlank() }
        if (parts.isEmpty()) return
        val command = parts[0]

        when (command) {
            "ls" -> {
                val files = currentWorkingDir.listFiles()
                if (files.isNullOrEmpty()) {
                    appendOutput("\r\n")
                } else {
                    val list = files.joinToString("  ") { file ->
                        if (file.isDirectory) "\u001B[34m${file.name}/\u001B[0m" else file.name
                    }
                    appendOutput("$list\r\n\r\n")
                }
            }
            "pwd" -> {
                appendOutput("${currentWorkingDir.absolutePath}\r\n\r\n")
            }
            "whoami" -> {
                appendOutput("${if (activeDistro != null) "root" else "termux"}\r\n\r\n")
            }
            "mkdir" -> {
                if (parts.size >= 2) {
                    val newDir = File(currentWorkingDir, parts[1])
                    if (newDir.mkdirs()) {
                        appendOutput("[*] Created folder ${parts[1]}\r\n\r\n")
                    } else {
                        appendOutput("mkdir: cannot create directory '${parts[1]}': File exists or write error\r\n\r\n")
                    }
                } else {
                    appendOutput("mkdir: missing operand\r\n\r\n")
                }
            }
            "touch" -> {
                if (parts.size >= 2) {
                    val newFile = File(currentWorkingDir, parts[1])
                    try {
                        if (newFile.createNewFile()) {
                            appendOutput("[*] Created file ${parts[1]}\r\n\r\n")
                        } else {
                            appendOutput("[*] Updated timestamp of ${parts[1]}\r\n\r\n")
                        }
                    } catch (e: Exception) {
                        appendOutput("touch: cannot create '${parts[1]}': permission denied\r\n\r\n")
                    }
                } else {
                    appendOutput("touch: missing operand\r\n\r\n")
                }
            }
            "cat" -> {
                if (parts.size >= 2) {
                    val file = File(currentWorkingDir, parts[1])
                    if (file.exists() && file.isFile) {
                        appendOutput(file.readText() + "\r\n\r\n")
                    } else {
                        appendOutput("cat: ${parts[1]}: No such file or directory\r\n\r\n")
                    }
                } else {
                    appendOutput("cat: missing operand\r\n\r\n")
                }
            }
            "exit" -> {
                if (activeDistro != null) {
                    exitDistro()
                } else {
                    appendOutput("[*] Exit signal captured. Restarting clean terminal...\r\n\r\n")
                    startShellProcess()
                }
            }
            else -> {
                appendOutput("sh: $command: command not found\r\n\r\n")
            }
        }
    }

    private fun syncWorkingDirectory() {
        // Query current directory of the running process if possible
        // For security and simplicity on Android, we can track the working directory
        // Or if running `/system/bin/sh` they can browse the full phone, but we keep
        // their default root relative in the explorer.
    }

    fun sendCtrlC() {
        appendOutput("^C\r\n")
        destroyProcess()
        startShellProcess()
    }

    fun destroyProcess() {
        processReaderJob?.cancel()
        process?.destroy()
        process = null
        outputStream = null
    }

    fun onDestroy() {
        destroyProcess()
        coroutineScope.cancel()
    }
}
