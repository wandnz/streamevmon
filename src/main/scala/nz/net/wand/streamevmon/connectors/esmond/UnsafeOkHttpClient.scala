package nz.net.wand.streamevmon.connectors.esmond

import java.security.cert.X509Certificate

import javax.net.ssl._
import okhttp3.OkHttpClient

/** Yikes, esmond APIs behind HTTPS use self-signed certificates. This class is
  * scary and totally disables certificate checking. It should really only trust
  * self-signed certs and probably do name verification as well, but it doesn't
  * yet.
  */
object UnsafeOkHttpClient {
  def get(): OkHttpClient = {
    val manager: TrustManager =
      new X509TrustManager() {
        override def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}

        override def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}

        override def getAcceptedIssuers: Array[X509Certificate] = Seq.empty[X509Certificate].toArray
      }
    val trustAllCertificates = Seq(manager).toArray

    val sslContext = SSLContext.getInstance("SSL")
    sslContext.init(null, trustAllCertificates, new java.security.SecureRandom())
    val sslSocketFactory = sslContext.getSocketFactory
    val okBuilder = new OkHttpClient.Builder()
    okBuilder.sslSocketFactory(sslSocketFactory, trustAllCertificates(0).asInstanceOf[X509TrustManager])
    okBuilder.hostnameVerifier((_: String, _: SSLSession) => true)
    okBuilder.build()
  }
}
