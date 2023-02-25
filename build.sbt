ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.1"

lazy val recompileNativeCode = taskKey[Unit]("recompile-native-code")
recompileNativeCode := {
    import sys.process._
    val nativeCodeDir = (ThisBuild / baseDirectory).value / "native" / "mandelbrot"
    val cargoBuild = Process("cargo build --release", nativeCodeDir)
    val cargoBuildResult = cargoBuild.!
    if (cargoBuildResult != 0) {
      throw new RuntimeException("Failed to compile native code")
    }
}
(compile in Compile) := ((compile in Compile) dependsOn recompileNativeCode).value

lazy val root = (project in file("."))
  .settings(
    name := "Mandelbrot",
    idePackagePrefix := Some("com.skdziwak.mandelbrot")
  )

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.15" % "test"
libraryDependencies += "net.java.dev.jna" % "jna" % "5.12.1"
libraryDependencies += "org.scala-lang.modules" %% "scala-swing" % "3.0.0"
