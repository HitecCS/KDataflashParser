import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

/*
 * DFReaderText
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
class DFReaderText(filename: String, zeroBasedTime: Boolean?, progressCallback: ProgressCallback?) : DFReader() {
    var filename: String
    var progressCallback: ProgressCallback?
    var dataLen: Int
    var pythonLength: Int
    var fileLines: List<String>
    var dataMap: String = ""
    var offset: Int
    var delimeter: String
    var offsets = hashMapOf<String,ArrayList<Int>>()
    var typeSet : HashSet<String>? = null

    var formats: HashMap<String, DFFormat>
    var idToName: HashMap<Int, String>

    var counts = hashMapOf<String,Int>()
    var count = 0
    var ofs = 0

    var allMessages = arrayListOf<DFMessage>()
        get() {
    //        var typeAtOffset = hashMapOf<o,String>()
            if(field.isNotEmpty())
                return field

            var count = 0
            var pct = 0
            var nullCount = 0
            while (offset < dataMap.length) {
                parseNext()?.let {
                    field.add(it)
                } ?: run {
                    nullCount++
                }
                val newPct = offset / dataMap.length
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
        this.zeroTimeBase = zeroBasedTime ?: false
        this.progressCallback = progressCallback
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
        dataLen = flength
        dataMap = ""
        offset = 0
        delimeter = ", "

        println("combining lines into a single String")
        val builder = StringBuilder()
        fileLines.forEach {
            builder.append(it)
            builder.append("\n")
//            dataMap += it//.split(delimeter))
//            dataMap += "\n"
        }
        dataMap = builder.toString()
        println("finished combining")

        formats = hashMapOf(Pair("FMT", DFFormat(0x80, "FMT", 89, "BBnNZ", "Type,Length,Name,Format,Columns")))
        idToName = hashMapOf(Pair(0x80, "FMT"))
        rewind()
        initClock()
        rewind()
        initArrays()
    }

    /**
     * rewind to start of log
     */
    override fun rewind() {
        println("rewind()")
        super.rewind()
        // find the first valid line
        offset = findNextTag("FMT, ", null, null)
        if (offset == -1) {
            offset = findNextTag("FMT,", null, null)
            if (offset != -1) {
                delimeter = ","
            }
        }
        typeSet = null
    }


    /**
     * initialise arrays for fast recvMatch()
     */
    private fun initArrays() {
        println("initArrays")
        offsets = hashMapOf()
        counts = hashMapOf()
        count = 0
        ofs = offset
        var pct = 0

        while (ofs + 16 < dataMap.length) {//dataLen) {
            var mtype = dataMap.substring(ofs, ofs + 4)
            if (mtype[3] == ',') {
                mtype = mtype.substring(0, 3)
            }
            if (!offsets.containsKey(mtype)) {
                counts[mtype] = 0
                offsets[mtype] = arrayListOf()
                offset = ofs
                parseNext()
            }
            offsets[mtype]?.add(ofs)

            counts[mtype] = counts[mtype]!! + 1

            if (mtype == "FMT") {
                offset = ofs
                parseNext()
            }

            if (mtype == "FMTU") {
                offset = ofs
                parseNext()
            }

            ofs = dataMap.indexOf("\n", ofs)
            if (ofs == -1) {
                break
            }
            ofs += 1
            val newPct = (100 * ofs) / dataMap.length
            progressCallback?.let { callback ->
                if(newPct != pct) {
                    callback.update(newPct)
                    pct = newPct
                }
            }
        }


        for (key in counts.keys) {
            count += counts[key]!!
        }
        offset = 0
    }

    /**
     * skip fwd to next msg matching given type set
     */
    fun skipToType(type : String) {

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
    override fun parseNext() : DFMessage? {

        var elements = arrayListOf<String>()

        while (true) {
            var endline = dataMap.indexOf("\n", offset)
            if (endline == -1) {
                endline = dataLen
                if (endline < offset) {
                    break
                }
            }

            val s = dataMap.substring(offset,endline).trimEnd()
            elements = ArrayList(s.split(delimeter))
            offset = endline + 1
            if (elements.size >= 2) {
                // this_line is good
                break
            }
        }

        if (offset > dataLen) {
            return null
        }

        // cope with empty structures
        if (elements.size == 5 && elements[elements.size - 1] == ",") {
            val lastIndex = elements.size - 1
            elements[lastIndex] = ""
            elements.add("")
        }

        percent = 100f * (offset.toFloat() / dataLen.toFloat())

        val msgType = elements[0]

        if (!formats.contains(msgType)) {
            return parseNext()
        }

        val fmt = formats[msgType]

        if (elements.size < fmt!!.format.length + 1) {
            // not enough columns
            return parseNext()
        }

        elements = ArrayList(elements.subList(1, elements.size))

        val name = fmt.name//.rstrip("\0")
        if (name == "FMT") {
            // add to formats
            // name, len, format, headings
            val fType = elements[0].toInt()
            val fName = elements[2]
            if (delimeter == ",") {
                val last = elements.subList(4, elements.size).joinToString(",")
                elements = ArrayList(elements.subList(0,4))
                elements.add(last)
            }
            var columns = elements[4]
            if (fName == "FMT" && columns == "Type,Length,Name,Format") {
                // some logs have the 'Columns' column missing from text logs
                columns = "Type,Length,Name,Format,Columns"
            }
            val newFmt = DFFormat(
                fType,
                fName,
                elements[1].toInt(),
                elements[3],
                columns,
                oldFmt = formats[fName]
            )
            formats[fName] = newFmt
            idToName[fType] = fName
        }
        var m : DFMessage? = null
        try {
            m = DFMessage(fmt, elements, false, this)
        } catch (valError: Throwable) {
            return parseNext()
        }

        if (m.getType() == "FMTU") {
            val fmtID = m.getAttr( "FmtType", null)
            if (fmtID.first != null && idToName.containsKey(fmtID.first as Int)) {
                val fmtU = formats[idToName[fmtID.first as Int]!!]!!
                fmtU.setUnitIdsAndInstField(m.getAttr("UnitIds", null).first as String)
                fmtU.multIds = m.getAttr( "MultIds", null).first as String
            }
        }
        addMsg(m)

        return m
    }

    /**
     * get the last timestamp in the log
     */
    private fun lastTimestamp() : Long {
        var highestOffset = 0
        for (mType in counts.keys) {
            if (offsets[mType]!!.size == 0) {
                continue
            }
            ofs = offsets[mType]!![offsets.size-1]
            if (ofs > highestOffset) {
                highestOffset = ofs
            }
        }
        offset = highestOffset
        val m = recvMsg()
        return m!!.timestamp
    }

    /**
     * Finds and returns the first index of the tag in the given range
     */
    private fun findNextTag(tag: String, start: Int?, end: Int?): Int {
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