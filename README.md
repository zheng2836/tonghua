# tonghua

Android-only managed ConnectionService VoIP scaffold.

## 已落地

### Android 工程骨架

- Gradle root: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`
- App module: `app/build.gradle.kts`
- Manifest: `app/src/main/AndroidManifest.xml`
- `App.kt`
- `telecom/PhoneAccountRegistrar.kt`
- `telecom/TelecomFacade.kt`
- `telecom/ManagedVoipConnectionService.kt`
- `telecom/ManagedVoipConnection.kt`
- `push/AppFirebaseMessagingService.kt`
- `ui/MainActivity.kt`

### 当前能力

- 启动时注册 managed `PhoneAccount`
- 可引导用户打开系统里的电话账号设置
- 已接入 `ConnectionService` / `Connection` 骨架
- 已预留 FCM data message 入呼入口
- 已预留 `placeCall(...)` / `addNewIncomingCall(...)` 封装

## 还没落地的部分

- 真正的 WebSocket 信令
- WebRTC 音频链路
- FCM 服务端发送器
- TURN / coturn 实配
- 登录 / 联系人 / 最近通话
- 完整 server

## 目录

```text
app/
  build.gradle.kts
  proguard-rules.pro
  src/main/
    AndroidManifest.xml
    java/com/zheng2836/tonghua/
      App.kt
      push/AppFirebaseMessagingService.kt
      telecom/
        PhoneAccountRegistrar.kt
        TelecomFacade.kt
        ManagedVoipConnection.kt
        ManagedVoipConnectionService.kt
      ui/MainActivity.kt
build.gradle.kts
gradle.properties
settings.gradle.kts
```

## 说明

这次先把 Android 端最关键的 managed Telecom 路线钉死，避免项目后面又滑到 self-managed 或假来电 UI。
