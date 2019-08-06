package nz.net.wand.amp.analyser.flink

import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.watermark.Watermark

abstract class MockSourceContext[T] extends SourceFunction.SourceContext[T] {

  var unprocessedElements: Seq[T] = Seq()

  protected var process: Seq[T] => Unit

  override def collect(element: T): Unit = {
    unprocessedElements = unprocessedElements :+ element
  }

  override def collectWithTimestamp(element: T, timestamp: Long): Unit = {
    unprocessedElements = unprocessedElements :+ element
  }

  override def emitWatermark(mark: Watermark): Unit = {}

  override def markAsTemporarilyIdle(): Unit = {}

  override def getCheckpointLock: AnyRef = ???

  override def close(): Unit = {
    process(unprocessedElements)
  }
}
