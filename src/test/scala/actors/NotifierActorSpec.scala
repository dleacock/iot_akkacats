package actors

import akka.Done
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorRef
import notifier.Notifier
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future

class NotifierActorSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike {

  import NotifierActor._

  val dummyNotifier: Notifier[Done] = new Notifier[Done] {
    override def sendNotification: Future[Done] = Future.successful(Done)

    override def getType: String = "dummy"
  }

  "NotifierActor" must {
    "do the thing" in {
      val probe = createTestProbe[NotifierMessage]()
      val notifierActor = spawn(NotifierActor(dummyNotifier))

      notifierActor ! NotifierActor.Notify(probe.ref)
      val response: NotifierMessage = probe.receiveMessage()
      val str = response match {
        case NotifierResponse(response) => response
      }
      str shouldBe "done"

    }
  }
}
