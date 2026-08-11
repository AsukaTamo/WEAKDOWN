package com.example.courseapp.data.importer

import com.example.courseapp.data.model.CourseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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
    fun parsesRealDataHtmlWithTeacherAndFullLocation() {
        val dataFile = listOf(
            File("data.html"),
            File("..", "data.html"),
            File("../..", "data.html")
        ).firstOrNull { it.exists() }
            ?: error("Cannot find data.html in the expected locations")

        val html = dataFile.readText(Charsets.UTF_8)
        val parsed = CourseHtmlParser.parse(html)

        assertTrue(parsed.isNotEmpty())
        assertTrue(parsed.any { it.teacher.isNotBlank() })
        assertTrue(parsed.any { it.location.contains("公教2-303") || it.location.contains("公教2-509") })
        assertTrue(parsed.filter { it.name == "知识图谱" }.any { it.teacher == "姚宏" })
        assertTrue(parsed.filter { it.name == "深度学习" }.any { it.teacher == "黄绍广" })
    }
}
