# AGENT Troubleshooting Playbook

本文件记录本仓库中可复用的排障线路。每次读取 `AGENT.md` 后也要读取本文件；遇到相同错误时优先按这里执行，避免重复猜测。

## Gradle Wrapper 启动失败

> **现状（2026-10-27）**：wrapper 已用 Gradle 9.6 重新生成，`gradle-wrapper.jar` 换成标准包，
> `.\gradlew.bat --version` / 构建均可直接运行，分发地址为腾讯镜像的 `gradle-9.6.0-bin.zip`。
> 下面关于 8.7 的排障记录只用于「wrapper 再次损坏」或回退旧版本时参考。

### 典型现象

运行 `.\gradlew.bat ...` 时出现以下任一错误：

- `找不到或无法加载主类 org.gradle.wrapper.GradleWrapperMain`
- `java.lang.ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain`
- `endlocal was unexpected at this time`
- wrapper jar 存在，但 Gradle wrapper 无法启动

### 已知根因

本仓库曾出现过 `gradle/wrapper/gradle-wrapper.jar` 不是标准 Gradle wrapper jar 的情况。脚本 `gradlew.bat` 会尝试启动 `org.gradle.wrapper.GradleWrapperMain`，但 jar 内没有这个类，所以 wrapper 启动失败。

一次排查中，`jar tf gradle\wrapper\gradle-wrapper.jar` 显示 jar 内含有 `org/gradle/launcher/...` 类；`META-INF/MANIFEST.MF` 中的入口是 `org.gradle.launcher.GradleMain`，这说明它更像 Gradle launcher jar，而不是 wrapper jar。

### 排障步骤

1. 先确认工作区状态，避免把排障和业务改动混在一起：

```powershell
git status --short --branch
```

2. 检查 wrapper jar 是否存在：

```powershell
Get-Item gradle\wrapper\gradle-wrapper.jar | Format-List FullName,Length
```

3. 检查 jar 内是否有标准 wrapper 入口：

```powershell
jar tf gradle\wrapper\gradle-wrapper.jar | Select-String 'GradleWrapperMain'
```

4. 如果没有 `GradleWrapperMain`，检查 manifest 和 jar 内容，确认是否是 launcher jar：

```powershell
jar tf gradle\wrapper\gradle-wrapper.jar | Select-Object -First 40
```

不要为了查看 manifest 把 `META-INF/` 解压到仓库根目录；如果必须解压，结束前必须删除临时文件，并确认 `git status` 没有留下它。

5. 检查 wrapper 配置的 Gradle 版本：

```powershell
Get-Content -Raw gradle\wrapper\gradle-wrapper.properties
```

本仓库现在使用 Gradle `9.6.0`；下面的 8.7 缓存路径仅用于回退旧版本或对照历史排障。

6. 如果系统没有全局 `gradle`，先查本机 Gradle wrapper 缓存：

```powershell
Get-ChildItem 'C:\Users\HP\.gradle\wrapper\dists' -Recurse -Directory -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -match 'gradle-8\.7' } |
    Select-Object -First 20 FullName
```

7. 如果找到 `gradle-8.7\bin\gradle.bat`，可用它作为验证命令入口。例如：

```powershell
& 'C:\Users\HP\.gradle\wrapper\dists\gradle-8.7-all\549ctztx5g5jixmk70l9ykwir\gradle-8.7\bin\gradle.bat' :app:testDebugUnitTest
```

```powershell
& 'C:\Users\HP\.gradle\wrapper\dists\gradle-8.7-all\549ctztx5g5jixmk70l9ykwir\gradle-8.7\bin\gradle.bat' :app:assembleDebug
```

8. 在最终回复中明确说明：`.\gradlew.bat` 当前因 wrapper jar 入口异常失败，验证改用了本机缓存的 Gradle 8.7 分发包。

### 不要做的事

- 不要在没有确认 jar 内容前反复修改业务代码。
- 不要随意替换 `gradle-wrapper.jar` 或升级 Gradle/Kotlin/Android 插件，除非用户明确要求或任务就是修 wrapper。
- 不要把解压出的 `META-INF/`、Gradle 缓存、构建产物、APK、class、dex 等加入版本控制。
- 不要把本机绝对路径写进生产代码或构建配置；本机 Gradle 路径只能作为当前机器上的命令行 fallback。

## 通用 Bug 处理线路

