/*
 * DFMessage
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

class DFMessage(val fmt: DFFormat, val elements: Array<Any>, val apply_multiplier : String, var parent: DFMessage?) {
    var fieldnames : List<String>
    init {
        fieldnames = fmt.columnsArr
    }

    fun to_dict() : HashMap<String, String>{
        var d = hashMapOf ( "mavpackettype" to fmt.name )

        for(field in fieldnames)
            d[field] = getAttr(field)

        return d
    }

    /**
     * override field getter
     */
    fun getAttr(field: String) {
        var i = 0
        try {
            i = fmt.colhash[field]!!
        } catch (e: Exception) {
            throw java.lang.Exception(field)
        }

        var v = 0
        if (elements[i] is ByteArray) {
            v = elements[i].decode("utf-8")
        } else {
            v = elements[i]
        }
        if (fmt.format[i] == 'a') {
            pass
        } else if (fmt.format[i] != 'M' || self._apply_multiplier) {
            v = fmt.msg_types[i](v)
        }
        if (fmt.msg_types[i] == str) {
            v = null_term(v)
        }
        if (fmt.msg_mults[i] != null && _apply_multiplier) {
            v *= fmt.msg_mults[i]
        }
        return v
    }


    /**
     * override field setter
     */
    fun setAttr( field, v) {
        if (not field [0].isupper() or not field in self . fmt . colhash) {
            super(DFMessage, self).__setattr__(field, v)
        } else {
            i = self.fmt.colhash[field]
            if (self.fmt.msg_mults[i] != null and self . _apply_multiplier) {
                v /= self.fmt.msg_mults[i]
            }
            self._elements[i] = v
        }
    }


    fun get_type() {
        return fmt.name
    }

    fun __str__() {
        var ret = String.format("%s {" , fmt.name)
        var col_count = 0
        for (c in fmt.columns) {
            var v = self.__getattr__(c)
            if(isinstance(v, float) and math.isnan(v )) {
//                quiet nans have more non - zero values :
                noisy_nan = "\x7f\xf8\x00\x00\x00\x00\x00\x00"
                if (struct.pack(">d", v) != noisy_nan) {
                    v = "qnan"
                }
            }
            ret += "%s : %s, " % (c, v )
            col_count += 1
        }
        if(col_count != 0)
            ret = ret[:-2]

        return ret + '}'
    }

    /**
     * create a binary message buffer for a message
     */
    fun get_msgbuf() {
        values = []
        is_py2 = sys.version_info < (3, 0)
        for (i in range(len(self.fmt.columns))) {
            if (i >= len(self.fmt.msg_mults)) {
                continue
            }
            mul = self.fmt.msg_mults[i]
            name = self.fmt.columns[i]
            if (name == "Mode" and "ModeNum" in self.fmt.columns) {
                name = "ModeNum"
            }
            v = self.__getattr__(name)
            if (is_py2) {
                if (isinstance(v, unicode)) {// NOQA
                    v = str(v)
                }
            } else {
                if isinstance(v, str) {
                    v = bytes(v, "ascii")
                }
            }
            if (isinstance(v, array.array)) {
                v = v.tostring()
            }
            if (mul != null) {
                v /= mul
                v = int(round(v))
            }
            values.append(v)
        }

        ret1 = struct.pack("BBB", 0xA3, 0x95, self.fmt.type)
        try {
            ret2 = struct.pack(self.fmt.msg_struct, *values)
        } catch (e: Exception) {
            return null
        }
        return ret1 + ret2
    }


    fun get_fieldnames() {
        return _fieldnames
    }

    /**
     * support indexing, allowing for multi-instance sensors in one message
     */
    fun __getitem__(key) {
        if (fmt.instance_field == null) {
//            raise IndexError ()
            throw java.lang.Exception("IndexError")
        }
        val k = String.format("%s[%s]", fmt.name, str(key))
        if (not k in self . _parent . messages ) {
            throw java.lang.Exception("IndexError")
        }
        return _parent.messages[k]
    }
}