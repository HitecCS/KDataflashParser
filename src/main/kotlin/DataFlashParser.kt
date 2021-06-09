import java.lang.reflect.Type
import kotlin.reflect.KClass

/*
 * KDataFlashParser
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


data class StructContainer(val s: String, val mul: Double?, val type: KClass<out Any>) //TODO change name

val formatToStruct = hashMapOf(
    'a' to StructContainer("64s", null, String::class),
    'b' to StructContainer("b", null, Int::class),
    'B' to StructContainer("B", null, Int::class),
    'h' to StructContainer("h", null, Int::class),
    'H' to StructContainer("H", null, Int::class),
    'i' to StructContainer("i", null, Int::class),
    'I' to StructContainer("I", null, Int::class),
    'f' to StructContainer("f", null, Float::class),
    'n' to StructContainer("4s", null, String::class),
    'N' to StructContainer("16s", null, String::class),
    'Z' to StructContainer("64s", null, String::class),
    'c' to StructContainer("h", 0.01, Float::class),
    'C' to StructContainer("H", 0.01, Float::class),
    'e' to StructContainer("i", 0.01, Float::class),
    'E' to StructContainer("I", 0.01, Float::class),
    'L' to StructContainer("i", 1.0e-7, Float::class),
    'd' to StructContainer("d", null, Float::class),
    'M' to StructContainer("b", null, Int::class),
    'q' to StructContainer("q", null, Long::class),
    'Q' to StructContainer("Q", null, Long::class),
)

class DFFormat(
    val type: String,
    val name: String,
    val flen: String,
    val format: String,
    val columns: String,
    val oldfmt: DFFormat? = null
) {
    var instance_field: String? = null
    var unit_ids: String? = null
    var mult_ids: String? = null
    var columnsArr = listOf<String>()
    var colhash = hashMapOf<String, Int>()

    init {
        columnsArr = columns.split(',')
        instance_field = null
        unit_ids = null
        mult_ids = null

        if (columnsArr.size == 1 && columnsArr[0] == "")
            columnsArr = emptyList()

        var msg_struct = "<"
        val msg_mults = arrayListOf<Double?>()
        val msg_types = arrayListOf<KClass<out Any>>()
        val msg_fmts = arrayListOf<Char>()

        for (c in format) {
//            if u_ord(c) == 0:
//                return @loop
            try {
                msg_fmts.add(c)
                val structContainer = formatToStruct[c]
                msg_struct += structContainer!!.s
                msg_mults.add(structContainer.mul)
                if (c == 'a')
                    msg_types.add(Array<out Any>::class)
                else
                    msg_types.add(structContainer.type)
            } catch (e: Throwable) {
                val msg = "Unsupported format char: '$c' in message $name"
                println("DFFormat: $msg")
                throw Exception(msg)
            }
        }

        for (i in 0..columnsArr.size) {
            colhash[columnsArr[i]] = i
        }

        var a_indexes = arrayListOf<Int>()
        for (i in 0..msg_fmts.size) {
            if (msg_fmts[i] == 'a') {
                a_indexes.add(i)
            }
        }

        if (oldfmt != null) {
            set_unit_ids(oldfmt.unit_ids)
            set_mult_ids(oldfmt.mult_ids)
        }
    }

    /**
     * set unit IDs string from FMTU
     */
    fun set_unit_ids(unit_ids: String?) {
        unit_ids ?: return

        this.unit_ids = unit_ids
        val instance_idx = unit_ids.indexOf('#')
        if (instance_idx != -1)
            instance_field = columnsArr[instance_idx]
    }

    /**
     * set mult IDs string from FMTU
     */
    fun set_mult_ids(mult_ids: String?) {
        this.mult_ids = mult_ids
    }

    override fun toString(): String {
        return String.format("DFFormat(%s,%s,%s,%s)", type, name, format, columns)
    }
}

