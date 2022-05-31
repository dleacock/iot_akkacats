package actors

import actors.NotifierActor.NotifierMessage
import akka.Done
import akka.actor.typed.scaladsl.{ AbstractBehavior, ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, Behavior }
import notifier.Notifier

import scala.util.{ Failure, Success }

object NotifierActor {
  def apply(notifier: Notifier[Done]): Behavior[NotifierMessage] =
    Behaviors.setup(context => new NotifierActor(context, notifier))
  type NotifierFailed = String

  sealed trait NotifierMessage

  case class Notify(replyTo: ActorRef[Either[NotifierFailed, Done]])
      extends NotifierMessage
}

class NotifierActor[T](
  context: ActorContext[NotifierMessage],
  notifier: Notifier[Done])
    extends AbstractBehavior[NotifierMessage](context) {
  selfBehavior =>

  import NotifierActor._

  override def onMessage(msg: NotifierMessage): Behavior[NotifierMessage] = {
    case class WrappedNotifyResponse(
      notifierResponse: Either[NotifierFailed, Done],
      replyTo: ActorRef[Either[NotifierFailed, Done]])
        extends NotifierMessage

    msg match {
      case Notify(replyTo) => {
        context.pipeToSelf(notifier.sendNotification) {
          case Success(done) =>
            WrappedNotifyResponse(Right(done), replyTo)
          case Failure(exception) =>
            WrappedNotifyResponse(Left(exception.getMessage), replyTo)
        }
      }
      case WrappedNotifyResponse(notifierResponse, replyTo) =>
        replyTo ! notifierResponse
      case _ => context.log.error("Unknown message.")
    }

    selfBehavior
  }
}
