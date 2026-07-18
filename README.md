# V-ABHelper

V-ABHelper（系统更新助手）是一款面向已 Root 的 Virtual A/B Android 设备的本地 OTA 辅助工具。应用通过 [libsu](https://github.com/topjohnwu/libsu) 启动 RootService，并调用系统 `update_engine` 写入完整 OTA 包。

项目最初用于魅族 20 Pro，目前已在魅族 21 Pro 上完成更新、受控降级及 KernelSU 保留 Root 的真实设备测试。

## 功能

- 从本地选择完整 OTA ZIP，并在 RootService 中解析 `payload.bin`、`payload_properties.txt` 和固件信息。
- 开始写入前检查更新包完整性、全量包标记、目标机型和构建时间。
- 显示 `update_engine` 的真实进度和结果，并针对空间不足、包校验失败、读取失败、防回滚拒绝等常见错误给出明确提示。
- 更新完成后支持使用 Magisk、KernelSU 或 APatch 修补目标槽位，以保留 Root。
- 兼容 KernelSU 3.x 的 `ksud` 接口，并保留旧版 KernelSU 的兼容回退。
- 对满足条件的旧版完整 OTA 提供受控强制降级流程，包括双重确认、临时属性恢复和强制清除数据标记。

## 设备要求

- Android 11（API 30）或更高版本。
- 使用 Virtual A/B 分区和系统 `update_engine`。
- 设备已取得 Root 权限，且应用已获 Root 授权。
- 与当前设备匹配、下载完整且未损坏的全量 OTA ZIP。

普通升级不要求 KernelSU。强制降级还必须同时满足以下条件：

- Bootloader 的真实启动状态为 `orange` 且 `vbmeta.device_state=unlocked`；应用读取 `/proc/bootconfig` 判断，不依赖运行时伪装值。
- KernelSU 的 `/data/adb/ksu/bin/resetprop` 存在且可执行。
- `super` 分区存在并可写。
- 所选固件构建时间早于当前系统，且明确标记为完整包。

即使满足以上条件，设备厂商的签名校验、AVB 防回滚或 `update_engine` 策略仍可能拒绝降级。本项目不会绕过这些底层安全限制。

## 使用方法

1. 安装 APK，启动应用并授予 Root 和通知权限。
2. 点击“选择文件更新”，选择与设备匹配的完整 OTA ZIP。
3. 核对应用解析出的包信息，然后开始更新。
4. 等待 `update_engine` 完成；过程中不要强制停止应用、重启设备或断电。
5. 更新成功后按当前 Root 方案选择 Magisk、KernelSU、APatch，或自行处理 Root 保留。
6. 修补成功后按提示重启设备，进入新槽位。

## 强制降级警告

强制降级会向 payload 属性加入 `POWERWASH=1` 和 `SPL_DOWNGRADE=1`。成功后系统将恢复出厂设置，`/data` 中的应用数据以及内部存储（包括通常显示为 `/sdcard` 的内容）都会被清除。

开始降级前必须：

- 将照片、文件、下载内容、Root 管理器 APK 及 LSPosed/KernelSU 相关配置备份到电脑或其他外部存储。
- 确保电量充足并保持设备连接电源。
- 确认能够使用 bootloader、Recovery 或厂商刷机工具恢复设备。

应用不会自动备份或恢复用户数据。不要将唯一备份保存在同一设备的 `/data` 或 `/sdcard` 中。

## 已验证环境

以下结果来自真实设备测试，不代表对其他机型或固件版本作出兼容性保证：

- 设备：Meizu 21 Pro
- 场景：Flyme `10.5.5.1A` 强制降级至 `10.5.1.1A`
- 槽位：`_a` 写入 `_b`
- Root：KernelSU `3.2.5`，LKM 模式
- 结果：OTA、强制清除数据、目标槽位启动及 KernelSU Root 恢复成功

## 构建

构建环境建议使用 JDK 17 和已配置好的 Android SDK：

```bash
./gradlew testDebugUnitTest assembleDebug lintDebug
```

调试 APK 输出到：

```text
app/build/outputs/apk/debug/app-debug.apk
```

项目当前配置：`minSdk 30`、`compileSdk 33`、Android Gradle Plugin `8.1.1`。

## 致谢

- 原项目及“一叶孤舟”的 Shell 实现提供了早期思路。
- [libsu](https://github.com/topjohnwu/libsu) 提供 RootService 和 Root 文件访问能力。

## 免责声明

本项目仅用于设备所有者或已获明确授权设备的系统维护、学习和测试。OTA 写入、Root 保留和系统降级均可能造成数据丢失、无法启动或设备损坏，请在理解风险并具备恢复能力后使用。使用者需自行承担操作后果。
