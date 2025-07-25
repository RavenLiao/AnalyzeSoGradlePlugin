package io.github.ravenliao.plugin

import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

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
                generateReport(emptyList())
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
            val soFiles: List<SoInfo> = if (artifactFile.isDirectory) {
                SoCollector.collectSoFilesFromDirectory(
                    artifactFile,
                    ElfUtils::checkElfAlignmentWithKb
                )
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
     * 生成分析报告
     */
    private fun generateReport(modules: List<ModuleSoInfo>) {
    val outputFile = reportFile.get().asFile
    // 确保输出目录存在
    outputFile.parentFile?.mkdirs()
    val json = Json { prettyPrint = true }
    val jsonContent = json.encodeToString(modules)
    outputFile.writeText(jsonContent, Charsets.UTF_8)

    // 生成HTML报告
    val htmlFile = File(outputFile.parentFile, "analyze-so-report.html")
    val templateStream = javaClass.classLoader.getResourceAsStream("so_analysis_report_template.html")
    if (templateStream != null) {
        val template = templateStream.bufferedReader(Charsets.UTF_8).readText()
        // 用紧凑JSON，避免HTML体积过大
        val compactJson = Json.encodeToString(modules)
        val htmlContent = template.replace("__SO_ANALYSIS_JSON_DATA__", compactJson)
        htmlFile.writeText(htmlContent, Charsets.UTF_8)
    } else {
        logger.warn("so_analysis_report_template.html not found in resources, HTML report not generated.")
    }

    logger.info("SO analysis report generated: ${outputFile.absolutePath}")
    logger.info("SO analysis HTML report generated: ${htmlFile.absolutePath}")
    // 更新内部属性（用于缓存）
    analyzedModules.set(modules)
}

    /**
     * 记录分析结果统计
     */
    private fun logAnalysisResults(modules: List<ModuleSoInfo>) {
        // 汇总所有 so 文件和架构
        val totalSoFiles =
            modules.sumOf { it.soFiles.sumOf { sfi -> sfi.architectures.values.sumOf { list -> list.size } } }
        val totalUniqueSoFiles = modules.sumOf { it.soFiles.size }

        logger.lifecycle("=".repeat(50))
        logger.lifecycle("SO Files Analysis Summary")
        logger.lifecycle("=".repeat(50))
        logger.lifecycle("Variant: ${variantName.get()}")
        logger.lifecycle("Modules with SO files: ${modules.size}")
        logger.lifecycle("Total SO file entries: $totalSoFiles")
        logger.lifecycle("Unique SO file names: $totalUniqueSoFiles")

        if (modules.isNotEmpty()) {
            logger.lifecycle("")
            logger.lifecycle("Modules breakdown:")
            modules.forEach { module ->
                logger.lifecycle("  • ${module.moduleName}")
                logger.lifecycle("    - Path: ${module.modulePath}")
                if (module.soFiles.isEmpty()) {
                    logger.lifecycle("    - No SO files found.")
                } else {
                    module.soFiles.forEach { soInfo ->
                        logger.lifecycle("    - ${soInfo.name}:")
                        soInfo.architectures.forEach { (arch, details) ->
                            logger.lifecycle("      [${arch}]:")
                            details.forEach { detail ->
                                val pageInfo =
                                    detail.alignmentKb?.let { " | page size: ${it}KB" } ?: ""
                                logger.lifecycle(
                                    "        ${detail.fileName} | aligned: ${detail.aligned} | alignment: ${detail.alignment}${pageInfo}"
                                )
                            }
                        }
                    }
                }
            }
        }

        logger.lifecycle("=".repeat(50))
        logger.lifecycle("Report saved to: ${reportFile.get().asFile.absolutePath}")
        val htmlFile = File(reportFile.get().asFile.parentFile, "analyze-so-report.html")
        logger.lifecycle("HTML report: ${htmlFile.absolutePath}")
        logger.lifecycle("=".repeat(50))
    }
}