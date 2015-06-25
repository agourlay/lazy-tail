import scalariform.formatter.preferences._

import bintray.Plugin._

name := "lazy-tail"

organization := "com.github.agourlay"

version := "0.2-SNAPSHOT"

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

scalaVersion := "2.11.7"

scalacOptions := Seq(
  "-unchecked",
  "-deprecation",
  "-encoding", "UTF-8",
  "-Ywarn-dead-code",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-feature",
  "-Ywarn-unused-import"
)

fork in Test := true

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(AlignParameters, true)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(PreserveDanglingCloseParenthesis, true)
  .setPreference(RewriteArrowSymbols, true)

libraryDependencies ++= {
  val akkaV      = "2.3.11"
  val akkaHttpV  = "1.0-RC4"
  val akkaSseV   = "0.14.0"
  val sprayJsonV = "1.3.2"
  val logbackV   = "1.1.3"
  val scalaTestV = "2.2.5"
  Seq(
    "com.typesafe.akka"  %% "akka-actor"                        % akkaV
    ,"com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaHttpV
    ,"com.typesafe.akka" %% "akka-http-experimental"            % akkaHttpV
    ,"de.heikoseeberger" %% "akka-sse"                          % akkaSseV
    ,"io.spray"          %% "spray-json"                        % sprayJsonV
    ,"ch.qos.logback"    %  "logback-classic"                   % logbackV
    ,"com.typesafe.akka" %% "akka-testkit"                      % akkaV      % "test"
    ,"org.scalatest"     %% "scalatest"                         % scalaTestV % "test"
  )
}

Seq(bintraySettings:_*)

Seq(Revolver.settings: _*)
