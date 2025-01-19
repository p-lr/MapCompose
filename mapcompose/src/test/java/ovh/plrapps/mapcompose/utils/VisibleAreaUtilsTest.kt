package ovh.plrapps.mapcompose.utils

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import ovh.plrapps.mapcompose.api.VisibleArea

class VisibleAreaUtilsTest {
    @Test
    fun testVisibleAreaIntersect() {
        val area1 = VisibleArea(
            _p1x = 0.45080566406223405,
            _p1y = 0.3736979166669377,
            _p2x = 0.5572740261482944,
            _p2y = 0.3736979166669377,
            _p3x = 0.5572740261482944,
            _p3y = 0.5914016629881235,
            _p4x = 0.45080566406223405,
            _p4y = 0.5914016629881235
        )
        val area2 = VisibleArea(
            _p1x = 0.45167756735567244,
            _p1y = 0.3721153620806136,
            _p2x = 0.5485091051388183,
            _p2y = 0.3721153620806136,
            _p3x = 0.5485091051388183,
            _p3y = 0.6169437501755156,
            _p4x = 0.45167756735567244,
            _p4y = 0.6169437501755156
        )
        assertTrue(area1.intersects(area2))

        val area3 = area1.copy(
            _p1x = area1._p1x + 0.1,
            _p2x = area1._p2x + 0.1,
            _p3x = area1._p3x + 0.1,
            _p4x = area1._p4x + 0.1
        )
        assertTrue(area3.intersects(area1))

        val area4 = area1.copy(
            _p1x = area1._p1x + 0.1,
            _p1y = area1._p1y + 0.1,
            _p2x = area1._p2x + 0.1,
            _p2y = area1._p2y + 0.1,
            _p3x = area1._p3x + 0.1,
            _p3y = area1._p3y + 0.1,
            _p4x = area1._p4x + 0.1,
            _p4y = area1._p4y + 0.1
        )
        assertTrue(area4.intersects(area1))

        val area5 = area1.copy(
            _p1x = area1._p1x + 0.1,
            _p1y = area1._p1y + 0.1,
            _p2x = area1._p2x - 0.1,
            _p2y = area1._p2y + 0.1,
            _p3x = area1._p3x - 0.1,
            _p3y = area1._p3y - 0.1,
            _p4x = area1._p4x + 0.1,
            _p4y = area1._p4y - 0.1
        )
        assertTrue(area5.intersects(area1))

        val area6 = area1.copy(
            _p1x = area1._p1x + 1,
            _p1y = area1._p1y + 1,
            _p2x = area1._p2x + 1,
            _p2y = area1._p2y + 1,
            _p3x = area1._p3x + 1,
            _p3y = area1._p3y + 1,
            _p4x = area1._p4x + 1,
            _p4y = area1._p4y + 1
        )
        assertFalse(area6.intersects(area1))
    }
}