package service

import actors.PersistentDevice
import actors.PersistentDevice.Command.{AlertDevice, GetDeviceState, InitializeDevice}
import actors.PersistentDevice.Response
import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import org.slf4j.LoggerFactory

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class DefaultIotDeviceService(
  sharding: ClusterSharding
)(implicit ec: ExecutionContext)
    extends IotDeviceService {

  private val log = LoggerFactory.getLogger(this.getClass)

  private implicit val askTimeout: Timeout = Timeout(5.seconds)

  override def registerDevice(id: String): Future[Either[String, Done]] = {
    val ref: EntityRef[PersistentDevice.Command] =
      sharding.entityRefFor(PersistentDevice.TypeKey, id)
    val eventualResponse: Future[Done] =
      ref.ask(replyTo => InitializeDevice(replyTo))

    val future: Future[Either[String, Done]] = eventualResponse.map {
      case Done => {
        log.info(s"registerDevice call returned for $id")
        Right(Done)
      }
      case _ => Left(s"registerDevice error returned for $id")
    }

    future
  }

  override def processDeviceEvent(
    id: String,
    message: String
  ): Future[Either[String, Done]] = {
    val ref: EntityRef[PersistentDevice.Command] =
      sharding.entityRefFor(PersistentDevice.TypeKey, id)

    val eventualResponse: Future[Done] =
      ref.ask(replyTo => AlertDevice(message, replyTo))

    val future: Future[Either[String, Done]] = eventualResponse.map {
      case Done=> {
        log.info(s"processDeviceEvent return $id with msg $message")
        Right(Done)
      }
      case _ => {
        log.info(s"processDeviceEvent return $id with msg $message")
        Left("Error")
      }
    }
    future
  }

  override def retrieveDevice(id: String): Future[Either[String, Done]] = {
    val ref: EntityRef[PersistentDevice.Command] =
      sharding.entityRefFor(PersistentDevice.TypeKey, id)

    val eventualResponse: Future[PersistentDevice.Response] =
      ref.ask(replyTo => GetDeviceState(replyTo))

    val future: Future[Either[String, Done]] = eventualResponse.map {
      case Response.DeviceResponse(device, state) => {
        log.info(s"processDeviceEvent return $device in $state")
        Right(Done)
      }
      case _ => {
        log.info(s"processDeviceEvent return error for $id")
        Left("Error")
      }
    }
    future
  }
}
