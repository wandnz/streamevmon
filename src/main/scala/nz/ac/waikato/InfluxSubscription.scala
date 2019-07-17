package nz.ac.waikato

import java.io._
import java.net.ServerSocket

import org.apache.flink.api.scala.ExecutionEnvironment
import org.apache.flink.streaming.api.scala._

class SubscriptionServer extends Thread
{
  override def run(): Unit =
  {
    val server = new ServerSocket(8008)
    while (true)
    {
      val reader =
        new BufferedReader(
          new InputStreamReader(
            server.accept.getInputStream))

      println("Beginning to listen to subscription...")

      val lines = Stream.continually(reader.readLine).takeWhile(_ != null)

      lines.foreach(l => println(ICMPFactory.CreateICMPs(l)))

      reader.close()
    }
  }
}

object InfluxSubscription
{
  def main(args: Array[String]): Unit =
  {
    val env = ExecutionEnvironment.getExecutionEnvironment

    val oneRequest = env.fromElements("POST /write?consistency=&db=nntsc&precision=ns&rp=nntscdefault HTTP/1.1\nHost: 130.217.250.59:2000\nUser-Agent: InfluxDBClient\nContent-Length: 1190\nContent-Type: \nAccept-Encoding: gzip\n\ndata_amp_icmp,stream=3 loss=0i,lossrate=0.0,median=218i,packet_size=367i,results=1i,rtts=\"[218]\" 1563311880000000000\ndata_amp_icmp,stream=4 loss=0i,lossrate=0.0,median=36038i,packet_size=367i,results=1i,rtts=\"[36038]\" 1563311880000000000\ndata_amp_icmp,stream=3 loss=0i,lossrate=0.0,median=178i,packet_size=552i,results=1i,rtts=\"[178]\" 1563311940000000000\ndata_amp_icmp,stream=4 loss=0i,lossrate=0.0,median=36148i,packet_size=552i,results=1i,rtts=\"[36148]\" 1563311940000000000\ndata_amp_icmp,stream=3 loss=0i,lossrate=0.0,median=810i,packet_size=216i,results=1i,rtts=\"[810]\" 1563312000000000000\ndata_amp_icmp,stream=4 loss=0i,lossrate=0.0,median=36764i,packet_size=216i,results=1i,rtts=\"[36764]\" 1563312000000000000\ndata_amp_icmp,stream=3 loss=0i,lossrate=0.0,median=236i,packet_size=391i,results=1i,rtts=\"[236]\" 1563312060000000000\ndata_amp_icmp,stream=4 loss=0i,lossrate=0.0,median=36360i,packet_size=391i,results=1i,rtts=\"[36360]\" 1563312060000000000\ndata_amp_icmp,stream=3 loss=0i,lossrate=0.0,median=357i,packet_size=572i,results=1i,rtts=\"[357]\" 1563312120000000000\ndata_amp_icmp,stream=4 loss=0i,lossrate=0.0,median=36275i,packet_size=572i,results=1i,rtts=\"[36275]\" 1563312120000000000")

    val entries = oneRequest.flatMap(x => ICMPFactory.CreateICMPs(x))
    entries.print

    //val server = new SubscriptionServer
    //server.run()

  }
}