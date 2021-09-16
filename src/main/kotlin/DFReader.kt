/*
 * DFReader
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
 * parse a generic dataflash file
 */
abstract class DFReader {
    var clock : DFReaderClock? = null
    var timestamp : Int = 0
    var mavType = MAV_TYPE.MAV_TYPE_FIXED_WING
    var verbose = false
    var params = HashMap<String, Float>()
    var flightmodes : Array<Array<Any?>>? = null
    var messages = hashMapOf<String, Any>() //can be DFReader or DFMessage
    var zeroTimeBase = false
    var flightmode : Any? = null
    var percent : Float = 0f

    val startTime: Long
        get() { return clock?.timebase?.toLong() ?: 0L }

    var endTime: Long = 0L

    abstract fun parseNext() : DFMessage?

    open fun rewind() {
        // be careful not to replace self.messages with a new hash;
        // some people have taken a reference to self.messages and we
        // need their messages to disappear to . If they want their own
        // copy they can copy.copy it!
        messages.clear()
        messages["MAV"] = this
        if (flightmodes != null && flightmodes!!.isNotEmpty()) {
            flightmode = flightmodes!![0][0]
        } else {
            flightmode = "UNKNOWN"
        }
        percent = 0f
        clock?.rewindEvent()
    }

    fun initClockPx4 (px4_msg_time: DFMessage, px4_msg_gps : DFMessage) : Boolean {
        clock = DFReaderClockPx4()
        if ( !zeroTimeBase ) {
            (clock as DFReaderClockPx4).setPx4Timebase(px4_msg_time)
            (clock as DFReaderClockPx4).findTimeBase(px4_msg_gps)
        }
        return true
    }

    fun initClockMsec() {
        // it is a new style flash log with full timestamps
        clock = DFReaderClockMSec()
    }

    fun initClockUsec() {
        clock = DFReaderClockUSec()
    }

    fun initClockGpsInterpolated(clock: DFReaderClock) {
        this.clock = clock
    }

    /**
     * work out time basis for the log
     */
    fun initClock() {
        println("init_clock")
        rewind()

        // speculatively create a gps clock in case we don't find anything better
        val gpsClock = DFReaderClockGPSInterpolated()
        clock = gpsClock

        var px4MsgTime : DFMessage? = null
        var px4MsgGps : DFMessage? = null
        var gpsInterpMsgGps1 : DFMessage? = null
        var firstUsStamp : Int? = null
        var firstMsStamp : Float? = null//guessed type

        var haveGoodClock = false
        var count = 0
        while (true) {
            count++
            val m = recvMsg() ?: break

            val type = m.getType()

            if (firstUsStamp == null) {
                firstUsStamp =  m.TimeUS
            }

            if (firstMsStamp == null && (type != "GPS" && type != "GPS2")) {
                // Older GPS messages use TimeMS for msecs past start of gps week
                firstMsStamp = m.TimeMS
            }

            if (type == "GPS" || type == "GPS2") {
                if (m.TimeUS != 0 && m.GWk != 0f) {//   everything-usec-timestamped
                    initClockUsec()
                    if (!zeroTimeBase ) {
                        (clock as DFReaderClockUSec).findTimeBase(m, firstUsStamp!!)
                    }
                    haveGoodClock = true
                    break
                }
                if ( m.T != 0 && m.getAttr("Week", 0).first as Int != 0) {//   GPS is msec-timestamped
                    if (firstMsStamp == null) {
                        firstMsStamp = m.getAttr( "T", 0).first as Float
                    }
                    initClockMsec()
                    if (!zeroTimeBase) {
                        (clock as DFReaderClockMSec).findTimeBase(m, firstMsStamp)
                    }
                    haveGoodClock = true
                    break
                }
                if (m.GPSTime != 0f) {  // px4-style-only
                    px4MsgGps = m
                }
                if (m.Week != 0f) {
                    if (gpsInterpMsgGps1 != null && (gpsInterpMsgGps1.TimeMS != m.TimeMS || gpsInterpMsgGps1.Week != m.Week)) {
//                      we've received two distinct, non-zero GPS
//                      packets without finding a decent clock to
//                      use; fall back to interpolation . Q : should
//                      we wait a few more messages befoe doing
//                      this?
                        this.initClockGpsInterpolated(gpsClock)
                        haveGoodClock = true
                        break
                    }
                    gpsInterpMsgGps1 = m
                }

            } else if (type == "TIME") {
//                only px4-style logs use TIME
                if (m.StartTime != null)
                    px4MsgTime = m
            }

            if (px4MsgTime != null && px4MsgGps != null) {
                initClockPx4(px4MsgTime, px4MsgGps)
                haveGoodClock = true
                break
            }
        }

        //        print("clock is " + str(this.clock))
        if (!haveGoodClock) {
//             we failed to find any GPS messages to set a time
//             base for usec and msec clocks . Also, not a
//             PX4 - style log
            if (firstUsStamp != null) {
                this.initClockUsec()
            } else if (firstMsStamp != null) {
                this.initClockMsec()
            }
        }
        rewind()

        return
    }

