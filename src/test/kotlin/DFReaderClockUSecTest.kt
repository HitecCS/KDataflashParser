import org.junit.jupiter.api.Test

class DFReaderClockUSecTest {

    val parent = DFReaderText("log211.log", null, null)

    @Test
    fun findTimeBaseTest() {
        val testClock = DFReaderClockUSec()

        val dfFormat = DFFormat(130,"GPS",50,"QBIHBcLLeffffB","TimeUS,Status,GMS,GWk,NSats,HDop,Lat,Lng,Alt,Spd,GCrs,VZ,Yaw,U")

        val elements = arrayListOf("132936353",
            "3",
            "179188200",
            "2138",
            "9",
            "1.21",
            "36.727971",
            "127.4285675",
            "154.53",
            "0.279",
             "66.03654",
             "0.032",
             "0",
             "1")
        val dfMessage = DFMessage(dfFormat, elements, false, parent)

        testClock.findTimeBase(dfMessage, 125978878)

        assert(testClock.timestamp == 1.609206363234525E9)
        assert(testClock.timebase == 1.609206237255647E9)
    }

}