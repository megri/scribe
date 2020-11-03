package scribe.writer

import scribe.{Level, LogRecord, Logger}
import scribe.output.LogOutput

object ASCIIConsoleWriter extends Writer {
  override def write[M](record: LogRecord[M], output: LogOutput): Unit = {
    val stream = if (record.level <= Level.Info) {
      Logger.system.out
    } else {
      Logger.system.err
    }
    stream.println(output.plainText)
  }
}