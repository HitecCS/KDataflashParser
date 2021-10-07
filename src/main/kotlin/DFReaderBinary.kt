import Util.nullTerm
import java.io.File


/*
 * DFReaderBinary
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
 * Parse a binary dataflash file
 */
@OptIn(ExperimentalUnsignedTypes::class)
class DFReaderBinary(val filename: String, zero_based_time: Boolean?, private val progressCallback: ((Int) -> Unit)?) : DFReader() {

    // read the whole file into memory for simplicity
    private var dataMap : UByteArray//= File(filename).readBytes() //: IntArray
    private var formats : HashMap<Int, DFFormat> = hashMapOf(Pair(
        0x80, DFFormat(0x80,
        "FMT",
        89,
        "BBnNZ",
        "Type,Length,Name,Format,Columns")
    ))
    private val HEAD1 : UByte = 0xA3.toUByte() //2s -93
    private val HEAD2 : UByte = 0x95.toUByte() //2s -107
    private var dataLen : Int
    private var prevType : Any? //Placeholder type
    private var offset : Int = 0
    private var remaining : Int = 0
    private var typeNums : ArrayList<Int>? = null //Placeholder type
    private val unpackers = hashMapOf<Int, (UByteArray) -> Array<String>>()

    private var offsets =  arrayListOf<ArrayList<Int>>()
    private var counts = arrayListOf<Int>()
    private var _count = 0
    private var nameToId = hashMapOf<String, Int>() //Guess
    private var idToName = hashMapOf<Int, String>() //Guess
    private var indices : ArrayList<Int> = arrayListOf()

    init {
        val bytes = File(filename).readBytes()
        dataLen = bytes.size
        dataMap = bytes.toUByteArray()

        zeroTimeBase = zero_based_time ?: false
        prevType = null
        initClock()
        prevType = null
        rewind()
        initArrays()
        endTime = lastTimestamp()
    }

    /**
     * Rewind to start of log
     */
    override fun rewind() {
        super.rewind()
        offset = 0
        remaining = dataLen
        typeNums = null
        timestamp = 0
    }

    override fun getAllMessages(): ArrayList<DFMessage> {
        val returnable = arrayListOf<DFMessage>()
        rewind()
        var pct = 0
        var nullCount = 0
        while (dataLen - offset > 3) {
            parseNext()?.let {
                returnable.add(it)
            } ?: run {
                nullCount++
            }
            val newPct = (offset * 100)/ dataLen
            if(pct != newPct) {
                pct = newPct
                println(newPct)
            }
        }
        rewind()
        return returnable
    }

    override fun getFieldLists(fields: Collection<String>): HashMap<String, ArrayList<Pair<Long, Any>>> {
        rewind()
        var pct = 0

        val returnable = hashMapOf<String, ArrayList<Pair<Long,Any>>>()
        fields.forEach {
            returnable[it] = arrayListOf()
        }

        while (dataLen - offset > 3) {//dataMap.length

            parseNext()?.let { m ->
                val intersection = m.fieldnames intersect fields
                intersection.forEach {
                    returnable[it]?.add(Pair(m.timestamp, m.getAttr(it).first!!))
                }
            }
            val newPct = (offset * 100)/ dataLen
            if(pct != newPct) {
                pct = newPct
                println(newPct)
            }
        }
        rewind()
        return returnable
    }

    override fun getFieldListConditional(
        field: String,
        shouldInclude: (DFMessage) -> Boolean
    ): ArrayList<Pair<Long, Any>> {
        rewind()
        var pct = 0

        val returnable = ArrayList<Pair<Long,Any>>()

        while (dataLen - offset > 3) {

            parseNext()?.let { m ->
                if(m.fieldnames.contains(field) && shouldInclude(m)) {
                    returnable.add(Pair(m.timestamp, m.getAttr(field).first!!))
                }
            }
            val newPct = (offset * 100)/ dataLen
            if(pct != newPct) {
                pct = newPct
                println(newPct)
            }
        }
        rewind()
        return returnable
    }


