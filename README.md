# 悬浮歌词

一款安卓端高版本可用的悬浮窗歌词APP，能读取系统正在播放的歌曲，并自动从网上下载、本地缓存获取歌词并按当前播放进度显示歌词。

## 功能特性

- 系统音乐播放监听
- 自动下载歌词（支持网易云音乐API）
- 本地歌词缓存
- 悬浮窗歌词显示
- 歌词样式自定义（字体大小、颜色、背景色）
- 歌词动画效果（淡入淡出、滑动、缩放）
- 悬浮窗位置调节（支持拖拽和虚拟按键微调）
- 本地音乐扫描和歌词下载
- 歌词搜索功能

## 项目结构

```
Lyric-Auto/
├── app/
│   ├── src/main/
│   │   ├── java/com/lyricauto/
│   │   │   ├── adapter/              # RecyclerView适配器
│   │   │   │   ├── LyricSearchAdapter.kt
│   │   │   │   └── LocalMusicAdapter.kt
│   │   │   ├── model/                # 数据模型
│   │   │   │   ├── MusicInfo.kt
│   │   │   │   ├── Lyric.kt
│   │   │   │   ├── FloatWindowSettings.kt
│   │   │   │   └── LocalMusic.kt
│   │   │   ├── network/              # 网络请求
│   │   │   │   └── LyricDownloader.kt
│   │   │   ├── service/              # 服务
│   │   │   │   ├── MusicListenerService.kt
│   │   │   │   ├── LyricFloatService.kt
│   │   │   │   └── LyricDownloadService.kt
│   │   │   ├── utils/                # 工具类
│   │   │   │   ├── LyricParser.kt
│   │   │   │   ├── LyricCacheManager.kt
│   │   │   │   ├── SharedPreferencesManager.kt
│   │   │   │   ├── PermissionHelper.kt
│   │   │   │   └── MusicMetadataExtractor.kt
│   │   │   ├── MainActivity.kt       # 主界面
│   │   │   ├── SettingsActivity.kt   # 设置界面
│   │   │   ├── LyricSearchActivity.kt # 歌词搜索界面
│   │   │   ├── LocalMusicActivity.kt # 本地音乐界面
│   │   │   └── NotificationListenerService.kt
│   │   ├── res/                      # 资源文件
│   │   │   ├── layout/
│   │   │   ├── values/
│   │   │   └── drawable/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 核心功能说明

### 1. 音乐监听服务 (MusicListenerService)
- 监听系统音乐播放状态变化
- 支持多种音乐播放器的广播监听
- 使用MediaSession API获取当前播放信息
- 前台服务保证持续运行

### 2. 悬浮窗服务 (LyricFloatService)
- 创建系统级悬浮窗
- 支持拖拽移动
- 支持虚拟按键微调位置
- 实时同步歌词显示
- 支持多种动画效果

### 3. 歌词下载服务 (LyricDownloadService)
- 支持网易云音乐API
- 自动下载歌词
- 本地缓存管理
- 支持手动搜索和下载

### 4. 歌词解析 (LyricParser)
- 支持LRC格式歌词解析
- 支持歌词标签提取（标题、歌手、专辑）
- 时间标签解析和同步

### 5. 权限管理
- 悬浮窗权限
- 存储权限
- 通知权限
- 通知监听权限

## 使用说明

### 首次使用
1. 打开应用，授予悬浮窗权限
2. 授予通知监听权限（可选，用于更精确的音乐监听）
3. 点击"开启悬浮窗"按钮
4. 开始播放音乐，悬浮窗会自动显示歌词

### 自定义设置
1. 点击"设置"按钮进入设置界面
2. 调整歌词样式（字体大小、颜色、背景色）
3. 选择动画效果（无动画、淡入淡出、滑动、缩放）
4. 调整悬浮窗位置（顶部、居中、底部、自定义）
5. 开启/关闭自动下载和缓存功能

### 搜索歌词
1. 点击"搜索歌词"按钮
2. 输入歌曲名或歌手名
3. 点击搜索按钮
4. 预览或下载歌词

### 本地音乐
1. 点击"本地音乐"按钮
2. 授予存储权限
3. 点击"扫描本地音乐"
4. 为每首歌曲搜索和下载歌词

## 技术栈

- Kotlin
- Android SDK 34 (minSdk 24)
- Material Design Components
- OkHttp (网络请求)
- Gson (JSON解析)
- Coroutines (异步处理)

## 依赖库

```kotlin
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.appcompat:appcompat:1.6.1")
implementation("com.google.android.material:material:1.11.0")
implementation("androidx.constraintlayout:constraintlayout:2.1.4")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
implementation("androidx.lifecycle:lifecycle-service:2.7.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.google.code.gson:gson:2.10.1")
implementation("androidx.preference:preference-ktx:1.2.1")
implementation("androidx.recyclerview:recyclerview:1.3.2")
```

## 构建说明

1. 确保已安装Android Studio
2. 打开项目
3. 等待Gradle同步完成
4. 连接Android设备或启动模拟器
5. 点击运行按钮

## 注意事项

- 需要Android 7.0 (API 24) 或更高版本
- 首次使用需要授予悬浮窗权限
- 某些音乐播放器可能需要授予通知监听权限
- 歌词下载依赖网络连接
- 本地音乐扫描需要授予存储权限

## 许可证

本项目仅供学习和研究使用。
