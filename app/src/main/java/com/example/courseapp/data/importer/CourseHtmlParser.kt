package com.example.courseapp.data.importer

import com.example.courseapp.data.model.Course
import com.example.courseapp.data.model.CourseType
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

data class ParsedCourse(
    val name: String,
    val teacher: String = "",
    val location: String = "",
    val dayOfWeek: Int,       // 0=Mon .. 6=Sun
    val startSlot: Int,       // 0-based
    val slotCount: Int = 2,
    val weekRange: String = "",
    val type: CourseType = CourseType.REQUIRED
)

object CourseHtmlParser {

    fun parse(html: String, semester: String = ""): List<ParsedCourse> {
        val doc = Jsoup.parse(html)
        val results = mutableListOf<ParsedCourse>()

        // Try multiple parsing strategies
        results.addAll(parseTableFormat(doc))
        if (results.isEmpty()) {
            results.addAll(parseDivFormat(doc))
        }
        if (results.isEmpty()) {
            results.addAll(parseJsonFormat(html))
        }

        return results.distinctBy { "${it.name}_${it.dayOfWeek}_${it.startSlot}" }
    }

    /**
     * Strategy 1: Parse traditional table-based schedule
     * Most Chinese universities use a table with rows=periods, cols=days
     */
    private fun parseTableFormat(doc: Document): List<ParsedCourse> {
        val results = mutableListOf<ParsedCourse>()

        // Find the schedule table - usually the largest table or one with specific structure
        val tables = doc.select("table")
        val scheduleTable = tables.maxByOrNull { it.select("tr").size * it.select("tr td").size }
            ?: return results

        val rows = scheduleTable.select("tr")
        if (rows.size < 3) return results

        // Skip header row (days of week)
        // Typical structure: first row = day headers, subsequent rows = period pairs
        val dataRows = rows.drop(1)

        // Track which cells are already consumed (for rowspan handling)
        val consumed = mutableSetOf<Pair<Int, Int>>() // (row, col)

        for ((rowIdx, row) in dataRows.withIndex()) {
            val cells = row.select("td, th")
            var colIdx = 0

            for (cell in cells) {
                // Skip consumed cells
                while (consumed.contains(rowIdx to colIdx)) colIdx++

                val rowspan = cell.attr("rowspan").toIntOrNull() ?: 1
                val colspan = cell.attr("colspan").toIntOrNull() ?: 1

                // Mark consumed cells
                for (r in rowIdx until rowIdx + rowspan) {
                    for (c in colIdx until colIdx + colspan) {
                        consumed.add(r to c)
                    }
                }

                // Day of week (0-based, skip first column which is usually period label)
                val dayOfWeek = colIdx - 1
                if (dayOfWeek < 0 || dayOfWeek > 6) {
                    colIdx += colspan
                    continue
                }

                // Parse course info from cell text
                val cellText = cell.text().trim()
                if (cellText.isEmpty() || cellText.matches(Regex("\\d+"))) {
                    colIdx += colspan
                    continue
                }

                val parsed = parseCourseCell(cell, dayOfWeek, rowIdx, rowspan)
                if (parsed != null) {
                    results.add(parsed)
                }

                colIdx += colspan
            }
        }

        return results
    }

    /**
     * Parse a single table cell containing course information
     */
    private fun parseCourseCell(cell: Element, dayOfWeek: Int, rowIdx: Int, rowspan: Int): ParsedCourse? {
        // Try to find course info from structured elements
        val nameEl = cell.selectFirst(".course-name, .name, [class*=name], [class*=title]")
        val teacherEl = cell.selectFirst(".teacher, .instructor, [class*=teacher], [class*=instructor]")
        val locationEl = cell.selectFirst(".location, .place, .room, [class*=location], [class*=place], [class*=room]")
        val weekEl = cell.selectFirst(".week, [class*=week]")

        val name = nameEl?.text()?.trim() ?: extractCourseName(cell.text())
        if (name.isEmpty()) return null

        val teacher = teacherEl?.text()?.trim() ?: extractTeacher(cell.text())
        val location = locationEl?.text()?.trim() ?: extractLocation(cell.text())
        val weekRange = weekEl?.text()?.trim() ?: extractWeekRange(cell.text())

        return ParsedCourse(
            name = name,
            teacher = teacher,
            location = location,
            dayOfWeek = dayOfWeek,
            startSlot = rowIdx * 2,  // Most tables have 2 periods per row
            slotCount = rowspan * 2,
            weekRange = weekRange
        )
    }

