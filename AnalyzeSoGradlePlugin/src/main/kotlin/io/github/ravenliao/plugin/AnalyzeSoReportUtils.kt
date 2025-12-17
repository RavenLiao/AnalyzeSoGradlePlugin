package io.github.ravenliao.plugin

import java.io.File
import org.gradle.api.Project

internal object AnalyzeSoReportUtils {

    fun shouldOpenReport(project: Project, allowWhenAggregateRun: Boolean): Boolean {
        if (!allowWhenAggregateRun) {
            val requestedTasks = project.gradle.startParameter.taskNames
            val isAggregateRun = requestedTasks.any {
                it == AnalyzeSoConstants.TASK_AGGREGATE || it.endsWith(":" + AnalyzeSoConstants.TASK_AGGREGATE)
            }
            if (isAggregateRun) return false
        }

        val value =
            project.providers.gradleProperty(AnalyzeSoConstants.PROP_OPEN_REPORT_DOT).orNull
                ?: project.providers.gradleProperty(AnalyzeSoConstants.PROP_OPEN_REPORT_CAMEL).orNull
                ?: project.providers.systemProperty(AnalyzeSoConstants.PROP_OPEN_REPORT_DOT).orNull
                ?: project.providers.systemProperty(AnalyzeSoConstants.PROP_OPEN_REPORT_CAMEL).orNull

        return value?.equals("true", ignoreCase = true) == true
    }

    fun openInBrowser(logger: org.gradle.api.logging.Logger, htmlFile: File) {
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

    fun readObjdumpPath(project: Project): String {
        return project.providers.gradleProperty(AnalyzeSoConstants.PROP_OBJDUMP_PATH_DOT).orNull
            ?: project.providers.gradleProperty(AnalyzeSoConstants.PROP_OBJDUMP_PATH_CAMEL).orNull
            ?: project.providers.systemProperty(AnalyzeSoConstants.PROP_OBJDUMP_PATH_DOT).orNull
            ?: project.providers.systemProperty(AnalyzeSoConstants.PROP_OBJDUMP_PATH_CAMEL).orNull
            ?: "objdump"
    }
}
