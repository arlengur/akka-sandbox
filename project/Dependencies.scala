import sbt.*

object Dependencies {
  val akkaVersion = "2.6.19"

  def akkaModule(module: String): ModuleID = "com.typesafe.akka" %% s"akka-$module" % akkaVersion

  lazy val akka: Seq[sbt.ModuleID] = Seq(
    "com.typesafe.akka" %% s"akka-http" % "10.2.0",
    akkaModule("protobuf-v3"),
    akkaModule("actor-typed"),
    akkaModule("slf4j"),
    akkaModule("stream"),
    akkaModule("discovery"),
    akkaModule("persistence-typed"),
    akkaModule("persistence-query"),
    akkaModule("coordination"),
    akkaModule("cluster"),
    akkaModule("cluster-tools"),
    akkaModule("actor-testkit-typed") % Test
  )

  lazy val json = "com.typesafe.play" %% "play-json" % "2.7.4"

  lazy val slf4j: Seq[ModuleID] = Seq("org.slf4j" % "slf4j-api" % "2.0.7",
    "org.slf4j" % "slf4j-simple" % "2.0.7")

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.1.0" % Test

  lazy val levelDb: Seq[ModuleID] = Seq("org.iq80.leveldb" % "leveldb" % "0.7",
    "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8")

  lazy val scalaLikeJdbc = "org.scalikejdbc" %% "scalikejdbc" % "3.5.0"

  lazy val cassandra: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-persistence-cassandra" % "1.0.5",
    "com.typesafe.akka" %% "akka-persistence-cassandra-launcher" % "1.0.5")

  lazy val postgres = "org.postgresql" % "postgresql" % "42.2.2"

  lazy val alpAkka = "com.lightbend.akka" %% "akka-stream-alpakka-slick" % "4.0.0"

  lazy val config = "com.typesafe" % "config" % "1.4.0"

  lazy val slick = Seq("com.typesafe.slick" %% "slick" % "3.3.3",
    "com.typesafe.slick" %% "slick-hikaricp" % "3.3.3")



}

