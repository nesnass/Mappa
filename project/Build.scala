import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "Mappa"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
  	"postgresql" % "postgresql" % "9.1-901.jdbc4",
  	"com.amazonaws" % "aws-java-sdk" % "1.3.11",
  	"net.sf.flexjson" % "flexjson" % "2.1",
  	"org.imgscalr" % "imgscalr-lib" % "4.2",
    javaCore,
    javaJdbc,
    javaEbean
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here    
    ebeanEnabled := true
  )

}
