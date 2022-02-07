package scribe.benchmark

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits._

import java.util.concurrent.TimeUnit
import com.typesafe.config.ConfigFactory
import com.typesafe.{scalalogging => sc}
import org.apache.logging.log4j.LogManager
import org.openjdk.jmh.annotations
import org.pmw.tinylog
import org.typelevel.log4cats.SelfAwareStructuredLogger
import scribe.Logger
import scribe.file._
import scribe.format._
import scribe.handler.AsynchronousLogHandle

// jmh:run -i 3 -wi 3 -f1 -t1 -rf JSON -rff benchmarks.json
@annotations.State(annotations.Scope.Thread)
class LoggingSpeedBenchmark {
  assert(LogManager.getRootLogger.isInfoEnabled, "INFO is not enabled in log4j!")

  private lazy val asyncHandle = AsynchronousLogHandle()

  @annotations.Setup(annotations.Level.Trial)
  def doSetup(): Unit = {
    ConfigFactory.load()
    tinylog.Configurator
      .defaultConfig()
      .removeAllWriters()
      .writer(new tinylog.writers.FileWriter("logs/tiny.log"))
      .level(tinylog.Level.INFO)
      .activate()
  }

  @annotations.Benchmark
  @annotations.BenchmarkMode(Array(annotations.Mode.AverageTime))
//  @annotations.BenchmarkMode(Array(annotations.Mode.AverageTime, annotations.Mode.SampleTime, annotations.Mode.Throughput))
  @annotations.OutputTimeUnit(TimeUnit.NANOSECONDS)
  @annotations.OperationsPerInvocation(1000)
  def withScribe(): Unit = {
    val fileWriter = FileWriter("logs" / "scribe.log")
    val formatter = formatter"$date $levelPaddedRight [$threadName] $message"
    val logger = Logger.empty.orphan().withHandler(formatter = formatter, writer = fileWriter)

    var i = 0
    while (i < 1000) {
      logger.info("test")
      i += 1
    }
    fileWriter.dispose()
  }

  @annotations.Benchmark
  @annotations.BenchmarkMode(Array(annotations.Mode.AverageTime))
  //  @annotations.BenchmarkMode(Array(annotations.Mode.AverageTime, annotations.Mode.SampleTime, annotations.Mode.Throughput))
  @annotations.OutputTimeUnit(TimeUnit.NANOSECONDS)
  @annotations.OperationsPerInvocation(1000)
  def withScribeEffect(): Unit = {
    import scribe.cats._

    val fileWriter = FileWriter("logs" / "scribe-effect.log")
    val formatter = formatter"$date $levelPaddedRight [$threadName] $message"
    val logger = Logger.empty.orphan().withHandler(formatter = formatter, writer = fileWriter).f[IO]

    val io = (0 until 1000).toList.map { _ =>
      logger.info("test")
    }.sequence.map(_ => ())
    io.unsafeRunSync()
    fileWriter.dispose()
  }

  @annotations.Benchmark
  @annotations.BenchmarkMode(Array(annotations.Mode.AverageTime))
  //  @annotations.BenchmarkMode(Array(annotations.Mode.AverageTime, annotations.Mode.SampleTime, annotations.Mode.Throughput))
  @annotations.OutputTimeUnit(TimeUnit.NANOSECONDS)
  @annotations.OperationsPerInvocation(1000)
  def withScribeEffectParallel(): Unit = {
    import scribe.cats._

    val fileWriter = FileWriter("logs" / "scribe-effect-par.log")
    val formatter = formatter"$date $levelPaddedRight [$threadName] $message"
    val logger = Logger.empty.orphan().withHandler(formatter = formatter, writer = fileWriter).f[IO]

    val io = (0 until 1000).toList.map { _ =>
      logger.info("test")
    }.parSequence.map(_ => ())
    io.unsafeRunSync()
    fileWriter.dispose()
  }

  @annotations.Benchmark
  @annotations.BenchmarkMode(Array(annotations.Mode.AverageTime))
//  @annotations.BenchmarkMode(Array(annotations.Mode.AverageTime, annotations.Mode.SampleTime, annotations.Mode.Throughput))
  @annotations.OutputTimeUnit(TimeUnit.NANOSECONDS)
  @annotations.OperationsPerInvocation(1000)
  def withScribeAsync(): Unit = {
    val fileWriter = FileWriter("logs" / "scribe-async.log")
    val formatter = formatter"$date $levelPaddedRight [$threadName] $message"
    val logger = Logger.empty.orphan().withHandler(formatter = formatter, writer = fileWriter, handle = asyncHandle)

    var i = 0
    while (i < 1000) {
      logger.info("test")
      i += 1
    }
  }

