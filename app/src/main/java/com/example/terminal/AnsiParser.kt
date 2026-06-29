package com.example.terminal

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.example.settings.SettingsManager

object AnsiParser {

    fun parse(text: String, theme: SettingsManager.TerminalTheme): AnnotatedString {
        val builder = AnnotatedString.Builder()
        var i = 0
        val length = text.length

        var currentForeground = theme.foreground
        var currentBold = false

        while (i < length) {
            val char = text[i]
            if (char == '\u001B' || char == '\u001b') {
                // Escape sequence detected
                if (i + 1 < length && text[i + 1] == '[') {
                    // Start of ANSI parameters
                    i += 2
                    val paramBuilder = StringBuilder()
                    while (i < length && text[i] != 'm') {
                        paramBuilder.append(text[i])
                        i++
                    }
                    if (i < length && text[i] == 'm') {
                        // We reached the end of the escape sequence 'm'
                        i++ // Skip 'm'
                        val params = paramBuilder.toString().split(';')
                        for (p in params) {
                            val code = p.toIntOrNull() ?: 0
                            when (code) {
                                0 -> { // Reset
                                    currentForeground = theme.foreground
                                    currentBold = false
                                }
                                1 -> { // Bold
                                    currentBold = true
                                }
                                in 30..37 -> { // Standard foreground
                                    val colorIndex = code - 30
                                    currentForeground = theme.ansiColors.getOrNull(colorIndex) ?: theme.foreground
                                }
                                in 90..97 -> { // Bright foreground (we can map to bright colors, or standard color + bold)
                                    val colorIndex = code - 90
                                    currentForeground = theme.ansiColors.getOrNull(colorIndex) ?: theme.foreground
                                }
                                39 -> { // Default foreground
                                    currentForeground = theme.foreground
                                }
                            }
                        }
                        continue
                    }
                }
            }

            // Normal character
            // We apply the style and append
            builder.withStyle(
                SpanStyle(
                    color = currentForeground,
                    fontWeight = if (currentBold) FontWeight.Bold else FontWeight.Normal
                )
            ) {
                append(char)
            }
            i++
        }

        return builder.toAnnotatedString()
    }
}
