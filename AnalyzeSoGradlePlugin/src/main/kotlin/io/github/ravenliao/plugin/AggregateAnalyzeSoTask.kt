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
    init {
        group = "analyze-so"
        description =
            "Aggregate SO analysis reports for all variants and generate a tabbed HTML report."
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
        logger.lifecycle("[analyzeSo] Aggregate report generated: file://${aggregateHtmlFile.absolutePath}")
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