  @annotations.Benchmark
  @annotations.BenchmarkMode(Array(annotations.Mode.AverageTime))
//  @annotations.BenchmarkMode(Array(annotations.Mode.AverageTime, annotations.Mode.SampleTime, annotations.Mode.Throughput))
  @annotations.OutputTimeUnit(TimeUnit.NANOSECONDS)
  @annotations.OperationsPerInvocation(1000)
  def withLog4j(): Unit = {
    val logger = LogManager.getRootLogger
    var i = 0
    while (i < 1000) {
      logger.info("test")
      i += 1
    }
    LogManager.shutdown()
  }

  @annotations.Benchmark
  @annotations.BenchmarkMode(Array(annotations.Mode.AverageTime))
  //  @annotations.BenchmarkMode(Array(annotations.Mode.AverageTime, annotations.Mode.SampleTime, annotations.Mode.Throughput))
  @annotations.OutputTimeUnit(TimeUnit.NANOSECONDS)
  @annotations.OperationsPerInvocation(1000)
  def withLog4cats(): Unit = {
    import org.typelevel.log4cats.Logger
    import org.typelevel.log4cats.slf4j.Slf4jLogger
    import cats.effect.Sync
    import cats.implicits._

    implicit def unsafeLogger[F[_]: Sync]: SelfAwareStructuredLogger[F] = Slf4jLogger.getLoggerFromName[F]("log4cats")

    val io = (0 until 1000).toList.map { _ =>
      Logger[IO].info("test")
    }.sequence.map(_ => ())
    io.unsafeRunSync()
  }

  @annotations.Benchmark
  @annotations.BenchmarkMode(Array(annotations.Mode.AverageTime))
//  @annotations.BenchmarkMode(Array(annotations.Mode.AverageTime, annotations.Mode.SampleTime, annotations.Mode.Throughput))
  @annotations.OutputTimeUnit(TimeUnit.NANOSECONDS)
  @annotations.OperationsPerInvocation(1000)
  def withLog4s(): Unit = {
    val logger = org.log4s.getLogger("scala")
    var i = 0
    while (i < 1000) {
      logger.info("test")
      i += 1
    }
  }

  @annotations.Benchmark
  @annotations.BenchmarkMode(Array(annotations.Mode.AverageTime))
//  @annotations.BenchmarkMode(Array(annotations.Mode.AverageTime, annotations.Mode.SampleTime, annotations.Mode.Throughput))
  @annotations.OutputTimeUnit(TimeUnit.NANOSECONDS)
  @annotations.OperationsPerInvocation(1000)
  def withLog4jTrace(): Unit = {
    val logger = LogManager.getLogger("Trace")
    var i = 0
    while (i < 1000) {
      logger.info("test")
      i += 1
    }
    LogManager.shutdown()
  }

  @annotations.Benchmark
  @annotations.BenchmarkMode(Array(annotations.Mode.AverageTime))
//  @annotations.BenchmarkMode(Array(annotations.Mode.AverageTime, annotations.Mode.SampleTime, annotations.Mode.Throughput))
  @annotations.OutputTimeUnit(TimeUnit.NANOSECONDS)
  @annotations.OperationsPerInvocation(1000)
  def withScalaLogging(): Unit = {
    val logger = sc.Logger("root")
    var i = 0
    while (i < 1000) {
      logger.info("test")
      i += 1
    }
  }

  @annotations.Benchmark
  @annotations.BenchmarkMode(Array(annotations.Mode.AverageTime))
//  @annotations.BenchmarkMode(Array(annotations.Mode.AverageTime, annotations.Mode.SampleTime, annotations.Mode.Throughput))
  @annotations.OutputTimeUnit(TimeUnit.NANOSECONDS)
  @annotations.OperationsPerInvocation(1000)
  def withLogback(): Unit = {
    val logger = org.slf4j.LoggerFactory.getLogger("logback")
    var i = 0
    while (i < 1000) {
      logger.info("test")
      i += 1
    }
  }

  @annotations.Benchmark
  @annotations.BenchmarkMode(Array(annotations.Mode.AverageTime))
//  @annotations.BenchmarkMode(Array(annotations.Mode.AverageTime, annotations.Mode.SampleTime, annotations.Mode.Throughput))
  @annotations.OutputTimeUnit(TimeUnit.NANOSECONDS)
  @annotations.OperationsPerInvocation(1000)
  def withTinyLog(): Unit = {
    var i = 0
    while (i < 1000) {
      tinylog.Logger.info("test")
      i += 1
    }
  }

  /*@annotations.Benchmark
  @annotations.BenchmarkMode(Array(annotations.Mode.AverageTime))
//  @annotations.BenchmarkMode(Array(annotations.Mode.AverageTime, annotations.Mode.SampleTime, annotations.Mode.Throughput))
  @annotations.OutputTimeUnit(TimeUnit.NANOSECONDS)
  @annotations.OperationsPerInvocation(1000)
  def withPrintLine(): Unit = {
    var i = 0
    while (i < 1000) {
      println("test")
      i += 1
    }
  }*/

  @annotations.TearDown
  def tearDown(): Unit = {
//    asynchronousWriter.dispose()
  }
}