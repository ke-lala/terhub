package com.example.store

import android.content.Context
import com.example.settings.SettingsManager
import com.example.terminal.TerminalSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class PackageItem(
    val id: String,
    val name: String,
    val version: String,
    val category: String,
    val description: String,
    val size: String,
    val icon: String, // e.g. "terminal", "code", "utils", "game", "security", "media"
    val isInstalled: Boolean = false,
    val sourceName: String = "官方源"
)

data class CustomRepo(
    val name: String,
    val url: String,
    val packages: List<PackageItem>
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

    private val _customRepos = MutableStateFlow<List<CustomRepo>>(emptyList())
    val customRepos: StateFlow<List<CustomRepo>> = _customRepos

    // Static list of available packages in our customized store
    private val availablePackages = listOf(
        PackageItem("git", "Git", "2.41.0", "开发环境", "快速、可扩展、分布式的版本控制系统。", "1.4 MB", "code"),
        PackageItem("python", "Python", "3.11.4", "开发环境", "新一代面向对象解释型高级程序设计语言。", "12.3 MB", "code"),
        PackageItem("htop", "Htop", "3.2.2", "系统工具", "一个交互式的进程监控器和系统监视器。", "235 kB", "utils"),
        PackageItem("cmatrix", "Cmatrix", "2.6.3", "休闲娱乐", "模拟黑客帝国（Matrix）数字雨屏幕保护程序。", "54 kB", "game"),
        PackageItem("neofetch", "Neofetch", "7.1.0", "系统工具", "快速、高度可定制的系统信息命令行展示脚本。", "15 kB", "terminal", isInstalled = true),
        PackageItem("cowsay", "Cowsay", "3.0.4", "休闲娱乐", "能够生成一头说出/想出指定文字的 ASCII 字符牛的趣味工具。", "12 kB", "game", isInstalled = true),
        PackageItem("tree", "Tree", "2.1.1", "系统工具", "以树状格式递归地列出目录内容的小工具。", "46 kB", "utils"),
        PackageItem("nano", "Nano", "7.2.1", "系统工具", "轻量且用户友好的、基于键盘快捷键的终端文本编辑器。", "180 kB", "utils"),
        PackageItem("curl", "Curl", "8.2.1", "系统工具", "利用 URL 语法支持在命令行下进行文件传输的工具。", "350 kB", "utils"),
        PackageItem("sl", "SL (Steam Locomotive)", "5.02", "休闲娱乐", "当你将 ls 误输入为 sl 时，会驶过一辆可爱的蒸汽火车。", "20 kB", "game", isInstalled = true)
    )

    // Pre-configured recommended repositories that users can add in one click
    val recommendedRepos = listOf(
        CustomRepo(
            name = "Kali 安全渗透工具源",
            url = "https://kali.org/packages.json",
            packages = listOf(
                PackageItem("nmap", "Nmap Scanner", "7.94", "安全评估", "网络探测和安全 / 端口漏洞扫描工具。", "5.2 MB", "security", sourceName = "Kali 安全渗透工具源"),
                PackageItem("sqlmap", "Sqlmap Engine", "1.7.8", "安全评估", "自动化的 SQL 注入与数据库漏洞渗透测试工具。", "8.1 MB", "security", sourceName = "Kali 安全渗透工具源"),
                PackageItem("hydra", "Hydra Cracker", "9.5", "安全评估", "并行的网络服务登录密码爆破/审计工具。", "1.1 MB", "security", sourceName = "Kali 安全渗透工具源")
            )
        ),
        CustomRepo(
            name = "多媒体与创意媒体源",
            url = "https://media.org/packages.json",
            packages = listOf(
                PackageItem("ffmpeg", "FFmpeg CLI", "6.0", "多媒体", "强大的、跨平台音视频转码、剪辑和处理核心工具。", "24.5 MB", "media", sourceName = "多媒体与创意媒体源"),
                PackageItem("imagemagick", "ImageMagick", "7.1.1", "多媒体", "在命令行创建、编辑、合成、缩放或转换位图图像的工具。", "12.8 MB", "media", sourceName = "多媒体与创意媒体源")
            )
        ),
        CustomRepo(
            name = "极客生产力效率源",
            url = "https://geektools.org/packages.json",
            packages = listOf(
                PackageItem("tmux", "Tmux Multiplexer", "3.3a", "系统工具", "终端复用器，允许在单个屏幕上管理多个终端会话。", "340 kB", "terminal", sourceName = "极客生产力效率源"),
                PackageItem("vim", "Vim Editor", "9.0", "开发环境", "无处不在的高级、高度可定制的控制台文本编辑器。", "4.6 MB", "code", sourceName = "极客生产力效率源"),
                PackageItem("ranger", "Ranger Explorer", "1.9.3", "系统工具", "具有 vi 键绑定的控制台控制，带多栏文件预览管理器。", "120 kB", "terminal", sourceName = "极客生产力效率源")
            )
        )
    )

    init {
        loadCustomRepos()
        refreshPackages()
    }

    fun setPackageManager(useApt: Boolean) {
        _isAptSelected.value = useApt
    }

    private fun loadCustomRepos() {
        try {
            val jsonStr = settingsManager.getCustomSourcesJson()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<CustomRepo>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.getString("name")
                val url = obj.getString("url")
                val pkgsArray = obj.getJSONArray("packages")
                val pkgs = mutableListOf<PackageItem>()
                for (j in 0 until pkgsArray.length()) {
                    val pObj = pkgsArray.getJSONObject(j)
                    pkgs.add(
                        PackageItem(
                            id = pObj.getString("id"),
                            name = pObj.getString("name"),
                            version = pObj.getString("version"),
                            category = pObj.getString("category"),
                            description = pObj.getString("description"),
                            size = pObj.getString("size"),
                            icon = pObj.getString("icon"),
                            sourceName = name
                        )
                    )
                }
                list.add(CustomRepo(name, url, pkgs))
            }
            _customRepos.value = list
        } catch (e: Exception) {
            e.printStackTrace()
            _customRepos.value = emptyList()
        }
    }

    private fun saveCustomRepos(repos: List<CustomRepo>) {
        try {
            val array = JSONArray()
            for (repo in repos) {
                val obj = JSONObject()
                obj.put("name", repo.name)
                obj.put("url", repo.url)
                val pkgsArray = JSONArray()
                for (p in repo.packages) {
                    val pObj = JSONObject()
                    pObj.put("id", p.id)
                    pObj.put("name", p.name)
                    pObj.put("version", p.version)
                    pObj.put("category", p.category)
                    pObj.put("description", p.description)
                    pObj.put("size", p.size)
                    pObj.put("icon", p.icon)
                    pkgsArray.put(pObj)
                }
                obj.put("packages", pkgsArray)
                array.put(obj)
            }
            settingsManager.saveCustomSourcesJson(array.toString())
            _customRepos.value = repos
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addCustomRepo(repo: CustomRepo): Boolean {
        val current = _customRepos.value.toMutableList()
        if (current.any { it.url == repo.url }) return false
        current.add(repo)
        saveCustomRepos(current)
        refreshPackages()
        return true
    }

    fun removeCustomRepo(url: String) {
        val current = _customRepos.value.filter { it.url != url }
        saveCustomRepos(current)
        refreshPackages()
    }

    fun refreshPackages() {
        val installed = settingsManager.getInstalledPackages()
        
        // Merge static packages and packages from all custom repos
        val allAvailable = mutableListOf<PackageItem>()
        allAvailable.addAll(availablePackages)
        for (repo in _customRepos.value) {
            allAvailable.addAll(repo.packages)
        }

        _packages.value = allAvailable.map { pkg ->
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
                echo "Server: TermuxHubAppServer/1.0"
                echo ""
                echo "Success! Hello from Termux Hub curl simulated parser!"
            """.trimIndent()
            "nmap" -> """
                #!/system/bin/sh
                echo -e "\u001B[31mStarting Nmap 7.94 ( https://nmap.org ) at $(date)"
                echo -e "Nmap scan report for localhost (127.0.0.1)"
                echo -e "Host is up (0.00016s latency)."
                echo -e "Not shown: 996 closed ports"
                echo -e "PORT     STATE SERVICE"
                echo -e "22/tcp   open  ssh"
                echo -e "80/tcp   open  http"
                echo -e "5901/tcp open  vnc-1"
                echo -e "8080/tcp open  http-proxy"
                echo -e "Nmap scan complete: 1 IP address scanned in 0.23 seconds\u001B[0m"
            """.trimIndent()
            "sqlmap" -> """
                #!/system/bin/sh
                echo -e "\u001B[33m    ___"
                echo -e "   __H__"
                echo -e "  (___)_   ___ ___  __ _ _ _ _"
                echo -e "  |_ -| . | . | . |/ _\` | ' ' |"
                echo -e "  |___|_  |  _|  _|\__,_|_|_|_|"
                echo -e "        |_|_| |_|   http://sqlmap.org\u001B[0m"
                echo -e ""
                echo -e "[*] starting at $(date +%H:%M:%S)"
                echo -e "[INFO] testing connection to the target URL"
                echo -e "[INFO] checking if the target URL is stable"
                echo -e "[INFO] heuristic (basic) test shows that the back-end DBMS is 'MySQL'"
                echo -e "[INFO] target URL appears to be SQL injectable!"
                echo -e "[INFO] fetching database banner..."
                echo -e "DBMS: MySQL >= 5.6 (Community Server)"
            """.trimIndent()
            "hydra" -> """
                #!/system/bin/sh
                echo -e "\u001B[35mHydra v9.5-dev (c) 2023 by van Hauser/THC - for legal purposes only"
                echo -e "Hydra starting on port 22/ssh against target: localhost"
                echo -e "[DATA] max 16 tasks, ssh service, trying admin credentials..."
                echo -e "[22][ssh] host: localhost   login: admin   password: admin - FAILED"
                echo -e "[22][ssh] host: localhost   login: root    password: password - FAILED"
                echo -e "[22][ssh] host: localhost   login: root    password: 123456 - \u001B[32mSUCCESS\u001B[0m"
                echo -e "1 of 1 target successfully completed, 1 valid password found\u001B[0m"
            """.trimIndent()
            "ffmpeg" -> """
                #!/system/bin/sh
                echo -e "ffmpeg version 6.0 Copyright (c) 2000-2023 the FFmpeg developers"
                echo -e "built with clang version 16.0.4"
                echo -e "configuration: --enable-gpl --enable-libmp3lame --enable-libx264"
                echo -e "Input #0, mp3, from 'soundtrack.mp3':"
                echo -e "  Duration: 00:03:45.12, start: 0.025057, bitrate: 320 kb/s"
                echo -e "  Stream #0:0: Audio: mp3, 44100 Hz, stereo, fltp, 320 kb/s"
                echo -e "Output #0, wav, to 'soundtrack.wav':"
                echo -e "  Stream #0:0: Audio: pcm_s16le, 44100 Hz, stereo, s16, 1411 kb/s"
                echo -e "[wav @ 0x55bc121] size=38712KB time=00:03:45.12 bitrate=1411kb/s"
                echo -e "\u001B[32mAudio transcoding successfully completed!\u001B[0m"
            """.trimIndent()
            "imagemagick" -> """
                #!/system/bin/sh
                echo -e "ImageMagick 7.1.1-12 Q16-HDRI x86_64 https://imagemagick.org"
                echo -e "Command: convert input.jpg -resize 1024x768 wallpaper.png"
                echo -e "\u001B[32mSuccessfully resized and converted input.jpg (2.4MB) to wallpaper.png (1.1MB)!\u001B[0m"
            """.trimIndent()
            "tmux" -> """
                #!/system/bin/sh
                echo -e "\u001B[30;42m[0] 0:bash*                                            \"localhost\" 12:46 29-Jun-26\u001B[0m"
                echo -e "=========================================================================="
                echo -e "  This is a simulated tmux terminal multiplexer session."
                echo -e "  Use 'Ctrl+B' followed by '%' to split vertically, or '\"' horizontally."
                echo -e "  Type 'exit' to return to your primary shell."
                echo -e "=========================================================================="
            """.trimIndent()
            "vim" -> """
                #!/system/bin/sh
                echo -e "\u001B[34m~                                                                           "
                echo -e "~                               VIM - Vi IMproved                           "
                echo -e "~                                                                           "
                echo -e "~                                version 9.0                                "
                echo -e "~                            by Bram Moolenaar et al.                       "
                echo -e "~                  Vim is open source and freely distributable              "
                echo -e "~                                                                           "
                echo -e "~                     type  :help<Enter>            to get help             "
                echo -e "~                     type  :q<Enter>               to exit                 "
                echo -e "~                     type  :help version9<Enter>   for version info        "
                echo -e "~                                                                           \u001B[0m"
            """.trimIndent()
            "ranger" -> """
                #!/system/bin/sh
                echo -e "\u001B[34m ranger 1.9.3 ------ Files in: ${terminalSession.homeDir.absolutePath} -----\u001B[0m"
                echo -e " \u001B[32mDocuments/\u001B[0m          notes.md (420B)            [1/3] 100%"
                echo -e " \u001B[32mDownloads/\u001B[0m          setup.sh (1.2KB)"
                echo -e " main.py (2.1KB)"
                echo -e "--------------------------------------------------------------------------"
                echo -e " File: main.py   Size: 2.1KB   Modified: 2026-06-29 12:00:00"
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

