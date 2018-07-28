import sbt.complete.DefaultParsers._

version := "0.1"

scalaVersion := "2.11.8"

crossScalaVersions := Seq("2.10.6", "2.11.8")

libraryDependencies += "org.specs2" %% "specs2" % "2.3.13" % "test"

val checkScalaVersion = inputKey[Unit]("Input task to compare the value of scalaVersion setting with a given input.")
checkScalaVersion := {
  val arg: String = (Space ~> StringBasic).parsed
  if (scalaVersion.value != arg) sys.error(s"scalaVersion [${scalaVersion.value}] not equal to expected [$arg]")
  ()
}

val checkScalafuzzEnabled = inputKey[Unit]("Input task to compare the value of scalafuzzEnabled setting with a given input.")
checkScalafuzzEnabled := {
  val arg: String = (Space ~> StringBasic).parsed
  if (scalafuzzEnabled.value.toString != arg) sys.error(s"coverageEnabled [${scalafuzzEnabled.value}] not equal to expected [$arg]")
  ()
}


resolvers ++= {
  if (sys.props.get("plugin.version").map(_.endsWith("-SNAPSHOT")).getOrElse(false)) Seq(Resolver.sonatypeRepo("snapshots"))
  else Seq.empty
}

// We force coverage to be always disabled for 2.10. This is not an uncommon real world scenario
scalafuzzEnabled := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 10)) => false
    case _ => scalafuzzEnabled.value
  }
}
