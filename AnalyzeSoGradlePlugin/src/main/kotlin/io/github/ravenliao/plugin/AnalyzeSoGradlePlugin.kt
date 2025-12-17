package io.github.ravenliao.plugin

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

@Suppress("unused")
class AnalyzeSoGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val aggregateTask = project.tasks.register(AnalyzeSoConstants.TASK_AGGREGATE, AggregateAnalyzeSoTask::class.java) {
            group = AnalyzeSoConstants.GROUP
            description = "Aggregate SO analysis reports for all variants and generate a tabbed HTML report."
            markNotCompatibleWithConfigurationCacheIfSupported(
                "This task aggregates reports and may use Gradle model types at execution time."
            )
        }

        // 检测项目类型并应用相应的配置
        configureProject(project, aggregateTask)
    }

    private fun configureProject(
        project: Project,
        aggregateTask: TaskProvider<AggregateAnalyzeSoTask>
    ) {
        project.plugins.withId("com.android.application") {
            val androidComponents =
                project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
            configureAndroidComponents(project, androidComponents, aggregateTask)
        }

        project.plugins.withId("com.android.library") {
            val androidComponents =
                project.extensions.getByType(LibraryAndroidComponentsExtension::class.java)
            configureAndroidComponents(project, androidComponents, aggregateTask)
        }
    }

    private fun configureAndroidComponents(
        project: Project,
        androidComponents: AndroidComponentsExtension<*, *, *>,
        aggregateTask: TaskProvider<AggregateAnalyzeSoTask>
    ) {
        androidComponents.onVariants { variant ->
            val currentVariantName = variant.name
            val variantNameCapitalized = currentVariantName.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }

            // 创建基本的分析任务
            val analyzeTaskName = "analyze${variantNameCapitalized}So"
            val analyzeTask = project.tasks.register(analyzeTaskName, AnalyzeSoTask::class.java) {
                group = AnalyzeSoConstants.GROUP
                description = "Analyze SO files for $currentVariantName variant"
                variantName.set(currentVariantName)
                reportFile.set(
                    project.layout.buildDirectory.file("${AnalyzeSoConstants.REPORTS_DIR}/${currentVariantName}/${AnalyzeSoConstants.REPORT_JSON_FILE_NAME}")
                )
                markNotCompatibleWithConfigurationCacheIfSupported(
                    "This task resolves dependencies and accesses Gradle model types at execution time."
                )
            }

            aggregateTask.configure {
                dependsOn(analyzeTask)
            }
        }
    }

    private fun Task.markNotCompatibleWithConfigurationCacheIfSupported(reason: String) {
        try {
            val method = javaClass.methods.firstOrNull {
                it.name == "notCompatibleWithConfigurationCache" &&
                    it.parameterTypes.size == 1 &&
                    it.parameterTypes[0] == String::class.java
            }
            method?.invoke(this, reason)
        } catch (_: Throwable) {
        }
    }
}