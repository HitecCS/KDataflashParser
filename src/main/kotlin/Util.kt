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
    companion object {
        val mode_mapping_apm = hashMapOf(
            Pair(0, "MANUAL"),
            Pair(1, "CIRCLE"),
            Pair(2, "STABILIZE"),
            Pair(3, "TRAINING"),
            Pair(4, "ACRO"),
            Pair(5, "FBWA"),
            Pair(6, "FBWB"),
            Pair(7, "CRUISE"),
            Pair(8, "AUTOTUNE"),
            Pair(10, "AUTO"),
            Pair(11, "RTL"),
            Pair(12, "LOITER"),
            Pair(13, "TAKEOFF"),
            Pair(14, "AVOID_ADSB"),
            Pair(15, "GUIDED"),
            Pair(16, "INITIALISING"),
            Pair(17, "QSTABILIZE"),
            Pair(18, "QHOVER"),
            Pair(19, "QLOITER"),
            Pair(20, "QLAND"),
            Pair(21, "QRTL"),
            Pair(22, "QAUTOTUNE"),
            Pair(23, "QACRO"),
            Pair(24, "THERMAL"),
        )

        val mode_mapping_acm = hashMapOf(
            Pair(0, "STABILIZE"),
            Pair(1, "ACRO"),
            Pair(2, "ALT_HOLD"),
            Pair(3, "AUTO"),
            Pair(4, "GUIDED"),
            Pair(5, "LOITER"),
            Pair(6, "RTL"),
            Pair(7, "CIRCLE"),
            Pair(8, "POSITION"),
            Pair(9, "LAND"),
            Pair(10, "OF_LOITER"),
            Pair(11, "DRIFT"),
            Pair(13, "SPORT"),
            Pair(14, "FLIP"),
            Pair(15, "AUTOTUNE"),
            Pair(16, "POSHOLD"),
            Pair(17, "BRAKE"),
            Pair(18, "THROW"),
            Pair(19, "AVOID_ADSB"),
            Pair(20, "GUIDED_NOGPS"),
            Pair(21, "SMART_RTL"),
            Pair(22, "FLOWHOLD"),
            Pair(23, "FOLLOW"),
            Pair(24, "ZIGZAG"),
        )

        val mode_mapping_rover = hashMapOf(
            Pair(0, "MANUAL"),
            Pair(1, "ACRO"),
            Pair(2, "LEARNING"),
            Pair(3, "STEERING"),
            Pair(4, "HOLD"),
            Pair(5, "LOITER"),
            Pair(6, "FOLLOW"),
            Pair(7, "SIMPLE"),
            Pair(10, "AUTO"),
            Pair(11, "RTL"),
            Pair(12, "SMART_RTL"),
            Pair(15, "GUIDED"),
            Pair(16, "INITIALISING")
        )

        val mode_mapping_tracker = hashMapOf(
            Pair(0, "MANUAL"),
            Pair(1, "STOP"),
            Pair(2, "SCAN"),
            Pair(4, "GUIDED"),
            Pair(10, "AUTO"),
            Pair(16, "INITIALISING")
        )

        val mode_mapping_sub = hashMapOf(
            Pair(0, "STABILIZE"),
            Pair(1, "ACRO"),
            Pair(2, "ALT_HOLD"),
            Pair(3, "AUTO"),
            Pair(4, "GUIDED"),
            Pair(7, "CIRCLE"),
            Pair(9, "SURFACE"),
            Pair(16, "POSHOLD"),
            Pair(19, "MANUAL"),
        )
        var AP_MAV_TYPE_MODE_MAP_DEFAULT = hashMapOf(
//        # copter
            Pair(MAV_TYPE.MAV_TYPE_HELICOPTER, mode_mapping_acm),
            Pair(MAV_TYPE.MAV_TYPE_TRICOPTER, mode_mapping_acm),
            Pair(MAV_TYPE.MAV_TYPE_QUADROTOR, mode_mapping_acm),
            Pair(MAV_TYPE.MAV_TYPE_HEXAROTOR, mode_mapping_acm),
            Pair(MAV_TYPE.MAV_TYPE_OCTOROTOR, mode_mapping_acm),
            Pair(MAV_TYPE.MAV_TYPE_DECAROTOR, mode_mapping_acm),
            Pair(MAV_TYPE.MAV_TYPE_DODECAROTOR, mode_mapping_acm),
            Pair(MAV_TYPE.MAV_TYPE_COAXIAL, mode_mapping_acm),
//        # plane
            Pair(MAV_TYPE.MAV_TYPE_FIXED_WING, mode_mapping_apm),
//        # rover
            Pair(MAV_TYPE.MAV_TYPE_GROUND_ROVER, mode_mapping_rover),
//        # boat
            Pair(MAV_TYPE.MAV_TYPE_SURFACE_BOAT, mode_mapping_rover), //# for the time being
//        # tracker
            Pair(MAV_TYPE.MAV_TYPE_ANTENNA_TRACKER, mode_mapping_tracker),
//        # sub
            Pair(MAV_TYPE.MAV_TYPE_SUBMARINE, mode_mapping_sub),
        )
//// TODO
///**
// * desperate attempt to convert a string regardless of what garbage we get
// * from DFReader.py
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
         * from: DFReader.py
         */
        fun DFReader_is_text_log(filename: String) {
            with open (filename, 'r') as f:
            val isTextLog = (f.read(8000).find("FMT,") != -1)

            return isTextLog
        }

        /**
         * evaluation an expression
         * from: Python mavexpression.py
         */
        fun evaluate_expression(expression, vars, nocondition = False) {
            // first check for conditions which take the form EXPRESSION { CONDITION }
            val v = Any?
            if (expression[-1] == '}') {
                startidx = expression.rfind('{')
                if (startidx == -1) {
                    return null
                }
                condition = expression[startidx + 1:-1]
                expression = expression[:startidx]
                try {
                    v = eval(condition, globals(), vars)
                } catch (e: Throwable) {
                    return null
                }
                if (!nocondition and not v) {
                    return null
                }
            }
            try {
                v = eval(expression, globals(), vars)
            } catch (e: Throwable) {
                return null
            }
            return v
        }


        /**
         * evaluation a conditional (boolean) statement
         * from: Python mavutil.py
         */
        fun evaluate_condition(condition, vars): Boolean {
            if (condition == null)
                return true
            var v = evaluate_expression(condition, vars)
            if (v == null)
                return false
            return v
        }

        /**
         * return dictionary mapping mode numbers to name, or None if unknown
         */
        fun mode_mapping_bynumber(mav_type: MAV_TYPE): HashMap<Int, String>? {
            return if (AP_MAV_TYPE_MODE_MAP_DEFAULT.containsKey(mav_type)) AP_MAV_TYPE_MODE_MAP_DEFAULT[mav_type] else null
        }

        /**
         * return mode string for APM:Plane
         */
        fun mode_string_apm(mode_number: Int): String {
            if (mode_mapping_apm.contains(mode_number))
                return mode_mapping_apm[mode_number]!!
            return "Mode($mode_number)"
        }

        /**
         * return mode string for APM:Copter
         */
        fun mode_string_acm(mode_number: Int): String {
            if (mode_mapping_acm.contains(mode_number))
                return mode_mapping_acm[mode_number]!!
            return "Mode(%$mode_number)"
        }

        /** map from a PX4 "main_state" to a string; see msg/commander_state.msg
         This allows us to map sdlog STAT.MainState to a simple "mode"
         string, used in DFReader and possibly other places.  These are
         related but distict from what is found in mavlink messages; see
         "Custom mode definitions", below.
        */
        val mainstate_mapping_px4 = hashMapOf(
            Pair(0 , "MANUAL"),
            Pair(1 , "ALTCTL"),
            Pair(2 , "POSCTL"),
            Pair(3 , "AUTO_MISSION"),
            Pair(4 , "AUTO_LOITER"),
            Pair(5 , "AUTO_RTL"),
            Pair(6 , "ACRO"),
            Pair(7 , "OFFBOARD"),
            Pair(8 , "STAB"),
            Pair(9 , "RATTITUDE"),
            Pair(10 , "AUTO_TAKEOFF"),
            Pair(11 , "AUTO_LAND"),
            Pair(12 , "AUTO_FOLLOW_TARGET"),
            Pair(13 , "MAX"),
        )

        fun mode_string_px4(MainState : Int) : String {
            return if(mainstate_mapping_px4.containsKey(MainState)) mainstate_mapping_px4[MainState]!! else "Unknown"
        }
    }
}