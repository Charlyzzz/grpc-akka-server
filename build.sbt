name := "live"

version := "0.1"

scalaVersion := "2.13.1"

val akkaTypedVersion = "2.5.25"

libraryDependencies ++= Seq(
  "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % "1.0.4",
  "com.typesafe.akka" %% "akka-stream-kafka" % "1.1.0",
  "com.typesafe.akka" %% "akka-actor-typed" % akkaTypedVersion,
  "com.typesafe.akka" %% "akka-cluster-typed" % akkaTypedVersion
)
enablePlugins(AkkaGrpcPlugin)