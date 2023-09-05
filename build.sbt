scalaVersion := "2.13.8"
version := "1.0" // project version
name := "grpc akka test"
organization := "ru.arlen"

libraryDependencies ++= Dependencies.akka
libraryDependencies += Dependencies.json
libraryDependencies ++= Dependencies.slf4j
libraryDependencies += Dependencies.scalaTest
libraryDependencies ++= Dependencies.levelDb
libraryDependencies += Dependencies.scalaLikeJdbc
libraryDependencies += Dependencies.postgres
libraryDependencies ++= Dependencies.cassandra
libraryDependencies += Dependencies.alpAkka

enablePlugins(AkkaGrpcPlugin)