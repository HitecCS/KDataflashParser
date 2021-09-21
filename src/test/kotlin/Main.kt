
fun main() {

//    val dataFlashFile = File("2021-06-02 13-08-24.log")
    val filename = "log211.bin"
//    val filename = "log211.log"

    if (filename.endsWith(".log")) {
        val dfReader = DFReaderText(filename, null) { pct : Int -> println("percent $pct") }
        println(dfReader.toString())
        val a = dfReader.getAllMessages()
        val fieldLists = dfReader.getFieldLists(hashSetOf("Roll",
            "Pitch",
            "Yaw",
            "Lat",
            "Lng",
            "VD",
            "VN",
            "VE",
            "Airspeed",
            "Spd",
            "Alt",
            "SM",
            "NSats",
            "HDop",
            "Mode"))

        val baroAlts = dfReader.getFieldListConditional("Alt") { m -> m.getType() == "BARO" }
        val nonBaroAlts = dfReader.getFieldListConditional("Alt") { m -> m.getType() != "BARO" }
        println(fieldLists)
    } else {
        val dfReader = DFReaderBinary(filename, null)  { pct : Int -> println("percent $pct") }
        println(dfReader)
    }

}