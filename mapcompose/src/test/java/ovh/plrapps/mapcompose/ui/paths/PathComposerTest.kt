package ovh.plrapps.mapcompose.ui.paths

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import junit.framework.TestCase.assertEquals
import org.junit.Test
import ovh.plrapps.mapcompose.ui.paths.model.PatternItem.*

class PathComposerTest {
    private val density = object : Density {
        override val density: Float = 1f
        override val fontScale: Float = 1f
    }

    @Test
    fun patternTest1() {
        val pattern = listOf(Dot, Gap(15.dp))
        val pathEffectData = makeIntervals(pattern, 10f, 1f, density)
        assertEquals(listOf(1f, 25f), pathEffectData?.intervals?.toList())
        assertEquals(0f, pathEffectData?.phase)
    }

    @Test
    fun patternTest2() {
        val pattern = listOf(Dot, Gap(15.dp), Dash(3.dp), Gap(15.dp))
        val pathEffectData = makeIntervals(pattern, 10f, 1f, density)
        assertEquals(listOf(1f, 25f, 3f, 25f), pathEffectData?.intervals?.toList())
        assertEquals(0f, pathEffectData?.phase)
    }

    @Test
    fun patternTest3() {
        val pattern = listOf(Dot, Dot, Gap(15.dp), Dash(3.dp))
        val pathEffectData = makeIntervals(pattern, 10f, 1f, density)
        assertEquals(listOf(1f, 10f, 1f, 25f, 3f, 10f), pathEffectData?.intervals?.toList())
        assertEquals(0f, pathEffectData?.phase)
    }

    @Test
    fun `pattern with leading gap`() {
        val pattern = listOf(Gap(25.dp), Dot, Dash(7.dp))
        val pathEffectData = makeIntervals(pattern, 10f, 1f, density)
        assertEquals(listOf(1f, 10f, 7f, 35f), pathEffectData?.intervals?.toList())
        assertEquals(25f, pathEffectData?.phase)
    }

    @Test
    fun `pattern with leading and trailing gap`() {
        val pattern = listOf(Gap(25.dp), Dot, Gap(15.dp))
        val pathEffectData = makeIntervals(pattern, 10f, 1f, density)
        assertEquals(listOf(1f, 50f), pathEffectData?.intervals?.toList())
        assertEquals(25f, pathEffectData?.phase)
    }

    @Test
    fun `consecutive gaps should be concatenated`() {
        val pattern = listOf(Dot, Gap(15.dp), Gap(4.dp), Dash(6.dp), Gap(9.dp))
        val pathEffectData = makeIntervals(pattern, 10f, 1f, density)
        assertEquals(listOf(1f, 29f, 6f, 19f), pathEffectData?.intervals?.toList())
        assertEquals(0f, pathEffectData?.phase)
    }
}