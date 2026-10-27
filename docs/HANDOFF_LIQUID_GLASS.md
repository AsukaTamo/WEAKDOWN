# 交接：顶栏/底栏液态玻璃（Vitro 方案）

> 一句话现状：**顶栏 / 底栏已接入 Vitro 液态玻璃，浅深材质的静态外观正常；
> 但当前真机仍报告页面无法纵向移动、除顶栏和底栏外的按钮无响应，交互验收未通过。**
> 改动全部在分支 `Lingfan666/feature-liquid-glass` 上，**没有合入 main**。
>
> 材质细节、参数标定依据、更新上游的步骤在 [VITRO_LIQUID_GLASS.md](VITRO_LIQUID_GLASS.md)；
> 本文只讲「交接要做的事」。

## 一、分支与推送关系

```
main (404d2ee, == origin/main)
 └─ Lingfan666/feature-liquid-glass   ← 本分支，只做液态玻璃这一件事
       ← 从 main 拉出，未合入
```

| 项 | 值 |
|---|---|
| 当前分支 | `Lingfan666/feature-liquid-glass`（基线 commit `404d2ee`） |
| 推送目标 | `origin` = `github.com/AsukaTamo/WEAKDOWN`，分支 `Lingfan666/feature-liquid-glass` |
| 镜像远端 | `gitee` = `gitee.com/lingfan555/weakdown`（同一条分支如需同步，自己再推一次） |
| **没做**的事 | 没推 `main`、没建 PR、没动远端其它分支 |
| 合入方式 | 由你在 GitHub 上开 PR / 本地 merge，AI 不代做 |

拉下来之后：

```powershell
git fetch origin
git switch Lingfan666/feature-liquid-glass
```

## 二、改了哪些东西

| 文件 | 改动 | 为什么 |
|---|---|---|
| `app/src/main/java/io/github/itshedimisawi/vitro/**` | 新增（34 个 `.kt` + `LICENSE-vitro.txt`） | vendor 上游 Vitro 0.1.0，顶栏/底栏的玻璃材质 |
| `app/src/main/java/com/example/courseapp/MainActivity.kt` | 背景、壁纸和当前可见课表统一进入 `glassBackdrop`；顶栏/底栏改 `.glassEffect(...)`；页面内容不再被栏的整体避让区域隔开；删掉原磨砂渐变/彩虹描边/`.shadow()`；次级文字色换 `textSecondary`；`LocalGlassDarkTheme` 跟随 App 主题 | 玻璃要采样真实滚动内容，且亮玻璃上 muted 灰只有 2.6:1 |
| `app/src/main/java/com/example/courseapp/ui/schedule/ScheduleScreen.kt` | 增加 `contentPadding` 参数；把顶/底安全区留白放入统一纵向滚动内容；时间轴和七天课程列共享滚动范围 | 课程首尾可完整露出，中间课程可以经过顶栏/底栏并被 Vitro 实时折射 |
| `ui/components/LiquidGlass.kt` | **删除** | 自研着色器的真折射一直没打通，改走 Vitro；历史在 `404d2ee` 里可找回 |
| `app/build.gradle.kts` | `compileSdk 36 → 37` | 与 Vitro 的编译基线一致（`targetSdk` 仍 36） |
| `gradle.properties` | 新增 `android.suppressUnsupportedCompileSdk=36,37` | AGP 8.2.2 官方只测到 34，仅压提示 |
| `docs/VITRO_LIQUID_GLASS.md` | 新增 | 方案、参数标定、验证、更新上游步骤 |
| `docs/LIQUID_GLASS_TODO.md` | 顶部加状态说明，文件清单更新 | 旧的自研路线归档为历史记录 |
| `docs/PROJECT_STRUCTURE.md` | 补 vendor 小节、compileSdk 更新 | 结构说明别过期 |

本轮继续保留首页表头避让和滚动布局调整：`ScheduleGrid` 使用一个纵向滚动容器，
并把顶/底栏避让作为滚动内容的 spacer；`MainActivity` 的页面留白取栏位实测尺寸与
系统 inset/尺寸令牌兜底值中的较大值，`Scaffold` 内容槽保持空。调整后截图中日期表头
位于顶栏下方。**以上调整没有解决真机纵向手势与页面按钮无响应问题，不要据此标记交互通过。**

刻意**没有**提交：`.idea/deviceManager.xml`（你本机的 IDE 状态）。

## 三、换机器要复现的 4 件事

1. **SDK**：需要 `platforms/android-37`（来源 `platform-37.0_r02.zip`），`android-36` 保留即可。
   装好后**必须**把 `platforms/android-37/source.properties` 里的
   `AndroidVersion.ApiLevel=37.0` 改成 `37`，否则 AGP 8.2.2 报
   `Failed to find Platform SDK with path: platforms;android-37`（本机原文件备份为 `source.properties.bak`）。
2. **Gradle**：本机 `gradlew.bat` 的 wrapper jar 是坏的，用缓存 8.7 分发包（见 `AGENT-TROUBLESHOOT.md`）。
3. **不要指望上游自带的构建**：Vitro 仓库自己用 Gradle 9.6 + AGP 9.4.1 + Kotlin 2.4.21 + Compose 1.12.1，
   本机从 `services.gradle.org` 下载 Gradle 分发包一直卡在 0 字节，所以是「vendor 源码 + 本项目 Compose 1.7.5」，
   只差一处 `UnplacedAwareModifierNode` 兼容改动（见 VITRO 文档第四节）。
