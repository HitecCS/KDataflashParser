/*
 * DFReaderClock_msec
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
 * DFReaderClock_msec - a format where many messages have TimeMS in
 * their formats, and GPS messages have a "T" field giving msecs
 */
class DFReaderClock_msec() : DFReaderClock() {

    /**
     * work out time basis for the log - new style
     */
    fun find_time_base( gps : DFMessage, first_ms_stamp : Int) {
        val t = _gpsTimeToTime(gps.Week!!, gps.TimeMS!!)
        set_timebase((t - gps.T!! * 0.001).toInt())
        timestamp = (timebase + first_ms_stamp * 0.001).toInt()
    }

    override fun set_message_timestamp(m : DFMessage) {
        if ("TimeMS" == m.fieldnames[0]) {
            m._timestamp = (timebase + m.TimeMS!! * 0.001).toLong()
        } else if (listOf("GPS", "GPS2").contains(m.get_type())) {
            m._timestamp = (timebase + m.T!! * 0.001).toLong()
        } else {
            m._timestamp = timestamp.toLong()
        }
        timestamp = m._timestamp.toInt()
    }
}