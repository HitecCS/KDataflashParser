import org.junit.jupiter.api.Test

class DFReaderTextTest {
    private val dfReaderText = DFReaderText("log211.log", null, null)

    @Test
    fun getAllMessagesTest() {
        val allDFMessage = dfReaderText.getAllMessages(null)
        assert(allDFMessage.size == 130656)
    }

}