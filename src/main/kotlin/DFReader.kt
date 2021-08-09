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
abstract class DFReader() {
    var clock : DFReaderClock? = null
    var timestamp : Int = 0
    var mav_type = MAV_TYPE.MAV_TYPE_FIXED_WING
    var verbose = false
    var params = HashMap<String, Float>()
    var _flightmodes : Array<Array<Any?>>? = null
    var messages = hashMapOf<String, Any>() //can be DFReader or DFMessage
    var _zero_time_base = false
    var flightmode : Any? = null
    var percent : Float = 0f

    abstract fun _parse_next() : DFMessage?

    open fun _rewind() {
        // be careful not to replace self.messages with a new hash;
        // some people have taken a reference to self.messages and we
        // need their messages to disappear to . If they want their own
        // copy they can copy.copy it!
        messages.clear()
        messages["MAV"] = this
        if (_flightmodes != null && _flightmodes!!.isNotEmpty()) {
            flightmode = _flightmodes!![0][0]
        } else {
            flightmode = "UNKNOWN"
        }
        percent = 0f
        clock?.rewind_event()
    }

    fun init_clock_px4 ( px4_msg_time: DFMessage, px4_msg_gps : DFMessage) : Boolean {
        clock = DFReaderClock_px4()
        if ( !_zero_time_base ) {
            (clock as DFReaderClock_px4).set_px4_timebase(px4_msg_time)
            (clock as DFReaderClock_px4).find_time_base(px4_msg_gps)
        }
        return true
    }

    fun init_clock_msec() {
        // it is a new style flash log with full timestamps
        clock = DFReaderClock_msec()
    }

    fun init_clock_usec() {
        clock = DFReaderClock_usec()
    }

    fun init_clock_gps_interpolated( clock: DFReaderClock) {
        this.clock = clock
    }

    /**
     * work out time basis for the log
     */
    fun init_clock() {
        println("init_clock")
        _rewind()

        // speculatively create a gps clock in case we don't find anything better
        val gps_clock = DFReaderClock_gps_interpolated()
        clock = gps_clock

        var px4_msg_time : DFMessage? = null
        var px4_msg_gps : DFMessage? = null
        var gps_interp_msg_gps1 : DFMessage? = null
        var first_us_stamp : Int? = null
        var first_ms_stamp : Float? = null//guessed type

        var have_good_clock = false
        var count = 0
        while (true) {
            count++
            val m = recv_msg()
            if (m == null) {
                break
            }

            val type = m.get_type()

            if (first_us_stamp == null) {
                first_us_stamp =  m.TimeUS
            }

            if (first_ms_stamp == null && (type != "GPS" && type != "GPS2")) {
                // Older GPS messages use TimeMS for msecs past start of gps week
                first_ms_stamp = m.TimeMS
            }

            if (type == "GPS" || type == "GPS2") {
                if (m.TimeUS != 0 && m.GWk != 0f) {//   everything-usec-timestamped
                    init_clock_usec()
                    if (!_zero_time_base ) {
                        (clock as DFReaderClock_usec).find_time_base(m, first_us_stamp!!)
                    }
                    have_good_clock = true
                    break
                }
                if ( m.T != 0 && m.__getattr__("Week", 0).first as Int != 0) {//   GPS is msec-timestamped
                    if (first_ms_stamp == null) {
                        first_ms_stamp = m.__getattr__( "T", 0).first as Float
                    }
                    init_clock_msec()
                    if (!_zero_time_base) {
                        (clock as DFReaderClock_msec).find_time_base(m, first_ms_stamp)
                    }
                    have_good_clock = true
                    break
                }
                if (m.GPSTime != 0f) {  // px4-style-only
                    px4_msg_gps = m
                }
                if (m.Week != 0f) {
                    if (gps_interp_msg_gps1 != null && (gps_interp_msg_gps1.TimeMS != m.TimeMS || gps_interp_msg_gps1.Week != m.Week)) {
//                      we've received two distinct, non-zero GPS
//                      packets without finding a decent clock to
//                      use; fall back to interpolation . Q : should
//                      we wait a few more messages befoe doing
//                      this?
                        this.init_clock_gps_interpolated(gps_clock)
                        have_good_clock = true
                        break
                    }
                    gps_interp_msg_gps1 = m
                }

            } else if (type == "TIME") {
//                only px4-style logs use TIME
                if (m.StartTime != null)
                    px4_msg_time = m
            }

            if (px4_msg_time != null && px4_msg_gps != null) {
                init_clock_px4(px4_msg_time, px4_msg_gps)
                have_good_clock = true
                break
            }
        }

        //        print("clock is " + str(this.clock))
        if (!have_good_clock) {
//             we failed to find any GPS messages to set a time
//             base for usec and msec clocks . Also, not a
//             PX4 - style log
            if (first_us_stamp != null) {
                this.init_clock_usec()
            } else if (first_ms_stamp != null) {
                this.init_clock_msec()
            }
        }
        _rewind()

        return
    }

    /**
     * set time for a message
     */
    fun _set_time(m: DFMessage) {
        // really just left here for profiling
        m._timestamp = timestamp.toLong()
        if (m.fieldnames.isNotEmpty() && this.clock != null)
            clock!!.set_message_timestamp(m)
    }

    fun recv_msg(): DFMessage? {
        return _parse_next()
    }

    /**
     * add a new message
     */
    fun _add_msg( m: DFMessage) {
        val type = m.get_type()
        this.messages[type] = m
        if (m.fmt.instance_field != null) {
            val i = m.__getattr__(m.fmt.instance_field!!).first
            this.messages[String.format("%s[%s]",type, i)] = m
        }

        clock?.message_arrived(m)


        if (type == "MSG" && m.Message != null) {
            if (m.Message!!.indexOf("Rover") != -1) {
                this.mav_type = MAV_TYPE.MAV_TYPE_GROUND_ROVER
            } else if (m.Message!!.indexOf("Plane") != -1) {
                this.mav_type = MAV_TYPE.MAV_TYPE_FIXED_WING
            } else if (m.Message!!.indexOf("Copter") != -1) {
                this.mav_type = MAV_TYPE.MAV_TYPE_QUADROTOR
            } else if (m.Message!!.startsWith("Antenna")) {
                this.mav_type = MAV_TYPE.MAV_TYPE_ANTENNA_TRACKER
            } else if (m.Message!!.indexOf("ArduSub") != -1) {
                this.mav_type = MAV_TYPE.MAV_TYPE_SUBMARINE
            }
        }
        if (type == "MODE") {
            if (m.Mode != null && m.Mode is String) {
                this.flightmode = (m.Mode as String).uppercase()
            } else if ( m.fieldnames.contains("ModeNum")) {
                val mapping = Util.mode_mapping_bynumber(this.mav_type)
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
        this._set_time(m)
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
}