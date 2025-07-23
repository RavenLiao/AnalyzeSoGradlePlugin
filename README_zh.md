# AnalyzeSoGradlePlugin

[![License][license_image]][license_link]

Other Language：[English](README.md)



**AnalyzeSoGradlePlugin** 是一个用于分析 Android so 库来源和架构的 Gradle 插件，旨在帮助开发者更好地了解项目中各种依赖引入的 so 文件信息。

## 功能特性

### 已实现
- 输出 so 库的来源和架构信息
- 16KB 内存页大小检测，方便适配 Android 15 的要求([支持 16 KB 的页面大小](https://developer.android.google.cn/guide/practices/page-sizes?hl=zh-cn))
- 美观的 HTML 报告输出，一键筛出没有兼容Android 15的原生库及其来源。

### 待实现
- 添加聚合所有变体的任务，并生成合并的 HTML 报告
- 集成 [LibChecker-Rules](https://github.com/LibChecker/LibChecker-Rules) 规则库，在 HTML 报告中提供更详细的库信息
- 欢迎提出问题或建议！

## 快速开始
**Gradle 插件引入方式：**

本插件已发布至 Maven Central 仓库：

 ${LAST_VERSION}: [![Download][version_icon]][version_link] (去掉'v')

在 `build.gradle` 中应用插件：

```kotlin
plugins {
    id("io.github.ravenliao.analyze-so") version "$LAST_VERSION"
}
```

## 使用方法

应用插件后，Gradle 会为每个构建变体自动生成对应的分析任务：

```bash
# 分析特定变体的 so 文件
./gradlew analyze[VariantName]So

# 例如，分析 debug 变体
./gradlew analyzeDebugSo
```

分析完成后，任务输出的末尾会显示详细的 HTML 报告路径。报告通常位于：
```
app/build/reports/analyze-so/[VariantName]/analyze-so-report.html
```

例如，对于 debug 变体，报告将位于：
```
app/build/reports/analyze-so/Debug/analyze-so-report.html
```

## 特别感谢

- [mainlxl/AnalyzeSoPlugin](https://github.com/mainlxl/AnalyzeSoPlugin): 本项目参考了其实现思路

## License

Apache 2.0. 详情见 [LICENSE](LICENSE) 文件。



[version_icon]: https://img.shields.io/maven-central/v/io.github.ravenliao.analyze-so/gradle-plugin

[version_link]: https://repo1.maven.org/maven2/io/github/ravenliao/analyze-so/gradle-plugin/
[license_image]: https://img.shields.io/badge/License-Apache%202-blue.svg
[license_link]: https://www.apache.org/licenses/LICENSE-2.0
