package playground

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.stream.ActorMaterializer
import org.apache.pekko.http.scaladsl.server.Directives._
import scala.io.StdIn

object Playground extends App {

  implicit val system: ActorSystem = ActorSystem("PekkoHttpPlayground")
  // implicit val materializer = ActorMaterializer() // needed only with Pekko Streams < 2.6

  import system.dispatcher

  val simpleRoute =
    pathEndOrSingleSlash {
      complete(HttpEntity(
        ContentTypes.`text/html(UTF-8)`,
        """
          |<html>
          | <body>
          |   Rock the JVM with Pekko HTTP!
          | </body>
          |</html>
        """.stripMargin
      ))
    }

  val bindingFuture = Http().bindAndHandle(simpleRoute, "localhost", 8080)
  // wait for a new line, then terminate the server
  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
