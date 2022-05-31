package actors

import akka.Done
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import notifier.Notifier
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future
// TODO clean up, improve  tests
class NotifierActorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  import NotifierActor._
  import NotifierMocks._

  "NotifierActor" must {
    val probe = createTestProbe[NotifierMessage]()

    "do the thing" in {
      val notifierActor = spawn(NotifierActor(mockSuccessNotifier))

      notifierActor ! NotifierActor.Notify(probe.ref)
      val response: NotifierMessage = probe.receiveMessage()
      val str = response match {
        case NotifierResponse(response) => response
        case _                          => "unmatched"
      }
      str shouldBe "done"
    }

    "cant do the thing" in {
      val notifierActor = spawn(NotifierActor(mockFailureNotifier))

      notifierActor ! NotifierActor.Notify(probe.ref)
      val response: NotifierMessage = probe.receiveMessage()
      val str = response match {
        case NotifierResponse(response) => response
        case _                          => "unmatched"
      }
      str shouldBe "problem"
    }
  }
}

object NotifierMocks {
  val mockSuccessNotifier: Notifier[Done] = new Notifier[Done] {
    override def sendNotification: Future[Done] = Future.successful(Done)

    override def getType: String = "dummy"
  }

  val mockFailureNotifier: Notifier[Done] = new Notifier[Done] {
    override def sendNotification: Future[Done] =
      Future.failed(new RuntimeException("problem"))

    override def getType: String = "bad"
  }
}
