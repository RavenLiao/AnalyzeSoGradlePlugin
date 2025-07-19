package io.github.ravenliao.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.io.Serializable

/**
 * 表示包含 SO 文件的模块信息
 */
data class ModuleSoInfo(
    val moduleName: String,
    val soFiles: List<String>,
    val modulePath: String
) : Serializable

/**
 * 分析 Android 项目中 SO 文件依赖的 Gradle Task
 *
 * 此任务会扫描指定构建变体的运行时依赖，找到所有包含原生库(.so文件)的模块，
 * 并生成详细的分析报告。
 */
@DisableCachingByDefault(because = "Analysis task that should run every time")
abstract class AnalyzeSoTask : DefaultTask() {

    /**
     * 要分析的构建变体名称（如 debug, release）
     */
    @get:Input
    abstract val variantName: Property<String>

    /**
     * 分析结果的输出文件
     */
    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    /**
     * 分析到的模块信息列表（用于构建缓存和增量构建）
     */
    @get:Internal
    abstract val analyzedModules: ListProperty<ModuleSoInfo>

    init {
        group = "analysis-so"
        description = "Analyzes SO files in Android project dependencies for the specified variant"

        // 设置默认输出文件
        reportFile.convention(
            project.layout.buildDirectory.file("reports/analyze-so/analyze-so-report.json")
        )

        // 强制任务每次都执行，不走缓存
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun executeAnalysis() {
        val variant = variantName.get()
        logger.lifecycle("Starting SO file analysis for variant: $variant")

        try {
            // 1. 获取配置
            val configuration = findConfiguration(variant)
            if (configuration == null) {
                logger.warn("Configuration for variant '$variant' not found. Skipping analysis.")
                writeEmptyReport()
                return
            }

            // 2. 分析 SO 文件
            val modules = analyzeNativeLibraries(configuration)

            // 3. 生成报告
            generateReport(modules)

            // 4. 输出统计信息
            logAnalysisResults(modules)

        } catch (exception: Exception) {
            logger.error("Failed to analyze SO files for variant '$variant'", exception)
            throw exception
        }
    }

    /**
     * 查找指定变体的配置
     */
    private fun findConfiguration(variant: String): Configuration? {
        val configurationName = "${variant}RuntimeClasspath"
        return project.configurations.findByName(configurationName)
            ?: project.configurations.findByName("${variant}CompileClasspath")
    }

    /**
     * 分析配置中的原生库
     */
    private fun analyzeNativeLibraries(configuration: Configuration): List<ModuleSoInfo> {
        logger.info("Analyzing configuration: ${configuration.name}")

        return try {
            // 创建 ArtifactView 来获取 JNI 工件
            val artifactView = configuration.incoming.artifactView {
                attributes.attribute(
                    Attribute.of("artifactType", String::class.java),
                    "android-jni"
                )
            }

            // 处理每个工件
            artifactView.artifacts.artifacts.mapNotNull { artifact ->
                processArtifact(artifact)
            }.filter { it.soFiles.isNotEmpty() }

        } catch (exception: Exception) {
            logger.warn(
                "Error analyzing native libraries in configuration ${configuration.name}",
                exception
            )
            emptyList()
        }
    }

    /**
     * 处理单个工件
     */
    private fun processArtifact(artifact: org.gradle.api.artifacts.result.ResolvedArtifactResult): ModuleSoInfo? {
        return try {
            val artifactFile = artifact.file
            if (!artifactFile.exists()) {
                logger.debug("Artifact file does not exist: ${artifactFile.absolutePath}")
                return null
            }

            val moduleName = artifact.id.componentIdentifier.displayName
            val soFiles = if (artifactFile.isDirectory) {
                collectSoFilesFromDirectory(artifactFile)
            } else {
                emptyList()
            }

            if (soFiles.isEmpty()) {
                logger.debug("No SO files found in module: $moduleName")
                return null
            }

            ModuleSoInfo(
                moduleName = moduleName,
                soFiles = soFiles,
                modulePath = artifactFile.absolutePath
            )

        } catch (exception: Exception) {
            logger.warn("Failed to process artifact: ${artifact.id}", exception)
            null
        }
    }

    /**
     * 从目录中收集所有 SO 文件
     */
    private fun collectSoFilesFromDirectory(directory: File): List<String> {
        if (!directory.isDirectory) return emptyList()

        return try {
            val soFiles = mutableListOf<String>()
            val basePathLength = directory.absolutePath.length + 1 // +1 for path separator

            directory.walkTopDown()
                .filter { it.isFile && it.name.endsWith(".so") }
                .forEach { soFile ->
                    val relativePath = soFile.absolutePath.substring(basePathLength)
                        .replace('\\', '/') // 统一路径分隔符
                    soFiles.add(relativePath)
                }

            soFiles.sorted() // 保持一致的顺序
        } catch (exception: Exception) {
            logger.warn(
                "Error collecting SO files from directory: ${directory.absolutePath}",
                exception
            )
            emptyList()
        }
    }

    /**
     * 生成分析报告
     */
    private fun generateReport(modules: List<ModuleSoInfo>) {
        val outputFile = reportFile.get().asFile

        // 确保输出目录存在
        outputFile.parentFile?.mkdirs()

        val jsonContent = buildJsonReport(modules)
        outputFile.writeText(jsonContent, Charsets.UTF_8)

        logger.info("SO analysis report generated: ${outputFile.absolutePath}")

        // 更新内部属性（用于缓存）
        analyzedModules.set(modules)
    }

    /**
     * 构建 JSON 报告
     */
    private fun buildJsonReport(modules: List<ModuleSoInfo>): String {
        return buildString {
            appendLine("[")
            modules.forEachIndexed { index, module ->
                append("  {")
                appendLine()
                append("    \"moduleName\": \"${escapeJsonString(module.moduleName)}\",")
                appendLine()
                append("    \"modulePath\": \"${escapeJsonString(module.modulePath)}\",")
                appendLine()
                append("    \"soFiles\": [")
                appendLine()

                module.soFiles.forEachIndexed { soIndex, soFile ->
                    append("      \"${escapeJsonString(soFile)}\"")
                    if (soIndex < module.soFiles.size - 1) append(",")
                    appendLine()
                }

                append("    ]")
                appendLine()
                append("  }")
                if (index < modules.size - 1) append(",")
                appendLine()
            }
            append("]")
        }
    }

    /**
     * JSON 字符串转义
     */
    private fun escapeJsonString(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * 写入空报告
     */
    private fun writeEmptyReport() {
        generateReport(emptyList())
    }

    /**
     * 记录分析结果统计
     */
    private fun logAnalysisResults(modules: List<ModuleSoInfo>) {
        val totalSoFiles = modules.sumOf { it.soFiles.size }

        logger.lifecycle("=".repeat(50))
        logger.lifecycle("SO Files Analysis Summary")
        logger.lifecycle("=".repeat(50))
        logger.lifecycle("Variant: ${variantName.get()}")
        logger.lifecycle("Modules with SO files: ${modules.size}")
        logger.lifecycle("Total SO files found: $totalSoFiles")

        if (modules.isNotEmpty()) {
            logger.lifecycle("")
            logger.lifecycle("Modules breakdown:")
            modules.forEach { module ->
                logger.lifecycle("  • ${module.moduleName}")
                logger.lifecycle("    - SO files: ${module.soFiles.size}")
                logger.lifecycle("    - Path: ${module.modulePath}")
                if (logger.isInfoEnabled) {
                    module.soFiles.forEach { soFile ->
                        logger.info("      - $soFile")
                    }
                }
            }
        }

        logger.lifecycle("=".repeat(50))
        logger.lifecycle("Report saved to: ${reportFile.get().asFile.absolutePath}")
        logger.lifecycle("=".repeat(50))
    }
}