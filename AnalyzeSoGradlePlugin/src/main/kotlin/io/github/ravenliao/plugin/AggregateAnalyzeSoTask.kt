package io.github.ravenliao.plugin

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Aggregate SO analysis reports for all variants and generate a tabbed HTML report.
 */
abstract class AggregateAnalyzeSoTask : DefaultTask() {
    private val openOnceService =
        project.gradle.sharedServices.registerIfAbsent("analyzeSoOpenReportOnce", OpenReportOnceService::class.java) {}

    init {
        group = "analyze-so"
        description =
            "Aggregate SO analysis reports for all variants and generate a tabbed HTML report."
        usesService(openOnceService)
    }

    @TaskAction
    fun aggregate() {
        val reportsDir = project.layout.buildDirectory.file("reports/analyze-so").get().asFile
        if (!reportsDir.exists()) {
            logger.lifecycle("[analyzeSo] Analysis report directory not found: ${reportsDir.absolutePath}")
            return
        }
        // Find all variant JSON reports
        val variantDirs =
            reportsDir.listFiles { f -> f.isDirectory && f.name != "aggregate" } ?: emptyArray()
        val variantJsons = variantDirs.mapNotNull { variantDir ->
            val jsonFile = File(variantDir, "analyze-so-report.json")
            if (jsonFile.exists()) {
                variantDir.name to jsonFile.readText(Charsets.UTF_8)
            } else null
        }.toMap()
        if (variantJsons.isEmpty()) {
            logger.lifecycle("[analyzeSo] No variant JSON reports found.")
            return
        }
        // Generate aggregate HTML
        val aggregateDir = File(reportsDir, "aggregate")
        aggregateDir.mkdirs()
        val aggregateHtmlFile = File(aggregateDir, "analyze-so-report.html")
        aggregateHtmlFile.writeText(buildAggregateHtml(variantJsons), Charsets.UTF_8)
        logger.lifecycle("[analyzeSo] Aggregate report generated: ${aggregateHtmlFile.toURI()}")
        if (shouldOpenReport() && openOnceService.get().tryAcquire()) {
            openInBrowser(aggregateHtmlFile)
        }
    }

    abstract class OpenReportOnceService : BuildService<BuildServiceParameters.None>, AutoCloseable {
        private val opened = AtomicBoolean(false)

        fun tryAcquire(): Boolean = opened.compareAndSet(false, true)

        override fun close() {
        }
    }

    private fun shouldOpenReport(): Boolean {
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

    private fun buildAggregateHtml(variantJsons: Map<String, String>): String {
        val templateStream =
            javaClass.classLoader.getResourceAsStream("so_analysis_report_template.html")
        val template = templateStream?.bufferedReader(Charsets.UTF_8)?.readText()
            ?: error("so_analysis_report_template.html not found in resources")
        return if (variantJsons.size > 1) {
            val allVariantsJson = Json.encodeToString(variantJsons)
            template.replace("ALL_VARIANTS_DATA_PLACEHOLDER", allVariantsJson)
                .replace("__SO_ANALYSIS_JSON_DATA__", "null")
        } else {
            // Single variant, compatible with original logic
            val only = variantJsons.values.firstOrNull() ?: "[]"
            template.replace("ALL_VARIANTS_DATA_PLACEHOLDER", "undefined")
                .replace("__SO_ANALYSIS_JSON_DATA__", only)
        }
    }
}