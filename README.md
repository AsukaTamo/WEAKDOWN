# CourseApp - Android 课程表应用

一款面向大学生的课程表 Android 应用，支持课表展示、课程管理、教务系统导入、学期管理等核心功能。采用 Jetpack Compose + Material3 构建，界面简洁流畅。

## 功能特性

### 课表展示
- 每周课表网格，支持左右滑动查看完整周视图
- 课程卡片展示课程名、节次、地点、教师、周次范围
- 长按课程卡片弹出操作菜单（编辑/复制/移动/删除）
- 拖拽移动课程，自动检测时间冲突并提供替换/保留选项
- 当前课程高亮、进行中进度条、即将开始倒计时
- 周数选择器，快速切换查看不同周次

### 课程管理
- 手动添加课程：名称、教师、地点、学分、星期、节次、周次、类型、备注
- 课程按名称自动分组，支持展开/折叠查看
- 批量管理模式：全选、批量删除
- 自定义卡片颜色（10 种预设色 + 类型默认色）
- 课程复制功能

### 数据导入
- WebView 内嵌教务系统页面，自动检测课表页面并解析
- Jsoup 多策略 HTML 解析（表格 / div 布局 / JSON 数据）
- 导入预览：勾选课程、检测冲突、冲突解决（替换 / 保留两者）
- JSON 格式课表导出/导入备份

### 学期管理
- 创建多个学期，设置起始日期和总周数
- 快速切换当前学期，课表自动刷新
- 学期删除（非当前学期）

### 作息时间设置
- 自定义每日节次时间段（上课/下课时间）
- 保存为模板，支持多模板切换
- 恢复默认 10 节课时间表

### 界面
- 深色模式切换（顶栏图标旋转动画）
- 磨砂玻璃风格顶栏和底栏导航
- 启动页（Splash Screen）

## 项目结构

```
app/src/main/java/com/example/courseapp/
├── CourseApp.kt                    # @HiltAndroidApp Application
├── MainActivity.kt                 # 单 Activity，Compose 导航宿主
│
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt         # Room 数据库 (v3, 3 张表, 2 次迁移)
│   │   ├── Converters.kt          # CourseType 枚举 TypeConverter
│   │   ├── CourseDao.kt           # 课程 DAO
│   │   ├── SemesterDao.kt         # 学期 DAO
│   │   └── TimeSlotTemplateDao.kt # 时间段模板 DAO
│   ├── importer/
│   │   └── CourseHtmlParser.kt    # Jsoup 多策略 HTML 解析器
│   ├── model/
│   │   ├── Course.kt              # 课程实体 + CourseType 枚举
│   │   ├── Semester.kt            # 学期实体
│   │   └── TimeSlotTemplate.kt    # 时间段模板实体
│   └── repository/
│       └── CourseRepository.kt    # 数据仓库，封装所有 DAO
│
├── di/
│   └── AppModule.kt               # Hilt 依赖注入模块
│
├── network/
│   ├── ApiService.kt              # Retrofit 接口
│   └── HtmlParser.kt              # Jsoup 解析器（旧版）
│
├── ui/
│   ├── components/
│   │   ├── AppSnackbar.kt         # 通用 Snackbar 组件
│   │   ├── ColorPickerRow.kt      # 颜色选择器
│   │   ├── CourseCard.kt          # 课表课程卡片
│   │   ├── CourseContextMenu.kt   # 长按操作菜单（BottomSheet）
│   │   ├── CourseDetailSheet.kt   # 课程详情/编辑面板
│   │   ├── CourseMoveSheet.kt     # 移动课程目标选择面板
│   │   └── WeekSelectorDialog.kt  # 周数选择器
│   ├── import/
│   │   ├── ImportPreviewScreen.kt # 导入预览（勾选+冲突检测）
│   │   └── WebImportScreen.kt     # WebView 教务系统导入
│   ├── manage/
│   │   └── ManageScreen.kt        # 课程管理（全部课程 + 添加课程）
│   ├── profile/
│   │   └── ProfileScreen.kt       # 个人中心（学期/外观/数据/关于）
│   ├── schedule/
│   │   └── ScheduleScreen.kt      # 首页课表网格
│   ├── settings/
│   │   └── TimeSlotSettingsScreen.kt # 作息时间设置
│   └── theme/
│       ├── Color.kt               # 颜色方案（亮/暗 + 课程渐变色）
│       ├── Theme.kt               # Material3 主题
│       └── Type.kt                # 字体排版
│
└── viewmodel/
    ├── ImportViewModel.kt          # 导入状态管理 + 冲突检测
    ├── ManageViewModel.kt          # 课程 CRUD + 批量操作
    ├── ProfileViewModel.kt         # 学期管理 + JSON 导出导入
    └── ScheduleViewModel.kt        # 课表展示 + 拖拽移动 + 时间段模板
```

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin 1.9.22 |
| UI 框架 | Jetpack Compose (BOM 2024.01.00) + Material3 1.1.2 |
| 架构 | 单 Activity + MVVM |
| 依赖注入 | Hilt 2.50 |
| 数据库 | Room 2.6.1 (Flow 响应式查询) |
| 网络 | Retrofit 2.9.0 + OkHttp 4.12.0 |
| HTML 解析 | Jsoup 1.17.2 |
| JSON | Gson |
| 异步 | Kotlin Coroutines 1.7.3 |
| 注解处理 | KSP 1.9.22-1.0.17 |
| 启动页 | SplashScreen API 1.0.1 |
| 最低版本 | Android 8.0 (API 26) |
| 目标版本 | Android 15 (API 36) |

## 数据库设计

### courses 表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK) | 自增主键 |
| name | String | 课程名称 |
| teacher | String | 授课教师 |
| location | String | 上课地点 |
| dayOfWeek | Int | 星期几 (0=周一 .. 6=周日) |
| startSlot | Int | 开始节次 (0-based) |
| slotCount | Int | 持续节数 |
| type | CourseType | 必修/选修/实验/自定义 |
| weekRange | String | 周次范围 (如 "1-18周") |
| semester | String | 所属学期 ID |
| credits | Float | 学分 |
| notes | String | 备注 |
| examDate | String | 考试日期 |
| customColor | String | 自定义卡片颜色 (hex) |

### semesters 表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | String (PK) | 学期 ID (如 "2025-2026-2") |
| name | String | 学期名称 |
| startDate | String | 起始日期 |
| totalWeeks | Int | 总周数 |
| isActive | Boolean | 是否为当前学期 |

### time_slot_templates 表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK) | 自增主键 |
| name | String | 模板名称 |
| slotsJson | String | 时间段 JSON 数组 |
| isActive | Boolean | 是否为当前使用模板 |

## 构建与运行

```bash
# 克隆项目
git clone <repo-url>
cd WEAKDOWN

# 使用 Android Studio 打开项目，或命令行构建
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug
```

**环境要求：**
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 21
- Android SDK 36
