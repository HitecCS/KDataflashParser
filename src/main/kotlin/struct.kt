import java.nio.ByteOrder

/*object struct {
    fun unpack(packet: CharArray, raw: ByteArray): Array<String?> {
        val result = arrayOfNulls<String>(packet.size)
        var pos = 0
        var strIndex = 0
        for (i in packet.indices) {
            val type = packet[i]
            when (type) {
                'x' -> {
                    pos += 1
                    continue
                }
                'c' -> {
                    val c = (raw[pos].and(0xFF.toByte())).toInt().toChar()
                    result[strIndex] = c.toString()
                    strIndex += 1
                    pos += 1
                }
                'h' -> {
                    val bb: ByteBuffer = ByteBuffer.allocate(2)
                    bb.order(ByteOrder.LITTLE_ENDIAN)
                    bb.put(raw[pos])
                    bb.put(raw[pos + 1])
                    val shortVal: Short = bb.getShort(0)
                    result[strIndex] = shortVal.toString()
                    pos += 2
                    strIndex += 1
                }
                'n' -> {
                    var s: String? = ""
                    while (raw[pos] != 0x00.toByte()) {
                        val c = (raw[pos].and(0xFF.toByte())).toInt().toChar()
                        s += c.toString()
                        pos += 1
                    }
                    result[strIndex] = s
                    strIndex += 1
                    pos += 1
                }
                'b' -> {
                    val p = raw[pos]
                    result[strIndex] = p.toInt().toString()
                    strIndex += 1
                    pos += 1
                }
            }
        }
        return result
    }
}*/

