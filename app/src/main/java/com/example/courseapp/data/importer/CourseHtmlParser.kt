package com.example.courseapp.data.importer

import android.util.Log
import com.example.courseapp.data.model.Course
import com.example.courseapp.data.model.CourseType
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

private const val TAG = "HtmlParser"

data class ParsedCourse(
    val name: String,
    val teacher: String = "",
    val location: String = "",
    val campus: String = "",
    val dayOfWeek: Int,        // 0=Mon .. 6=Sun
    val startSlot: Int,        // 0-based start period
    val slotCount: Int = 2,
    val weekRange: String = "", // normalized: "1-18周" or "1,3,5-16周"
    val type: CourseType = CourseType.REQUIRED,
    val credits: Float = 0f,
    val assessmentMethod: String = "",  // 考核方式
    val notes: String = ""              // 选课备注
)

object CourseHtmlParser {

    fun parse(html: String): List<ParsedCourse> {
        Log.d(TAG, "parse() called, html length: ${html.length}")
        val doc = Jsoup.parse(html)

        val tables = doc.select("table")
        Log.d(TAG, "Found ${tables.size} <table> elements")

        val results = mutableListOf<ParsedCourse>()

        results.addAll(parseTableFormat(doc))
        Log.d(TAG, "parseTableFormat returned ${results.size} courses")

        if (results.isEmpty()) {
            results.addAll(parseDivFormat(doc))
            Log.d(TAG, "parseDivFormat returned ${results.size} courses")
        }
        if (results.isEmpty()) {
            results.addAll(parseJsonFormat(html))
            Log.d(TAG, "parseJsonFormat returned ${results.size} courses")
        }

        return results.distinctBy { "${it.name}_${it.dayOfWeek}_${it.startSlot}_${it.weekRange}" }
    }

    // ── Strategy 1: Table-based (most common Chinese university schedule) ──

