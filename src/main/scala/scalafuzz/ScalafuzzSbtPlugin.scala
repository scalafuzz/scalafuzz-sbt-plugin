package scalafuzz

import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

object ScalafuzzSbtPlugin extends AutoPlugin {

  val Org = "org.scalafuzz"
  val ScalacRuntimeArtifact = "scalafuzz-scalac-runtime"
  val ScalacPluginArtifact = "scalafuzz-scalac-plugin"
  // this should match the version defined in build.sbt
  val DefaultScalafuzzVersion = "0.1.7-SNAPSHOT"
  val autoImport = ScalafuzzKeys
  lazy val ScalafuzzPluginConfig = config("scalafuzzPlugin").hide

  import autoImport._

  override def requires: JvmPlugin.type = plugins.JvmPlugin
  override def trigger: PluginTrigger = allRequirements

  override def globalSettings: Seq[Def.Setting[_]] = super.globalSettings ++ Seq(
    scalafuzzEnabled := false,
    scalafuzzExcludedPackages := "",
    scalafuzzExcludedFiles := "",
    scalafuzzScalacPluginVersion := DefaultScalafuzzVersion
  )

  override def buildSettings: Seq[Setting[_]] = super.buildSettings ++
    addCommandAlias("scalafuzz", ";set scalafuzzEnabled in ThisBuild := true") ++
    addCommandAlias("scalafuzzOn", ";set scalafuzzEnabled in ThisBuild := true") ++
    addCommandAlias("scalafuzzOff", ";set scalafuzzEnabled in ThisBuild := false")

  override def projectSettings: Seq[Setting[_]] = Seq(
    ivyConfigurations += ScalafuzzPluginConfig,
  ) ++ libDepsSettings ++ scalacSettings

  private lazy val libDepsSettings = Seq(
    libraryDependencies  ++= {
      if (scalafuzzEnabled.value)
        Seq(
          // We only add for "compile"" because of macros. This setting could be optimed to just "test" if the handling
          // of macro coverage was improved.
          Org %% scalacRuntime(libraryDependencies.value) % scalafuzzScalacPluginVersion.value,
          // We don't want to instrument the test code itself, nor add to a pom when published with coverage enabled.
          Org %% ScalacPluginArtifact % scalafuzzScalacPluginVersion.value % ScalafuzzPluginConfig.name
        )
      else
        Nil
    }
  )

  private lazy val scalacSettings = Seq(
    scalacOptions in(Compile, compile) ++= {
      val updateReport = update.value
      if (scalafuzzEnabled.value) {
        val scalafuzzDeps: Seq[File] = updateReport matching configurationFilter(ScalafuzzPluginConfig.name)
        val pluginPath: File =  scalafuzzDeps.find(_.getAbsolutePath.contains(ScalacPluginArtifact)) match {
          case None => throw new Exception(s"Fatal: $ScalacPluginArtifact not in libraryDependencies")
          case Some(path) => path
        }
        Seq(
          Some(s"-Xplugin:${pluginPath.getAbsolutePath}"),
          Some(s"-P:scalafuzz:dataDir:${crossTarget.value.getAbsolutePath}/scalafuzz-data"),
          Option(scalafuzzExcludedPackages.value.trim).filter(_.nonEmpty).map(v => s"-P:scalafuzz:excludedPackages:$v"),
          Option(scalafuzzExcludedFiles.value.trim).filter(_.nonEmpty).map(v => s"-P:scalafuzz:excludedFiles:$v")
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
