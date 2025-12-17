package io.github.ravenliao.plugin

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Aggregate SO analysis reports for all variants and generate a tabbed HTML report.
 */
abstract class AggregateAnalyzeSoTask : DefaultTask() {

    private val openOnceService =
        project.gradle.sharedServices.registerIfAbsent("analyzeSoOpenReportOnce", OpenReportOnceService::class.java) {}

    init {
        group = AnalyzeSoConstants.GROUP
        description =
            "Aggregate SO analysis reports for all variants and generate a tabbed HTML report."
        usesService(openOnceService)
    }

    @TaskAction
    fun aggregate() {
        val reportsDir = project.layout.buildDirectory.file(AnalyzeSoConstants.REPORTS_DIR).get().asFile
        if (!reportsDir.exists()) {
            logger.lifecycle("[analyzeSo] Analysis report directory not found: ${reportsDir.absolutePath}")
            return
        }
        // Find all variant JSON reports
        val variantDirs =
            reportsDir.listFiles { f -> f.isDirectory && f.name != AnalyzeSoConstants.AGGREGATE_DIR_NAME } ?: emptyArray()
        val variantJsons = variantDirs.mapNotNull { variantDir ->
            val jsonFile = File(variantDir, AnalyzeSoConstants.REPORT_JSON_FILE_NAME)
            if (jsonFile.exists()) {
                variantDir.name to jsonFile.readText(Charsets.UTF_8)
            } else null
        }.toMap()

        if (variantJsons.isEmpty()) {
            logger.lifecycle("[analyzeSo] No variant JSON reports found.")
            return
        }
        // Generate aggregate HTML
        val aggregateDir = File(reportsDir, AnalyzeSoConstants.AGGREGATE_DIR_NAME)
        aggregateDir.mkdirs()
        val aggregateHtmlFile = File(aggregateDir, AnalyzeSoConstants.REPORT_HTML_FILE_NAME)
        aggregateHtmlFile.writeText(buildAggregateHtml(variantJsons), Charsets.UTF_8)
        logger.lifecycle("[analyzeSo] Aggregate report generated: ${aggregateHtmlFile.toURI()}")
        if (AnalyzeSoReportUtils.shouldOpenReport(project, allowWhenAggregateRun = true) && openOnceService.get().tryAcquire()) {
            AnalyzeSoReportUtils.openInBrowser(logger, aggregateHtmlFile)
        }
    }

    private fun buildAggregateHtml(variantJsons: Map<String, String>): String {
        val classLoader = javaClass.classLoader
            ?: error("ClassLoader is null, cannot load ${AnalyzeSoConstants.TEMPLATE_RESOURCE}")
        val templateStream =
            classLoader.getResourceAsStream(AnalyzeSoConstants.TEMPLATE_RESOURCE)
        val template = templateStream?.bufferedReader(Charsets.UTF_8)?.readText()
            ?: error("${AnalyzeSoConstants.TEMPLATE_RESOURCE} not found in resources")
        return if (variantJsons.size > 1) {
            val allVariantsJson = Json.encodeToString(variantJsons)
            template.replace(AnalyzeSoConstants.PLACEHOLDER_ALL_VARIANTS_JSON, allVariantsJson)
                .replace(AnalyzeSoConstants.PLACEHOLDER_SINGLE_JSON, "null")
        } else {
            // Single variant, compatible with original logic
            val only = variantJsons.values.firstOrNull() ?: "[]"
            template.replace(AnalyzeSoConstants.PLACEHOLDER_ALL_VARIANTS_JSON, "undefined")
                .replace(AnalyzeSoConstants.PLACEHOLDER_SINGLE_JSON, only)
        }
    }
}