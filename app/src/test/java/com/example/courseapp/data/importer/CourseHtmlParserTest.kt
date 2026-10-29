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
}
