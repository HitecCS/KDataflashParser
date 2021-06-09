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
class DFReader_binary() : DFReader() {
    init__( filename, zero_time_base=false, progress_callback=null):
    DFReader.__init__(self)
    // read the whole file into memory for simplicity
    this.filehandle = open(filename, 'r')
    this.filehandle.seek(0, 2)
    this.data_len = this.filehandle.tell()
    this.filehandle.seek(0)
    if platform.system() == "Windows":
    this.data_map = mmap.mmap(this.filehandle.fileno(), this.data_len, null, mmap.ACCESS_READ)
    else:
    this.data_map = mmap.mmap(this.filehandle.fileno(), this.data_len, mmap.MAP_PRIVATE, mmap.PROT_READ)

    this.HEAD1 = 0xA3
    this.HEAD2 = 0x95
    this.unpackers = {}
    if sys.version_info.major < 3:
    this.HEAD1 = chr(this.HEAD1)
    this.HEAD2 = chr(this.HEAD2)
    this.formats = {
        0x80: DFFormat(0x80,
        "FMT",
        89,
        "BBnNZ",
        "Type,Length,Name,Format,Columns")
    }
    this._zero_time_base = zero_time_base
    this.prev_type = null
    this.init_clock()
    this.prev_type = null
    this._rewind()
    this.init_arrays(progress_callback)

    /**
     * rewind to start of log
     */
    fun _rewind():
            DFReader._rewind(self)
    this.offset = 0
    this.remaining = this.data_len
    this.type_nums = null
    this.timestamp = 0

    /**
     * rewind to start of log
     */
    fun rewind() {
        this._rewind()
    }

    /**
     * initialise arrays for fast recv_match()
     */
    fun init_arrays( progress_callback=null):
            self.offsets = []
    self.counts = []
    self._count = 0
    self.name_to_id = {}
    self.id_to_name = {}
    for i in range(256):
    self.offsets.append([])
    self.counts.append(0)
    fmt_type = 0x80
    fmtu_type = null
    ofs = 0
    pct = 0
    HEAD1 = self.HEAD1
    HEAD2 = self.HEAD2
    lengths = [-1] * 256

    while ofs+3 < self.data_len:
    hdr = self.data_map[ofs:ofs+3]
    if hdr[0] != HEAD1 or hdr[1] != HEAD2:
    // avoid end of file garbage, 528 bytes has been use consistently throughout this implementation
    // but it needs to be at least 249 bytes which is the block based logging page size (256) less a 6 byte header and
    // one byte of data. Block based logs are sized in pages which means they can have up to 249 bytes of trailing space.
    if self.data_len - ofs >= 528 or self.data_len < 528:
    print("bad header 0x%02x 0x%02x at %d" % (u_ord(hdr[0]), u_ord(hdr[1]), ofs), file=sys.stderr)
    ofs += 1
    continue
    mtype = u_ord(hdr[2])
    self.offsets[mtype].append(ofs)

