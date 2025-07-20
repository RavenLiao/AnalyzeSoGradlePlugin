package io.github.ravenliao.plugin

import kotlinx.serialization.Serializable


@Serializable
/**
 * 单个模块的so信息
 */
data class ModuleSoInfo(
    val moduleName: String,
    val soFiles: List<SoInfo>,
    val modulePath: String
)

@Serializable
/**
 * 支持多架构、每个架构下可有多个so文件及其详细信息
 */
data class SoInfo(
    val name: String, // 逻辑名
    val architectures: Map<String, List<SoDetail>> // arch -> so文件详情列表
)

@Serializable
/**
 * 单个so文件的详细信息
 */
data class SoDetail(
    val fileName: String, // so文件名
    val filePath: String, // so文件路径
    val aligned: Boolean, // 是否对齐
    val alignment: String, // 对齐值（如2**14、2**12等）
    val alignmentKb: Int? // 解析后的页面大小，单位KB
)