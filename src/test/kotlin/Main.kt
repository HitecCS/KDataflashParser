
fun main() {

    val filename = "log211.log"
    val filename2 = "log211.bin"

    val dfTextParser = DataFlashParser(filename) { pct : Int -> println("percent $pct") }
    val allTextMessages = dfTextParser.getAllMessages()
    val fieldLists = dfTextParser.getFieldLists(hashSetOf("Roll",
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

    val baroAlts = dfTextParser.getFieldListConditional("Alt") { m -> m.getType() == "BARO" }
    val nonBaroAlts = dfTextParser.getFieldListConditional("Alt") { m -> m.getType() != "BARO" }

    val dfBinParser = DataFlashParser(filename2)  { pct : Int -> println("percent $pct") }
    val allBinMessages = dfBinParser.getAllMessages()
    val binFieldLists = dfBinParser.getFieldLists(hashSetOf("Roll",
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
    val baroAlts2 = dfBinParser.getFieldListConditional("Alt") { m -> m.getType() == "BARO" }
    val nonBaroAlts2 = dfBinParser.getFieldListConditional("Alt") { m -> m.getType() != "BARO" }

    println(dfTextParser)
    println(dfBinParser)

}