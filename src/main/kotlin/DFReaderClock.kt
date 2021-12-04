
/**
 * DFReaderClock
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
abstract class DFReaderClock {

    var timebase : Double = 0.0
    var timestamp : Double = 0.0

    /**
     * convert GPS week and TOW to a time in seconds since 1970
     */
    fun gpsTimeToTime(week : Float, mSec: Float) : Double {
        val epoch = 86400 * (10 * 365 + ((1980 - 1969) / 4) + 1 + 6 - 2)
        return epoch + 86400 * 7 * week + mSec * 0.001 - 18
    }

    open fun messageArrived(m : DFMessage) {
//        pass
    }

    open fun rewindEvent() {
//        pass
    }

    abstract fun setMessageTimestamp(m : DFMessage)
}