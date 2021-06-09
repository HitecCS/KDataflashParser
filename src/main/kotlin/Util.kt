/*
 * Util, a utility class for this project
 * Copyright (C) 2021 Hitec Commercial Solutions
 * Author, Stephen Woerner
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
 * This software is based on:
 * APM DataFlash log file reader
 * Copyright Andrew Tridgell 2011
 *
 * Released under GNU GPL version 3 or later
 * Partly based on SDLog2Parser by Anton Babushkin
 */

class Util {

//// TODO
///**
// * desperate attempt to convert a string regardless of what garbage we get
// */
//fun to_string(s : String): String {
//    var str = s
//    try {
//        return str.decode("utf-8")
//    } catch (e: Exception) {
//        // Do nothing
//    }
//
//    try {
//        val s2 = str.encode("utf-8", "ignore")
//        val x = u"%s" % s2
//        return s2
//    } catch (e: Exception) {
//        // Do nothing
//    }
//
//    // so its a nasty one.Let's grab as many characters as we can
//    var r = ""
//    while (str != "") {
//        try {
//            var r2 = r + str[0]
//            str = str.substring(1)
//            r2 = r2.encode("ascii", "ignore")
//            var x = u"%s" % r2
//            r = r2
//        } catch (e : Exception) {
//            break
//        }
//    }
//    return r + "_XXX"
//}

    /**
     * return true if a file appears to be a valid text log
     */
    fun DFReader_is_text_log(filename: String) {
        with open (filename, 'r') as f:
        val isTextLog = (f.read(8000).find("FMT,") != -1)

        return isTextLog
    }
}