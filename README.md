<div align="center">

<h1>WEAKDOWN</h1>

<p><strong>你的课程管家</strong><br/>让学习更有条理，让生活更轻松</p>

<p>
  <img alt="Open Source Non-Commercial" src="https://img.shields.io/badge/Open%20Source-Non--Commercial-2EA44F?style=flat-square">
  <img alt="Buy Me a Coffee" src="https://img.shields.io/badge/Buy%20Me%20a%20Coffee-%E2%98%95-FFDD00?style=flat-square&logo=buymeacoffee&logoColor=000000">
  <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white">
  <img alt="License" src="https://img.shields.io/badge/License-PolyForm%20Noncommercial%201.0.0-6F42C1?style=flat-square">
</p>

<p>
  <strong>简体中文</strong> · <a href="README_EN.md">English</a>
</p>

<p>
  <a href="#下载与安装">下载安装</a> ·
  <a href="#功能展示">功能展示</a> ·
  <a href="#使用说明">使用说明</a> ·
  <a href="#开源声明">开源声明</a>
</p>

<img src="assets/readme/poster.png" width="320" alt="WEAKDOWN 课程表 App 海报">

</div>

## 下载与安装

<p align="center">
  <a href="https://github.com/AsukaTamo/WEAKDOWN/releases">
    <img src="https://img.shields.io/badge/下载-WEAKDOWN%202.4.1-0F64B5?style=for-the-badge&logo=android&logoColor=white" alt="下载 WEAKDOWN 2.4.1">
  </a>
</p>

| 项目 | 说明 |
| --- | --- |
| 最新版本 | v2.4.1 |
| 系统要求 | Android 8.0（API 26）及以上 |
| 安装包 | `WEAKDOWN-2.4.1.apk`（约 16 MB） |
| 下载 | [GitHub Releases](https://github.com/AsukaTamo/WEAKDOWN/releases) |

### 安装步骤

1. 打开 [Releases](https://github.com/AsukaTamo/WEAKDOWN/releases) 页面，下载最新 APK。
2. 在系统提示时，允许浏览器或文件管理器「安装未知应用」。
3. 点击 APK 完成安装，首次打开即可使用。
4. 从旧版本升级时直接覆盖安装；卸载应用会清除本地数据。

> 所有课程、学期与个人信息都保存在本机。卸载应用或使用「清空数据」后无法恢复，重要课表请自行留存。

## 功能展示

<p align="center">
  <img src="assets/readme/liquid-glass.png" width="900" alt="WEAKDOWN 浅色与深色液态玻璃课表">
</p>

| 课表 | 课程管理 | 个性设置 |
| :---: | :---: | :---: |
| 周次切换 · 今日高亮<br/>进行中课程进度 | 教务导入 · 批量管理<br/>复制 / 移动 / 冲突提示 | 液态玻璃 · 深浅主题<br/>背景图 · 作息模板 |

- **液态玻璃视觉**：浅色「鸢尾蓝 + 流光白」与深色「火山橙 + 藏蓝」双主题，卡片、顶栏、底栏使用统一的玻璃材质。
- **本周一眼看清**：按周切换，今日列高亮；进行中的课程显示进度与剩余时间，即将开始的课程给出提醒。
- **三种添加方式**：手动填写、空白格快捷添加、从学校教务系统抓取页面导入。
- **课程管理**：按课程名折叠分组，支持批量删除、复制到下一天、移动到其他时段，冲突时二次确认。
- **学期与作息**：多学期管理、自定义总周数、多套作息模板，逐节调整上下课时间。
- **个性化**：课表背景图与遮罩、辅助线开关、深浅色切换；个人信息卡支持触摸 3D 倾斜与径向流光。

## 使用说明

### 1. 先设置学期

进入「我的」→「学期」，新建或选择当前学期，填写起始日期和总周数。首页的周次、今日高亮和课程显示都以这里为准。

### 2. 添加课程

- **快捷添加**：在首页空白格长按并拖选节次，弹出快捷添加表单。
- **手动添加**：点底栏「+」→「手动添加」，可填写名称、教师、地点、学分、周次、节次、类型、颜色和备注。
- **教务导入**：点底栏「+」→「从教务系统导入」，从学校教务页面抓取课表。

### 3. 查看与整理

- 点击课程卡片查看详情、教师、地点与时间。
- 长按课程卡片可以编辑、复制、移动到其他时段或删除。
- 点底栏「+」→「本学期课程」可按课程名折叠、批量选择和批量删除。

### 4. 导入教务课表

1. 点底栏「+」→「从教务系统导入」。
2. 在页面内登录学校教务系统，进入本学期课表页。
3. 点击抓取当前页面，确认识别结果后导入。

> 不同学校的教务页面结构差异较大。当前支持表格、div 与 JSON 结构；如果导入失败，欢迎在 [Issues](https://github.com/AsukaTamo/WEAKDOWN/issues) 附上脱敏后的 HTML 片段。

### 5. 个性化

- 顶栏的太阳 / 月亮按钮切换浅色与深色主题。
- 「我的」→「课表背景」可设置学期背景图、遮罩透明度与辅助线。
- 「我的」→「作息时间」可管理多套作息模板，并逐节修改上下课时间。
- 个人信息卡支持跟随触摸位置的 3D 透视倾斜与径向反射流光，松手后弹性回正。

### 6. 数据与隐私

- 课程、学期、作息和个人信息保存在本机 Room 数据库，不需要账号登录。
- 教务导入只在应用内 WebView 中进行，不会把课表上传到第三方服务器。
- 使用「我的」页底部的「清空数据」会删除全部本地数据，操作不可撤销。

## 开源声明

<p align="center">
  <img src="https://img.shields.io/badge/Open%20Source-Non--Commercial-2EA44F?style=for-the-badge" alt="Open Source Non-Commercial">
</p>

WEAKDOWN 的源码公开，供学习、研究与个人使用。项目采用 **[PolyForm Noncommercial License 1.0.0](LICENSE)**：

- **允许**：个人学习、研究、教学、非商业项目使用、修改与分发。
- **禁止**：任何商业用途，包括售卖、付费分发、商业产品集成，以及以盈利为目的的二次发布。
- 分发或修改时，请保留 [LICENSE](LICENSE) 与版权声明。

> 说明：PolyForm Noncommercial 是「源码公开、禁止商用」的许可证，不属于 OSI 定义的开源许可证。README 顶部的 Open Source 徽章用于表达源码公开，不改变许可证的实际条款。

第三方组件（Jetpack Compose、AndroidX、Vitro 等）遵循各自许可证；Vitro 的 Apache 2.0 许可文本见 [LICENSE-vitro.txt](app/src/main/java/io/LICENSE-vitro.txt)。

## 支持 WEAKDOWN

如果 WEAKDOWN 帮到了你，欢迎把它分享给同学，或请作者喝杯咖啡。

<p align="center">
  <img src="https://img.shields.io/badge/Buy%20Me%20a%20Coffee-%E2%98%95%20Support-FFDD00?style=for-the-badge&logo=buymeacoffee&logoColor=000000" alt="Buy Me a Coffee">
</p>

> Buy Me a Coffee 支持入口准备中；在那之前，分享项目或提交反馈就是最好的支持。

遇到问题或有功能建议，欢迎提交 [GitHub Issue](https://github.com/AsukaTamo/WEAKDOWN/issues)。

<div align="center">
  <sub>WEAKDOWN · 让学习更有条理，让生活更轻松</sub>
</div>
