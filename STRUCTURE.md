# SimpRewards 架构设计文档

## 1. 项目定位

SimpRewards 是一个基于 Paper API 的 Minecraft 奖励插件，目标是通过 GUI 为玩家提供每日签到、累计在线、每日在线任务、每周在线任务、连续活跃天数奖励等功能。

当前项目仍处于插件骨架阶段，仅包含主插件类、`plugin.yml`、`pom.xml` 和需求文档。本文档根据 `docs/SimpRewards.md` 梳理目标架构，用于后续实现时统一模块边界、数据结构和调用流程。

## 2. 当前项目结构

```text
SimpRewards/
├── docs/
│   └── SimpRewards.md
├── src/
│   └── main/
│       ├── java/
│       │   └── org/simpmc/simpRewards/
│       │       └── SimpRewards.java
│       └── resources/
│           └── plugin.yml
├── pom.xml
└── STRUCTURE.md
```

现状说明：

- `SimpRewards.java` 仅包含 Paper 插件生命周期入口。
- `plugin.yml` 已声明插件主类、版本、API 版本和基础元信息。
- `pom.xml` 使用 Java 21，并依赖 Paper API `1.21.11-R0.1-SNAPSHOT`。
- 暂无命令、事件监听、配置加载、GUI、奖励发放或持久化实现。

## 3. 需求边界

插件需要支持五类奖励：

| 奖励类型 | 状态维度 | 重置规则 | 领取方式 | 领取次数 |
| --- | --- | --- | --- | --- |
| 每日签到 | 今日是否签到、连续签到天数 | 每日 00:00 | GUI 点击 | 每日一次 |
| 总累计在线 | 玩家终身累计在线分钟数 | 不重置 | GUI 点击 | 每档一次 |
| 每日在线任务 | 当日在线分钟数、今日是否领取 | 每日 00:00 | GUI 点击 | 每日一次 |
| 每周在线任务 | 本周在线分钟数、本周是否领取 | 每周一 00:00，可配置 | GUI 点击 | 每周一次 |
| 连续活跃奖励 | 每日活跃达标、连续活跃天数、档次领取状态 | 日历连续判断 | GUI 点击 | 每档一次 |

奖励内容必须由配置驱动，插件本身不应写死具体物品、经济金额、经验、命令或称号奖励。

## 4. 目标分层

建议采用“入口层、应用服务层、领域模型层、基础设施层、展示层”的轻量分层，避免把所有逻辑堆进主类或 GUI 事件中。

```text
org.simpmc.simpRewards
├── SimpRewards.java
├── command/
├── config/
├── data/
├── gui/
├── listener/
├── reward/
├── service/
├── time/
└── util/
```

### 4.1 插件入口层

建议文件：

```text
org/simpmc/simpRewards/SimpRewards.java
```

职责：

- 保存插件单例或核心上下文引用。
- 在 `onEnable` 中初始化配置、存储、服务、命令、监听器、定时任务。
- 在 `onDisable` 中保存在线玩家状态并关闭持久化资源。
- 不直接承载奖励判断、GUI 渲染或数据读写细节。

启动顺序建议：

1. 保存默认配置文件。
2. 加载配置与消息文本。
3. 初始化存储仓库。
4. 初始化时间与奖励服务。
5. 注册命令和事件监听器。
6. 启动在线时长定时记录任务。
7. 为当前已在线玩家建立会话状态。

### 4.2 配置层

建议包：

```text
config/
├── ConfigManager.java
├── RewardConfig.java
├── GuiConfig.java
├── MessageConfig.java
└── RewardDefinitionParser.java
```

职责：

- 加载 `config.yml`、`gui.yml`、`messages.yml` 等配置文件。
- 将 YAML 转换为强类型配置对象。
- 校验奖励档位、槽位、材质、时间阈值和命令格式。
- 支持 `/reward reload` 热重载。

建议配置文件：

```text
src/main/resources/
├── plugin.yml
├── config.yml
├── gui.yml
└── messages.yml
```

