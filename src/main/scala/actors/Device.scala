package actors

import actors.Device.Response.{DeviceAlertedResponse, DeviceDisabledResponse, DeviceInitializedResponse, DeviceStateUpdatedResponse, DeviceStopAlertResponse, GetDeviceStateResponse}
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

import scala.util.{Success, Try}

// TODO This needs to be a finite state machine eventually

object Device {

  sealed trait Command

  object Command {
    case class InitializeDevice(id: String, name: String, replyTo: ActorRef[Response]) extends Command

    case class DisableDevice(id: String, replyTo: ActorRef[Response]) extends Command

    case class AlertDevice(id: String, message: String, replyTo: ActorRef[Response]) extends Command

    case class StopAlert(id: String, replyTo: ActorRef[Response]) extends Command

    case class GetDeviceState(id: String, replyTo: ActorRef[Response]) extends Command
  }

  sealed trait Event
  // TODO do I need the id for the event?

  case class DeviceInitialized(id: String, initialState: String) extends Event

  case class DeviceAlerted(message: String) extends Event

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

  // TODO use case class for state
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
            case InitializeDevice(id, name, replyTo) =>
              Effect
                .persist(DeviceInitialized(id, name))
                .thenReply(replyTo)(_ => DeviceInitializedResponse(id))
            case GetDeviceState(id, replyTo) =>
              Effect.reply(replyTo)(GetDeviceStateResponse(Some(Inactive(device))))
            case _ => Effect.none
          }
        case Monitoring(device) =>
          command match {
            case AlertDevice(id, message, replyTo) =>
              Effect
                .persist(DeviceAlerted(message))
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

  val eventHandler: (State, Event) => State = ???
  //    (state, event) =>
  //      event match {
  //        case DeviceInitialized(id, state) => Device(id, state)
  //        case DeviceStateUpdated(newState) => state.copy(id = state.id, state = newState)
  //      }

  def apply(id: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId(id),
      emptyState = Inactive(Device(id, "")), // TODO come up with better empty state
      commandHandler = commandHandler,
      eventHandler = eventHandler
    )
}
