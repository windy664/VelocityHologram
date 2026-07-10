# VelocityHologram 开发计划

> 对标 DecentHolograms，补齐核心差距。
> 完成后预计达到 DH 功能覆盖 85%+。

> **v1.2.0 已完成所有 Phase（1-8）** ✅

---

## Phase 1 — 多页系统（Pages）

DH 最大卖点。一个悬浮字有多页内容，玩家可翻页，每页独立动作。

### 1.1 数据结构

```
Hologram
  └── List<Page> pages          // 多页
       └── Page
            └── List<HologramLine> lines
```

- `Hologram` 新增 `pages` 字段，替代原来的 `lines`
- `Page` 为新增类，持有 `List<HologramLine>`
- 默认 1 页，向后兼容旧配置

### 1.2 每玩家页索引

- `Map<UUID, Integer> playerPage` 存在 `Hologram` 上
- `showTo(playerId)` 时发送当前页的行
- `switchPage(playerId, pageIndex)` 翻页：先 despawn 旧页 → spawn 新页

### 1.3 配置格式

```yaml
pages:
  - lines:
    - text: "§b第一页"
    - text: "§7内容A"
  - lines:
    - text: "§a第二页"
    - text: "§7内容B"
```

旧格式 `lines:` 自动转为单页。

### 1.4 命令

- `/holo addpage <名称>` — 添加新页
- `/holo removepage <名称> <页码>` — 删除页
- `/holo switchpage <名称> <页码>` — 切换编辑目标页
- `/holo addline` 等命令操作当前编辑页

---

## Phase 2 — 扩展点击动作

### 2.1 新增点击类型

当前只有 LEFT / RIGHT，补齐：

- `SHIFT_LEFT` — 潜行左键
- `SHIFT_RIGHT` — 潜行右键

**实现**：`ClickHandler` 解析 `INTERACT_ENTITY` 包时，读取玩家是否潜行（从移动包的 flags 字段追踪，或从 Player 对象查询）。

### 2.2 新增动作类型

| 动作 | 语法 | 说明 |
|------|------|------|
| CONNECT | `connect:lobby` | 点击切换子服 |
| TELEPORT | `teleport:world:0:100:0` | 点击传送到坐标 |
| SOUND | `sound:entity.experience_orb.pickup:1.0:1.0` | 点击播放音效 |
| PERMISSION | `perm:some.permission` | 权限门控，无权限则后续动作不执行 |
| NEXT_PAGE | `nextpage` | 翻到下一页 |
| PREV_PAGE | `prevpage` | 翻到上一页 |
| PAGE | `page:2` | 跳到指定页 |

### 2.3 动作链

一个点击可串联多个动作，用 `;` 分隔：

```yaml
left-click: "perm:holo.use;connect:lobby"
right-click: "sound:entity.experience_orb.pickup;nextpage"
```

从左到右依次执行，遇到 PERMISSION 门控失败则中断。

### 2.4 配置扩展

```yaml
lines:
  - text: "§b点击翻页"
    left-click: "nextpage"
    right-click: "prevpage"
    shift-left-click: "connect:survival"
    shift-right-click: "command:/spawn"
```

---

## Phase 3 — 命令补全

### 3.1 移动

- `/holo move <名称>` — 移动到玩家当前位置
- `/holo movehere <名称>` — 同上（别名）

### 3.2 克隆

- `/holo clone <名称> <新名称>` — 克隆悬浮字

### 3.3 居中

- `/holo center <名称>` — 将悬浮字居中到当前区块中心

### 3.4 行操作增强

- `/holo insertline <名称> <行号> <文本>` — 在指定位置插入行
- `/holo swaplines <名称> <行号A> <行号B>` — 交换两行

### 3.5 行偏移

- `/holo setoffset <名称> <行号> <x> <y> <z>` — 设置行偏移

配置：
```yaml
lines:
  - text: "§b标题"
    offset-x: 0.5
    offset-y: 0.3
    offset-z: 0.0
```

### 3.6 查询

- `/holo near [半径]` — 列出附近的悬浮字

---

## Phase 4 — 更新间隔 + 显示/更新范围

### 4.1 更新间隔

当前动画 200ms、可见性 500ms 写死。改为每悬浮字可配置：

```yaml
update-interval: 20  # tick（1秒 = 20tick）
```

### 4.2 显示范围 vs 更新范围

- `display-range` — 玩家进入此范围才显示
- `update-range` — 玩家在此范围内才接收更新（更远的冻结在最后状态）

```yaml
display-range: 48
update-range: 32
```

---

## Phase 5 — Flag 系统

悬浮字级别的功能开关。

```yaml
flags:
  - ALWAYS_VISIBLE    # 忽略距离检查
  - DISABLE_PLACEHOLDERS  # 不解析占位符
  - DISABLE_ACTIONS   # 禁用点击动作
  - DISABLE_UPDATES   # 禁止自动更新
```

命令：
- `/holo addflag <名称> <flag>`
- `/holo removeflag <名称> <flag>`

