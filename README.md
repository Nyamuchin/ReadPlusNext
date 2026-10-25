# ReadPlusNext

完全离线的本地漫画 / 视频管理器，使用 Kotlin + Jetpack Compose 实现，界面遵循 **Material 3 Expressive** 设计语言。应用不申请 `INTERNET` 权限，所有数据都保存在设备本地。

## 功能

### 漫画
- 导入 `zip / cbz / cbr / 7z / cb7 / pdf / epub / mobi / azw / azw3`，支持多选导入与文件夹批量导入。
- 归档格式自动嗅探（扩展名 + 魔数），ZIP/CBZ 直接读取；CBR 经 junrar 解包、7z/CB7 经 commons-compress 解包、EPUB 按 spine 顺序解析、MOBI/AZW 扫描 PalmDOC 图像记录，随后统一重打包为私有 ZIP；PDF 保留原文件并用 `PdfRenderer` 按页渲染。
- 页面按自然顺序排序（`page_2` 在 `page_10` 之前），封面存于 `filesDir/covers/`，页码缩略图带磁盘缓存。
- 详情页：封面预览、从任意页或图片文件更换封面、4 列页面预览、阅读进度与「开始 / 继续阅读」。
- 阅读器：左右翻页 / 右到左 / 纵向滚动三种模式，双指缩放 1×–5×、双击 2.5×，沉浸式全屏与可显隐工具栏，自动记录阅读进度。

### 视频
- 导入 `mp4 / mkv / webm / avi`，支持多选与 SAF 文件夹批量扫描（记住文件夹授权）。
- 使用 Media3 ExoPlayer 全屏竖滑播放，每页独立播放器并在离开时释放；TextureView 首帧回调后封面淡出、循环播放、80ms 刷新的极细进度线，暂停时展开可拖动的精确进度条。
- 记住每个视频的播放位置，再次进入自动续播。

### 分类与首页
- 漫画、视频各自独立的分类，支持新建、重命名、删除、多选归类和首页筛选。
- 首页大标题 + Material 3 Expressive `ButtonGroup` 双标签页、分类胶囊、封面网格与底部 `HorizontalFloatingToolbar`。
- **首页网格列数可调**：漫画与视频各自支持 2–5 列（默认 3 列），可在首页「布局」底部面板或设置页调整，选择通过 DataStore 持久化。

### 备份与设置
- 一键完整导出：数据库、封面、漫画文件、视频文件打包为 ZIP（通过 SAF 保存），带实时进度。
- 完整导入：校验后恢复文件并事务性重建数据库，视频地址改写为 `file://`，导入前二次确认。
- 设置页：Material 3 / HyperOS 两种主题风格、浅色 / 深色 / 跟随系统、阅读方向、网格列数、备份恢复。
- Material 3 分支使用 `MaterialExpressiveTheme` + `MotionScheme.expressive()` + Expressive 形状与排版，Android 12+ 自动动态取色；HyperOS 分支使用自建配色。

## 构建

```bash
# 需要 JDK 17+ 与 Android SDK 35
export JAVA_HOME=/path/to/jdk
./gradlew :app:assembleDebug
```

产物位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 技术栈

Kotlin 2.0.21 · Jetpack Compose (BOM 2024.12.01) · Material 3 1.4.0-alpha10（Expressive API）· Hilt · Room · DataStore · Navigation Compose · Media3 ExoPlayer · Coil · junrar · commons-compress · Gson

架构为单模块 Clean Architecture + MVVM：`domain`（模型与仓库接口）、`data`（Room、DataStore、归档解析、备份、仓库实现）、`ui`（Compose 页面、主题、组件）、`di`（Hilt 模块）。UI 层不直接访问数据库或文件系统。

## 单元测试

```bash
./gradlew :app:testDebugUnitTest
```

覆盖自然排序、哈希、文件名处理、归档格式嗅探与网格列数约束。
