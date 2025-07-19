package io.github.ravenliao.plugin

import java.io.Serializable

/**
 * 表示包含 SO 文件的模块信息
 */
data class SoFileInfo(
    val fileName: String,
    val architectures: List<String>
) : Serializable

data class ModuleSoInfo(
    val moduleName: String,
    val soFiles: List<SoFileInfo>,
    val modulePath: String
) : Serializable