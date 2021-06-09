import java.io.File

fun main() {
    val df = DataFlashParser()

    val dataFlashFile = File("testfile.log")

    df.parse(dataFlashFile)

}