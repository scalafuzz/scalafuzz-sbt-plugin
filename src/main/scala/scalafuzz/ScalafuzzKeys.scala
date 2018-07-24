package scalafuzz

import sbt._

object ScalafuzzKeys {
  lazy val coverageEnabled = settingKey[Boolean]("controls whether code instrumentation is enabled or not")
  lazy val coverageReport = taskKey[Unit]("run report generation")
  lazy val coverageAggregate = taskKey[Unit]("aggregate reports from subprojects")
  lazy val coverageExcludedPackages = settingKey[String]("regex for excluded packages")
  lazy val coverageExcludedFiles = settingKey[String]("regex for excluded file paths")
  lazy val coverageScalacPluginVersion = settingKey[String]("version of scalafuzz-scalac-plugin to use")
}
