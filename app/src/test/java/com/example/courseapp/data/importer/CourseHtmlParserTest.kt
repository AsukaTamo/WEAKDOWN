package com.example.courseapp.data.importer

import com.example.courseapp.data.model.CourseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseHtmlParserTest {
    @Test
    fun parsesTeacherAndLocationFromTimetableCell() {
        val html = """
            <table>
              <tr>
                <td colspan="9">header</td>
              </tr>
              <tr>
                <td>时间段</td>
                <td>节次</td>
                <td>星期一</td>
                <td>星期二</td>
                <td>星期三</td>
                <td>星期四</td>
                <td>星期五</td>
                <td>星期六</td>
                <td>星期日</td>
              </tr>
              <tr>
                <td rowspan="4">上午</td>
                <td>1</td>
                <td rowspan="2" class="td_wrap">
                  <div class="timetable_con text-left">
                    <p class="title">知识图谱*</p>
                    <p><span title="节/周"></span> (1-2节)1-5周,7周</p>
                    <p><span title="上课地点"></span> 未来城校区 公教2-303</p>
                    <p><span title="教师"></span> 姚宏</p>
                    <p><span title="课程性质简称"></span> 选修</p>
                    <p><span title="考核方式"></span> 未安排</p>
                  </div>
                </td>
              </tr>
            </table>
        """.trimIndent()

        val parsed = CourseHtmlParser.parse(html)

        assertEquals(1, parsed.size)
        assertEquals("知识图谱", parsed[0].name)
        assertEquals("姚宏", parsed[0].teacher)
        assertEquals("公教2-303", parsed[0].location)
        assertEquals(CourseType.ELECTIVE, parsed[0].type)
        assertEquals("1-5,7周", parsed[0].weekRange)
    }

    @Test
    fun mapsPublicElectiveCourseType() {
        val html = """
            <table>
              <tr>
                <td colspan="9">header</td>
              </tr>
              <tr>
                <td>时间段</td>
                <td>节次</td>
                <td>星期一</td>
                <td>星期二</td>
                <td>星期三</td>
                <td>星期四</td>
                <td>星期五</td>
                <td>星期六</td>
                <td>星期日</td>
              </tr>
              <tr>
                <td rowspan="4">上午</td>
                <td>1</td>
                <td rowspan="2" class="td_wrap">
                  <div class="timetable_con text-left">
                    <p class="title">艺术鉴赏*</p>
                    <p><span title="节/周"></span> (1-2节)1-8周</p>
                    <p><span title="上课地点"></span> 公教1-101</p>
                    <p><span title="教师"></span> 王老师</p>
                    <p><span title="课程性质简称"></span> 通选</p>
                  </div>
                </td>
              </tr>
            </table>
        """.trimIndent()

        val parsed = CourseHtmlParser.parse(html)

        assertEquals(1, parsed.size)
        assertEquals(CourseType.PUBLIC_ELECTIVE, parsed[0].type)
    }

    @Test
    fun parsesTableFixtureWithTeacherAndFullLocation() {
        val html = checkNotNull(javaClass.getResourceAsStream("/timetable_sample.html")) {
            "Missing test resource: /timetable_sample.html"
        }.bufferedReader(Charsets.UTF_8).use { it.readText() }

        val parsed = CourseHtmlParser.parse(html)

        assertEquals(1, parsed.size)
        val course = parsed.single()
        assertEquals("高等数学", course.name)
        assertEquals("张老师", course.teacher)
        assertEquals("公教2-303", course.location)
        assertEquals("未来城校区", course.campus)
        assertEquals("1-5,7-9周", course.weekRange)
        assertEquals(CourseType.REQUIRED, course.type)
    }

    private fun whutFixture(): String =
        checkNotNull(javaClass.getResourceAsStream("/whut_timetable_sample.html")) {
            "Missing test resource: /whut_timetable_sample.html"
        }.bufferedReader(Charsets.UTF_8).use { it.readText() }

    @Test
    fun parsesWhutJwappTimetable() {
        val parsed = CourseHtmlParser.parse(whutFixture())

        assertEquals(17, parsed.size)

        val monday = parsed.single { it.name == "课程A" && it.dayOfWeek == 0 && it.startSlot == 0 }
        assertEquals("教师甲", monday.teacher)
        assertEquals("南院-教学楼-212", monday.location)
        assertEquals("南湖校区", monday.campus)
        assertEquals(2, monday.slotCount)
        assertEquals("1-4,6,7-9周", monday.weekRange)

        // 第6-8节（第三大节）落在 0 基第 5 节，而不是按 unit 8 直接减一。
        val thirdBlock = parsed.single { it.name == "课程D" && it.dayOfWeek == 2 && it.startSlot == 5 }
        assertEquals(3, thirdBlock.slotCount)

        // 同一个大节里两门课占的节数不同：要按格子里的「第三节-第四节」修正到 2 节。
        val tuesday = parsed.filter { it.name == "课程E" && it.dayOfWeek == 1 }
        assertEquals(2, tuesday.size)
        assertEquals(listOf(2, 3), tuesday.map { it.slotCount }.sorted())
        assertEquals("12周", tuesday.single { it.slotCount == 2 }.weekRange)

        // 晚课（第五大节 第11-13节）映射到 0 基第 10 节。
        val evening = parsed.single { it.name == "课程H" }
        assertEquals(3, evening.dayOfWeek)
        assertEquals(10, evening.startSlot)
        assertEquals(3, evening.slotCount)
        assertEquals("1-16周", evening.weekRange)

        // 多段周次按 app 的格式归一化。
        assertEquals("1-2,5-6,8,9-10周", parsed.single { it.name == "课程C" && it.dayOfWeek == 4 }.weekRange)

        // 「本」是修读层次标记，不能进课程名。
        assertTrue(parsed.none { it.name.startsWith("本") })
    }

    @Test
    fun whutCoursesKeepCampusInNotes() {
        val courses = CourseHtmlParser.toCourses(CourseHtmlParser.parse(whutFixture()), "2025-2026-1")

        val monday = courses.single { it.name == "课程A" && it.location == "南院-教学楼-212" }
        assertEquals(0, monday.dayOfWeek)
        assertEquals(0, monday.startSlot)
        assertTrue(monday.notes.contains("校区: 南湖校区"))
    }
}
