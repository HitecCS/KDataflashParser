import org.junit.jupiter.api.Test

class DataFlashParserTest {

    val dfParserText = DataFlashParser("log211.log", null)
    val dfParserBin = DataFlashParser("log211.bin", null)
    @Test
    fun getAllMessagesTest() {
        val allTextMessages = dfParserText.getAllMessages()
        assert(allTextMessages.size == 130656)

        val allBinMessage = dfParserBin.getAllMessages()
        assert(allBinMessage.size == 130656)
    }
}