配置拆分建议：

- `config.yml`：规则配置、奖励配置、存储配置、命令别名。
- `gui.yml`：菜单标题、大小、槽位、图标、名称、Lore、装饰项。
- `messages.yml`：聊天提示、ActionBar 文本、错误信息、重载提示。

### 4.3 数据与持久化层

建议包：

```text
data/
├── PlayerRewardState.java
├── PlayerSession.java
├── PlayerRewardRepository.java
├── YamlPlayerRewardRepository.java
└── StorageType.java
```

职责：

- 保存玩家奖励状态。
- 从磁盘加载玩家状态。
- 定期或在关键事件中落盘。
- 屏蔽 YAML、SQLite、MySQL 等存储实现差异。

初期建议使用 YAML 文件存储，便于快速实现：

```text
plugins/SimpRewards/
├── config.yml
├── gui.yml
├── messages.yml
└── data/
    └── players/
        └── <uuid>.yml
```

后续如果玩家规模较大，可通过 `PlayerRewardRepository` 接口扩展到 SQLite。

核心玩家状态建议：

```text
PlayerRewardState
├── uuid
├── name
├── totalOnlineMinutes
├── dailyOnlineMinutes
├── weeklyOnlineMinutes
├── lastSeenDate
├── lastDailyResetDate
├── lastWeeklyResetWeek
├── signedDate
├── signStreak
├── activeStreak
├── activeDates
├── claimedTotalMilestones
├── claimedDailyDate
├── claimedWeeklyWeek
└── claimedActiveStreakMilestones
```

说明：

- 日期建议以服务器时区的 `LocalDate` 为准。
- 周期标识建议使用 `YearWeek` 或自定义字符串，如 `2026-W26`。
- 所有已领取档位使用集合保存，避免重复领取。
- 在线分钟数只存整数分钟，GUI 可根据需要格式化为小时或天。

### 4.4 时间与重置层

建议包：

```text
time/
├── TimeService.java
├── ResetService.java
└── WeekKey.java
```

职责：

- 统一提供服务器时区下的当前日期、时间和周标识。
- 判断玩家数据是否需要执行每日或每周重置。
- 处理连续签到、连续活跃天数中断逻辑。

关键规则：

- 每日重置以服务器时区 00:00 为边界。
- 每周重置默认周一 00:00，起始星期可配置。
- 玩家登录、打开 GUI、领取奖励、定时统计在线时长前，都应先执行惰性重置检查。
- 不建议依赖一个必须在 00:00 准时执行成功的全局任务；应以玩家状态中的最后重置日期做幂等判断。

### 4.5 在线统计服务

建议包：

```text
service/
├── OnlineTimeService.java
└── PlayerSessionService.java
```

职责：

- 玩家登录时创建在线会话。
- 玩家退出时结算剩余在线时长并保存。
- 定时按分钟累加在线时长。
- 更新总累计、每日、每周在线分钟数。
- 达到每日活跃门槛时标记当天活跃，并维护连续活跃天数。

统计策略建议：

- 使用 Bukkit Scheduler 每 60 秒扫描在线玩家并累加 1 分钟。
- 玩家退出时可根据会话开始时间补齐不足一个周期的时间，减少服务器重启或退出造成的误差。
- 在线统计服务只负责记录时间，不负责发奖。

### 4.6 奖励领域层

建议包：

```text
reward/
├── RewardType.java
├── RewardDefinition.java
├── RewardMilestone.java
├── RewardResult.java
├── RewardGrantService.java
└── RewardEligibilityService.java
```

职责：

- 判断某个奖励是否满足领取条件。
- 执行奖励发放。
- 记录领取状态。
- 返回成功、已领取、不满足条件、配置错误等结果。

奖励类型建议：

```text
RewardType
├── DAILY_SIGN
├── TOTAL_ONLINE
├── DAILY_ONLINE
├── WEEKLY_ONLINE
└── ACTIVE_STREAK
```

`RewardDefinition` 建议支持：

