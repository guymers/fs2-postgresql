val fs2Version = "1.0.0"
val catsVersion = "1.4.0" // version used by fs2

lazy val commonSettings = Seq(
  name := "fs2-postgresql",
  organization := "com.github.guymers",
  licenses ++= Seq(("MIT", url("http://opensource.org/licenses/MIT"))),
  scalaVersion := "2.12.7",
  crossScalaVersions := Seq("2.11.12", scalaVersion.value),

  // https://tpolecat.github.io/2017/04/25/scalac-flags.html
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-explaintypes",
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked",
    "-Xcheckinit",
    "-Xfatal-warnings",
    "-Xfuture",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ypartial-unification",
    "-Ywarn-dead-code",
    "-Ywarn-inaccessible",
    "-Ywarn-infer-any",
    "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit",
    "-Ywarn-numeric-widen",
    "-Ywarn-unused",
    "-Ywarn-value-discard"
  ),
  scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, minor)) if minor >= 12 => Seq("-Ywarn-extra-implicit")
    case _ => Seq.empty
  }),

  scalacOptions in (Compile, console) --= Seq(
    "-Xfatal-warnings",
    "-Ywarn-unused"
  ),

  wartremoverErrors in (Compile, compile) := Warts.allBut(
    Wart.Any,
    Wart.ExplicitImplicitTypes,
    Wart.ImplicitParameter,
    Wart.Nothing,
    Wart.PublicInference,
    Wart.Recursion,
  ),

  wartremoverErrors in (Compile, console) := Seq.empty,
  wartremoverErrors in (Test, compile) := (wartremoverErrors in (Compile, compile)).value.filterNot { w =>
    Seq(
      Wart.Equals
    ).exists(_.clazz == w.clazz)
  },

  dependencyOverrides += "org.scala-lang" % "scala-library" % scalaVersion.value,
  dependencyOverrides += "org.scala-lang" % "scala-reflect" % scalaVersion.value,

  fork := true,

  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oDF")
)


lazy val `fs2-postgresql` = project.in(file("."))
  .settings(commonSettings)
  .settings(Seq(
    addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.8" cross CrossVersion.binary),

    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.3",

      "org.typelevel" %% "cats-core" % catsVersion,
      "org.typelevel" %% "cats-free" % catsVersion,

      "co.fs2" %% "fs2-core" % fs2Version,
      "co.fs2" %% "fs2-io" % fs2Version,

      "org.scodec" %% "scodec-bits" % "1.1.7",
      "org.scodec" %% "scodec-core" % "1.10.3",

      "org.typelevel" %% "cats-laws" % catsVersion % Test,
      "org.typelevel" %% "cats-kernel-laws" % catsVersion % Test,
      "org.scalacheck" %% "scalacheck" % "1.14.0" % "test",
      "org.scalatest" %% "scalatest" % "3.0.5" % "test"
    )

  ))

lazy val example = project.in(file("example"))
  .settings(moduleName := "fs2-postgresql-example")
  .settings(skip in publish := true)
  .settings(commonSettings)
  .dependsOn(`fs2-postgresql`)
