
fun main() {

//    val dataFlashFile = File("2021-06-02 13-08-24.log")
    val filename2 = "log211.bin"
    val filename = "log211.log"

//    if (filename.endsWith(".log")) {
        val dfReader = DataFlashParser(filename) { pct : Int -> println("percent $pct") }
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
//    } else {
        val dfReader2 = DataFlashParser(filename2)  { pct : Int -> println("percent $pct") }
        val baroAlts2 = dfReader2.getFieldListConditional("Alt") { m -> m.getType() == "BARO" }
        val nonBaroAlts2 = dfReader.getFieldListConditional("Alt") { m -> m.getType() != "BARO" }
        println(dfReader)
//    }

}