# Pinmap 项目

## 项目概述

Pinmap 是一个基于高德地图的 Android 地图标记应用，使用现代 Android 开发技术栈构建。用户可以在地图上创建标记、分类管理，并为每个标记添加自定义字段（文本、数字、图片）。

### 技术栈

- **编程语言**: Kotlin 2.2.20
- **UI 框架**: Jetpack Compose (Material3)
- **构建系统**: Gradle 9.1.0 (Kotlin DSL)
- **最小 SDK**: 29 (Android 10)
- **目标 SDK**: 36 (Android 15)
- **包名**: com.sinus.pinmap
- **地图服务**: 高德地图 AMap 3D SDK 9.1.0
- **本地数据库**: Room 2.7.0

### 核心依赖

- AndroidX Core KTX 1.18.0
- AndroidX Lifecycle Runtime KTX 2.10.0
- AndroidX Activity Compose 1.13.0
- Jetpack Compose BOM 2024.09.00
- Material3
- 高德地图 3D SDK 9.1.0
- 高德定位 SDK 6.4.5
- 高德搜索 SDK 9.7.1
- Room 数据库 2.7.0 (运行时 + 编译器 + KTX)
- Navigation Compose 2.8.4
- Coil Compose 2.7.0 (图片加载)
- JUnit 4.13.2 (单元测试)
- AndroidX Test 1.3.0 (仪器化测试)
- Espresso 3.7.0 (UI 测试)

## 项目结构

