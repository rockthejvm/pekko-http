package part3_highlevelserver

import java.util.concurrent.TimeUnit

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.pekko.http.scaladsl.model.{HttpResponse, StatusCodes}
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.stream.ActorMaterializer
import org.apache.pekko.http.scaladsl.server.Directives._
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtJson}
import spray.json._

import scala.util.{Failure, Success}


case class LoginRequest(username: String, password: String)

trait SecurityProtocol extends DefaultJsonProtocol {
  implicit val loginRequestFormat: RootJsonFormat[LoginRequest] = jsonFormat2(LoginRequest)
}

object JwtAuthorization extends App with SprayJsonSupport with SecurityProtocol {

  implicit val system: ActorSystem = ActorSystem()
  // implicit val materializer = ActorMaterializer() // needed only with Pekko Streams < 2.6
  import system.dispatcher

  val superSecretPasswordDb = Map(
    "admin" -> "admin",
    "daniel" -> "Rockthejvm1!"
  )

  val algorithm = JwtAlgorithm.HS256
  val secretKey = "rockthejvmsecret"

  def checkPassword(username: String, password: String): Boolean =
    superSecretPasswordDb.contains(username) && superSecretPasswordDb(username) == password

  def createToken(username: String, expirationPeriodInDays: Int): String = {
    val claims = JwtClaim(
      expiration = Some(System.currentTimeMillis() / 1000 + TimeUnit.DAYS.toSeconds(expirationPeriodInDays)),
      issuedAt = Some(System.currentTimeMillis() / 1000),
      issuer = Some("rockthejvm.com")
    )

    JwtJson.encode(claims, secretKey, algorithm) // JWT string
  }

  def isTokenExpired(token: String): Boolean = JwtJson.decode(token, secretKey, Seq(algorithm)) match {
    case Success(claims) => claims.expiration.getOrElse(0L) < System.currentTimeMillis() / 1000
    case Failure(_) => true
  }

  def isTokenValid(token: String): Boolean = JwtJson.isValid(token, secretKey, Seq(algorithm))

  val loginRoute =
    post {
      entity(as[LoginRequest]) {
        case LoginRequest(username, password) if checkPassword(username, password) =>
          val token = createToken(username, 1)
          respondWithHeader(RawHeader("Access-Token", token)) {
            complete(StatusCodes.OK)
          }
        case _ => complete(StatusCodes.Unauthorized)
      }
    }

  val authenticatedRoute =
    (path("secureEndpoint") & get) {
      optionalHeaderValueByName("Authorization") {
        case Some(token) =>
          if (isTokenValid(token)) {
            if (isTokenExpired(token)) {
              complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token expired."))
            } else {
              complete("User accessed authorized endpoint!")
            }
          } else {
            complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "Token is invalid, or has been tampered with."))
          }
        case _ => complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "No token provided!"))
      }
    }

  val route = loginRoute ~ authenticatedRoute

  Http().bindAndHandle(route, "localhost", 8080)
}
