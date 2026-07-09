# VelocityHologram

纯代理端悬浮字系统，基于 Velocity + packetevents 实现。

## 特性

- **零子服依赖**：完全在代理端运行，无需子服安装任何插件
- **packetevents 驱动**：直接构造 Text Display 实体包，跨版本兼容
- **实时追踪**：通过拦截移动包实时更新玩家坐标
- **空间分区**：优化的可见性计算，支持大量悬浮字
- **异步更新**：不阻塞 Netty 线程

## 架构

```
VelocityHologram/
├── api/                    # 公共 API 接口
│   ├── IHologram
│   └── IHologramLine
├── hologram/               # 悬浮字核心实现
│   ├── Hologram
│   ├── HologramLine
│   └── HologramManager
├── display/                # 实体包构造
│   └── TextDisplayFactory
├── tracker/                # 玩家状态追踪
│   ├── PlayerTracker
│   ├── PlayerState
│   └── HologramPacketListener
├── config/                 # 配置加载
│   ├── HologramConfig
│   └── HologramLoader
└── VelocityHologramPlugin  # Velocity 插件主类
```

## 依赖

- Velocity 3.3.0+
- packetevents 2.13.0（内置）

## 构建

```bash
./gradlew shadowJar
```

输出：`build/libs/VelocityHologram-1.0.0.jar`

## 配置

在 `plugins/VelocityHologram/holograms/` 目录下创建 YAML 文件：

```yaml
# 悬浮字名称 = 文件名
x: 0.5
y: 100
z: 0.5
dimension: minecraft:overworld
server: lobby
lines:
  - "§b§l欢迎来到本服"
  - "§7VelocityHologram v1.0.0"
```

## TODO

- [ ] 完善 TextDisplayFactory 的 EntityMetadata 构造
- [ ] 实现维度追踪（解析 Login/Respawn 包）
- [ ] 实现 name -> UUID 映射
- [ ] 添加命令系统
- [ ] 支持动态文本更新（占位符）
- [ ] 支持动画效果
- [ ] 空间分区优化