//// TODO Swiped into mavgen_python.py
///**
// * desperate attempt to convert a string regardless of what garbage we get
// */
//fun to_string(s : String): String {
//    var str = s
//    try {
//        return str.decode("utf-8")
//    } catch (e: Exception) {
//        // Do nothing
//    }
//
//    try {
//        val s2 = str.encode("utf-8", "ignore")
//        val x = u"%s" % s2
//        return s2
//    } catch (e: Exception) {
//        // Do nothing
//    }
//
//    // so its a nasty one.Let's grab as many characters as we can
//    var r = ""
//    while (str != "") {
//        try {
//            var r2 = r + str[0]
//            str = str.substring(1)
//            r2 = r2.encode("ascii", "ignore")
//            var x = u"%s" % r2
//            r = r2
//        } catch (e : Exception) {
//            break
//        }
//    }
//    return r + "_XXX"
//}

/* // Uncomment for a good time ;-)
class DFMessage(val fmt: DFFormat, val elements: Array<Any>, val apply_multiplier : String, var parent: DFMessage?) {
    var fieldnames : List<String>
    init {
        fieldnames = fmt.columnsArr
    }

    fun to_dict() : HashMap<String, String>{
        var d = hashMapOf ( "mavpackettype" to fmt.name )

        for(field in fieldnames)
            d[field] = getAttr(field)

        return d
    }

    /**
     * override field getter
     */
    fun getAttr(field: String) {
        var i = 0
        try {
            i = fmt.colhash[field]!!
        } catch (e: Exception) {
            throw java.lang.Exception(field)
        }

        var v = 0
        if (elements[i] is ByteArray) {
            v = elements[i].decode("utf-8")
        } else {
            v = elements[i]
        }
        if (fmt.format[i] == 'a') {
            pass
        } else if (fmt.format[i] != 'M' || self._apply_multiplier) {
            v = fmt.msg_types[i](v)
        }
        if (fmt.msg_types[i] == str) {
            v = null_term(v)
        }
        if (fmt.msg_mults[i] != null && _apply_multiplier) {
            v *= fmt.msg_mults[i]
        }
        return v
    }


    /**
     * override field setter
     */
    fun setAttr( field, v) {
        if (not field [0].isupper() or not field in self . fmt . colhash) {
            super(DFMessage, self).__setattr__(field, v)
        } else {
            i = self.fmt.colhash[field]
            if (self.fmt.msg_mults[i] != null and self . _apply_multiplier) {
                v /= self.fmt.msg_mults[i]
            }
            self._elements[i] = v
        }
    }


    fun get_type() {
        return fmt.name
    }

    fun __str__() {
        var ret = String.format("%s {" , fmt.name)
        var col_count = 0
        for (c in fmt.columns) {
            var v = self.__getattr__(c)
            if(isinstance(v, float) and math.isnan(v )) {
//                quiet nans have more non - zero values :
                noisy_nan = "\x7f\xf8\x00\x00\x00\x00\x00\x00"
                if (struct.pack(">d", v) != noisy_nan) {
                    v = "qnan"
                }
            }
            ret += "%s : %s, " % (c, v )
            col_count += 1
        }
        if(col_count != 0)
            ret = ret[:-2]

        return ret + '}'
    }

    /**
     * create a binary message buffer for a message
     */
    fun get_msgbuf() {
        values = []
        is_py2 = sys.version_info < (3, 0)
        for (i in range(len(self.fmt.columns))) {
            if (i >= len(self.fmt.msg_mults)) {
                continue
            }
            mul = self.fmt.msg_mults[i]
            name = self.fmt.columns[i]
            if (name == "Mode" and "ModeNum" in self.fmt.columns) {
                name = "ModeNum"
            }
            v = self.__getattr__(name)
            if (is_py2) {
                if (isinstance(v, unicode)) {// NOQA
                    v = str(v)
                }
            } else {
                if isinstance(v, str) {
                    v = bytes(v, "ascii")
                }
            }
            if (isinstance(v, array.array)) {
                v = v.tostring()
            }
            if (mul != null) {
                v /= mul
                v = int(round(v))
            }
            values.append(v)
        }

        ret1 = struct.pack("BBB", 0xA3, 0x95, self.fmt.type)
        try {
            ret2 = struct.pack(self.fmt.msg_struct, *values)
        } catch (e: Exception) {
            return null
        }
        return ret1 + ret2
    }


    fun get_fieldnames() {
        return _fieldnames
    }

    /**
     * support indexing, allowing for multi-instance sensors in one message
     */
    fun __getitem__(key) {
        if (fmt.instance_field == null) {
//            raise IndexError ()
            throw java.lang.Exception("IndexError")
        }
        val k = String.format("%s[%s]", fmt.name, str(key))
        if (not k in self . _parent . messages ) {
            throw java.lang.Exception("IndexError")
        }
        return _parent.messages[k]
    }
}

