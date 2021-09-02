import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.math.BigInteger
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
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
 * Parses a text DataFlash log
 */
class DFReaderText(private val filename: String, zeroBasedTime: Boolean?, private val progressCallback: ((Int) -> Unit)?) : DFReader() {

    private var dataLen: Int
    private var pythonLength: Int
    private var numLines: BigInteger = BigInteger.valueOf(0L)

    private var bufferedReader : BufferedReader
    private var offset: Int
    private var delimiter: String
    private var offsets = hashMapOf<String,ArrayList<Int>>()
    private var typeSet : HashSet<String>? = null

    var formats: HashMap<String, DFFormat>
    private var idToName: HashMap<Int, String>

    private var counts = hashMapOf<String,Int>()
    private var count = 0
    private var ofs = 0

    private val startTime: Long
        get() { return clock?.timebase?.toLong() ?: 0L }

    private var endTime: Long = 0L

    init {
        this.zeroTimeBase = zeroBasedTime ?: false
        // read the whole file into memory for simplicity
        println("Beginning initial parse")

        var fLength = 0
        pythonLength = 0
        File(filename).forEachLine {
            numLines += BigInteger.ONE
            fLength += it.length + "\n".length
            pythonLength += it.length + 2 // Not exactly sure why its 2 but I assume its formatting characters (File.tell() in python may count "\n\r")
        }
        println("Reading $numLines lines")
        dataLen = fLength
        offset = 0
        bufferedReader = BufferedReader( FileReader(filename))
        delimiter = ", "

        formats = hashMapOf(Pair("FMT", DFFormat(0x80, "FMT", 89, "BBnNZ", "Type,Length,Name,Format,Columns")))
        idToName = hashMapOf(Pair(0x80, "FMT"))
        rewind()
        initClock()
        rewind()
        initArrays()
        rewind()
    }

    /**
     * Rewind to start of log
     */
    override fun rewind() {
        println("rewind()")
        super.rewind()
        // find the first valid line
        offset = 0
        bufferedReader.close()
        bufferedReader = BufferedReader( FileReader(filename))

//        offset = findNextTag("FMT, ", null, null)
//        if (offset == -1) {
//            offset = findNextTag("FMT,", null, null)
//            if (offset != -1) {
//                delimiter = ","
//            }
//        }
        typeSet = null
    }

    /**
     * Calls close() on DFReaderText's internal BufferedReader. Use with caution
     *
     * @throws IOException
     */
    fun close()  {
        bufferedReader.close()
    }


    /**
     * Returns the value the DFReaderText's internal BufferedReader returns ready() function. If a Throwable occurs, it
     * returns false.
     *
     * Can be used to avoid exceptions when using parseNext()
     */
    fun hasNext() : Boolean {
        return try {
            bufferedReader.ready()
        } catch (e : Throwable) {
            false
        }
    }

    /**
     * Warning, long-running, memory intensive operation
     *
     * This function returns, every entry from a DataFlash log, fully parsed as a DFMessage in an ArrayList. The list
     * returned will be ordered in the same order as the DataFlash log (presumably, time-sorted order).
     *
     * @return ArrayList<DFMessage> containing a DFMessage for each entry in the DataFlash log
     */
    fun getAllMessages(): ArrayList<DFMessage> {
        val returnable = arrayListOf<DFMessage>()
        rewind()
        var lineCount = BigInteger.ZERO
        var pct = 0
        var nullCount = 0
        while (lineCount < numLines) {
            parseNext()?.let {
                returnable.add(it)
            } ?: run {
                nullCount++
            }
            val newPct = offset / dataLen
            if(pct != newPct) {
                pct = newPct
                println(newPct)
            }
            lineCount ++
        }
        offset = 0
        bufferedReader.close()
        return returnable
    }


    /**
     * Warning, possibly long-running operation.
     *
     * This function returns, for each field specified, an ArrayList containing every instance value for a given field
     * paired with the timestamp of that values occurrence. In each Pair, the timestamp will be the first element and
     * the value is the second. The list returned will be ordered in same order as the DataFlash log (presumably,
     * time-sorted order). The ArrayLists are returned in a HashMap, in which each key is the name of the field, and the
     * value is the ArrayList.
     *
     * In the case that a given field does not occur in the log, and empty ArrayList will be returned.
     *
     * Field names are case-sensitive.
     *
     * @param fields a collection of field names to search for the values of within the DataFlash log
     * @return HashMap containing an ArrayList<Pair<Long, Any>> for each given field. Where the Pair's first element is
     * the timestamp, and the second element is the value of the field at that instance
     */
    fun getFieldLists(fields : Collection<String>) : HashMap<String, ArrayList<Pair<Long,Any>>> {
        rewind()
        var lineCount = BigInteger.ZERO
        var pct = 0

        val returnable = hashMapOf<String, ArrayList<Pair<Long,Any>>>()
        fields.forEach {
            returnable[it] = arrayListOf()
        }

        while (lineCount < numLines) {//dataMap.length

            parseNext()?.let { m ->
                val intersection = m.fieldnames intersect fields
                intersection.forEach {
                    returnable[it]?.add(Pair(m.timestamp, m.getAttr(it).first!!))
                }
            }
            val newPct = offset / dataLen//dataMap.length
            if(pct != newPct) {
                pct = newPct
                println(newPct)
            }
            lineCount ++
        }
        offset = 0
        bufferedReader.close()
        return returnable
    }


