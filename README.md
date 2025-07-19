# AnalyzeSoGradlePlugin

[![License][license_image]][license_link]

Other Language: [中文](README_zh.md)

**AnalyzeSoGradlePlugin** is a Gradle plugin for analyzing the source and architecture of Android so libraries, helping developers better understand the so files introduced by various dependencies in their project.



## Features

### Implemented
- Output the source and architecture information of so libraries

### Planned
- Support for detecting 16KB memory page size to help adapt to Android 15 requirements([Support 16 KB page sizes](https://developer.android.com/guide/practices/page-sizes))
- More elegant output UI
- Issues and suggestions are welcome!

## Quick Start

This plugin is published on Maven Central: [![Download][version_icon]][version_link] (excluding 'v')

**How to apply the Gradle plugin:**

In your `build.gradle` file:

```kotlin
plugins {
    id("io.github.ravenliao.analyze-so") version "0.0.1"
}
```

## Special Thanks

- [mainlxl/AnalyzeSoPlugin](https://github.com/mainlxl/AnalyzeSoPlugin): This project references its implementation ideas

## License

Apache 2.0. See [LICENSE](LICENSE) for details.

[version_icon]: https://img.shields.io/maven-central/v/io.github.ravenliao.analyze-so/gradle-plugin
[version_link]: https://repo1.maven.org/maven2/io/github/ravenliao/analyze-so/gradle-plugin/
[license_image]: https://img.shields.io/badge/License-Apache%202-blue.svg
[license_link]: https://www.apache.org/licenses/LICENSE-2.0
