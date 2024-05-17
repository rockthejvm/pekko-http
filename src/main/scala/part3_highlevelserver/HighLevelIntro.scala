package part3_highlevelserver

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.stream.ActorMaterializer
import part2_lowlevelserver.HttpsContext

object HighLevelIntro extends App {

  implicit val system: ActorSystem = ActorSystem("HighLevelIntro")
  // implicit val materializer = ActorMaterializer() // needed only with Pekko Streams < 2.6
  import system.dispatcher

  // directives
  import org.apache.pekko.http.scaladsl.server.Directives._

  val simpleRoute: Route =
    path("home") { // DIRECTIVE
      complete(StatusCodes.OK) // DIRECTIVE
    }

  val pathGetRoute: Route =
    path("home") {
      get {
        complete(StatusCodes.OK)
      }
    }

  // chaining directives with ~

  val chainedRoute: Route =
    path("myEndpoint") {
      get {
        complete(StatusCodes.OK)
      } /* VERY IMPORTANT ---> */ ~
      post {
        complete(StatusCodes.Forbidden)
      }
    } ~
    path("home") {
      complete(
        HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   Hello from the high level Pekko HTTP!
            | </body>
            |</html>
          """.stripMargin
        )
      )
    } // Routing tree


  Http().bindAndHandle(pathGetRoute, "localhost", 8080)


}
