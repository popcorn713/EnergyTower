# 能量塔科技模组（Energy Tower）

一个基于 **Java 21 + NeoForge 1.21.1** 的科技模组，使用 **NeoForge 标准 FE 能量系统**。

## 功能

- **能量塔（Energy Tower）**
  - 储能方块：接收任何使用 NeoForge 标准 FE 接口（`Capabilities.EnergyStorage.BLOCK`）的线缆 / 机器输入电能，内置 **1000 万 FE** 缓存，**输入速度无上限**（仅受剩余容量限制）。
  - **不自产电**：电量耗尽立即停止无线供电，必须外部持续补给。
  - **无线供电**：储存电能后，向绑定设备无线分发能量，无需电线，**无距离限制**（同维度）。
- **能量链路扳手（Energy Link Wrench）**
  - `Shift+右键` 能量塔：选定为无线发射源。
  - `Shift+右键` 机器：绑定为无线供电目标（一对多）；对已绑定机器再右键 → 单独解绑。
  - `Shift+Ctrl+右键` 机器：一键批量绑定所有相邻（相连）的同种机器。
  - `右键` 能量塔：打开控制面板（储能显示、传输速率：滑块(1~32000)/预设档(100/1k/5k/10k/32k)/自定义输入(1~400000，超出自动设为400000)、速率限制/无上限模式、带二次确认的【清除全部绑定】）。
  - 手持扳手：高亮显示已选能量塔（青）与绑定机器（绿），被拆机器自动不再高亮。
  - 副手持有：之后放置的可接收 FE 机器自动绑定到已选能量塔。
  - 绑定的机器被拆后链接自动断开（唯一 ID 识别，原位重放同类型机器也不残留）。

## 设计要点

- 所有能量计算、绑定关系、无线传输逻辑**全部在服务端**；客户端仅负责高亮渲染、粒子、GUI。
- 绑定清单（含机器唯一 ID）、传输速率、模式、能量缓存通过方块实体 NBT **持久化**，退出重进世界不丢失（跨维度绑定加载时丢弃）。
- 无线传输不设最大距离限制。
- 客户端 → 服务端数据包（调速率 / 切模式 / 清空 / 批量绑定）仅传递意图，服务端校验后才生效，防作弊。
- 仅使用 NeoForge 1.21.1 正式 API（`DeferredRegister`、能力系统、附件系统、`CustomPacketPayload` 网络、`IMenuTypeExtension`）。

## 工程导入 / 测试 / 打包

1. **导入**：用 IntelliJ IDEA（推荐）或 Eclipse 打开 `EnergyTower/` 目录，等待 Gradle 同步（JDK 21）。
2. **运行**：终端执行 `.\gradlew.bat runClient` 启动客户端测试；`.\gradlew.bat runServer` 启动服务端。
3. **打包**：`.\gradlew.bat build`，产物在 `build/libs/energy_tower-<version>.jar`，放入游戏 `mods/` 文件夹即可。
4. **贴图**：修改 `tools/gen_resources.py` 后用 Python 重新生成 `src/main/resources/assets/energy_tower/textures/`。

## 目录结构

```
src/main/java/com/energytower/
├── EnergyTower.java                 主类（注册 + 事件总线接线）
├── ModBlocks.java                   方块注册
├── ModItems.java                    物品注册
├── ModBlockEntities.java            方块实体注册 + FE 能力暴露
├── ModMenuTypes.java                GUI 菜单类型注册
├── ModCreativeTabs.java             创造标签页
├── ModAttachments.java              附件类型（机器绑定唯一 ID）
├── ModClientEvents.java             客户端事件（屏幕注册）
├── block/EnergyTowerBlock.java      能量塔方块
├── blockentity/EnergyTowerBlockEntity.java  能量塔方块实体（核心逻辑）
├── energy/ModEnergyStorage.java     可持久化 FE 存储
├── energy/EnergyTransferUtil.java   标准 FE 探测工具（方向探测 / 可接收判断）
├── item/EnergyLinkWrenchItem.java   能量链路扳手
├── menu/EnergyTowerMenu.java        控制面板菜单（数据槽同步）
├── menu/EnergyTowerScreen.java      控制面板屏幕（滑块 + 预设档 + 自定义输入框 + 模式 + 清除按钮）
├── network/                         网络包（C2S 调速/切模式/清空/批量绑定 + S2C 高亮）
├── server/WrenchSelectionManager.java  服务端玩家选择状态
├── server/MassBindTracker.java      批量绑定保护（防误解绑）
├── server/AutoBindHandler.java      副手自动绑定
└── client/                          客户端高亮数据与渲染、批量绑定检测
```

### 配方

- 能量塔：`III / IRI / IGI`（I=铁锭，R=红石块，G=玻璃）
- 能量链路扳手：` I  / IRI /  I `（I=铁锭，R=红石）