    private fun parseTableFormat(doc: Document): List<ParsedCourse> {
        val results = mutableListOf<ParsedCourse>()
        val tables = doc.select("table")

        for ((tableIdx, table) in tables.withIndex()) {
            val rows = table.select("tr")
            Log.d(TAG, "Table #$tableIdx: ${rows.size} rows")
            if (rows.size < 3) continue

            // Detect weekday column mapping — try header first, then first few data rows
            // Some tables have a merged header cell, with weekday labels in the first data row
            var weekdayColMap = mapOf<Int, Int>()
            var headerRowCount = 0

            for (tryRow in rows.take(3)) {
                val cells = tryRow.select("th, td")
                val texts = cells.map { it.text().trim() }
                Log.d(TAG, "  Trying row for weekdays: $texts")
                val detected = detectWeekdayColumns(cells)
                if (detected.isNotEmpty()) {
                    weekdayColMap = detected
                    Log.d(TAG, "  Weekday column map: $weekdayColMap (from row $headerRowCount)")
                    break
                }
                headerRowCount++
            }
            if (weekdayColMap.isEmpty()) continue

            Log.d(TAG, "Using headerRowCount=$headerRowCount, weekdayColMap=$weekdayColMap")

            // Pre-scan: build a map of rowIdx -> period number from column 1
            val dataRows = rows.drop(headerRowCount)
            // The weekday row is always dataRows[0] — skip it for course parsing
            val courseStartRow = 1
            Log.d(TAG, "dataRows=${dataRows.size}, courseStartRow=$courseStartRow")
            // Build row → period map: scan column 1 for period labels,
            // fill gaps by incrementing from last known value.
            val rowPeriodMap = mutableMapOf<Int, Int>()
            val tempConsumed = mutableSetOf<Pair<Int, Int>>()
            for ((rowIdx, row) in dataRows.withIndex()) {
                val cells = row.select("td, th")
                var colIdx = 0
                var foundPeriod = false
                for (cell in cells) {
                    while (tempConsumed.contains(rowIdx to colIdx)) colIdx++
                    val rs = cell.attr("rowspan").toIntOrNull() ?: 1
                    val cs = cell.attr("colspan").toIntOrNull() ?: 1
                    for (r in rowIdx until rowIdx + rs) {
                        for (c in colIdx until colIdx + cs) {
                            tempConsumed.add(r to c)
                        }
                    }
                    if (colIdx == 1) {
                        val periodNum = cell.text().trim().toIntOrNull()
                        if (periodNum != null) {
                            rowPeriodMap[rowIdx] = periodNum
                            foundPeriod = true
                        }
                        break
                    }
                    colIdx += cs
                }
                // If no explicit period label, inherit from previous row + 1
                if (!foundPeriod && rowIdx > 0) {
                    val prevPeriod = rowPeriodMap[rowIdx - 1]
                    if (prevPeriod != null) {
                        rowPeriodMap[rowIdx] = prevPeriod + 1
                    }
                }
            }
            Log.d(TAG, "Row period map: $rowPeriodMap")

            // Now parse course cells
            val consumed = mutableSetOf<Pair<Int, Int>>()
            val weekdayKeywords = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日", "周一", "周二", "周三", "周四", "周五", "周六", "周日")

            for ((rowIdx, row) in dataRows.withIndex()) {
                // Skip the weekday header row
                val rowText = row.text()
                val isWeekdayRow = rowIdx < courseStartRow || weekdayKeywords.any { rowText.contains(it) }
                if (isWeekdayRow) {
                    // Mark its cells as consumed so rowspan tracking works
                    val hCells = row.select("td, th")
                    var hCol = 0
                    for (hCell in hCells) {
                        while (consumed.contains(rowIdx to hCol)) hCol++
                        val hrs = hCell.attr("rowspan").toIntOrNull() ?: 1
                        val hcs = hCell.attr("colspan").toIntOrNull() ?: 1
                        for (r in rowIdx until rowIdx + hrs) {
                            for (c in hCol until hCol + hcs) {
                                consumed.add(r to c)
                            }
                        }
                        hCol += hcs
                    }
                    Log.d(TAG, "Skipping weekday row $rowIdx")
                    continue
                }
                val cells = row.select("td, th")
                var colIdx = 0

                for (cell in cells) {
                    // Skip cells consumed by rowspan from above
                    while (consumed.contains(rowIdx to colIdx)) colIdx++

                    val rowspan = cell.attr("rowspan").toIntOrNull() ?: 1
                    val colspan = cell.attr("colspan").toIntOrNull() ?: 1

                    // Mark all cells covered by rowspan/colspan as consumed
                    for (r in rowIdx until rowIdx + rowspan) {
                        for (c in colIdx until colIdx + colspan) {
                            consumed.add(r to c)
                        }
                    }

                    val cellText = cell.text().trim()

                    // Column 0: time period label — skip
                    if (colIdx == 0) {
                        colIdx += colspan
                        continue
                    }

                    // Column 1: period number — skip (already pre-scanned)
                    if (colIdx == 1) {
                        colIdx += colspan
                        continue
                    }

                    // Columns 2+: course cells for weekdays
                    val dayOfWeek = weekdayColMap[colIdx]
                    if (dayOfWeek == null) {
                        colIdx += colspan
                        continue
                    }

                    // Skip empty cells
                    if (cellText.isEmpty()) {
                        colIdx += colspan
                        continue
                    }

                    // Use the period from the row where this cell STARTS (rowIdx)
                    // This is critical for rowspan cells — the period belongs to the start row
                    val period = rowPeriodMap[rowIdx] ?: (rowIdx + 1)
                    val startSlot = period - 1
                    val slotCount = rowspan

                    Log.d(TAG, "Cell at row=$rowIdx col=$colIdx day=$dayOfWeek period=$period rowspan=$rowspan text='${cellText.take(50)}'")

                    val parsed = parseCourseCell(cell, dayOfWeek, startSlot, slotCount)
                    if (parsed.isNotEmpty()) {
                        for (p in parsed) {
                            Log.d(TAG, "  -> Parsed course: ${p.name}, weeks=${p.weekRange}")
                        }
                        results.addAll(parsed)
                    } else {
                        Log.d(TAG, "  -> parseCourseCell returned empty")
                    }

                    colIdx += colspan
                }
            }

            if (results.isNotEmpty()) return results
        }

        return results
    }

