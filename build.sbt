ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.4"

lazy val akkaVersion = "2.10.0"
lazy val akkaHttpVersion = "10.7.0"
lazy val akkaGrpcVersion = sys.props.getOrElse("akka-grpc.version", "2.5.0")
enablePlugins(AkkaGrpcPlugin)

resolvers += "Akka library repository".at("https://repo.akka.io/maven")

fork := true

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      scalaVersion    := "3.3.4"
    )),
    name := "AutomaticAgent",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "com.typesafe.akka" %% "akka-pki"                 % akkaVersion,
      "io.circe"          %% "circe-core"               % "0.14.10",
      "io.circe"          %% "circe-parser"             % "0.14.10",
      "ch.qos.logback"    % "logback-classic"           % "1.5.7",
      "com.typesafe"      %  "config"                   % "1.4.3",
      "io.github.ollama4j" % "ollama4j"                 % "1.0.89",

      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.2.18"        % Test
    ),
//    Compile / mainClass := Some("com.llmchat.MainApp") // Set the main class explicitly
  )

