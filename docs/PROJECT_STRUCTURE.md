# CourseApp 当前代码结构说明

本文档只说明当前仓库中的代码文件职责、包结构和主要依赖关系，不承担 README 的安装、运行或产品介绍职责。

## 总体结构

CourseApp 目前是单模块 Android 应用：

```text
E:\WEAKDOWN
├─ build.gradle.kts                 # 根 Gradle 插件版本声明
├─ settings.gradle.kts              # 项目名、仓库源与 :app 模块声明
├─ gradle.properties                # Gradle/Android 构建属性
├─ gradlew.bat                      # Windows Gradle Wrapper 入口
├─ app/
│  ├─ build.gradle.kts              # :app Android/Kotlin/Compose/Hilt/Room 依赖与编译配置
│  ├─ proguard-rules.pro            # Release 混淆规则入口
│  └─ src/main/
│     ├─ AndroidManifest.xml        # 权限、Application、Activity 注册
│     ├─ java/com/example/courseapp # Kotlin 源码
│     └─ res/                       # XML 布局、主题、字符串、启动图标资源
└─ design/design.html               # 独立设计预览稿，不参与 Android 构建
```

源码整体接近单模块 MVVM + Repository 架构：`Activity/Application` 提供 Android 入口，`ui` 负责 Compose 页面，`viewmodel` 管理页面状态和业务动作，`data` 封装 Room 实体、DAO、Repository 与导入解析，`di` 通过 Hilt 提供数据库和网络对象。

## 主要运行链路

```text
CourseApp(Application)
  -> MainActivity
     -> CourseAppTheme
        -> ui.navigation.CourseAppScaffold
           -> ScheduleScreen / ManageScreen / ProfileScreen / WebImportScreen / ImportPreviewScreen
              -> ScheduleViewModel / ManageViewModel / ProfileViewModel / ImportViewModel
                 -> CourseRepository
                    -> CourseDao / SemesterDao / TimeSlotTemplateDao
                       -> AppDatabase(Room)
```

教务网页导入链路相对独立：

```text
WebImportScreen
  -> 启动 WebImportActivity 或接收 HTML
  -> ImportViewModel.parseHtml()
  -> data.importer.CourseHtmlParser
  -> CourseRepository.insertCourse()
```

## 根目录与构建文件

| 文件 | 作用 |
| --- | --- |
| `settings.gradle.kts` | 配置插件仓库、依赖仓库、根项目名 `CourseApp`，并声明唯一模块 `:app`。 |
| `build.gradle.kts` | 根工程插件版本集中声明，包括 Android Gradle Plugin、Kotlin、KSP、Hilt。 |
| `gradle.properties` | Gradle 与 Android 构建属性。 |
| `gradlew.bat` | Windows 下优先使用的 Gradle Wrapper。 |
| `skills-lock.json` | Codex/技能相关锁定文件，与 App 运行逻辑无关。 |
| `AGENTS.md` | AI 协作规范，不参与 Android 编译。 |
| `README.md` | 项目介绍与使用说明，和本文档职责不同。 |
| `design/design.html` | 设计稿/预览文件，不被 `:app` 模块引用。 |

## app 模块配置

| 文件 | 作用 |
| --- | --- |
| `app/build.gradle.kts` | Android 应用模块配置。启用 Compose、KSP、Hilt，配置 SDK 版本、Java/Kotlin 21、Room、Retrofit/OkHttp、Jsoup、Navigation Compose、SplashScreen 等依赖。 |
| `app/proguard-rules.pro` | Release 构建混淆规则扩展点。当前 release 未开启 minify。 |
| `app/src/main/AndroidManifest.xml` | 声明网络权限；注册 `.CourseApp`、`.MainActivity` 和 `.ui.import.WebImportActivity`；配置启动页主题、硬件加速、明文流量等。 |

## 应用入口

| 文件 | 作用 |
| --- | --- |
| `CourseApp.kt` | `@HiltAndroidApp` Application。初始化 Hilt，设置 WebView 数据目录后缀，并注册全局未捕获异常日志。 |
| `MainActivity.kt` | `@AndroidEntryPoint` Activity，只负责安装 SplashScreen、启用 edge-to-edge、挂载 Compose 根主题与 `CourseAppScaffold`。导航与页面壳已从此文件拆出。 |

## 依赖注入

| 文件 | 作用 |
| --- | --- |
| `di/AppModule.kt` | Hilt 单例模块。提供 Room `AppDatabase`、三个 DAO、`OkHttpClient`、`Retrofit`。数据库名为 `course_db`，注册 1->2、2->3、3->4 迁移，同时保留 `fallbackToDestructiveMigration()`。 |