    /**
     * Warning, possibly long-running operation.
     *
     * This function returns an ArrayList containing every instance value for a given field paired with the timestamp
     * of that instance values occurrence, where the given lambda function also returns true when passed the DFMessage
     * (the DataFlash log entry) the value was found in . In each Pair, the timestamp will be the first element and the
     * value is the second. The list returned will be ordered in same order as the DataFlash log (presumably,
     * time-sorted order).
     *
     * Field names are case-sensitive.
     *
     * This function is useful in the case where one only wants the instance of a field under certain conditions.
     *
     * Ex: val dfReader = DFReaderText("dataflash.log", null, null)
     * val altsFromBaroMessages = dfReader.getFieldListConditional("Alt", { message -> message.getType() == "BARO" })
     *
     * The above example would return the list of timestamp/instance-value pair from the DataFlash log where the
     * DFMessage's type was "BARO"
     *
     * @param field a field name to search for the values of within the DataFlash log
     * @param shouldInclude a lambda function, which must return true to add a timestamp/instance-value pair to the
     * returned ArrayList
     * @return ArrayList<Pair<Long, Any>> where the Pair's first element is the timestamp, and the second element is the value of the field at that instance
     */
    fun getFieldListConditional(field : String, shouldInclude: (DFMessage) -> Boolean) : ArrayList<Pair<Long,Any>> {
        rewind()
        var lineCount = BigInteger.ZERO
        var pct = 0

        val returnable = ArrayList<Pair<Long,Any>>()

        while (lineCount < numLines) {

            parseNext()?.let { m ->
                if(m.fieldnames.contains(field) && shouldInclude(m)) {
                    returnable.add(Pair(m.timestamp, m.getAttr(field).first!!))
                }
            }
            val newPct = offset / dataLen
            if(pct != newPct) {
                pct = newPct
                println(newPct)
            }
            lineCount ++
        }
        offset = 0
        bufferedReader.close()
        return returnable
    }



    /**
     * Initialise arrays for fast recvMatch()
     */
    private fun initArrays() {
        println("initArrays")
        offsets = hashMapOf()
        counts = hashMapOf()
        count = 0
        ofs = offset
        var pct = 0

        var lastMessage : DFMessage? = null
        while (ofs + 16 < dataLen) {
            val line = bufferedReader.readLine() ?: break
            var mType = line.substring(0, 4)
            if (mType[3] == ',') {
                mType = mType.substring(0, 3)
            }
            if (!offsets.containsKey(mType)) {
                counts[mType] = 0
                offsets[mType] = arrayListOf()
                offset = ofs
                parseNext()?.let {
                    lastMessage = it
                }
            }
            offsets[mType]?.add(ofs)

            counts[mType] = counts[mType]!! + 1

            if (mType == "FMT") {
                offset = ofs
                parseNext()?.let {
                    lastMessage = it
                }
            }

            if (mType == "FMTU") {
                offset = ofs
                parseNext()?.let {
                    lastMessage = it
                }
            }

            ofs += line.length //indexOf("\n", ofs)
            if (ofs == -1) {
                break
            }
            ofs += 1
            val newPct = ((100.0 * ofs) / dataLen).toInt()
            progressCallback?.let { callback ->
                if(newPct != pct) {
                    callback(newPct)
                    pct = newPct
                }
            }
        }

        lastMessage?.let {
            endTime = it.timestamp
        }


        for (key in counts.keys) {
            count += counts[key]!!
        }
        offset = 0
    }

    /**
     * Skip Forward to next message matching given type set
     */
    fun skipToType(type : String) {

//        if (type_list == null) {
//// always add some key msg types, so we can track "flightmode," "params," etc.
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
     * Read one message, returning it as an DFMessage
     */
    override fun parseNext() : DFMessage? {

        var elements = arrayListOf<String>()

        var line :String?
        while (true) {

            line = bufferedReader.readLine()
            if(line == null || line.isEmpty() )
                break

            elements = ArrayList(line.split(delimiter))
            offset += line.length + 1
            if (elements.size >= 2) {
                // this_line is good
                break
            }
        }

        if (offset > dataLen || line == null) {
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

        val name = fmt.name
        if (name == "FMT") {
            // add to "formats"
            // name, len, format, headings
            val fType = elements[0].toInt()
            val fName = elements[2]
            if (delimiter == ",") {
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
        val m: DFMessage?
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


    fun getStartAndEndTimes() : Pair<Long, Long> {
        return Pair(startTime , endTime )
    }

    /**
     * Get the last timestamp in the log
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
     *
     * return the char index of the next instance of the tag after start and before end, or -1 if no instance was found
     */
    private fun findNextTag(tag: String, start: Int?, end: Int?): Int {
        val a = start?.toBigInteger() ?: BigInteger.ZERO
        val z = end?.toBigInteger() ?: (numLines - BigInteger.valueOf(1L))

        val fr = FileReader(File(filename))
        val br = BufferedReader(fr)

        var head = a.toInt()
        br.skip(a.toLong())

        while (head < z.toInt()) {
//            if (fileLines[i][0].startsWith(tag)) {
            val line = br.readLine()
            if (line.startsWith(tag)) {
                return head
            }
            head += line.length + "\n".length
        }
        return -1
    }

}