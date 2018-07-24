package scalafuzz

import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

object ScalafuzzSbtPlugin extends AutoPlugin {

  val Org = "org.scalafuzz"
  val ScalacRuntimeArtifact = "scalafuzz-scalac-runtime"
  val ScalacPluginArtifact = "scalafuzz-scalac-plugin"
  // this should match the version defined in build.sbt
  val DefaultScalafuzzVersion = "0.1.0-SNAPSHOT"
  val autoImport = ScalafuzzKeys
  lazy val ScalafuzzPluginConfig = config("scalafuzzPlugin").hide

  import autoImport._

  val aggregateFilter = ScopeFilter(inAggregates(ThisProject),
    inConfigurations(Compile)) // must be outside of the 'coverageAggregate' task (see: https://github.com/sbt/sbt/issues/1095 or https://github.com/sbt/sbt/issues/780)

  override def requires: JvmPlugin.type = plugins.JvmPlugin
  override def trigger: PluginTrigger = allRequirements

  override def globalSettings: Seq[Def.Setting[_]] = super.globalSettings ++ Seq(
    coverageEnabled := false,
    coverageExcludedPackages := "",
    coverageExcludedFiles := "",
    coverageScalacPluginVersion := DefaultScalafuzzVersion
  )

  override def buildSettings: Seq[Setting[_]] = super.buildSettings ++
    addCommandAlias("coverage", ";set coverageEnabled in ThisBuild := true") ++
    addCommandAlias("coverageOn", ";set coverageEnabled in ThisBuild := true") ++
    addCommandAlias("coverageOff", ";set coverageEnabled in ThisBuild := false")

  override def projectSettings: Seq[Setting[_]] = Seq(
    ivyConfigurations += ScalafuzzPluginConfig,
//    coverageReport := coverageReport0.value,
//    coverageAggregate := coverageAggregate0.value,
    aggregate in coverageAggregate := false
  ) ++ coverageSettings ++ scalacSettings

  private lazy val coverageSettings = Seq(
    libraryDependencies  ++= {
      if (coverageEnabled.value)
        Seq(
          // We only add for "compile"" because of macros. This setting could be optimed to just "test" if the handling
          // of macro coverage was improved.
          Org %% (scalacRuntime(libraryDependencies.value)) % coverageScalacPluginVersion.value,
          // We don't want to instrument the test code itself, nor add to a pom when published with coverage enabled.
          Org %% ScalacPluginArtifact % coverageScalacPluginVersion.value % ScalafuzzPluginConfig.name
        )
      else
        Nil
    }
  )

  private lazy val scalacSettings = Seq(
    scalacOptions in(Compile, compile) ++= {
      val updateReport = update.value
      if (coverageEnabled.value) {
        val scalafuzzDeps: Seq[File] = updateReport matching configurationFilter(ScalafuzzPluginConfig.name)
        val pluginPath: File =  scalafuzzDeps.find(_.getAbsolutePath.contains(ScalacPluginArtifact)) match {
          case None => throw new Exception(s"Fatal: $ScalacPluginArtifact not in libraryDependencies")
          case Some(pluginPath) => pluginPath
        }
        Seq(
          Some(s"-Xplugin:${pluginPath.getAbsolutePath}"),
          Some(s"-P:scalafuzz:dataDir:${crossTarget.value.getAbsolutePath}/scalafuzz-data"),
          Option(coverageExcludedPackages.value.trim).filter(_.nonEmpty).map(v => s"-P:scalafuzz:excludedPackages:$v"),
          Option(coverageExcludedFiles.value.trim).filter(_.nonEmpty).map(v => s"-P:scalafuzz:excludedFiles:$v")
        ).flatten
      } else {
        Nil
      }
    }
  )

  private def scalacRuntime(deps: Seq[ModuleID]): String = {
    ScalacRuntimeArtifact + optionalScalaJsSuffix(deps)
  }

  // returns "_sjs$sjsVersion" for Scala.js projects or "" otherwise
  private def optionalScalaJsSuffix(deps: Seq[ModuleID]): String = {
    val sjsClassifier = deps.collectFirst {
      case moduleId if moduleId.organization == "org.scala-js" && moduleId.name == "scalajs-library" => moduleId.revision
    }.map(_.take(3)).map(sjsVersion => "_sjs" + sjsVersion)

    sjsClassifier getOrElse ""
  }

}
