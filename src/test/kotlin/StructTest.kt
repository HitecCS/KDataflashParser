import org.junit.jupiter.api.Test
import java.math.BigInteger

class StructTest {

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun unpack_single_data_test() {
        val a = 1f
        val b = a.toRawBits()
        val c = Float.fromBits(b)

        println(c)
//        val oneF = Struct.unpack_single_data('i', ubyteArrayOf(0u,0u, 0u,0u, 8u, 0u))
//        val b = 8 shl 4
//        val d = Float.fromBits(b)


//        val oneF = ubyteArrayOf(0u,0u, 0u,0u, 8u, 0u)
//        val i = Struct.unpack_single_data('f', oneF)
//        assert(i == "1.0")

//        val str64 = "Hello! my name is Stephen, \tand my tests aren't very creative123"
//        val str64ByteArray = IntArray(64)
//        str64.forEachIndexed { i, c ->
//            str64ByteArray[i] = c.code
//        }
//        val a = Struct.unpack_single_data('a', str64ByteArray)
//        assert(a == str64)
//
//        val b = Struct.unpack_single_data('b', intArrayOf(53))
//        assert(b == "53")
//
//        val B = Struct.unpack_single_data('B',  intArrayOf(53))
//        assert(B == "53")
//
//        val x = Struct.unpack_single_data('x', intArrayOf(4))
//        assert(x == "")
//
////        val fiveOneFive = ubyteArrayOf((515 shr 8).toUByte(), (515).toUByte()) //and 0xff
////        val h = Struct.unpack_single_data('h', fiveOneFive)
////        assert(h == "515")
//
//        val H = Struct.unpack_single_data('H', intArrayOf(2,3))
//        assert(H == "515")
//
//        val i = Struct.unpack_single_data('i', intArrayOf((105890847 shr 24), (105890847 shr 16), (105890847 shr 8), (105890847)))
//        assert(i == "105890847")
//
//        val c = Struct.unpack_single_data('c', intArrayOf((515 shr 8), (515)))
//        assert(c == "5.15")
//
//        val f = Struct.unpack_single_data('f', intArrayOf(0,0,0,170f.toInt()))
//        assert(f == "170.0")

//
//        val bigInt = BigInteger("-6073823909672292010")
//        val q = Struct.unpack_single_data('q', bigInt.toByteArray().toUByteArray())
//        assert(q == "-6073823909672292010")
//
//        val bigInt2 = BigInteger("6073823909672292010")
//        val Q = Struct.unpack_single_data('Q', bigInt2.toByteArray().toUByteArray())
//        assert(Q == "6073823909672292010")
//
//        val l = Struct.unpack_single_data('L', ubyteArrayOf((105890847 shr 24).toUByte(), (105890847 shr 16).toUByte(), (105890847 shr 8).toUByte(), (105890847).toUByte()))
//        assert(l == "10.5890847")



//        println("$b $x $h $H $i $c")
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun unpack_test() {

        val fTest = intArrayOf(-36, -12, 54, 43, 0, 0, 0, 0, 0, 15, 0, 89, 0, 106, 0, -104, -50, 70, -67, 79, 11, -43, 61, 122, -26, -120, -67, 126, 29, -68, -68, 28, -94, 28, 65, -6, -84, -83, -64, 90, -57, 46, -65, 11, 0, 2, 0, 4, 0, -77, 33, 0, 0)
        val fTestBody = UByteArray(fTest.size) { i -> fTest[i].toUByte() }
        val a = Struct.unpack("QBccCfffffffccce", fTestBody)
        print("test")

//        val fmt = "BBnNZ"
//        val bodyIntArray = intArrayOf(-128, 89, 70, 77, 84, 0, 66, 66, 110, 78, 90, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 84, 121, 112, 101, 44, 76, 101, 110, 103, 116, 104, 44, 78, 97, 109, 101, 44, 70, 111, 114, 109, 97, 116, 44, 67, 111, 108, 117, 109, 110, 115, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
//        val body = UByteArray(bodyIntArray.size) { i -> bodyIntArray[i].toUByte() }
//
//        val result = Struct.unpack(fmt, body)
//        assert(result[0] == "128")
//        assert(result[1] == "89")
//        assert(Util.nullTerm(result[2]) == "FMT")
//
//        val clean3 = Util.nullTerm(result[3])
//        assert(clean3 == "BBnNZ")
//
//        val clean4 = Util.nullTerm(result[4])
//        assert(clean4 == "Type,Length,Name,Format,Columns")
//
//
//        val fmt3 = "QBIHBcLLeffffB"
//        val bodyIntArray3 = intArrayOf(-95, 114, -20, 7, 0, 0, 0, 0, 3, -24, 49, -82, 10, 90, 8, 9, 121, 0, 94, 62, -28, 21, 107, 14, -12, 75, 93, 60, 0, 0, 23, -39, -114, 62, -74, 18, -124, 66, 111, 18, 3, 61, 0, 0, 0, 0, 1)
//        val body3 = UByteArray(bodyIntArray3.size) { i -> bodyIntArray3[i].toUByte() }
//        val result3 = Struct.unpack(fmt3, body3)
//        assert(result3[0] == "132936353")
//        assert(result3[2] == "179188200")
//        assert(result3[3] == "2138")
//
//        val fmt2 = "BBnNZ"
//        val bodyIntArray2 = intArrayOf(-37, 76, 85, 78, 73, 84, 81, 98, 90, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 84, 105, 109, 101, 85, 83, 44, 73, 100, 44, 76, 97, 98, 101, 108, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
//        val body2 = UByteArray(bodyIntArray2.size) { i -> bodyIntArray2[i].toUByte() }
//        val result2 = Struct.unpack(fmt2, body2)
//        assert(result2[0] == "219")
//        assert(result2[1] == "76")
//        assert(result2[2] == "UNIT")
//
//        val clean3_2 = Util.nullTerm(result2[3])
//        assert(clean3_2 == "QbZ")
//
//        val clean4_2 = Util.nullTerm(result2[4])
//        assert(clean4_2 == "TimeUS,Id,Label")
    }
}