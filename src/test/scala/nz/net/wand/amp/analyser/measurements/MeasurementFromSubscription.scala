package nz.net.wand.amp.analyser.measurements

import org.scalatest.FlatSpec

//noinspection SimplifyBoolean
class MeasurementFromSubscription extends FlatSpec {

  def dnsSubscriptionLine =
    "data_amp_dns,stream=1 flag_aa=False,flag_ad=False,flag_cd=False,flag_qr=True,flag_ra=True,flag_rd=False,flag_tc=False,lossrate=0.0,opcode=0i,query_len=40i,rcode=0i,requests=1i,response_size=68i,rtt=35799i,total_additional=1i,total_answer=1i,total_authority=0i,ttl=0i 1563761810000000000"

  def icmpSubscriptionLine =
    "data_amp_icmp,stream=3 loss=0i,lossrate=0.0,median=225i,packet_size=520i,results=1i,rtts=\"[225]\" 1563761840000000000"

  def tracerouteSubscriptionLine =
    "data_amp_traceroute_pathlen,stream=5 path_length=12.0 1563764080000000000"

  def getDNS: Option[DNS] = {
    DNS.create(dnsSubscriptionLine)
  }

  def assertCorrectDNS(dns: DNS): Unit = {
    assert(dns.stream == "1")
    assert(dns.flag_aa == false)
    assert(dns.flag_ad == false)
    assert(dns.flag_cd == false)
    assert(dns.flag_qr == true)
    assert(dns.flag_ra == true)
    assert(dns.flag_rd == false)
    assert(dns.flag_tc == false)
    assert(dns.lossrate == 0.0)
    assert(dns.opcode == 0)
    assert(dns.query_len == 40)
    assert(dns.rcode == 0)
    assert(dns.requests == 1)
    assert(dns.response_size == 68)
    assert(dns.rtt == 35799)
    assert(dns.total_additional == 1)
    assert(dns.total_answer == 1)
    assert(dns.total_authority == 0)
    assert(dns.ttl == 0)
    assert(dns.time == 1563761810000000000L)
  }

  behavior of "getDNS"

  it should "convert an entry from a subscription into a DNS object" in {
    val dnsResult = getDNS

    dnsResult match {
      case Some(x) => assertCorrectDNS(x)
      case None    => fail()
    }
  }

  def getICMP: Option[ICMP] = {
    ICMP.create(icmpSubscriptionLine)
  }

  def assertCorrectICMP(icmp: ICMP): Unit = {
    assert(icmp.stream == "3")
    assert(icmp.loss == 0)
    assert(icmp.lossrate == 0.0)
    assert(icmp.median == 225)
    assert(icmp.packet_size == 520)
    assert(icmp.results == 1)
    assert(icmp.rtts == "\"[225]\"")
    assert(icmp.time == 1563761840000000000L)
  }

  behavior of "getICMP"

  it should "convert an entry from a subscription into an ICMP object" in {
    val icmpResult = getICMP

    icmpResult match {
      case Some(x) => assertCorrectICMP(x)
      case None    => fail()
    }
  }

  def getTraceroute: Option[Traceroute] = {
    Traceroute.create(tracerouteSubscriptionLine)
  }

  def assertCorrectTraceroute(traceroute: Traceroute): Unit = {
    assert(traceroute.stream == "5")
    assert(traceroute.path_length == 12.0)
    assert(traceroute.time == 1563764080000000000L)
  }

  behavior of "getTraceroute"

  it should "convert an entry from a subscription into a Traceroute object" in {
    val tracerouteResult = getTraceroute

    tracerouteResult match {
      case Some(x) => assertCorrectTraceroute(x)
      case None    => fail()
    }
  }

  def getFromFactory: Seq[Option[Measurement]] = {
    Seq(
      MeasurementFactory.createMeasurement(dnsSubscriptionLine),
      MeasurementFactory.createMeasurement(icmpSubscriptionLine),
      MeasurementFactory.createMeasurement(tracerouteSubscriptionLine)
    )
  }

  behavior of "getFromFactory"

  it should "convert several entries to their respective Measurement subclasses" in {
    val results = getFromFactory

    results.foreach {
      case Some(x) =>
        x match {
          case _: DNS        => assertCorrectDNS(x.asInstanceOf[DNS])
          case _: ICMP       => assertCorrectICMP(x.asInstanceOf[ICMP])
          case _: Traceroute => assertCorrectTraceroute(x.asInstanceOf[Traceroute])
          case _             => fail()
        }
      case None => fail()
    }
  }
}