- 物品奖励：材质、数量、名称、Lore、附魔、NBT 或 CustomModelData。
- 经济奖励：金额，依赖 Vault 时启用。
- 经验奖励：经验值或等级。
- 命令奖励：控制台命令或玩家命令，支持变量。
- 消息与音效：领取成功后的反馈。

常用变量建议：

```text
%player%
%uuid%
%total_minutes%
%daily_minutes%
%weekly_minutes%
%sign_streak%
%active_streak%
%milestone%
```

### 4.7 命令层

建议包：

```text
command/
├── RewardCommand.java
└── RewardCommandTabCompleter.java
```

命令建议：

```text
/reward
/reward open [player]
/reward reload
/reward stats [player]
/daily
```

权限建议：

```text
simprewards.use
simprewards.open
simprewards.admin
simprewards.reload
simprewards.stats
```

职责：

- `/reward` 和 `/daily` 打开奖励 GUI。
- `/reward reload` 重载配置。
- `/reward stats` 查看玩家奖励状态，便于调试和运维。
- 命令层只处理权限、参数和用户反馈，不直接写奖励数据。

### 4.8 GUI 展示层

建议包：

```text
gui/
├── RewardMenu.java
├── RewardMenuRenderer.java
├── RewardMenuHolder.java
├── RewardButton.java
├── RewardButtonType.java
└── ItemBuilder.java
```

职责：

- 渲染 4x9 奖励主界面。
- 根据玩家状态动态生成图标、名称和 Lore。
- 处理点击事件并调用奖励服务。
- 领取后刷新当前菜单。

GUI 推荐槽位：

| 槽位 | 功能 |
| --- | --- |
| 0 | 每日签到总览 |
| 1 | 累计在线总览 |
| 2 | 每日任务总览 |
| 3 | 每周任务总览 |
| 4 | 连续在线总览 |
| 9 | 签到领取按钮 |
| 10 | 累计在线奖励按钮 |
| 11 | 每日在线任务按钮 |
| 12 | 每周在线任务按钮 |
| 13 | 连续活跃奖励按钮 |
| 14-17 | 公告或装饰 |
| 27-35 | 底部装饰 |

GUI 状态建议：

- `LOCKED`：未满足条件。
- `CLAIMABLE`：可领取。
- `CLAIMED`：已领取。
- `INFO`：仅展示信息。
- `DECORATION`：装饰项，不处理点击。

### 4.9 监听器层

建议包：

```text
listener/
├── PlayerConnectionListener.java
└── InventoryClickListener.java
```

职责：

- `PlayerJoinEvent`：加载状态、执行惰性重置、创建在线会话。
- `PlayerQuitEvent`：结算在线时长、保存状态、移除会话。
- `InventoryClickEvent`：识别插件 GUI，阻止物品被拿走，分发按钮点击。
- `InventoryCloseEvent`：必要时清理临时 GUI 上下文。

### 4.10 工具层

建议包：

```text
util/
├── Texts.java
├── Materials.java
├── Durations.java
└── Placeholders.java
```

职责：

- 颜色代码和 MiniMessage 文本处理。
- 安全解析 Bukkit `Material`。
- 分钟、小时、天的格式化。
- 配置变量替换。

## 5. 核心流程

### 5.1 玩家登录

```text
PlayerJoinEvent
→ PlayerRewardRepository.load(uuid)
→ ResetService.applyResetsIfNeeded(state)
→ PlayerSessionService.start(player)
→ PlayerRewardRepository.save(state)
```

登录时不建议自动发放所有奖励。每日签到可根据配置选择是否首次登录自动领取；如果未开启自动领取，则只更新 GUI 中的可领取状态。

### 5.2 在线时长累计

```text
Bukkit Scheduler 每 60 秒
→ 遍历在线玩家
→ ResetService.applyResetsIfNeeded(state)
→ OnlineTimeService.addMinute(state)
→ 判断每日活跃门槛
→ 必要时保存状态
```

该流程只更新进度，不自动领取奖励。

