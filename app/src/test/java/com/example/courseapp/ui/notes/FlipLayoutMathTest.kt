package com.example.courseapp.ui.notes

import com.example.courseapp.data.model.NormalizedRect
import com.example.courseapp.data.model.NoteAnnotationMapping
import com.example.courseapp.data.model.NoteEditorDocument
import com.example.courseapp.data.model.NotePoint
import com.example.courseapp.data.model.NoteStroke
import com.example.courseapp.data.model.NoteStrokeTool
import com.example.courseapp.data.model.NoteTextLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlipLayoutMathTest {

    @Test
    fun `grid rows keep equal cells and stable ids`() {
        val ids = listOf("p1", "p2", "p3", "p4")
        val bounds = buildGridBounds(
            ids = ids,
            containerWidth = 400f,
            columns = 2,
            horizontalGap = 10f,
            verticalGap = 12f,
            aspect = 0.9f
        )
        assertEquals(195f, bounds.getValue("p1").width, 0.001f)
        assertEquals(175.5f, bounds.getValue("p1").height, 0.001f)
        assertEquals(205f, bounds.getValue("p2").left, 0.001f)
        assertEquals(0f, bounds.getValue("p1").top, 0.001f)
        assertEquals(187.5f, bounds.getValue("p3").top, 0.001f)
        assertEquals(205f, bounds.getValue("p4").left, 0.001f)
    }

    @Test
    fun `list rows are full width and vertically stacked`() {
        val bounds = buildListBounds(
            ids = listOf("a", "b"),
            containerWidth = 400f,
            rowHeight = 92f,
            verticalGap = 12f
        )
        assertEquals(400f, bounds.getValue("a").width, 0.001f)
        assertEquals(92f, bounds.getValue("a").height, 0.001f)
        assertEquals(104f, bounds.getValue("b").top, 0.001f)
    }

    @Test
    fun `every photo keeps the same id in both modes`() {
        val ids = (1..7).map { "photo$it" }
        val grid = buildGridBounds(ids, 360f, 2, 11f, 12f, 0.94f)
        val list = buildListBounds(ids, 360f, 94f, 12f)
        assertEquals(ids.toSet(), grid.keys)
        assertEquals(ids.toSet(), list.keys)
    }
}

class NoteAnnotationMappingTest {

    @Test
    fun `output coordinates round trip through rotation and crop`() {
        val documents = listOf(
            NoteEditorDocument(),
            NoteEditorDocument(rotation = 90),
            NoteEditorDocument(rotation = 180, crop = NormalizedRect(0.1f, 0.2f, 0.9f, 0.8f)),
            NoteEditorDocument(rotation = 270, crop = NormalizedRect(0.2f, 0.1f, 0.75f, 0.6f))
        )
        val points = listOf(NotePoint(0.1f, 0.1f), NotePoint(0.5f, 0.5f), NotePoint(0.9f, 0.2f))
        documents.forEach { document ->
            points.forEach { point ->
                val output = NoteAnnotationMapping.toOutput(point, document)
                val back = NoteAnnotationMapping.fromOutput(output, document)
                assertEquals("x for $document", point.x, back.x, 0.0001f)
                assertEquals("y for $document", point.y, back.y, 0.0001f)
            }
        }
    }

    @Test
    fun `cropping zooms the annotation relative to the output image`() {
        val doc = NoteEditorDocument(crop = NormalizedRect(0.25f, 0.25f, 0.75f, 0.75f))
        val output = NoteAnnotationMapping.toOutput(NotePoint(0.5f, 0.5f), doc)
        assertEquals(0.5f, output.x, 0.001f)
        assertEquals(0.5f, output.y, 0.001f)
        assertEquals(2f, 1f / doc.crop.width, 0.001f)
    }

