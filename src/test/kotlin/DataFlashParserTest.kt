import org.junit.jupiter.api.Test

class DataFlashParserTest {

    val dfParserText = DataFlashParser("log211.log", null)
    val dfParserBin = DataFlashParser("log211.bin", null)
    @Test
    fun getAllMessagesTest() {
        val allTextMessages = dfParserText.getAllMessages(null)
        assert(allTextMessages.size == 130656)

        val allBinMessage = dfParserBin.getAllMessages(null)
        assert(allBinMessage.size == 130656)
    }

    @Test
    fun constructorTest() {
        val allTextMessages = dfParserText.getAllMessages(null)
        assert(allTextMessages.size == 130656)

        val allBinMessage = dfParserBin.getAllMessages(null)
        assert(allBinMessage.size == 130656)
    }
}