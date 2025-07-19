package io.github.ravenliao.plugin

import kotlinx.serialization.Serializable


@Serializable
data class ModuleSoInfo(
    val moduleName: String,
    val soFiles: List<SoFileInfo>,
    val modulePath: String
)

@Serializable
data class SoFileInfo(
    val name: String,
    val architectures: List<String>
)