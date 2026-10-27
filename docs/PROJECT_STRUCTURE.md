# CourseApp 当前代码结构说明

本文档只说明当前仓库中的代码文件职责、包结构和主要依赖关系，不承担 README 的安装、运行或产品介绍职责。最后更新：2026-10-26。

## 总体结构

CourseApp 目前是单模块 Android 应用：

```text
E:\WEAKDOWN
├─ build.gradle.kts                 # 根 Gradle 插件版本声明
├─ settings.gradle.kts              # 项目名、仓库源与 :app 模块声明
├─ gradle.properties                # Gradle/Android 构建属性
├─ gradlew.bat                      # Windows Gradle Wrapper 入口（本机 wrapper jar 异常，见 AGENT-TROUBLESHOOT.md）
├─ AGENT.md                         # AI 协作规范
├─ AGENT-TROUBLESHOOT.md            # 可复用的排障线路
├─ README.md                        # 产品介绍与使用说明
├─ docs/
│  ├─ PROJECT_STRUCTURE.md          # 本文档
│  └─ superpowers/plans/            # 历史实现计划
├─ design/design.html               # 独立设计预览稿，不参与 Android 构建
└─ app/
   ├─ build.gradle.kts              # :app 模块依赖与编译配置
   ├─ proguard-rules.pro            # Release 混淆规则入口（当前未开启 minify）
   └─ src/
      ├─ main/
      │  ├─ AndroidManifest.xml
      │  ├─ java/com/example/courseapp
      │  └─ res/
      └─ test/                      # JVM 单元测试
```

源码整体是单模块 MVVM + Repository：`MainActivity` 提供 Android 入口与唯一的应用壳，`ui` 负责 Compose 页面，`viewmodel` 管理页面状态和业务动作，`data` 封装 Room 实体、DAO、Repository 与导入解析，`di` 通过 Hilt 提供数据库对象。

## 主要运行链路

```text
CourseApp(Application)
  -> MainActivity
     -> CourseAppTheme
        -> MainActivity.CourseApp（应用壳：顶栏 / 底栏 / 页面切换 / 周选择 / 作息设置 / 课程详情抽屉）
           -> ScheduleScreen / ManageScreen / ProfileScreen / WebImportScreen / ImportPreviewScreen
              -> ScheduleViewModel / ManageViewModel / ProfileViewModel / ImportViewModel
                 -> CourseRepository
                    -> CourseDao / SemesterDao / TimeSlotTemplateDao
                       -> AppDatabase(Room)
```

教务网页导入链路相对独立：

```text
WebImportScreen
  -> 启动 WebImportActivity（WebView）
  -> 抓取 outerHTML 通过 Activity result 返回
  -> ImportViewModel.parseHtml()
  -> data.importer.CourseHtmlParser
  -> CourseRepository.insertCourses()
```

## 根目录与构建文件

| 文件 | 作用 |
| --- | --- |
| `settings.gradle.kts` | 配置插件仓库、依赖仓库、根项目名 `CourseApp`，并声明唯一模块 `:app`。 |
| `build.gradle.kts` | 根工程插件版本集中声明：Android Gradle Plugin 8.2.2、Kotlin 1.9.22、KSP 1.9.22-1.0.17、Hilt 2.50。 |
| `gradle.properties` | Gradle 与 Android 构建属性。 |
| `gradlew.bat` | Windows 下优先使用的 Gradle Wrapper。本机 `gradle-wrapper.jar` 不是标准 wrapper 包，需按 `AGENT-TROUBLESHOOT.md` 用缓存的 Gradle 8.7 分发包代替。 |
| `AGENT.md` | AI 协作规范，不参与 Android 编译。 |
| `AGENT-TROUBLESHOOT.md` | Gradle wrapper、KSP、片段化 HTML 解析等排障线路。 |
| `README.md` | 产品介绍、技术栈、构建与测试说明。 |
| `design/design.html` | 设计稿/预览文件，不被 `:app` 模块引用。 |
| `skills-lock.json` | 技能相关锁定文件，与 App 运行逻辑无关。 |

## app 模块配置