1. 完整读取错误输出：保留关键报错、异常类型、文件路径、行号和命令退出码。
2. 先复现再修复：确认同一命令能稳定触发问题；如果不能稳定复现，先收集更多证据。
3. 检查最近状态：运行 `git status --short --branch`，必要时看相关 diff，区分用户已有改动和本次改动。
4. 缩小根因：优先检查直接失败边界，例如脚本入口、配置文件、依赖缓存、DAO 查询、ViewModel 状态、Compose 状态读取。
5. 对比可工作的路径：例如 wrapper 失败时尝试已缓存的 Gradle 分发包；UI 行为异常时找同项目中相似弹框、按钮和状态流写法。
6. 先写可验证条件：有测试框架时优先补单测；没有合适测试时写清手动验收步骤。
7. 修复只针对根因：不要把无关重构、格式化、依赖升级混入同一次 bugfix。
8. 修复后必须重新运行验证命令，不能用“应该可以”代替证据。

## 修改文件注意事项

- 开始前读取 `AGENT.md` 和本文件；再运行 `git status --short --branch`。
- 如果工作区已有未提交改动，默认认为是用户改动；只读必要上下文，不回滚、不覆盖、不格式化无关文件。
- 使用 `apply_patch` 做手工编辑，保持 diff 小而清晰。
- 中文文件使用 UTF-8 读取，例如：

```powershell
[Console]::OutputEncoding=[System.Text.Encoding]::UTF8; Get-Content -Raw -Encoding UTF8 <path>
```

- 修改 Compose UI 时检查：
  - 是否有完整的返回/取消路径。
  - destructive 或覆盖类操作是否有确认弹框。
  - 按钮是否在无数据时禁用或给出清晰反馈。
  - 深色/浅色主题是否仍使用项目现有 token。
  - 触控目标是否保持约 48dp 或以上。

- 修改导入、删除、覆盖类逻辑时检查：
  - 是否明确作用于当前学期或目标学期。
  - 是否避免误删其他学期数据。
  - 覆盖操作是否先确认再执行。
  - 用户取消时是否不写库、不改变导入状态。
  - 完成后是否有成功或失败反馈。

- 结束前检查：

```powershell
git status --short --branch
git diff --name-only
```

确认没有临时文件、构建产物或无关文件混入本次改动。
## KSP 增量编译生成文件缺失

- 典型现象：`kspDebugKotlin`、`kspDebugUnitTestKotlin` 或 `assembleDebug` 报 `java.nio.file.NoSuchFileException`，路径指向 `app/build/generated/ksp/.../hilt_aggregated_deps/...`，看起来像生成文件突然丢了。
- 处理顺序：先跑一次 `:app:clean`，再重跑原来的验证命令；如果清理后恢复正常，就按缓存/增量产物异常处理，不要先怀疑业务代码。
- 如果问题在 clean 之后仍复现，再检查是否有人手工动过 `build/generated/ksp`、是否有并发构建残留，以及是否有旧的增量缓存被别的任务污染。

## KSP 并发写入冲突

- 典型现象：同时跑两个 Gradle 任务时，KSP 报 `FileAlreadyExistsException`，路径通常落在 `app/build/generated/ksp/...`。
- 处理顺序：把验证改成串行，一次只跑一个 Gradle 命令；必要时先 `:app:clean` 再执行。
- 如果同一轮里同时启动了 `assembleDebug` 和 `testDebugUnitTest`，先别把它当成业务编译错误，优先怀疑生成目录被并发写坏了。
## 截取的课表片段解析失败

### 典型现象

- `data.html` 只是网页源代码里的 `tbody` 或 `tr` 片段，不是完整的 `table` 页面。
- 直接 `Jsoup.parse(html)` 后，表格语义可能被打散，`table/tbody/tr` 查找不到，导致导入结果为空。
- 即使单元格里有 `span title="教师"`，只要整张表没被识别，教师、地点、课程名都会一起丢失。

### 处理线路

1. 先确认输入是不是被裁剪过的片段，尤其是只保留了 `<tbody>`、`<tr>`、`<td>` 的情况。
2. 在进入表格解析前先做正规化：如果只有 `tbody`，外层补一层 `table`；如果只有 `tr`，补 `table > tbody`。
3. 再跑表格解析，而不是直接跳到 div/JSON 兜底。
4. 如果教师仍然为空，再检查 `教师` / `任课教师` 这一行是否是带空格而不是带冒号，解析正则要允许两种写法。
5. 修完以后必须用真实片段样本重新跑单测，不能只看示例 HTML 通过。

### 这次会话里的通用提醒

- 遇到“明明页面里有字段，但解析结果为空”时，优先查输入结构，而不是先怪展示层。
- 片段类 HTML 尤其要注意 `tbody` 和 `table` 的边界，`body` 级解析和 `table` 级解析不是一回事。
- 只改正则不一定够，必要时先把 HTML 结构补回去，再让原有解析逻辑工作。

## Compose 页面收不到触摸 / 无法滚动（全屏 Surface 抢命中测试）

### 典型现象

