package demos

import DataFlashParser

/**
 * Demo program
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2021 Hitec Commercial Solutions
 * @author Stephen Woerner
 */
fun main() {

    // Change the name of this to name of your file
//    val filename = "log101.bin"
    val filename = "137.BIN"

    // The DataFlashParser class accepts binary or text DataFlash files and an optional progress callback, which will
    // return an integer (0-100) representing the percent of the log which has been parsed/indexed
    val dfParser = DataFlashParser(filename) { pct : Int -> println("Percent indexed: $pct") }

    val startAndEndTimes = dfParser.getStartAndEndTimes()
    println("Start time: ${startAndEndTimes.first}; End time: ${startAndEndTimes.second}")

    // The MSG message type contains useful text information in its Message field such as information about vehicle
    // hardware and firmware. More information about messages can be found in the Ardupilot and Px4 documentation:
    // Copter: https://ardupilot.org/copter/docs/logmessages.html
    // Plane: https://ardupilot.org/plane/docs/logmessages.html
    val msgs = dfParser.getAllMessagesOfType("MSG", null)

    msgs.forEach {
        println("MSG: ${it.Message}")
    }

    // This function allows you to search for entries of a particular field across
    val results = dfParser.getFieldLists(hashSetOf("Roll",
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
        "Mode"), null)

    val pitches = results["Pitch"]

    // If you would like to set up some conditional logic on whether an entry should be included in the results you can
    // use this method
    val nonBaroAltitudes = dfParser.getFieldListConditional("Alt", { dfMessage -> dfMessage.getType() != "BARO" }, null)

    // If your system has RAM for it, you can get all the DFMessages parsed out in there full form. This is not recommended on Android
    val allDFMessages = dfParser.getAllMessages(null)

}