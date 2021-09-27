import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UtilTest {
    @Test
    fun isDFTextLog() {
        val filename1 = "log211.log"
        val filename2 = "log211.bin"

        val isTextLog = Util.isDFTextLog(filename1)
        assertTrue(isTextLog)

        val isNotTextLog = Util.isDFTextLog(filename2)
        assertFalse(isNotTextLog)
    }
}