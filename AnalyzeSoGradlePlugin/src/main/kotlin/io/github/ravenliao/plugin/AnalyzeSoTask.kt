package io.github.ravenliao.plugin

import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
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
        group = "analyze-so"
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
            val introducedByMap = computeIntroducedByMap(configuration)
            val introducedByPathsMap = computeIntroducedByPathsMap(configuration)

            // 创建 ArtifactView 来获取 JNI 工件
            val artifactView = configuration.incoming.artifactView {
                attributes {
                    attribute(
                        Attribute.of("artifactType", String::class.java),
                        "android-jni"
                    )
                }
            }

            // 处理每个工件
            artifactView.artifacts.artifacts.mapNotNull { artifact ->
                processArtifact(artifact, introducedByMap, introducedByPathsMap)
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
    private fun processArtifact(
        artifact: org.gradle.api.artifacts.result.ResolvedArtifactResult,
        introducedByMap: Map<String, List<String>>,
        introducedByPathsMap: Map<String, Map<String, List<String>>>
    ): ModuleSoInfo? {
        return try {
            val artifactFile = artifact.file
            if (!artifactFile.exists()) {
                logger.debug("Artifact file does not exist: ${artifactFile.absolutePath}")
                return null
            }

            val moduleName = artifact.id.componentIdentifier.displayName
            val introducedBy = introducedByMap[moduleName] ?: emptyList()
            val introducedByPaths = introducedByPathsMap[moduleName] ?: emptyMap()
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
                introducedBy = introducedBy,
                introducedByPaths = introducedByPaths,
                soFiles = soFiles,
                modulePath = artifactFile.absolutePath
            )

        } catch (exception: Exception) {
            logger.warn("Failed to process artifact: ${artifact.id}", exception)
            null
        }
    }

    private fun computeIntroducedByMap(configuration: Configuration): Map<String, List<String>> {
        return try {
            val root = configuration.incoming.resolutionResult.root
            computeIntroducedByMapFromRoot(root)
        } catch (exception: Exception) {
            logger.info(
                "[analyzeSo] Failed to compute dependency graph for configuration ${configuration.name}, introducedBy will be empty.",
                exception
            )
            emptyMap()
        }
    }

    private fun computeIntroducedByMapFromRoot(root: ResolvedComponentResult): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableSet<String>>()

        val queue = ArrayDeque<Pair<ResolvedComponentResult, Set<String>>>()

        root.dependencies
            .asSequence()
            .filterIsInstance<ResolvedDependencyResult>()
            .filter { !it.isConstraint }
            .forEach { dep ->
                val direct = dep.selected
                val origin = direct.id.displayName
                result.getOrPut(direct.id.displayName) { mutableSetOf() }.add(origin)
                queue.addLast(direct to setOf(origin))
            }

        while (queue.isNotEmpty()) {
            val (component, origins) = queue.removeFirst()

            component.dependencies
                .asSequence()
                .filterIsInstance<ResolvedDependencyResult>()
                .filter { !it.isConstraint }
                .forEach { dep ->
                    val child = dep.selected
                    val set = result.getOrPut(child.id.displayName) { mutableSetOf() }
                    val changed = set.addAll(origins)
                    if (changed) {
                        queue.addLast(child to origins)
                    }
                }
        }

        return result.mapValues { (_, v) -> v.toList().sorted() }
    }

    private fun computeIntroducedByPathsMap(configuration: Configuration): Map<String, Map<String, List<String>>> {
        return try {
            val root = configuration.incoming.resolutionResult.root
            computeIntroducedByPathsMapFromRoot(root)
        } catch (exception: Exception) {
            logger.info(
                "[analyzeSo] Failed to compute dependency paths for configuration ${configuration.name}, introducedByPaths will be empty.",
                exception
            )
            emptyMap()
        }
    }

    private fun computeIntroducedByPathsMapFromRoot(root: ResolvedComponentResult): Map<String, Map<String, List<String>>> {
        val topLevelComponents = root.dependencies
            .asSequence()
            .filterIsInstance<ResolvedDependencyResult>()
            .filter { !it.isConstraint }
            .map { it.selected }
            .distinctBy { it.id.displayName }
            .toList()

        val result = mutableMapOf<String, MutableMap<String, List<String>>>()
        for (top in topLevelComponents) {
            val topName = top.id.displayName
            val parent = computeParentsFromTopLevel(top)
            for ((nodeName, path) in buildPathsFromParentMap(topName, parent)) {
                result.getOrPut(nodeName) { mutableMapOf() }[topName] = path
            }
        }
        return result
    }

    private fun computeParentsFromTopLevel(top: ResolvedComponentResult): Map<String, String?> {
        val parent = mutableMapOf<String, String?>()
        val seen = mutableSetOf<String>()

        val topName = top.id.displayName
        parent[topName] = null
        seen.add(topName)

        val queue = ArrayDeque<ResolvedComponentResult>()
        queue.addLast(top)

        while (queue.isNotEmpty()) {
            val component = queue.removeFirst()
            val componentName = component.id.displayName

            component.dependencies
                .asSequence()
                .filterIsInstance<ResolvedDependencyResult>()
                .filter { !it.isConstraint }
                .forEach { dep ->
                    val child = dep.selected
                    val childName = child.id.displayName
                    if (seen.add(childName)) {
                        parent[childName] = componentName
                        queue.addLast(child)
                    }
                }
        }

        return parent
    }

    private fun buildPathsFromParentMap(
        topName: String,
        parent: Map<String, String?>
    ): Map<String, List<String>> {
        val paths = mutableMapOf<String, List<String>>()
        for (nodeName in parent.keys) {
            paths[nodeName] = buildPathToTop(nodeName, topName, parent)
        }
        return paths
    }

    private fun buildPathToTop(
        nodeName: String,
        topName: String,
        parent: Map<String, String?>
    ): List<String> {
        val path = ArrayList<String>()
        var current: String? = nodeName
        while (current != null) {
            path.add(current)
            if (current == topName) break
            current = parent[current]
        }
        path.reverse()
        return path
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
        val templateStream =
            javaClass.classLoader?.getResourceAsStream("so_analysis_report_template.html")
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
        val outputFile = reportFile.get().asFile
        logger.lifecycle("Report saved to: ${outputFile.toURI()}")
        val htmlFile = File(outputFile.parentFile, "analyze-so-report.html")
        logger.lifecycle("HTML report: ${htmlFile.toURI()}")
        if (shouldOpenReport()) {
            openInBrowser(htmlFile)
        }
        logger.lifecycle("=".repeat(50))
    }

    private fun shouldOpenReport(): Boolean {
        val requestedTasks = project.gradle.startParameter.taskNames
        val isAggregateRun = requestedTasks.any { it == "analyzeSo" || it.endsWith(":analyzeSo") }
        if (isAggregateRun) return false

        val value =
            project.providers.gradleProperty("analyzeSo.openReport").orNull
                ?: project.providers.gradleProperty("analyzeSoOpenReport").orNull
                ?: project.providers.systemProperty("analyzeSo.openReport").orNull
                ?: project.providers.systemProperty("analyzeSoOpenReport").orNull
        return value?.equals("true", ignoreCase = true) == true
    }

    private fun openInBrowser(htmlFile: File) {
        if (!htmlFile.exists()) return
        val uri = htmlFile.toURI().toString()
        try {
            val os = (System.getProperty("os.name") ?: "").lowercase()
            val command = when {
                os.contains("windows") -> listOf("cmd", "/c", "start", "", uri)
                os.contains("mac") -> listOf("open", uri)
                else -> listOf("xdg-open", uri)
            }
            ProcessBuilder(command).start()
        } catch (t: Throwable) {
            logger.warn("Failed to open HTML report in browser: ${htmlFile.absolutePath}", t)
        }
    }
}