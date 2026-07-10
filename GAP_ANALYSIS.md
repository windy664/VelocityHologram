# VelocityHolograms vs DecentHolograms 差距分析

## 生成时间: 2026-07-10

---

## ✅ 已实现的功能

### 核心功能
| 功能 | DecentHolograms | VelocityHolograms | 状态 |
|------|-----------------|-------------------|------|
| 多页系统 | ✅ | ✅ | 完成 |
| 行类型(TEXT/ITEM/BLOCK/HEAD/ENTITY/ICON) | ✅ | ✅ | 完成 |
| 点击动作(LEFT/RIGHT/SHIFT_LEFT/SHIFT_RIGHT) | ✅ | ✅ | 完成 |
| 动作链 | ✅ | ✅ | 完成 |
| 动画(cycle/random/typewriter/wave/burn/scroll/colors) | ✅ | ✅ | 完成 |
| 渐变色 | ✅ | ✅ | 完成 |
| 占位符 | ✅ | ✅ | 完成 |
| DisableCause | ✅ | ✅ | 完成 |
| downOrigin | ✅ | ✅ | 完成 |
| alwaysFacePlayer | ✅ | ✅ | 完成 |
| 每玩家可见性 | ✅ | ✅ | 完成 |
| 点击冷却 | ✅ | ✅ | 完成 |
| 眼级定位 | ✅ | ✅ | 完成 |
| 自定义替换字符 | ✅ | ✅ | 完成 |
| DamageDisplay | ✅ | ✅ | 完成 |
| HealingDisplay | ✅ | ✅ | 完成 |

### 命令
| 功能 | DecentHolograms | VelocityHolograms | 状态 |
|------|-----------------|-------------------|------|
| create/delete/rename | ✅ | ✅ | 完成 |
| move/clone/center/align | ✅ | ✅ | 完成 |
| addline/setline/removeline | ✅ | ✅ | 完成 |
| insertline/swaplines | ✅ | ✅ | 完成 |
| addpage/removepage/swappages | ✅ | ✅ | 完成 |
| page actions | ✅ | ✅ | 完成 |
| downorigin/alwaysface | ✅ | ✅ | 完成 |
| hide/show | ✅ | ✅ | 完成 |
| convert | ✅ | ✅ | 完成 |

### API
| 功能 | DecentHolograms | VelocityHolograms | 状态 |
|------|-----------------|-------------------|------|
| DHAPI | ✅ | ✅ | 完成 |
| 事件系统 | ✅ | ✅ | 完成 |
| IHologram/IHologramLine/IHologramManager | ✅ | ✅ | 完成 |

### 工具类
| 功能 | DecentHolograms | VelocityHolograms | 状态 |
|------|-----------------|-------------------|------|
| BungeeUtils | ✅ | ✅ | 完成 |
| UpdateChecker | ✅ | ✅ | 完成 |
| HeadDatabaseUtils | ✅ | ✅ | 完成 |
| 颜色工具 | ✅ | ✅ | 完成 |
| 物品工具 | ✅ | ✅ | 完成 |
| 位置工具 | ✅ | ✅ | 完成 |
| 调度器 | ✅ | ✅ | 完成 |
| LRU缓存 | ✅ | ✅ | 完成 |
| PAPI集成 | ✅ | ✅ | 完成 |

---

## ❌ 缺失的功能

### 1. Hologram 方法

| 方法 | 说明 | 优先级 |
|------|------|--------|
| `showAll()` | 显示给所有玩家 | 高 |
| `hideAll()` | 隐藏所有玩家 | 高 |
| `updateAll()` | 更新所有玩家 | 高 |
| `updateAll(boolean force)` | 强制更新所有玩家 | 中 |
| `updateAnimationsAll()` | 更新所有动画 | 中 |
| `realignLines()` | 重新对齐行 | 中 |
| `isInDisplayRange(Player)` | 检查是否在显示范围 | 高 |
| `isInUpdateRange(Player)` | 检查是否在更新范围 | 高 |
| `isVisibleState()` | 检查可见状态 | 中 |
| `onClick(Player, int, ClickType)` | 点击处理 | 高 |
| `onQuit(Player)` | 退出处理 | 高 |
| `size()` | 获取页面数量 | 低 |
| `fromFile(String)` | 从文件加载 | 中 |
| `save()` | 保存到文件 | 中 |
| `clone(String, Location, boolean)` | 克隆悬浮字 | 中 |
| `getViewerPlayers(int)` | 获取观看者 | 低 |
| `setLocation(Location)` | 设置位置 | 低 |
| `delete()` | 删除悬浮字 | 中 |
| `enable()` | 启用悬浮字 | 中 |

### 2. HologramPage 方法