    override fun parseNext() : DFMessage? {

        // skip over bad messages; after this loop has run msg_type
        // indicates the message which starts at self.offset (including
        // signature bytes and msg_type itself)
        var skipType : Array<Int>? = null
        var skipStart = 0
        var msgType: Int
        while (true) {
            if (dataLen - offset < 3) {
                return null
            }

            val hdr = dataMap.copyOfRange(offset,offset+3)
            if (hdr[0] == HEAD1 && hdr[1] == HEAD2) {
                // signature found
                if (skipType != null) {
                    // emit message about skipped bytes
                    if (remaining >= 528) {
                        // APM logs often contain garbage at end
                        val skipBytes = offset - skipStart
                        println(String.format("Skipped %s bad bytes in log at offset %s, type=%s (prev=%s)", skipBytes, skipStart, skipType, prevType))
                    }
                    skipType = null
                }
                // check we recognise this message type:
                msgType = hdr[2].toInt()
                if (msgType in formats) {
                    // recognised message found
                    prevType = msgType
                    break
                }
                // message was not recognised; fall through so these
                // bytes are considered "skipped".  The signature bytes
                // are easily recognisable in the "Skipped bytes"
                // message.
            }
            if (skipType == null) {
                skipType = arrayOf(hdr[0].toInt(), hdr[1].toInt(), hdr[2].toInt())// arrayOf(uOrd(hdr[0]), uOrd(hdr[1]), uOrd(hdr[2]))
                skipStart = offset
            }
            offset += 1
            remaining -= 1
        }

        offset += 3
        remaining = dataLen - offset

        var fmt = formats[msgType]
        if (remaining < fmt!!.len - 3) {
            // out of data - can often happen halfway through a message
            if (verbose) {
                println("out of data")
            }
            return null
        }
        val body = dataMap.copyOfRange(offset, offset+ fmt.len-3)
        var elements : Array<String>? = null
        try {
//            if(!unpackers.contains(msgType)) {
//                unpackers[msgType] = { format: String, array : UByteArray -> Struct.unpack(format, array) }
//            }
            //elements = unpackers[msgType]!!(body, fmt)
            elements = Struct.unpack(fmt.format, body)
        } catch (ex: Throwable) {
            println(ex)
            if (remaining < 528) {
                // we can have garbage at the end of an APM2 log
                return null
            }
            // we should also cope with other corruption; logs
            // transferred via DataFlash_MAVLink may have blocks of 0s
            // in them, for example
            println(String.format("Failed to parse %s/%s with len %s (remaining %s)" , fmt.name, fmt.msgStruct, body.size, remaining))
        }
        if (elements == null) {
            return parseNext()
        }
        val name = fmt.name
        // transform elements which can't be done at unpack time:
        for (aIndex in fmt.aIndexes) {
            try {
                elements[aIndex] = ""//elements[aIndex].split(",")
            } catch (e: Throwable) {
                println(String.format("Failed to transform array: %s" , e.message))
            }
        }


        if (name == "FMT") {
            // add to hashmap "formats"
            // name, len, format, headings
            try {
                val fType = elements[0].toInt()
                val mFmt = DFFormat(
                    fType,
                    nullTerm(elements[2]),
                    elements[1].toInt(),
                    nullTerm(elements[3]),
                    nullTerm(elements[4]),
                    formats[fType]
                )
                formats[fType] = mFmt
            } catch (e: Throwable) {
                return parseNext()
            }
        }

        offset += fmt.len - 3
        remaining = dataLen - offset
        val m = DFMessage(fmt, ArrayList(elements.toList()), true, this)

        if (m.fmt.name == "FMTU") {
            // add to units information
            val fmtType = elements[1].toInt()
            val unitIds = elements[2]
            val multIds = elements[3]
            if (fmtType in formats) {
                fmt = formats[fmtType]
                fmt?.apply {
                    setUnitIdsAndInstField(unitIds)
                    this.multIds = multIds
                }
            }
        }

        try {
            addMsg(m)
        } catch (e: Throwable) {
            println(String.format("bad msg at offset %s, %s", offset, e.message))
        }
        percent = (100.0 * (offset / dataLen)).toFloat()

        return m
    }

