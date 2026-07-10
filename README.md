# VelocityHologram

[![Build](https://github.com/windy664/VelocityHologram/actions/workflows/gradle.yml/badge.svg)](https://github.com/windy664/VelocityHologram/actions)
[![License](https://img.shields.io/github/license/windy664/VelocityHologram)](LICENSE)
[![Version](https://img.shields.io/github/v/release/windy664/VelocityHologram)](https://github.com/windy664/VelocityHologram/releases)
[![Java](https://img.shields.io/badge/Java-17%2B-blue)](https://adoptium.net/)
[![Velocity](https://img.shields.io/badge/Velocity-3.3.0%2B-blue)](https://velocitypowered.com/)

纯代理端悬浮字系统，基于 Velocity + packetevents 实现。

## 特性

- **零子服依赖**：完全在代理端运行，无需子服安装任何插件
- **多实体类型**：Text Display / Item Display / Block Display / Entity（1.19.4+）
- **packetevents 驱动**：直接构造实体包，跨版本兼容
- **多页系统**：支持悬浮字多页内容，玩家可独立翻页
- **扩展点击动作**：支持 4 种点击类型 + 动作链 + 7 种动作类型
- **实时追踪**：通过拦截移动包实时更新玩家坐标
- **视觉效果**：Billboard / 缩放 / 透明度 / 背景 / 渐变色 / 动画
- **权限控制**：命令权限 + 悬浮字可见性权限 + Flag 系统
- **空间分区**：优化的可见性计算，支持大量悬浮字
- **异步更新**：不阻塞 Netty 线程
- **Java API + 事件**：完整的公共 API 和事件系统
- **多语言支持**：可自定义所有提示消息

## 架构

```
VelocityHologram/
├── api/                    # 公共 API 接口
├── hologram/               # 悬浮字核心实现
├── display/                # Display 实体工厂（可扩展）
│   ├── DisplayEntityFactory    # 工厂接口
│   ├── DisplayFactoryRegistry  # 注册表
│   ├── DisplayConfig           # 统一配置
│   ├── TextDisplayFactory      # Text Display
│   ├── ItemDisplayFactory      # Item Display
│   └── BlockDisplayFactory     # Block Display
├── tracker/                # 玩家状态追踪
├── action/                 # 点击动作系统
├── animation/              # 动画 + 渐变色
├── placeholder/            # 占位符系统
├── config/                 # 配置加载
└── command/                # 命令系统
```

## 依赖

- Velocity 3.3.0+
- packetevents 2.13.0（内置）

## 下载

从 [GitHub Releases](https://github.com/windy664/VelocityHologram/releases) 下载最新版本。

## 构建

```bash
./gradlew shadowJar
```

输出：`build/libs/VelocityHologram-1.2.0.jar`

## 配置

在 `plugins/VelocityHologram/holograms/` 目录下创建 YAML 文件：

```yaml
# 悬浮字名称 = 文件名
x: 0.5
y: 100
z: 0.5
dimension: minecraft:overworld
server: lobby
view-distance: 48
line-spacing: 0.3
permission: "velocityhologram.view.welcome"
lines:
  - text: "§b§l欢迎来到本服"
    billboard: center
    scale: 1.5
    right-click: "command:/spawn"
  - text: "§7在线: %server_online%/%server_max_players%"
  - item: "diamond_sword"
    scale: 2.0
  - block: "grass_block"
    scale: 0.5
  - text: "{cycle:20|§a帧1|§b帧2|§c帧3}"
  - text: "{gradient:#FF0000:#0000FF|渐变文本}"
  - text: "{gradient:rainbow|彩虹文本}"
```

## 命令

| 命令 | 说明 | 权限 |
|------|------|------|
| `/holo create <名称>` | 在你位置创建悬浮字 | `velocityhologram.command.create` |
| `/holo delete <名称>` | 删除悬浮字 | `velocityhologram.command.delete` |
| `/holo addline <名称> <文本>` | 添加文本行 | `velocityhologram.command.edit` |
| `/holo additem <名称> <物品ID>` | 添加物品行 | `velocityhologram.command.edit` |
| `/holo addblock <名称> <方块ID>` | 添加方块行 | `velocityhologram.command.edit` |
| `/holo addentity <名称> <实体ID>` | 添加实体行 | `velocityhologram.command.edit` |
| `/holo addhead <名称> <物品ID>` | 添加头颅行 | `velocityhologram.command.edit` |
| `/holo addsmallhead <名称> <物品ID>` | 添加小头颅行 | `velocityhologram.command.edit` |
| `/holo setline <名称> <行号> <文本>` | 设置某行 | `velocityhologram.command.edit` |
| `/holo removeline <名称> <行号>` | 删除某行 | `velocityhologram.command.edit` |
| `/holo insertline <名称> <行号> <文本>` | 在指定位置插入行 | `velocityhologram.command.edit` |
| `/holo swaplines <名称> <A> <B>` | 交换两行 | `velocityhologram.command.edit` |
| `/holo setoffset <名称> <行号> <x> <y> <z>` | 设置行偏移 | `velocityhologram.command.edit` |
| `/holo addpage <名称>` | 添加新页 | `velocityhologram.command.edit` |
| `/holo removepage <名称> <页码>` | 删除页 | `velocityhologram.command.edit` |
| `/holo switchpage <名称> <页码>` | 切换编辑页 | `velocityhologram.command.edit` |
| `/holo move <名称>` | 移动到你的位置 | `velocityhologram.command.edit` |
| `/holo movehere <名称>` | 同上（别名） | `velocityhologram.command.edit` |
| `/holo clone <名称> <新名称>` | 克隆悬浮字 | `velocityhologram.command.create` |
| `/holo center <名称>` | 居中到区块中心 | `velocityhologram.command.edit` |
| `/holo near [半径]` | 列出附近悬浮字 | `velocityhologram.command.list` |
| `/holo list` | 列出所有悬浮字 | `velocityhologram.command.list` |
| `/holo save [名称]` | 保存（全部/指定） | `velocityhologram.command.save` |
| `/holo reload` | 重新加载配置 | `velocityhologram.command.reload` |
| `/holo debug <名称>` | 显示调试信息 | `velocityhologram.command.debug` |
| `/holo tp <名称>` | 传送到悬浮字 | `velocityhologram.command.debug` |
| `/holo info` | 显示统计信息 | `velocityhologram.command.debug` |
| `/holo permission <名称> <节点>` | 设置可见权限 | `velocityhologram.command.admin` |
| `/holo addflag <名称> <flag>` | 添加 flag | `velocityhologram.command.admin` |
| `/holo removeflag <名称> <flag>` | 移除 flag | `velocityhologram.command.admin` |

## 占位符

| 占位符 | 说明 |
|--------|------|
| `%server_online%` | 在线玩家数 |
| `%server_max_players%` | 最大玩家数 |
| `%player_name%` | 玩家名 |
| `%player_ping%` | 玩家延迟 |
| `%player_server%` | 玩家所在服务器 |
| `%server_time%` | 当前时间（HH:mm:ss） |
| `%server_time:HH:mm%` | 自定义时间格式 |
| `%server_date%` | 当前日期（yyyy-MM-dd） |
| `%server_date:yyyy/MM/dd%` | 自定义日期格式 |
| `%server_motd%` | 服务器 MOTD |

## 扩展

通过 `DisplayFactoryRegistry` 注册自定义 Display 工厂：

```java
DisplayFactoryRegistry registry = ...;
registry.register(DisplayEntityType.TEXT_DISPLAY, new MyCustomTextFactory());
```