| 文件 | 作用 |
| --- | --- |
| `app/build.gradle.kts` | 应用模块配置。启用 Compose、KSP、Hilt；`compileSdk = 37（targetSdk 36）`、`minSdk = 26`、Java/Kotlin 21；依赖包含 Compose BOM 2024.01.00、Material3、Navigation Compose、Hilt 2.50、Room 2.6.1、Jsoup 1.17.2、Gson 2.10.1、Coroutines 1.7.3、SplashScreen 1.0.1。 |
| `app/proguard-rules.pro` | Release 混淆规则扩展点，当前未开启 minify。 |
| `app/src/main/AndroidManifest.xml` | 声明网络权限；注册 `.CourseApp`、`.MainActivity`、`.ui.import.WebImportActivity`；配置启动页主题、硬件加速、明文流量等。 |

## 应用入口

| 文件 | 作用 |
| --- | --- |
| `CourseApp.kt` | `@HiltAndroidApp` Application。初始化 Hilt，设置 WebView 数据目录后缀，注册全局未捕获异常日志。 |
| `MainActivity.kt` | `@AndroidEntryPoint` Activity。安装 SplashScreen、启用 edge-to-edge、挂载 Compose 根主题；文件内 `CourseApp()` 是**唯一的应用壳**（顶栏、底栏、页面枚举 `Screen`、周选择弹层、作息设置弹层、课程详情抽屉、根返回键处理）。 |

## 依赖注入

| 文件 | 作用 |
| --- | --- |
| `di/AppModule.kt` | Hilt 单例模块。提供 Room `AppDatabase` 与三个 DAO。数据库名 `course_db`，注册 1->2、2->3、3->4、4->5 四段迁移，同时保留 `fallbackToDestructiveMigration()`。 |

## data 数据层

### data/model

| 文件 | 作用 |
| --- | --- |
| `Course.kt` | Room `courses` 表实体：名称、教师、地点、星期、节次、课程类型、周次范围、学期 id、学分、备注、自定义颜色；同时定义 `CourseType` 枚举。`colorIndex`、`examDate` 为预留字段，当前无读写。 |
| `Semester.kt` | Room `semesters` 表实体：id、名称、开始日期、总周数、激活状态、背景图路径、遮罩透明度。 |
| `TimeSlotTemplate.kt` | Room `time_slot_templates` 表实体：模板名称、节次 JSON、激活状态。 |

### data/db

| 文件 | 作用 |
| --- | --- |
| `AppDatabase.kt` | Room 数据库定义，版本号 **5**，聚合 `Course`、`Semester`、`TimeSlotTemplate` 三类实体，内置 1->2、2->3、3->4、4->5 四段迁移 SQL。 |
| `Converters.kt` | `CourseType` 与字符串之间的 TypeConverter。 |
| `CourseDao.kt` | 课程 DAO：全量查询流、插入/更新/删除、按 id 批量删除、按学期清空、按学期计数、时间冲突查询。 |
| `SemesterDao.kt` | 学期 DAO：全部学期、当前激活学期（同步/异步）、增删改、取消全部激活、激活指定学期。 |
| `TimeSlotTemplateDao.kt` | 作息模板 DAO：全部模板、当前激活模板（流/同步）、增删改、取消全部激活、激活指定模板。 |

### data/repository

| 文件 | 作用 |
| --- | --- |
| `CourseRepository.kt` | 应用唯一 Repository。封装课程、学期、作息模板的读写；提供两个事务型组合操作：`deleteSemesterWithCourses`（删学期同时删其课程）与 `replaceActiveTimeSlotTemplate`（保证同时只有一条激活模板）。 |

### data/importer

| 文件 | 作用 |
| --- | --- |
| `CourseHtmlParser.kt` | 教务 HTML 解析器（约 590 行）。基于 Jsoup，按「表格 → div 布局 → JSON 片段」三种策略依次尝试；包含 rowspan/colspan 网格重建、星期列探测、周次/节次/教师/地点归一化、片段化 HTML 补齐，并可转换为 `Course` 实体。是后续可继续拆分的重点。 |

## viewmodel 状态层