    @Test
    fun `size fraction compensates for rotated width`() {
        assertEquals(1f, NoteAnnotationMapping.sizeFractionScale(NoteEditorDocument(), 100, 200), 0.001f)
        assertEquals(
            0.5f,
            NoteAnnotationMapping.sizeFractionScale(NoteEditorDocument(rotation = 90), 100, 200),
            0.001f
        )
        assertEquals(
            1f,
            NoteAnnotationMapping.sizeFractionScale(NoteEditorDocument(rotation = 180), 100, 200),
            0.001f
        )
    }

    @Test
    fun `rotating crop four times returns to the original rect`() {
        val start = NormalizedRect(0.1f, 0.2f, 0.8f, 0.6f)
        var doc = NoteEditorDocument(crop = start)
        repeat(4) { doc = doc.rotatedClockwise() }
        assertEquals(start.left, doc.crop.left, 0.0001f)
        assertEquals(start.top, doc.crop.top, 0.0001f)
        assertEquals(start.right, doc.crop.right, 0.0001f)
        assertEquals(start.bottom, doc.crop.bottom, 0.0001f)
        assertEquals(0, doc.rotation)
    }

    @Test
    fun `rotation is normalized and identity detection is honest`() {
        assertTrue(NoteEditorDocument().isIdentity)
        assertFalse(NoteEditorDocument(rotation = 90).isIdentity)
        assertFalse(
            NoteEditorDocument(
                texts = listOf(NoteTextLayer(text = "重点")),
                strokes = listOf(
                    NoteStroke(points = listOf(NotePoint(0.1f, 0.1f)), tool = NoteStrokeTool.PEN)
                )
            ).isIdentity
        )
        assertEquals(270, NoteEditorDocument(rotation = -90).normalizedRotation)
    }

    @Test
    fun `sub rect stays inside the current crop`() {
        val doc = NoteEditorDocument(crop = NormalizedRect(0.2f, 0.2f, 0.8f, 0.8f))
        val updated = doc.withCrop(NormalizedRect(0f, 0f, 0.5f, 0.5f))
        assertEquals(0.2f, updated.crop.left, 0.0001f)
        assertEquals(0.2f, updated.crop.top, 0.0001f)
        assertEquals(0.5f, updated.crop.right, 0.0001f)
        assertEquals(0.5f, updated.crop.bottom, 0.0001f)
    }
}

class CropGeometryTest {

    @Test
    fun `corner drag never inverts the crop rect`() {
        val updated = updateDraftCrop(
            current = NormalizedRect(0.2f, 0.2f, 0.8f, 0.8f),
            handle = CropHandle.TOP_LEFT,
            dx = 2f,
            dy = 2f,
            ratio = null,
            displayAspect = 1f
        )
        assertTrue(updated.left <= updated.right)
        assertTrue(updated.top <= updated.bottom)
        assertEquals(0.72f, updated.left, 0.0001f)
        assertEquals(0.72f, updated.top, 0.0001f)
    }

    @Test
    fun `move keeps the crop size`() {
        val updated = updateDraftCrop(
            current = NormalizedRect(0.1f, 0.1f, 0.5f, 0.5f),
            handle = CropHandle.MOVE,
            dx = 0.8f,
            dy = 0.8f,
            ratio = null,
            displayAspect = 1f
        )
        assertEquals(0.4f, updated.width, 0.0001f)
        assertEquals(0.4f, updated.height, 0.0001f)
        assertTrue(updated.right <= 1f)
        assertTrue(updated.bottom <= 1f)
    }

    @Test
    fun `three to one ratio is applied in display space`() {
        val updated = updateDraftCrop(
            current = NormalizedRect(0f, 0f, 1f, 1f),
            handle = CropHandle.BOTTOM_RIGHT,
            dx = 0f,
            dy = 0f,
            ratio = 1f,
            displayAspect = 2f
        )
        // 归一化宽高比 = ratio / displayAspect = 0.5
        assertEquals(0.5f, updated.width / updated.height, 0.0001f)
    }
}
