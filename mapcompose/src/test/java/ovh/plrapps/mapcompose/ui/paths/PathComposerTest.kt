package ovh.plrapps.mapcompose.ui.paths

import junit.framework.TestCase.assertEquals
import org.junit.Test
import ovh.plrapps.mapcompose.ui.paths.model.PatternItem.*

class PathComposerTest {
    @Test
    fun patternTest1() {
        val pattern = listOf(Dot, Gap(15f))
        val pathEffectData = makeIntervals(pattern, 10f, 1f)
        assertEquals(listOf(1f, 25f), pathEffectData?.intervals?.toList())
        assertEquals(0f, pathEffectData?.phase)
    }

    @Test
    fun patternTest2() {
        val pattern = listOf(Dot, Gap(15f), Dash(3f), Gap(15f))
        val pathEffectData = makeIntervals(pattern, 10f, 1f)
        assertEquals(listOf(1f, 25f, 3f, 25f), pathEffectData?.intervals?.toList())
        assertEquals(0f, pathEffectData?.phase)
    }

    @Test
    fun patternTest3() {
        val pattern = listOf(Dot, Dot, Gap(15f), Dash(3f))
        val pathEffectData = makeIntervals(pattern, 10f, 1f)
        assertEquals(listOf(1f, 10f, 1f, 25f, 3f, 10f), pathEffectData?.intervals?.toList())
        assertEquals(0f, pathEffectData?.phase)
    }

    @Test
    fun `pattern with leading gap`() {
        val pattern = listOf(Gap(25f), Dot, Dash(7f))
        val pathEffectData = makeIntervals(pattern, 10f, 1f)
        assertEquals(listOf(1f, 10f, 7f, 35f), pathEffectData?.intervals?.toList())
        assertEquals(25f, pathEffectData?.phase)
    }

    @Test
    fun `pattern with leading and trailing gap`() {
        val pattern = listOf(Gap(25f), Dot, Gap(15f))
        val pathEffectData = makeIntervals(pattern, 10f, 1f)
        assertEquals(listOf(1f, 50f), pathEffectData?.intervals?.toList())
        assertEquals(25f, pathEffectData?.phase)
    }

    @Test
    fun `consecutive gaps should be concatenated`() {
        val pattern = listOf(Dot, Gap(15f), Gap(4f), Dash(6f), Gap(9f))
        val pathEffectData = makeIntervals(pattern, 10f, 1f)
        assertEquals(listOf(1f, 29f, 6f, 19f), pathEffectData?.intervals?.toList())
        assertEquals(0f, pathEffectData?.phase)
    }
}