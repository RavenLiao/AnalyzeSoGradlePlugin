package io.github.ravenliao.plugin

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class AnalyzeSoGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // 检测项目类型并应用相应的配置
        configureProject(project)
        // 注册聚合报告任务
        project.afterEvaluate {
            val analyzeTasks = project.tasks.matching { it.name.startsWith("analyze") && it.name.endsWith("So") && it.name != "analyzeSo" }
            project.tasks.register("analyzeSo", AggregateAnalyzeSoTask::class.java) {
                group = "analyze-so"
                description = "Aggregate SO analysis reports for all variants and generate a tabbed HTML report."
                dependsOn(analyzeTasks)
            }
        }
    }

    private fun configureProject(project: Project) {
        project.plugins.withId("com.android.application") {
            val androidComponents =
                project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
            configureAndroidComponents(project, androidComponents)
        }

        project.plugins.withId("com.android.library") {
            val androidComponents =
                project.extensions.getByType(LibraryAndroidComponentsExtension::class.java)
            configureAndroidComponents(project, androidComponents)
        }
    }

    private fun configureAndroidComponents(
        project: Project,
        androidComponents: AndroidComponentsExtension<*, *, *>
    ) {
        androidComponents.onVariants { variant ->
            val currentVariantName = variant.name
            val variantNameCapitalized = currentVariantName.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }

            // 创建基本的分析任务
            val analyzeTaskName = "analyze${variantNameCapitalized}So"
            project.tasks.register(analyzeTaskName, AnalyzeSoTask::class.java) {
                group = "analyze-so"
                description = "Analyze SO files for $currentVariantName variant"
                variantName.set(currentVariantName)
                reportFile.set(
                    project.layout.buildDirectory.file("reports/analyze-so/${currentVariantName}/analyze-so-report.json")
                )
            }
        }
    }
}