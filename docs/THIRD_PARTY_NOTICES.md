# 第三方代码声明

本项目包含来自以下开源项目的代码，按各自原始许可证进行分发。

## scrcpy

- **项目地址**：[Genymobile/scrcpy](https://github.com/Genymobile/scrcpy)
- **版权**：Copyright 2018 Genymobile
- **许可证**：[Apache License 2.0](../LICENSE-Apache-2.0)
- **原始代码**：[server/src/main/java/com/genymobile/scrcpy](https://github.com/Genymobile/scrcpy/tree/master/server/src/main/java/com/genymobile/scrcpy)
- **本项目中的位置**：[`app/src/main/java/com/aliothmoon/maameow/third/`](../app/src/main/java/com/aliothmoon/maameow/third/)

### 用途

这些代码用于在 Shizuku 用户服务进程中构造 Android `Context`，并通过反射访问 Android Hidden API，实现虚拟显示器管理、输入事件注入、屏幕信息获取等功能。

### 包含文件

| 文件 | 说明 |
|------|------|
| `third/FakeContext.java` | 伪造的 Android Context，用于在无 Activity 的进程中获取系统服务 |
| `third/Ln.java` | 日志工具，同时输出到 Android Logger 和标准输出 |
| `third/Workarounds.java` | Android 系统兼容性处理，构造 ActivityThread 和 Looper |
| `third/Command.java` | Shell 命令执行工具 |
| `third/IO.java` | I/O 工具类 |
| `third/Size.java` | 尺寸数据类 |
| `third/DisplayInfo.java` | 显示器信息数据类 |
| `third/wrappers/ServiceManager.java` | Android ServiceManager 反射封装，获取各系统服务实例 |
| `third/wrappers/DisplayManager.java` | DisplayManagerGlobal 反射封装，管理显示器信息和虚拟显示器 |
| `third/wrappers/InputManager.java` | InputManager 反射封装，注入输入事件 |
| `third/wrappers/WindowManager.java` | IWindowManager 反射封装，管理旋转、显示尺寸、IME 策略等 |
| `third/wrappers/ActivityManager.java` | ActivityManagerNative 反射封装，获取 ContentProvider、启动 Activity |
| `third/wrappers/PowerManager.java` | IPowerManager 反射封装，查询屏幕状态 |
| `third/wrappers/StatusBarManager.java` | IStatusBarService 反射封装，控制通知栏和快捷设置面板 |
| `third/wrappers/SurfaceControl.java` | SurfaceControl 反射封装，管理物理显示器令牌和电源模式 |

### 主要修改

以下是相对于 scrcpy 原始代码的主要修改：

- 调整包名从 `com.genymobile.scrcpy` 至 `com.aliothmoon.maameow.third`
- 移除了与屏幕录制、视频编码、音频采集相关的代码，仅保留系统服务反射封装部分
- 新增 `ActivityManager`、`SurfaceControl`、`StatusBarManager`、`PowerManager` 等封装类
- `DisplayManager` 增加了 `createNewVirtualDisplay()` 方法，用于创建独立虚拟显示器
- `WindowManager` 增加了 `captureDisplay()`、`setForcedDisplaySize()`、`clearForcedDisplaySize()` 等方法
- `Ln` 的日志 TAG 和前缀修改为本项目标识

---

## MaaAssistantArknights

- **项目地址**：[MaaAssistantArknights](https://github.com/MaaAssistantArknights/MaaAssistantArknights)
- **许可证**：[AGPL-3.0](https://www.gnu.org/licenses/agpl-3.0.html)
- **使用方式**：通过 `scripts/setup_maa_core.py` 下载预编译产物（`libMaaCore.so` 及资源文件），运行时由 JNA 动态加载
---

## compose-miuix-ui example

- **项目地址**：[compose-miuix-ui](https://github.com/compose-miuix-ui/miuix)
- **版权**：Copyright (c) 2024 Kyant
- **许可证**：[Apache License 2.0](../LICENSE-Apache-2.0)
- **本项目中的位置**：
  - [`app/src/main/java/com/aliothmoon/maameow/ui/component/bottombar/FloatingBottomBar.kt`](../app/src/main/java/com/aliothmoon/maameow/ui/component/bottombar/FloatingBottomBar.kt)
  - [`app/src/main/java/com/aliothmoon/maameow/ui/component/liquid/CombinedBackdrop.kt`](../app/src/main/java/com/aliothmoon/maameow/ui/component/liquid/CombinedBackdrop.kt)
  - [`app/src/main/java/com/aliothmoon/maameow/ui/component/liquid/Lens.kt`](../app/src/main/java/com/aliothmoon/maameow/ui/component/liquid/Lens.kt)
  - [`app/src/main/java/com/aliothmoon/maameow/ui/component/liquid/Vibrancy.kt`](../app/src/main/java/com/aliothmoon/maameow/ui/component/liquid/Vibrancy.kt)

### 用途

这些文件借鉴了 `compose-miuix-ui` 示例项目中的 Liquid Glass / Bottom Bar 实现思路，参考其 API 调用方式与效果组合方法。本项目仅参考了 Miuix SDK 公开 API（`BackdropEffectScope` / `runtimeShaderEffect` / `colorControls` / `IosLiquidGlassNavigationBar` 等）与示例布局，未逐字复制示例源码；具体效果实现与参数适配由本项目独立完成。

注：`ui/component/liquid/` 下的三个文件同时基于 [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)（Apache 2.0），头部的文件级声明以 Kyant0 为主要来源；本条目仅说明其与 `compose-miuix-ui` 示例的参考关系。

---

## tiann/KernelSU

- **项目地址**：[tiann/KernelSU](https://github.com/tiann/KernelSU)
- **版权**：Copyright (c) 2022 tiann
- **许可证**：[GPL-3.0](https://www.gnu.org/licenses/gpl-3.0.html)
- **本项目中的位置**：[`app/src/main/java/com/aliothmoon/maameow/ui/screen/settings/ThemeSettingsViewMiuix.kt`](../app/src/main/java/com/aliothmoon/maameow/ui/screen/settings/ThemeSettingsViewMiuix.kt)

### 用途

Miuix 模式设置页中的“主题预览卡片”（`ThemePreviewMiuix` / `PreviewInfoRowMiuix` / `PreviewDotMiuix`）参考了 KernelSU `ui.theme.Preview` 的结构布局，使应用内 Mockup 与上游设置预览的视觉效果保持一致。本项目未逐字复制其源码；所有显示数值均绑定至本地 `AppSettingsManager`，Compose 布局形态借鉴自上游。