## data 数据层

### data/model

| 文件 | 作用 |
| --- | --- |
| `Course.kt` | Room `courses` 表实体。描述课程名称、教师、地点、星期、节次、课程类型、周次、学期、学分、备注、考试时间、自定义颜色等字段；同时定义 `CourseType` 枚举。 |
| `Semester.kt` | Room `semesters` 表实体。描述学期 id、名称、开始日期、总周数、激活状态、背景图路径和遮罩透明度。 |
| `TimeSlotTemplate.kt` | Room `time_slot_templates` 表实体。保存作息模板名称、节次 JSON 和激活状态。 |

### data/db

| 文件 | 作用 |
| --- | --- |
| `AppDatabase.kt` | Room 数据库定义，聚合 `Course`、`Semester`、`TimeSlotTemplate` 三类实体，版本号为 4，并内置三段迁移 SQL。 |
| `Converters.kt` | Room 类型转换器，目前处理 `List<String>` 与逗号分隔字符串之间的转换。 |
| `CourseDao.kt` | 课程表 DAO。提供按学期、星期、id 查询，插入、更新、删除、批量删除、冲突查询等操作。 |
| `SemesterDao.kt` | 学期 DAO。提供全部学期、当前激活学期、插入更新删除、取消全部激活、激活指定学期等操作。 |
| `TimeSlotTemplateDao.kt` | 作息模板 DAO。提供全部模板、当前激活模板、插入更新删除、取消全部激活、激活指定模板等操作。 |

### data/repository

| 文件 | 作用 |
| --- | --- |
| `CourseRepository.kt` | 应用当前唯一 Repository。把 DAO 操作整理成 ViewModel 可调用的课程、学期、作息模板 API，并封装“切换激活学期/作息模板”这类组合操作。 |

### data/importer

| 文件 | 作用 |
| --- | --- |
| `CourseHtmlParser.kt` | 主要教务 HTML 解析器。基于 Jsoup，包含表格解析、div 解析、JSON 片段解析、周次/节次/教师地点等字段归一化，并可转换为 `Course` 实体。文件较大，是后续可继续拆分的重点。 |

## network 网络层

| 文件 | 作用 |
| --- | --- |
| `ApiService.kt` | Retrofit 接口占位。提供任意 URL 页面获取和 `schedule` 接口获取方法。当前主导入流程没有直接依赖它。 |
| `HtmlParser.kt` | 较早期/轻量级 HTML 解析器。可从简单表格或课程列表元素中生成 `Course`，功能少于 `data.importer.CourseHtmlParser`。当前更像兼容或备用代码。 |

## viewmodel 状态层

| 文件 | 作用 |
| --- | --- |
| `ScheduleViewModel.kt` | 首页课表状态中心。管理课程流、当前周、深色模式、Snackbar、FAB、选中课程、作息模板、当前学期、拖拽移动、背景图；包含周次判断、课程进行中/即将开始判断、课程移动/复制/删除等逻辑。 |
| `ManageViewModel.kt` | 课程管理页状态。维护添加课程表单、列表批量选择、保存/删除/复制课程、Snackbar。 |
| `ImportViewModel.kt` | 导入预览状态。调用 `CourseHtmlParser` 解析 HTML，维护解析出的课程、冲突列表、选中项、导入完成状态，并执行冲突替换或整学期替换。 |
| `ProfileViewModel.kt` | 我的页状态。管理用户展示信息、课程/学期流、通知开关、背景图复制与清理、遮罩透明度、JSON 导入导出等。 |

## ui 界面层

### ui/navigation

| 文件 | 作用 |
| --- | --- |
| `AppDestination.kt` | 定义应用内部页面目的地 `AppDestination`，以及底栏配置模型 `BottomNavItem`。 |
| `CourseAppScaffold.kt` | Compose 应用主壳。持有当前目的地、周选择弹层、作息设置弹层等导航级 UI 状态，并按目的地挂载各页面。 |
| `AppNavigationBars.kt` | 主壳的顶栏和底栏 UI。包含学期标题/周选择入口、深色模式按钮动画、底部导航项渲染。 |

### ui/schedule

| 文件 | 作用 |
| --- | --- |
| `ScheduleScreen.kt` | 首页课表页面。绘制课表网格、背景图遮罩、悬浮操作菜单、课程详情、课程长按菜单、移动冲突对话框，并消费 `ScheduleViewModel` 状态。 |

### ui/manage

