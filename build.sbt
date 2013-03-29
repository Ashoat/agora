name := "agora"

version := "0.1"

scalaVersion := "2.10.1"

// Slick

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= List(
  "com.typesafe.slick" %% "slick" % "1.0.0",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "net.liftweb" %% "lift-json" % "2.5-RC2"
)

// Continuations

autoCompilerPlugins := true

addCompilerPlugin("org.scala-lang.plugins" % "continuations" % "2.10.1")

scalacOptions ++= List(
  "-P:continuations:enable"//,
  //"-Xprint:selectivecps"
)
