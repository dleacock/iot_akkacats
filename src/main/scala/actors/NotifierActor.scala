package actors

import actors.NotifierActor.{NotifierMessage, NotifierResponse}
import akka.Done
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import notifier.Notifier

import scala.util.{Failure, Success}


// TODO create tests

object NotifierActor {
  def apply(notifier: Notifier[Done]): Behavior[NotifierMessage] =
    Behaviors.setup(context => new NotifierActor(context, notifier))

  sealed trait NotifierMessage

  case class Notify(replyTo: ActorRef[NotifierResponse]) extends NotifierMessage

  case class NotifierResponse(response: String) extends NotifierMessage
}

class NotifierActor(context: ActorContext[NotifierMessage], notifier: Notifier[Done])
  extends AbstractBehavior[NotifierMessage](context) {
  self =>

  import NotifierActor._

  context.log.info(s"Creating Notifier using ${notifier.getType} implementation")

  override def onMessage(msg: NotifierMessage): Behavior[NotifierMessage] = {

    case class WrappedNotifyResponse(notifierResponse: NotifierResponse, replyTo: ActorRef[NotifierResponse])
      extends NotifierMessage

    msg match {
      case Notify(replyTo) => {
        context.pipeToSelf(notifier.sendNotification) {
          case Success(_) => WrappedNotifyResponse(NotifierResponse("done"), replyTo)
          case Failure(exception) => WrappedNotifyResponse(NotifierResponse(s"${exception.getMessage}"), replyTo)
        }
        self
      }
      case WrappedNotifyResponse(notifierResponse, replyTo) =>
        replyTo ! notifierResponse
        self
      case _ => {
        context.log.info("Unknown message.")
        self
      }
    }
  }
}