    /**
     * set time for a message
     */
    fun setTime(m: DFMessage) {
        // really just left here for profiling
        m.timestamp = timestamp.toLong()
        if (m.fieldnames.isNotEmpty() && this.clock != null)
            clock!!.setMessageTimestamp(m)
    }

    fun recvMsg(): DFMessage? {
        return parseNext()
    }

    /**
     * add a new message
     */
    fun addMsg(m: DFMessage) {
        val type = m.getType()
        this.messages[type] = m
        if (m.fmt.instanceField != null) {
            val i = m.getAttr(m.fmt.instanceField!!).first
            this.messages[String.format("%s[%s]",type, i)] = m
        }

        clock?.messageArrived(m)


        if (type == "MSG" && m.Message != null) {
            if (m.Message!!.indexOf("Rover") != -1) {
                this.mavType = MAV_TYPE.MAV_TYPE_GROUND_ROVER
            } else if (m.Message!!.indexOf("Plane") != -1) {
                this.mavType = MAV_TYPE.MAV_TYPE_FIXED_WING
            } else if (m.Message!!.indexOf("Copter") != -1) {
                this.mavType = MAV_TYPE.MAV_TYPE_QUADROTOR
            } else if (m.Message!!.startsWith("Antenna")) {
                this.mavType = MAV_TYPE.MAV_TYPE_ANTENNA_TRACKER
            } else if (m.Message!!.indexOf("ArduSub") != -1) {
                this.mavType = MAV_TYPE.MAV_TYPE_SUBMARINE
            }
        }
        if (type == "MODE") {
            if (m.Mode != null && m.Mode is String) {
                this.flightmode = (m.Mode as String).uppercase()
            } else if ( m.fieldnames.contains("ModeNum")) {
                val mapping = Util.mode_mapping_bynumber(this.mavType)
                if (mapping != null && mapping.contains(m.ModeNum)) {
                    this.flightmode = mapping[m.ModeNum]
                } else {
                    this.flightmode = "UNKNOWN"
                }
            } else if ( m.Mode != null) {
                this.flightmode = Util.mode_string_acm(m.Mode!!.toInt())
            }


        }
        if (type == "STAT" && m.fieldnames.contains("MainState")) {
            this.flightmode = Util.mode_string_px4(m.MainState!!)
        }
        if (type == "PARM" && m.Name != null) {
            this.params[m.Name!!] = m.Value!!
        }
        this.setTime(m)
    }

    /**
     * recv the next message that matches the given condition
     * type can be a string or a list of strings
     */
//    fun recv_match( condition=null, type=null, blocking=false) : DFMessage? {
//        var typeAsSet : HashSet<List<String>>? = null
//        if (type != null) {
//            if (type is String) {
//                typeAsSet = hashSetOf(listOf(type))
//            } else if (type is List<Any>) {
//                typeAsSet = hashSetOf(type)
//            }
//        }
//        while (true) {
////            if ( typeAsSet != null)
////                skip_to_type(typeAsSet) todo
//            val m = this.recv_msg()
//            if (m == null) {
//                return null
//            }
//            if (typeAsSet != null && !typeAsSet.contains(m.get_type())) {
//                continue
//            }
//            if (!Util.evaluate_condition(condition, this.messages)) {
//                continue
//            }
//            return m
//        }
//    }
    /**
     * check if a condition is true
     */
//    fun check_condition( condition) : Boolean {
//        return Util.evaluate_condition(condition, this.messages)
//    }

    /**
     * convenient function for returning an arbitrary MAVLink
     * parameter with a default
     */
    fun param( name: String, default : Any? = null) : Any? {
        if (!this.params.contains(name)) {
            return default
        }
        return this.params[name]
    }

    /**
     * return an array of tuples for all flightmodes in log. Tuple is (modestring, t0, t1)
     */
    fun flightmode_list() {
//        var tstamp: Long?= null
//        val fmode : Any? = null //unknown type
//        if (_flightmodes == null) {
//            this._rewind()
//            this._flightmodes = arrayOf()
//            val types = hashSetOf(listOf("MODE"))
//            while(true) {
//                val m = this.recv_match(type = types)
//                if (m == null)
//                    break
//                tstamp = m._timestamp
//                if (this.flightmode == fmode)
//                    continue
//                if (this._flightmodes!!.size > 0) {
//                    (mode, t0, t1) = this._flightmodes[-1]
//                    this._flightmodes[-1] = (mode, t0, tstamp)
//                }
//                this._flightmodes.add((this.flightmode, tstamp, null))
//                fmode = this.flightmode
//            }
//            if (tstamp != null) {
//                (mode, t0, t1) = this._flightmodes[-1]
//                this._flightmodes[-1] = (mode, t0, this.last_timestamp())
//            }
//        }
//        this._rewind()
//        return this._flightmodes
    }

    fun u_ord(c : Byte) : Int {
        return c.toInt()
    }

}