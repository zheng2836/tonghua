# tonghua

Android-only managed ConnectionService VoIP skeleton.

## 目标

- 只做 Android
- 使用 **managed ConnectionService**，不是 self-managed
- 来电走 `TelecomManager.addNewIncomingCall(...)`
- 去电走 `TelecomManager.placeCall(...)`
- 媒体层预留给 WebRTC
- 入呼唤醒预留给 Firebase high-priority data message

## 当前仓库内容

本次提交先落地可继续开发的工程骨架：

- Android Gradle 工程
- `PhoneAccount` 注册
- `ConnectionService` 骨架
- `Connection` 骨架
- `FirebaseMessagingService` 骨架
- 最小调试页面

## 后续要补

1. 真正的 WebSocket 信令层
2. WebRTC 音频链路
3. FCM 服务端下发
4. TURN / coturn
5. 最近通话、登录、联系人

## 目录

```text
app/
  src/main/
    AndroidManifest.xml
    java/com/zheng2836/tonghua/
      App.kt
      ui/MainActivity.kt
      telecom/
      push/
      data/
settings.gradle.kts
build.gradle.kts
gradle.properties
```