/*
class Struct internal constructor() {
    private val BigEndian: Short = 0
    private val LittleEndian: Short = 1
    private var byteOrder: Short
    private var nativeByteOrder: Short = 0
    private fun reverseBytes(b: ByteArray): ByteArray {
        var tmp: Byte
        for (i in 0 until b.size / 2) {
            tmp = b[i]
            b[i] = b[b.size - i - 1]
            b[b.size - i - 1] = tmp
        }
        return b
    }

    private fun packRaw_16b(value: Short): ByteArray {
        var bx = ByteArray(2)
        if (value >= 0) {
            bx[0] = (value and 0xff).toByte()
            bx[1] = (value shr 8 and 0xff) as Byte
        } else {
            var v2 = abs(value.toInt())
            v2 = (v2 xor 0x7fff) + 1 // invert bits and add 1
            v2 = v2 or (1 shl 15)
            bx[0] = (v2 and 0xff).toByte()
            bx[1] = (v2 shr 8 and 0xff).toByte()
        }
        if (byteOrder == BigEndian) {
            bx = reverseBytes(bx)
        }
        return bx
    }

    private fun packRaw_u16b(mValue: Int): ByteArray {
        var value = mValue
        var bx = ByteArray(2)
        value = value and 0xffff //truncate
        if (value >= 0) {
            bx[0] = (value and 0xff).toByte()
            bx[1] = (value shr 8 and 0xff).toByte()
        }
        if (byteOrder == BigEndian) {
            bx = reverseBytes(bx)
        }
        return bx
    }

    private fun packRaw_32b(value: Int): ByteArray {
        var bx = ByteArray(4)
        if (value >= 0) {
            bx[0] = (value and 0xff).toByte()
            bx[1] = (value shr 8 and 0xff).toByte()
            bx[2] = (value shr 16 and 0xff).toByte()
            bx[3] = (value shr 24 and 0xff).toByte()
        } else {
            var v2 = Math.abs(value).toLong()
            v2 = (v2 xor 0x7fffffff) + 1 // invert bits and add 1
            v2 = v2 or (1 shl 31) // add the 32nd bit as negative bit
            bx[0] = (v2 and 0xff).toByte()
            bx[1] = (v2 shr 8 and 0xff).toByte()
            bx[2] = (v2 shr 16 and 0xff).toByte()
            bx[3] = (v2 shr 24 and 0xff).toByte()
        }
        if (byteOrder == BigEndian) {
            bx = reverseBytes(bx)
        }
        return bx
    }

    private fun packRaw_u32b(mValue: Long): ByteArray {
        var value = mValue
        var bx = ByteArray(4)
        value = value and -0x1
        if (value >= 0) {
            bx[0] = (value and 0xff).toByte()
            bx[1] = (value shr 8 and 0xff).toByte()
            bx[2] = (value shr 16 and 0xff).toByte()
            bx[3] = (value shr 24 and 0xff).toByte()
        }
        if (byteOrder == BigEndian) {
            bx = reverseBytes(bx)
        }
        return bx
    }

    fun pack_single_data(fmt: Char, value: Long): ByteArray? {
        val bx: ByteArray?
        bx = when (fmt) {
            'h' -> {
                val value = (value and 0xffff).toShort()
                packRaw_16b(value)
            }
            'H' -> packRaw_u16b(value.toInt())
            'i' -> {
                val ival = (value and -0x1).toInt()
                packRaw_32b(ival)
            }
            'I' -> packRaw_u32b(value)
            else -> {
                //do nothing
                println("Invalid format specifier")
                null
            }
        }
        return bx
    }

    @Throws(Exception::class)
    fun pack(fmt: String, value: Long): ByteArray? {
        if (fmt.length > 2) {
            throw Exception("Single values may not have multiple format specifiers")
        }
        var bx: ByteArray? = ByteArray(1)
        for (i in 0 until fmt.length) {
            val c = fmt[i]
            if (i == 0 && (c == '>' || c == '<' || c == '@' || c == '!')) {
                if (c == '>') byteOrder = BigEndian else if (c == '<') byteOrder =
                    LittleEndian else if (c == '!') byteOrder = BigEndian else if (c == '@') byteOrder = nativeByteOrder
            } else if (c != '>' && c != '<' && c != '@' && c != '!') {
                bx = pack_single_data(c, value)
                if (bx == null) throw Exception("Invalid character specifier")
            }
        }
        return bx
    }

    @Throws(Exception::class)
    fun pack(fmt: String, vals: LongArray): ByteArray {
        val c0 = fmt[0]
        var len: Int
        len = if (c0 == '@' || c0 == '>' || c0 == '<' || c0 == '!') {
            fmt.length - 1
        } else {
            fmt.length
        }
        if (len != vals.size) throw Exception("format length and values aren't equal")
        len = lenEst(fmt)
        var bxx = ByteArray(0)
        var bx: ByteArray?
        var temp: ByteArray
        for (i in 0 until fmt.length) {
            val c = fmt[i]
            if (i == 0 && (c == '>' || c == '<' || c == '@' || c == '!')) {
                if (c == '>') byteOrder = BigEndian else if (c == '<') byteOrder =
                    LittleEndian else if (c == '!') byteOrder = BigEndian else if (c == '@') byteOrder = nativeByteOrder
            } else if (c != '>' && c != '<' && c != '@' && c != '!') {
                bx = if (c0 == '@' || c0 == '>' || c0 == '<' || c0 == '!') {
                    pack(Character.toString(c), vals[i - 1])
                } else {
                    pack(Character.toString(c), vals[i])
                }
                temp = ByteArray(bxx.size + bx!!.size)
                System.arraycopy(bxx, 0, temp, 0, bxx.size)
                System.arraycopy(bx, 0, temp, bxx.size, bx.size)
                bxx = Arrays.copyOf(temp, temp.size)
            }
        }
        return bxx
    }

    private fun unpackRaw_16b(value: ByteArray): Long {
        if (byteOrder == LittleEndian) reverseBytes(value)
        var x: Long
        x = (value[0] shl 8 or (value[1] and 0xff)).toLong()
        if (x ushr 15 and 1 == 1L) {
            x = (x xor 0x7fff and 0x7fff) + 1 //2's complement 16 bit
            x *= -1
        }
        return x
    }

    private fun unpackRaw_u16b(value: ByteArray): Long {
        if (byteOrder == LittleEndian) reverseBytes(value)
        val x: Long
        x = (value[0] and 0xff shl 8 or (value[1] and 0xff)).toLong()
        return x
    }

    private fun unpackRaw_32b(value: ByteArray): Long {
        if (byteOrder == LittleEndian) reverseBytes(value)
        var x: Long
        x = (value[0] shl 24 or (value[1] shl 16) or (value[2] shl 8) or value[3]).toLong()
        if (x ushr 31 and 1 == 1L) {
            x = (x xor 0x7fffffff and 0x7fffffff) + 1 //2's complement 32 bit
            x *= -1
        }
        return x
    }

    private fun unpackRaw_u32b(value: ByteArray): Long {
        if (byteOrder == LittleEndian) reverseBytes(value)
        val x: Long
        x =
            (value[0] and 0xff).toLong() shl 24 or ((value[1] and 0xff).toLong() shl 16) or ((value[2] and 0xff).toLong() shl 8) or (value[3] and 0xff).toLong()
        return x
    }

    @Throws(Exception::class)
    fun unpack_single_data(fmt: Char, value: ByteArray): Long {
        var `var`: Long = 0
        when (fmt) {
            'h' -> {
                if (value.size != 2) throw Exception("Byte length mismatch")
                `var` = unpackRaw_16b(value)
            }
            'H' -> {
                if (value.size != 2) throw Exception("Byte length mismatch")
                `var` = unpackRaw_u16b(value)
            }
            'i' -> {
                if (value.size != 4) throw Exception("Byte length mismatch")
                `var` = unpackRaw_32b(value)
            }
            'I' -> {
                if (value.size != 4) throw Exception("Byte length mismatch")
                `var` = unpackRaw_u32b(value)
            }
            else -> {
            }
        }
        return `var`
    }

    private fun lenEst(fmt: String): Int {
        var counter = 0
        var x = '\u0000'
        for (i in 0 until fmt.length) {
            x = fmt[i]
            if (x == 'i' || x == 'I') counter += 4 else if (x == 'h' || x == 'H') counter += 2
        }
        return counter
    }

    @Throws(Exception::class)
    fun unpack(fmt: String, vals: ByteArray): LongArray {
        val len: Int
        len = lenEst(fmt)
        if (len != vals.size) throw Exception("format length and values aren't equal")
        val c0 = fmt[0]
        val bxx: LongArray
        bxx = if (c0 == '@' || c0 == '<' || c0 == '>' || c0 == '!') {
            LongArray(fmt.length - 1)
        } else {
            LongArray(fmt.length)
        }
        var c: Char
        val bShort = ByteArray(2)
        val bLong = ByteArray(4)
        val bs = ByteArrayInputStream(vals)
        var p = 0
        for (i in 0 until fmt.length) {
            c = fmt[i]
            if (i == 0 && (c == '>' || c == '<' || c == '@' || c == '!')) {
                byteOrder =
                    if (c == '>') BigEndian else if (c == '<') LittleEndian else if (c == '!') BigEndian else nativeByteOrder
            } else {
                if (c != '>' && c != '<' && c != '@' && c != '!') {
                    if (c == 'h' || c == 'H') {
                        val read = bs.read(bShort)
                        bxx[p] = unpack_single_data(c, bShort)
                    } else if (c == 'i' || c == 'I') {
                        val read = bs.read(bLong)
                        bxx[p] = unpack_single_data(c, bLong)
                    }
                    p++
                }
            }
        }
        return bxx
    }

    init {
        val x = ByteOrder.nativeOrder()
        nativeByteOrder = if (x == ByteOrder.LITTLE_ENDIAN) LittleEndian else BigEndian
        byteOrder = nativeByteOrder
    }
}*/
/*
class struct {

    @Throws(Exception::class)
    fun unpack_single_data(fmt: Char, byteArray: ByteArray): String? {
        val value = when (fmt) {
            'h' -> {
                if (byteArray.size != 2) throw Exception("Byte length mismatch")
                unpackRaw_16b(byteArray).toString() + ""
            }
            'H' -> {
                if (byteArray.size != 2) throw Exception("Byte length mismatch")
                Struct.unpackRaw_u16b(byteArray).toString() + ""
            }
            'i' -> {
                if (byteArray.size != 4) throw Exception("Byte length mismatch")
                value = Struct.unpackRaw_32b(byteArray).toString() + ""
            }
            'I' -> {
                if (byteArray.size != 4) throw Exception("Byte length mismatch")
                value = Struct.unpackRaw_u32b(byteArray).toString() + ""
            }
            'x' -> {
                if (byteArray.size != 1) throw Exception("Byte length mismatch")
                ""
            }
            'c' -> {
                val c: `val` = raw.get(pos).and(0xFF.toByte()).toInt().toChar()
                result.get(strIndex) = c.toString()
                strIndex += 1
                pos += 1
            }
            'h' -> {
                var bb: `val`
                ByteBuffer = ByteBuffer.allocate(2)
                bb.order(ByteOrder.LITTLE_ENDIAN)
                bb.put(raw.get(pos))
                bb.put(raw.get(pos + 1))
                var shortVal: `val`
                Short = bb.getShort(0)
                result.get(strIndex) = shortVal.toString()
                pos += 2
                strIndex += 1
            }
            'n' -> {
                var s: Unit
                if (String) = ""
                while (raw.get(pos) !== 0x00.toByte()) {
                    val c: `val` = raw.get(pos).and(0xFF.toByte()).toInt().toChar()
                    s += c.toString()
                    pos += 1
                }
                result.get(strIndex) = s
                strIndex += 1
                pos += 1
            }
            'b' -> {
                val p: `val` = raw.get(pos)
                result.get(strIndex) = p.toInt().toString()
                strIndex += 1
                pos += 1
            }
            else -> {
            }
        }
        return value
    }
}
*/