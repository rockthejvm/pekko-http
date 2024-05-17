package part4_client

import org.apache.pekko.pattern.ask

import scala.concurrent.duration._
import org.apache.pekko.actor.{Actor, ActorLogging, ActorSystem, Props}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.stream.ActorMaterializer
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.util.Timeout
import spray.json._


case class CreditCard(serialNumber: String, securityCode: String, account: String)

object PaymentSystemDomain {
  case class PaymentRequest(creditCard: CreditCard, receiverAccount: String, amount: Double)
  case object PaymentAccepted
  case object PaymentRejected
}

trait PaymentJsonProtocol extends DefaultJsonProtocol {
  implicit val creditCardFormat: RootJsonFormat[CreditCard] = jsonFormat3(CreditCard)
  implicit val paymentRequestFormat: RootJsonFormat[PaymentSystemDomain.PaymentRequest] = jsonFormat3(PaymentSystemDomain.PaymentRequest)
}

class PaymentValidator extends Actor with ActorLogging {
  import PaymentSystemDomain._

  override def receive: Receive = {
    case PaymentRequest(CreditCard(serialNumber, _, senderAccount), receiverAccount, amount) =>
      log.info(s"$senderAccount is trying to send $amount dollars to $receiverAccount")
      if (serialNumber == "1234-1234-1234-1234") sender() ! PaymentRejected
      else sender() ! PaymentAccepted
  }
}

object PaymentSystem extends App with PaymentJsonProtocol with SprayJsonSupport {

  // microservice for payments
  implicit val system: ActorSystem = ActorSystem("PaymentSystem")
  // implicit val materializer = ActorMaterializer() // needed only with Pekko Streams < 2.6
  import system.dispatcher
  import PaymentSystemDomain._

  val paymentValidator = system.actorOf(Props[PaymentValidator](), "paymentValidator")
  implicit val timeout: Timeout = Timeout(2.seconds)

  val paymentRoute =
    path("api" / "payments") {
      post {
        entity(as[PaymentRequest]) { paymentRequest =>
          val validationResponseFuture = (paymentValidator ? paymentRequest).map {
            case PaymentRejected => StatusCodes.Forbidden
            case PaymentAccepted => StatusCodes.OK
            case _ => StatusCodes.BadRequest
          }

          complete(validationResponseFuture)
        }
      }
    }

  Http().bindAndHandle(paymentRoute, "localhost", 8080)

}
