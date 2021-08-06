/*
 * DFReaderClock_usec
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

/**
 * DFReaderClock_usec - use microsecond timestamps from messages
 */
class DFReaderClock_usec(): DFReaderClock() {

    /**
     * work out time basis for the log - even newer style
     */
    fun find_time_base( gps: DFMessage, first_us_stamp: Int) {
        val t = _gpsTimeToTime(gps.GWk!!, gps.GMS!!)
        set_timebase((t - gps.TimeUS!! * 0.000001).toFloat())
//         this ensures FMT messages get appropriate timestamp:
        timestamp = (timebase + first_us_stamp * 0.000001).toFloat()
    }

    /**
     * The TimeMS in some messages is not from *our* clock!
     */
    fun type_has_good_TimeMS(type: String) : Boolean {
        if (type.startsWith("ACC")) {
            return false
        }
        if ( type.startsWith("GYR")) {
            return false
        }
        return true
    }

    fun should_use_msec_field0( m: DFMessage) : Boolean {
        if (type_has_good_TimeMS (m.get_type())) {
            return false
        }
        if ("TimeMS" != m.fieldnames[0]) {
            return false
        }
        if (timebase + m.TimeMS!! * 0.001 < timestamp) {
            return false
        }
        return true
    }

    override fun set_message_timestamp(m: DFMessage) {
        if("TimeUS" == m.fieldnames[0]) {
//             only format messages don 't have a TimeUS in them...
            m._timestamp = (timebase + m.TimeUS!! * 0.000001).toLong()
        } else if(should_use_msec_field0(m)) {
//             ... in theory. I expect there to be some logs which are not
//             "pure":
            m._timestamp = (timebase + m.TimeMS!! * 0.001).toLong()
        } else {
            m._timestamp = timestamp.toLong()
        }
        timestamp = m._timestamp.toFloat()
    }
}