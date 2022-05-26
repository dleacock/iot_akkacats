package actors

import actors.PersistentIotDevice.Command._
import actors.PersistentIotDevice.Response._
import actors.PersistentIotDevice._
import akka.actor.testkit.typed.scaladsl.{LogCapturing, ScalaTestWithActorTestKit}
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.util.Success

class PersistentIotDeviceSpec
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
      PersistentIotDevice(id, name))

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    eventSourcedTestKit.clear()
  }

  "A Device" must {
    val device = Device(id, name)
    val alertMessage = "alert_message"

    "be created in inactive state" in {
      val result = eventSourcedTestKit.runCommand[Response](replyTo => GetDeviceState(id, replyTo))
      result.reply shouldBe GetDeviceStateResponse(Some(Inactive(device)))
      result.stateOfType[Inactive].device shouldBe device
    }

    "start monitoring once initialized" in {
      val result = eventSourcedTestKit.runCommand[Response](replyTo => InitializeDevice(id, replyTo))
      result.reply shouldBe DeviceInitializedResponse(id)
      result.stateOfType[Monitoring].device shouldBe device
    }

    "become Alerting when device is alerted after being initialized" in {
      eventSourcedTestKit.runCommand[Response](replyTo => InitializeDevice(id, replyTo))

      val result = eventSourcedTestKit.runCommand[Response](replyTo => AlertDevice(id, alertMessage, replyTo))
      result.reply shouldBe DeviceAlertedResponse(Success(device))
      result.stateOfType[Alerting].device shouldBe device
    }

    "stop Alerting and go back to Monitoring" in {
      eventSourcedTestKit.runCommand[Response](replyTo => InitializeDevice(id, replyTo))
      eventSourcedTestKit.runCommand[Response](replyTo => AlertDevice(id, alertMessage, replyTo))

      val result = eventSourcedTestKit.runCommand[Response](replyTo => StopAlert(id, replyTo))
      result.reply shouldBe DeviceStopAlertResponse(Success(device))
      result.stateOfType[Monitoring].device shouldBe device
    }
  }
}