    /**
     * Initialise arrays for fast recv_match()
     */
    private fun initArrays() {
        offsets = arrayListOf()
        counts = arrayListOf()
        _count = 0
        nameToId = hashMapOf()
        idToName = hashMapOf()
        for (i in 0 until 256) {
            offsets.add(arrayListOf())
            counts.add(0)
        }
        val fmtType = 0x80
        var fmtuType : Int ?= null
        var ofs = 0
        var pct = 0
        val lengths = IntArray(256) { -1 }
        var fmt : DFFormat?
        var elements : Array<String>?

        while ( ofs+3 < dataLen) {
            val hdr = dataMap.copyOfRange(ofs,ofs+3)
            if (hdr[0] != HEAD1 || hdr[1] != HEAD2) {
                // avoid end of file garbage, 528 bytes has been use consistently throughout this implementation,
                // but it needs to be at least 249 bytes which is the block based logging page size (256) less a 6 byte header and
                // one byte of data. Block based logs are sized in pages which means they can have up to 249 bytes of trailing space.
                if (dataLen - ofs >= 528 || dataLen < 528)
                    println(String.format("bad header %s %s at %d" , hdr[0].toString(), hdr[1].toString(), ofs))//uOrd(hdr[0]), uOrd(hdr[1]), ofs))
                ofs += 1
                continue
            }
            val mType = hdr[2].toInt()//uOrd(hdr[2])
            offsets[mType].add(ofs)

            if (lengths[mType] == -1) {
                if (!formats.contains(mType)) {
                    if (dataLen - ofs >= 528 || dataLen < 528) {
                        println(String.format("unknown msg type 0x%02x (%s) at %d" , mType, mType, ofs))
                    }
                    break
                }
                offset = ofs
                parseNext()
                fmt = formats[mType]
                lengths[mType] = fmt!!.len
            } else if ( formats[mType]!!.instanceField != null) {
                parseNext()
            }

            counts[mType] += 1
            val mLen = lengths[mType]

            if (mType == fmtType) {
                val body = dataMap.copyOfRange(ofs+3,ofs+mLen)
                if (body.size + 3 < mLen) {
                    break
                }
                fmt = formats[mType]
                elements = Struct.unpack(fmt!!.format, body)
                val fType = elements[0].toInt()
                val mFmt = DFFormat(
                    fType,
                    nullTerm(elements[2]), elements[1].toInt(),
                    nullTerm(elements[3]), nullTerm(elements[4].toString()),
                    formats[fType]
                )
                formats[fType] = mFmt
                nameToId[mFmt.name] = mFmt.type
                idToName[mFmt.type] = mFmt.name
                if (mFmt.name == "FMTU") {
                    fmtuType = mFmt.type
                }
            }

            if (fmtuType != null && mType == fmtuType) {
                val fmt2 = formats[mType]
                val body = dataMap.copyOfRange(ofs + 3,ofs+mLen)
                if (body.size + 3 < mLen)
                    break
                elements = Struct.unpack(fmt2!!.format, body)
                val fType : Int = elements[1].toInt()
                if (fType in formats) {
                    val fmt3 = formats[fType]
                    if (fmt2.colhash.contains("UnitIds"))
                        fmt3?.setUnitIdsAndInstField(nullTerm(elements[fmt2.colhash["UnitIds"]!!]))
                    if (fmt2.colhash.contains("MultIds"))
                        fmt3?.multIds = (nullTerm(elements[fmt2.colhash["MultIds"]!!]))
                }
            }

            ofs += mLen
            val newPct = ((100.0 * ofs) / dataLen).toInt()
            progressCallback?.let { callback ->
//                val newPct = (100 * ofs) // self.data_len

                if (newPct != pct) {
                    callback(newPct)
                    pct = newPct
                }
            }
        }
        for (i in 0 until 256) {
            _count += counts[i]
        }
        offset = 0
    }

    /**
     * Get the last timestamp in the log
     *
     */
    private fun lastTimestamp() : Long {
        var highestOffset = 0
        var secondHighestOffset = 0
        for (i in 0 until 256) {
            if (counts[i] == -1)
                continue
            if (offsets[i].size == 0)
                continue
            val ofs = offsets[i][offsets[i].size - 1]
            if (ofs > highestOffset) {
                secondHighestOffset = highestOffset
                highestOffset = ofs
            } else if (ofs > secondHighestOffset) {
                secondHighestOffset = ofs
            }
        }
        offset = highestOffset
        var m = parseNext()
        if (m == null) {
            offset = secondHighestOffset
            m = parseNext()
        }
        return m!!.timestamp
    }

    /**
     * skip fwd to next msg matching given type set
     */
    private fun skipToType(type : String) {
/*
        if (typeNums == null) {
            // always add some key msg types, so we can track flightmode, params etc.
            type = type.copy()
            type.update(HashSet<String>("MODE", "MSG", "PARM", "STAT"))
            indices = arrayListOf()
            typeNums = arrayListOf()
            for (t in type) {
                if (!name_to_id.contains(t)) {
                    continue
                }
                typeNums!!.add(name_to_id[t]!!)
                indices!!.add(0)
            }
        }
        var smallest_index = -1
        var smallest_offset = data_len
        for (i in 0..typeNums!!.size) {
            val mType = typeNums!![i]
            if (indices[i] >= counts[mType]) {
                continue
            }
            var ofs = offsets[mType][indices[i]]
            if (ofs < smallest_offset) {
                smallest_offset = ofs
                smallest_index = i
            }
        }
        if (smallest_index >= 0) {
            indices[smallest_index] += 1
            offset = smallest_offset
        }
        */
    }

}