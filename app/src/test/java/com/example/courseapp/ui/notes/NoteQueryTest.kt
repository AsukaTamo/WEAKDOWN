package com.example.courseapp.ui.notes

import com.example.courseapp.data.model.NoteCategory
import com.example.courseapp.data.model.NotePhoto
import com.example.courseapp.data.repository.NoteFolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class NoteQueryTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun folder(
        id: String,
        name: String,
        photos: List<NotePhoto>,
        system: Boolean = false
    ) = NoteFolder(
        category = NoteCategory(id = id, name = name, isSystem = system),
        photos = photos,
        latestAt = photos.maxOfOrNull { it.capturedAt } ?: 0L
    )

    private val mathPhotos = listOf(
        NotePhoto(
            id = "m1",
            categoryId = "math",
            originalFileName = "m1.jpg",
            originalRelativePath = "originals/m1.jpg",
            title = "1.2 极限的定义",
            note = "数列极限",
            capturedAt = millis(2026, 11, 3, 9, 0)
        ),
        NotePhoto(
            id = "m2",
            categoryId = "math",
            originalFileName = "m2.jpg",
            originalRelativePath = "originals/m2.jpg",
            title = "导数公式",
            note = "重点",
            capturedAt = millis(2026, 11, 2, 10, 0)
        )
    )

    private val codePhotos = listOf(
        NotePhoto(
            id = "c1",
            categoryId = "code",
            originalFileName = "c1.jpg",
            originalRelativePath = "originals/c1.jpg",
            title = "二叉树",
            note = "",
            capturedAt = millis(2026, 11, 1, 15, 0)
        )
    )

    private val folders = listOf(
        folder("math", "高等数学", mathPhotos),
        folder("code", "数据结构", codePhotos),
        folder("uncat", "未归类", emptyList(), system = true)
    )

    @Test
    fun `empty query keeps all folders and category filter`() {
        assertEquals(3, filterNoteFolders(folders, "").size)
        assertEquals(1, filterNoteFolders(folders, "", "code").size)
        assertEquals("数据结构", filterNoteFolders(folders, "", "code").first().category.name)
    }

    @Test
    fun `category name match keeps the whole folder`() {
        val result = filterNoteFolders(folders, "高数")
        assertTrue(result.isEmpty())
        val byName = filterNoteFolders(folders, "数学")
        assertEquals(1, byName.size)
        assertEquals(2, byName.first().count)
    }

    @Test
    fun `photo title and note match their folder`() {
        assertEquals(1, filterNoteFolders(folders, "极限").size)
        assertEquals(1, filterNoteFolders(folders, "重点").size)
        assertEquals(1, matchingNotePhotos(folders, "二叉树").size)
        assertEquals(1, matchingNotePhotos(folders, "极限").size)
    }

    @Test
    fun `search results keep photo order newest first`() {
        val matches = matchingNotePhotos(folders, "")
        assertEquals(0, matches.size)
        val all = matchingNotePhotos(folders, "数")
        assertEquals(listOf("m1", "m2"), all.map { it.id })
    }

    @Test
    fun `query trims whitespace and matches case insensitively`() {
        val english = folder(
            "eng",
            "English",
            listOf(
                NotePhoto(
                    id = "e1",
                    categoryId = "eng",
                    originalFileName = "e1.jpg",
                    originalRelativePath = "originals/e1.jpg",
                    title = "Key Vocabulary",
                    note = "APPROACH"
                )
            )
        )
        assertEquals(1, filterNoteFolders(listOf(english), "  english  ").size)
        assertEquals(1, matchingNotePhotos(listOf(english), "vocabulary").size)
        assertEquals(1, matchingNotePhotos(listOf(english), "approach").size)
    }

    @Test
    fun `folder filter applies before search`() {
        assertEquals(0, filterNoteFolders(folders, "二叉树", "math").size)
        assertEquals(0, filterNoteFolders(folders, "极限", "code").size)
    }

    @Test
    fun `photo query returns all photos when blank`() {
        assertEquals(2, filterNotePhotos(mathPhotos, "").size)
        assertEquals(1, filterNotePhotos(mathPhotos, "导数").size)
    }
}

class NoteFormatTest {

    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = LocalDateTime.of(2026, 11, 3, 14, 30)
        .atZone(zone).toInstant().toEpochMilli()

    private fun at(month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(2026, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `today and yesterday use relative labels`() {
        assertEquals("今天 09:05", formatNoteTime(at(11, 3, 9, 5), now, zone))
        assertEquals("昨天 16:10", formatNoteTime(at(11, 2, 16, 10), now, zone))
    }

    @Test
    fun `older dates use month day`() {
        assertEquals("10月28日", formatNoteTime(at(10, 28, 8, 0), now, zone))
    }

    @Test
    fun `detail time includes year and minute`() {
        assertEquals(
            "2026年11月3日 09:05",
            formatNoteDateTime(at(11, 3, 9, 5), zone)
        )
    }

    @Test
    fun `zero timestamp is treated as empty`() {
        assertEquals("暂无内容", formatNoteTime(0L, now, zone))
    }
}
