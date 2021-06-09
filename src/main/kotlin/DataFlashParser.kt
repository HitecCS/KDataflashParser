import java.lang.reflect.Type
import kotlin.reflect.KClass

/*
 * KDataFlashParser
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

/* // Uncomment for a good time ;-)



if (__name__ == "__main__") {
        val use_profiler = false
        if (use_profiler) {
//        from line_profiler import LineProfiler
            val profiler = LineProfiler()
            profiler.add_function(DFReader_binary._parse_next)
            profiler.add_function(DFReader_binary._add_msg)
            profiler.add_function(DFReader._set_time)
            profiler.enable_by_count()
        }

        var filename = sys.argv[1]
        if (filename.endswith(".log")) {
            log = DFReader_text(filename)
        } else {
            log = DFReader_binary(filename)
        }
//bfile = filename + ".bin"
//bout = open(bfile, "wb")
        while (true) {
            m = log.recv_msg()
            if (m == null)
                break
            //bout.write(m.get_msgbuf())
            //print(m)
        }

        if( use_profiler)
            profiler.print_stats()
}

 */