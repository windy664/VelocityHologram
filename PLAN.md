# VelocityHologram 开发计划

## 已完成

### 核心架构
- [x] Text Display 实体生成/销毁/元数据更新
- [x] 玩家坐标追踪（移动包拦截）
- [x] 维度追踪（JoinGame/Respawn 包）
- [x] 服务器追踪（ServerPostConnectEvent）
- [x] 可见性计算（距离 + 服务器 + 维度）
- [x] 空间分区优化（按服务器分组）
- [x] 异步更新调度（500ms 可见性 / 100ms 动画）

### 动作系统
- [x] Action 接口 + ActionFactory
- [x] CommandAction（玩家执行命令）
- [x] ConsoleCommandAction（控制台执行）
- [x] RconAction（RCON 执行，目前退化为控制台）
- [x] UrlAction（打开 URL）
- [x] MessageAction（发送消息）
- [x] SuggestCommandAction（建议命令）
- [x] ClickHandler（INTERACT_ENTITY 包拦截，左键/右键）

### 动画系统
- [x] TextAnimation（CYCLE/RANDOM/TYPEWRITER）
- [x] AnimationParser（{cycle:20|帧1|帧2} 语法）

### 占位符系统
- [x] PlaceholderManager
- [x] 内置占位符（%online%/%max%/%player%/%ping%/%server%/%time%）
- [x] 自定义占位符注册 API

### 持久化
- [x] HologramLoader YAML 保存/加载
- [x] 包含动作、动画、视距等完整配置
- [x] shutdown 自动保存

### 命令系统
- [x] /holo create/delete/addline/setline/removeline/list/save/reload

---

## 待开发

### P0 - 高优先级

#### 1. 实体支持（Item Display / Block Display）
- [ ] `ItemDisplayFactory` - 显示物品（如悬浮掉落物）
  - 支持 `FILLED_MAP`、`DIAMOND_SWORD` 等任意物品
  - 物品 NBT / 数据组件跨版本兼容
- [ ] `BlockDisplayFactory` - 显示方块（如草方块、钻石块）
  - 支持 `BlockState` 映射
  - 旋转/缩放支持
- [ ] HologramLine 支持多种实体类型
  - `text` - Text Display
  - `item:<物品ID>` - Item Display
  - `block:<方块ID>` - Block Display
- [ ] 配置语法扩展
  ```yaml
  lines:
    - text: "§b标题"
    - item: "diamond_sword"
    - block: "grass_block"
  ```

#### 2. 真正的 RCON 客户端
- [ ] `RconClient` - TCP 连接到服务器 RCON 端口
  - RCON 协议实现（认证 + 命令执行）
  - 连接池管理（多子服）
  - 超时/重连机制
- [ ] 配置
  ```yaml
  rcon:
    enabled: true
    servers:
      lobby:
        host: 127.0.0.1
        port: 25575
        password: "xxx"
      survival:
        host: 127.0.0.1
        port: 25576
        password: "xxx"
  ```

#### 3. 权限系统
- [ ] 命令权限节点
  - `velocityhologram.command.create`
  - `velocityhologram.command.delete`
  - `velocityhologram.command.edit`
  - `velocityhologram.command.admin`
- [ ] 悬浮字可见性权限
  - 按权限节点控制哪些玩家能看到哪些悬浮字
  - 支持通配符 `velocityhologram.view.*`

### P1 - 中优先级

#### 4. 行间距可配置
- [ ] 每个悬浮字的行间距可配置（默认 0.3）
- [ ] 每行的独立偏移量
  ```yaml
  lines:
    - text: "标题"
      offset-y: 0.5
    - text: "内容"
      offset-y: 0.3
  ```

#### 5. 渐变色支持
- [ ] 文本渐变语法
  ```
  {gradient:#FF0000:#0000FF|渐变文本}
  ```
- [ ] RGB 颜色支持（1.16+）
- [ ] 动态渐变动画

#### 6. 更多占位符
- [ ] 时间格式化 `%time:HH:mm:ss%`
- [ ] 日期 `%date:yyyy-MM-dd%`
- [ ] 变量 `%var:key%`（自定义变量存储）
- [ ] 条件 `%if:condition:true_text:false_text%`

#### 7. 视觉效果
- [ ] Billboard 模式（始终面向玩家 / 固定 / 垂直）
  ```yaml
  billboard: center  # center / vertical / fixed / horizontal
  ```
- [ ] 缩放
  ```yaml
  scale: 1.5
  ```
- [ ] 透明度
  ```yaml
  opacity: 0.8
  ```
- [ ] 背景颜色
  ```yaml
  background: 0x40000000
  ```

### P2 - 低优先级

#### 8. 多语言支持
- [ ] 消息文件 `messages.yml`
- [ ] 命令提示/错误消息国际化

#### 9. API 完善
- [ ] Java API 供其他插件使用
  ```java
  // 获取管理器
  HologramManager manager = VelocityHologramAPI.getManager();
  
  // 创建悬浮字
  Hologram holo = manager.createHologram("test", x, y, z, dimension, server);
  holo.addLine("§bHello");
  holo.setLineAction(0, new CommandAction("/spawn"), null);
  
  // 监听事件
  HologramClickEvent
  HologramCreateEvent
  HologramDeleteEvent
  ```

#### 10. 性能优化
- [ ] 批量发包（合并多个 EntityMetadata 为一个）
- [ ] 脏标记（只更新变化的行）
- [ ] 视距分级（近处高精度，远处低更新频率）

#### 11. 调试工具
- [ ] `/holo debug <名称>` - 显示悬浮字调试信息
- [ ] `/holo tp <名称>` - 传送到悬浮字位置
- [ ] `/holo info` - 显示性能统计（实体数/观察者数/更新频率）

---

## 已知问题

1. **EntityMetadata 文本更新**：当前实现可能在某些版本不正确，需要测试
2. **维度追踪**：JoinGame/Respawn 包的维度解析未实现，暂时用默认维度
3. **RCON**：目前退化为控制台命令，需要真正的 RCON 客户端

---

## 依赖版本

- Velocity API: 3.3.0-SNAPSHOT
- packetevents: 2.13.0（内置）
- Java: 21+
- Gradle: 9.0
- Shadow: 8.3.5
