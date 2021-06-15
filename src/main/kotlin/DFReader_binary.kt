import java.io.File

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
class DFReader_binary(filename: String, zero_based_time: Boolean?, progress_callback: ProgressCallback?) : DFReader() {
    var filehandle : Any//Placeholder type
    var formats : HashMap<Int, DFFormat>
    var HEAD1 : Int
    var HEAD2 : Int
    var data_len : Int
    var prev_type : Any? //Placeholder type
    var offset : Int = 0
    var remaining : Int = 0
    var type_nums : ArrayList<Int>? = null //Placeholder type
    var unpackers : HashMap<Any, Any>? = null

    init {
//        DFReader.__init__(self)
        // read the whole file into memory for simplicity
        filehandle = File(filename, 'r')
        this.filehandle.seek(0, 2)
        this.data_len = this.filehandle.tell()
        this.filehandle.seek(0)
        if (platform.system() == "Windows") {
            this.data_map = mmap.mmap(this.filehandle.fileno(), this.data_len, null, mmap.ACCESS_READ)
        } else {
            this.data_map = mmap.mmap(this.filehandle.fileno(), this.data_len, mmap.MAP_PRIVATE, mmap.PROT_READ)
        }
        this.HEAD1 = 0xA3
        this.HEAD2 = 0x95
        this.unpackers = hashMapOf()
        if (sys.version_info.major < 3) {
            this.HEAD1 = chr(this.HEAD1)
            this.HEAD2 = chr(this.HEAD2)
        }
        formats = hashMapOf(Pair(
            0x80, DFFormat(0x80,
            "FMT",
            89,
            "BBnNZ",
            "Type,Length,Name,Format,Columns")
        ))
        this._zero_time_base = zero_based_time
        this.prev_type = null
        this.init_clock()
        this.prev_type = null
        this._rewind()
        this.init_arrays(progress_callback)
    }
    /**
     * rewind to start of log
     */
    fun _rewind() {
        super._rewind()
        this.offset = 0
        this.remaining = this.data_len
        this.type_nums = null
        this.timestamp = 0L
    }

    /**
     * rewind to start of log
     */
    fun rewind() {
        this._rewind()
    }

    var offsets = arrayListOf<Array<Int>>() //Guess
    var counts = arrayListOf<Int>() //Guess
    var _count = 0
    var name_to_id = hashMapOf<String, Int>() //Guess
    var id_to_name = hashMapOf<Int, String>() //Guess
    /**
     * initialise arrays for fast recv_match()
     */
    fun init_arrays( progress_callback: ProgressCallback?) {
        offsets = arrayListOf()
        counts = arrayListOf()
        _count = 0
        name_to_id = hashMapOf()
        id_to_name = hashMapOf()
        for (i in 0..256) {
            offsets.add(arrayOf<Int>())
            counts.add(0)
        }
        var fmt_type = 0x80
        var fmtu_type : Int?= null
        var ofs = 0
        var pct = 0
        var HEAD1 = this.HEAD1
        var HEAD2 = this.HEAD2
        var lengths = arrayOf(-1 * 256)
        var fmt : DFFormat?
        var elements : List<Int>?

        while ( ofs+3 < data_len) {
            val hdr = data_map[ofs:ofs+3]
            if (hdr[0] != HEAD1 || hdr[1] != HEAD2) {
                // avoid end of file garbage, 528 bytes has been use consistently throughout this implementation
                // but it needs to be at least 249 bytes which is the block based logging page size (256) less a 6 byte header and
                // one byte of data. Block based logs are sized in pages which means they can have up to 249 bytes of trailing space.
                if (data_len - ofs >= 528 || data_len < 528)
                    println(String.format("bad header 0x%02x 0x%02x at %d" , (u_ord(hdr[0]), u_ord(hdr[1]), ofs)))
                ofs += 1
                continue
            }
            val mtype = u_ord(hdr[2])
            offsets[mtype].add(ofs)

            if (lengths[mtype] == -1) {
                if (!formats.contains(mtype)) {
                    if (data_len - ofs >= 528 || data_len < 528) {
                        println(String.format("unknown msg type 0x%02x (%u) at %d" , mtype, mtype, ofs))
                    }
                    break
                }
                offset = ofs
                _parse_next()
                fmt = formats[mtype]
                lengths[mtype] = fmt.len
            } else if ( formats[mtype].instance_field != null) {
                _parse_next()
            }

            counts[mtype] += 1
            var mlen = lengths[mtype]

            if (mtype == fmt_type) {
                var body = data_map[ofs + 3:ofs+mlen]
                if (body.size + 3 < mlen) {
                    break
                }
                fmt = formats[mtype]
                elements = listOf(struct.unpack(fmt.msg_struct, body))
                val ftype = elements[0]
                var mfmt = DFFormat(
                    ftype,
                    null_term(elements[2]), elements[1],
                    null_term(elements[3]), null_term(elements[4]),
                    oldfmt = formats.get(ftype, null)
                )
                formats[ftype] = mfmt
                name_to_id[mfmt.name] = mfmt.type
                id_to_name[mfmt.type] = mfmt.name
                if (mfmt.name == "FMTU") {
                    fmtu_type = mfmt.type
                }
            }

            if (fmtu_type != null && mtype == fmtu_type) {
                val fmt = formats[mtype]
                val body = data_map[ofs + 3:ofs+mlen]
                if (body.size + 3 < mlen)
                    break
                elements = listOf(struct.unpack(fmt.msg_struct, body))
                val ftype = int(elements[1])
                if (ftype in formats) {
                    val fmt2 = formats[ftype]
                    if (fmt!!.colhash.contains("UnitIds"))
                        fmt2?.set_unit_ids(null_term(elements[fmt.colhash["UnitIds"]]))
                    if (fmt.colhash.contains("MultIds"))
                        fmt2?.set_mult_ids(null_term(elements[fmt.colhash["MultIds"]]))
                }
            }

            ofs += mlen
            if (progress_callback != null) {
                val new_pct = (100 * ofs) // self.data_len
                if (new_pct != pct) {
                    progress_callback.update(new_pct)
                    pct = new_pct
                }
            }
        }
        for (i in 0..256) {
            _count += counts[i]
        }
        offset = 0
    }
    /**
     * get the last timestamp in the log
     */
    fun last_timestamp() : Long{
        var highest_offset = 0
        var second_highest_offset = 0
        for (i in 0..256) {
            if (counts[i] == -1)
                continue
            if (offsets[i].size == 0)
                continue
            val ofs = offsets[i][-1]
            if (ofs > highest_offset) {
                second_highest_offset = highest_offset
                highest_offset = ofs
            } else if (ofs > second_highest_offset) {
                second_highest_offset = ofs
            }
        }
        offset = highest_offset
        var m = recv_msg()
        if (m == null) {
            offset = second_highest_offset
            m = recv_msg()
        }
        return m!!._timestamp
    }

