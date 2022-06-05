package actors

import actors.PersistentDevice.Response._
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

import scala.util.{Success, Try}

object PersistentDevice {

  sealed trait Command

  object Command {
    case class InitializeDevice(replyTo: ActorRef[Response]) extends Command

    case class DisableDevice(replyTo: ActorRef[Response]) extends Command

    case class AlertDevice(message: String, replyTo: ActorRef[Response])
        extends Command

    case class StopAlert(replyTo: ActorRef[Response]) extends Command

    case class GetDeviceState(replyTo: ActorRef[Response]) extends Command
  }

  sealed trait Response

  object Response {
    case class DeviceResponse(maybeDevice: Try[Device]) extends Response

    case class GetDeviceStateResponse(maybeDevice: Option[State])
      extends Response
  }

  sealed trait Event

  case class DeviceInitialized() extends Event

  case class DeviceAlerted(message: String) extends Event

  case class DeviceAlertStopped() extends Event

  case class DeviceDisabled() extends Event


  sealed trait State

  case class Inactive(device: Device) extends State

  case class Monitoring(device: Device) extends State

  case class Alerting(device: Device) extends State

  case class Device(id: String, stateMsg: Option[String])

  import Command._

  val commandHandler: (State, Command) => Effect[Event, State] =
    (state, command) => {
      state match {
        case Inactive(device) =>
          command match {
            case InitializeDevice(replyTo) =>
              Effect
                .persist(DeviceInitialized())
                .thenReply(replyTo)(_ => DeviceResponse(Success(device)))
            case GetDeviceState(replyTo) =>
              Effect.reply(replyTo)(
                GetDeviceStateResponse(Some(Inactive(device)))
              )
            case _ => Effect.none
          }
        case Monitoring(device) =>
          command match {
            case AlertDevice(message, replyTo) =>
              Effect
                .persist(DeviceAlerted(message))
                .thenReply(replyTo)(_ =>
                  DeviceResponse(
                    Success(device.copy(stateMsg = Some(message)))
                  )
                )
            case DisableDevice(replyTo) =>
              Effect
                .persist(DeviceDisabled())
                .thenReply(replyTo)(_ =>
                  DeviceResponse(Success(device))
                )
            case GetDeviceState(replyTo) =>
              Effect.reply(replyTo)(
                GetDeviceStateResponse(Some(Monitoring(device)))
              )
            case _ => Effect.none
          }
        case Alerting(device) =>
          command match {
            case StopAlert(replyTo) =>
              Effect
                .persist(DeviceAlertStopped())
                .thenReply(replyTo)(_ =>
                  DeviceResponse(Success(device.copy(stateMsg = None)))
                )
            case GetDeviceState(replyTo) =>
              Effect.reply(replyTo)(
                GetDeviceStateResponse(Some(Alerting(device)))
              )
            case _ => Effect.none
          }
      }
    }

  val eventHandler: (State, Event) => State =
    (state, event) =>
      state match {
        case inactive @ Inactive(device) =>
          event match {
            case DeviceInitialized() => Monitoring(device)
            case _                   => inactive
          }
        case monitoring @ Monitoring(device) =>
          event match {
            case DeviceAlerted(message) =>
              Alerting(device.copy(stateMsg = Some(message)))
            case DeviceDisabled() => Inactive(device)
            case _                => monitoring
          }
        case alerting @ Alerting(device) =>
          event match {
            case DeviceAlertStopped() =>
              Monitoring(device.copy(stateMsg = None))
            case _ => alerting
          }
      }

  val TypeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("PersistentDevice")

  def apply(persistenceId: PersistenceId): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      persistenceId = persistenceId,
      emptyState = Inactive(Device(persistenceId.id, None)),
      commandHandler = commandHandler,
      eventHandler = eventHandler
    )
}
