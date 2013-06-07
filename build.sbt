name := "AkkaIoSample2.2.0"

version := "0.0.1"

scalaVersion := "2.10.1"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq (
  "com.typesafe.akka" %% "akka-actor" % "2.2.0-RC1"
)
