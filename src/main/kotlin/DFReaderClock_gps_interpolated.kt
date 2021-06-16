/*
 * DFReaderClock_gps_interpolated
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
 * DFReaderClock_gps_interpolated - for when the only real references in a message are GPS timestamps
 */
class DFReaderClock_gps_interpolated() : DFReaderClock() {

    var msg_rate : HashMap<String, Int>
    var counts : List<String>
    var counts_since_gps : HashMap<String, Int>

    init {
        msg_rate = hashMapOf()
        counts = listOf()
        counts_since_gps = hashMapOf()
    }

    /**
     * reset counters on rewind
     */
    override fun rewind_event() {
        counts = listOf()
        counts_since_gps = hashMapOf()
    }

    fun message_arrived(m : DFMessage) {
        val type = m.get_type()
        if (!counts.contains(type)) {
            counts[type] = 1
        } else {
            counts[type] += 1
        }
        // this preserves existing behaviour -but should we be doing this
        // if type == "GPS"?
        if (!counts_since_gps.contains(type)) {
            counts_since_gps[type] = 1
        } else {
            counts_since_gps[type] = counts_since_gps[type]?.plus(1)!!
        }

        if( type == "GPS" || type == "GPS2") {
            gps_message_arrived(m)
        }
    }

    // adjust time base from GPS message
    fun gps_message_arrived( m: DFMessage) {
        // msec - style GPS message?
        var gps_week : Double = getattr(m, "Week", null)
        var gps_timems : Int = getattr(m, "TimeMS", null)
        if (gps_week == null) {
            // usec - style GPS message?
            gps_week = getattr(m, "GWk", null)
            gps_timems = getattr(m, "GMS", null)
            if (gps_week == null) {
                if (getattr(m, "GPSTime", null) != null ) {
                    // PX4 - style timestamp; we've only been called
                    // because we were speculatively created in case no
                    // better clock was found .
                    return
                }
            }
        }

        if (gps_week == null && hasattr(m, "Wk")) {
            // AvA - style logs
            gps_week = getattr(m, "Wk")
            gps_timems = getattr(m, "TWk")
            if (gps_week == null || gps_timems == null)
                return
        }

        val t = _gpsTimeToTime(gps_week, gps_timems)

        val deltat = t - timebase
        if (deltat <= 0)
            return

        for (type in counts_since_gps) {
            var rate = counts_since_gps[type] / deltat
            if (rate > msg_rate.get(type, 0)!!) {
                msg_rate[type] = rate
            }
        }
        msg_rate["IMU"] = 50.0
        timebase = t
        counts_since_gps = {}


    }

    fun set_message_timestamp( m: DFMessage) {
        var rate = msg_rate[m.fmt.name] ?: 50
        if (rate == 0)
            rate = 50

        val count = counts_since_gps[m.fmt.name] ?: 0
        m._timestamp = (timebase + count / rate).toLong()
    }
}
