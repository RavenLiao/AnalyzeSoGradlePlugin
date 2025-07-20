package io.github.ravenliao.plugin

import java.io.File

object SoCollector {
    /**
     * 从目录中收集所有 SO 文件，返回 List<SoInfo>
     */
    fun collectSoFilesFromDirectory(directory: File, checkElf: (File) -> Triple<Boolean, String, Int?>): List<SoInfo> {
        if (!directory.isDirectory) return emptyList()

        val archMap = mutableMapOf<String, MutableList<SoDetail>>()
        val soNameSet = mutableSetOf<String>()
        val basePathLength = directory.absolutePath.length + 1

        directory.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".so") }
            .forEach { soFile ->
                val relativePath = soFile.absolutePath.substring(basePathLength).replace('\\', '/')
                val parts = relativePath.split('/')
                if (parts.size == 2) {
                    val arch = parts[0]
                    val fileName = parts[1]
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
            }
        // 按so逻辑名聚合
        return soNameSet.sorted().map { soName ->
            val archMapForSo = archMap.filterValues { list -> list.any { it.fileName == soName } }
                .mapValues { (_, list) -> list.filter { it.fileName == soName } }
            SoInfo(soName, archMapForSo)
        }
    }
}
