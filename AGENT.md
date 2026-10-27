# AGENTS.md

本文件是本仓库内 AI 助手需要遵守的项目级工作规范。适用范围为仓库根目录及其所有子目录；若用户在对话中给出更具体的要求，以用户最新明确要求为准。

## 项目概况

- 项目名称：CourseApp。
- 技术栈：Android、Kotlin 2.4、Jetpack Compose 1.12.1（BOM 2026.09）、Gradle 9.6 / AGP 9.4.1、Gradle Kotlin DSL、Hilt、Room、Jsoup；网络层（Retrofit/OkHttp）已移除。
- 根模块：`:app`。
- 构建命令优先使用 Windows Gradle Wrapper：`.\gradlew.bat ...`；wrapper 已用 Gradle 9.6 重新生成，
  分发地址指向腾讯镜像。历史文档里的「wrapper jar 异常、改用缓存 Gradle 8.7」只适用于升级前的旧版本。

## 仓库与分支规范

- `main` 是唯一主链路，默认不得直接在 `main` 上实现新功能、修复 bug 或做优化。
- 一个分支只负责一个功能、一个修复或一个优化；每次开始新的开发任务前都需要新建分支。
- 分支命名默认使用 `Lingfan666/<type>-<short-description>`，例如 `Lingfan666/feature-import-preview`、`Lingfan666/fix-schedule-crash`、`Lingfan666/docs-agent-guidelines`。
- 新分支应从当前本地 `main` 或用户指定基线创建；若当前工作区已有未提交改动，必须先说明状态并避免覆盖、回滚或混入无关改动。
- 不允许私自推送代码到远端；任何 `git push`、创建远端分支或打开 PR 都必须由用户自行操作，除非用户在当前对话中明确授权推送。
- 不允许执行破坏性 Git 操作，例如 `git reset --hard`、强制覆盖、删除用户改动；除非用户明确要求并确认风险。
- 不主动提交代码。若用户提出“可以 commit”或需要准备 PR，必须先给出 comment，包括变更摘要、验证结果、风险点、建议 commit message 或 PR 描述；即使完成本地 commit，也不能自主 push。

## 需求确认与计划规范

- 在执行需求前，如果存在会影响实现结果的模糊点，必须逐条向用户确认；每个问题只确认一个具体决策点。
- 禁止在关键需求不明确时直接开始实现，例如目标页面、交互行为、数据来源、兼容范围、视觉风格、验收标准不清楚时必须先问。
- 若细节不影响最终结果，或已有项目惯例可参考，可以按惯例继续，但必须在回复中说明所采用的假设。
- 对多步骤任务，应先给出简洁计划；计划应包含实现、验证和交付说明，不写空泛步骤。
- 开始实现后应持续推进到可验证状态，不停留在分析或建议层面，除非用户明确只要求方案。

## Skills 使用规范

- 每次执行任务前都必须使用 superpowers 相关流程；优先使用 `superpowers:using-superpowers`，并按任务类型加载对应 superpowers skill。
- 每次 code review、定位功能段、追踪调用关系、评估影响范围时，都必须调用 `code-graph` skill。
- 前端 UI、交互、视觉设计相关工作必须使用 `ui-ux-pro-max` skill；涉及动效、滚动动画、时间线、GSAP 代码或动画性能时，还必须使用对应 GSAP skill。
- Bug 定位应使用系统化调试流程；不要在没有复现、证据或调用链依据时直接猜测原因。
- 实现新功能或修复 bug 时，优先采用测试驱动或至少先明确可验证的验收点，再写实现。
- 如果多个 skill 都适用，先使用流程类 skill，再使用具体技术或领域 skill。

## 代码实现规范

- 优先遵循仓库已有架构、命名、目录组织和 Compose 写法，不为单次需求引入不必要的新抽象。
- 修改范围应尽量小，只触碰完成任务必需的文件；不得顺手重构无关代码。
- 保持 Kotlin 代码清晰、类型明确、空值处理严谨；避免吞掉异常或用宽泛 fallback 掩盖真实错误。
- Compose UI 应保持状态单向流动，避免把业务逻辑塞进可组合函数；复杂状态优先放入 ViewModel 或已有状态容器。
- 数据层修改应尊重 Room、Repository、ViewModel 的既有边界；涉及 schema 变化时必须说明迁移影响。
- 网络和解析逻辑应有超时、错误状态和用户可理解的失败反馈，避免 UI 无限加载。
- 不把密钥、账号、token、个人路径或本机敏感配置写入仓库。
- 不编辑或提交构建产物、缓存文件和 IDE 临时文件，除非用户明确要求；例如 `.gradle/`、`app/build/`、APK、class、dex 等。

## UI 与体验规范

