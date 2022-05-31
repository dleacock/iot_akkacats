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
    val probe = createTestProbe[Either[String, Done]]()

    "successfully call notifier and return positive result" in {
      val notifierActor = spawn(NotifierActor(mockSuccessNotifier))

      notifierActor ! NotifierActor.Notify(probe.ref)
      val response = probe.receiveMessage()
      val responsePayload = response match {
        case Left(notifierFailed)    => notifierFailed
        case Right(done) => done
      }

      responsePayload shouldBe Done
    }

    "fail to call notifier and return exception message" in {
      val notifierActor = spawn(NotifierActor(mockFailureNotifier))

      notifierActor ! NotifierActor.Notify(probe.ref)
      val response = probe.receiveMessage()
      val responsePayload = response match {
        case Left(notifierFailed)    => notifierFailed
        case Right(done) => done
      }
      responsePayload shouldBe exceptionMessage
    }
  }
}

object NotifierMocks {
  val exceptionMessage = "problem"

  val mockSuccessNotifier: Notifier[Done] = new Notifier[Done] {
    override def sendNotification: Future[Done] = Future.successful(Done)

    override def getType: String = "dummy"
  }

  val mockFailureNotifier: Notifier[Done] = new Notifier[Done] {
    override def sendNotification: Future[Done] =
      Future.failed(new RuntimeException(exceptionMessage))

    override def getType: String = "bad"
  }
}
