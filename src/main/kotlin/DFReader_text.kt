import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

/*
 * DFReader_text
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
 * parse a text dataflash file
 */
class DFReader_text(filename: String, zero_based_time: Boolean?, progressCallback: ProgressCallback?) : DFReader() {
    var filename: String
    var zero_time_base: Boolean
    var progress_callback: ProgressCallback?
    var data_len: Int
    var pythonLength: Int
    var fileLines: List<String>
    var data_map: String = ""
    var offset: Int
    var delimeter: String
    var offsets = hashMapOf<String,ArrayList<Int>>()
    var type_list : HashSet<String>? = null

    var formats: HashMap<String, DFFormat>
    var id_to_name: HashMap<Int, String>

    var counts = hashMapOf<String,Int>()
    var _count = 0
    var ofs = 0

    var allMessages = arrayListOf<DFMessage>()
        get() {
    //        var typeAtOffset = hashMapOf<o,String>()
            if(field.isNotEmpty())
                return field

            var count = 0
            var pct = 0
            var nullCount = 0
            while (offset < data_map.length) {
                _parse_next()?.let {
                    field.add(it)
                } ?: run {
                    nullCount++
                }
                val newPct = offset / data_map.length
                if(pct != newPct) {
                    pct = newPct
                    println(newPct)
                }
                count ++
            }
            offset = 0
            return field
        }

    init {
        this.filename = filename
        this.zero_time_base = zero_based_time ?: false
        progress_callback = progressCallback
//        DFReader.__init__()
        // read the whole file into memory for simplicity
        println("reading in log")
        fileLines = File(filename).readLines()

        println("Read ${fileLines.size} lines")
        var flength = 0
        pythonLength = 0
        fileLines.forEach {
            flength += it.length
            pythonLength += it.length + 2 // Not exactly sure why its 2 but I assume its formatting characters (File.tell() in python may count "\n\r")
        }
        data_len = flength
        data_map = ""
        offset = 0
        delimeter = ", "

        println("combining lines into a single String")
        val builder = StringBuilder()
        fileLines.forEach {
            builder.append(it)
            builder.append("\n")
//            data_map += it//.split(delimeter))
//            data_map += "\n"
        }
        data_map = builder.toString()
        println("finished combining")

        formats = hashMapOf(Pair("FMT", DFFormat(0x80, "FMT", 89, "BBnNZ", "Type,Length,Name,Format,Columns")))
        id_to_name = hashMapOf(Pair(0x80, "FMT"))
        _rewind()
        _zero_time_base = zero_time_base
        init_clock()
        _rewind()
        init_arrays(progress_callback)
    }

