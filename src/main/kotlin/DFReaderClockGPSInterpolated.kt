/**
 * DFReaderClockGPSInterpolated
 * Copyright (C) 2021 Hitec Commercial Solutions
 * @author Stephen Woerner
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
 * DFReaderClockGPSInterpolated - for when the only real references in a message are GPS timestamps
 */
class DFReaderClockGPSInterpolated : DFReaderClock() {

    private var msgRate = hashMapOf<String, Int>()
    private var counts = listOf<String>()
    private var countsSinceGPS = hashMapOf<String, Int>()


    /**
     * Reset counters on rewind
     */
    override fun rewindEvent() {
        counts = listOf()
        countsSinceGPS = hashMapOf()
    }

    /*fun message_arrived(m : DFMessage) {
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
    }*/

    /**
     * Adjust time base from GPS message
     */
    fun gpsMessageArrived(m: DFMessage) {
        // msec - style GPS message?
        var gpsWeek : Double? = m.getAttr( "Week", null).first as Double?
        var gpsTimeMS : Int? =  m.getAttr( "TimeMS", null).first as Int?
        if (gpsWeek == null) {
            // usec - style GPS message?
            gpsWeek =  m.getAttr("GWk", null).first as Double?
            gpsTimeMS =  m.getAttr("GMS", null).first as Int?
            if (gpsWeek == null) {
                if (m.getAttr("GPSTime", null).first != null ) {
                    // PX4 - style timestamp; we've only been called
                    // because we were speculatively created in case no
                    // better clock was found .
                    return
                }
            }
        }

        if (gpsWeek == null && m.hasAttr( "Wk")) {
            // AvA - style logs
            gpsWeek = m.getAttr( "Wk").first as Double?
            gpsTimeMS = m.getAttr( "TWk").first as Int?
            if (gpsWeek == null || gpsTimeMS == null)
                return
        }

        val t = gpsTimeToTime(gpsWeek!!.toFloat(), gpsTimeMS!!.toFloat())

        val deltaT = t - timebase
        if (deltaT <= 0)
            return

        for (type in countsSinceGPS) {
            val rate = countsSinceGPS[type.key]!! / deltaT
            if (rate > (msgRate[type.key] ?: 0)) {
                msgRate[type.key] = rate.toInt()
            }
        }
        msgRate["IMU"] = 50
        timebase = t
        countsSinceGPS = hashMapOf()


    }

    override fun setMessageTimestamp(m: DFMessage) {
        var rate = msgRate[m.fmt.name] ?: 50
        if (rate == 0)
            rate = 50

        val count = countsSinceGPS[m.fmt.name] ?: 0
        m.timestamp = (timebase + count / rate).toLong()
    }
}
