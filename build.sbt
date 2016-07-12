name := "REST server"

//organization  := "org.smartjava"

scalaVersion  := "2.10.5"
//scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")




libraryDependencies ++= {
  val akkaV = "2.3.6"
  val sprayV = "1.3.2"
  val sparkV = "1.2.1"
  Seq(
    "io.spray"              %%  "spray-can"                 % sprayV,
    "io.spray"              %%  "spray-routing"             % sprayV,
    "io.spray"              %%  "spray-json"                % "1.3.1",
    "io.spray"              %%  "spray-testkit"             % sprayV    % "test",
    "com.typesafe.akka"     %%  "akka-actor"                % akkaV,
    "com.typesafe.akka"     %%  "akka-testkit"              % akkaV     % "test",
    "org.specs2"            %%  "specs2-core"               % "2.3.11"  % "test",
    "org.scalaz"            %%  "scalaz-core"               % "7.1.0",

    "com.datastax.spark"    %%  "spark-cassandra-connector" % sparkV,
    "org.apache.spark"      %%  "spark-core"                % sparkV,
    "com.datastax.cassandra" %  "cassandra-driver-core"     % "2.1.5"

  )
}







