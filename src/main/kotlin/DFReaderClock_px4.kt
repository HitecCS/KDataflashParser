/*
 * DFReaderClock_px4
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
 * DFReaderClock_px4 - a format where a starting time is explicitly given in a message
 */
class DFReaderClock_px4() : DFReaderClock() {

    var px4_timebase : Int

    init {
        px4_timebase = 0
    }

    /**
     * work out time basis for the log - PX4 native
     */
    fun find_time_base( gps : DFMessage) {
        val t = gps.GPSTime * 1.0e-6
        timebase = t - px4_timebase
    }

    fun set_px4_timebase( time_msg: DFMessage) {
        px4_timebase = time_msg.StartTime * 1.0e-6
    }

    fun set_message_timestamp( m) {
        m._timestamp = timebase + px4_timebase
    }

    fun message_arrived( m) {
        type = m.get_type()
        if (type == "TIME" && "StartTime" in m._fieldnames) {
            set_px4_timebase(m)
        }
    }
}
