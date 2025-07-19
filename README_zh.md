# AnalyzeSoGradlePlugin

[![License][license_image]][license_link]

Other Language：[English](README.md)



**AnalyzeSoGradlePlugin** 是一个用于分析 Android so 库来源和架构的 Gradle 插件，旨在帮助开发者更好地了解项目中各种依赖引入的 so 文件信息。

## 功能特性

### 已实现
- 输出 so 库的来源和架构信息

### 待实现
- 支持 16KB 内存页大小检测, 方便适配Android 15的要求([支持 16 KB 的页面大小](https://developer.android.google.cn/guide/practices/page-sizes?hl=zh-cn))
- 更优美的输出 UI
- 欢迎提出问题或建议！

## 快速开始

本插件已发布至 Maven Central 仓库： [![Download][version_icon]][version_link] (excluding 'v')

**Gradle 插件引入方式：**

在 `build.gradle` 中应用插件：

```kotlin
plugins {
    id("io.github.ravenliao.analyze-so") version "0.0.1"
}
```


## 特别感谢

- [mainlxl/AnalyzeSoPlugin](https://github.com/mainlxl/AnalyzeSoPlugin): 本项目参考了其实现思路

## License

Apache 2.0. 详情见 [LICENSE](LICENSE) 文件。



[version_icon]: https://img.shields.io/maven-central/v/io.github.ravenliao.analyze-so/gradle-plugin

[version_link]: https://repo1.maven.org/maven2/io/github/ravenliao/analyze-so/gradle-plugin/
[license_image]: https://img.shields.io/badge/License-Apache%202-blue.svg
[license_link]: https://www.apache.org/licenses/LICENSE-2.0