| 文件 | 作用 |
| --- | --- |
| `ManageScreen.kt` | 课程管理页面。包含课程列表、添加课程表单、批量选择/删除、课程类型与颜色等 UI。文件较大，后续可按列表、表单、批量工具栏拆分。 |

### ui/profile

| 文件 | 作用 |
| --- | --- |
| `ProfileScreen.kt` | 我的页。包含用户信息、设置项、学期管理、背景设置、数据导入导出等 UI。文件较大，后续可按设置卡片、学期管理、背景管理拆分。 |

### ui/import

| 文件 | 作用 |
| --- | --- |
| `WebImportScreen.kt` | Compose 导入入口页。维护学校列表/输入网址等导入交互，并负责进入 WebView 导入流程。 |
| `WebImportActivity.kt` | XML + WebView Activity。加载教务系统网页，支持桌面/手机版 UA 切换，提取当前页面 HTML 并通过 Activity result 返回。 |
| `ImportPreviewScreen.kt` | 导入预览页。展示解析课程、冲突信息、选择/取消选择、替换冲突或整学期替换等操作。 |

### ui/settings

| 文件 | 作用 |
| --- | --- |
| `TimeSlotSettingsScreen.kt` | 作息时间设置页。编辑每节课开始/结束时间，保存为作息模板，并和 `ScheduleViewModel` 交互。 |

### ui/components

| 文件 | 作用 |
| --- | --- |
| `AppSnackbar.kt` | 应用内自定义 Snackbar。 |
| `ColorPickerRow.kt` | 课程颜色选择行。 |
| `CourseCard.kt` | 课表课程卡片与课程颜色/渐变计算。 |
| `CourseContextMenu.kt` | 课程长按上下文菜单。 |
| `CourseDetailSheet.kt` | 课程详情/编辑底部弹层。 |
| `CourseMoveSheet.kt` | 课程移动目标选择底部弹层。 |
| `WeekSelectorDialog.kt` | 当前周选择弹层。 |

### ui/theme

| 文件 | 作用 |
| --- | --- |
| `Color.kt` | 应用颜色常量，包括主色、背景、玻璃态、课程卡片颜色和状态颜色。 |
| `Theme.kt` | Compose `CourseAppTheme`，根据深色模式选择 Material3 color scheme。 |
| `Type.kt` | Compose typography 设置。 |

## res 资源文件

| 文件 | 作用 |
| --- | --- |
| `res/values/strings.xml` | 应用名称 `课程表`。 |
| `res/values/themes.xml` | Android 传统主题与 SplashScreen 主题。Compose 主题定义在 Kotlin 的 `ui/theme` 中。 |
| `res/layout/activity_web_import.xml` | `WebImportActivity` 使用的 XML 布局，包含全屏 WebView 和底部两个 Button。 |
| `res/drawable/ic_launcher_foreground.xml` | 启动图标前景矢量资源。 |
| `res/values/ic_launcher_background.xml` | 启动图标背景色资源。 |
| `res/mipmap-anydpi-v26/ic_launcher.xml` | 自适应启动图标配置。 |
| `res/mipmap-anydpi-v26/ic_launcher_round.xml` | 圆形自适应启动图标配置。 |

## 当前结构评估

已经具备的良好边界：

- 数据持久化集中在 `data/db` 和 `data/model`。
- 页面状态基本由 ViewModel 承载，Compose 页面主要消费状态并触发事件。
- Repository 已经隔离 DAO，页面层没有直接访问 Room。
- Hilt 负责数据库、DAO、网络对象创建。
- 入口导航已从 `MainActivity` 拆到 `ui/navigation`，Activity 职责更接近标准 Android 入口。

仍需后续优化的点：

- `CourseHtmlParser.kt`、`ProfileScreen.kt`、`ManageScreen.kt`、`ScheduleScreen.kt` 文件体积较大，后续可以按解析策略、页面区块或表单组件继续拆分。
- `network/HtmlParser.kt` 与 `data/importer/CourseHtmlParser.kt` 职责重复，需要确认旧解析器是否仍有实际调用价值。
- `ScheduleViewModel` 同时包含周次计算、课程时间判断、样例数据初始化、UI 消息和课程操作，后续可把纯计算逻辑提取为可单测的 domain/helper。
- `TimeSlot` 当前定义在 `ScheduleViewModel.kt`，但作息设置和课表 UI 都使用它，后续可移动到 `data/model` 或独立 domain model。
- Room 数据库当前同时配置了明确迁移和 `fallbackToDestructiveMigration()`，若进入正式版本，应重新评估破坏性迁移风险。
