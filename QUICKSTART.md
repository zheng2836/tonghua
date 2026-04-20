# TongHua Quick Start

## 你现在只需要自己提供两样东西

1. `google-services.json`（安卓 Firebase 配置）
2. Firebase service account JSON（服务器推送凭证）

这两样不能由代码自动生成，因为它们来自你自己的 Firebase 项目。

---

## 服务器：一条命令安装（CentOS）

```bash
curl -fsSL https://raw.githubusercontent.com/zheng2836/tonghua/main/deploy/install-centos.sh | sudo bash -s -- joker404.xyz
```

安装完成后，把 Firebase service account JSON 放到：

```text
/opt/tonghua/firebase-service-account.json
```

然后重启：

```bash
cd /opt/tonghua/deploy && sudo docker compose up -d --build
```

---

## APK：最简生成方式

### 方法 A：GitHub Actions

仓库已经包含：

```text
.github/workflows/android-debug-apk.yml
```

你需要先把：

```text
app/google-services.json
```

放进仓库，然后进入 GitHub Actions，运行 `build-android-debug-apk`，下载产物 `tonghua-debug-apk`。

### 方法 B：本地 Android Studio

导入项目后执行：

- Build
- Build APK(s)

生成位置通常为：

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## App 内默认配置

- 默认服务器：`https://joker404.xyz`
- 默认 TURN：`turn:joker404.xyz:3478?transport=udp`
- App 内可以直接改服务器地址

---

## 当前状态

项目已经具备：

- 虚拟号联系人拨号页
- managed ConnectionService
- WebSocket 信令
- WebRTC 占位通话链
- 自动重连 + ping
- 服务器设备注册 / 调试接口 / push 链
- CentOS 一键安装脚本

最近一次更新已经处理：

- 删除不可下载的 `google-webrtc` 依赖
- 替换为可编译的 `WebRtcEngine` 占位实现
- 延后高风险启动时操作，降低 Android 15 / HyperOS 打开即退概率

现在真正卡住的不是代码骨架，而是 Firebase 两个真实凭证文件和真机联调。
