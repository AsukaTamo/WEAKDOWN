package com.example.courseapp.data.model

/**
 * 学校目录：导入页和后端个人信息共用同一份列表，避免两处学校名称/网址漂移。
 *
 * [shortName] 用于「我的」页背景水印：有校徽的学校画校徽，没有校徽的学校用校名缩写
 * 画一个同尺寸的圆形徽章，保证切换学校时背景水印一定跟着变。
 *
 * [portalUrl] 直接指向学校统一身份认证（CAS）登录入口，而不是学校官网首页：
 * 导入页内置浏览器打开后就能直接登录，少一次手跳。
 */
data class School(
    val id: String,
    val name: String,
    val shortName: String,
    val portalUrl: String,
    /**
     * 教务系统是否只对手机版页面开放课表表格。
     * 武汉理工的 jwapp 教务是移动端 Web 应用，用桌面 UA 打开会进另一套页面，
     * 抓不到 `wut_table` 那种课表表格，所以导入页默认用手机 UA。
     */
    val mobileUa: Boolean = false
)

object Schools {
    val CUG = School(
        id = "cug",
        name = "中国地质大学（武汉）",
        shortName = "地大",
        portalUrl = "https://sfrz.cug.edu.cn/tpass/login?service=https%3A%2F%2Fi.cug.edu.cn%2Fweb%2F%3FCASLOGIN%3DCASLOGIN%23%2Flogin%3FredirectUrl%3D%252Fweb%252F%253FCASLOGIN%253DCASLOGIN%2523%252Fcurrent%252Fsys-portal"
    )

    val WHUT = School(
        id = "whut",
        name = "武汉理工大学",
        shortName = "武理",
        portalUrl = "https://zhlgd.whut.edu.cn/tpass/login?service=https%3A%2F%2Fjwxt.whut.edu.cn%2Fjwapp%2Fsys%2Fhomeapp%2Findex.do%3FforceCas%3D1",
        mobileUa = true
    )

    val All = listOf(
        CUG,
        School("whu", "武汉大学", "武大", "https://www.whu.edu.cn/"),
        School("hust", "华中科技大学", "华科", "https://www.hust.edu.cn/"),
        WHUT,
        School("ccnu", "华中师范大学", "华师", "https://www.ccnu.edu.cn/"),
        School("zuel", "中南财经政法大学", "中南财", "https://www.zuel.edu.cn/")
    )

    /** 导入页未指定学校时的兜底，仅在用户没有填过个人信息时生效。 */
    val Default: School = CUG

    /** 找不到就回落到 [Default]；需要区分「没选学校」时用 [byIdOrNull]。 */
    fun byId(id: String?): School = byIdOrNull(id) ?: Default

    /** 空 id / 未知 id 视为「没选学校」，用于「我的」页默认不显示任何校徽。 */
    fun byIdOrNull(id: String?): School? {
        if (id.isNullOrBlank()) return null
        return All.firstOrNull { it.id == id }
    }
}
