package actors

import actors.Device.Command.{AlertDevice, GetDeviceState, InitializeDevice}
import actors.Device.Response.{DeviceInitializedResponse, DeviceStateUpdatedResponse, GetDeviceStateResponse}
import actors.Device.{Command, Device, DeviceAlerted, DeviceInitialized, Event, Inactive, Response, State}
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
  private val name: String = "test_device"
  private val eventSourcedTestKit: EventSourcedBehaviorTestKit[Command, Event, State] =
    EventSourcedBehaviorTestKit[Command, Event, State](
      system,
      actors.Device(id, name)) // TODO fix namespace collision

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    eventSourcedTestKit.clear()
  }

  "A Device" must {
    val initialState = "initial_state"
    val updatedState = "updated_state"
    val device = Device(id, name)

    "be created in inactive state" in {
      val result = eventSourcedTestKit.runCommand[Response](replyTo => GetDeviceState(id, replyTo))
      result.reply shouldBe GetDeviceStateResponse(Some(Inactive(device)))
      result.stateOfType[Inactive].device shouldBe device
    }
    //
    //    "handle update" in {
    //      eventSourcedTestKit.runCommand[Response](replyTo => InitializeDevice(id, initialState, replyTo))
    //
    //      val result = eventSourcedTestKit.runCommand[Response](replyTo => UpdateDevice(id, updatedState, replyTo))
    //      result.reply shouldBe DeviceStateUpdatedResponse(Success(Device(id, updatedState)))
    //      result.event shouldBe DeviceStateUpdated(updatedState)
    //      result.stateOfType[Device].state shouldBe updatedState
    //      result.stateOfType[Device].id shouldBe id
    //    }
    //
    //    "handle get" in {
    //      eventSourcedTestKit.runCommand[Response](replyTo => InitializeDevice(id, initialState, replyTo))
    //      eventSourcedTestKit.runCommand[Response](replyTo => UpdateDevice(id, updatedState, replyTo))
    //
    //      val result = eventSourcedTestKit.runCommand[Response](replyTo => GetDeviceState(id, replyTo))
    //      result.reply shouldBe GetDeviceStateResponse(Some(Device(id, updatedState)))
    //    }
    //  }
  }
}
