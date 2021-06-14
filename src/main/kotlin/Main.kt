import java.io.File

fun main() {

    val dataFlashFile = File("testfile.log")

    val filename = dataFlashFile.absolutePath.toString()
    val dfParser = if (filename.endsWith(".log")) {
        DFReader_text(filename)
    } else {
        DFReader_binary(filename)
    }

    while (true) {
        if ( dfParser.recv_msg() == null)
            break
    }
}