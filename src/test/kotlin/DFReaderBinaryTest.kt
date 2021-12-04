import org.junit.jupiter.api.Test

class DFReaderBinaryTest {

    val dfReaderBin = DFReaderBinary("log211.bin",null,null)
    @Test
    fun getAllMessagesTest() {
        val allDFMessage = dfReaderBin.getAllMessages(null)
        assert(allDFMessage.size == 130656)
    }

    @Test
    fun getStartAndEndTimesTest() {
        val startAndEndTimes = dfReaderBin.getStartAndEndTimes()
        assert(startAndEndTimes.first == 14259290234L && startAndEndTimes.second == 14259290368L)
    }
}