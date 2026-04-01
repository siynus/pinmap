# Pinmap - 地图标记应用

一个基于 Android 的地图标记应用，让用户可以在地图上标记位置、记录信息，并进行分类管理。

## 项目概述

Pinmap 是一个完全本地化的地图标记应用，允许用户在地图上点击任意位置创建标记，为每个标记添加文字、图片、语音等多媒体信息，并支持自定义字段和分类管理。所有数据存储在用户设备本地，无需联网，保护用户隐私。

## 核心功能

### 1. 地图交互
- 使用高德地图 SDK
- 支持点击地图任意位置创建新标记
- 点击现有标记查看详情
- 标记以图标形式显示在地图上

### 2. 标记信息管理
- **文字记录**：支持多行文本输入
- **图片支持**：支持拍摄照片或从相册选择图片
- **语音记录**：
  - 支持录音并保存为音频文件
  - 支持语音转文字功能
  - 可播放已录制的音频
- **自定义字段**：每个标记可以独立添加任意数量的自定义字段
  - 字段名称自定义
  - 字段类型：文本、数字、日期、单选、多选等

### 3. 分类管理
- **完全自定义分类**：用户可以自由创建、编辑、删除分类
- **无预设分类**：所有分类由用户创建，没有固定的默认分类
- **分类筛选**：根据分类显示或隐藏标记
- **分类颜色**：不同分类使用不同颜色的标记图标
- **未分类标记**：不选择分类的标记将显示默认样式

### 4. 编辑功能
- 编辑现有标记的所有信息
- 添加、修改、删除自定义字段
- 更新图片、语音等多媒体内容
- 更改标记分类
- 删除标记

### 5. 数据展示
- 点击地图标记弹出详细信息窗口
- 支持标记列表页面
- 支持按分类筛选标记
- 支持搜索标记（按标题、字段内容等）

## 技术栈

### 核心框架
- **开发语言**：Kotlin
- **UI 框架**：Jetpack Compose + Material3
- **构建工具**：Gradle (Kotlin DSL)
- **最低 SDK**：29 (Android 10)
- **目标 SDK**：36 (Android 15)

### 主要依赖
- **高德地图 SDK**：地图展示和交互
- **Room Database**：本地数据存储
- **Jetpack Navigation**：页面导航
- **Coil**：图片加载
- **ExoPlayer**：音频播放
- **CameraX**：相机拍照（可选）

### 数据存储
- **数据库**：Room（SQLite）
- **文件存储**：
  - 图片：`/data/data/com.sinus.pinmap/files/images/`
  - 音频：`/data/data/com.sinus.pinmap/files/audio/`
  - 离线地图：`/data/data/com.sinus.pinmap/files/maps/`
- **语音识别**：使用 Android 原生 SpeechRecognizer API（仅支持中文）
- **完全本地化**：所有数据存储在本地，无云端同步，无需网络连接（除地图离线下载外）

## 数据模型

### 标记（Pin）
```kotlin
data class Pin(
    id: Long,
    latitude: Double,
    longitude: Double,
    title: String,
    description: String?,
    categoryId: Long?,
    createdAt: Long,
    updatedAt: Long
)
```

### 自定义字段（CustomField）
```kotlin
data class CustomField(
    id: Long,
    pinId: Long,
    fieldName: String,
    fieldType: FieldType, // TEXT, NUMBER, DATE, SINGLE_CHOICE, MULTI_CHOICE
    value: String?,
    options: String? // 用于单选/多选的选项
)
```

### 附件（Attachment）
```kotlin
data class Attachment(
    id: Long,
    pinId: Long,
    type: AttachmentType, // IMAGE, AUDIO, VIDEO
    filePath: String,
    fileName: String,
    fileSize: Long,
    duration: Long?, // 音频/视频时长（毫秒）
    transcription: String?, // 语音转文字结果
    createdAt: Long
)
```

### 分类（Category）
```kotlin
data class Category(
    id: Long,
    name: String,
    color: Int, // 标记颜色
    icon: String?, // 标记图标
    createdAt: Long,
    updatedAt: Long
)
```

### 离线地图（OfflineMap）
```kotlin
data class OfflineMap(
    id: Long,
    cityName: String,
    cityCode: String,
    filePath: String,
    fileSize: Long,
    downloadDate: Long,
    version: String, // 地图版本
    isDownloaded: Boolean,
    downloadProgress: Int // 0-100
)
```

## 应用架构

