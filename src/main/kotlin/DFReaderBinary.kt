import Util.Companion.null_term
import java.io.File
import java.nio.ByteBuffer

/*
 * DFReader_binary
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
 * parse a binary dataflash file
 */
class DFReaderBinary(private val filename: String, zero_based_time: Boolean?, private val progressCallback: ((Int) -> Unit)?) : DFReader() {

//    var filehandle : Any//Placeholder type
    private var dataMap : ByteArray
    var formats : HashMap<Int, DFFormat>
    private var HEAD1 : Int
    private var HEAD2 : Int
    var dataLen : Int
    private var prevType : Any? //Placeholder type
    private var offset : Int = 0
    private var remaining : Int = 0
    private var typeNums : ArrayList<Int>? = null //Placeholder type
    private var unpackers : HashMap<Int, ByteBuffer>? = null

    private var offsets =  arrayListOf<ArrayList<Int>>()
    var counts = arrayListOf<Int>()
    private var _count = 0
    private var nameToId = hashMapOf<String, Int>() //Guess
    private var idToName = hashMapOf<Int, String>() //Guess

    init {
        // read the whole file into memory for simplicity
        dataMap = File(filename).readBytes()
        dataLen = dataMap.size

        this.HEAD1 = 0xA3
        this.HEAD2 = 0x95
        this.unpackers = hashMapOf()
//        if (sys.version_info.major < 3) {
//            this.HEAD1 = chr(this.HEAD1)
//            this.HEAD2 = chr(this.HEAD2)
//        }
        formats = hashMapOf(Pair(
            0x80, DFFormat(0x80,
            "FMT",
            89,
            "BBnNZ",
            "Type,Length,Name,Format,Columns")
        ))
        this.zeroTimeBase = zero_based_time ?: false
        this.prevType = null
        this.initClock()
        this.prevType = null
        this.rewind()
        this.initArrays()
    }
    /**
     * rewind to start of log
     */
    override fun rewind() {
        super.rewind()
        this.offset = 0
        this.remaining = this.dataLen
        this.typeNums = null
        this.timestamp = 0
    }


