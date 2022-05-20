package actors

import actors.Device.Response.DeviceCreatedResponse
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

import scala.util.Try

object Device {

  sealed trait Command

  object Command {
    case class CreateDevice(id: String, initialState: String, replyTo: ActorRef[Response]) extends Command

    case class UpdateDevice(id: String, newState: String, replyTo: ActorRef[Response]) extends Command

    case class GetDeviceState(id: String, replyTo: ActorRef[Response]) extends Command
  }

  sealed trait Event

  case class DeviceCreated(id: String, initialState: String) extends Event

  case class DeviceUpdated(newState: String) extends Event

  sealed trait Response

  object Response {
    case class DeviceCreatedResponse(id: String) extends Response

    // TODO Either?
    case class DeviceStateUpdatedResponse(maybeDevice: Try[DeviceState]) extends Response

    case class GetDeviceStateResponse(maybeDevice: Option[DeviceState]) extends Response
  }

  // TODO use case class for state
  case class DeviceState(id: String, state: String)

  import Command._

  val commandHandler: (DeviceState, Command) => Effect[Event, DeviceState] =
    (state, command) => {
      command match {
        case CreateDevice(id, initialState, replyTo) =>
          Effect
            .persist(DeviceCreated(id, initialState))
            .thenReply(replyTo)(_ => DeviceCreatedResponse(id))
        case UpdateDevice(id, newState, replyTo) => ???
        case GetDeviceState(id, replyTo) => ???
      }
    }

  val eventHandler: (DeviceState, Event) => DeviceState =
    (state, event) =>
      event match {
        case DeviceCreated(id, state) => DeviceState(id, state)
        case DeviceUpdated(newState) => ???
      }

  def apply(id: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, DeviceState](
      persistenceId = PersistenceId.ofUniqueId(id),
      emptyState = DeviceState(id, ""), // TODO come up with better empty state
      commandHandler = commandHandler,
      eventHandler = eventHandler
    )
}