| 方法 | 说明 | 优先级 |
|------|------|--------|
| `getCenter()` | 获取页面中心位置 | 低 |
| `realignLines()` | 重新对齐行 | 中 |
| `isClickable()` | 检查是否有可点击的行 | 中 |
| `getClickableEntityId(int)` | 获取可点击实体ID | 中 |
| `hasEntity(int)` | 检查是否包含实体 | 中 |
| `executeActions(Player, ClickType)` | 执行页面动作 | 高 |
| `clearActions(ClickType)` | 清除指定类型的页面动作 | 中 |
| `removeAction(ClickType, int)` | 移除指定类型的页面动作 | 中 |
| `getActions(ClickType)` | 获取指定类型的页面动作 | 中 |
| `hasActions()` | 检查是否有页面动作 | 中 |
| `clone(Hologram, int)` | 克隆页面 | 中 |
| `getNextLineLocation()` | 获取下一行位置 | 中 |

### 3. HologramLine 方法

| 方法 | 说明 | 优先级 |
|------|------|--------|
| `enable()` | 启用行 | 中 |
| `disable()` | 禁用行 | 中 |
| `parseContent()` | 解析内容 | 中 |
| `serializeToMap()` | 序列化为Map | 中 |
| `getType()` | 获取行类型 | 中 |
| `hasPermission(Player)` | 检查权限 | 高 |
| `updateVisibility(Player)` | 更新可见性 | 高 |
| `show(Player...)` | 显示给玩家 | 高 |
| `update(Player...)` | 更新玩家 | 高 |
| `update(boolean force, Player...)` | 强制更新玩家 | 中 |
| `updateLocation(boolean, Player...)` | 更新位置 | 中 |
| `updateAnimations(Player...)` | 更新动画 | 中 |
| `hide(Player...)` | 隐藏玩家 | 高 |
| `isInDisplayRange(Player)` | 检查是否在显示范围 | 高 |
| `isInUpdateRange(Player)` | 检查是否在更新范围 | 高 |
| `hasFlag(EnumFlag)` | 检查Flag | 中 |
| `canShow(Player)` | 检查是否可以显示 | 中 |
| `getEntityIds()` | 获取所有实体ID | 中 |

### 4. HologramManager 方法

| 方法 | 说明 | 优先级 |
|------|------|--------|
| `updateVisibility(Hologram)` | 更新悬浮字可见性 | 高 |
| `updateVisibility(Player)` | 更新玩家可见性 | 高 |
| `updateVisibility(Player, Hologram)` | 更新玩家对特定悬浮字的可见性 | 高 |
| `spawnTemporaryHologramLine(Location, String, long)` | 生成临时悬浮字行 | 中 |
| `onClick(Player, int, ClickType)` | 点击处理 | 高 |
| `onQuit(Player)` | 退出处理 | 高 |
| `reload()` | 重新加载 | 中 |
| `destroy()` | 销毁所有 | 中 |
| `showAll(Player)` | 显示所有给玩家 | 高 |
| `hideAll(Player)` | 隐藏所有给玩家 | 高 |
| `containsHologram(String)` | 检查是否包含悬浮字 | 低 |
| `registerHologram(Hologram)` | 注册悬浮字 | 中 |
| `removeHologram(String)` | 移除悬浮字 | 中 |
| `getHologramNames()` | 获取所有悬浮字名称 | 低 |

### 5. 监听器

| 监听器 | 说明 | 优先级 |
|--------|------|--------|
| `WorldListener` | 世界加载/卸载时处理 | 高 |
| `PlayerListener` | 玩家加入/退出/传送/重生时处理 | 高 |

### 6. 配置系统

| 功能 | 说明 | 优先级 |
|------|------|--------|
| `FileConfig` | 文件配置管理 | 中 |
| `CFG` | 配置基类 | 中 |
| `Key` | 配置键注解 | 低 |
| `Phrase` | 语言短语 | 低 |

### 7. 其他

| 功能 | 说明 | 优先级 |
|------|------|--------|
| `bStats` | 统计集成 | 用户不需要 |
| `NMS适配` | 我们用 packetevents 代替 | 不需要 |

---

## 总结

### 缺失功能数量
- Hologram 方法: 19个
- HologramPage 方法: 12个
- HologramLine 方法: 18个
- HologramManager 方法: 14个
- 监听器: 2个
- 配置系统: 4个

### 优先级分布
- 高优先级: ~30个
- 中优先级: ~35个
- 低优先级: ~10个

### 建议实现顺序
1. **Phase 1**: 监听器 (WorldListener, PlayerListener)
2. **Phase 2**: Hologram 核心方法 (showAll/hideAll/updateAll/onClick/onQuit)
3. **Phase 3**: HologramLine 方法 (权限检查/可见性更新/显示隐藏)
4. **Phase 4**: HologramManager 方法 (updateVisibility/showAll/hideAll)
5. **Phase 5**: 配置系统优化 (FileConfig/CFG)
