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
    var timestamp = 0L
    var mav_type = MAV_TYPE.MAV_TYPE_FIXED_WING
    var verbose = false
    var params = {}
    var _flightmodes : Array<Array<Any?>>? = null
    var messages = hashMapOf<String, Any>() //can be DFReader or DFMessage
    var _zero_time_base = false
    var flightmode : Any? = null
    var percent : Float = 0f

    abstract fun _parse_next() : DFMessage?

    fun _rewind() {
        // be careful not to replace self.messages with a new hash;
        // some people have taken a reference to self.messages and we
        // need their messages to disappear to . If they want their own
        // copy they can copy.copy it!
        messages.clear()
        messages["MAV"] = this
        if (_flightmodes != null && _flightmodes!!.size > 0) {
            flightmode = _flightmodes!![0][0]
        } else {
            flightmode = "UNKNOWN"
        }
        percent = 0f
        clock?.rewind_event()
    }

    fun init_clock_px4 ( px4_msg_time: Int, px4_msg_gps) : Boolean {
        clock = DFReaderClock_px4()
        if ( !_zero_time_base ) {
            clock!!.set_px4_timebase(px4_msg_time)
            clock!!.find_time_base(px4_msg_gps)
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

        _rewind()

        // speculatively create a gps clock in case we don't find anything better
        val gps_clock = DFReaderClock_gps_interpolated()
        clock = gps_clock

        var px4_msg_time = null
        var px4_msg_gps = null
        var gps_interp_msg_gps1 = null
        var first_us_stamp = null
        var first_ms_stamp = null

        var have_good_clock = false
        while (true) {
            val m = recv_msg()
            if (m == null) {
                break
            }

            val type = m.get_type()

            if (first_us_stamp == null) {
                first_us_stamp = getattr(m, "TimeUS", null)
            }

            if (first_ms_stamp == null && (type != "GPS" && type != "GPS2")) {
                // Older GPS messages use TimeMS for msecs past start of gps week
                first_ms_stamp = getattr(m, "TimeMS", null)
            }

            if (type == "GPS" || type == "GPS2") {
                if (getattr(m, "TimeUS", 0) != 0 and getattr(m, "GWk", 0) != 0) {//   everything-usec-timestamped
                    init_clock_usec()
                    if (!_zero_time_base ) {
                        self.clock.find_time_base(m, first_us_stamp)
                    }
                    have_good_clock = true
                    break
                } if ( getattr(m, "T", 0) != 0 and getattr(m, "Week", 0) != 0) {//   GPS is msec-timestamped
                    if (first_ms_stamp == null) {
                        first_ms_stamp = m.T
                    }
                    init_clock_msec()
                    if (!_zero_time_base) {
                        clock.find_time_base(m, first_ms_stamp)
                    }
                    have_good_clock = true
                    break
                }
                if (getattr(m, "GPSTime", 0) != 0) {  // px4-style-only
                    px4_msg_gps = m
                }
                if (getattr(m, "Week", 0) != 0) {
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
                if (getattr(m, "StartTime", null) != null)
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
        m._timestamp = this.timestamp
        if (m._fieldnames.size > 0 && this.clock != null)
            this.clock.set_message_timestamp(m)
    }

    fun recv_msg(): DFMessage? {
        return _parse_next()
    }

    /**
     * add a new message
     */
    fun _add_msg( m) {
        type = m.get_type()
        this.messages[type] = m
        if (m.fmt.instance_field != null) {
            i = m.__getattr__(m.fmt.instance_field)
            this.messages[String.format("%s[%s]",type, str(i)))] = m
        }

        clock?.message_arrived(m)


        if (type == "MSG" && hasattr(m,"Message")) {
            if (m.Message.find("Rover") != -1) {
                this.mav_type = mavutil.mavlink.MAV_TYPE_GROUND_ROVER
            } else if (m.Message.find("Plane") != -1) {
                this.mav_type = mavutil.mavlink.MAV_TYPE_FIXED_WING
            } else if (m.Message.find("Copter") != -1) {
                this.mav_type = mavutil.mavlink.MAV_TYPE_QUADROTOR
            } else if (m.Message.startswith("Antenna")) {
                this.mav_type = mavutil.mavlink.MAV_TYPE_ANTENNA_TRACKER
            } else if (m.Message.find("ArduSub") != -1) {
                this.mav_type = mavutil.mavlink.MAV_TYPE_SUBMARINE
            }
        }
        if (type == "MODE") {
            if (hasattr(m,"Mode") && isinstance(m.Mode, str)) {
                this.flightmode = m.Mode.upper()
            } else if ( "ModeNum" in m._fieldnames) {
                mapping = mavutil.mode_mapping_bynumber(this.mav_type)
                if (mapping != null && m.ModeNum in mapping) {
                    this.flightmode = mapping[m.ModeNum]
                } else {
                    this.flightmode = "UNKNOWN"
                }
            } else if ( hasattr(m,"Mode")) {
                this.flightmode = mavutil.mode_string_acm(m.Mode)
            }


        }
        if (type == "STAT" && "MainState" in m._fieldnames) {
            this.flightmode = mavutil.mode_string_px4(m.MainState)
        }
        if (type == "PARM" && getattr(m, "Name", null) != null) {
            this.params[m.Name] = m.Value
        }
        this._set_time(m)
    }

    /**
     * recv the next message that matches the given condition
     * type can be a string or a list of strings
     */
    fun recv_match( condition=null, type=null, blocking=false) : Any {
        if (type != null) {
            if (isinstance(type, str)) {
                type = set([type])
            } else if ( isinstance(type, list)) {
                type = set(type)
            }
        }
        while (true) {
            if ( type != null)
                this.skip_to_type(type)
            m = this.recv_msg()
            if (m == null) {
                return null
            }
            if (type != null && ! m.get_type() in type) {
                continue
            }
            if (!mavutil.evaluate_condition(condition, this.messages)) {
                continue
            }
            return m
        }
    }
    /**
     * check if a condition is true
     */
    fun check_condition( condition) : Boolean {
        return Util.evaluate_condition(condition, this.messages)
    }

    /**
     * convenient function for returning an arbitrary MAVLink
    parameter with a default
     */
    fun param( name, default=null) {
        if (name not in this.params) {
            return default
        }
        return this.params[name]
    }

    /**
     * return an array of tuples for all flightmodes in log. Tuple is (modestring, t0, t1)
     */
    fun flightmode_list() {
        val tstamp = null
        val fmode = null
        if (_flightmodes == null) {
            this._rewind()
            this._flightmodes = []
            types = set(["MODE"])
            while(true) {
                m = this.recv_match(type = types)
                if (m == null)
                    break
                tstamp = m._timestamp
                if (this.flightmode == fmode)
                    continue
                if (len(this._flightmodes) > 0) {
                    (mode, t0, t1) = this._flightmodes[-1]
                    this._flightmodes[-1] = (mode, t0, tstamp)
                }
                this._flightmodes.append((this.flightmode, tstamp, null))
                fmode = this.flightmode
            }
            if (tstamp != null) {
                (mode, t0, t1) = this._flightmodes[-1]
                this._flightmodes[-1] = (mode, t0, this.last_timestamp())
            }
        }
        this._rewind()
        return this._flightmodes
    }
}