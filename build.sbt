import sbt.Keys._

val commonSettings = Seq(
  version := "0.1.0",
  scalaVersion := "2.11.7",
  organization := "org.globalnames",
  homepage := Some(new URL("http://globalnames.org/")),
  description := "Global scientific names resolver",
  startYear := Some(2015),
  licenses := Seq("MIT" -> new URL("https://github.com/GlobalNamesArchitecture/gnresolver/blob/master/LICENSE")),
  resolvers ++= Seq(
    "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
    "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
  ),
  javacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-source", "1.6",
    "-target", "1.6",
    "-Xlint:unchecked",
    "-Xlint:deprecation"),
  scalacOptions ++= List(
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-Xlint",
    "-language:_",
    "-target:jvm-1.6",
    "-Xlog-reflective-calls"))

val publishingSettings = Seq(
  publishMavenStyle := true,
  useGpg := true,
  publishTo <<= version { v: String =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
    else                             Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  pomIncludeRepository := { _ => false },
  pomExtra :=
    <scm>
      <url>git@github.com:GlobalNamesArchitecture/gnresolver.git</url>
      <connection>scm:git:git@github.com:GlobalNamesArchitecture/gnresolver.git</connection>
    </scm>
      <developers>
        <developer>
          <id>dimus</id>
          <name>Dmitry Mozzherin</name>
        </developer>
        <developer>
          <id>alexander-myltsev</id>
          <name>Alexander Myltsev</name>
          <url>http://myltsev.name</url>
        </developer>
      </developers>)

val noPublishingSettings = Seq(
  publishArtifact := false,
  publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo"))))

/////////////////////// DEPENDENCIES /////////////////////////

val slick          = "com.typesafe.slick" %% "slick"                 % "3.0.0"
val slf4jnop       = "org.slf4j"          %  "slf4j-nop"             % "1.6.4"
val mysql          = "mysql"              %  "mysql-connector-java"  % "5.1.35"
val hikariCP       = "com.zaxxer"         %  "HikariCP"              % "2.4.1"
val specs2core     = "org.specs2"         %% "specs2-core"           % "3.6.3" % Test
val specs2extra    = "org.specs2"         %% "specs2-matcher-extra"  % "3.6.3" % Test

/////////////////////// PROJECTS /////////////////////////

lazy val root = project.in(file("."))
  .aggregate(resolver, dataMigrate, benchmark)
  .settings(noPublishingSettings: _*)
  .settings(
    crossScalaVersions := Seq("2.10.3", "2.11.7")
  )

lazy val resolver = (project in file("./resolver"))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings: _*)
  .settings(publishingSettings: _*)
  .settings(
    name := "gnresolver",

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "org.globalnames.resolver",
    test in assembly := {},

    libraryDependencies ++= Seq(specs2core, specs2extra),

    scalacOptions in Test ++= Seq("-Yrangepos")
  )

lazy val benchmark = (project in file("./benchmark"))
  .dependsOn(resolver)
  .enablePlugins(JmhPlugin)
  .settings(commonSettings: _*)
  .settings(noPublishingSettings: _*)
  .settings(
    name := "gnresolver-benchmark"
  )

lazy val dataMigrate = (project in file("./data-migrate"))
  .dependsOn(resolver)
  .settings(commonSettings: _*)
  .settings(noPublishingSettings: _*)
  .settings(
    name := "gnresolver-data-migrate",

    libraryDependencies ++= Seq(slick, slf4jnop, mysql, hikariCP)
  )