### 5.3 打开奖励 GUI

```text
/reward 或 /daily
→ 加载玩家状态
→ ResetService.applyResetsIfNeeded(state)
→ RewardMenuRenderer.render(player, state)
→ player.openInventory(menu)
```

GUI 所有文字、图标、Lore、音效应从配置读取。

### 5.4 点击领取

```text
InventoryClickEvent
→ RewardButton.resolve(slot)
→ RewardEligibilityService.check(type, state)
→ RewardGrantService.grant(player, rewardDefinition)
→ 更新领取状态
→ 保存玩家状态
→ 刷新 GUI
```

必须保证领取逻辑幂等：

- 重复点击不会重复发奖。
- 背包空间不足时，应按配置选择取消发放、掉落地面或延迟领取。
- 奖励发放失败时，不应提前写入已领取状态。

### 5.5 每日重置

```text
ResetService.applyDailyReset(state)
→ 如果 state.lastDailyResetDate < today
→ 清空 dailyOnlineMinutes
→ 清空 claimedDailyDate
→ 判断签到是否断签
→ 判断活跃是否断档
→ 更新 lastDailyResetDate
```

断签规则：

- 默认不补签，缺失一天则连续签到归零。
- 如果后续开启补签，需要单独设计补签凭证、可补天数和补签奖励，不应混入正常签到逻辑。

### 5.6 每周重置

```text
ResetService.applyWeeklyReset(state)
→ 如果 state.lastWeeklyResetWeek < currentWeek
→ 清空 weeklyOnlineMinutes
→ 清空 claimedWeeklyWeek
→ 更新 lastWeeklyResetWeek
```

## 6. 配置设计草案

### 6.1 config.yml

```yaml
timezone: server

commands:
  aliases:
    - reward
    - daily

sign:
  auto-claim-on-first-join: false
  streak-cycle: 7
  allow-makeup: false
  rewards:
    base: []
    streaks: {}

online:
  tick-minutes: 1

daily-task:
  target-minutes: 30
  rewards: []

weekly-task:
  week-start: MONDAY
  target-minutes: 300
  rewards: []

total-online:
  milestones: {}

active-streak:
  active-threshold-minutes: 30
  milestones: {}

storage:
  type: yaml
  autosave-minutes: 5
```

### 6.2 gui.yml

```yaml
menu:
  title: "奖励中心"
  size: 36

items:
  daily-sign:
    slot: 9
    locked: {}
    claimable: {}
    claimed: {}
  total-online:
    slot: 10
  daily-task:
    slot: 11
  weekly-task:
    slot: 12
  active-streak:
    slot: 13
```

### 6.3 messages.yml

```yaml
prefix: "&6[奖励]&r "
claim-success: "&a奖励领取成功。"
already-claimed: "&e该奖励已经领取过了。"
not-enough-progress: "&c条件未满足，还需要 %remaining% 分钟。"
reload-success: "&aSimpRewards 配置已重载。"
```

## 7. 存储设计草案

单个玩家 YAML 示例：

```yaml
uuid: "00000000-0000-0000-0000-000000000000"
name: "Player"
total-online-minutes: 120
daily-online-minutes: 20
weekly-online-minutes: 80
last-seen-date: "2026-06-21"
last-daily-reset-date: "2026-06-21"
last-weekly-reset-week: "2026-W25"
signed-date: "2026-06-21"
sign-streak: 3
active-streak: 2
active-dates:
  - "2026-06-20"
  - "2026-06-21"
claimed-total-milestones:
  - 30
  - 60
claimed-daily-date: "2026-06-21"
claimed-weekly-week: ""
claimed-active-streak-milestones:
  - 3
```

## 8. 外部依赖设计

当前只依赖 Paper API。根据需求，后续可能增加：

- Vault：用于经济奖励，建议设为软依赖。
- PlaceholderAPI：用于展示更多变量，建议设为软依赖。
- MiniMessage 或 Adventure：Paper 已包含 Adventure，可优先使用。

`plugin.yml` 后续建议补充：

