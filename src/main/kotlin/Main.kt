import java.io.File

fun main() {

    val dataFlashFile = File("2021-06-02 13-08-24.log")

    val filename = dataFlashFile.absolutePath.toString()
    val dfParser = if (filename.endsWith(".log")) {
        DFReader_text(filename, null, null)
    } else {
//        DFReader_binary(filename, null, null)
    }

//    while (true) {
//        if ( dfParser.recv_msg() == null)
//            break
//    }
}