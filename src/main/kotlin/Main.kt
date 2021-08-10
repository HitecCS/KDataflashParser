import java.io.File

fun main() {

//    val dataFlashFile = File("2021-06-02 13-08-24.log")
    val dataFlashFile = File("test3.log")

    val filename = dataFlashFile.absolutePath.toString()
    val dfParser = if (filename.endsWith(".log")) {
        val callback = object : ProgressCallback {
            override fun update(pct: Int) {
                println("$pct")
            }
        }
        val dfreader = DFReaderText(filename, null, callback)
        println(dfreader.toString())
        val a = dfreader.allMessages
        println(a)
    } else {
//        DFReader_binary(filename, null, null)
    }

//    while (true) {
//        if ( dfParser.recv_msg() == null)
//            break
//    }
}