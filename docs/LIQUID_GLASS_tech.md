我正在开发一个安卓端的课程表软件。请为该软件实现苹果液态玻璃（Liquid Glass）风格的导航栏（Navigation Bar）和信息栏（Info Bar）组件。请严格遵循以下光学与着色器渲染规范进行开发（若使用 Android AGSL/RuntimeShader、OpenGL ES 3.0 或 WebGL2/Three.js 渲染管线，请完整映射以下底层数学与渲染逻辑）：
1.  渲染架构与三层拆分 (Background / Glass / Content)
 * 严格三层结构：将液态玻璃组件严格拆分为三层——最底层 Background、中间层 Glass、最上层 Content。画幅基准参考竖屏 1080×1920（需适配实际移动端视口比例）。
 * Background（背景层）：先把课程表背景内容渲染到离屏渲染目标（paintComposeRT），并生成可供 textureLod（或多级 Mipmap）采样的模糊层级。背景必须在玻璃层之前完成渲染，严禁使用简单的 CSS backdrop-filter、普通视图截图或最终 RGB 假扭曲来替代。
 * Glass（玻璃层）：读取 paintComposeRT，通过 SDF 高度场、双界面折射、Face/Inner/Outer 三路光学采样、边缘色散与 GGX BRDF 计算得到最终透明材质。
 * Content（内容层）：导航栏与信息栏的文字、图标和按钮命中区放到最后绘制，不参与折射和模糊计算。合成顺序固定为：Background -> Glass -> Content，确保图标和文字始终清晰且真正可交互。
2.  SDF 塑造轮廓 (Signed Distance Field)
 * 宽高比校正 (Coordinates)：在片元着色器中先做宽高比校正：p = (uv - 0.5) * vec2(aspect, 1.0)。所有圆形、圆角矩形（导航栏/信息栏主体）和后续法线计算都必须使用该坐标，确保视口 resize 后轮廓不会变扁。
 * 距离场公式 (Formula)：
   * 圆形使用：length(p) - radius。
   * 圆角矩形使用：q = abs(p) - halfSize + radius，再计算 min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius。负值表示内部，零值即为轮廓边界。
 * 形状融合 (Combine)：当导航栏或信息栏存在多个形状需要动态融合（如展开动画、悬浮按钮合并）时，使用 polynomial smooth-min，不要从渲染结果反推边界。形变只改变 SDF 输入坐标，若包含边缘噪声必须保持时间连续，不能逐帧随机。
 * 抗锯齿与表面隔离 (Mask)：使用 fwidth(sdf) 建立抗锯齿宽度，并用 smoothstep 得到稳定的 Alpha Mask。同时保留一份低噪声的 glassSurfaceSdf 专门用于计算高度和法线，避免轮廓噪声污染内部表面。
3.  高度场与法线 (Flat Face & Soft Bevel)
 * 高度计算 (Height)：由平滑 SDF 计算 interior = max(-sdf, 0.0)。令倒角宽度 bevel = uNormalTransition * 0.82，t = clamp(interior / bevel, 0.0, 1.0)，高度 height = uGlassThickness * quintic(t)。当 t = 1 后必须保持常量，使组件中心区域完全平坦。
 * 差分采样核 (Kernel)：法线采样步长使用 step = clamp(uNormalTransition * 0.13, 0.003, 0.012)，分别读取上下左右四个方向的高度；严禁直接对带噪 Mask 求导，否则组件中心会出现内框或锐利隆起。
 * 法线合成 (Normal)：gradient = vec2(right - left, up - down) / (2.0 * step)，surfaceNormal = normalize(vec3(-gradient, 1.0))。X 方向导数需包含 aspect 校正，保证竖屏画幅中四边曲率一致。
 * 法线校验 (Validation)：中心区域 normal 必须稳定为 (0, 0, 1)，法线只在边缘倒角（Bevel）内连续弯曲。高光与环境反射仅使用倒角外半段权重，不能让平面镜面瓣形成凸起的内方框。
