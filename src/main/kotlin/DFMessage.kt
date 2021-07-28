
import java.util.*
import kotlin.collections.ArrayList
import kotlin.reflect.KClass

/*
 * DFMessage
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

class DFMessage(val fmt: DFFormat, val _elements: ArrayList<String>, val _apply_multiplier : Boolean, var _parent: DFReader) {
    var fieldnames : List<String> = fmt.columnsArr
    var _timestamp : Long = 0L
    var Message: String? = null
    var Mode : Any? = null
    var ModeNum : Int? = null
    var MainState: Int? = null
    var Name: String? = null
    var Value: Any? = null
    var TimeUS : Int? = null
    var TimeMS : Int? = null
    var GWk : Int? = null
    var GMS : Int? = null
    var T : Int? = null
    var Week : Int? = null
    var GPSTime : Int? = null
    var StartTime : Long? = null

    fun to_dict() : HashMap<String, String> {
        val d = hashMapOf ( "mavpackettype" to fmt.name )

        for(field in fieldnames)
            d[field] = __getattr__(field) as String

        return d
    }

    fun __getattr__(field: String) : Pair<Any?, KClass<out Any>>  {
        return __getattr__(field, null)
    }

    /**
     * override field getter
     */
    fun __getattr__(field: String, default : Any?) : Pair<Any?, KClass<out Any>> {
        var i = 0
        try {
            i = fmt.colhash[field]!!
        } catch (e: Exception) {
            throw java.lang.Exception(field)
        }

        var v = default
        var kClass : KClass<out Any>? = null
//        if (_elements[i] is ByteArray) {
//            v = _elements[i].toString("utf-8")
//            kClass = ByteArray::class
//        } else {
            v = _elements[i]
            kClass = Array<Float>::class
//        }

        if (fmt.format[i] == 'a') {
            //Squeltch
        } else if (fmt.format[i] != 'M' || _apply_multiplier) {
            v = when(fmt.msg_types[i]) {
                Int::class -> v as Int
                Double::class -> v as Double
                String::class -> v as String
                else -> v

            }
            kClass = fmt.msg_types[i]
        }
        if (fmt.msg_types[i] == String::class) {
//            v = Util.null_term(v)
//            v = Util.null_term(v)
        }
        if (fmt.msg_mults[i] != null && _apply_multiplier) {
            v = (v as Double) * fmt.msg_mults[i]!!
        }
        return Pair(v, kClass)
    }

    fun __hasattr__(name : String) : Boolean {
        return fieldnames.contains(name)
    }


    /**
     * override field setter
     */
    fun __setattr__( field: String, vTemp : Any?) {
        var v = vTemp
        if (!field[0].isUpperCase() || !fmt.colhash.containsKey(field)) {
            when (field) {
                "fieldnames" -> fieldnames = v as List<String>
                "_timestamp" -> _timestamp = v as Long
                "Message" -> Message = v as String
                "Mode" -> Mode = v
                "ModeNum" -> ModeNum = v as Int
                "MainState" -> MainState = v as Int
                "Name" -> Name = v as String
                "Value" -> Value = v
                "TimeUS" -> TimeUS = v as Int
                "TimeMS" -> TimeMS = v as Int
                "GWk" -> GWk = v as Int
                "GMS" -> GMS = v as Int
                "T" -> T = v as Int
                "Week" -> Week = v as Int
                "GPSTime" -> GPSTime = v as Int
                "StartTime" -> StartTime = v as Long
            }
        } else {
            val i = fmt.colhash[field]
            if (fmt.msg_mults[i!!] != null && _apply_multiplier) {
                v = (v as Double) / fmt.msg_mults[i]!!
            }
            _elements[i] = v.toString()
        }
    }


    fun get_type() : String {
        return fmt.name
    }

    fun __str__() {
//        var ret = String.format("%s {" , fmt.name)
//        var col_count = 0
//        for (c in fmt.columns) {
//            var v = __getattr__(c)
//            if(v is Float && math.isnan(v )) {
////                quiet nans have more non - zero values :
//                val noisy_nan = "\x7f\xf8\x00\x00\x00\x00\x00\x00"
//                if (struct.pack(">d", v) != noisy_nan) {
//                    v = "qnan"
//                }
//            }
//            ret += String.format("%s : %s, " , c, v )
//            col_count += 1
//        }
//        if(col_count != 0)
//            ret = ret[:-2]
//
//        return ret + "}"
    }

    /**
     * create a binary message buffer for a message
     */
    fun get_msgbuf() {
//        var values = arrayListOf<Any>()
//        for (i in 0..fmt.columns.length) {
//            if (i >= fmt.msg_mults.size) {
//                continue
//            }
//            val mul = fmt.msg_mults[i]
//            var name = fmt.columns[i]
//            if (name == "Mode" && fmt.columns.contains("ModeNum")) {
//                name = "ModeNum"
//            var  v = __getattr__(name)
//            if (v is String) {
//                v = bytes(v, "ascii")
//            }
//            if (v is Array<out Any>::class) {
//                v = v.tostring()
//            }
//            if (mul != null) {
//                v /= mul
//                v = Int(round(v))
//            }
//            values.add(v)
//        }
//
//        var ret1 = struct.pack("BBB", 0xA3, 0x95, fmt.type)
//        var ret2 = Any
//        try {
//            ret2 = struct.pack(fmt.msg_struct, *values)
//        } catch (e: Exception) {
//            return null
//        }
//        return ret1 + ret2
    }


    fun get_fieldnames() : List<String>{
        return fieldnames
    }

    /**
     * support indexing, allowing for multi-instance sensors in one message
     */
    fun __getitem__(key: String) : Any {
        if (fmt.instance_field == null) {
//            raise IndexError ()
            throw java.lang.Exception("IndexError")
        }
        val k = String.format("%s[%s]", fmt.name, key)
        if (!_parent.messages.contains(k)) {
            throw java.lang.Exception("IndexError")
        }
        return _parent.messages[k]!!
    }
}