/**
 * base class for all the different ways we count time in logs
 */
open class DFReaderClock() {

    var timebase : Int = 0
    var timestamp : Int
    init {
        set_timebase(0)
        timestamp = 0
    }

    /**
     * convert GPS week and TOW to a time in seconds since 1970
     */
    fun _gpsTimeToTime( week : Double, msec: Int) : Double {
        val epoch = 86400 * (10 * 365 + ((1980 - 1969) / 4) + 1 + 6 - 2)
        return epoch + 86400 * 7 * week + msec * 0.001 - 18
    }

    fun set_timebase( base : Int) {
        timebase = base
    }

    fun message_arrived( m : String) {
//        pass
    }

    fun rewind_event() {
//        pass
    }
}

/**
 * DFReaderClock_usec - use microsecond timestamps from messages
 */
class DFReaderClock_usec(): DFReaderClock() {

    /**
     * work out time basis for the log - even newer style
     */
    fun find_time_base( gps, first_us_stamp) {
        val t = _gpsTimeToTime(gps.GWk, gps.GMS)
        set_timebase(t - gps.TimeUS * 0.000001)
//         this ensures FMT messages get appropriate timestamp:
        timestamp = timebase + first_us_stamp * 0.000001
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

    fun should_use_msec_field0( m: String) : Boolean {
        if (type_has_good_TimeMS (m.get_type())) {
            return false
        }
        if ("TimeMS" != m._fieldnames[0]) {
            return false
        }
        if (timebase + m.TimeMS * 0.001 < timestamp) {
            return false
        }
        return true
    }

    fun set_message_timestamp( m: String) {
        if("TimeUS" == m._fieldnames[0]) {
//             only format messages don 't have a TimeUS in them...
            m._timestamp = self.timebase + m.TimeUS * 0.000001
        } else if(should_use_msec_field0(m)) {
//             ... in theory. I expect there to be some logs which are not
//             "pure":
            m._timestamp = self.timebase + m.TimeMS * 0.001
        } else {
            m._timestamp = self.timestamp
        }
        timestamp = m._timestamp
    }
}

/**
 * DFReaderClock_msec - a format where many messages have TimeMS in
 * their formats, and GPS messages have a "T" field giving msecs
 */
class DFReaderClock_msec() : DFReaderClock() {

    /**
     * work out time basis for the log - new style
     */
    fun find_time_base( gps, first_ms_stamp) {
        val t = ._gpsTimeToTime(gps.Week, gps.TimeMS)
        set_timebase(t - gps.T * 0.001)
        timestamp = timebase + first_ms_stamp * 0.001
    }

    fun set_message_timestamp( m ) {
        if ("TimeMS" == m._fieldnames[0]) {
            m._timestamp = timebase + m.TimeMS * 0.001
        } else if (m . get_type () in ["GPS", "GPS2"]) {
            m._timestamp = timebase + m.T * 0.001
        } else {
            m._timestamp = timestamp
        }
        timestamp = m._timestamp
    }
}

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
    fun find_time_base( gps) {
        val t = gps.GPSTime * 1.0e-6
        timebase = t - px4_timebase
    }

