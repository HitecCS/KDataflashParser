import org.junit.jupiter.api.Test

class DFReaderClockTest {
    @Test
    fun gpsTimeToTimeTest() {
        val testVal = DFReaderClockUSec().gpsTimeToTime(2138.0f, 1.79188192E8f)
        assert(testVal == 1.609206370192E9)
    }
}