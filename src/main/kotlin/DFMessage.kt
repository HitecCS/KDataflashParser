
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

class DFMessage(val fmt: DFFormat, val elements: ArrayList<String>, val applyMultiplier : Boolean, var parent: DFReader) {

    var fieldnames : List<String> = fmt.columnsArr
    var timestamp : Long = 0L
    var Message: String? = null
        get() {
            if(field == null) field = getStringFieldByName("Message")
            return field
        }
    var Mode : String? = null
        get() {
            if(field == null) field = getStringFieldByName("Mode")
            return field
        }
    var ModeNum : Int? = null
        get() {
            if(field == null) field = getIntFieldByName("ModeNum")
            return field
        }
    var MainState: Int? = null
        get() {
            if(field == null) field = getIntFieldByName("MainState")
            return field
        }
    var Name: String? = null
        get() {
            if(field == null) field = getStringFieldByName("Name")
            return field
        }
    var Value: Float? = null//TODO
        get() {
        if(field == null) field = getFloatFieldByName("Value")
        return field
        }
    var TimeUS : Int? = null
        get() {
            if(field == null) field = getIntFieldByName("TimeUS")
            return field
        }
    var TimeMS : Float? = null
        get() {
            if(field == null) field = getFloatFieldByName("TimeMS")
            return field
        }
    var GWk : Float? = null
        get() {
            if(field == null) field = getFloatFieldByName("GWk")
            return field
        }
    var GMS : Float? = null
        get() {
            if(field == null) field = getFloatFieldByName("GMS")
            return field
        }
    var T : Int? = null
        get() {
            if(field == null) field =getIntFieldByName("T")
            return field
        }
    var Week : Float? = null
        get() {
            if(field == null) field = getFloatFieldByName("Week")
            return field
        }
    var GPSTime : Float? = null
        get() {
            if(field == null) field =getFloatFieldByName("GPSTime")
            return field
        }
    var StartTime : Long? = null
        get() {
            if(field == null) field = getLongFieldByName("StartTime")
            return field
        }

    fun getIntFieldByName(name: String) : Int? {
        val index = fieldnames.indexOf(name)
        if(index != -1) {
            return elements[index].toInt()
        }
        return null
    }

    fun getLongFieldByName(name: String) : Long? {
        val index = fieldnames.indexOf(name)
        if(index != -1) {
            return elements[index].toLong()
        }
        return null
    }

    fun getFloatFieldByName(name: String) : Float? {
        val index = fieldnames.indexOf(name)
        if(index != -1) {
            return elements[index].toFloat()
        }
        return null
    }

    fun getStringFieldByName(name: String) : String? {
        val index = fieldnames.indexOf(name)
        if(index != -1) {
            return elements[index]
        }
        return null
    }

    fun toDict() : HashMap<String, String> {
        val d = hashMapOf ( "mavpackettype" to fmt.name )

        for(field in fieldnames)
            d[field] = getAttr(field).first as String

        return d
    }

    fun getAttr(field: String) : Pair<Any?, KClass<out Any>>  {
        return getAttr(field, null)
    }

    /**
     * override field getter
     */
    fun getAttr(field: String, default : Any?) : Pair<Any?, KClass<out Any>> {
        val i: Int
        try {
            i = fmt.colhash[field]!!
        } catch (e: Exception) {
            throw java.lang.Exception(field)
        }

        var v: Any?
        var kClass: KClass<out Any>?
        v = elements[i]
        kClass = Array<Float>::class

        if (fmt.format[i] == 'a') {
            //Squeltch
        } else if (fmt.format[i] != 'M' || applyMultiplier) {
            v = when(fmt.msgTypes[i]) {
                Int::class -> v.toInt()
                Double::class -> v.toDouble()
                Boolean::class -> v.toString().lowercase().toBoolean()
                String::class -> v
                else -> v

            }
            kClass = fmt.msgTypes[i]
        }
        if (fmt.msgTypes[i] == String::class) {
            v = Util.nullTerm(v as String)
        }
        if (fmt.msgMults[i] != null && applyMultiplier) {
            if (v is Double)
                v *= fmt.msgMults[i]!!
            else if (v is String)
                v = v.toDouble() * fmt.msgMults[i]!!
        }
        return Pair(v, kClass)
    }

    fun hasAttr(name : String) : Boolean {
        return fieldnames.contains(name)
    }


    /**
     * override field setter
     */
    fun setAttr(field: String, vTemp : Any?) {
        var v = vTemp
        if (!field[0].isUpperCase() || !fmt.colhash.containsKey(field)) {
            when (field) {
                "fieldnames" -> fieldnames = v as List<String>
                "_timestamp" -> timestamp = v as Long
                "Message" -> Message = v as String
                "Mode" -> Mode = v as String
                "ModeNum" -> ModeNum = v as Int
                "MainState" -> MainState = v as Int
                "Name" -> Name = v as String
                "Value" -> Value = v as Float
                "TimeUS" -> TimeUS = v as Int
                "TimeMS" -> TimeMS = v as Float
                "GWk" -> GWk = v as Float
                "GMS" -> GMS = v as Float
                "T" -> T = v as Int
                "Week" -> Week = v as Float
                "GPSTime" -> GPSTime = v as Float
                "StartTime" -> StartTime = v as Long
            }
        } else {
            val i = fmt.colhash[field]
            if (fmt.msgMults[i!!] != null && applyMultiplier) {
                v = (v as Double) / fmt.msgMults[i]!!
            }
            elements[i] = v.toString()
        }
    }


    fun getType() : String {
        return fmt.name
    }

//    override fun toString() : String {
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
//        return this.toString()
//    }

    /**
     * create a binary message buffer for a message
     */
    fun getMsgBuf() {
//        var values = arrayListOf<Any>()
//        for (i in 0..fmt.columns.length) {
//            if (i >= fmt.msg_mults.size) {
//                continue
//            }
//            val mul = fmt.msg_mults[i]
//            var name = fmt.columns[i]
//            if (name == "Mode" && fmt.columns.contains("ModeNum")) {
//                name = "ModeNum"
//            var  v = getAttr(name)
//            if (v is String) {
//                v = bytes(v, "ascii")
//            }
//            if (v is Array<out Any>::class) {
//                v = v.toString()
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


    fun getDFFieldnames() : List<String>{
        return fieldnames
    }

    /**
     * support indexing, allowing for multi-instance sensors in one message
     */
    fun getItem(key: String) : Any {
        if (fmt.instanceField == null) {
//            raise IndexError ()
            throw java.lang.Exception("IndexError")
        }
        val k = String.format("%s[%s]", fmt.name, key)
        if (!parent.messages.contains(k)) {
            throw java.lang.Exception("IndexError")
        }
        return parent.messages[k]!!
    }
}