```
Pinmap/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/sinus/pinmap/
│   │   │   │   ├── MainActivity.kt                  # 主 Activity
│   │   │   │   ├── PinmapApplication.kt             # Application 类
│   │   │   │   ├── data/
│   │   │   │   │   ├── database/
│   │   │   │   │   │   ├── PinmapDatabase.kt       # Room 数据库配置
│   │   │   │   │   │   └── Converters.kt           # 类型转换器
│   │   │   │   │   ├── dao/
│   │   │   │   │   │   ├── PinDao.kt              # 标记数据访问对象
│   │   │   │   │   │   ├── CategoryDao.kt         # 分类数据访问对象
│   │   │   │   │   │   ├── FieldTemplateDao.kt    # 字段模板数据访问对象
│   │   │   │   │   │   ├── FieldValueDao.kt       # 字段值数据访问对象
│   │   │   │   │   │   ├── AttachmentDao.kt       # 附件数据访问对象
│   │   │   │   │   │   └── OfflineMapDao.kt       # 离线地图数据访问对象
│   │   │   │   │   ├── entity/
│   │   │   │   │   │   ├── Pin.kt                 # 标记实体
│   │   │   │   │   │   ├── Category.kt            # 分类实体
│   │   │   │   │   │   ├── FieldTemplate.kt       # 字段模板实体
│   │   │   │   │   │   ├── FieldValue.kt          # 字段值实体
│   │   │   │   │   │   ├── FieldType.kt           # 字段类型枚举
│   │   │   │   │   │   ├── Attachment.kt          # 附件实体
│   │   │   │   │   │   └── OfflineMap.kt          # 离线地图实体
│   │   │   │   │   └── repository/
│   │   │   │   │       ├── PinRepository.kt        # 标记数据仓库
│   │   │   │   │       ├── CategoryRepository.kt   # 分类数据仓库
│   │   │   │   │       ├── FieldTemplateRepository.kt  # 字段模板数据仓库
│   │   │   │   │       └── FieldValueRepository.kt    # 字段值数据仓库
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/
│   │   │   │   │   │   └── NavigationDrawer.kt     # 侧边栏导航组件
│   │   │   │   │   ├── model/
│   │   │   │   │   │   └── FieldData.kt            # 字段数据模型
│   │   │   │   │   ├── navigation/
│   │   │   │   │   │   ├── NavGraph.kt             # 导航图
│   │   │   │   │   │   └── Screen.kt               # 路由定义
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── MapScreen.kt            # 地图页面
│   │   │   │   │   │   ├── PinListScreen.kt        # 标记列表页面
│   │   │   │   │   │   ├── PinDetailScreen.kt     # 标记详情页面
│   │   │   │   │   │   ├── CategoryListScreen.kt   # 分类列表页面
│   │   │   │   │   │   ├── OfflineMapScreen.kt     # 离线地图页面
│   │   │   │   │   │   ├── CreatePinDialog.kt      # 创建标记对话框
│   │   │   │   │   │   ├── CreateCategoryDialog.kt # 创建分类对话框
│   │   │   │   │   │   ├── CreateFieldTemplateDialog.kt  # 创建字段对话框
│   │   │   │   │   │   └── FieldValueEditor.kt     # 字段值编辑组件
│   │   │   │   │   ├── viewmodel/
│   │   │   │   │   │   ├── MapViewModel.kt          # 地图页面视图模型
│   │   │   │   │   │   ├── PinListViewModel.kt      # 标记列表视图模型
│   │   │   │   │   │   ├── PinDetailViewModel.kt   # 标记详情视图模型
│   │   │   │   │   │   ├── CategoryViewModel.kt     # 分类视图模型
│   │   │   │   │   │   └── ViewModelFactory.kt     # 视图模型工厂
│   │   │   │   │   ├── theme/
│   │   │   │   │   │   ├── Theme.kt                # 主题配置
│   │   │   │   │   │   ├── Color.kt                # 颜色定义
│   │   │   │   │   │   └── Type.kt                 # 字体定义
│   │   │   │   │   └── utils/
│   │   │   │   │       ├── LocationManager.kt       # 位置管理器
│   │   │   │   │       └── OfflineMapManager.kt    # 离线地图管理器
│   │   │   ├── res/                                # 资源文件
│   │   │   │   ├── drawable/
│   │   │   │   ├── layout/
│   │   │   │   ├── mipmap-*/
│   │   │   │   ├── values/
│   │   │   │   └── xml/
│   │   │   └── AndroidManifest.xml                  # 应用清单
│   │   ├── androidTest/                             # 仪器化测试
│   │   └── test/                                    # 单元测试
│   └── build.gradle.kts                             # 应用级构建配置
├── gradle/
│   ├── libs.versions.toml                           # 版本目录
│   └── wrapper/                                     # Gradle Wrapper
├── build.gradle.kts                                 # 项目级构建配置
├── settings.gradle.kts                              # Gradle 设置
├── gradlew                                          # Gradle 包装器脚本 (Unix)
├── gradlew.bat                                      # Gradle 包装器脚本 (Windows)
├── AGENTS.md                                        # 项目文档
└── README.md                                        # 项目说明

## 构建和运行

### 前置要求

- JDK 11 或更高版本
- Android SDK (API 29-36)
- Android Studio (推荐)
- 高德地图开发者账号（需要 API Key）

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
- **KSP**: 使用 KSP (Kotlin Symbol Processing) 进行 Room 数据库编译时注解处理

### 代码结构

- **包名**: com.sinus.pinmap
- **主入口**: MainActivity
- **UI 开发**: 
  - 完全使用 Jetpack Compose 构建 UI
  - Material3 设计规范
  - 导航使用 Jetpack Navigation Compose
  - 状态管理使用 ViewModel + StateFlow
- **数据层**:
  - Room 数据库作为本地存储
  - Repository 模式封装数据访问
  - DAO (Data Access Object) 模式

### 数据库架构

- **数据库版本**: 2
- **实体**: Pin, Category, FieldTemplate, FieldValue, Attachment, OfflineMap
- **关系**:
  - Pin 属于一个 Category (多对一)
  - Pin 可以有多个 FieldTemplate (通过 FieldValue 关联)
  - FieldTemplate 可以是类别的通用模板 (categoryId != null) 或自定义 (categoryId == null)
- **外键约束**: 使用 `@ForeignKey` 保证数据完整性

### 主题和样式

- 应用主题: Theme.Pinmap (在 `ui/theme/Theme.kt` 中定义)
- Material3 设计规范
- 支持颜色主题定制
- 支持字体大小调整

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

## 核心功能

### 1. 地图功能

- 使用高德地图 3D SDK
- 支持地图手势操作（缩放、平移、旋转）
- 支持定位功能（需要位置权限）
- 支持地图长按创建标记
- 支持搜索标记并跳转
- 支持保存和恢复地图位置

### 2. 标记管理

- 创建标记：长按地图任意位置
- 编辑标记：点击标记图标
- 删除标记：在标记列表或详情页面
- 标记属性：标题、坐标、分类

### 3. 分类管理

- 创建分类：自定义名称和颜色
- 分类颜色：使用圆形颜色标识
- 分类过滤：标记列表按分类筛选

### 4. 自定义字段系统

- **字段类型**: 文本、数字、图片
- **字段模板**: 可以为类别创建通用字段模板
- **自定义字段**: 可以为单个标记添加独立字段
- **字段值**: 每个标记的字段值独立存储

### 5. 图片管理

- 图片存储：应用私有存储 (`/data/data/com.sinus.pinmap/files/images/`)
- 图片格式：JPEG
- 图片显示：支持预览和全屏查看
- 图片上传：支持从相册选择

### 6. 导航结构

- 侧边栏导航：地图、标记列表、分类列表、离线地图
- 标记详情页面：显示标记信息和自定义字段
- 懒加载机制：避免重复加载

## 当前状态

项目已完成核心功能开发：
- ✅ 地图集成（高德地图 3D SDK）
- ✅ 标记 CRUD 功能
- ✅ 分类管理功能
- ✅ 自定义字段系统
- ✅ 图片上传和查看功能
- ✅ 搜索功能
- ✅ 侧边栏导航
- ✅ 数据库持久化
- ✅ 权限管理（位置、存储）

## Git 仓库信息

- 远程仓库: https://gitee.com/sinuss/pinmap.git
- 当前分支: master
- 最新提交: 890001c (add database foreign constraints)

## 注意事项

1. **Gradle Wrapper**: 项目包含 Gradle Wrapper，确保使用项目自带的 Gradle 版本
2. **Android SDK**: 确保本地安装了 Android SDK 并配置了正确的环境变量
3. **高德地图 API Key**: 需要申请高德地图开发者账号并配置 API Key
4. **依赖管理**: 添加新依赖时，请更新 `gradle/libs.versions.toml` 文件以保持版本一致性
5. **数据库迁移**: 数据库升级时需要正确处理迁移逻辑
6. **权限管理**: 位置权限和存储权限需要运行时请求
7. **图片存储**: 图片存储在应用私有目录，应用卸载时会自动清除

## 开发建议

1. **添加新字段类型**: 在 `FieldType` 枚举中添加新类型，并在 `FieldValueEditor` 中实现对应的 UI
2. **批量操作**: 可以考虑为标记列表添加批量删除功能
3. **数据导出**: 可以考虑添加标记数据导出功能（JSON、CSV 等格式）
4. **云同步**: 可以考虑添加数据云同步功能
5. **标记分享**: 可以考虑添加标记分享功能
6. **路线规划**: 可以利用高德地图 SDK 添加路线规划功能
7. **离线地图**: 可以完善离线地图下载和管理功能
8. **性能优化**: 对于大量标记的场景，考虑使用分页加载或虚拟滚动