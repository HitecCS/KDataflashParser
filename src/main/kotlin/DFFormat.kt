import kotlin.reflect.KClass

/*
 * DFFormat
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

class DFFormat(
    val type: Int, //TODO these types are for sure incorrect
    val name: String,
    val len: Int,
    val format: String,
    val columns: String,
    val oldFmt: DFFormat? = null
) {
    /**
     * https://docs.python.org/3/library/struct.html#format-characters
     */
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

    var instanceField: String? = null
    var unitIds: String? = null
    var multIds: String? = null
    var columnsArr = listOf<String>()
    var colhash = hashMapOf<String, Int>()
    var msgMults = arrayListOf<Double?>()
    var msgTypes = arrayListOf<KClass<out Any>>()
    var msgStruct = ""
    val aIndexes = arrayListOf<Int>()
    var msgFmts = arrayListOf<Char>()

    init {
        columnsArr = columns.split(',')
        instanceField = null
        unitIds = null
        multIds = null

        if (columnsArr.size == 1 && columnsArr[0] == "")
            columnsArr = emptyList()

        var msgStruct = "<"
        val msgFmts = arrayListOf<Char>()
        val msgTypes = arrayListOf<KClass<out Any>>()
        var msgMults = arrayListOf<Double?>()

        for (c in format) {
//            if u_ord(c) == 0:
//                return @loop
            try {
                msgFmts.add(c)
                val structContainer = formatToStruct[c]
                msgStruct += structContainer!!.cFormatCode
                msgMults.add(structContainer.mul)
                if (c == 'a')
                    msgTypes.add(Array<out Any>::class)
                else
                    msgTypes.add(structContainer.type)
            } catch (e: Throwable) {
                val msg = "Unsupported format char: '$c' in message $name"
                println("DFFormat: $msg")
                throw Exception(msg)
            }
        }

        this.msgStruct = msgStruct
        this.msgTypes = msgTypes
        this.msgMults = msgMults
        this.msgFmts = msgFmts

        for (i in columnsArr.indices) {
            colhash[columnsArr[i]] = i
        }

        val aIndexes = arrayListOf<Int>()
        for (i in 0 until msgFmts.size) {
            if (msgFmts[i] == 'a') {
                aIndexes.add(i)
            }
        }

        if (oldFmt != null) {
            setUnitIdsAndInstField(oldFmt.unitIds)
            multIds = oldFmt.multIds
        }
    }

    /**
     * set unit IDs string from FMTU
     */
    fun setUnitIdsAndInstField(unitIds: String?) {
        unitIds ?: return

        this.unitIds = unitIds
        val instanceIdx = unitIds.indexOf('#')
        if (instanceIdx != -1)
            instanceField = columnsArr[instanceIdx]
    }

    override fun toString(): String {
        return String.format("DFFormat(%s,%s,%s,[%s])", type, name, format, columns)
    }
//
//    fun getUnpacker() : (ByteArray) -> ArrayList<String> {
//        return { input : ByteArray ->
//            val returnable = arrayListOf<String>()
//
//            var index = 0
//            msgTypes.forEach {
//                when(msgStruct) {
//                    Int::class -> input.toInt()
//                    Double::class -> v.toDouble()
//                    Boolean::class -> v.toString().lowercase().toBoolean()
//                    String::class -> v
//                    else -> v
//                }
//            }
//
//            returnable
//        }
//    }
}