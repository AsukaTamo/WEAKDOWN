# 液态玻璃（Liquid Glass）待办清单

> **状态：自研着色器路线已停止，改用 Vitro。**
> 现状见 [VITRO_LIQUID_GLASS.md](VITRO_LIQUID_GLASS.md)：材质换成了 `io.github.itshedimisawi.vitro`
> 的 AGSL 液态玻璃（源码已 vendor 进本仓库），`ui/components/LiquidGlass.kt` 已删除。
>
> 本文档保留下来只作为**历史记录**：记录自研路线已经排除的假设和踩过的坑，避免以后有人再走一遍。
> 下面第二、三、四节是**旧路线**的内容，不再适用。

---

## 一、当前实现（已上线）

| 组件 | 做法 | 位置 |
|---|---|---|
| 顶栏 / 底栏 | `surface` 62%~74% 磨砂渐变 + 顶部白色高光渐变 + 0.9dp `sweepGradient` 彩虹描边 | `MainActivity.kt` 的 `topBar` / `bottomBar` |
| 主题适配 | 描边与高光强度按深浅色分别取值，深色压暗避免白线 | 同上 |
| 回退 | 无壁纸 / API < 33 时仍是这一套（不依赖着色器，永不失败） | — |

视觉上已经具备：磨砂感、边缘彩虹色散、顶部高光。**缺**：真实的背景折射/放大、边缘压缩、内回声。

---

## 二、已经排除的假设（别再重复试）

| 假设 | 结论 | 证据 |
|---|---|---|
| 录制层缺壁纸导致白块 | ✅ 成立并已修 | 把壁纸搬进录制层后，dump 纹理是「街景 + 课表」 |
| `BitmapShader` 矩阵平移方向错 | ❌ 不成立 | 三种约定（`-origin` / `+origin` / 单位矩阵）结果完全相同 |
| 挂在错误的节点上 | ❌ 不成立 | 红色探针 `drawBehind { drawRect(Color.Red) }` 完整覆盖整条顶栏 |
| AGSL `uniform shader` 输入可用 | ❌ **失效** | 调试通道只输出采样原图，得到的是黑块，连「原样输出一张图」都做不到 |
| `drawImage(dstOffset = -origin, 整屏尺寸)` 可用 | ❌ 失效 | 只有左上角一小块可见（约 205×50 px） |
| `clipRect + translate + drawImage` 可用 | ❌ 同样失效 | 与上一条现象完全一致 |
| 全限定名调用 DrawScope 扩展函数 | ⚠️ 编译期错误 | `androidx.compose.ui.graphics.drawscope.clipRect(...)` 不合法，必须 import 后直接调用 |

**关键观察**：纯色 `drawRect` 能铺满整条栏，但**任何绘制图片的 API 都只显示左上角一小块**。这说明节点与尺寸正确，问题在「图片绘制/遮挡」这一层。

---

## 三、下一步的两个判别实验（各一次构建即可定论）

### 实验 A：确认位图尺寸

```kotlin
android.util.Log.d("GlassSlice", "bitmap=${bitmap.width}x${bitmap.height}")
```

- 若不是 1080×2376 → `createScaledBitmap` 的目标尺寸取自 `displayMetrics` 有问题，改用 `LocalConfiguration.screenWidthDp * density` 计算
- 若是 1080×2376 → 进入实验 B

### 实验 B：确认是否被上层遮挡

1. 把切片画成 `alpha = 0.5f` 的半透明
2. 临时去掉该节点外层 `Box` 的 `.background(...)`
3. 截图看那一小块之外是否出现切片

- 出现 → 被父层/兄弟层盖住，调整绘制顺序（把切片移到 `.background` 之后、或用 `Modifier.drawWithContent`）
- 不出现 → 图片绘制本身被裁剪，改用 `drawImageRect`（注意：是 `DrawScope` 的成员函数，**不要** import `androidx.compose.ui.graphics.drawscope.drawImageRect`，那个路径不存在）

