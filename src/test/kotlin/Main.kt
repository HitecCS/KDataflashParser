
fun main() {

    val filename = "log211.log"
//    val filename2 = "log4.bin"
    val filename2 = "137.BIN"
    val filename3 = "log137.log"
//    val dfTextParser = DataFlashParser(filename) { pct : Int -> println("percent $pct") }
//    val allTextMessages = dfTextParser.getAllMessages()
//    val fieldLists = dfTextParser.getFieldLists(hashSetOf("Roll",
//        "Pitch",
//        "Yaw",
//        "Lat",
//        "Lng",
//        "VD",
//        "VN",
//        "VE",
//        "Airspeed",
//        "Spd",
//        "Alt",
//        "SM",
//        "NSats",
//        "HDop",
//        "Mode"))
//
//    val baroAlts = dfTextParser.getFieldListConditional("Alt") { m -> m.getType() == "BARO" }
//    val nonBaroAlts = dfTextParser.getFieldListConditional("Alt") { m -> m.getType() != "BARO" }

//    val a = Byte(A)
//    val b = a and 0xFF

//    val a = 178
//    val u = a.toUByte()
//    val a2= u.toInt()

    val dfReaderText = DataFlashParser(filename3) { pct : Int -> println("percent $pct") }
    val start = dfReaderText.getStartAndEndTimes()

    val dfBinParser = DataFlashParser(filename2)  { pct : Int -> println("percent $pct") }
//    val allBinMessages = dfBinParser.getAllMessages()
    val lastTimestamp = dfBinParser.getStartAndEndTimes()
    val binFieldLists = dfBinParser.getFieldLists(hashSetOf(
//        "Roll",
//        "Pitch",
//        "Yaw",
//        "Lat",
//        "Lng",
        "VD",
        "VN",
        "VE",
//        "Airspeed",
//        "Spd",
//        "Alt",
//        "SM",
//        "NSats",
//        "HDop",
//        "Mode"
    ))
//    val baroAlts2 = dfBinParser.getFieldListConditional("Alt") { m -> m.getType() == "BARO" }
//    val nonBaroAlts2 = dfBinParser.getFieldListConditional("Alt") { m -> m.getType() != "BARO" }

//    println(dfTextParser)
    println(dfBinParser)

}