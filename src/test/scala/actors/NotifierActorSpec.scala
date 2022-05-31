package actors

import akka.Done
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import notifier.Notifier
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future
// TODO clean up, improve  tests
class NotifierActorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  import NotifierActor._

  val dummyNotifier: Notifier[Done] = new Notifier[Done] {
    override def sendNotification: Future[Done] = Future.successful(Done)

    override def getType: String = "dummy"
  }

  val badNotifier: Notifier[Done] = new Notifier[Done] {
    override def sendNotification: Future[Done] =
      Future.failed(new RuntimeException("problem"))

    override def getType: String = "bad"
  }

  "NotifierActor" must {
    val probe = createTestProbe[NotifierMessage]()

    "do the thing" in {
      val notifierActor = spawn(NotifierActor(dummyNotifier))

      notifierActor ! NotifierActor.Notify(probe.ref)
      val response: NotifierMessage = probe.receiveMessage()
      val str = response match {
        case NotifierResponse(response) => response
        case _                          => "unmatched"
      }
      str shouldBe "done"
    }

    "cant do the thing" in {
      val notifierActor = spawn(NotifierActor(badNotifier))

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
