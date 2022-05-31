package actors

import actors.NotifierActor.NotifierMessage
import akka.Done
import akka.actor.typed.scaladsl.{ AbstractBehavior, ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, Behavior }
import notifier.Notifier

import scala.util.{ Failure, Success }

// TODO create tests
object NotifierActor {
  def apply(notifier: Notifier[Done]): Behavior[NotifierMessage] =
    Behaviors.setup(context => new NotifierActor(context, notifier))

  sealed trait NotifierMessage

  case class Notify(replyTo: ActorRef[NotifierResponse]) extends NotifierMessage

  // TODO Wrap response in Try?Option?Either?
  case class NotifierResponse(response: String) extends NotifierMessage
}

class NotifierActor(
  context: ActorContext[NotifierMessage],
  notifier: Notifier[Done])
    extends AbstractBehavior[NotifierMessage](context) {
  selfBehavior =>

  import NotifierActor._

  override def onMessage(msg: NotifierMessage): Behavior[NotifierMessage] = {
    case class WrappedNotifyResponse(
      notifierResponse: NotifierResponse,
      replyTo: ActorRef[NotifierResponse])
        extends NotifierMessage

    msg match {
      case Notify(replyTo) => {
        context.pipeToSelf(notifier.sendNotification) {
          case Success(_) => // TODO fix return payload
            WrappedNotifyResponse(NotifierResponse("done"), replyTo)
          case Failure(exception) =>
            WrappedNotifyResponse(
              NotifierResponse(
                s"${exception.getMessage}"
              ), // TODO improve this (Either?)
              replyTo
            )
        }
        selfBehavior
      }
      case WrappedNotifyResponse(notifierResponse, replyTo) =>
        replyTo ! notifierResponse
        selfBehavior
      case _ => {
        context.log.info("Unknown message.")
        selfBehavior
      }
    }
  }
}
