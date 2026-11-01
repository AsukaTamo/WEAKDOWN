package com.example.courseapp.ui.todo

import org.junit.Assert.*
import org.junit.Test

class TodoSheetMotionTest {

    @Test fun layersAreHiddenAtFabAndFullyVisibleAtPanel() {
        repeat(5) { index ->
            assertEquals(0f, todoSheetLayer(0f,index), .001f)
            assertEquals(1f, todoSheetLayer(1f,index), .001f)
        }
    }

    @Test fun eachGroupStartsFourFramesAfterPrevious() {
        for(index in 1..4) {
            val start = index * (4000f / 60f)
            assertEquals(0f,todoSheetLayer((start-1f)/480f,index),.001f)
            assertTrue(todoSheetLayer((start+5f)/480f,index) > 0f)
        }
    }

    @Test fun geometryHasStableEndpointsAndReusesSameCurveInReverse() {
        assertEquals(0f,todoSheetGeometry(0f),.0001f)
        assertEquals(1f,todoSheetGeometry(1f),.0001f)
        val opening=(0..100).map { todoSheetGeometry(it/100f) }
        val closing=(100 downTo 0).map { todoSheetGeometry(it/100f) }
        assertTrue(opening.zipWithNext().all { (a,b) -> b >= a })
        assertEquals(opening,closing.reversed())
    }

    @Test fun reversingUsesSameLayerAtSameGeometryPosition() {
        for(index in 0..4) {
            val opening = (0..100).map { todoSheetLayer(it/100f,index) }
            val closing = (100 downTo 0).map { todoSheetLayer(it/100f,index) }
            assertEquals(opening,closing.reversed())
            assertTrue(opening.zipWithNext().all { (a,b) -> b >= a })
        }
    }
}