4. **真机闭环**：`AGENT.md` 里的 adb 路径与命令；本机设备 `10CG5J0Q0F004E7`。

## 四、验证记录（本轮实际跑过的）

| 命令 | 结果 |
|---|---|
| `gradle :app:assembleDebug` | ✅（compileSdk 37，无 error） |
| `gradle :app:testDebugUnitTest` | ✅ |
| `:app:assembleDebug :app:testDebugUnitTest --offline`（本轮布局调整后） | ✅ 通过；使用本机缓存 Gradle 8.7 |
| 装机 + 冷启动 + 截图 | ✅ 页面可启动；玻璃质感正常，调整后日期表头位于顶栏下方 |
| 浅/深主题 | ⚠️ 有静态画面；本轮未完成系统化交互回归 |
| 首页课表纵向拖动 | ❌ 真机截图中未观察到内容位移；用户也报告首页及后续页面均无法上下滑动 |
| 页面按钮 | ❌ 用户报告除顶栏、底栏按钮外，其余按钮无响应；本轮未定位根因 |
| 课程穿过玻璃的动态折射 | ⏸ 未验收；由于滚动未工作，不能用静态截图判定通过 |
| 本轮帧耗时与卡顿率 | ⏸ 未测；历史首次接线数据不代表当前布局 |
| 首尾课程可达、横向翻周、课程点击/长按、弹层返回 | ⏸ 未完成回归 |

旧记录中的 p50 8ms / p90 17ms / p99 23ms、janky 4.77%、missed vsync 0，
只代表首次 Vitro 接线的历史基线。当前版本没有新的性能测量结果。

### 本轮交接问题：页面纵向手势和普通控件无响应

- **现象**：用户报告首页课表及后续页面均无法上下滑动，除顶栏/底栏按钮外其余按钮失去原有功能。
  本轮真机课表前后截图没有观察到纵向位移；具体普通控件点击是否全部失效尚未逐项确认。
- **环境**：设备 `10CG5J0Q0F004E7`（V2520A，Android 版本以设备为准），应用包名 `com.example.courseapp`；
  本轮 APK 已安装并可启动。
- **已尝试但未证实有效的改动**：课表网格合为单一纵向滚动容器；顶/底栏避让改为滚动内容 spacer；
  页面留白使用测量值与系统安全区兜底值的较大者；移除 Scaffold 内容槽中的全屏透明 `Spacer`。
  构建和单测通过，但装机后仍未观察到滚动位移，不能把这些改动记为问题修复。
- **当前判断**：根因未定位。页面移入全屏 `glassBackdrop` 并与 `Scaffold` 分为兄弟层后，视觉层级与输入命中
  层级需要继续核对；这是排查方向，不是已确认原因。勿先归咎于 Vitro shader 或继续叠加手势补丁。
- **下次排查建议**：先用可重复的交互测试确认触摸事件是否到达当前页面（记录 ScrollState 的 `value/maxValue`，
  并用应用内日志或 Compose 测试确认普通 `clickable` 回调）；检查 root `Box`、backdrop、常驻隐藏页的
  placement/hit-test 和 Scaffold 空槽的节点层级；再按最小结构复现并修复。修复后重新验收首页滚动、
  管理/我的页列表滚动、普通设置按钮、课表点击/长按、横向翻周及玻璃内动态采样。

材质与原型图的数值对齐（真机截图像素）：

| | 本实现 | 原型图 |
|---|---|---|
| 浅色玻璃体 / 高光 | 204 / 231 | 207 / 228 |
| 深色顶栏 / 底栏 | 60 / 33 | 57 / 25 |

## 五、已知差距与下一步（按性价比排序）

1. **先解决上方的交互回归**：在真机上确认纵向手势和页面控件恢复，再继续动态折射与性能验收。
2. **彩色边（原型的美术化「色彩溢出」）**：Vitro 的 `dispersion` 是物理 RGB 分离，背景越平越看不出彩色；
   要那种沿边一圈的彩虹，得在玻璃外再叠一层极淡的 `sweepGradient` 描边（~0.6dp）。
3. **其它面板没换材质**：周选择面板、作息设置、课程详情抽屉还是实色面板，可以复用同一个 `backdrop`。
4. **顶栏 + 周数胶囊融合**：用 `glassEffectContainer` + `spacing = 0.dp` 能做成一块玻璃。
5. **性能贴线**：历史首次接线 p90 = 17ms；当前课表采样布局需要重新测量。若仍贴线，可以只在有壁纸时开玻璃、其它页面回到纯色/磨砂。
6. **要用 Vitro 1.12+ 的新 API**：得把 Compose 升上去，并撤回第四节的兼容改动。

## 六、回滚

```powershell
git switch main            # 回到未接玻璃的状态
# 或者只回滚这一个提交
git revert <commit>
```

`ui/components/LiquidGlass.kt` 已删除，需要旧实现时从 `404d2ee` 取。

## 七、五分钟上手

1. 打开 `MainActivity.CourseApp()`，找到 `glassMaterial`（材质参数都在这一处）；
2. 需要调手感就改 `brightness` / `tint` / `dispersion` / `blurRadius`，字段含义见 `Glass.kt` 的 KDoc；
3. 构建 → 装机 → 冷启动 → 截图（命令见 `AGENT.md`「真机调试方法」）；
4. 对着原型图看三件事：玻璃体的亮度、边缘高光是否成圈、背景是否被真正折射（拖动课表时看栏下方的课程色块、文字和壁纸有没有随内容移动并产生边缘折射）。