```yaml
commands:
  reward:
    description: Open reward menu
    aliases: [daily]
    permission: simprewards.use

permissions:
  simprewards.use:
    default: true
  simprewards.admin:
    default: op

softdepend:
  - Vault
  - PlaceholderAPI
```

## 9. 实现优先级

建议分阶段实现：

1. 基础框架：配置加载、命令注册、玩家状态 YAML 存储。
2. 在线统计：登录、退出、定时累加、每日和每周惰性重置。
3. GUI 框架：菜单渲染、槽位映射、点击拦截、状态刷新。
4. 奖励发放：物品、经验、命令奖励。
5. 奖励规则：每日签到、每日任务、每周任务、累计在线、连续活跃。
6. 管理能力：重载、调试状态、错误日志、配置校验提示。
7. 扩展能力：Vault 经济、PlaceholderAPI、SQLite 存储。

## 10. 关键设计约束

- 主线程安全：Bukkit API 调用必须在主线程执行，异步存储不得直接操作 Bukkit 对象。
- 奖励幂等：领取状态必须在奖励成功发放后再写入。
- 配置优先：奖励内容、GUI 元素、提示文本和阈值不得写死在代码中。
- 惰性重置：每日和每周重置应通过玩家状态判断，避免依赖定时任务准点执行。
- 数据可迁移：玩家状态字段应保持清晰稳定，后续添加版本号便于升级。
- GUI 隔离：只处理带有自定义 `InventoryHolder` 的插件菜单，避免误拦截其他菜单。
- 错误可见：配置错误应在启动和重载时明确记录到日志，不能静默失败。

## 11. 推荐后续目录结构

```text
src/main/java/org/simpmc/simpRewards/
├── SimpRewards.java
├── command/
│   ├── RewardCommand.java
│   └── RewardCommandTabCompleter.java
├── config/
│   ├── ConfigManager.java
│   ├── GuiConfig.java
│   ├── MessageConfig.java
│   ├── RewardConfig.java
│   └── RewardDefinitionParser.java
├── data/
│   ├── PlayerRewardRepository.java
│   ├── PlayerRewardState.java
│   ├── PlayerSession.java
│   ├── StorageType.java
│   └── YamlPlayerRewardRepository.java
├── gui/
│   ├── ItemBuilder.java
│   ├── RewardButton.java
│   ├── RewardButtonType.java
│   ├── RewardMenu.java
│   ├── RewardMenuHolder.java
│   └── RewardMenuRenderer.java
├── listener/
│   ├── InventoryClickListener.java
│   └── PlayerConnectionListener.java
├── reward/
│   ├── RewardDefinition.java
│   ├── RewardEligibilityService.java
│   ├── RewardGrantService.java
│   ├── RewardMilestone.java
│   ├── RewardResult.java
│   └── RewardType.java
├── service/
│   ├── OnlineTimeService.java
│   └── PlayerSessionService.java
├── time/
│   ├── ResetService.java
│   ├── TimeService.java
│   └── WeekKey.java
└── util/
    ├── Durations.java
    ├── Materials.java
    ├── Placeholders.java
    └── Texts.java
```

## 12. 测试与验证建议

虽然 Paper 插件的端到端测试通常需要服务端环境，仍建议保留可单元测试的纯 Java 逻辑：

- `ResetService`：验证每日重置、每周重置、断签和连续活跃中断。
- `RewardEligibilityService`：验证各类奖励是否可领取。
- `RewardDefinitionParser`：验证配置解析和错误提示。
- `WeekKey`：验证不同周起始日下的周标识。
- `Durations` 与 `Placeholders`：验证格式化与变量替换。

需要服务器环境验证的内容：

- `/reward` 和 `/daily` 是否正常打开 GUI。
- GUI 点击是否不会移动物品。
- 奖励发放、背包满处理和领取状态保存是否正确。
- 玩家退出、服务器重启后在线时长是否正确保存。
- 每日 00:00 和每周一 00:00 前后的重置行为是否符合预期。
