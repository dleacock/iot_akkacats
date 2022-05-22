package actors

import actors.Device.Command.{CreateDevice, GetDeviceState, UpdateDevice}
import actors.Device.Response.{DeviceCreatedResponse, DeviceStateUpdatedResponse, GetDeviceStateResponse}
import actors.Device.{Command, DeviceCreated, DeviceState, DeviceUpdated, Event, Response}
import akka.actor.testkit.typed.scaladsl.{LogCapturing, ScalaTestWithActorTestKit}
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.util.Success

class DeviceTest
  extends ScalaTestWithActorTestKit(
    ConfigFactory.parseString("akka.actor.allow-java-serialization = on")
      .withFallback(EventSourcedBehaviorTestKit.config))
    with AnyWordSpecLike
    with BeforeAndAfterEach
    with LogCapturing {

  private val id: String = UUID.randomUUID().toString
  private val eventSourcedTestKit: EventSourcedBehaviorTestKit[Command, Event, DeviceState] =
    EventSourcedBehaviorTestKit[Command, Event, DeviceState](
      system,
      Device(id))

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    eventSourcedTestKit.clear()
  }

  "A Device" must {
    val initialState = "initial_state"
    val updatedState = "updated_state"

    "be created with initial state" in {
      val result = eventSourcedTestKit.runCommand[Response](replyTo => CreateDevice(id, initialState, replyTo))
      result.reply shouldBe DeviceCreatedResponse(id)
      result.event shouldBe DeviceCreated(id, initialState)
      result.stateOfType[DeviceState].state shouldBe initialState
      result.stateOfType[DeviceState].id shouldBe id
    }

    "handle update" in {
      eventSourcedTestKit.runCommand[Response](replyTo => CreateDevice(id, initialState, replyTo))

      val result = eventSourcedTestKit.runCommand[Response](replyTo => UpdateDevice(id, updatedState, replyTo))
      result.reply shouldBe DeviceStateUpdatedResponse(Success(DeviceState(id, updatedState)))
      result.event shouldBe DeviceUpdated(updatedState)
      result.stateOfType[DeviceState].state shouldBe updatedState
      result.stateOfType[DeviceState].id shouldBe id
    }

    "handle get" in {
      eventSourcedTestKit.runCommand[Response](replyTo => CreateDevice(id, initialState, replyTo))
      eventSourcedTestKit.runCommand[Response](replyTo => UpdateDevice(id, updatedState, replyTo))

      val result = eventSourcedTestKit.runCommand[Response](replyTo => GetDeviceState(id, replyTo))
      result.reply shouldBe GetDeviceStateResponse(Some(DeviceState(id, updatedState)))
    }
  }
}
