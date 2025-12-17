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

        val archMap = mutableMapOf<String, MutableList<SoDetail>>()
        val soNameSet = mutableSetOf<String>()
        val basePathLength = directory.absolutePath.length + 1

        directory.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".so") }
            .forEach { soFile ->
                val relativePath = soFile.absolutePath.substring(basePathLength).replace('\\', '/')
                val parts = relativePath.split('/').filter { it.isNotEmpty() }
                val arch = parts.firstOrNull { it in knownAbis } ?: "unknown"
                val fileName = soFile.name

                soNameSet.add(fileName)
                val (aligned, alignment, alignmentKb) = checkElf(soFile)
                archMap.getOrPut(arch) { mutableListOf() }.add(
                    SoDetail(
                        fileName = fileName,
                        filePath = soFile.absolutePath,
                        aligned = aligned,
                        alignment = alignment,
                        alignmentKb = alignmentKb
                    )
                )
            }
        // 按so逻辑名聚合
        return soNameSet.sorted().map { soName ->
            val archMapForSo = archMap.filterValues { list -> list.any { it.fileName == soName } }
                .mapValues { (_, list) -> list.filter { it.fileName == soName } }
            SoInfo(soName, archMapForSo)
        }
    }
}
