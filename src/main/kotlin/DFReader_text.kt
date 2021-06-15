import java.io.File

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
    var filename : String
    var zero_time_base : Boolean
    var progress_callback : ProgressCallback?
    var fileLines : List<String>
    var data_len : Int
    var data_map : Any?
    var offset = Int
    var delimeter : String
    var offsets = arrayOf<Int>()

    var formats : HashMap<String, DFFormat>
    var id_to_name : HashMap<Int, String>

    init {
        this.filename = filename
        this.zero_time_base = zero_based_time ?: false
        progress_callback = progressCallback
//        DFReader.__init__()
        // read the whole file into memory for simplicity
        fileLines = File(filename).readLines()
        data_len = fileLines.size
        data_map = mmap.mmap(filehandle.fileno(), data_len, null, mmap.ACCESS_READ)
        offset = 0
        delimeter = ", "

       formats = hashMapOf(Pair("FMT", DFFormat(0x80, "FMT", 89, "BBnNZ", "Type,Length,Name,Format,Columns")))
        id_to_name = hashMapOf(Pair( 0x80 , "FMT" ))
        _rewind()
        _zero_time_base = zero_time_base
        init_clock()
        _rewind()
        init_arrays(progress_callback)
    }

    /**
     * rewind to start of log
     */
     fun _rewind(){
        super._rewind()
        // find the first valid line
        offset = data_map.find(b"FMT, ")
        if (offset == -1) {
            offset = data_map.find(b"FMT,")
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

    var counts = arrayOf<Int>()
    var _count = 0
    var ofs = 0
    /**
     * initialise arrays for fast recv_match()
     */
    fun init_arrays(progress_callback : ProgressCallback?) {
        offsets = arrayOf<Int>()
        counts = arrayOf<Int>()
        _count = 0
        ofs = offset
        var pct = 0

        while( ofs + 16 < data_len) {
            mtype = data_map[ofs:ofs+4]
            if mtype[3] == b',':
            mtype = mtype[0:3]
            if not mtype in self . offsets {
                counts[mtype] = 0
                offsets[mtype] = []
                offset = ofs
                _parse_next()
            }
            offsets[mtype].append(ofs)

            counts[mtype] += 1

            if (mtype == "FMT") {
                offset = ofs
                _parse_next()
            }

            if (mtype == "FMTU") {
                offset = ofs
                _parse_next()
            }

            ofs = data_map.find(b"\n", ofs)
            if (ofs == -1) {
                break
            }
            ofs += 1
            new_pct = (100 * ofs) // data_len
            if (progress_callback != null && new_pct != pct) {
                progress_callback.update(new_pct)
                pct = new_pct
            }
        }

        for (mtype in counts.keys()) {
            _count += counts[mtype]
        }
        offset = 0
    }

    /**
     * skip fwd to next msg matching given type set
     */
    fun skip_to_type(type) {

        if (type_list is null) {
// always add some key msg types so we can track flightmode, params etc
            type_list = type.copy()
            type_list.update(set(["MODE", "MSG", "PARM", "STAT"]))
            type_list = list(type_list)
            indexes = []
            type_nums = []
            for (t in type_list) {
                indexes.append(0)
            }
        }
        smallest_index = -1
        smallest_offset = data_len
        for (i in range(len(type_list))) {
            mtype = type_list[i]
            if (not mtype in self . counts) {
                continue
            }
            if (indexes[i] >= counts[mtype]) {
                continue
            }
            ofs = offsets[mtype][indexes[i]]
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
    fun _parse_next() {

        while (true) {
            endline = data_map.find(b'\n', offset)
            if (endline == -1) {
                endline = data_len
            }
            if (endline < offset) {
                break
            }
            s = data_map[offset:endline].rstrip()
            if (sys.version_info.major >= 3)
                s = s.decode("utf-8")
            elements = s.split(delimeter)
            offset = endline + 1
            if (len(elements) >= 2) {
                // this_line is good
                break
            }
        }

        if (offset > data_len) {
            return null
        }

// cope with empty structures
        if (len(elements) == 5 && elements[-1] == ',') {
            elements[-1] = ''
            elements.append('')
        }

        percent = 100.0 * (offset / float(data_len))

        msg_type = elements[0]

        if (msg_type not in formats) {
            return _parse_next()
        }

        fmt = formats[msg_type]

        if (len(elements) < len(fmt.format)+1) {
            // not enough columns
            return _parse_next()
        }

        elements = elements[1:]

        name = fmt.name.rstrip("\0")
        if (name == "FMT") {
            // add to formats
            // name, len, format, headings
            ftype = int(elements[0])
            fname = elements[2]
            if (delimeter == ",") {
                elements = elements[0:4]+[",".join(elements[4:])]
            }
            columns = elements[4]
            if (fname == "FMT" && columns == "Type,Length,Name,Format") {
                // some logs have the 'Columns' column missing from text logs
                columns = "Type,Length,Name,Format,Columns"
            }
            new_fmt = DFFormat(
                ftype,
                fname,
                int(elements[1]),
                elements[3],
                columns,
                oldfmt = formats.get(ftype, null)
            )
            formats[fname] = new_fmt
            id_to_name[ftype] = fname
        }
        try {
            m = DFMessage(fmt, elements, false, self)
        } catch (valError: ValueError) {
            return _parse_next()
        }

        if (m.get_type() == "FMTU") {
            fmtid = getattr(m, "FmtType", null)
            if (fmtid != null && fmtid in id_to_name) {
                fmtu = formats[id_to_name[fmtid]]
                fmtu.set_unit_ids(getattr(m, "UnitIds", null))
                fmtu.set_mult_ids(getattr(m, "MultIds", null))
            }
        }
        _add_msg(m)

        return m
    }

    /**
     * get the last timestamp in the log
     */
    fun last_timestamp() {
        highest_offset = 0
        for (mtype in counts.keys()) {
            if (len(offsets[mtype]) == 0) {
                continue
            }
            ofs = offsets[mtype][-1]
            if (ofs > highest_offset) {
                highest_offset = ofs
            }
        }
        offset = highest_offset
        m = recv_msg()
        return m._timestamp
    }

}