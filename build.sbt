import sbt.Keys._

lazy val UnitTest   = config("unit").extend(Test)

lazy val ItTest     = config("it").extend(Test)

/////////////////////// DEPENDENCIES /////////////////////////

val akkaV           = "2.4.8"

val akkaActor       = "com.typesafe.akka"  %% "akka-actor"                          % akkaV
val akkaHttp        = "com.typesafe.akka"  %% "akka-http-core"                      % akkaV
val akkaHttpCore    = "com.typesafe.akka"  %% "akka-http-experimental"              % akkaV
val sprayJson       = "com.typesafe.akka"  %% "akka-http-spray-json-experimental"   % akkaV
val slick           = "com.typesafe.slick" %% "slick"                               % "3.1.1"
val logback         = "ch.qos.logback"     %  "logback-classic"                     % "1.1.7"
val postgresql      = "postgresql"         %  "postgresql"                          % "9.1-901.jdbc4"
val hikariSlick     = "com.typesafe.slick" %% "slick-hikaricp"                      % "3.1.1"
val gnparser        = "org.globalnames"    %% "gnparser"                            % "0.3.2"
val gnmatcher       = "org.globalnames"    %% "gnmatcher"                           % "0.1.0"
val scalaz          = "org.scalaz"         %% "scalaz-core"                         % "7.1.7"
val jodaTime        = "joda-time"          %  "joda-time"                           % "2.9.4"     % Test
val jodaConvert     = "org.joda"           %  "joda-convert"                        % "1.8.1"     % Test
val scalatest       = "org.scalatest"      %% "scalatest"                           % "2.2.6"     % Test
val akkaHttpTestkit = "com.typesafe.akka"  %% "akka-http-testkit"                   % akkaV       % Test
val pegdown         = "org.pegdown"        %  "pegdown"                             % "1.6.0"     % Test

//////////////////////////////////////////////////////////////

val commonSettings = Seq(
  version := "0.1.0-SNAPSHOT",
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
    "-Xlog-reflective-calls"),
  javaOptions += "-Dlog.level=" + sys.env.getOrElse("LOG_LEVEL", "OFF"),
  scalacOptions in Test ++= Seq("-Yrangepos")
)

val testSettings = Seq(
  fork in Test := true,
  javaOptions in test += "-Xms512M -Xmx512M -XX:+UseConcMarkSweepGC -XX:+UseParNewGC",
  libraryDependencies += pegdown,
  parallelExecution in ItTest := false,

  test in Test <<= (test in ItTest).dependsOn(test in UnitTest),

  testOptions in UnitTest := Seq(
    Tests.Argument("-h", "target/test-html"),
    Tests.Argument("-u", "target/test-xml"),
    Tests.Argument("-C", "org.globalnames.resolver.TestReporter"),
    Tests.Argument("-oD"), // Configuring summaries has no effect when running with SBT
    Tests.Argument("-eTNCXEHLOPQRM"), // T = full backtraces, NCXEHLOPQRM = Ignore all events that are already sent to out (SBT)
    Tests.Filter { testName => !testName.contains("Integration") }
  ),
  testOptions in ItTest := Seq(
    Tests.Argument("-h", "target/test-html"),
    Tests.Argument("-u", "target/test-xml"),
    Tests.Argument("-C", "org.globalnames.resolver.TestReporter"),
    Tests.Argument("-W", "2", "2"), // warn on slow tests (over 2 seconds) every 2 seconds
    Tests.Argument("-oD"), // Configuring summaries has no effect when running with SBT
    Tests.Argument("-eTNCXEHLOPQRM"), // T = full backtraces, NCXEHLOPQRM = Ignore all events that are already sent to out (SBT)
    Tests.Filter { testName => testName.contains("Integration") }
  )
)

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

/////////////////////// PROJECTS /////////////////////////

lazy val root = project.in(file("."))
  .aggregate(resolver, benchmark, api, front)
  .settings(noPublishingSettings: _*)
  .settings(
    crossScalaVersions := Seq("2.10.6", "2.11.7")
  )

lazy val resolver = (project in file("./resolver"))
  .configs(UnitTest, ItTest)
  .settings(inConfig(UnitTest)(Defaults.testTasks))
  .settings(inConfig(ItTest)(Defaults.testTasks))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings ++ publishingSettings ++ testSettings: _*)
  .settings(
    name := "gnresolver",

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "org.globalnames.resolver",
    test in assembly := {},

    libraryDependencies ++= Seq(slick, logback, postgresql, hikariSlick, gnparser, gnmatcher,
                                scalatest, jodaTime, jodaConvert)
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
  .configs(UnitTest, ItTest)
  .settings(inConfig(UnitTest)(Defaults.testTasks))
  .settings(inConfig(ItTest)(Defaults.testTasks))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings ++ testSettings: _*)
  .settings(
    name := "gnresolver-api",

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "org.globalnames.resolver.api",

    mainClass in reStart := Some("org.globalnames.resolver.api.GnresolverMicroservice"),
    libraryDependencies ++= Seq(akkaActor, akkaHttpCore, akkaHttp, sprayJson, akkaHttpTestkit,
                                scalatest, jodaTime, jodaConvert),

    mainClass in assembly := Some("org.globalnames.resolver.api.GnresolverMicroservice"),
    test in assembly := {},
    assemblyMergeStrategy in assembly := {
      case "logback.xml" => MergeStrategy.last
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
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