    var indexes : ArrayList<Int> = arrayListOf()
    /**
     * skip fwd to next msg matching given type set
     */
    fun skip_to_type( type) {

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
    }

    /**
     * read one message, returning it as an object
     */
    override fun _parse_next() : DFMessage? {

        // skip over bad messages; after this loop has run msg_type
        // indicates the message which starts at self.offset (including
        // signature bytes and msg_type itself)
        var skip_type = null
        var skip_start = 0
        var msg_type = 0// unknown type
        while (true) {
            if (data_len - offset < 3) {
                return null
            }

            var hdr = data_map[offset:offset+3]
            if (hdr[0] == HEAD1 && hdr[1] == HEAD2) {
                // signature found
                if (skip_type != null) {
                    // emit message about skipped bytes
                    if (remaining >= 528) {
                        // APM logs often contain garbage at end
                        var skip_bytes = offset - skip_start
                        println(String.format("Skipped %u bad bytes in log at offset %u, type=%s (prev=%s)", skip_bytes, skip_start, skip_type, prev_type))
                    }
                    skip_type = null
                }
                // check we recognise this message type:
                var msg_type = u_ord(hdr[2])
                if (msg_type in formats) {
                    // recognised message found
                    prev_type = msg_type
                    break
                }
                // message was not recognised; fall through so these
                // bytes are considered "skipped".  The signature bytes
                // are easily recognisable in the "Skipped bytes"
                // message.
            }
            if (skip_type == null) {
                skip_type = (u_ord(hdr[0]), u_ord(hdr[1]), u_ord(hdr[2]))
                skip_start = offset
            }
            offset += 1
            remaining -= 1
        }

        offset += 3
        remaining = data_len - offset

        var fmt = formats[msg_type]
        if (remaining < fmt.len - 3) {
            // out of data - can often happen half way through a message
            if (verbose) {
                println("out of data")
            }
            return null
        }
        var body = data_map[offset:offset+fmt.len-3]
        var elements : List<Any>? = null
        try {
            if(!unpackers!!.contains(msg_type)) {
                unpackers!![msg_type] = struct.Struct(fmt.msg_struct).unpack
            }
            elements = listOf(unpackers[msg_type](body))
        } catch (ex: Throwable) {
            print(ex)
            if (remaining < 528) {
                // we can have garbage at the end of an APM2 log
                return null
            }
            // we should also cope with other corruption; logs
            // transfered via DataFlash_MAVLink may have blocks of 0s
            // in them, for example
            println(String.format("Failed to parse %s/%s with len %u (remaining %u)" , fmt!!.name, fmt.msg_struct, body.size, remaining))
        }
        if (elements == null) {
            return _parse_next()
        }
        val name = fmt!!.name
        // transform elements which can't be done at unpack time:
        for (a_index in fmt.a_indexes) {
            try {
                elements[a_index] = array.array('h', elements[a_index])
            } catch (e: Throwable) {
                println(String.format("Failed to transform array: %s" , e.message))
            }
        }


        if (name == "FMT") {
            // add to formats
            // name, len, format, headings
            try {
                val ftype = elements[0] as Int
                val mfmt = DFFormat(
                    ftype,
                    null_term(elements[2]),
                    elements[1] as Int,
                    null_term(elements[3]),
                    null_term(elements[4]),
                    oldfmt = formats.get(ftype, null)
                )
                formats[ftype] = mfmt
            } catch (e: Throwable) {
                return _parse_next()
            }
        }

        offset += fmt.len - 3
        remaining = data_len - offset
        val m = DFMessage(fmt, elements, true)

        if (m.fmt.name == "FMTU") {
            // add to units information
            val FmtType = (elements[0] as String).toInt()
            val UnitIds = elements[1] as String
            val MultIds = elements[2] as String
            if (FmtType in formats) {
                fmt = formats[FmtType]
                fmt!!.set_unit_ids(UnitIds)
                fmt.set_mult_ids(MultIds)
            }
        }

        try {
            _add_msg(m)
        } catch (e: Throwable) {
            println(String.format("bad msg at offset %s, %s", offset, e.message))
        }
        percent = (100.0 * (offset / data_len)).toFloat()

        return m
    }

}