    /**
     * initialise arrays for fast recv_match()
     */
    private fun initArrays() {
        offsets = arrayListOf()
        counts = arrayListOf()
        _count = 0
        nameToId = hashMapOf()
        idToName = hashMapOf()
        for (i in 0..256) {
            offsets.add(arrayListOf<Int>())
            counts.add(0)
        }
        val fmtType = 0x80
        var fmtuType : Int ?= null
        var ofs = 0
        var pct = 0
        val HEAD1 = this.HEAD1
        val HEAD2 = this.HEAD2
        val lengths = arrayOf(256, -1 )
        var fmt : DFFormat?
        var elements : List<Int>?

        while ( ofs+3 < dataLen) {
            val hdr : ByteArray = dataMap.copyOfRange(ofs,ofs+3)
            if (hdr[0].toInt() != HEAD1 || hdr[1].toInt() != HEAD2) {
                // avoid end of file garbage, 528 bytes has been use consistently throughout this implementation
                // but it needs to be at least 249 bytes which is the block based logging page size (256) less a 6 byte header and
                // one byte of data. Block based logs are sized in pages which means they can have up to 249 bytes of trailing space.
                if (dataLen - ofs >= 528 || dataLen < 528)
                    println(String.format("bad header 0x%02x 0x%02x at %d" , u_ord(hdr[0]), u_ord(hdr[1]), ofs))
                ofs += 1
                continue
            }
            val mtype = u_ord(hdr[2])
            offsets[mtype].add(ofs)

            if (lengths[mtype] == -1) {
                if (!formats.contains(mtype)) {
                    if (dataLen - ofs >= 528 || dataLen < 528) {
                        println(String.format("unknown msg type 0x%02x (%u) at %d" , mtype, mtype, ofs))
                    }
                    break
                }
                offset = ofs
                parseNext()
                fmt = formats[mtype]
                lengths[mtype] = fmt!!.len
            } else if ( formats[mtype]!!.instanceField != null) {
                parseNext()
            }

            counts[mtype] += 1
            val mlen = lengths[mtype]

            if (mtype == fmtType) {
                val body = dataMap.copyOfRange(ofs+3,ofs+mlen)
                if (body.size + 3 < mlen) {
                    break
                }
                fmt = formats[mtype]
                elements = listOf()//TODO struct.unpack(fmt!!.msgStruct, body))
                val fType = elements[0]
                val mFmt = DFFormat(
                    fType,
                    null_term(elements[2].toString()), elements[1],
                    null_term(elements[3].toString()), null_term(elements[4].toString()),
                    formats[fType]
                )
                formats[fType] = mFmt
                nameToId[mFmt.name] = mFmt.type
                idToName[mFmt.type] = mFmt.name
                if (mFmt.name == "FMTU") {
                    fmtuType = mFmt.type
                }
            }

            if (fmtuType != null && mtype == fmtuType) {
                val fmt2 = formats[mtype]
                val body = dataMap.copyOfRange(ofs + 3,ofs+mlen)
                if (body.size + 3 < mlen)
                    break
                elements = listOf()//struct.unpack(fmt2!!.msgStruct, body))
                val fType : Int = elements[1]
                if (fType in formats) {
                    val fmt3 = formats[fType]
                    if (fmt2!!.colhash.contains("UnitIds"))
                        fmt3?.setUnitIdsAndInstField(null_term(elements[fmt2.colhash["UnitIds"]!!].toString()))
                    if (fmt2.colhash.contains("MultIds"))
                        fmt3?.multIds = (null_term(elements[fmt2.colhash["MultIds"]!!].toString()))
                }
            }

            ofs += mlen
            progressCallback?.let { callback ->
                val newPct = (100 * ofs) // self.data_len

                if (newPct != pct) {
                    callback(newPct)
                    pct = newPct
                }
            }
        }
        for (i in 0..256) {
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
        for (i in 0..256) {
            if (counts[i] == -1)
                continue
            if (offsets[i].size == 0)
                continue
            val ofs = offsets[i][-1]
            if (ofs > highestOffset) {
                secondHighestOffset = highestOffset
                highestOffset = ofs
            } else if (ofs > secondHighestOffset) {
                secondHighestOffset = ofs
            }
        }
        offset = highestOffset
        var m = recvMsg()
        if (m == null) {
            offset = secondHighestOffset
            m = recvMsg()
        }
        return m!!.timestamp
    }

    var indexes : ArrayList<Int> = arrayListOf()
    /**
     * skip fwd to next msg matching given type set
     */
    fun skipToType(type : String) {
/*
        if (type_nums == null) {
            // always add some key msg types so we can track flightmode, params etc
            type = type.copy()
            type.update(HashSet<String>("MODE", "MSG", "PARM", "STAT"))
            indexes = arrayListOf()
            type_nums = arrayListOf()
            for (t in type) {
                if (!name_to_id.contains(t)) {
                    continue
                }
                type_nums!!.add(name_to_id[t]!!)
                indexes!!.add(0)
            }
        }
        var smallest_index = -1
        var smallest_offset = data_len
        for (i in 0..type_nums!!.size) {
            val mtype = type_nums!![i]
            if (indexes[i] >= counts[mtype]) {
                continue
            }
            var ofs = offsets[mtype][indexes[i]]
            if (ofs < smallest_offset) {
                smallest_offset = ofs
                smallest_index = i
            }
        }
        if (smallest_index >= 0) {
            indexes[smallest_index] += 1
            offset = smallest_offset
        }
        */
    }

    /**
     * read one message, returning it as an object
     */
    override fun parseNext() : DFMessage? {

        // skip over bad messages; after this loop has run msg_type
        // indicates the message which starts at self.offset (including
        // signature bytes and msg_type itself)
        var skipType : Array<Int>? = null
        var skipStart = 0
        val msgType = 0// unknown type
        while (true) {
            if (dataLen - offset < 3) {
                return null
            }

            val hdr = dataMap.copyOfRange(offset,offset+3)
            if (hdr[0].toInt() == HEAD1 && hdr[1].toInt() == HEAD2) {
                // signature found
                if (skipType != null) {
                    // emit message about skipped bytes
                    if (remaining >= 528) {
                        // APM logs often contain garbage at end
                        val skipBytes = offset - skipStart
                        println(String.format("Skipped %u bad bytes in log at offset %u, type=%s (prev=%s)", skipBytes, skipStart, skipType, prevType))
                    }
                    skipType = null
                }
                // check we recognise this message type:
                val msgType1 = u_ord(hdr[2])
                if (msgType1 in formats) {
                    // recognised message found
                    prevType = msgType1
                    break
                }
                // message was not recognised; fall through so these
                // bytes are considered "skipped".  The signature bytes
                // are easily recognisable in the "Skipped bytes"
                // message.
            }
            if (skipType == null) {
                skipType = arrayOf(u_ord(hdr[0]), u_ord(hdr[1]), u_ord(hdr[2]))
                skipStart = offset
            }
            offset += 1
            remaining -= 1
        }

        offset += 3
        remaining = dataLen - offset

        var fmt = formats[msgType]
        if (remaining < fmt!!.len - 3) {
            // out of data - can often happen half way through a message
            if (verbose) {
                println("out of data")
            }
            return null
        }
        val body = dataMap.copyOfRange(offset,offset+fmt!!.len-3)
        var elements : ArrayList<String>? = null
        try {
            if(!unpackers!!.contains(msgType)) {
                unpackers!![msgType] = ByteBuffer.wrap(byteArrayOf())//TODO struct.Struct(fmt.msgStruct).unpack
            }
            elements = arrayListOf<String>()// TODO arrayListOf(unpackers!![msgType](body))
        } catch (ex: Throwable) {
            print(ex)
            if (remaining < 528) {
                // we can have garbage at the end of an APM2 log
                return null
            }
            // we should also cope with other corruption; logs
            // transfered via DataFlash_MAVLink may have blocks of 0s
            // in them, for example
            println(String.format("Failed to parse %s/%s with len %u (remaining %u)" , fmt.name, fmt.msgStruct, body.size, remaining))
        }
        if (elements == null) {
            return parseNext()
        }
        val name = fmt.name
        // transform elements which can't be done at unpack time:
        for (aIndex in fmt.aIndexes) {
            try {
                elements[aIndex] = ""// TODO elements[aIndex].split(",").toByteArray()
            } catch (e: Throwable) {
                println(String.format("Failed to transform array: %s" , e.message))
            }
        }


        if (name == "FMT") {
            // add to formats
            // name, len, format, headings
            try {
                val fType = elements[0].toInt()
                val mFmt = DFFormat(
                    fType,
                    null_term(elements[2]),
                    elements[1].toInt(),
                    null_term(elements[3]),
                    null_term(elements[4]),
                    formats[fType]
                )
                formats[fType] = mFmt
            } catch (e: Throwable) {
                return parseNext()
            }
        }

        offset += fmt.len - 3
        remaining = dataLen - offset
        val m = DFMessage(fmt, elements, true, this)

        if (m.fmt.name == "FMTU") {
            // add to units information
            val fmtType = elements[0].toInt()
            val unitIds = elements[1]
            val multIds = elements[2]
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

        if(endTime < m.timestamp) {
            endTime = m.timestamp
        }

        return m
    }
}