- 页面整体不能上下滑动，页面里的普通按钮全部无响应。
- 同一屏的顶栏/底栏按钮却正常（它们是那个全屏 `Surface` 的子节点，处在命中路径内）。

### 已确认根因

Material3 的 `Scaffold` 内部是一个全屏 `Surface`，而 `Surface` 会额外挂一个空的
`Modifier.pointerInput(Unit) {}` 节点（用于阻止点击穿透）。Compose 的命中测试在某个子节点
拿到直接命中后就不再测试它的兄弟节点：如果页面内容是这个全屏 `Surface` 的兄弟、又画在它下面，
就完全收不到 down / move / up，滚动和 `clickable` 一起失效。与 Vitro shader、手势冲突无关。

### 处理线路

1. 不要把需要交互的页面内容放在全屏 `Surface` / `Scaffold` 的下面当兄弟层。
2. 页面必须留在 `glassBackdrop` 子树里采样时，去掉 `Scaffold`，用普通 `Box` + `Alignment`
   摆放顶栏/底栏浮层（`docs/HANDOFF_LIQUID_GLASS.md` 第四节）。
3. 页面视口若要在状态栏以下裁断，用布局留白（会同时裁掉命中测试），不要只做绘制裁剪。
4. 验收时先截图看视觉，再用 `adb shell input swipe` 看内容是否位移、点一个普通 `clickable`
   看回调；**栏按钮能点不能证明页面命中正常**。

## AGP 9 / Compose 1.12 升级踩点

- **为什么必须升**：Compose 1.12.1 的 AAR 元数据写死 `minCompileSdk=37`、
  `minAndroidGradlePluginVersion=9.1.0`。停留在 AGP 8.x 时 `checkDebugAarMetadata` 会直接失败。
- **工具链**：Gradle 9.6.0 + AGP 9.4.1 + Kotlin（内置）2.4.21 + Compose BOM 2026.09.00。
- **内置 Kotlin**：AGP 9 默认启用，必须移除 `org.jetbrains.kotlin.android` 插件；
  `android.kotlinOptions {}` 换成 `kotlin.compilerOptions {}`，或直接删掉让 jvmTarget
  跟随 `compileOptions.targetCompatibility`。
- **KSP / Hilt**：AGP 9 下分别需要 KSP ≥ 2.3.6、Hilt ≥ 2.59.2（本仓库用 2.3.12 / 2.60.1）。
- **wrapper**：原 wrapper jar 损坏，可在临时目录用新 Gradle 执行
  `gradle wrapper --gradle-version 9.6.0 --distribution-type bin --gradle-distribution-url <镜像地址>`
  后把 `gradlew.bat` 与 `gradle/wrapper/*` 拷回仓库。
- **装机**：vivo 会为 `adb install` 弹「外部来源应用」确认框；改用
  `adb push` + `adb shell pm install -r` 可绕过。
- **验证**：升级后必须重新跑真机滚动/点击/翻周/主题，并对比玻璃静态外观与帧耗时；
  仅编译通过不代表视觉或命中测试没变。

## glassEffect 覆盖层里的按钮/列表点不到（遮罩抢命中）

### 典型现象

- 给周选择面板或课程详情抽屉的容器加 `glassEffect` 后，点周格、编辑、复制、删除都会把面板关掉；
  去掉玻璃就恢复。
- `glassEffect` 本身不消费事件，问题出在 Vitro 非交互玻璃的
  `sharePointerInputWithSiblings() = true`：它会把**同一父层里的兄弟遮罩**也加入命中路径，
  全屏遮罩的 `clickable` 抢在子内容之前响应。

### 处理线路

1. 把遮罩从「面板/抽屉的兄弟」改成「面板/抽屉的父层」：
   ```kotlin
   Box(Modifier.fillMaxSize().clickable { dismiss() }) {   // 遮罩父层
       Box(...glassEffect(...)) { /* 面板内容，子控件优先命中 */ }
   }
   ```
2. 遮罩需要按动画变透明度时，alpha 放在**只负责视觉的子 Box** 上，不要包住面板内容，
   否则面板会跟着整体变透明。
3. 验证时不要只看“能打开”：必须点一个子控件（周格、编辑铅笔、复制/删除），确认回调生效，
   并确认点空白区域仍能关闭。
4. `glassEffectContainer(spacing = 0.dp)` **不会融合**，官方 KDoc 明确零间距只是合并绘制；
   融合需要 `spacing > 0` 且材质相同，组内会使用 lead 的 `tint`，彩色胶囊会失去强调色。
5. `scrollEdgeEffect` 的 veil 是**全宽矩形**：开顶边时会在圆角顶栏两侧、状态栏下方留下方角暗带。
   本仓库因此用 `isTopHidden = true` 只开底边；要顶边必须先按顶栏轮廓裁剪 veil。