4.  双界面折射与色散 (Air -> Glass -> Air -> Background)
 * 入射 (Enter)：视线使用 incident = vec3(0.0, 0.0, -1.0)。在前表面以 refract(incident, frontNormal, 1.0 / IOR) 得到玻璃内射线（insideRay）；折射率 IOR 默认取 1.46，并为全反射或零向量提供稳定回退。
 * 内部光程 (Travel)：前表面点为 vec3(correctedUV, height)。使用 glassPath = height / max(-insideRay.z, 0.025) 推进到平坦背面；UV 偏移必须来自真实厚度与实际光程计算，严禁使用简单的 normal.xy * strength 假偏移。
 * 出射与求交 (Exit)：在背面以 normal = vec3(0.0, 0.0, 1.0)、eta = IOR 再次调用 refract，使光线从玻璃回到空气。随后与 z = -uBackgroundDistance 的背景平面求交，并转换回背景采样 UV。
 * 光谱色散 (Spectrum)：分别用 IOR - dispersion、IOR、IOR + dispersion 追踪 R / G / B 三条光线。色差仅应用于 Inner / Outer 边缘采样，Face 中心区域保持稳定，形成真实光路色散而非廉价的红蓝描边。
5.  三路光学采样 (Face / Inner Rim / Outer Rim)
 * Face（中心面）：使用 opticalCenter + (uv - opticalCenter) * 0.975 做轻微放大，再与绿色物理光路（Green Ray）少量混合。Face 采样使用较高 LOD 模糊，但不得加入明显色散或中心高度差。
 * Inner（内缘）：沿平滑高度法线的 edgeDirection 计算 innerUV = greenRayUV - rimDirection * rimDisplacement。Inner 负责主要压缩与拉伸效果，使用较低 LOD 以保持边缘折射清晰。
 * Outer（外缘）：计算 outerUV = uv + rimDirection * rimDisplacement * 1.28，方向与 Inner 相反，并允许读取玻璃边界外的背景像素。Outer 使用更高 LOD，形成柔和翻卷与色彩溢出（Bleed）。
 * 混合权重 (Blend)：由 SDF 倒角带生成 faceWeight、innerWeight、outerWeight，归一化后合成三路颜色。RGB 光路差只加入 Inner / Outer，保留中心清晰度和 Apple 式边缘镜头感。
6.  BRDF 光照层次 (Refract -> Shadow -> Tint -> Light)
 * 严格渲染顺序 (Order)：先输出完整折射背景，再加入外部柔和阴影（Shadow）与白色染色（Tint），最后计算镜面反射（Light）。严禁先覆盖半透明白层或绘制整圈死板的白边，否则材质会像廉价塑料。
 * BRDF 模型：介质高光使用 GGX 法线分布、Smith height-correlated visibility 与 Schlick Fresnel。基础反射率 F0 = pow((IOR - 1.0) / (IOR + 1.0), 2.0)，默认感知粗糙度（Perceptual Roughness）设为 0.32。
 * 光源阵列 (Lights)：使用围绕相机缓慢旋转的四点掠射柔光阵列，方向约为 xy = 0.90 ~ 0.96、z = 0.28 ~ 0.42，并赋予轻微冷暖色温差，使大部分边缘都有反射但强弱不同。
 * 边缘高光约束 (Support)：用曲率权重和 outerBevelSupport 把直接高光、环境 Fresnel 严格限制在倒角外半段。中心区域只保留折射、模糊和 Tint，避免 BRDF 平面瓣制造中心突起感。
交付要求
 * 请根据上述全部着色器与渲染管线规范，编写完整的材质/Shader代码以及对应的安卓 UI 组件封装（包含底部/顶部液态玻璃导航栏、课程详情液态玻璃信息栏）。
 * 暴露核心 Uniform 参数（如 uGlassThickness、uNormalTransition、uBackgroundDistance、IOR、dispersion、roughness 等）以便在代码中灵活微调视觉效果。
 * 确保在课程表滚动时，底层的课程色块与背景能够实时通过 paintComposeRT 传递并呈现真实的液态玻璃折射、边缘镜头扭曲与动态柔光高光效果。
