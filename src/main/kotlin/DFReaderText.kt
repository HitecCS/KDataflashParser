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
class DFReaderText(val filename: String, zeroBasedTime: Boolean?, private val progressCallback: ((Int) -> Unit)?) : DFReader() {

    private var dataLen: Int
    private var pythonLength: Int
    private var numLines: BigInteger = BigInteger.valueOf(0L)

    private var bufferedReader : BufferedReader
    private var offset: Int
    private var delimiter: String
    private var offsets = hashMapOf<String,ArrayList<Int>>()
//    private val lineIndex
    private var typeSet : HashSet<String>? = null

    var formats: HashMap<String, DFFormat>
    private var idToName: HashMap<Int, String>

    private var counts = hashMapOf<String,Int>()
    private var count = 0
    private var ofs = 0

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
        endTime = lastTimestamp()
    }


    override fun rewind() {
        println("rewind()")
        super.rewind()
        // find the first valid line
        offset = 0
        bufferedReader.close()
        bufferedReader = BufferedReader(FileReader(filename))

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

    override fun getAllMessages(): ArrayList<DFMessage> {
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


    override fun getFieldLists(fields : Collection<String>) : HashMap<String, ArrayList<Pair<Long,Any>>> {
        rewind()
        var pct = 0

        val returnable = hashMapOf<String, ArrayList<Pair<Long,Any>>>()

        fields.forEach { field ->

            val typesWithThisField = arrayListOf<String>()
            formats.forEach { entry ->
                if(entry.value.columnsArr.contains(field))
                    typesWithThisField.add(entry.key)
            }

            val offsetsWithThisField = arrayListOf<Int>()
            typesWithThisField.forEach { type ->
                offsets[type]?.let {
                    offsetsWithThisField.addAll(it)
                }
            }

            val sortedOffsets = offsetsWithThisField.sorted()

            val returnableForField = ArrayList<Pair<Long,Any>>()

            sortedOffsets.forEach { nextOffset ->
                while(offset != nextOffset) {
                    skipMessage()
                }
                parseNext()?.let { m ->
                    returnableForField.add(Pair(m.timestamp, m.getAttr(field).first!!))
                    val newPct = offset / dataLen
                    if(pct != newPct) {
                        pct = newPct
                        println(newPct)
                    }
                }
            }

            returnable[field] = returnableForField
            rewind()
        }

        offset = 0
        bufferedReader.close()
        return returnable
    }


    override fun getFieldListConditional(field : String, shouldInclude: (DFMessage) -> Boolean) : ArrayList<Pair<Long,Any>> {
        rewind()
        var nullCount = 0
        var pct = 0

        val typesWithThisField = arrayListOf<String>()
        formats.forEach { entry ->
            if(entry.value.columnsArr.contains(field))
                typesWithThisField.add(entry.key)
        }

        val offsetsWithThisField = arrayListOf<Int>()
        typesWithThisField.forEach { type ->
            offsets[type]?.let {
                offsetsWithThisField.addAll(it)
            }
        }

        val sortedOffsets = offsetsWithThisField.sorted()

        val returnable = ArrayList<Pair<Long,Any>>()

        sortedOffsets.forEach { nextOffset ->
            while(offset != nextOffset) {
                skipMessage()
            }
            parseNext()?.let { m ->
                if(shouldInclude(m)) {
                    returnable.add(Pair(m.timestamp, m.getAttr(field).first!!))
                }
                val newPct = offset / dataLen
                if(pct != newPct) {
                    pct = newPct
                    println(newPct)
                }
            } ?: run {
                nullCount++
            }
        }
        offset = 0
        bufferedReader.close()
        return returnable
    }

    override fun getAllMessagesOfType(msgType : String) : ArrayList<DFMessage> {
        val returnable = arrayListOf<DFMessage>()
        rewind()
        var nullCount = 0

        offsets[msgType]?.forEach { nextOffset ->
            while(offset != nextOffset) {
                skipMessage()
            }
            parseNext()?.let {
                returnable.add(it)
            } ?: run {
                nullCount++
            }
        }

        rewind()
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
        var lineIndex = BigInteger.ZERO

        while (lineIndex < numLines) {
            parseNext()?.let { msg ->
                var mType = msg.getType()
//            var mType = line.substring(0, 4)
//            if (mType[3] == ',') {
//                mType = mType.substring(0, 3)
//            }
                if (!offsets.containsKey(mType)) {
                    counts[mType] = 0
                    offsets[mType] = arrayListOf()
                    offset = ofs
//                parseNext()
                }
                offsets[mType]?.add(ofs)

                counts[mType] = counts[mType]!! + 1

//            if (mType == "FMT") {
//                offset = ofs
//                parseNext()
//            }
//
//            if (mType == "FMTU") {
//                offset = ofs
//                parseNext()
//            }

                val line = "${msg.getType()}, ${msg.elements.joinToString(", ")}"
                ofs += line.length
                if (ofs == -1) {
                    return
                }
                ofs += 2
                val newPct = ((100.0 * ofs) / dataLen).toInt()
                progressCallback?.let { callback ->
                    if (newPct != pct) {
                        callback(newPct)
                        pct = newPct
                    }
                }
            }
            lineIndex++
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
//            mType = type_list[i]
//            if (not mType in self . counts) {
//                continue
//            }
//            if (indexes[i] >= counts[mType]) {
//                continue
//            }
//            ofs = offsets[mType][indexes[i]]
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
            offset += line.length + 2
            if (elements.size >= 2) {
                // this line is good
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

    /**
     * Get the last timestamp in the log
     */
    private fun lastTimestamp() : Long {
        rewind()
        var lastM: DFMessage? = null
        var m : DFMessage? = parseNext()
        while (m != null) {
            lastM = m
            m = parseNext()
        }
        return lastM!!.timestamp
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

    private fun skipMessage() {
        val line = bufferedReader.readLine()
        offset += line.length + 2
    }

}