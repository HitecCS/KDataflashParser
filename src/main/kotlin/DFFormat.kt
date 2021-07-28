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
    val flen: Int,
    val format: String,
    val columns: String,
    val oldfmt: DFFormat? = null
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

    var instance_field: String? = null
    var unit_ids: String? = null
    var mult_ids: String? = null
    var columnsArr = listOf<String>()
    var colhash = hashMapOf<String, Int>()
    var msg_mults = arrayListOf<Double?>()
    val msg_types = arrayListOf<KClass<out Any>>()
    val msg_struct = 0 //Type unknown
    val a_indexes = arrayListOf<Int>()

    init {
        columnsArr = columns.split(',')
        instance_field = null
        unit_ids = null
        mult_ids = null

        if (columnsArr.size == 1 && columnsArr[0] == "")
            columnsArr = emptyList()

        var msg_struct = "<"
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

        for (i in columnsArr.indices) {
            colhash[columnsArr[i]] = i
        }

        val a_indexes = arrayListOf<Int>()
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