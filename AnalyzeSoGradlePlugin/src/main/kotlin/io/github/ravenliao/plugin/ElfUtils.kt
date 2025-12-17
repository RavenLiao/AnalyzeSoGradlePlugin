package io.github.ravenliao.plugin

import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

object ElfUtils {
    /**
     * 检查so文件的ELF段对齐情况，返回(aligned, alignment, alignmentKb)
     */
    fun checkElfAlignmentWithKb(soFile: File): Triple<Boolean, String, Int?> {
        return checkElfAlignmentWithKb(soFile, "objdump")
    }

    fun checkElfAlignmentWithKb(soFile: File, objdumpPath: String): Triple<Boolean, String, Int?> {
        return try {
            val process = ProcessBuilder(objdumpPath, "-p", soFile.absolutePath)
                .redirectErrorStream(true)
                .start()

            val outputRef = AtomicReference<String>("")
            val readerThread = thread(start = true, isDaemon = true) {
                runCatching {
                    process.inputStream.bufferedReader().use { it.readText() }
                }.onSuccess { outputRef.set(it) }
            }

            val finished = process.waitFor(5, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                readerThread.join(200)
                return Triple(false, "timeout", null)
            }

            readerThread.join(1000)
            val exitCode = runCatching { process.exitValue() }.getOrNull()
            if (exitCode == null || exitCode != 0) {
                return Triple(false, "error", null)
            }

            val output = outputRef.get()
            val loadLine = output.lineSequence().firstOrNull { it.trim().startsWith("LOAD") }
            val alignment = loadLine?.trim()?.split(Regex("\\s+"))?.lastOrNull() ?: "unknown"
            val alignmentKb = parseAlignmentKb(alignment)
            val aligned = alignmentKb?.let { it >= 16 } == true
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
            if (exp != null && exp in 0..30) (1 shl exp) / 1024 else null
        } else null
    }
}
