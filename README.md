# AnalyzeSoGradlePlugin

[![License][license_image]][license_link]

Other Language: [中文](README_zh.md)

**AnalyzeSoGradlePlugin** is a Gradle plugin for analyzing the source and architecture of Android so libraries, helping developers better understand the so files introduced by various dependencies in their project.



## Features

### Implemented
- Output the source and architecture information of so libraries
- Detect 16KB memory page size to help adapt to Android 15 requirements([Support 16 KB page sizes](https://developer.android.com/guide/practices/page-sizes))
- Beautiful HTML report output to quickly identify native libraries that are not compatible with Android 15 and their sources
- Add task to aggregate all variants and generate a combined HTML report
- Integrate with [LibChecker-Rules](https://github.com/LibChecker/LibChecker-Rules) database to provide more detailed library information in the HTML report

### Planned
- Issues and suggestions are welcome!

## Prerequisites (objdump)

Purpose: the plugin runs `objdump -p` to parse ELF Program Headers for `Align 2**N` (page size). If you only need the so library source and architecture info, you can ignore this.

### Linux
- Install binutils if missing:
  - Debian/Ubuntu: `sudo apt-get install binutils`
  - Fedora/RHEL/CentOS: `sudo dnf install binutils`
  - Arch: `sudo pacman -S binutils`

### macOS
- Usually works out of the box. If needed: `brew install binutils`.

### Windows (GUI)
1) Download and run the TDM-GCC installer: https://jmeubank.github.io/tdm-gcc/
2) During setup, choose "Add to PATH" (or the equivalent option).
3) Finish installation. `objdump.exe` will be available to Gradle.


## Quick Start
**How to apply the Gradle plugin:**

This plugin is published on Maven Central:

${LAST_VERSION}: [![Download][version_icon]][version_link] (excluding 'v')

In your `build.gradle` file:

```kotlin
plugins {
    id("io.github.ravenliao.analyze-so") version "$LAST_VERSION"
}
```

## Usage

After applying the plugin, Gradle will automatically generate analysis tasks for each build variant:

```bash
# Analyze so files for a specific variant
./gradlew analyze[VariantName]So

# For example, analyze debug variant
./gradlew analyzeDebugSo

# Analyze so files for all variant
./gradlew analyzeSo
```

After running the analysis, the path to the detailed HTML report will be shown at the end of the task output. The report is typically located at:
```
app/build/reports/analyze-so/[VariantName]/analyze-so-report.html
```

For example, for the debug variant, the report will be at:
```
app/build/reports/analyze-so/Debug/analyze-so-report.html
```

## Special Thanks

- [mainlxl/AnalyzeSoPlugin](https://github.com/mainlxl/AnalyzeSoPlugin): This project references its implementation ideas
- [LibChecker/LibChecker-Rules](https://github.com/LibChecker/LibChecker-Rules): Thank for so info database

## License

Apache 2.0. See [LICENSE](LICENSE) for details.

[version_icon]: https://img.shields.io/maven-central/v/io.github.ravenliao.analyze-so/gradle-plugin
[version_link]: https://repo1.maven.org/maven2/io/github/ravenliao/analyze-so/gradle-plugin/
[license_image]: https://img.shields.io/badge/License-Apache%202-blue.svg
[license_link]: https://www.apache.org/licenses/LICENSE-2.0