- UI 改动优先保持现有产品风格一致，避免突兀的新配色、新组件体系或过度装饰。
- Compose 页面应关注真实使用流程：加载、空状态、错误状态、成功状态、返回/取消路径都要考虑。
- 移动端布局必须适配不同屏幕尺寸，避免文字截断、元素重叠、触控区域过小。
- 图标、按钮、输入框、列表、卡片等组件应使用项目已有 Material/Compose 体系；新增样式要有明确理由。
- 动效应服务于反馈和转场，不应影响性能、可读性或可访问性。

## 验证与测试规范

- AI 助手负责代码改动、代码构建、冲突检查和逻辑检查。**当设备已通过 adb 连接且用户已授权时，AI 可以自行完成「构建 → 装机 → 启动 → 截图 → 读图分析」的完整闭环**（见下节），不必每步都交回用户。
- 完成代码改动后必须进行与职责匹配的检查，并在回复中说明实际执行过的命令。
- 常用构建/检查命令：
  - `.\gradlew.bat assembleDebug`
  - `.\gradlew.bat test`（仅在用户要求或任务明确需要本地单元测试时执行）
- 若验证命令失败，必须说明失败原因、相关日志摘要和下一步建议；不能声称任务完成。
- 若设备未连接、adb 断连或需要交互式操作（如系统相册选图、权限弹窗），再交回用户，并给出清晰的验收路径。
- 修复 bug 时应尽量补充或更新测试；若当前项目没有合适测试框架，也要说明手动验收路径。

## Review、Commit 与 PR 规范

- Code review 输出应优先列出问题、风险、影响范围和可复现证据，再给摘要。
- 准备 commit comment 时应包含：变更范围、关键文件、验证命令、已知风险、建议 commit message。
- 准备 PR comment 时应包含：背景、实现要点、测试结果、截图/录屏需求、回归风险、待用户确认事项。
- 未经用户明确授权，不创建远端 PR、不 push、不修改远端状态。
- 对 main 派生分支的提交，commit 前必须完成代码冲突检查和逻辑检查；当前本地可移动端跑通时，检查无误且用户明确要求后可直接本地 commit。
- 如果用户要求本地 commit，commit 前必须确认只暂存本次任务相关文件，避免混入用户已有改动或生成文件。

## 协作与安全边界

- 始终尊重用户已有改动；遇到未提交或不属于本次任务的变更，只读取必要上下文，不回滚、不格式化、不重写。
- 若发现当前分支、工作区状态或依赖环境会影响任务，应先说明并给出最小风险路径。
- 不擅自安装依赖、升级 Gradle/Android/Kotlin 版本或修改构建配置；确需修改时先说明原因和影响。
- 不生成大体积文件、二进制文件或构建产物进入版本控制。
- 交付时保持简洁：说明完成内容、修改文件、验证结果和仍需用户处理的事项。

## 真机调试方法（构建 → 装机 → 截图 → 分析）

本仓库的 UI/性能问题优先用"真机闭环"定位，而不是靠读代码猜。下列命令已在本机验证可用。

### 环境

| 项 | 值 |
|---|---|
| 设备 | `10CG5J0Q0F004E7`（vivo V2520A），Android 16 / API 36，1080×2376 px，density 3.0 |
| adb | `E:\Androidstdio_SDK\platform-tools\adb.exe` |
| Gradle | `.\gradlew.bat`（wrapper 9.6.0，可用；分发包走腾讯镜像） |
| 包名 | `com.example.courseapp` |

### 标准闭环

```powershell
$adb = 'E:\Androidstdio_SDK\platform-tools\adb.exe'

.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest  # 1. 构建 + 单测
& $adb push 'E:\WEAKDOWN\app\build\outputs\apk\debug\app-debug.apk' /data/local/tmp/courseapp.apk
& $adb shell pm install -r /data/local/tmp/courseapp.apk  # 2. 装机（绕过 vivo 外部来源弹窗）
& $adb shell am force-stop com.example.courseapp          # 3. 冷启动，保证跑的是新代码
& $adb shell am start -n com.example.courseapp/.MainActivity
Start-Sleep -Seconds 7                                   # 4. 等启动动画结束
& $adb exec-out screencap -p > "$env:TEMP\shot.png"      # 5. 截图
```

然后直接用读图工具分析 `$env:TEMP\shot.png`。**截图必须真的看**，不能凭代码推断"应该是对的"。

### 交互与定位

```powershell
& $adb shell input tap 540 2200        # 点击（坐标按 1080×2376 真机像素，不是预览图坐标）
& $adb shell input swipe 540 1700 540 800 260   # 滑动（起点/终点/时长 ms）
& $adb shell uiautomator dump /sdcard/ui.xml      # 拿控件树
& $adb shell cat /sdcard/ui.xml > "$env:TEMP\ui.xml"
```

