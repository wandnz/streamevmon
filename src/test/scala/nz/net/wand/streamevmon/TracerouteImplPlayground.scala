package nz.net.wand.streamevmon

import nz.net.wand.streamevmon.connectors.postgres.AsInetPath

class TracerouteImplPlayground extends PostgresContainerSpec {

  "Traceroute" should {
    "work" in {
      getPostgres.getAllTracerouteMeta match {
        case None => fail()
        case Some(metas) => metas.foreach { meta =>
          val entries = getPostgres.getTracerouteData(meta.stream).get

          val paths = entries.map { entry =>
            val path = getPostgres.getTraceroutePath(entry)
            val asPath = getPostgres.getTracerouteAsPath(entry)

            AsInetPath(path.get.path, asPath.map(_.aspath))
          }.toSet

          paths.toSeq.foreach(println)
        }
      }
    }
  }
}
