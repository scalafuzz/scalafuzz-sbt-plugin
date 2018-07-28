name := "scalafuzz-sbt-plugin"

organization := "org.scalafuzz"

sbtPlugin := true

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

resolvers ++= {
  if (isSnapshot.value) Seq(Resolver.sonatypeRepo("snapshots")) else Nil
}

libraryDependencies += "org.scalafuzz" %% "scalafuzz-scalac-plugin" % "0.1.5-SNAPSHOT"

publishMavenStyle := true

publishArtifact in Test := false

scriptedLaunchOpts ++= Seq(
  "-Xmx1024M",
  "-Dplugin.version=" + version.value
)

import ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepCommandAndRemaining("^ test"),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("^ publishSigned"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

releaseCrossBuild := false

publishTo := {
  if (isSnapshot.value)
    Some("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
  else
    Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
}

pomExtra := {
  <url>https://github.com/scoverage/sbt-scoverage</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
}

crossSbtVersions := Vector("0.13.17", "1.1.1")

scalariformAutoformat := false
