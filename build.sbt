
lazy val buildSettings = Seq(
  organization := "io.darwin",
  version := "0.1.0",
  scalaVersion := "2.11.8"
  //updateOptions := updateOptions.value.withCachedResolution(true)
)

// Macro setting is any module that has macros, or manipulates meta trees
lazy val macroSettings = Seq(
  libraryDependencies += "org.scalameta" %% "scalameta" % "1.4.0",
  resolvers += Resolver.url(
    "scalameta",
    url("http://dl.bintray.com/scalameta/maven"))(Resolver.ivyStylePatterns),
  addCompilerPlugin(
    "org.scalameta" % "paradise" % "3.0.0-beta4" cross CrossVersion.full),
  scalacOptions += "-Xplugin-require:macroparadise"
)

lazy val allSettings = buildSettings

lazy val root = project
  .in(file("."))
  .settings(
    allSettings,
    moduleName := "macro-tuturial"
  )
  .aggregate(
    core,
    macros
  )
  .dependsOn(core)

lazy val core = project.settings(
  allSettings,
  moduleName := "macro-tut",
  libraryDependencies ++= Seq(
    //"com.lihaoyi"    %% "sourcecode"   % "0.1.2",
    "org.scalameta"  %% "scalameta"    % "1.4.0",
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    // Test dependencies
    "org.scalatest"                  %% "scalatest" % "3.0.1" % "test",
    "org.scalactic"                  %% "scalactic" % "3.0.1"
    //"com.googlecode.java-diff-utils" % "diffutils"  % "1.3.0" % "test"
  )
)

lazy val macros = project.settings(
  allSettings,
  macroSettings,
  libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1",
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)
