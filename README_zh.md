# AnalyzeSoGradlePlugin

[![License][license_image]][license_link]

Other Language：[English](README.md)



**AnalyzeSoGradlePlugin** 是一个用于分析 Android so 库来源和架构的 Gradle 插件，旨在帮助开发者更好地了解项目中各种依赖引入的 so 文件信息。

## 一句话跑起来

在构建脚本中应用插件：

将 `<latest_version>` 替换为[![Download][version_icon]][version_link] (去掉'v')显示的版本号。

`build.gradle.kts`（Kotlin DSL）

```kotlin
plugins {
    id("io.github.ravenliao.analyze-so") version "<latest_version>"
}
```

然后直接执行：

```bash
./gradlew analyzeSo -PanalyzeSoOpenReport=true
```

说明：Windows PowerShell 下请使用：

```bash
.\gradlew analyzeSo -PanalyzeSoOpenReport=true
```

## 功能特性

### 已实现
- 输出 so 库的来源和架构信息
- 16KB 内存页大小检测，方便适配 Android 15 的要求([支持 16 KB 的页面大小](https://developer.android.google.cn/guide/practices/page-sizes?hl=zh-cn))
- 美观的 HTML 报告输出，一键筛出没有兼容Android 15的原生库及其来源。
- 在 HTML 报告中展示可展开的“依赖路径”，用于从包含 so 的底层依赖反查到顶层直接依赖（按每个顶层直接依赖给出一条最短生效路径）
- 添加聚合所有变体的任务，并生成合并的 HTML 报告
- 集成 [LibChecker-Rules](https://github.com/LibChecker/LibChecker-Rules) 规则库，在 HTML 报告中提供更详细的库信息

### 待实现
- 欢迎提出问题或建议！

## 环境依赖（objdump）

插件通过执行 `objdump -p` 来解析 ELF Program Header 的 `Align 2**N`（内存页大小）。若仅需要分析 so 库来源和架构信息，可以忽略。

### Linux
- 一般已内置；如缺请安装 binutils（例如 `sudo apt-get install binutils`、`sudo dnf install binutils`、`sudo pacman -S binutils`）。

### macOS
- 通常可直接使用；如需可执行：`brew install binutils`。

### Windows（图形化）
1) 下载并运行 TDM-GCC 安装器：https://jmeubank.github.io/tdm-gcc/
2) 安装过程中选择“Add to PATH”（或等效选项）。
3) 完成安装后，`objdump.exe` 可被 Gradle 直接使用。

## 快速开始
**Gradle 插件引入方式：**

本插件已发布至 Maven Central 仓库：

最新版本：[![Download][version_icon]][version_link] (去掉'v')

在 `build.gradle.kts`（Kotlin DSL）中应用插件：

```kotlin
plugins {
    id("io.github.ravenliao.analyze-so") version "<latest_version>"
}
```

在 `build.gradle`（Groovy DSL）中应用插件：

```groovy
plugins {
    id 'io.github.ravenliao.analyze-so' version '<latest_version>'
}
```

## 使用方法

应用插件后，Gradle 会为每个构建变体自动生成对应的分析任务：

```bash
# 推荐：分析所有变体并自动打开报告
./gradlew analyzeSo -PanalyzeSoOpenReport=true

# 分析特定变体的 so 文件
./gradlew analyze[VariantName]So

# 例如，分析 debug 变体
./gradlew analyzeDebugSo

# 分析所有变体的 so 文件
./gradlew analyzeSo
```

分析完成后，任务输出的末尾会显示详细的 HTML 报告路径。报告通常位于：
```
app/build/reports/analyze-so/[VariantName]/analyze-so-report.html
```

例如，对于 debug 变体，报告将位于：
```
app/build/reports/analyze-so/Debug/analyze-so-report.html
```

## 配置

### Configuration Cache（Gradle）

启用 Gradle 的 Configuration Cache 后，本插件的分析任务在部分环境下可能会导致任务运行失败，并使 Configuration Cache entry 被丢弃或报错。如你开启了 Configuration Cache 并遇到问题，可在项目的 `gradle.properties` 中添加：
```properties
org.gradle.configuration-cache=false
```

### 自动打开报告

可选：自动用默认浏览器打开 HTML 报告：
```bash
./gradlew analyzeSo -PanalyzeSoOpenReport=true
```

说明：
- 执行 `analyzeSo` 时，只会打开聚合后的 HTML 报告（每次 Gradle 构建只打开一次）。
- Windows PowerShell 下如果使用带点的属性名，可用 `"-PanalyzeSo.openReport=true"`（需要引号）。

### 指定 `objdump` 路径

可选：如果你的环境没有提供 `objdump`（Windows/CI 比较常见），导致报告里 alignment 显示为 `error`，可以手动指定路径：
```bash
./gradlew analyzeDebugSo -PanalyzeSoObjdumpPath=/path/to/objdump
```

### 限制 archive 解包大小

可选：如果你的依赖中包含较大的 AAR/ZIP/JAR，插件在扫描其中的 `.so` 时会尝试解包。为避免极端情况下解包导致构建变慢，默认只会解包不超过 **200MB** 的 archive。你可以修改或关闭这个限制：
```bash
# 将最大可解包大小设置为 500MB
./gradlew analyzeDebugSo -PanalyzeSoMaxArchiveSizeMb=500

# 关闭限制（<=0 表示关闭）
./gradlew analyzeDebugSo -PanalyzeSoMaxArchiveSizeMb=0
```

说明：Windows PowerShell 下如果使用带点的属性名，可用 `"-PanalyzeSo.maxArchiveSizeMb=500"`（需要引号）。

## 特别感谢

- [mainlxl/AnalyzeSoPlugin](https://github.com/mainlxl/AnalyzeSoPlugin): 本项目参考了其实现思路
- [LibChecker/LibChecker-Rules](https://github.com/LibChecker/LibChecker-Rules): 感谢提供了so库数据库

## License

Apache 2.0. 详情见 [LICENSE](LICENSE) 文件。



[version_icon]: https://img.shields.io/maven-central/v/io.github.ravenliao.analyze-so/gradle-plugin

[version_link]: https://repo1.maven.org/maven2/io/github/ravenliao/analyze-so/gradle-plugin/
[license_image]: https://img.shields.io/badge/License-Apache%202-blue.svg
[license_link]: https://www.apache.org/licenses/LICENSE-2.0
