package service

import actors.NotifierActor.{NotifierMessage, NotifierReply, Notify}
import actors.{NotifierActor, PersistentDevice}
import actors.PersistentDevice.Command.{AlertDevice, GetDeviceState, InitializeDevice}
import actors.PersistentDevice.Response
import akka.Done
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.util.Timeout
import notifier.{ConsoleLoggingNotifier, Notifier}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

// TODO There are two actor systems are use here. Clean that up
class DefaultIotDeviceService(
  system: ActorSystem[_],
  notifierActor: NotifierActor)
    extends IotDeviceService {
  implicit val notifierSystem: ActorSystem[NotifierMessage] =
    ActorSystem(notifierActor, "notifierActor")
  implicit val timeout: Timeout = Timeout(5.seconds)
  implicit val ec: ExecutionContext = system.executionContext

  private val sharding: ClusterSharding = ClusterSharding(system)

  // TODO how is ClusterSharding knowing what ID to use to map to actor?
  // TODO is this the right location for it?
  sharding.init(Entity(typeKey = PersistentDevice.TypeKey) { _ =>
    PersistentDevice("device_id", "device_name")
  })

  // TODO need params
  override def registerDevice(): Future[Either[String, Done]] = {
    val entityRef: EntityRef[PersistentDevice.Command] =
      sharding.entityRefFor(PersistentDevice.TypeKey, "device_id")
    val reply: Future[Response] =
      entityRef.ask(replyTo => InitializeDevice(replyTo))
    reply flatMap {
      case Response.DeviceResponse(maybeDevice) =>
        Future.successful(Right(Done))
      case _ => Future.successful(Left("Completed but problem"))
    } recoverWith { ex =>
      Future.failed(
        new RuntimeException("exception thrown problem")
      ) // need custom exception type - unable to complete?
    }

  }

  override def processDeviceEvent(): Future[Either[String, Done]] = {

    val entityRef: EntityRef[PersistentDevice.Command] =
      sharding.entityRefFor(PersistentDevice.TypeKey, "device_id")

    // TODO validate alert message?
    val reply: Future[Response] =
      entityRef.ask(replyTo => AlertDevice("alert", replyTo))
    reply flatMap {
      // TODO CHECK the state? this is where we notify if needed
      case Response.DeviceResponse(maybeDevice) => {

        // TODO how do I tell the NotifierActor something and receive it?
        notifierSystem.ask[NotifierReply](replyTo => Notify(replyTo)) flatMap  {
          case NotifierActor.NotifySuccess => Future.successful(Right(Done))
          case NotifierActor.NotifyFailed(reason) => Future.successful(Left("notifier returned but failed"))
        }
      }
      case _ => Future.successful(Left("Completed but problem"))
    } recoverWith { ex =>
      Future.failed(
        new RuntimeException("exception thrown problem")
      ) // need custom exception type - unable to complete?
    }
  }

  override def retrieveDevice(): Future[Either[String, Done]] = {
    val entityRef: EntityRef[PersistentDevice.Command] =
      sharding.entityRefFor(PersistentDevice.TypeKey, "device_id")

    val reply: Future[Response] =
      entityRef.ask(replyTo => GetDeviceState(replyTo))
    reply flatMap {
      case Response.DeviceResponse(maybeDevice) =>
        Future.successful(Right(Done))
      case _ => Future.successful(Left("Completed but problem"))
    } recoverWith { ex =>
      Future.failed(
        new RuntimeException("exception thrown problem")
      ) // need custom exception type - unable to complete?
    }
  }
}
