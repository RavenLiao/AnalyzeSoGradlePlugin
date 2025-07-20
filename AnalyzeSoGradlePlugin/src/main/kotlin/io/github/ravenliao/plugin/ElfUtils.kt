package io.github.ravenliao.plugin

import java.io.File

object ElfUtils {
    /**
     * 检查so文件的ELF段对齐情况，返回(aligned, alignment, alignmentKb)
     */
    fun checkElfAlignmentWithKb(soFile: File): Triple<Boolean, String, Int?> {
        return try {
            val process = ProcessBuilder("objdump", "-p", soFile.absolutePath)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            val loadLine = output.lines().firstOrNull { it.trim().startsWith("LOAD") }
            val alignment = loadLine?.split(Regex("\\s+"))?.lastOrNull() ?: "unknown"
            val aligned = Regex("2\\*\\*(1[4-9]|[2-9][0-9]|[1-9][0-9]{2,})").matches(alignment)
            val alignmentKb = parseAlignmentKb(alignment)
            Triple(aligned, alignment, alignmentKb)
        } catch (e: Exception) {
            Triple(false, "error", null)
        }
    }

    /**
     * 解析如2**14为16KB, 2**12为4KB等
     */
    fun parseAlignmentKb(alignment: String): Int? {
        val regex = Regex("2\\*\\*(\\d+)")
        val match = regex.matchEntire(alignment)
        return if (match != null) {
            val exp = match.groupValues[1].toIntOrNull()
            if (exp != null) (1 shl exp) / 1024 else null
        } else null
    }
}