---

## Phase 6 — Java API + 事件

### 6.1 公共 API

```java
// 获取管理器
HologramManager manager = VelocityHologramAPI.getManager();

// 创建悬浮字
Hologram holo = manager.createHologram("test", x, y, z, dimension, server);
holo.addLine("§bHello");
holo.addPage();
holo.setLineAction(0, Action.LEFT, new ConnectAction("lobby"));

// 查询
Collection<Hologram> all = manager.getAllHolograms();
Hologram byName = manager.getHologram("test");
```

### 6.2 事件

- `HologramCreateEvent` — 悬浮字创建
- `HologramDeleteEvent` — 悬浮字删除
- `HologramClickEvent` — 玩家点击（含点击类型、动作）
- `HologramPageSwitchEvent` — 玩家翻页

---

## Phase 7 — #ENTITY / #HEAD 行类型

### 7.1 #ENTITY

```yaml
lines:
  - entity: "PIG"
  - entity: "AXOLOTL"
```

用 Entity Display 或直接用 packetevents 构造对应实体包。

### 7.2 #HEAD / #SMALLHEAD

通过 ArmorStand 的头盔槽显示物品/方块：

```yaml
lines:
  - head: "GRASS_BLOCK"
  - smallhead: "PLAYER_HEAD"
    head-texture: "d0by"
```

---

## Phase 8 — 多语言

```yaml
# lang.yml
messages:
  created: "§a已创建悬浮字 '%name%'"
  deleted: "§a已删除悬浮字 '%name%'"
  no-permission: "§c你没有权限"
  not-found: "§c悬浮字 '%name%' 不存在"
```

---

## 实施顺序

```
Phase 1 (多页)  →  Phase 2 (动作扩展)  →  Phase 3 (命令补全)
     ↓                                        ↓
Phase 4 (更新间隔+范围)  →  Phase 5 (Flag)  →  Phase 6 (API)
     ↓
Phase 7 (实体行)  →  Phase 8 (多语言)
```

每个 Phase 完成后 `./gradlew clean build` 验证。

---

## 当前已完成（v1.2.0）

### Phase 1 — 多页系统（Pages）✅
- [x] Page 类实现
- [x] Hologram 多页支持
- [x] 每玩家页索引
- [x] 配置格式（pages: / lines:）
- [x] 命令：addpage/removepage/switchpage

### Phase 2 — 扩展点击动作 ✅
- [x] SHIFT_LEFT / SHIFT_RIGHT 支持
- [x] 动作类型：CONNECT/TELEPORT/SOUND/PERMISSION/NEXT_PAGE/PREV_PAGE/PAGE
- [x] 动作链（; 分隔）
- [x] 配置扩展（shift-left-click/shift-right-click）

### Phase 3 — 命令补全 ✅
- [x] /holo move <名称> — 移动到玩家位置
- [x] /holo movehere <名称> — 别名
- [x] /holo clone <名称> <新名称> — 克隆悬浮字
- [x] /holo center <名称> — 居中到区块中心
- [x] /holo insertline <名称> <行号> <文本> — 插入行
- [x] /holo swaplines <名称> <行号A> <行号B> — 交换两行
- [x] /holo setoffset <名称> <行号> <x> <y> <z> — 设置行偏移
- [x] /holo near [半径] — 列出附近悬浮字

### Phase 4 — 更新间隔 + 显示/更新范围 ✅
- [x] update-interval 配置
- [x] display-range / update-range 分离

### Phase 5 — Flag 系统 ✅
- [x] ALWAYS_VISIBLE / DISABLE_PLACEHOLDERS / DISABLE_ACTIONS / DISABLE_UPDATES
- [x] 命令：addflag/removeflag

### Phase 6 — Java API + 事件 ✅
- [x] VelocityHologramAPI
- [x] HologramCreateEvent / HologramDeleteEvent / HologramClickEvent / HologramPageSwitchEvent
- [x] EventBus

### Phase 7 — #ENTITY / #HEAD 行类型 ✅
- [x] EntityFactory
- [x] HeadFactory（HEAD / SMALLHEAD）

### Phase 8 — 多语言 ✅
- [x] Lang 类
- [x] lang.yml 配置

### 基础功能（v1.1）
- [x] Display 抽象层（Text/Item/Block）
- [x] 动作系统（Command/Console/Rcon/Url/Message/Suggest）
- [x] ClickHandler（左键/右键/潜行左键/潜行右键）
- [x] 动画（CYCLE/RANDOM/TYPEWRITER/渐变/彩虹）
- [x] 占位符（PAPI 命名）
- [x] 可见性权限
- [x] 行间距/偏移(X/Y/Z)
- [x] Billboard/缩放/透明度/背景
- [x] 维度追踪（JoinGame/Respawn 解析）
- [x] RCON 客户端 + 连接池
- [x] 主配置文件 config.yml
- [x] YAML 持久化（自动保存）
- [x] /holo tp（RCON 传送）
- [x] /holo debug/info/permission