    /**
     * rewind to start of log
     */
    override fun _rewind() {
        println("_rewind()")
        super._rewind()
        // find the first valid line
        offset = findNextTag("FMT, ", null, null)
        if (offset == -1) {
            offset = findNextTag("FMT,", null, null)
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
        _rewind()
    }


    /**
     * initialise arrays for fast recv_match()
     */
    fun init_arrays(progress_callback: ProgressCallback?) {
        println("init_arrays")
        offsets = hashMapOf<String,ArrayList<Int>>()
        counts = hashMapOf<String,Int>()
        _count = 0
        ofs = offset
        var pct = 0

        while (ofs + 16 < data_map.length) {//data_len) {
            var mtype = data_map.substring(ofs, ofs + 4)
            if (mtype[3] == ',') {
                mtype = mtype.substring(0, 3)
            }
            if (!offsets.containsKey(mtype)) {
                counts[mtype] = 0
                offsets[mtype] = arrayListOf<Int>()
                offset = ofs
                _parse_next()
            }
            offsets[mtype]?.add(ofs)

            counts[mtype] = counts[mtype]!! + 1

            if (mtype == "FMT") {
                offset = ofs
                _parse_next()
            }

            if (mtype == "FMTU") {
                offset = ofs
                _parse_next()
            }

            ofs = data_map.indexOf("\n", ofs)
            if (ofs == -1) {
                break
            }
            ofs += 1
            val new_pct = ((100 * ofs) / data_map.length).toInt()
            if (progress_callback != null && new_pct != pct) {
                progress_callback.update(new_pct)
                pct = new_pct
            }
        }


        for (key in counts.keys) {
            _count += counts[key]!!
        }
        offset = 0
    }

    /**
     * skip fwd to next msg matching given type set
     */
    fun skip_to_type(type : String) {

//        if (type_list == null) {
//// always add some key msg types so we can track flightmode, params etc
//            type_list = type.copy()
//            type_list.update(hashSetOf(["MODE", "MSG", "PARM", "STAT"]))
//            type_list = list(type_list)
//            indexes = []
//            type_nums = []
//            for (t in type_list) {
//                indexes.append(0)
//            }
//        }
//        var smallest_index = -1
//        var smallest_offset = data_len
//        for (i in 0..type_list!!.size) {
//            mtype = type_list[i]
//            if (not mtype in self . counts) {
//                continue
//            }
//            if (indexes[i] >= counts[mtype]) {
//                continue
//            }
//            ofs = offsets[mtype][indexes[i]]
//            if (ofs < smallest_offset) {
//                smallest_offset = ofs
//                smallest_index = i
//            }
//        }
//        if (smallest_index >= 0) {
//            indexes[smallest_index] += 1
//            offset = smallest_offset
//        }
    }

    /**
     * read one message, returning it as an object
     */
    override fun _parse_next() : DFMessage? {

        var elements = arrayListOf<String>()

        while (true) {
            var endline = data_map.indexOf("\n", offset)
            if (endline == -1) {
                endline = data_len
                if (endline < offset) {
                    break
                }
            }

            val s = data_map.substring(offset,endline).trimEnd()
            elements = ArrayList(s.split(delimeter))
            offset = endline + 1
            if (elements.size >= 2) {
                // this_line is good
                break
            }
        }

        if (offset > data_len) {
            return null
        }

        // cope with empty structures
        if (elements.size == 5 && elements[elements.size - 1] == ",") {
            val lastIndex = elements.size - 1
            elements[lastIndex] = ""
            elements.add("")
        }

        percent = 100f * (offset.toFloat() / data_len.toFloat())

        val msg_type = elements[0]

        if (!formats.contains(msg_type)) {
            return _parse_next()
        }

        val fmt = formats[msg_type]

        if (elements.size < fmt!!.format.length + 1) {
            // not enough columns
            return _parse_next()
        }

        elements = ArrayList(elements.subList(1, elements.size))

        val name = fmt.name//.rstrip("\0")
        if (name == "FMT") {
            // add to formats
            // name, len, format, headings
            val ftype = elements[0].toInt()
            val fname = elements[2]
            if (delimeter == ",") {
                val last = elements.subList(4, elements.size).joinToString(",")
                elements = ArrayList(elements.subList(0,4))
                elements.add(last)
            }
            var columns = elements[4]
            if (fname == "FMT" && columns == "Type,Length,Name,Format") {
                // some logs have the 'Columns' column missing from text logs
                columns = "Type,Length,Name,Format,Columns"
            }
            val new_fmt = DFFormat(
                ftype,
                fname,
                elements[1].toInt(),
                elements[3],
                columns,
                oldfmt = formats[fname]
            )
            formats[fname] = new_fmt
            id_to_name[ftype] = fname
        }
        var m : DFMessage? = null
        try {
            m = DFMessage(fmt, elements, false, this)
        } catch (valError: Throwable) {
            return _parse_next()
        }

        if (m.get_type() == "FMTU") {
            val fmtid = m.__getattr__( "FmtType", null)
            if (fmtid.first != null && id_to_name.containsKey(fmtid.first as Int)) {
                val fmtu = formats[id_to_name[fmtid.first as Int]!!]!!
                fmtu.set_unit_ids(m.__getattr__("UnitIds", null).first as String)
                fmtu.set_mult_ids(m.__getattr__( "MultIds", null).first as String)
            }
        }
        _add_msg(m)

        return m
    }

    /**
     * get the last timestamp in the log
     */
    fun last_timestamp() : Long {
        var highest_offset = 0
        for (mtype in counts.keys) {
            if (offsets[mtype]!!.size == 0) {
                continue
            }
            ofs = offsets[mtype]!![offsets.size-1]
            if (ofs > highest_offset) {
                highest_offset = ofs
            }
        }
        offset = highest_offset
        val m = recv_msg()
        return m!!._timestamp
    }

    /**
     * Finds and returns the first index of the tag in the given range
     */
    fun findNextTag(tag: String, start: Int?, end: Int?): Int {
        val a = start ?: 0
        val z = end ?: fileLines.size-1
        for (i in a..z) {
//            if (fileLines[i][0].startsWith(tag)) {
            if (fileLines[i].startsWith(tag)) {
                return i
            }
        }
        return -1
    }

}