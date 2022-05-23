package actors

import actors.Device.Command.{InitializeDevice, GetDeviceState, AlertDevice}
import actors.Device.Response.{DeviceInitializedResponse, DeviceStateUpdatedResponse, GetDeviceStateResponse}
import actors.Device.{Command, DeviceInitialized, Device, DeviceAlerted, Event, Response}
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
//  private val eventSourcedTestKit: EventSourcedBehaviorTestKit[Command, Event, Device] =
//    EventSourcedBehaviorTestKit[Command, Event, Device](
//      system,
//      Device(id))

  override protected def beforeEach(): Unit = {
    super.beforeEach()
//    eventSourcedTestKit.clear()
  }

//  "A Device" must {
//    val initialState = "initial_state"
//    val updatedState = "updated_state"
//
//    "be created with initial state" in {
//      val result = eventSourcedTestKit.runCommand[Response](replyTo => InitializeDevice(id, initialState, replyTo))
//      result.reply shouldBe DeviceInitializedResponse(id)
//      result.event shouldBe DeviceInitialized(id, initialState)
//      result.stateOfType[Device].state shouldBe initialState
//      result.stateOfType[Device].id shouldBe id
//    }
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
