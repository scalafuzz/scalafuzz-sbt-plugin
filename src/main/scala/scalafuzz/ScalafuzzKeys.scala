package scalafuzz

import sbt._

object ScalafuzzKeys {
  lazy val scalafuzzEnabled = settingKey[Boolean]("controls whether code instrumentation is enabled or not")
  lazy val scalafuzzExcludedPackages = settingKey[String]("regex for excluded packages")
  lazy val scalafuzzExcludedFiles = settingKey[String]("regex for excluded file paths")
  lazy val scalafuzzScalacPluginVersion = settingKey[String]("version of scalafuzz-scalac-plugin to use")
}
