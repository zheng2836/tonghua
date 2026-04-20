# tonghua

Android-only managed ConnectionService VoIP scaffold.

## 当前进度

仓库已经不再是单纯文件名骨架，而是进入“可继续开发”的项目状态。

### Android 已有

- Gradle root 工程
- app module
- Manifest 与权限
- `AppGraph` 全局对象装配
- `PhoneAccountRegistrar`
- `TelecomFacade`
- `ManagedVoipConnectionService`
- `ManagedVoipConnection`
- `ConnectionRegistry`
- `CallStore` / `CallSession` / `CallState`
- `SignalingClient` scaffold
- `WebRtcEngine` scaffold
- `AppFirebaseMessagingService`
- 调试首页 `MainActivity`

### server 已有

- `server/cmd/api/main.go`
- `server/internal/signaling/types.go`
- `server/internal/push/fcm.go`
- `server/internal/calls/store.go`
- `server/internal/ws/hub.go`
- `server/Dockerfile`
- `server/go.mod`

### deploy 已有

- `deploy/docker-compose.yml`
- `deploy/coturn/turnserver.conf`

## 当前已经实现的关键方向

- 只走 Android managed `ConnectionService`
- 启动时注册 `PhoneAccount`
- FCM data message 来电入口已留好
- `TelecomManager.placeCall(...)` / `addNewIncomingCall(...)` 封装已留好
- app 侧已有最小状态流转：
  - `FCM -> Telecom -> ConnectionService -> Connection -> signaling/webrtc/store`
- server 侧已有最小目录和状态存储结构

## 仍未完成

- 真正的 WebSocket 双向信令
- 真正的 WebRTC PeerConnection / ICE / TURN
- 真正的 FCM 服务账号发送
- 联系人 / 登录 / 最近通话
- 完整 API 与数据库接线

## 目录

```text
app/
  build.gradle.kts
  proguard-rules.pro
  src/main/
    AndroidManifest.xml
    java/com/zheng2836/tonghua/
      App.kt
      AppGraph.kt
      data/
        CallModels.kt
        CallStore.kt
        DebugState.kt
      push/
        AppFirebaseMessagingService.kt
      signaling/
        SignalingClient.kt
        SignalingModels.kt
      telecom/
        ConnectionRegistry.kt
        ManagedVoipConnection.kt
        ManagedVoipConnectionService.kt
        PhoneAccountRegistrar.kt
        TelecomFacade.kt
      ui/
        MainActivity.kt
      webrtc/
        WebRtcEngine.kt
server/
  cmd/api/main.go
  internal/
    calls/store.go
    push/fcm.go
    signaling/types.go
    ws/hub.go
  Dockerfile
  go.mod
deploy/
  docker-compose.yml
  coturn/turnserver.conf
```

## 下一步最该做

1. server `/ws` 从 `501 not implemented` 升级为真实 websocket 连接。
2. Android `SignalingClient` 换成真实 OkHttp WebSocket。
3. `WebRtcEngine` 接入真实 PeerConnection。
4. 把 server push sender 接到 FCM HTTP v1。

## Build trigger

Manual workflow trigger commit for Android debug APK.
