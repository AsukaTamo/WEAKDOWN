# CourseApp - Android 课程表应用

一款面向大学生的课程表 Android 应用：课表展示、课程管理、教务系统导入、学期与作息管理。采用 Jetpack Compose + Material3 构建。

- 应用包名：`com.example.courseapp`
- 当前版本：`2.4.1`（versionCode 1）
- 最低版本：Android 8.0（API 26）；目标/编译版本：API 36

## 功能

### 课表首页

- 按周横向翻页，左侧时间轴 + 7 天日期表头；「今日列」只在当前查看的周高亮
- 课程卡片展示课程名、教师、地点、节次，颜色按课程名/类型/自定义色计算
- **进行中课程**：旋转的锥形渐变描边 + 弥散光晕 + 底部呼吸背光，卡片底部显示进度条与剩余分钟
- **即将开始**（60 分钟内）的课程显示「X 分后」
- 周选择：点「第 X 周」展开/收起面板，面板内本周用描边标出，非本周时顶栏给出「回本周」入口
- 长按课程卡片：编辑 / 复制 / 移动到其他时段（冲突时提示替换）/ 删除
- 点击课程卡片：两档锚点的详情抽屉（拖动有阻尼橡皮筋、松手按速度吸附、下拉关闭），可切换到编辑表单
- FAB 菜单：教务系统导入、手动添加课程
- 自定义课表背景图与遮罩透明度、课表辅助线开关

### 课程管理

- 只展示**当前学期**的课程，按课程名分组折叠/展开，显示每周课时数
- 批量管理：全选、批量删除（二次确认）
- 新增课程：名称、教师、地点、学分、周次范围、星期、节次、节数、类型、自定义颜色、备注
- 单个课程删除（二次确认）、复制到下一天
- 「所属学期已删除」的孤儿课程检测与一键清理

### 数据导入

- 内嵌 WebView 打开学校教务系统，支持桌面/手机 UA 切换，抓取当前页面 HTML
- Jsoup 多策略解析：表格布局（含 rowspan/colspan 重建）→ div 布局 → JSON 片段；兼容被截断成 `tbody`/`tr` 的片段 HTML
- 导入预览：列出解析结果，整学期覆盖前二次确认

### 学期与作息

- 学期：新增、编辑（名称/起始日期/总周数）、切换当前学期、删除（连带删除该学期课程并二次确认提示门数）
- 作息：多套作息表保存、切换、删除；逐节编辑上下课时间（时间选择器 / 快捷时长）；恢复默认 12 节

### 其他

- 深色/浅色模式切换，顶栏太阳/月亮形变动画
- 全应用统一的青绿玻璃视觉体系
- 示例数据只在首次安装播种一次，清空数据后不会复活

## 尚未实现 / 计划中

| 项目 | 现状 |
| --- | --- |
| PDF 课表导入 | FAB 菜单中有入口，点击仅提示，未实现解析 |
| 上课提醒 | 「我的」页开关仅界面状态，没有通知实现 |
| JSON 备份导出/导入 | `ProfileViewModel` 已有读写逻辑，但没有任何 UI 入口 |
| 导入逐条勾选 / 冲突合并 | 目前只能整学期覆盖导入 |

## 项目结构

```text
app/src/main/java/com/example/courseapp/
├── CourseApp.kt                    # @HiltAndroidApp Application
├── MainActivity.kt                 # Activity 入口 + 唯一的应用壳（顶栏/底栏/页面切换/抽屉）
├── data/
│   ├── db/                         # Room 数据库 v5、DAO、TypeConverter
│   ├── importer/CourseHtmlParser.kt# 教务 HTML 解析（Jsoup 三策略）
│   ├── model/                      # Course / Semester / TimeSlotTemplate
│   └── repository/                 # CourseRepository（含事务组合操作）
├── di/AppModule.kt                 # Hilt：数据库与 DAO
├── viewmodel/                      # Schedule / Manage / Import / Profile + 纯计算工具
└── ui/
    ├── components/                 # 课程卡片、详情抽屉、周选择、颜色选择等
    ├── import/                     # 导入入口、WebView Activity、导入预览
    ├── manage/ profile/ schedule/  # 三个主页面
    ├── settings/                   # 作息时间设置
    ├── splash/                     # 启动动画
    └── theme/                      # 颜色、尺寸/动效令牌、Material3 主题
```