    /**
     * Detect which column indices correspond to which weekdays (0=Mon..6=Sun).
     */
    private fun detectWeekdayColumns(headerCells: List<Element>): Map<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        val weekdayPatterns = listOf(
            listOf("星期一", "周一", "Monday"),
            listOf("星期二", "周二", "Tuesday"),
            listOf("星期三", "周三", "Wednesday"),
            listOf("星期四", "周四", "Thursday"),
            listOf("星期五", "周五", "Friday"),
            listOf("星期六", "周六", "Saturday"),
            listOf("星期日", "星期天", "周日", "周天", "Sunday")
        )

        for ((colIdx, cell) in headerCells.withIndex()) {
            val text = cell.text().trim()
            for ((dayIdx, patterns) in weekdayPatterns.withIndex()) {
                if (patterns.any { text.contains(it) }) {
                    map[colIdx] = dayIdx
                    break
                }
            }
        }

        return map
    }

    /**
     * Parse a course cell. Structure:
     * <div class="timetable_con">
     *     <span class="title ...">课程名*</span>
     *     <p>(1-2节)1-5周,7-9周</p>
     *     <p>校区 教室</p>
     *     <p>教师：姓名</p>
     *     <p>考核 | 学分：3.0</p>
     * </div>
     */
    private fun parseCourseCell(cell: Element, dayOfWeek: Int, startSlot: Int, slotCount: Int): List<ParsedCourse> {
        val results = mutableListOf<ParsedCourse>()
        // Find all timetable_con divs (may have multiple courses in one cell)
        val courseDivs = cell.select("div.timetable_con")
        if (courseDivs.isEmpty()) {
            // Fallback: try parsing the whole cell
            val single = parseSingleCourseBlock(cell.html(), dayOfWeek, startSlot, slotCount)
            if (single != null) results.add(single)
            return results
        }

        // Parse each course div
        for (div in courseDivs) {
            val result = parseSingleCourseBlock(div.html(), dayOfWeek, startSlot, slotCount)
            if (result != null) results.add(result)
        }
        return results
    }

    /**
     * Parse a single course block from HTML content.
     */
    private fun parseSingleCourseBlock(html: String, dayOfWeek: Int, startSlot: Int, slotCount: Int): ParsedCourse? {
        val doc = Jsoup.parseBodyFragment(html)
        val body = doc.body()

        // Extract course name from <span class="title"> or first line with *
        var courseName = ""
        val titleSpan = body.selectFirst("span.title, span[class*=title]")
        if (titleSpan != null) {
            courseName = titleSpan.text().replace("*", "").trim()
        }
        if (courseName.isEmpty()) {
            // Fallback: find text with *
            val text = body.text()
            val starMatch = Regex("([\\u4e00-\\u9fa5A-Za-z0-9（）()·【】\\[\\] ]+?)\\*").find(text)
            if (starMatch != null) {
                courseName = starMatch.groupValues[1].trim()
            }
        }
        if (courseName.isEmpty()) {
            // Last resort: first meaningful text
            val lines = body.text().split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            courseName = lines.firstOrNull {
                it.length >= 2 && !it.matches(Regex("[\\d\\s:：\\-周节次第,，().|/]+"))
            }?.replace("*", "")?.trim() ?: ""
        }
        if (courseName.isEmpty()) {
            Log.d(TAG, "    parseSingleCourseBlock: no course name found in '${body.text().take(80)}'")
            return null
        }

        // Extract fields from <p> elements
        val paragraphs = body.select("p")
        var weekRange = ""
        var location = ""
        var campus = ""
        var teacher = ""
        var assessment = ""
        var credits = 0f

        for (p in paragraphs) {
            val pText = p.text().trim()

            // Week range + period info: "(1-2节)1-5周,7-9周" or "7-8周；9-11周(单)"
            if (weekRange.isEmpty()) {
                val weekResult = extractWeekRangeFromText(pText)
                if (weekResult != null) {
                    weekRange = weekResult
                    continue
                }
            }

            // Location: "未来城校区 公教2-403" or "校区 教室"
            if (location.isEmpty() || campus.isEmpty()) {
                val locResult = extractLocationAndCampus(pText)
                if (locResult != null) {
                    campus = locResult.first
                    location = locResult.second
                    continue
                }
            }

            // Teacher: "教师：万林"
            if (teacher.isEmpty()) {
                val teacherMatch = Regex("教师[：:：]\\s*(.+)").find(pText)
                if (teacherMatch != null) {
                    teacher = teacherMatch.groupValues[1].trim()
                    continue
                }
            }

            // Assessment + credits: "考试 | 学分：3.0" or "考查 | 学分：2.0" or just "学分：1.0"
            val creditMatch = Regex("学分[：:：]\\s*([\\d.]+)").find(pText)
            if (creditMatch != null) {
                credits = creditMatch.groupValues[1].toFloatOrNull() ?: 0f
            }
            val assessMatch = Regex("(考试|考查|考察)").find(pText)
            if (assessMatch != null) {
                assessment = assessMatch.groupValues[1]
            }
        }

        // Fallback: try to extract from full text if paragraphs didn't work
        val fullText = body.text()
        if (teacher.isEmpty()) {
            val m = Regex("教师[：:：]\\s*([\\u4e00-\\u9fa5/]+)").find(fullText)
            if (m != null) teacher = m.groupValues[1].trim()
        }
        if (credits == 0f) {
            val m = Regex("学分[：:：]\\s*([\\d.]+)").find(fullText)
            if (m != null) credits = m.groupValues[1].toFloatOrNull() ?: 0f
        }

        return ParsedCourse(
            name = courseName,
            teacher = teacher,
            location = location,
            campus = campus,
            dayOfWeek = dayOfWeek,
            startSlot = startSlot,
            slotCount = slotCount,
            weekRange = normalizeWeekRange(weekRange),
            credits = credits,
            assessmentMethod = assessment
        )
    }

    /**
     * Extract week range from text like "(1-2节)1-5周,7-9周" or "7-8周；9-11周(单)"
     * Returns normalized week range string like "1-5,7-9周"
     */
    private fun extractWeekRangeFromText(text: String): String? {
        // Must contain 周 to be a week range line
        if (!text.contains("周")) return null

        // Remove period info in parentheses like "(1-2节)"
        val cleaned = text.replace(Regex("\\(\\d+-\\d+节\\)"), "").trim()

        // Use a single regex to extract all week segments with optional parity markers.
        // Matches: "1-5周", "7-9周(单)", "11周", "15-17周(双)"
        // The parity marker "(单)" or "(双)" is part of the segment it follows.
        val segmentPattern = Regex("(\\d+)(?:\\s*[-~至]\\s*(\\d+))?\\s*周\\s*(?:\\((单|双)\\))?")
        val matches = segmentPattern.findAll(cleaned).toList()

        if (matches.isEmpty()) return null

        val segments = matches.map { m ->
            val start = m.groupValues[1]
            val end = m.groupValues[2]
            val parity = m.groupValues[3]
            val range = if (end.isNotEmpty()) "$start-$end" else start
            val suffix = if (parity.isNotEmpty()) "($parity)" else ""
            "$range$suffix"
        }

        return segments.joinToString(",") + "周"
    }

    /**
     * Extract campus and location from text like "未来城校区 公教2-403"
     * Returns Pair(campus, location) or null
     */
    private fun extractLocationAndCampus(text: String): Pair<String, String>? {
        // Common campus names
        val campusPattern = Regex("(.*?校区)")
        val campusMatch = campusPattern.find(text)
        if (campusMatch != null) {
            val campus = campusMatch.groupValues[1].trim()
            val location = text.substring(campusMatch.range.last + 1).trim()
            if (location.isNotEmpty()) {
                return campus to location
            }
        }

        // Try building + room pattern: "公教2-403", "课2-306"
        val roomPattern = Regex("[\\u4e00-\\u9fa5A-Za-z]+\\d+-\\d+")
        val roomMatch = roomPattern.find(text)
        if (roomMatch != null && text.length <= 30) {
            return "" to roomMatch.value
        }

        return null
    }

    /**
     * Normalize week range to consistent format.
     * Input: "1-5,7-9周" or "1-5(单),7-9周"
     * Output: "1-5,7-9周" (preserves single/double week markers)
     */
    private fun normalizeWeekRange(raw: String): String {
        if (raw.isEmpty()) return "1-18周"

        var s = raw.trim()
            .replace("第", "").replace("到", "-").replace("至", "-")
            .replace("，", ",").replace("、", ",").replace("；", ",")

        // Ensure ends with 周
        if (!s.endsWith("周")) s += "周"

        // If it already looks valid, return
        if (s.matches(Regex("[\\d,\\-~()单双]+周"))) return s

        // Try to extract just the numeric/comma/dash part
        val m = Regex("([\\d,\\-~()单双]+)\\s*周").find(s)
        return m?.groupValues?.get(1)?.plus("周") ?: "1-18周"
    }

    // ── Strategy 2: Div-based ──

    private fun parseDivFormat(doc: Document): List<ParsedCourse> {
        val results = mutableListOf<ParsedCourse>()
        val courseElements = doc.select(
            "[class*=course], [class*=lesson], [class*=kb-item], [class*=course-item], [class*=kbcontent]"
        )

        for (el in courseElements) {
            val text = el.text().trim()
            val name = extractCourseNameFromText(text) ?: continue

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
                name = name.replace("*", "").trim(),
                teacher = extractTeacherFromText(text),
                location = extractLocationFromText(text),
                dayOfWeek = (dayAttr - 1).coerceIn(0, 6),
                startSlot = periodAttr,
                slotCount = rowspan,
                weekRange = normalizeWeekRange(extractWeekSimple(text))
            ))
        }

        return results
    }

    // ── Strategy 3: JSON ──

    private fun parseJsonFormat(html: String): List<ParsedCourse> {
        val results = mutableListOf<ParsedCourse>()
        val jsonPattern = Regex("""(?:"courseList"|"lessons"|"schedule"|"kbList")\s*:\s*(\[[\s\S]*?\])""")
        val match = jsonPattern.find(html) ?: return results

        try {
            val jsonStr = match.groupValues[1]
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
                    name = name.replace("*", "").trim(),
                    teacher = teacher,
                    location = location,
                    dayOfWeek = (day - 1).coerceIn(0, 6),
                    startSlot = period,
                    slotCount = count,
                    weekRange = normalizeWeekRange(weeks)
                ))
            }
        } catch (_: Exception) {}

        return results
    }

    // ── Text extraction helpers ──

    private fun extractCourseNameFromText(text: String): String? {
        val starMatch = Regex("([\\u4e00-\\u9fa5A-Za-z0-9（）()·【】\\[\\] ]+?)\\*").find(text)
        if (starMatch != null) return starMatch.groupValues[1].trim()

        val lines = text.split("\n", "\r").map { it.trim() }.filter { it.isNotEmpty() }
        return lines.firstOrNull {
            it.length >= 2 && !it.matches(Regex("[\\d\\s:：\\-周节次第,，.()|/]+"))
        }?.replace("*", "")?.trim()
    }

    private fun extractTeacherFromText(text: String): String {
        val m = Regex("教师[：:：]\\s*([\\u4e00-\\u9fa5/]+)").find(text)
        return m?.groupValues?.get(1)?.trim() ?: ""
    }

    private fun extractLocationFromText(text: String): String {
        val m = Regex("[\\u4e00-\\u9fa5A-Za-z]+\\d+-\\d+").find(text)
        return m?.value ?: ""
    }

    private fun extractWeekSimple(text: String): String {
        val m = Regex("(\\d+[-~,，]\\d+)\\s*周").find(text)
        return m?.value ?: ""
    }

    private fun extractJsonValue(json: String, vararg keys: String): String? {
        for (key in keys) {
            val pattern = Regex(""""$key"\s*:\s*"([^"]*)"""")
            val match = pattern.find(json)
            if (match != null) return match.groupValues[1]
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
            val notesList = mutableListOf<String>()
            if (p.assessmentMethod.isNotEmpty()) notesList.add("考核方式: ${p.assessmentMethod}")
            if (p.notes.isNotEmpty()) notesList.add(p.notes)
            if (p.campus.isNotEmpty()) notesList.add("校区: ${p.campus}")

            Course(
                name = p.name,
                teacher = p.teacher,
                location = p.location,
                dayOfWeek = p.dayOfWeek,
                startSlot = p.startSlot,
                slotCount = p.slotCount,
                type = p.type,
                weekRange = p.weekRange.ifEmpty { "1-18周" },
                semester = semester,
                credits = p.credits,
                notes = notesList.joinToString("\n")
            )
        }
    }
}