### UI 结构
```
MainActivity
├── MapScreen (地图页面)
│   ├── 高德地图组件
│   └── 标记图层
├── PinDetailScreen (标记详情)
│   ├── 标记信息展示
│   ├── 附件列表
│   ├── 自定义字段列表
│   └── 编辑按钮
├── PinEditScreen (标记编辑)
│   ├── 基本信息（标题、描述）
│   ├── 附件管理
│   ├── 自定义字段管理
│   └── 分类选择
├── CategoryListScreen (分类列表)
│   ├── 分类列表（完全自定义）
│   └── 添加/编辑/删除分类
└── PinListScreen (标记列表)
    ├── 筛选器
    └── 标记列表
```

### 导航流程
1. 启动应用 → 显示地图页面
2. 点击地图空白处 → 创建新标记 → 进入编辑页面
3. 点击现有标记 → 显示详情弹窗
4. 详情弹窗中点击编辑 → 进入编辑页面
5. 从菜单进入分类管理 → 分类列表
6. 从菜单进入标记列表 → 标记列表页面

## 开发计划

### Phase 1: 基础功能
- [ ] 集成高德地图 SDK
- [ ] 实现数据库模型和 DAO
- [ ] 实现地图点击创建标记
- [ ] 实现标记基本信息的编辑（标题、描述）
- [ ] 实现标记详情展示

### Phase 2: 多媒体支持
- [ ] 实现图片拍照和选择功能
- [ ] 实现音频录制功能
- [ ] 实现音频播放功能
- [ ] 集成语音转文字功能
- [ ] 实现附件管理（添加、删除、查看）

### Phase 3: 自定义字段
- [ ] 实现自定义字段的增删改查
- [ ] 实现不同字段类型的输入组件
- [ ] 在编辑页面展示自定义字段列表

### Phase 4: 分类管理
- [ ] 实现分类的增删改查
- [ ] 实现分类颜色和图标选择
- [ ] 实现按分类筛选标记
- [ ] 在地图上显示不同分类的标记

### Phase 5: 优化和完善
- [ ] 实现标记搜索功能（按标题、描述、字段内容搜索）
- [ ] 添加离线地图下载功能
  - 支持按城市/区域下载
  - 显示下载进度和存储空间占用
  - 支持删除已下载的离线地图
  - 支持更新已下载的离线地图
- [ ] 添加标记列表页面
- [ ] 优化 UI/UX
- [ ] 添加单元测试和 UI 测试

## 权限需求

应用需要以下权限：

```xml
<!-- 网络权限（仅用于离线地图下载，应用本身无需网络） -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- 存储权限（保存图片、音频、离线地图） -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />

<!-- 相机权限（拍照） -->
<uses-permission android:name="android.permission.CAMERA" />

<!-- 录音权限（录制语音） -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- 位置权限（可选，用于定位当前位置） -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

**注意**：应用完全本地化，除离线地图下载外，无需网络连接即可正常使用。

## 使用说明

### 创建标记
1. 在地图上点击想要标记的位置
2. 填写标记标题和描述
3. 添加图片、音频或语音
4. 添加自定义字段（可选）
5. 选择分类（可选）
6. 保存标记

### 编辑标记
1. 点击地图上的标记查看详情
2. 点击编辑按钮
3. 修改需要更新的内容
4. 保存更改

### 管理分类
1. 从菜单进入"分类管理"
2. 点击"+"创建新分类
3. 设置分类名称、颜色和图标
4. 保存分类

### 筛选标记
1. 在地图页面点击筛选按钮
2. 选择要显示的分类
3. 地图将只显示选定分类的标记

### 下载离线地图
1. 从菜单进入"离线地图"
2. 选择要下载的城市或区域
3. 点击下载按钮
4. 等待下载完成
5. 下载后可在无网络环境下使用地图

## 注意事项

### 高德地图 SDK 配置
- 需要在高德开放平台申请 API Key
- 在 `AndroidManifest.xml` 中配置高德地图 Key
- 需要配置高德地图的 SHA1 签名
- 支持离线地图下载功能

### 数据安全
- 所有数据存储在本地，无云端同步
- 完全保护用户隐私
- 卸载应用会清除所有数据（包括标记、图片、音频、离线地图）
- 建议定期备份数据（后续版本将提供导出功能）

### 性能优化
- 图片会自动压缩以节省存储空间
- 音频文件建议控制在合理大小内
- 标记数量过多时建议使用分类筛选
- 离线地图占用存储空间较大，建议按需下载

## 贡献指南

欢迎提交 Issue 和 Pull Request！

## 许可证

待定

## 联系方式

如有问题或建议，请通过以下方式联系：
- 提交 Issue
- 发送邮件