| 文件 | 作用 |
| --- | --- |
| `ScheduleViewModel.kt` | 首页状态中心（约 410 行）。维护全部课程流、当前查看周、日历周、深色模式、Snackbar、FAB、选中课程、作息模板、当前学期、背景图与遮罩；提供课程增删改复制移动、冲突查询、周次范围判断（`isWeekInRange`）、学期周计算（`weekOfSemester`）等。 |
| `CourseTimeStatus.kt` | 纯计算工具 + `ScheduleClock`：时间槽解析、进行中/即将开始判定、进度与剩余分钟。全部参数显式传入，便于单测。 |
| `ManageViewModel.kt` | 课程管理页状态。当前学期课程流、孤儿课程计数与清理、批量选择、添加课程表单、保存/删除/复制、Snackbar。 |
| `ImportViewModel.kt` | 导入预览状态。调用 `CourseHtmlParser` 解析 HTML，维护解析结果与当前学期课程数，执行整学期覆盖导入。 |
| `ProfileViewModel.kt` | 我的页状态。用户展示信息、学期/课程流、通知开关、背景图复制与清理、遮罩透明度、JSON 读写逻辑（暂未接入 UI）。 |
| `ImportOverwritePolicy.kt` | 导入时"直接导入 / 需要确认覆盖 / 忽略"的策略枚举与判定。 |
| `ScheduleCourseFilter.kt` | 按当前学期过滤课程的纯函数。 |

## ui 界面层

> 应用壳（顶栏 / 底栏 / 页面切换 / 周选择弹层 / 作息设置弹层 / 课程详情抽屉）目前直接写在 `MainActivity.kt` 的 `CourseApp()` 里。历史上曾有一份 `ui/navigation/` 的拆出版本，因与 `MainActivity` 内的实现重复且未接入而被删除；后续若要再次拆分，应直接把 `CourseApp()` 迁出，而不是并行维护两套。

### ui/schedule

| 文件 | 作用 |
|---|---|
| `ScheduleScreen.kt` | 首页课表页。周分页横向滑动、日期表头（今日实心强调色块）、时间轴底衬、课程卡片布局、主题化壁纸遮罩、长按菜单、移动弹层与冲突对话框、Snackbar。首页 FAB 已移除。 |

### ui/manage

| 文件 | 作用 |
|---|---|
| `ManageScreen.kt` | 课程管理页。按当前学期展示课程、按名称分组折叠、批量管理与删除确认、孤儿课程清理入口、添加课程表单与颜色选择。 |

### ui/profile

| 文件 | 作用 |
|---|---|
| `ProfileScreen.kt` | 我的页。用户信息、学期管理弹窗（含删除确认）、课表背景与遮罩、辅助线、深色模式、作息入口、清空数据、关于。 |

### ui/import

| 文件 | 作用 |
|---|---|
| `WebImportScreen.kt` | 导入入口页。学校列表选择、说明、跳转 WebView 导入。 |
| `WebImportActivity.kt` | XML + WebView 页面。桌面/手机 UA 切换、`evaluateJavascript` 抓取 `outerHTML`、通过 Activity result 返回。 |
| `ImportPreviewScreen.kt` | 导入预览页。展示解析结果，整学期覆盖前二次确认。 |

### ui/settings

| 文件 | 作用 |
|---|---|
| `TimeSlotSettingsScreen.kt` | 作息时间设置页。多模板列表与切换、每节时间编辑（时间选择器/快捷时长）、另存为新模板、删除模板、恢复默认。 |

### ui/components

| 文件 | 作用 |
|---|---|
| `AppSnackbar.kt` | 应用内 Snackbar。 |
| `ColorPickerRow.kt` | 课程颜色选择行。 |
| `CourseCard.kt` | 课表课程卡片。含课程配色计算、进行中状态（旋转锥形渐变描边、弥散光晕、呼吸背光、进度条、剩余分钟）。 |
| `CourseContextMenu.kt` | 课程长按菜单。 |
| `CourseDetailSheet.kt` | 课程详情/编辑抽屉。自绘两档锚点、拖动阻尼、按速度吸附、编辑表单。 |
| `CourseMoveSheet.kt` | 课程移动目标选择弹层。 |
| `WeekSelectorDialog.kt` | 教学周选择面板。本周描边标记、总周数按学期收敛。 |

### ui/splash

| 文件 | 作用 |
|---|---|
| `WeakdownSplashOverlay.kt` | 应用内启动动画（约 4 秒），叠加在系统 Splash 之上。 |

### 第三方源码（vendor）

| 文件 | 作用 |
|---|---|
| `io/github/itshedimisawi/vitro/**` | **Vitro 0.1.0**（Apache-2.0，commit `3384d04`）源码，顶栏/底栏的液态玻璃材质。接线、编译基线（compileSdk 37）与本地兼容改动见 `docs/VITRO_LIQUID_GLASS.md`。许可证副本 `io/LICENSE-vitro.txt`。 |

