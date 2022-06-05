package service

import actors.NotifierActor.{ NotifierMessage, NotifierReply, Notify }
import actors.{ NotifierActor, PersistentDevice }
import actors.PersistentDevice.Command.{
  AlertDevice,
  GetDeviceState,
  InitializeDevice
}
import actors.PersistentDevice.Response
import akka.Done
import akka.actor.typed.{ ActorSystem, Behavior }
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  Entity,
  EntityRef
}
import akka.util.Timeout
import notifier.{ ConsoleLoggingNotifier, Notifier }
import org.slf4j.LoggerFactory

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor, Future }
import scala.concurrent.duration.DurationInt
import scala.util.{ Failure, Success }

// TODO There are two actor systems are use here. Clean that up
class DefaultIotDeviceService(
  sharding: ClusterSharding,
  notifierActor: Behavior[NotifierActor.NotifierMessage]
)(implicit executionContext: ExecutionContext)
    extends IotDeviceService {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private implicit val askTimeout: Timeout = Timeout(5.seconds)

  //  implicit val notifierSystem: ActorSystem[NotifierMessage] =
//    ActorSystem(notifierActor, "notifierActor")
//  implicit val timeout: Timeout = Timeout(5.seconds)
//  implicit val ec: ExecutionContext = system.executionContext

//  private val sharding: ClusterSharding = ClusterSharding(system)

  logger.info("Starting DefaultIotDeviceService")

  // TODO need params
  override def registerDevice(
    id: String
  ): Future[Either[String, Done]] = {
    logger.info(" DefaultIotDeviceService - Register Device")

//    // TODO how is ClusterSharding knowing what ID to use to map to actor?
//    // TODO is this the right location for it?
//    sharding.init(Entity(typeKey = PersistentDevice.TypeKey) { _ =>
//      PersistentDevice(id, name)
//    })

    val entityRef: EntityRef[PersistentDevice.Command] =
      sharding.entityRefFor(PersistentDevice.TypeKey, id)

    val reply: Future[Response] =
      entityRef.ask(replyTo => InitializeDevice(replyTo))
    reply flatMap {
      case Response.DeviceResponse(maybeDevice) => {
        maybeDevice match {
          case Failure(exception) =>
            logger.error(s"problem ${exception.getMessage}")
          case Success(value) =>
            logger
              .info(s"Device Response Success ${value.id}")
        }
        Future.successful(Right(Done))
      }
      case _ => Future.successful(Left("Completed but problem"))
    } recoverWith { ex =>
      Future.failed(
        new RuntimeException("exception thrown problem")
      ) // need custom exception type - unable to complete?
    }
  }

  override def processDeviceEvent(): Future[Either[String, Done]] = {
    Future(Right(Done))
//    val entityRef: EntityRef[PersistentDevice.Command] =
//      sharding.entityRefFor(PersistentDevice.TypeKey, "device_id")
//
//    // TODO validate alert message?
//    val reply: Future[Response] =
//      entityRef.ask(replyTo => AlertDevice("alert", replyTo))
//    reply flatMap {
//      // TODO CHECK the state? this is where we notify if needed
//      case Response.DeviceResponse(maybeDevice) => {
//
//        // TODO how do I tell the NotifierActor something and receive it?
//        notifierSystem.ask[NotifierReply](replyTo => Notify(replyTo)) flatMap {
//          case NotifierActor.NotifySuccess => Future.successful(Right(Done))
//          case NotifierActor.NotifyFailed(reason) =>
//            Future.successful(Left("notifier returned but failed"))
//        }
//      }
//      case _ => Future.successful(Left("Completed but problem"))
//    } recoverWith { ex =>
//      Future.failed(
//        new RuntimeException("exception thrown problem")
//      ) // need custom exception type - unable to complete?
//    }
  }

  override def retrieveDevice(): Future[Either[String, Done]] = {
    Future(Right(Done))

    //    val entityRef: EntityRef[PersistentDevice.Command] =
//      sharding.entityRefFor(PersistentDevice.TypeKey, "device_id")
//
//    val reply: Future[Response] =
//      entityRef.ask(replyTo => GetDeviceState(replyTo))
//    reply flatMap {
//      case Response.DeviceResponse(maybeDevice) =>
//        Future.successful(Right(Done))
//      case _ => Future.successful(Left("Completed but problem"))
//    } recoverWith { ex =>
//      Future.failed(
//        new RuntimeException("exception thrown problem")
//      ) // need custom exception type - unable to complete?
//    }
  }
}