---

## 四、真着色器路线（方案 C，推荐）

放弃 AGSL 的纹理输入，改成「Canvas 采样 + 纯数学光学层」：

1. **采样层（Canvas，已证明可行）**：`drawImage` / `drawImageRect` 把整屏纹理的对应切片画进栏内
2. **折射层**：按 SDF 高度场做 3 次偏移采样（Face / Inner / Outer），用 `saveLayer` + 多次 `drawBitmap` 叠加，实现边缘压缩与放大
3. **光学层（AGSL，无纹理输入）**：只负责色散描边、hairline、echo、GGX 四点高光——这些只依赖坐标与法线，不依赖纹理，因此不会再踩输入的坑

### 苹果配方（来自 [webgl-apple-liquid-glass](https://github.com/Oliverrr2424/webgl-apple-liquid-glass) 的 material 默认值）

| 组 | 参数 | 默认 | 含义 |
|---|---|---:|---|
| 透射 | `refraction` | 84 | 玻璃体弯折强度（0–110） |
| | `edgeReach` | 0.14 | 边缘取样距离 / 短边（0 关闭边缘捕获） |
| | `edgeWidth` | 0.21 | 边缘捕获带宽 / 短半边 |
| | `dispersion` | 2.0 | RGB 分离，**屏幕像素** |
| | `body` / `absorption` | 0.72 / 0.58 | 玻璃体密度 / 路径吸收 |
| 反射 | `rim` | 0.24 | 边缘光 |
| | `reflection` | 0.31 | 边缘里的背景反射 |
| | `highlight` | 0.34 | 镜面高光 |
| | `echo` | 0.28 | 内回声 |
| 界面 | `hairline` / `hairWidth` | 0.92 / 0.52 | 轮廓细线强度 / 宽度 |
| | `roundness` | 0.47 | 圆角半径 / 短半边 |

---

## 五、可选增强（做完真着色器之后再考虑）

- [ ] 用 `Modifier.blur(20.dp)` 把壁纸真正模糊后画进栏内（真磨砂，而不是半透明底）
- [ ] 触摸高光：按下时轻微放大 + 提亮（`interactive` 语义）
- [ ] 滚动边缘柔化：内容滚到栏下时做渐隐（`scrollEdgeEffect`）
- [ ] 课程详情信息栏套用同一材质
- [ ] 相邻玻璃融合（顶栏与周数胶囊）

---

## 六、相关文件

| 文件 | 内容 |
|---|---|
| `app/src/main/java/io/github/itshedimisawi/vitro/` | **当前有效**：vendor 进来的 Vitro（液态玻璃材质），接线说明见 `docs/VITRO_LIQUID_GLASS.md` |
| `ui/components/LiquidGlass.kt` | 已删除（自研着色器路线，见本文档历史记录） |
| `MainActivity.kt` | 顶栏/底栏的玻璃材质（`glassBackdrop` + `glassEffect`）、主题切换揭示动画 |
| `app/build.gradle.kts` | compileSdk 37 / Kotlin 2.4.21 / Compose 1.12.1（BOM 2026.09.00）/ KSP 2.3.12 / Hilt 2.60.1 / Room 2.8.5；Gradle 9.6 / AGP 9.4.1 |

## 七、参考资料

- [Vitro](https://github.com/itshedimisawi/vitro)：Compose + AGSL 的液态玻璃库，架构为「`glassBackdrop` 采集 + `glassEffect` 渲染」，采集在渲染线程之外；要求 `compileSdk ≥ 37` 与 AGP 9.1+，本项目已按这套基线接入
- [webgl-apple-liquid-glass](https://github.com/Oliverrr2424/webgl-apple-liquid-glass)：WebGL2 复刻，材质默认值与边缘捕获算法可直接借鉴
- [LiquidGlassKit](https://github.com/DnV1eX/LiquidGlassKit)：iOS 侧实现，API 语义参考
