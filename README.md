# Pinmap

Android 地图标记应用。基于 Jetpack Compose + Material3 + 高德地图 3D SDK。

## 前提条件

- JDK 17+
- Android SDK (target 36)
- 高德开放平台 API Key

## 获取 API Key

1. 前往 [高德开放平台](https://lbs.amap.com/) 注册并创建应用
2. 应用类型选择 **Android**
3. 填写发布版 SHA1（`keytool -list -v -keystore ~/.android/debug.keystore`）
4. 获取 Key 后保存

## 编译安装

```bash
# 1. 配置 API Key
echo "MAPS_API_KEY=你的高德Key" >> local.properties

# 2. 编译 Debug APK
./gradlew assembleDebug

# 3. 安装
./gradlew installDebug
```

APK 路径：`app/build/outputs/apk/debug/app-debug.apk`

## 首次使用

1. 启动应用
2. 进入「设置」tab
3. 输入 API Key → 保存
4. **手动重启应用**使 Key 生效
5. 地图正常显示

Key 会自动保存，下次启动无需重新输入。可在设置页面随时修改。

## 基本操作

| 操作 | 说明 |
|------|------|
| 长按地图 | 创建标记 → 进入编辑页 |
| 点击标记 | 跳转编辑页 |
| 底部导航 | 地图 / 标记列表 / 分类管理 / 设置 |

## 构建命令

```bash
./gradlew assembleDebug    # 编译
./gradlew installDebug     # 安装
./gradlew lint             # 代码检查
./gradlew test             # 单元测试
```

## 项目结构

```
Pinmap/
├── app/
│   └── src/main/java/com/sinus/pinmap/
│       ├── MainActivity.kt        # 主入口
│       ├── PinmapApplication.kt   # Application
│       ├── data/                   # 数据库层 (Room)
│       ├── ui/
│       │   ├── screens/           # 页面
│       │   ├── navigation/        # 导航路由
│       │   ├── viewmodel/         # ViewModel
│       │   └── utils/             # 工具类
│       └── res/
└── gradle/
```

## 技术栈

- Kotlin / Jetpack Compose / Material3
- AMap 3D SDK / 定位 SDK
- Room (SQLite)
- Navigation Compose
- Coil (图片加载)
