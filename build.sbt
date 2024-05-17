name := "pekko-http"

version := "0.1"

scalaVersion := "3.3.3"

val pekkoVersion = "1.0.1"
val scalaTestVersion = "3.2.17"

libraryDependencies ++= Seq(
  "org.apache.pekko" %% "pekko-stream" % pekkoVersion,
  "org.apache.pekko" %% "pekko-http" % pekkoVersion,
  "org.apache.pekko" %% "pekko-http-spray-json" % pekkoVersion,
  "org.apache.pekko" %% "pekko-http-testkit" % pekkoVersion,
  "org.apache.pekko" %% "pekko-testkit" % pekkoVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion,
  ("com.pauldijou" %% "jwt-play-json" % "5.0.0").cross(CrossVersion.for3Use2_13)
)
