# KDataflashParser
A Kotlin library providing a parser and convenience functions for parsing and analyzing Ardupilot/Px4 Dataflash logs.
It's compatible with both Binary (.bin) and Text (.log) log formats


## Overview

If you are totally unfamiliar with the format of DataFlash logs, this project contains a few Wiki pages about them; [this article on Ardupilots website](https://ardupilot.org/copter/docs/common-downloading-and-analyzing-data-logs-in-mission-planner.html) is also very helpful.

There is no standard set of messages for DataFlash files as they are self defining. However, if you intend to use this library to parse ArduPlane or ArduCopter logs, you can use these lists of log messages.

ArduPlane: https://ardupilot.org/plane/docs/logmessages.html

ArduCopter: https://ardupilot.org/copter/docs/logmessages.html

## Getting Started

To parse a log use the wrapper class DataFlashParser, or you can use the main parsing classes `DFReaderBinary` or `DFReaderText` directly
    
    val dfParser = DataFlashParser(filename) { pct : Int -> println("Percent $pct") }
    
To get a full list of every message in the log you can call `getAllMessages()`. However, this can be quite expensive on the processor and memory. This is especially not recommended on Android as it will quickly hit the memory limit.
   
     val allDataFlashMessages : ArrayList<DFMessage> = dfParser.getAllMessages()

If you only need access to certain data fields, you can get lists of every message which contains those fields like this:
    
    val fieldLists : HashMap<String, ArrayList<DFMessage>> = dfParser.getFieldLists(hashSetOf("Roll",
        "Pitch",
        "Yaw",
        "Lat",
        "Lng"))

    allDataFlashMessages.size // = 270441
    fieldLists["Roll"].size // = 40347
    
Some fields exist across multiple messages. If the message's type is important to you, you can filter messages in to your search in the following way. Note that this function can also be used to filter messages in more complex ways.

    val baroAlts : ArrayList<DFMessage> = dfParser.getFieldListConditional("Alt") { m -> m.getType() == "BARO" }
    
    val nonBaroAlts : ArrayList<DFMessage> = dfParser.getFieldListConditional("Alt") { m -> m.getType() != "BARO" }


##License

KDataFlashParser is open source under [GNU GPL version 3](https://github.com/HitecCS/KDataflashParser/blob/master/LICENSE)

This software is based on: [APM DataFlash log file reader](https://github.com/ArduPilot/pymavlink/blob/master/DFReader.py), Copyright Andrew Tridgell 2011;

APM DataFlash log file reader is partly based on SDLog2Parser by Anton Babushkin