    fun set_px4_timebase( time_msg) {
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


/**
 * DFReaderClock_gps_interpolated - for when the only real references in a message are GPS timestamps
 */
class DFReaderClock_gps_interpolated() : DFReaderClock() {

    var msg_rate : List<String>
    var counts : List<String>
    var counts_since_gps : List<Int>

    init {
        msg_rate = listOf()
        counts = listOf()
        counts_since_gps = listOf()
    }

    /**
     * reset counters on rewind
     */
    override fun rewind_event() {
        counts = listOf()
        counts_since_gps = listOf()
    }

    fun message_arrived( m) {
        var type = m.get_type()
        if (type not in self . counts) {
            counts[type] = 1
        } else {
            counts[type] += 1
        }
        // this preserves existing behaviour -but should we be doing this
        // if type == "GPS"?
        if (type not in self . counts_since_gps) {
            counts_since_gps[type] = 1
        } else {
            counts_since_gps[type] += 1
        }

        if( type == "GPS" || type == "GPS2") {
            gps_message_arrived(m)
        }
    }

    // adjust time base from GPS message
    fun gps_message_arrived( m) {
        // msec - style GPS message?
        var gps_week = getattr(m, "Week", null)
        var gps_timems = getattr(m, "TimeMS", null)
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
            if (rate > msg_rate.get(type, 0)) {
                msg_rate[type] = rate
            }
        }
        msg_rate["IMU"] = 50.0
        timebase = t
        counts_since_gps = {}


    }

    fun set_message_timestamp( m) {
        rate = msg_rate.get(m.fmt.name, 50.0)
        if (int(rate) == 0) {
            rate = 50
        }
        count = counts_since_gps.get(m.fmt.name, 0)
        m._timestamp = timebase + count / rate
    }
}


/**
 * parse a generic dataflash file
 */
open class DFReader() {
    var clock : DFReaderClock? = null
    var timestamp = 0
    var mav_type = mavutil.mavlink.MAV_TYPE_FIXED_WING
    var verbose = false
    var params = {}
    var _flightmodes = null
    var messages = {}

    fun _rewind() {
        // be careful not to replace self . messages with a new hash;
        // some people have taken a reference to self . messages and we
        // need their messages to disappear to . If they want their own
        // copy they can copy . copy it!
        messages.clear()
        messages["MAV"] = this
        if (self._flightmodes != null and len(self._flightmodes) > 0) {
            flightmode = _flightmodes[0][0]
        } else {
            flightmode = "UNKNOWN"
        }
        self.percent = 0
        if (self.clock) {
            self.clock.rewind_event()
        }
    }

