package scribe.slf4j

import org.slf4j.helpers.FormattingTuple
import org.slf4j.spi.LocationAwareLogger
import scribe.{Level, LogRecord}
import scribe.Loggable.StringLoggable
import scribe.message.Message

object SLF4JHelper {
  def scribeLevel(level: Int): Level = level match {
    case LocationAwareLogger.TRACE_INT => Level.Trace
    case LocationAwareLogger.DEBUG_INT => Level.Debug
    case LocationAwareLogger.INFO_INT => Level.Info
    case LocationAwareLogger.WARN_INT => Level.Warn
    case LocationAwareLogger.ERROR_INT => Level.Error
  }

  def log(name: String, level: Level, msg: String, t: Option[Throwable]): Unit = {
    val scribeLogger = scribe.Logger(name)
    val record = LogRecord(
      level = level,
      value = level.value,
      message = Message.static(msg),
      additionalMessages = t.toList,
      fileName = "",
      className = name,
      methodName = None,
      line = None,
      column = None
    )
    scribeLogger.log(record)
  }

  def logTuple(name: String, level: Level, tuple: FormattingTuple): Unit = {
    log(name, level, tuple.getMessage, Option(tuple.getThrowable))
  }

  def includes(name: String, level: Level): Boolean = scribe.Logger(name).includes(level)
}
