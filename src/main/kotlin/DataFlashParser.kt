import java.io.File

/**
 *  Optional wrapper class which abstracts the DFReader from the user
 */
class DataFlashParser(val file: File, progressCallback: ((Int) -> Unit)?) {
    val dfReader : DFReader = if(Util.isDFTextLog(file.absolutePath))
        DFReaderText(file.absolutePath, null, progressCallback)
    else
        DFReaderBinary(file.absolutePath, null, progressCallback)

    constructor(filename: String, progressCallback: ((Int) -> Unit)?) : this(File(filename), progressCallback)

    fun getAllMessages(): ArrayList<DFMessage> {
        return dfReader.getAllMessages()
    }

    fun getFieldLists(fields : Collection<String>) : HashMap<String, ArrayList<Pair<Long,Any>>> {
        return dfReader.getFieldLists(fields)
    }

    fun getFieldListConditional(field : String, shouldInclude: (DFMessage) -> Boolean) : ArrayList<Pair<Long,Any>> {
        return dfReader.getFieldListConditional(field, shouldInclude)
    }

    fun getAllMessagesOfType(type : String) : ArrayList<DFMessage> {
        return dfReader.getAllMessagesOfType(type)
    }

    fun getStartAndEndTimes() : Pair<Long, Long> {
        return dfReader.getStartAndEndTimes()
    }

}