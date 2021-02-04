/* This file is part of streamevmon.
 *
 * Copyright (C) 2021  The University of Waikato, Hamilton, New Zealand
 *
 * Author: Daniel Oosterwijk
 *
 * All rights reserved.
 *
 * This code has been developed by the University of Waikato WAND
 * research group. For further information please see https://wand.nz,
 * or our Github organisation at https://github.com/wanduow
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
 */

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
