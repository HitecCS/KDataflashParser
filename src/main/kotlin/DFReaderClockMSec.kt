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
 * DFReaderClockMSec - a format where many messages have TimeMS in
 * their formats, and GPS messages have a "T" field giving msecs
 */
class DFReaderClockMSec : DFReaderClock() {

    /**
     * work out time basis for the log - new style
     */
    fun findTimeBase(gps : DFMessage, firstMSStamp : Float) {
        val t = gpsTimeToTime(gps.Week!!, gps.TimeMS!!)
        timebase = ((t - gps.T!! * 0.001))
        timestamp = (timebase + firstMSStamp * 0.001)
    }

    override fun setMessageTimestamp(m : DFMessage) {
        if ("TimeMS" == m.fieldnames[0]) {
            m.timestamp = (timebase + m.TimeMS!! * 0.001).toLong()
        } else if (listOf("GPS", "GPS2").contains(m.getType())) {
            m.timestamp = (timebase + m.T!! * 0.001).toLong()
        } else {
            m.timestamp = timestamp.toLong()
        }
        timestamp = m.timestamp.toDouble()
    }
}