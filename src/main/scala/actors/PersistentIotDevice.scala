package actors

import actors.PersistentIotDevice.Response._
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

import scala.util.{Success, Try}

object PersistentIotDevice {

  sealed trait Command

  object Command {
    case class InitializeDevice(id: String, replyTo: ActorRef[Response]) extends Command

    case class DisableDevice(id: String, replyTo: ActorRef[Response]) extends Command

    case class AlertDevice(id: String, message: String, replyTo: ActorRef[Response]) extends Command

    case class StopAlert(id: String, replyTo: ActorRef[Response]) extends Command

    case class GetDeviceState(id: String, replyTo: ActorRef[Response]) extends Command
  }

  sealed trait Event
  // TODO do I need the id for the event?

  case class DeviceInitialized(id: String) extends Event

  case class DeviceAlerted(id: String, message: String) extends Event

  case class DeviceAlertStopped(id: String) extends Event

  case class DeviceDisabled(id: String) extends Event

  sealed trait Response

  object Response {
    case class DeviceInitializedResponse(id: String) extends Response

    // TODO Either?
    // TODO coalesce these responses, lots of repeating
    case class DeviceAlertedResponse(maybeDevice: Try[Device]) extends Response

    case class DeviceStopAlertResponse(maybeDevice: Try[Device]) extends Response

    case class DeviceStateUpdatedResponse(maybeDevice: Try[Device]) extends Response

    case class DeviceDisabledResponse(maybeDevice: Try[Device]) extends Response

    case class GetDeviceStateResponse(maybeDevice: Option[State]) extends Response
  }

  sealed trait State

  case class Inactive(device: Device) extends State

  case class Monitoring(device: Device) extends State

  case class Alerting(device: Device) extends State

  case class Device(id: String, name: String)

  import Command._

  val commandHandler: (State, Command) => Effect[Event, State] =
    (state, command) => {
      state match {
        case Inactive(device) =>
          command match {
            case InitializeDevice(id, replyTo) =>
              Effect
                .persist(DeviceInitialized(id))
                .thenReply(replyTo)(_ => DeviceInitializedResponse(id))
            case GetDeviceState(id, replyTo) =>
              Effect.reply(replyTo)(GetDeviceStateResponse(Some(Inactive(device))))
            case _ => Effect.none
          }
        case Monitoring(device) =>
          command match {
            case AlertDevice(id, message, replyTo) =>
              Effect
                .persist(DeviceAlerted(id, message))
                .thenReply(replyTo)(_ => DeviceAlertedResponse(Success(device)))
            case DisableDevice(id, replyTo) =>
              Effect
                .persist(DeviceDisabled(id))
                .thenReply(replyTo)(_ => DeviceDisabledResponse(Success(device)))
            case GetDeviceState(id, replyTo) =>
              Effect.reply(replyTo)(GetDeviceStateResponse(Some(Monitoring(device))))
            case _ => Effect.none
          }
        case Alerting(device) =>
          command match {
            case StopAlert(id, replyTo) =>
              Effect
                .persist(DeviceAlertStopped(id))
                .thenReply(replyTo)(_ => DeviceStopAlertResponse(Success(device)))
            case GetDeviceState(id, replyTo) =>
              Effect.reply(replyTo)(GetDeviceStateResponse(Some(Alerting(device))))
            case _ => Effect.none
          }
      }
    }

  val eventHandler: (State, Event) => State =
    (state, event) =>
      state match {
        case inactive@Inactive(device) => event match {
          case DeviceInitialized(id) => Monitoring(device)
          case _ => inactive
        }
        case monitoring@Monitoring(device) => event match {
          case DeviceAlerted(id, message) => Alerting(device)
          case DeviceDisabled(id) => Inactive(device)
          case _ => monitoring
        }
        case alerting@Alerting(device) => event match {
          case DeviceAlertStopped(id) => Monitoring(device)
          case _ => alerting
        }
      }

  def apply(id: String, name: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId(id),
      emptyState = Inactive(Device(id, name)), // TODO come up with better empty state
      commandHandler = commandHandler,
      eventHandler = eventHandler
    )
}
