package io.github.ravenliao.plugin

import java.io.File

object SoCollector {
    /**
     * 从目录中收集所有 SO 文件，返回 List<SoInfo>
     */
    fun collectSoFilesFromDirectory(directory: File, checkElf: (File) -> Triple<Boolean, String, Int?>): List<SoInfo> {
        if (!directory.isDirectory) return emptyList()

        val knownAbis = setOf(
            "armeabi-v7a",
            "arm64-v8a",
            "x86",
            "x86_64",
            "riscv64"
        )

        val bySoName = mutableMapOf<String, MutableMap<String, MutableList<SoDetail>>>()

        val basePath = directory.toPath()
        directory.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".so") }
            .forEach { soFile ->
                val relativePath = try {
                    basePath.relativize(soFile.toPath()).toString().replace('\\', '/')
                } catch (_: Exception) {
                    soFile.absolutePath.replace('\\', '/')
                }
                val parts = relativePath.split('/').filter { it.isNotEmpty() }
                val arch = parts.firstOrNull { it in knownAbis } ?: "unknown"
                val fileName = soFile.name

                val (aligned, alignment, alignmentKb) = checkElf(soFile)
                bySoName
                    .getOrPut(fileName) { mutableMapOf() }
                    .getOrPut(arch) { mutableListOf() }
                    .add(
                        SoDetail(
                            fileName = fileName,
                            filePath = soFile.absolutePath,
                            aligned = aligned,
                            alignment = alignment,
                            alignmentKb = alignmentKb
                        )
                    )
            }

        return bySoName
            .toSortedMap()
            .map { (soName, archMap) ->
                val archFinal = archMap.toSortedMap().mapValues { (_, list) -> list.toList() }
                SoInfo(soName, archFinal)
            }
    }
}