- 控件坐标从 uiautomator dump 里读 `bounds`，**不要**用截图预览的比例换算，容易点到别的地方
- 只验证视觉时优先截图；需要确认某个元素是否存在时才 dump

### 性能数据

```powershell
& $adb shell dumpsys gfxinfo com.example.courseapp reset | Out-Null
# ……执行待测操作（切页 / 滚动 / 开关抽屉）……
& $adb shell dumpsys gfxinfo com.example.courseapp | Select-String 'Janky frames|50th|90th|99th|Slow UI thread'
```

判断标准：`p90 ≤ 16ms` 且 `janky < 5%` 视为流畅；单帧 150ms 以上一定能在手感上感知，需要定位。

### 应用内部状态取证

需要在设备上看 App 自己生成的文件（如捕获纹理、导出的 JSON）时：

```powershell
& $adb shell run-as com.example.courseapp ls -la files
& $adb exec-out run-as com.example.courseapp cat files/xxx.png > "$env:TEMP\xxx.png"
```

配合代码里临时写的 dump（例如把 Compose 的 `GraphicsLayer` 落盘成 PNG），可以直接看到"程序眼里的纹理"，比截屏更能定位采样/坐标问题。

### 日志

```powershell
& $adb logcat -c
& $adb shell am start -n com.example.courseapp/.MainActivity
& $adb logcat -d -b crash -t 40 | Select-String 'FATAL|Caused by'
& $adb logcat -d | Select-String '<自己的 TAG>'      # 例如 EdgeBlur / GlassBackdrop
```

### 调试用实验的纪律

1. **一次只验证一个假设**，每次改动只回答一个是非题（红块铺满了吗？纹理是多大？）
2. **临时代码不提交**：验证完 `git checkout -- <file>` 回退，保持 `main` 始终可运行
3. **回退后必须重新构建 + 装机**，确认用户手上跑的是稳定版
4. 需要长期保留的探针（如 `debugSample` 开关）放在未被引用的 WIP 文件里，或加 `BuildConfig.DEBUG` 守卫
5. 设计实验时优先选**能被单张截图证伪**的方案（纯色探针、原样输出、channel 可视化），而不是靠调参数观察差异

### 安全边界（踩过的坑）

- 在「我的」页做自动化点击时，**避开"清除背景""清空数据"这类破坏性行**，本会话曾误触清除背景，删掉了用户的课表壁纸
- 自动滑动前确认起点落在可滚动区域，避免误触发开关控件（曾把"辅助线"开关滑开）
- 用户正在使用手机（前台是相册/视频/其他 App）时不要抢焦点，等回到桌面再操作
- 所有删除/移动命令执行前，先确认解析出的绝对路径就是目标路径

## 排障手册入口

- 每次会话读取本文件后，也必须阅读仓库根目录的 `AGENT-TROUBLESHOOT.md`，尤其是在构建、Gradle wrapper、测试命令、编码、文件删除、临时产物或工作区状态异常时。
- 遇到 `.\gradlew.bat` 启动失败、`org.gradle.wrapper.GradleWrapperMain` 找不到、`endlocal was unexpected at this time`、wrapper jar 入口异常等问题时，不要反复猜测或直接修改业务代码；先按 `AGENT-TROUBLESHOOT.md` 中的 Gradle Wrapper 排障线路处理。
- 通用 bug 处理顺序：先读完整错误输出，再确认是否可复现，然后检查最近改动和工作区状态，接着定位最小根因，最后才写修复。修复后必须跑与改动匹配的验证命令，并把真实命令和结果写入回复。
- 修改文件时只触碰本次任务所需文件；开始前检查 `git status --short --branch`，区分用户已有改动和自己新增改动。不要回滚、格式化或重写无关文件。
- 任何临时文件、解压目录、构建产物或缓存文件都不能留进版本控制；如果排障过程中生成了临时目录，结束前必须清理或确认其未被 Git 跟踪。
## 片段化课表 HTML 的解析提醒

- 当导入源是从网页里截出来的 `data.html`、`data.txt` 之类片段时，先判断它是不是只剩 `tbody`、`tr` 或 `td` 的局部结构。
- 这类输入不要直接当完整页面喂给解析器，优先补成完整 `table` 结构再走既有表格解析逻辑。
- 如果解析结果里课程名、地点有了但教师为空，先检查输入结构是否被裁剪，再检查 `教师` / `任课教师` 标签是否缺少冒号。
- 遇到这类问题时，先读 `AGENT-TROUBLESHOOT.md` 里对应的“截取的课表片段解析失败”条目，再动业务代码。
