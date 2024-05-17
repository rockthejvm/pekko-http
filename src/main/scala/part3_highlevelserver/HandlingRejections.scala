package part3_highlevelserver

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.stream.ActorMaterializer
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.{MethodRejection, MissingQueryParamRejection, Rejection, RejectionHandler}

object HandlingRejections extends App {

  implicit val system: ActorSystem = ActorSystem("HandlingRejections")
  // implicit val materializer = ActorMaterializer() // needed only with Pekko Streams < 2.6
  import system.dispatcher


  val simpleRoute =
    path("api" / "myEndpoint") {
      get {
        complete(StatusCodes.OK)
      } ~
      parameter("id") { _ =>
        complete(StatusCodes.OK)
      }
    }

  // Rejection handlers
  val badRequestHandler: RejectionHandler = { (rejections: Seq[Rejection]) =>
    println(s"I have encountered rejections: $rejections")
    Some(complete(StatusCodes.BadRequest))
  }

  val forbiddenHandler: RejectionHandler = { (rejections: Seq[Rejection]) =>
    println(s"I have encountered rejections: $rejections")
    Some(complete(StatusCodes.Forbidden))
  }

  val simpleRouteWithHandlers =
    handleRejections(badRequestHandler) { // handle rejections from the top level
      // define server logic inside
      path("api" / "myEndpoint") {
        get {
          complete(StatusCodes.OK)
        } ~
        post {
          handleRejections(forbiddenHandler) { // handle rejections WITHIN
            parameter("myParam") { _ =>
              complete(StatusCodes.OK)
            }
          }
        }
      }
    }

//  Http().bindAndHandle(simpleRouteWithHandlers, "localhost", 8080)

  // list(method rejection, query param rejection)
  implicit val customRejectionHandler: RejectionHandler = RejectionHandler.newBuilder()
    .handle {
      case m: MissingQueryParamRejection =>
        println(s"I got a query param rejection: $m")
        complete("Rejected query param!")
    }
    .handle {
      case m: MethodRejection =>
        println(s"I got a method rejection: $m")
        complete("Rejected method!")
    }
    .result()

  // sealing a route

  Http().bindAndHandle(simpleRoute, "localhost", 8080)

}