更细的文件职责与依赖关系见 [docs/PROJECT_STRUCTURE.md](docs/PROJECT_STRUCTURE.md)。

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin 1.9.22 |
| UI | Jetpack Compose（BOM 2024.01.00）+ Material3 |
| 架构 | 单 Activity + MVVM + Repository |
| 依赖注入 | Hilt 2.50 |
| 数据库 | Room 2.6.1（Flow 响应式查询） |
| 网页解析 | Jsoup 1.17.2 |
| JSON | Gson 2.10.1 |
| 异步 | Kotlin Coroutines 1.7.3 |
| 注解处理 | KSP 1.9.22-1.0.17 |
| 构建 | AGP 8.2.2 / Gradle 8.7 / JDK 21 |
| 启动页 | SplashScreen API 1.0.1 |
| 测试 | JUnit 4.13.2（JVM 单元测试） |

> 早期版本依赖的 Retrofit / OkHttp 已随未使用的网络层一并移除。

## 数据库设计

Room `course_db`，版本 **5**，包含四段迁移（1→2→3→4→5）。

### courses

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK) | 自增主键 |
| name | String | 课程名称 |
| teacher | String | 授课教师 |
| location | String | 上课地点 |
| dayOfWeek | Int | 星期（0=周一 .. 6=周日） |
| startSlot | Int | 开始节次（0 基） |
| slotCount | Int | 持续节数 |
| type | CourseType | 必修 / 选修 / 实验 / 自定义 |
| weekRange | String | 周次范围，如 `1-5,7-9周`、`1-5(单),7-9周` |
| semester | String | 所属学期 id |
| credits | Float | 学分 |
| notes | String | 备注 |
| examDate | String | 预留（当前无读写） |
| customColor | String | 自定义卡片颜色（hex） |
| colorIndex | Int | 预留（当前无读写） |

### semesters

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String (PK) | 学期 id（如 `2025-2026-2`） |
| name | String | 学期名称 |
| startDate | String | 起始日期（`yyyy-MM-dd`） |
| totalWeeks | Int | 总周数 |
| isActive | Boolean | 是否为当前学期 |
| backgroundUri | String | 课表背景图路径 |
| scrimAlpha | Float | 背景遮罩不透明度 |

### time_slot_templates

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK) | 自增主键 |
| name | String | 作息表名称 |
| slotsJson | String | 节次时间段 JSON 数组 |
| isActive | Boolean | 是否为当前使用模板（同一时刻只会有一条激活） |

## 构建与运行

```bash
# 使用 Android Studio 打开项目后直接 Run，或命令行构建
./gradlew assembleDebug
./gradlew installDebug
```

> **本机 Gradle Wrapper 异常**：仓库中的 `gradle-wrapper.jar` 不是标准 wrapper 包，`.\gradlew.bat` 会报 `ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain`。排障与替代命令见 [AGENT-TROUBLESHOOT.md](AGENT-TROUBLESHOOT.md)，本机可用缓存的 Gradle 8.7 分发包执行：
>
> ```powershell
> & 'C:\Users\HP\.gradle\wrapper\dists\gradle-8.7-all\549ctztx5g5jixmk70l9ykwir\gradle-8.7\bin\gradle.bat' :app:assembleDebug
> ```

**环境要求：**

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 21
- Android SDK 36

## 测试

JVM 单元测试共 6 个类 / 25 个用例，覆盖 HTML 解析、课程时间状态、周次换算、作息迁移、学期过滤与导入覆盖策略。

```bash
./gradlew testDebugUnitTest
```

## 已知限制

- 数据库同时配置了明确迁移与 `fallbackToDestructiveMigration()`，若后续漏写迁移会静默清库，正式发布前需重新评估。
- 导入为整学期覆盖，暂不支持逐条勾选或冲突合并。
- 课程管理、我的、导入预览等页面只按当前学期工作，跨学期数据需切换学期后查看。
- `examDate`、`colorIndex` 为预留字段，暂无读写。