    if lengths[mtype] == -1:
    if not mtype in self.formats:
    if self.data_len - ofs >= 528 or self.data_len < 528:
    print("unknown msg type 0x%02x (%u) at %d" % (mtype, mtype, ofs),
    file=sys.stderr)
    break
    self.offset = ofs
    self._parse_next()
    fmt = self.formats[mtype]
    lengths[mtype] = fmt.len
} else if ( self.formats[mtype].instance_field != null:
self._parse_next()

self.counts[mtype] += 1
mlen = lengths[mtype]

if mtype == fmt_type:
body = self.data_map[ofs+3:ofs+mlen]
if len(body)+3 < mlen:
break
fmt = self.formats[mtype]
elements = list(struct.unpack(fmt.msg_struct, body))
ftype = elements[0]
mfmt = DFFormat(
ftype,
null_term(elements[2]), elements[1],
null_term(elements[3]), null_term(elements[4]),
oldfmt=self.formats.get(ftype,null))
self.formats[ftype] = mfmt
self.name_to_id[mfmt.name] = mfmt.type
self.id_to_name[mfmt.type] = mfmt.name
if mfmt.name == "FMTU":
fmtu_type = mfmt.type

if fmtu_type != null && mtype == fmtu_type:
fmt = self.formats[mtype]
body = self.data_map[ofs+3:ofs+mlen]
if len(body)+3 < mlen:
break
elements = list(struct.unpack(fmt.msg_struct, body))
ftype = int(elements[1])
if ftype in self.formats:
fmt2 = self.formats[ftype]
if "UnitIds" in fmt.colhash:
fmt2.set_unit_ids(null_term(elements[fmt.colhash["UnitIds"]]))
if "MultIds" in fmt.colhash:
fmt2.set_mult_ids(null_term(elements[fmt.colhash["MultIds"]]))

ofs += mlen
if progress_callback != null:
new_pct = (100 * ofs) // self.data_len
if new_pct != pct:
progress_callback(new_pct)
pct = new_pct

for i in range(256):
self._count += self.counts[i]
self.offset = 0

/**
 * get the last timestamp in the log
 */
fun last_timestamp():
        highest_offset = 0
second_highest_offset = 0
for i in range(256):
if self.counts[i] == -1:
continue
if len(self.offsets[i]) == 0:
continue
ofs = self.offsets[i][-1]
if ofs > highest_offset:
second_highest_offset = highest_offset
highest_offset = ofs
} else if ( ofs > second_highest_offset:
second_highest_offset = ofs
self.offset = highest_offset
m = self.recv_msg()
if m is null:
self.offset = second_highest_offset
m = self.recv_msg()
return m._timestamp

/**
 * skip fwd to next msg matching given type set
 */
fun skip_to_type( type):

        if self.type_nums is null:
// always add some key msg types so we can track flightmode, params etc
type = type.copy()
type.update(set(["MODE","MSG","PARM","STAT"]))
self.indexes = []
self.type_nums = []
for t in type:
if not t in self.name_to_id:
continue
self.type_nums.append(self.name_to_id[t])
self.indexes.append(0)
smallest_index = -1
smallest_offset = self.data_len
for i in range(len(self.type_nums)):
mtype = self.type_nums[i]
if self.indexes[i] >= self.counts[mtype]:
continue
ofs = self.offsets[mtype][self.indexes[i]]
if ofs < smallest_offset:
smallest_offset = ofs
smallest_index = i
if smallest_index >= 0:
self.indexes[smallest_index] += 1
self.offset = smallest_offset

/**
 * read one message, returning it as an object
 */
fun _parse_next():

// skip over bad messages; after this loop has run msg_type
// indicates the message which starts at self.offset (including
// signature bytes and msg_type itself)
        skip_type = null
skip_start = 0
while true:
if self.data_len - self.offset < 3:
return null

hdr = self.data_map[self.offset:self.offset+3]
if hdr[0] == self.HEAD1 && hdr[1] == self.HEAD2:
// signature found
if skip_type != null:
// emit message about skipped bytes
if self.remaining >= 528:
// APM logs often contain garbage at end
skip_bytes = self.offset - skip_start
print("Skipped %u bad bytes in log at offset %u, type=%s (prev=%s)" %
(skip_bytes, skip_start, skip_type, self.prev_type),
file=sys.stderr)
skip_type = null
// check we recognise this message type:
msg_type = u_ord(hdr[2])
if msg_type in self.formats:
// recognised message found
self.prev_type = msg_type
break;
// message was not recognised; fall through so these
// bytes are considered "skipped".  The signature bytes
// are easily recognisable in the "Skipped bytes"
// message.
if skip_type is null:
skip_type = (u_ord(hdr[0]), u_ord(hdr[1]), u_ord(hdr[2]))
skip_start = self.offset
self.offset += 1
self.remaining -= 1

self.offset += 3
self.remaining = self.data_len - self.offset

fmt = self.formats[msg_type]
if self.remaining < fmt.len-3:
// out of data - can often happen half way through a message
if self.verbose:
print("out of data", file=sys.stderr)
return null
body = self.data_map[self.offset:self.offset+fmt.len-3]
elements = null
try:
if not msg_type in self.unpackers:
self.unpackers[msg_type] = struct.Struct(fmt.msg_struct).unpack
elements = list(self.unpackers[msg_type](body))
except Exception as ex:
print(ex)
if self.remaining < 528:
// we can have garbage at the end of an APM2 log
return null
// we should also cope with other corruption; logs
// transfered via DataFlash_MAVLink may have blocks of 0s
// in them, for example
print("Failed to parse %s/%s with len %u (remaining %u)" %
(fmt.name, fmt.msg_struct, len(body), self.remaining),
file=sys.stderr)
if elements is null:
return self._parse_next()
name = fmt.name
// transform elements which can't be done at unpack time:
for a_index in fmt.a_indexes:
try:
elements[a_index] = array.array('h', elements[a_index])
except Exception as e:
print("Failed to transform array: %s" % str(e),
file=sys.stderr)

if name == "FMT":
// add to formats
// name, len, format, headings
try:
ftype = elements[0]
mfmt = DFFormat(
ftype,
null_term(elements[2]), elements[1],
null_term(elements[3]), null_term(elements[4]),
oldfmt=self.formats.get(ftype,null))
self.formats[ftype] = mfmt
except Exception:
return self._parse_next()

self.offset += fmt.len - 3
self.remaining = self.data_len - self.offset
m = DFMessage(fmt, elements, true, self)

if m.fmt.name == "FMTU":
// add to units information
FmtType = int(elements[0])
UnitIds = elements[1]
MultIds = elements[2]
if FmtType in self.formats:
fmt = self.formats[FmtType]
fmt.set_unit_ids(UnitIds)
fmt.set_mult_ids(MultIds)

try:
self._add_msg(m)
except Exception as ex:
print("bad msg at offset %u" % self.offset, ex)
pass
self.percent = 100.0 * (self.offset / float(self.data_len))

return m
}