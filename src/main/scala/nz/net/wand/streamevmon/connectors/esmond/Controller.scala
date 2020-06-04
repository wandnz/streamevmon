package nz.net.wand.streamevmon.connectors.esmond

import nz.net.wand.streamevmon.connectors.esmond.schema.Archive

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import retrofit2.{Call, Callback, Response}
import retrofit2.Retrofit.Builder
import retrofit2.converter.jackson.JacksonConverterFactory

// TODO: Make this more generic.
// We should be able to pass onResponse methods at runtime along with the URL,
// so that responses can be handled uniquely.
class Controller extends Callback[List[Archive]] {

  val baseUrl = "http://denv-owamp.es.net:8085/esmond/perfsonar/"

  def start(): Unit = {
    val retrofit = new Builder()
      .addConverterFactory(JacksonConverterFactory.create(
        new ObjectMapper().registerModule(DefaultScalaModule)
      ))
      .baseUrl(baseUrl)
      .build()

    val esmondAPI = retrofit.create(classOf[EsmondAPI])

    val call = esmondAPI.root()
    call.enqueue(this)
  }

  override def onResponse(
    call                      : Call[List[Archive]],
    response                  : Response[List[Archive]]
  ): Unit = {
    if (response.isSuccessful) {
      response.body().foreach(println)
    }
    else {
      println(response.errorBody().string())
    }
  }

  override def onFailure(
    call                     : Call[List[Archive]],
    t                        : Throwable
  ): Unit = {
    println(t)
  }
}