    /**
     * Strategy 2: Parse div-based schedule layout
     */
    private fun parseDivFormat(doc: Document): List<ParsedCourse> {
        val results = mutableListOf<ParsedCourse>()

        // Look for common div-based schedule patterns
        val courseElements = doc.select("[class*=course], [class*=lesson], [class*=kb-item], [class*=course-item]")

        for (el in courseElements) {
            val name = el.selectFirst("[class*=name], [class*=title]")?.text()?.trim()
                ?: extractCourseName(el.text()) ?: continue
            val teacher = el.selectFirst("[class*=teacher]")?.text()?.trim()
                ?: extractTeacher(el.text())
            val location = el.selectFirst("[class*=location], [class*=room]")?.text()?.trim()
                ?: extractLocation(el.text())

            // Try to determine day and period from parent attributes or classes
            val dayAttr = el.attr("data-day").toIntOrNull()
                ?: el.attr("data-col").toIntOrNull()
                ?: el.className().let { cls ->
                    Regex("day[\\s-_]?(\\d)").find(cls)?.groupValues?.get(1)?.toIntOrNull()
                } ?: continue

            val periodAttr = el.attr("data-period").toIntOrNull()
                ?: el.attr("data-row").toIntOrNull()
                ?: el.className().let { cls ->
                    Regex("period[\\s-_]?(\\d)").find(cls)?.groupValues?.get(1)?.toIntOrNull()
                } ?: 0

            val rowspan = el.attr("data-rowspan").toIntOrNull()
                ?: el.attr("rowspan").toIntOrNull() ?: 1

            results.add(ParsedCourse(
                name = name,
                teacher = teacher,
                location = location,
                dayOfWeek = (dayAttr - 1).coerceIn(0, 6),
                startSlot = periodAttr,
                slotCount = rowspan * 2
            ))
        }

        return results
    }

    /**
     * Strategy 3: Parse JSON-embedded schedule data
     * Some modern portals embed schedule data as JSON in script tags
     */
    private fun parseJsonFormat(html: String): List<ParsedCourse> {
        val results = mutableListOf<ParsedCourse>()

        // Look for JSON data in script tags
        val jsonPattern = Regex("""(?:"courseList"|"lessons"|"schedule"|"kbList")\s*:\s*(\[[\s\S]*?\])""")
        val match = jsonPattern.find(html) ?: return results

        try {
            val jsonStr = match.groupValues[1]
            // Simple parsing - extract objects
            val objPattern = Regex("""\{[^{}]*\}""")
            for (obj in objPattern.findAll(jsonStr)) {
                val objStr = obj.value
                val name = extractJsonValue(objStr, "name", "courseName", "title") ?: continue
                val teacher = extractJsonValue(objStr, "teacher", "instructor", "teacherName") ?: ""
                val location = extractJsonValue(objStr, "location", "room", "classroom", "place") ?: ""
                val day = extractJsonValue(objStr, "day", "dayOfWeek", "weekday")?.toIntOrNull() ?: continue
                val period = extractJsonValue(objStr, "period", "startPeriod", "startSlot")?.toIntOrNull() ?: 0
                val count = extractJsonValue(objStr, "count", "periodCount", "slotCount")?.toIntOrNull() ?: 2
                val weeks = extractJsonValue(objStr, "weeks", "weekRange", "week") ?: ""

                results.add(ParsedCourse(
                    name = name,
                    teacher = teacher,
                    location = location,
                    dayOfWeek = (day - 1).coerceIn(0, 6),
                    startSlot = period,
                    slotCount = count,
                    weekRange = weeks
                ))
            }
        } catch (_: Exception) {}

        return results
    }

    // ── Helper extraction functions ──

    private fun extractCourseName(text: String): String {
        // Course name is usually the first line or the longest meaningful text
        val lines = text.split("\n", " ").map { it.trim() }.filter { it.isNotEmpty() }
        return lines.firstOrNull { it.length >= 2 && !it.matches(Regex("[\\d\\s:：\\-周节]+")) } ?: ""
    }

    private fun extractTeacher(text: String): String {
        val pattern = Regex("(?:教师|老师|主讲|授课)[：:]?\\s*([\\u4e00-\\u9fa5]{2,4})")
        return pattern.find(text)?.groupValues?.get(1) ?: ""
    }

    private fun extractLocation(text: String): String {
        val pattern = Regex("(?:教室|地点|上课地点|教室编号)[：:]?\\s*([\\u4e00-\\u9fa5A-Za-z0-9\\-]+楼?[\\u4e00-\\u9fa5A-Za-z0-9\\-]*)")
        val match = pattern.find(text)
        if (match != null) return match.groupValues[1]

        // Try to find room-like patterns (e.g., "教三楼301", "A201")
        val roomPattern = Regex("[A-Za-z]?\\d号楼?\\d{2,3}[室号]?|[A-Z]\\d{3}")
        return roomPattern.find(text)?.value ?: ""
    }

    private fun extractWeekRange(text: String): String {
        val pattern = Regex("(\\d+)[-~至](\\d+)\\s*周")
        return pattern.find(text)?.value ?: ""
    }

    private fun extractJsonValue(json: String, vararg keys: String): String? {
        for (key in keys) {
            val pattern = Regex(""""$key"\s*:\s*"([^"]*)"""")
            val match = pattern.find(json)
            if (match != null) return match.groupValues[1]
            // Try without quotes (numeric values)
            val numPattern = Regex(""""$key"\s*:\s*(\d+)""")
            val numMatch = numPattern.find(json)
            if (numMatch != null) return numMatch.groupValues[1]
        }
        return null
    }

    /**
     * Convert parsed courses to Course entities
     */
    fun toCourses(parsed: List<ParsedCourse>, semester: String): List<Course> {
        return parsed.map { p ->
            Course(
                name = p.name,
                teacher = p.teacher,
                location = p.location,
                dayOfWeek = p.dayOfWeek,
                startSlot = p.startSlot,
                slotCount = p.slotCount,
                type = p.type,
                weekRange = p.weekRange.ifEmpty { "1-18周" },
                semester = semester
            )
        }
    }
}
