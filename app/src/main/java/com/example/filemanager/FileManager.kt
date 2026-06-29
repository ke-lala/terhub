package com.example.filemanager

import android.util.Log
import com.example.terminal.TerminalSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: String,
    val lastModified: String,
    val extension: String
)

class FileManager(private val terminalSession: TerminalSession) {

    private val _currentDirectory = MutableStateFlow<File>(terminalSession.homeDir)
    val currentDirectory: StateFlow<File> = _currentDirectory

    private val _filesList = MutableStateFlow<List<FileItem>>(emptyList())
    val filesList: StateFlow<List<FileItem>> = _filesList

    // Editor States
    private val _editingFile = MutableStateFlow<File?>(null)
    val editingFile: StateFlow<File?> = _editingFile

    private val _editorContent = MutableStateFlow("")
    val editorContent: StateFlow<String> = _editorContent

    init {
        refreshFiles()
    }

    fun navigateTo(directory: File) {
        if (directory.exists() && directory.isDirectory) {
            _currentDirectory.value = directory
            refreshFiles()
        }
    }

    fun navigateUp(): Boolean {
        val current = _currentDirectory.value
        val root = terminalSession.termuxRootDir // Allow navigating up to termux root but not outside
        if (current.absolutePath != root.absolutePath && current.parentFile != null) {
            _currentDirectory.value = current.parentFile!!
            refreshFiles()
            return true
        }
        return false
    }

    fun refreshFiles() {
        val dir = _currentDirectory.value
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val files = dir.listFiles()?.map { file ->
            val sizeStr = if (file.isDirectory) {
                val count = file.listFiles()?.size ?: 0
                "$count items"
            } else {
                formatFileSize(file.length())
            }

            FileItem(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                size = sizeStr,
                lastModified = dateFormat.format(Date(file.lastModified())),
                extension = file.extension.lowercase()
            )
        }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()

        _filesList.value = files
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
        val pre = "KMGTPE"[exp - 1]
        return String.format(Locale.getDefault(), "%.1f %cB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
    }

    fun createFolder(name: String): Boolean {
        if (name.isBlank()) return false
        val newFolder = File(_currentDirectory.value, name)
        val success = newFolder.mkdirs()
        if (success) {
            refreshFiles()
            terminalSession.executeCommand("echo -e \"\\u001B[32m[*] File Manager created directory: $name\\u001B[0m\"")
        }
        return success
    }

    fun createFile(name: String, initialContent: String = ""): Boolean {
        if (name.isBlank()) return false
        val newFile = File(_currentDirectory.value, name)
        return try {
            val success = newFile.createNewFile()
            if (success) {
                if (initialContent.isNotEmpty()) {
                    newFile.writeText(initialContent)
                }
                refreshFiles()
                terminalSession.executeCommand("echo -e \"\\u001B[32m[*] File Manager created file: $name\\u001B[0m\"")
            }
            success
        } catch (e: Exception) {
            Log.e("FileManager", "Error creating file", e)
            false
        }
    }

    fun openFileForEditing(fileItem: FileItem) {
        val file = File(fileItem.path)
        if (file.exists() && file.isFile) {
            _editingFile.value = file
            _editorContent.value = file.readText()
        }
    }

    fun updateEditorContent(content: String) {
        _editorContent.value = content
    }

    fun saveEditingFile(): Boolean {
        val file = _editingFile.value ?: return false
        return try {
            file.writeText(_editorContent.value)
            refreshFiles()
            terminalSession.executeCommand("echo -e \"\\u001B[32m[*] File Manager saved file: ${file.name}\\u001B[0m\"")
            true
        } catch (e: Exception) {
            Log.e("FileManager", "Error saving file", e)
            false
        }
    }

    fun closeEditor() {
        _editingFile.value = null
        _editorContent.value = ""
    }

    fun deleteItem(fileItem: FileItem): Boolean {
        val file = File(fileItem.path)
        return if (file.exists()) {
            val success = file.deleteRecursively()
            if (success) {
                refreshFiles()
                terminalSession.executeCommand("echo -e \"\\u001B[33m[*] File Manager deleted: ${fileItem.name}\\u001B[0m\"")
            }
            success
        } else {
            false
        }
    }
}
