# Pinmap 项目

## 项目概述

Pinmap 是一个 Android 应用程序，使用现代 Android 开发技术栈构建。该项目是一个新初始化的项目，目前处于早期开发阶段。

### 技术栈

- **编程语言**: Kotlin 2.2.10
- **UI 框架**: Jetpack Compose (已配置，Material3)
- **构建系统**: Gradle 9.1.0 (Kotlin DSL)
- **最小 SDK**: 29 (Android 10)
- **目标 SDK**: 36 (Android 15)
- **包名**: com.sinus.pinmap

### 核心依赖

- AndroidX Core KTX 1.18.0
- AndroidX Lifecycle Runtime KTX 2.10.0
- AndroidX Activity Compose 1.13.0
- Jetpack Compose BOM 2024.09.00
- Material3
- JUnit 4.13.2 (单元测试)
- AndroidX Test 1.3.0 (仪器化测试)
- Espresso 3.7.0 (UI 测试)

## 项目结构

```
Pinmap/
├── app/                          # 主应用模块
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/sinus/pinmap/
│   │   │   │   └── MainActivity.kt       # 主 Activity
│   │   │   ├── res/                     # 资源文件
│   │   │   │   ├── drawable/            # 可绘制资源
│   │   │   │   ├── layout/              # 布局文件
│   │   │   │   ├── mipmap-*/            # 应用图标
│   │   │   │   ├── values/              # 值资源
│   │   │   │   └── xml/                 # XML 配置
│   │   │   └── AndroidManifest.xml      # 应用清单
│   │   ├── androidTest/                # 仪器化测试
│   │   └── test/                       # 单元测试
│   └── build.gradle.kts                # 应用级构建配置
├── gradle/
│   ├── libs.versions.toml              # 版本目录
│   └── wrapper/                        # Gradle Wrapper
├── build.gradle.kts                    # 项目级构建配置
├── settings.gradle.kts                 # Gradle 设置
├── gradlew                             # Gradle 包装器脚本 (Unix)
└── gradlew.bat                         # Gradle 包装器脚本 (Windows)
```

## 构建和运行

### 前置要求

- JDK 11 或更高版本
- Android SDK (API 29-36)
- Android Studio (推荐)

### 常用命令

#### 清理构建
```bash
./gradlew clean
```

#### 构建项目
```bash
# 构建所有变体
./gradlew build

# 构建 Debug 版本
./gradlew assembleDebug

# 构建 Release 版本
./gradlew assembleRelease
```

#### 运行测试
```bash
# 运行单元测试
./gradlew test

# 运行仪器化测试
./gradlew connectedAndroidTest
```

#### 安装到设备
```bash
# 安装 Debug 版本
./gradlew installDebug

# 安装 Release 版本
./gradlew installRelease
```

#### 其他有用命令
```bash
# 检查依赖更新
./gradlew dependencyUpdates

# 生成依赖报告
./gradlew :app:dependencies

# Lint 检查
./gradlew lint
```

## 开发规范

### 构建配置

- **版本管理**: 使用 Gradle Version Catalog (`gradle/libs.versions.toml`) 集中管理依赖版本
- **Kotlin DSL**: 所有 Gradle 配置使用 Kotlin DSL (`.gradle.kts` 文件)
- **代码混淆**: Release 构建默认关闭混淆 (isMinifyEnabled = false)，如需开启请修改 `app/build.gradle.kts`

### 代码结构

- **包名**: com.sinus.pinmap
- **主入口**: MainActivity
- **UI 开发**: 
  - 当前使用传统 XML 布局 (layout_main.xml)
  - 已配置 Jetpack Compose，可以逐步迁移到 Compose

### 主题和样式

- 应用主题: Theme.Pinmap (在 `res/values/themes.xml` 中定义)
- 支持深色主题 (如需要)
- 已配置 Jetpack Compose Material3 主题 (在 `ui/theme/` 目录下)

### 测试

- **单元测试**: 放置在 `app/src/test/` 目录
- **仪器化测试**: 放置在 `app/src/androidTest/` 目录
- 使用 JUnit 4 和 Espresso 进行测试
- 测试运行器: androidx.test.runner.AndroidJUnitRunner

### 配置文件说明

- **AndroidManifest.xml**: 定义应用组件、权限和元数据
- **proguard-rules.pro**: ProGuard 混淆规则
- **backup_rules.xml**: 备份规则配置
- **data_extraction_rules.xml**: 数据提取规则配置

## 当前状态

这是一个新初始化的项目，包含：
- 基础的 Gradle 构建配置
- 主 Activity (目前使用空布局)
- 完整的 Jetpack Compose 依赖配置
- 基础的测试框架设置
- 默认的应用图标和启动器配置

## Git 仓库信息

- 远程仓库: https://gitee.com/sinuss/pinmap.git
- 当前分支: master
- 最新提交: b1ccae4 (init repo)

## 注意事项

1. **Gradle Wrapper**: 项目包含 Gradle Wrapper，确保使用项目自带的 Gradle 版本
2. **Android SDK**: 确保本地安装了 Android SDK 并配置了正确的环境变量
3. **Compose 迁移**: 当前 MainActivity 使用传统布局，建议考虑迁移到 Jetpack Compose 以获得更现代的开发体验
4. **依赖管理**: 添加新依赖时，请更新 `gradle/libs.versions.toml` 文件以保持版本一致性

## 开发建议

1. **启用 Jetpack Compose**: 项目已配置 Compose，可以在新的 UI 组件中使用
2. **添加导航**: 考虑使用 Jetpack Navigation Compose 进行应用导航
3. **状态管理**: 根据需要选择合适的状态管理方案 (如 ViewModel + StateFlow)
4. **依赖注入**: 考虑使用 Hilt 或 Koin 进行依赖注入
5. **网络请求**: 如需要网络功能，建议使用 Retrofit + OkHttp
6. **本地存储**: 考虑使用 Room 数据库或 DataStore 进行本地数据存储