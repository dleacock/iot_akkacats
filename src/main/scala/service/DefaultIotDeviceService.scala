package service

import actors.PersistentDevice
import actors.PersistentDevice.Command.{
  AlertDevice,
  GetDeviceState,
  InitializeDevice
}
import actors.PersistentDevice.Response
import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.util.Timeout
import org.slf4j.LoggerFactory

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ ExecutionContext, Future }

// TODO do something about the code duplication

// TODO in order to make this more interesting of a project there needs to be another service and then this service
// aggregates over them both. Can use EitherT as well I think
class DefaultIotDeviceService(
  sharding: ClusterSharding
)(implicit ec: ExecutionContext)
    extends IotDeviceService {

  private val log = LoggerFactory.getLogger(this.getClass)

  private implicit val askTimeout: Timeout = Timeout(5.seconds)

  override def registerDevice(id: String): Future[Either[String, Done]] = {
    sharding
      .entityRefFor(PersistentDevice.TypeKey, id)
      .ask(replyTo => InitializeDevice(replyTo))
      .map {
        case Done => {
          log.info(s"registerDevice call returned for $id")
          Right(Done)
        }
        case _ => Left(s"registerDevice error returned for $id")
      }
  }

  override def processDeviceEvent(
    id: String,
    message: String
  ): Future[Either[String, Done]] = {
    sharding
      .entityRefFor(PersistentDevice.TypeKey, id)
      .ask(replyTo => AlertDevice(message, replyTo))
      .map {
        case Done => {
          log.info(s"processDeviceEvent return $id with msg $message")
          Right(Done)
        }
        case _ => {
          log.info(s"processDeviceEvent return $id with msg $message")
          Left("Error")
        }
      }
  }

  override def retrieveDevice(id: String): Future[Either[String, String]] = {
    sharding
      .entityRefFor(PersistentDevice.TypeKey, id)
      .ask(replyTo => GetDeviceState(replyTo))
      .map {
        case Response.DeviceResponse(device, state) => {
          log.info(s"processDeviceEvent return $device in $state")
          Right(s"${device.id} ${device.maybeMessage.map(_.toString)} $state")
        }
        case _ => {
          log.info(s"processDeviceEvent return error for $id")
          Left("Error")
        }
      }
  }
}
