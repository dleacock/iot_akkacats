package actors

import actors.Device.Command.CreateDevice
import actors.Device.Response.DeviceCreatedResponse
import actors.Device.{Command, DeviceCreated, DeviceState, Event, Response}
import akka.actor.testkit.typed.scaladsl.{LogCapturing, ScalaTestWithActorTestKit}
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

class DeviceTest
  extends ScalaTestWithActorTestKit(EventSourcedBehaviorTestKit.config)
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

    "be created with initial state" in {
      val result = eventSourcedTestKit.runCommand[Response](replyTo => CreateDevice(id, "state", replyTo))
      result.reply shouldBe DeviceCreatedResponse(id)
      result.event shouldBe DeviceCreated(id)
      result.stateOfType[DeviceState].state shouldBe "state"
      result.stateOfType[DeviceState].id shouldBe id
    }

    "handle update" in {

    }

    "handle get" in {

    }
  }
}