### ui/theme

| 文件 | 作用 |
|---|---|
| `AppColors.kt` | **全应用唯一的颜色源**。约 20 个语义角色（canvas / surface / surfaceRaised / hairline / 文字三档 / accent 四档 / success-warning-danger / glass 五档），`LightAppColors` 与 `DarkAppColors` 两套实例：浅色为鸢尾蓝 + 流光白奶油底，深色为火山橙 + 藏蓝近黑底。 |
| `Color.kt` | 课程卡配色：4 种课程类型 × 深浅两套基准色（白字全部过 4.5:1），外加 Snackbar/FAB 用的两个状态色常量。 |
| `ScheduleTokens.kt` | 尺寸/动效令牌；`schedulePalette`（首页，含壁纸玻璃层）与 `contentPalette`（其他页面/弹层）都由 `AppColors` 派生，不再各自写 hex。 |
| `Theme.kt` | Compose `CourseAppTheme`，Material3 配色方案同样由 `AppColors` 派生，保证对话框/开关/菜单跟页面同色系。 |
| `Type.kt` | Compose typography。 |

## res 资源文件

| 文件 | 作用 |
| --- | --- |
| `res/values/strings.xml` | 应用名称。 |
| `res/values/themes.xml` | 传统主题与 SplashScreen 主题。 |
| `res/layout/activity_web_import.xml` | `WebImportActivity` 的 WebView + 两个按钮布局。 |
| `res/drawable/ic_launcher_foreground.xml` | 启动图标前景。 |
| `res/values/ic_launcher_background.xml` | 启动图标背景色。 |
| `res/mipmap-anydpi-v26/ic_launcher*.xml` | 自适应图标配置。 |

## 测试

`app/src/test` 下为纯 JVM 单元测试（`junit:junit:4.13.2`），共 7 个类 30 个用例：

| 测试类 | 覆盖 |
|---|---|
| `CourseHtmlParserTest` | 表格解析、真实 `data.html` 样本 |
| `CourseTimeStatusTest` | 时间槽解析、进行中/即将开始/进度/剩余分钟边界 |
| `ScheduleWeekTest` | 开学日期到周次的换算边界 |
| `ScheduleTimeSlotsTest` | 默认作息与旧版作息迁移 |
| `ScheduleCourseFilterTest` | 按当前学期过滤 |
| `ImportOverwritePolicyTest` | 覆盖导入策略 |
| `ColorContrastTest` | 浅/深两套 token 的对比度（正文 4.5 / 标题 7 / 按钮文字 4.5 / 课程卡白字 4.5 / 选中态不许明显暗于未选中） |

## 当前结构评估

已经具备的良好边界：

- 数据持久化集中在 `data/db` 和 `data/model`，页面层没有直接访问 Room。
- 页面状态基本由 ViewModel 承载，Compose 页面主要消费状态并触发事件。
- Repository 隔离 DAO，并把"删学期连带删课程""切换激活模板"这类组合操作收进事务。
- 时间/周次相关的纯计算已抽到 `CourseTimeStatus` 与 `ScheduleViewModel.weekOfSemester`，有单测覆盖。
- 应用壳唯一：`MainActivity.CourseApp()` 是唯一外壳实现，不会再出现"改到死代码"的问题。
- 未使用的网络层（Retrofit/OkHttp）与旧版 `HtmlParser` 已移除，依赖也同步清理。

仍需后续优化的点：

- `MainActivity.kt` 同时承载 Activity 入口和整个应用壳（约 650 行），后续可把 `CourseApp()` 拆到独立文件。
- `CourseHtmlParser.kt`、`ProfileScreen.kt`、`ManageScreen.kt`、`ScheduleScreen.kt`、`CourseDetailSheet.kt` 体积偏大，可按解析策略、页面区块或表单组件继续拆分。
- `ScheduleViewModel` 仍同时包含课程操作、作息模板、背景图、UI 消息等职责，可继续按"课表数据 / 外观设置 / 作息"拆分。
- `Course.colorIndex`、`Course.examDate` 为预留字段，当前无读写；若确定不做考试/索引色，应在下次 schema 变更时移除。
- Room 同时配置了明确迁移和 `fallbackToDestructiveMigration()`，进入正式版本前应重新评估破坏性迁移风险。
- 导入预览尚不支持逐条勾选与冲突合并，目前只能是整学期覆盖。
