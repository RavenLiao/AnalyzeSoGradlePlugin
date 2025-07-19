pluginManagement {
    repositories {
        maven {
            setUrl("https://maven.aliyun.com/repository/google")
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }  // google
        maven { setUrl("https://maven.aliyun.com/repository/public") }  // central、jcenter
        maven { setUrl("https://maven.aliyun.com/repository/gradle-plugin") }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    includeBuild("AnalyzeSoGradlePlugin")
}
dependencyResolutionManagement {
repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            setUrl("https://maven.aliyun.com/repository/google")
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }  // google
        maven { setUrl("https://maven.aliyun.com/repository/public") }  // central、jcenter

        google()
        mavenCentral()
    }
}

rootProject.name = "AnalyzeSoGradlePlugin"
include(":app")
include(":libraryDemo")
