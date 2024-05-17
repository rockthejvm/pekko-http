package part4_client

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.stream.ActorMaterializer
import org.apache.pekko.stream.scaladsl.Source

import scala.util.{Failure, Success}
import spray.json._

object RequestLevel extends App with PaymentJsonProtocol {

  implicit val system: ActorSystem = ActorSystem("RequestLevelAPI")
  // implicit val materializer = ActorMaterializer() // needed only with Pekko Streams < 2.6
  import system.dispatcher

  val responseFuture = Http().singleRequest(HttpRequest(uri = "http://www.google.com"))

  responseFuture.onComplete {
    case Success(response) =>
      // VERY IMPORTANT
      response.discardEntityBytes()
      println(s"The request was successful and returned: $response")
    case Failure(ex) =>
      println(s"The request failed with: $ex")
  }

  import PaymentSystemDomain._

  val creditCards = List(
    CreditCard("4242-4242-4242-4242", "424", "tx-test-account"),
    CreditCard("1234-1234-1234-1234", "123", "tx-daniels-account"),
    CreditCard("1234-1234-4321-4321", "321", "my-awesome-account")
  )

  val paymentRequests = creditCards.map(creditCard => PaymentRequest(creditCard, "rtjvm-store-account", 99))
  val serverHttpRequests = paymentRequests.map(paymentRequest =>
    HttpRequest(
      HttpMethods.POST,
      uri = "http://localhost:8080/api/payments",
      entity = HttpEntity(
        ContentTypes.`application/json`,
        paymentRequest.toJson.prettyPrint
      )
    )
  )

  Source(serverHttpRequests)
    .mapAsyncUnordered(10)(request => Http().singleRequest(request))
    .runForeach(println)



}
