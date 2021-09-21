import org.junit.jupiter.api.Test
import java.math.BigInteger

class StructTest {

    @Test
    fun unpack_single_data_test() {

        val str64 = "Hello! my name is Stephen, \tand my tests aren't very creative123"
        val str64ByteArray = ByteArray(64)
        str64.forEachIndexed { i, c ->
            str64ByteArray[i] = c.code.toByte()
        }
        val a = Struct.unpack_single_data('a', str64ByteArray)
        assert(a == str64)

        val b = Struct.unpack_single_data('b', byteArrayOf(53.toByte(),32.toByte(), 'B'.code.toByte(),'F'.code.toByte()))
        assert(b == "5 BF")

        val B = Struct.unpack_single_data('B',  byteArrayOf(53.toByte(),32.toByte(), 'B'.code.toByte(),'F'.code.toByte()))
        assert(B == "5 BF")

        val x = Struct.unpack_single_data('x', byteArrayOf(4))
        assert(x == "")

        val fiveOneFive = byteArrayOf((515 shr 8).toByte(), (515).toByte()) //and 0xff
        val h = Struct.unpack_single_data('h', fiveOneFive)
        assert(h == "515")

        val H = Struct.unpack_single_data('H', byteArrayOf(2,3))
        assert(H == "515")

        val i = Struct.unpack_single_data('i', byteArrayOf((105890847 shr 24).toByte(), (105890847 shr 16).toByte(), (105890847 shr 8).toByte(), (105890847).toByte()))
        assert(i == "105890847")

        val c = Struct.unpack_single_data('c', byteArrayOf((515 shr 8).toByte(), (515).toByte()))
        assert(c == "5.15")

        val f = Struct.unpack_single_data('f', byteArrayOf(0,0,0,170f.toInt().toByte()))
        assert(f == "170.0")


        val bigInt = BigInteger("-6073823909672292010")
        val q = Struct.unpack_single_data('q', bigInt.toByteArray())
        assert(q == "-6073823909672292010")

        val bigInt2 = BigInteger("6073823909672292010")
        val Q = Struct.unpack_single_data('Q', bigInt2.toByteArray())
        assert(Q == "6073823909672292010")

        val l = Struct.unpack_single_data('L', byteArrayOf((105890847 shr 24).toByte(), (105890847 shr 16).toByte(), (105890847 shr 8).toByte(), (105890847).toByte()))
        assert(l == "10.5890847")



        println("$b $x $h $H $i $c")
    }

    @Test
    fun unpack_test() {
        val fmt = "BBnNZ"
        val bodyIntArray = intArrayOf(128, 89, 70, 77, 84, 0, 66, 66, 110, 78, 90, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 84, 121, 112, 101, 44, 76, 101, 110, 103, 116, 104, 44, 78, 97, 109, 101, 44, 70, 111, 114, 109, 97, 116, 44, 67, 111, 108, 117, 109, 110, 115, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

        val result = Struct.unpack(fmt, bodyIntArray)
        assert(result[0] == "128")
        assert(result[1] == "89")
        assert(result[2].replace("\u0000", "") == "FMT")

        val clean3 = result[3].replace("\u0000", "")
        assert(clean3 == "BBnNZ")

        val clean4 = result[4].replace("\u0000", "")
        assert(clean4 == "Type,Length,Name,Format,Columns")



        val fmt2 = "BBnNZ"
        val body2 = intArrayOf(219, 76, 85, 78, 73, 84, 81, 98, 90, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 84, 105, 109, 101, 85, 83, 44, 73, 100, 44, 76, 97, 98, 101, 108, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val result2 = Struct.unpack(fmt2, body2)
        assert(result2[0] == "219")
        assert(result2[1] == "89")
        assert(result2[2] == "FMT")

        val clean3_2 = result2[3].replace("\u0000", "")
        assert(clean3_2 == "QbZ")

        val clean4_2 = result2[4].replace("\u0000", "")
        assert(clean4_2 == "Type,Length,Name,Format,Columns")
    }
}