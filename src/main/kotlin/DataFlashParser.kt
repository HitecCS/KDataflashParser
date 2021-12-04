import java.io.File

/**
 *  Optional wrapper class which abstracts the DFReader from the user
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright (C) 2021 Hitec Commercial Solutions
 *  @author Stephen Woerner
 */
class DataFlashParser(val file: File, progressCallback: ((Int) -> Unit)?) {
    val dfReader : DFReader = if(Util.isDFTextLog(file.absolutePath))
        DFReaderText(file.absolutePath, null, progressCallback)
    else
        DFReaderBinary(file.absolutePath, null, progressCallback)

    constructor(filename: String, progressCallback: ((Int) -> Unit)?) : this(File(filename), progressCallback)

    fun getAllMessages(progressCallback: ((Int) -> Unit)?): ArrayList<DFMessage> {
        return dfReader.getAllMessages(progressCallback)
    }

    fun getFieldLists(fields : Collection<String>, progressCallback: ((Int) -> Unit)?) : HashMap<String, ArrayList<Pair<Long,Any>>> {
        return dfReader.getFieldLists(fields, progressCallback)
    }

    fun getFieldListConditional(field : String, shouldInclude: (DFMessage) -> Boolean, progressCallback: ((Int) -> Unit)?) : ArrayList<Pair<Long,Any>> {
        return dfReader.getFieldListConditional(field, shouldInclude, progressCallback)
    }

    fun getAllMessagesOfType(type : String, progressCallback: ((Int) -> Unit)?) : ArrayList<DFMessage> {
        return dfReader.getAllMessagesOfType(type, progressCallback)
    }

    fun getStartAndEndTimes() : Pair<Long, Long> {
        return dfReader.getStartAndEndTimes()
    }

    override fun toString(): String {
        return "DataFlashParser { \n${file.absolutePath}\n${dfReader} }"
    }
}