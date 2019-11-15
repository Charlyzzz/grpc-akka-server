name := "live"

version := "0.1"

scalaVersion := "2.13.1"

val akkaTypedVersion = "2.5.25"

libraryDependencies ++= Seq(
  "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % "1.0.4",
  "com.typesafe.akka" %% "akka-stream-kafka" % "1.1.0",
  "com.typesafe.akka" %% "akka-actor-typed" % akkaTypedVersion,
  "com.typesafe.akka" %% "akka-cluster-typed" % akkaTypedVersion,

  "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaTypedVersion,
  "org.scalatest" %% "scalatest" % "3.0.8" % Test
)

enablePlugins(AkkaGrpcPlugin)
enablePlugins(MultiJvmPlugin)
configs(MultiJvm)

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

maintainer in Docker := "Erwin Debusschere <erwincdl@gmail.com>"
packageSummary in Docker := "Live"
packageDescription := "Live server"
dockerEntrypoint := Seq("/opt/docker/bin/live-server")
dockerExposedPorts := Seq(8558, 2552, 8080)