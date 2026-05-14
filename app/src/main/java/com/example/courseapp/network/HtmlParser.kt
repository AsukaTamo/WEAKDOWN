package com.example.courseapp.network

import com.example.courseapp.data.model.Course
import com.example.courseapp.data.model.CourseType
import org.jsoup.Jsoup

object HtmlParser {

    fun parseScheduleHtml(html: String): List<Course> {
        val courses = mutableListOf<Course>()
        val doc = Jsoup.parse(html)
        val rows = doc.select("table tr")

        for ((rowIdx, row) in rows.withIndex()) {
            if (rowIdx == 0) continue // skip header
            val cells = row.select("td")
            for ((colIdx, cell) in cells.withIndex()) {
                val text = cell.text().trim()
                if (text.isNotEmpty()) {
                    val parts = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                    if (parts.isNotEmpty()) {
                        courses.add(
                            Course(
                                name = parts.getOrElse(0) { "未知课程" },
                                teacher = parts.getOrElse(1) { "" },
                                location = parts.getOrElse(2) { "" },
                                dayOfWeek = colIdx.coerceIn(0, 6),
                                startSlot = (rowIdx - 1).coerceAtLeast(0),
                                slotCount = 2,
                                type = CourseType.REQUIRED
                            )
                        )
                    }
                }
            }
        }
        return courses
    }

    fun parseCourseListHtml(html: String): List<Course> {
        val courses = mutableListOf<Course>()
        val doc = Jsoup.parse(html)
        val items = doc.select(".course-item, tr.course-row, .kc-item")

        for (item in items) {
            val name = item.selectFirst(".name, .course-name, td:first-child")?.text()?.trim() ?: continue
            val teacher = item.selectFirst(".teacher, .course-teacher, td:nth-child(2)")?.text()?.trim() ?: ""
            val location = item.selectFirst(".location, .course-location, td:nth-child(3)")?.text()?.trim() ?: ""

            courses.add(
                Course(
                    name = name,
                    teacher = teacher,
                    location = location,
                    dayOfWeek = 0,
                    startSlot = 0,
                    slotCount = 2
                )
            )
        }
        return courses
    }
}
