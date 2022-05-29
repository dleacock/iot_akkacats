package actors

import actors.NotifierActor.NotifierMessage
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import notifier.Notifier

object NotifierActor {
  def apply(notifier: Notifier): Behavior[NotifierMessage] =
    Behaviors.setup(context => new NotifierActor(context, notifier))

  sealed trait NotifierMessage

  case class Notify(replyTo: ActorRef[NotifierResponse]) extends NotifierMessage

  case class NotifierResponse(response: String) extends NotifierMessage

}

class NotifierActor(context: ActorContext[NotifierMessage], notifier: Notifier)
  extends AbstractBehavior[NotifierMessage](context) {

  import NotifierActor._

  context.log.info(s"Creating Notifier using ${notifier.getType} implementation")

  override def onMessage(msg: NotifierMessage): Behavior[NotifierMessage] = {
    msg match {
      case Notify(replyTo) => ???
      case NotifierResponse(response) => ???
    }
  }
}
