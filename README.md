# Pinmap

Android 地图标记应用：在地图上记录位置、分类整理、附加自定义字段与媒体文件，并支持一键导出备份。

<p align="center"><img src="images/screenshot-map.png" width="300"/></p>

## 功能

- **地图**：长按任意位置创建标记，点击标记查看/编辑，长按标记拖拽移动
- **搜索与定位**：底部搜索栏支持 POI 检索，一键回到当前位置
- **标记管理**：标记列表快速浏览，支持标题、描述、分类整理
- **自定义字段**：按分类配置字段模板（文本 / 数字 / 日期 / 单选 / 多选 / 图片）
- **媒体附件**：为标记附加图片等文件
- **数据导出**：全部 / 按分类 / 单条标记，导出为 `.pinmap` 备份文件

## 页面与操作

### 地图

| 操作     | 说明               |
|--------|------------------|
| 长按地图   | 创建标记 → 进入编辑页     |
| 点击标记   | 打开标记编辑页          |
| 长按标记拖拽 | 移动标记位置           |
| 底部搜索栏  | POI 搜索，点击结果定位并预览 |

### 标记编辑页

- 设置标题、描述、所属分类
- 按分类字段模板填写自定义字段
- 附加图片等媒体文件
- 右上角分享图标：导出单条标记为 `.pinmap` 文件

<p align="center"><img src="images/screenshot-edit.png" width="300"/></p>

### 标记列表

- 浏览全部标记，点击进入编辑页

<p align="center"><img src="images/screenshot-list.png" width="300"/></p>

### 分类管理

- 新增 / 重命名 / 删除分类
- 管理该分类的字段模板
- 右上角分享图标：导出该分类下全部标记

<p align="center"><img src="images/screenshot-category.png" width="300"/></p>

### 设置

- 展示当前生效的 API Key（编译时配置，不可在线修改）
- 导出全部标记数据

<p align="center"><img src="images/screenshot-settings.png" width="300"/></p>

## 数据与备份

- 所有数据保存在本机 SQLite（Room）
- 导出文件为 `.pinmap`（ZIP 格式），包含数据 JSON 与媒体文件，可用于备份或迁移
- 导出过程中可随时取消

## 编译安装

```bash
# 1. 配置高德地图 API Key
echo "MAPS_API_KEY=你的高德Key" >> local.properties

# 2. 编译 Debug APK
./gradlew assembleDebug

# 3. 安装
./gradlew installDebug
```

API Key 通过 [高德开放平台](https://lbs.amap.com/) 申请（应用类型选 Android，填入调试/发布 SHA1）。
