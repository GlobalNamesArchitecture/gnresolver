import sbt.Keys._

val commonSettings = Seq(
  version := "0.1.0",
  scalaVersion := "2.11.8",
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
          <url>http://myltsev.com</url>
        </developer>
      </developers>)

val noPublishingSettings = Seq(
  publishArtifact := false,
  publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo"))))

/////////////////////// DEPENDENCIES /////////////////////////

val akkaV           = "2.4.7"

val akkaActor       = "com.typesafe.akka"  %% "akka-actor"                          % akkaV
val akkaHttp        = "com.typesafe.akka"  %% "akka-http-core"                      % akkaV
val akkaHttpCore    = "com.typesafe.akka"  %% "akka-http-experimental"              % akkaV
val sprayJson       = "com.typesafe.akka"  %% "akka-http-spray-json-experimental"   % akkaV
val slick           = "com.typesafe.slick" %% "slick"                               % "3.1.1"
val logback         = "ch.qos.logback"     %  "logback-classic"                     % "1.1.7"
val postgresql      = "postgresql"         %  "postgresql"                          % "9.1-901.jdbc4"
val hikariSlick     = "com.typesafe.slick" %% "slick-hikaricp"                      % "3.1.1"
val gnparser        = "org.globalnames"    %% "gnparser"                            % "0.3.1"
val gnmatcher       = "org.globalnames"    %% "gnmatcher"                           % "0.1.0"
val scalaz          = "org.scalaz"         %% "scalaz-core"                         % "7.1.7"
val scalatest       = "org.scalatest"      %% "scalatest"                           % "2.2.6"     % Test
val akkaHttpTestkit = "com.typesafe.akka"  %% "akka-http-testkit"                   % akkaV       % Test

/////////////////////// PROJECTS /////////////////////////

lazy val root = project.in(file("."))
  .aggregate(resolver, benchmark, api, front)
  .settings(noPublishingSettings: _*)
  .settings(
    crossScalaVersions := Seq("2.10.6", "2.11.7")
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

    libraryDependencies ++= Seq(slick, logback, postgresql, hikariSlick, gnparser, gnmatcher,
                                scalatest),

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

lazy val api = (project in file("./api"))
  .dependsOn(resolver)
  .settings(commonSettings: _*)
  .settings(
    name := "gnresolver-api",

    mainClass in reStart := Some("org.globalnames.resolver.api.GnresolverMicroservice"),
    libraryDependencies ++= Seq(akkaActor, akkaHttpCore, akkaHttp, sprayJson, akkaHttpTestkit,
                                scalatest)
  )

lazy val front = (project in file("./front"))
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(noPublishingSettings: _*)
  .settings(
    name := "gnresolver-front",
    packageName := "gnresolver-front",
    pipelineStages := Seq(digest, gzip),
    libraryDependencies ++= Seq(
      filters, cache, ws, gnparser
    )
  )

