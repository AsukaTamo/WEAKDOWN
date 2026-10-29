package com.example.courseapp.data.model

/**
 * 学校目录：导入页和后端个人信息共用同一份列表，避免两处学校名称/网址漂移。
 *
 * [shortName] 用于「我的」页背景水印：有校徽的学校画校徽，没有校徽的学校用校名缩写
 * 画一个同尺寸的圆形徽章，保证切换学校时背景水印一定跟着变。
 */
data class School(
    val id: String,
    val name: String,
    val shortName: String,
    val portalUrl: String
)

object Schools {
    val CUG = School(
        id = "cug",
        name = "中国地质大学（武汉）",
        shortName = "地大",
        portalUrl = "https://sfrz.cug.edu.cn/tpass/login?service=https%3A%2F%2Fi.cug.edu.cn%2Fweb%2F%3FCASLOGIN%3DCASLOGIN%23%2Flogin%3FredirectUrl%3D%252Fweb%252F%253FCASLOGIN%253DCASLOGIN%2523%252Fcurrent%252Fsys-portal"
    )

    val All = listOf(
        CUG,
        School("whu", "武汉大学", "武大", "https://www.whu.edu.cn/"),
        School("hust", "华中科技大学", "华科", "https://www.hust.edu.cn/"),
        School("whut", "武汉理工大学", "武理", "https://www.whut.edu.cn/"),
        School("ccnu", "华中师范大学", "华师", "https://www.ccnu.edu.cn/"),
        School("zuel", "中南财经政法大学", "中南财", "https://www.zuel.edu.cn/")
    )

    val Default: School = CUG

    fun byId(id: String?): School = All.firstOrNull { it.id == id } ?: Default
}