    fun init_clock_px4 ( px4_msg_time, px4_msg_gps) : Boolean {
        clock = DFReaderClock_px4()
        if ( not self . _zero_time_base ) {
            clock.set_px4_timebase(px4_msg_time)
            clock.find_time_base(px4_msg_gps)
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
                    if (not self . _zero_time_base ) {
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
    fun _set_time( m) {
        // really just left here for profiling
        m._timestamp = this.timestamp
        if (len(m._fieldnames) > 0 && this.clock != null)
            this.clock.set_message_timestamp(m)
    }

    fun recv_msg(): Any {
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
        if (this.clock) {
            this.clock.message_arrived(m)
        }

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
    fun check_condition( condition) {
        return mavutil.evaluate_condition(condition, this.messages)
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

/**
 * parse a binary dataflash file
 */
class DFReader_binary() : DFReader() {
    init__( filename, zero_time_base=false, progress_callback=null):
    DFReader.__init__(self)
    // read the whole file into memory for simplicity
    this.filehandle = open(filename, 'r')
    this.filehandle.seek(0, 2)
    this.data_len = this.filehandle.tell()
    this.filehandle.seek(0)
    if platform.system() == "Windows":
    this.data_map = mmap.mmap(this.filehandle.fileno(), this.data_len, null, mmap.ACCESS_READ)
    else:
    this.data_map = mmap.mmap(this.filehandle.fileno(), this.data_len, mmap.MAP_PRIVATE, mmap.PROT_READ)

    this.HEAD1 = 0xA3
    this.HEAD2 = 0x95
    this.unpackers = {}
    if sys.version_info.major < 3:
    this.HEAD1 = chr(this.HEAD1)
    this.HEAD2 = chr(this.HEAD2)
    this.formats = {
        0x80: DFFormat(0x80,
        "FMT",
        89,
        "BBnNZ",
        "Type,Length,Name,Format,Columns")
    }
    this._zero_time_base = zero_time_base
    this.prev_type = null
    this.init_clock()
    this.prev_type = null
    this._rewind()
    this.init_arrays(progress_callback)

    /**
     * rewind to start of log
     */
    fun _rewind():
    DFReader._rewind(self)
    this.offset = 0
    this.remaining = this.data_len
    this.type_nums = null
    this.timestamp = 0

    /**
     * rewind to start of log
     */
    fun rewind() {
        this._rewind()
    }

    /**
     * initialise arrays for fast recv_match()
     */
    fun init_arrays( progress_callback=null):
    self.offsets = []
    self.counts = []
    self._count = 0
    self.name_to_id = {}
    self.id_to_name = {}
    for i in range(256):
    self.offsets.append([])
    self.counts.append(0)
    fmt_type = 0x80
    fmtu_type = null
    ofs = 0
    pct = 0
    HEAD1 = self.HEAD1
    HEAD2 = self.HEAD2
    lengths = [-1] * 256

    while ofs+3 < self.data_len:
    hdr = self.data_map[ofs:ofs+3]
    if hdr[0] != HEAD1 or hdr[1] != HEAD2:
    // avoid end of file garbage, 528 bytes has been use consistently throughout this implementation
    // but it needs to be at least 249 bytes which is the block based logging page size (256) less a 6 byte header and
    // one byte of data. Block based logs are sized in pages which means they can have up to 249 bytes of trailing space.
    if self.data_len - ofs >= 528 or self.data_len < 528:
    print("bad header 0x%02x 0x%02x at %d" % (u_ord(hdr[0]), u_ord(hdr[1]), ofs), file=sys.stderr)
    ofs += 1
    continue
    mtype = u_ord(hdr[2])
    self.offsets[mtype].append(ofs)

    if lengths[mtype] == -1:
    if not mtype in self.formats:
    if self.data_len - ofs >= 528 or self.data_len < 528:
    print("unknown msg type 0x%02x (%u) at %d" % (mtype, mtype, ofs),
    file=sys.stderr)
    break
    self.offset = ofs
    self._parse_next()
    fmt = self.formats[mtype]
    lengths[mtype] = fmt.len
    } else if ( self.formats[mtype].instance_field != null:
    self._parse_next()

    self.counts[mtype] += 1
    mlen = lengths[mtype]

    if mtype == fmt_type:
    body = self.data_map[ofs+3:ofs+mlen]
    if len(body)+3 < mlen:
    break
    fmt = self.formats[mtype]
    elements = list(struct.unpack(fmt.msg_struct, body))
    ftype = elements[0]
    mfmt = DFFormat(
    ftype,
    null_term(elements[2]), elements[1],
    null_term(elements[3]), null_term(elements[4]),
    oldfmt=self.formats.get(ftype,null))
    self.formats[ftype] = mfmt
    self.name_to_id[mfmt.name] = mfmt.type
    self.id_to_name[mfmt.type] = mfmt.name
    if mfmt.name == "FMTU":
    fmtu_type = mfmt.type

    if fmtu_type != null && mtype == fmtu_type:
    fmt = self.formats[mtype]
    body = self.data_map[ofs+3:ofs+mlen]
    if len(body)+3 < mlen:
    break
    elements = list(struct.unpack(fmt.msg_struct, body))
    ftype = int(elements[1])
    if ftype in self.formats:
    fmt2 = self.formats[ftype]
    if "UnitIds" in fmt.colhash:
    fmt2.set_unit_ids(null_term(elements[fmt.colhash["UnitIds"]]))
    if "MultIds" in fmt.colhash:
    fmt2.set_mult_ids(null_term(elements[fmt.colhash["MultIds"]]))

    ofs += mlen
    if progress_callback != null:
    new_pct = (100 * ofs) // self.data_len
    if new_pct != pct:
    progress_callback(new_pct)
    pct = new_pct

    for i in range(256):
    self._count += self.counts[i]
    self.offset = 0

    /**
     * get the last timestamp in the log
     */
    fun last_timestamp():
    highest_offset = 0
    second_highest_offset = 0
    for i in range(256):
    if self.counts[i] == -1:
    continue
    if len(self.offsets[i]) == 0:
    continue
    ofs = self.offsets[i][-1]
    if ofs > highest_offset:
    second_highest_offset = highest_offset
    highest_offset = ofs
    } else if ( ofs > second_highest_offset:
    second_highest_offset = ofs
    self.offset = highest_offset
    m = self.recv_msg()
    if m is null:
    self.offset = second_highest_offset
    m = self.recv_msg()
    return m._timestamp

    /**
     * skip fwd to next msg matching given type set
     */
    fun skip_to_type( type):

    if self.type_nums is null:
    // always add some key msg types so we can track flightmode, params etc
    type = type.copy()
    type.update(set(["MODE","MSG","PARM","STAT"]))
    self.indexes = []
    self.type_nums = []
    for t in type:
    if not t in self.name_to_id:
    continue
    self.type_nums.append(self.name_to_id[t])
    self.indexes.append(0)
    smallest_index = -1
    smallest_offset = self.data_len
    for i in range(len(self.type_nums)):
    mtype = self.type_nums[i]
    if self.indexes[i] >= self.counts[mtype]:
    continue
    ofs = self.offsets[mtype][self.indexes[i]]
    if ofs < smallest_offset:
    smallest_offset = ofs
    smallest_index = i
    if smallest_index >= 0:
    self.indexes[smallest_index] += 1
    self.offset = smallest_offset

    /**
     * read one message, returning it as an object
     */
    fun _parse_next():

    // skip over bad messages; after this loop has run msg_type
    // indicates the message which starts at self.offset (including
    // signature bytes and msg_type itself)
    skip_type = null
    skip_start = 0
    while true:
    if self.data_len - self.offset < 3:
    return null

    hdr = self.data_map[self.offset:self.offset+3]
    if hdr[0] == self.HEAD1 && hdr[1] == self.HEAD2:
    // signature found
    if skip_type != null:
    // emit message about skipped bytes
    if self.remaining >= 528:
    // APM logs often contain garbage at end
    skip_bytes = self.offset - skip_start
    print("Skipped %u bad bytes in log at offset %u, type=%s (prev=%s)" %
    (skip_bytes, skip_start, skip_type, self.prev_type),
    file=sys.stderr)
    skip_type = null
    // check we recognise this message type:
    msg_type = u_ord(hdr[2])
    if msg_type in self.formats:
    // recognised message found
    self.prev_type = msg_type
    break;
    // message was not recognised; fall through so these
    // bytes are considered "skipped".  The signature bytes
    // are easily recognisable in the "Skipped bytes"
    // message.
    if skip_type is null:
    skip_type = (u_ord(hdr[0]), u_ord(hdr[1]), u_ord(hdr[2]))
    skip_start = self.offset
    self.offset += 1
    self.remaining -= 1

    self.offset += 3
    self.remaining = self.data_len - self.offset

    fmt = self.formats[msg_type]
    if self.remaining < fmt.len-3:
    // out of data - can often happen half way through a message
    if self.verbose:
    print("out of data", file=sys.stderr)
    return null
    body = self.data_map[self.offset:self.offset+fmt.len-3]
    elements = null
    try:
    if not msg_type in self.unpackers:
    self.unpackers[msg_type] = struct.Struct(fmt.msg_struct).unpack
    elements = list(self.unpackers[msg_type](body))
    except Exception as ex:
    print(ex)
    if self.remaining < 528:
    // we can have garbage at the end of an APM2 log
    return null
    // we should also cope with other corruption; logs
    // transfered via DataFlash_MAVLink may have blocks of 0s
    // in them, for example
    print("Failed to parse %s/%s with len %u (remaining %u)" %
    (fmt.name, fmt.msg_struct, len(body), self.remaining),
    file=sys.stderr)
    if elements is null:
    return self._parse_next()
    name = fmt.name
    // transform elements which can't be done at unpack time:
    for a_index in fmt.a_indexes:
    try:
    elements[a_index] = array.array('h', elements[a_index])
    except Exception as e:
    print("Failed to transform array: %s" % str(e),
    file=sys.stderr)

    if name == "FMT":
    // add to formats
    // name, len, format, headings
    try:
    ftype = elements[0]
    mfmt = DFFormat(
    ftype,
    null_term(elements[2]), elements[1],
    null_term(elements[3]), null_term(elements[4]),
    oldfmt=self.formats.get(ftype,null))
    self.formats[ftype] = mfmt
    except Exception:
    return self._parse_next()

    self.offset += fmt.len - 3
    self.remaining = self.data_len - self.offset
    m = DFMessage(fmt, elements, true, self)

    if m.fmt.name == "FMTU":
    // add to units information
    FmtType = int(elements[0])
    UnitIds = elements[1]
    MultIds = elements[2]
    if FmtType in self.formats:
    fmt = self.formats[FmtType]
    fmt.set_unit_ids(UnitIds)
    fmt.set_mult_ids(MultIds)

    try:
    self._add_msg(m)
    except Exception as ex:
    print("bad msg at offset %u" % self.offset, ex)
    pass
    self.percent = 100.0 * (self.offset / float(self.data_len))

    return m
}

/**
 * return true if a file appears to be a valid text log
 */
fun DFReader_is_text_log(filename) {
    with open (filename, 'r') as f:
        ret = (f.read(8000).find("FMT,") != -1)

    return ret
}

/**
 * parse a text dataflash file
 */
class DFReader_text(DFReader) {
    fun __init__(filename, zero_time_base = false, progress_callback = null) {
                DFReader.__init__()
    // read the whole file into memory for simplicity
        self.filehandle = open (filename, 'r')
        self.filehandle.seek(0, 2)
        self.data_len = self.filehandle.tell()
        self.filehandle.seek(0, 0)
        if platform.system() == "Windows":
        self.data_map = mmap.mmap(self.filehandle.fileno(), self.data_len, null, mmap.ACCESS_READ)
        else:
        self.data_map = mmap.mmap(self.filehandle.fileno(), self.data_len, mmap.MAP_PRIVATE, mmap.PROT_READ)
        self.offset = 0
        self.delimeter = ", "

        self.formats =
        {
            "FMT": DFFormat(0x80,
            "FMT",
            89,
            "BBnNZ",
            "Type,Length,Name,Format,Columns")
        }
        self.id_to_name =
        { 0x80 : "FMT" }
        self._rewind()
        self._zero_time_base = zero_time_base
        self.init_clock()
        self._rewind()
        self.init_arrays(progress_callback)
    }

    /**
     * rewind to start of log
     */
    fun _rewind(){
            DFReader._rewind(this)
    // find the first valid line
        offset = data_map.find(b"FMT, ")
        if (offset == -1) {
            offset = data_map.find(b"FMT,")
            if (offset != -1) {
                delimeter = ","
            }
        }
        type_list = null
    }

    /**
     * rewind to start of log
     */
    fun rewind() {
        self._rewind()

        /**
         * initialise arrays for fast recv_match()
         */
        fun init_arrays(progress_callback = null):
                self.offsets = {}
        self.counts =
            {}
        self._count = 0
        ofs = self.offset
        pct = 0

        while ofs + 16 < self.data_len:
        mtype = self.data_map[ofs:ofs+4]
        if mtype[3] == b',':
        mtype = mtype[0:3]
        if not mtype in self . offsets :
        self.counts[mtype] = 0
        self.offsets[mtype] = []
        self.offset = ofs
        self._parse_next()
        self.offsets[mtype].append(ofs)

        self.counts[mtype] += 1

        if mtype == "FMT":
        self.offset = ofs
        self._parse_next()

        if mtype == "FMTU":
        self.offset = ofs
        self._parse_next()

        ofs = self.data_map.find(b"\n", ofs)
        if ofs == -1:
        break
        ofs += 1
        new_pct = (100 * ofs) // self.data_len
        if progress_callback != null && new_pct != pct:
        progress_callback(new_pct)
        pct = new_pct

        for mtype in self.counts.keys():
        self._count += self.counts[mtype]
        self.offset = 0
    }

    /**
     * skip fwd to next msg matching given type set
     */
    fun skip_to_type(type) {

        if self.type_list is null:
// always add some key msg types so we can track flightmode, params etc
        self.type_list = type.copy()
        self.type_list.update(set(["MODE", "MSG", "PARM", "STAT"]))
        self.type_list = list(self.type_list)
        self.indexes = []
        self.type_nums = []
        for t in self.type_list:
        self.indexes.append(0)
        smallest_index = -1
        smallest_offset = self.data_len
        for i in range(len(self.type_list)):
        mtype = self.type_list[i]
        if not mtype in self . counts :
        continue
        if self.indexes[i] >= self.counts[mtype]:
        continue
        ofs = self.offsets[mtype][self.indexes[i]]
        if ofs < smallest_offset:
        smallest_offset = ofs
        smallest_index = i
        if smallest_index >= 0:
        self.indexes[smallest_index] += 1
        self.offset = smallest_offset
    }

    /**
     * read one message, returning it as an object
     */
    fun _parse_next() {

            while true:
    endline = self.data_map.find(b'\n',self.offset)
    if endline == -1:
    endline = self.data_len
    if endline < self.offset:
    break
    s = self.data_map[self.offset:endline].rstrip()
    if sys.version_info.major >= 3:
    s = s.decode("utf-8")
    elements = s.split(self.delimeter)
    self.offset = endline+1
    if len(elements) >= 2:
// this_line is good
    break

    if self.offset > self.data_len:
    return null

// cope with empty structures
    if len(elements) == 5 && elements[-1] == ',':
    elements[-1] = ''
    elements.append('')

    self.percent = 100.0 * (self.offset / float(self.data_len))

    msg_type = elements[0]

    if msg_type not in self.formats:
    return self._parse_next()

    fmt = self.formats[msg_type]

    if len(elements) < len(fmt.format)+1:
// not enough columns
    return self._parse_next()

    elements = elements[1:]

    name = fmt.name.rstrip("\0")
    if name == "FMT":
// add to formats
// name, len, format, headings
    ftype = int(elements[0])
    fname = elements[2]
    if self.delimeter == ",":
    elements = elements[0:4] + [",".join(elements[4:])]
    columns = elements[4]
    if fname == "FMT" && columns == "Type,Length,Name,Format":
// some logs have the 'Columns' column missing from text logs
    columns = "Type,Length,Name,Format,Columns"
    new_fmt = DFFormat(ftype,
    fname,
    int(elements[1]),
    elements[3],
    columns,
    oldfmt=self.formats.get(ftype,null))
    self.formats[fname] = new_fmt
    self.id_to_name[ftype] = fname

    try:
    m = DFMessage(fmt, elements, false, self)
    except ValueError:
    return self._parse_next()

    if m.get_type() == "FMTU":
    fmtid = getattr(m, "FmtType", null)
    if fmtid != null && fmtid in self.id_to_name:
    fmtu = self.formats[self.id_to_name[fmtid]]
    fmtu.set_unit_ids(getattr(m, "UnitIds", null))
    fmtu.set_mult_ids(getattr(m, "MultIds", null))

    self._add_msg(m)

    return m
}

    /**
     * get the last timestamp in the log
     */
    fun last_timestamp() {
        highest_offset = 0
        for mtype in self.counts.keys():
        if len(self.offsets[mtype]) == 0:
        continue
        ofs = self.offsets[mtype][-1]
        if ofs > highest_offset:
        highest_offset = ofs
        self.offset = highest_offset
        m = self.recv_msg()
        return m._timestamp
    }

}

if (__name__ == "__main__") {
        val use_profiler = false
        if (use_profiler) {
//        from line_profiler import LineProfiler
            val profiler = LineProfiler()
            profiler.add_function(DFReader_binary._parse_next)
            profiler.add_function(DFReader_binary._add_msg)
            profiler.add_function(DFReader._set_time)
            profiler.enable_by_count()
        }

        var filename = sys.argv[1]
        if (filename.endswith(".log")) {
            log = DFReader_text(filename)
        } else {
            log = DFReader_binary(filename)
        }
//bfile = filename + ".bin"
//bout = open(bfile, "wb")
        while (true) {
            m = log.recv_msg()
            if (m == null)
                break
            //bout.write(m.get_msgbuf())
            //print(m)
        }

        if( use_profiler)
            profiler.print_stats()
}

 */