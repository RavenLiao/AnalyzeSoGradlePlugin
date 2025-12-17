package io.github.ravenliao.plugin

internal object AnalyzeSoConstants {
    const val GROUP = "analyze-so"

    const val REPORTS_DIR = "reports/analyze-so"
    const val REPORT_JSON_FILE_NAME = "analyze-so-report.json"
    const val REPORT_HTML_FILE_NAME = "analyze-so-report.html"
    const val AGGREGATE_DIR_NAME = "aggregate"

    const val TEMPLATE_RESOURCE = "so_analysis_report_template.html"

    const val PLACEHOLDER_SINGLE_JSON = "__SO_ANALYSIS_JSON_DATA__"
    const val PLACEHOLDER_ALL_VARIANTS_JSON = "ALL_VARIANTS_DATA_PLACEHOLDER"

    const val TASK_AGGREGATE = "analyzeSo"

    const val PROP_OPEN_REPORT_DOT = "analyzeSo.openReport"
    const val PROP_OPEN_REPORT_CAMEL = "analyzeSoOpenReport"

    const val PROP_OBJDUMP_PATH_DOT = "analyzeSo.objdumpPath"
    const val PROP_OBJDUMP_PATH_CAMEL = "analyzeSoObjdumpPath"

    const val PROP_MAX_ARCHIVE_MB_DOT = "analyzeSo.maxArchiveSizeMb"
    const val PROP_MAX_ARCHIVE_MB_CAMEL = "analyzeSoMaxArchiveSizeMb"
    const val DEFAULT_MAX_ARCHIVE_SIZE_MB = 200L

    const val ATTRIBUTE_ARTIFACT_TYPE = "artifactType"
    const val ARTIFACT_TYPE_ANDROID_JNI = "android-jni"
}
